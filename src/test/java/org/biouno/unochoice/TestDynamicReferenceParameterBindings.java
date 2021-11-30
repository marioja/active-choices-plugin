/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2020 Ioannis Moutsatsos, Bruno P. Kinoshita
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.biouno.unochoice;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProjectTest.scheduleAndFindBranchProject;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;

public class TestDynamicReferenceParameterBindings {

    private static final Logger logger=LoggerFactory.getLogger(TestDynamicReferenceParameterBindings.class);
	private final String SCRIPT = "return ['D', 'C', 'B', 'A']";
    private final String FALLBACK_SCRIPT = "";
    private static String JENKINSFILE;
    private final static String ip="String value=\"Cannot retrieve anything\"\n" + 
    		"String activeChoiceBinding=null\n" + 
    		"String exceptionMessage=\"\"\n" + 
    		"try {activeChoiceBinding=jenkinsBuild.toString()} catch(Exception e){exceptionMessage=\"(\"+e.getMessage()+\")\"};\n" + 
    		"if (activeChoiceBinding==null) value=\"Cannot retrieve jenkinsBuild\"+exceptionMessage;\n" + 
    		"else value=activeChoiceBinding;\n" + 
    		"activeChoiceBinding=null;\n" + 
    		"exceptionMessage=\"\"\n" + 
    		"try {activeChoiceBinding=jenkinsProject.toString()} catch(Exception e){exceptionMessage=\"(\"+e.getMessage()+\")\"};\n" + 
    		"if (activeChoiceBinding==null) value=value+\"/Cannot retrieve jenkinsProject\"+exceptionMessage;\n" + 
    		"else value=value+\"/\"+activeChoiceBinding;\n" + 
    		"activeChoiceBinding=null;\n" + 
    		"exceptionMessage=\"\"\n" + 
    		"try {activeChoiceBinding=jenkinsParameter.toString()} catch(Exception e){exceptionMessage=\"(\"+e.getMessage()+\")\"};\n" + 
    		"if (activeChoiceBinding==null) value=value+\"/Cannot retrieve jenkinsParameter\"+exceptionMessage;\n" + 
    		"else value=value+\"/\"+activeChoiceBinding;\n" + 
    		"return '<input name=\"value\" value=\"'+value+'\" class=\"setting-input\" type=\"text\">'";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();
    
    @BeforeClass public static void onlyOnce() throws UnsupportedEncodingException, IOException {
    	JENKINSFILE=new String(Files.readAllBytes(Paths.get("src/test/resources/org/biouno/unochoice/Jenkinsfile")), "UTF-8");
    }

    @Before
    public void setUp() throws Exception {
        ScriptApproval.get().preapprove(SCRIPT, GroovyLanguage.get());
        ScriptApproval.get().preapprove(FALLBACK_SCRIPT, GroovyLanguage.get());
    }

    @Test
    public void testConstructor() throws Exception {
    	try {
        	sampleRepo.init();
        	sampleRepo.write("Jenkinsfile", JENKINSFILE);
        	sampleRepo.git("add","Jenkinsfile");
        	sampleRepo.git("commit", "--all", "--message=flow");
        	WorkflowMultiBranchProject mp = jenkins.createProject(WorkflowMultiBranchProject.class, "p");
        	mp.getSourcesList().add(new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false)));
        	WorkflowJob p = scheduleAndFindBranchProject(mp, "master");
        	assertEquals(1, mp.getItems().size());
        	jenkins.waitUntilNoActivity();
        	WorkflowRun b1 = p.getLastBuild();
        	assertEquals(1, b1.getNumber());
        	jenkins.assertLogContains("ip=\r", b1);
        	WorkflowRun b2 = jenkins.assertBuildStatusSuccess(p.scheduleBuild2(0,  new ParametersAction(new StringParameterValue("INTERESTED_PARTIES", "hello bye"))));
    	} finally {
        	logger.info("Temp dir copied to: "+saveTempDir());
    	}
    }
    public static void main(String[] args) throws IOException {
//		System.out.println(ip);
    	System.out.println("Temp dir copied to: "+saveTempDir());
	}

	private static Path saveTempDir() throws IOException {
		String td = System.getProperty("java.io.tmpdir");
		final Path tdp = Paths.get(td);
		final Path dest = Files.createTempDirectory(Paths.get(System.getProperty("user.home")), "td");
		logger.info("Copying folder {} to {}...", tdp, dest);
		Files.walk(tdp).forEach(source -> {
			try {
				Files.copy(source, dest.resolve(tdp.relativize(source)), REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		});
		return dest;
	}
}
