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

import static org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProjectTest.scheduleAndFindBranchProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.biouno.unochoice.model.GroovyScript;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;


public class TestDynamicReferenceParameterBindings {

    private final String SCRIPT = "return ['D', 'C', 'B', 'A']";
    private final String FALLBACK_SCRIPT = "";
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

    @Before
    public void setUp() throws Exception {
        ScriptApproval.get().preapprove(SCRIPT, GroovyLanguage.get());
        ScriptApproval.get().preapprove(FALLBACK_SCRIPT, GroovyLanguage.get());
    }

    @Test
    public void testConstructor() throws Exception {
    	sampleRepo.init();
    	sampleRepo.write("Jenkinsfile", "pipeline {\n" + 
    			"  agent { label 'master' }\n" + 
    			"  stages {\n" + 
    			"    stage('Parameters'){\n" + 
    			"      steps {\n" + 
    			"        script {\n" + 
    			"        String errorValue='***ERROR***'\n" + 
    			"  String ip='''\n" + 
    			"String value=\"Cannot retrieve anything\"\n" + 
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
    			"return '<input name=\"value\" value=\"'+value+'\" class=\"setting-input\" type=\"text\">'\n" + 
    			"'''\n" + 
    			"  String get_email_addresses='''\n" + 
    			"String ipParam='';\n" + 
    			"try {ipParam=INTERESTED_PARTIES} catch(Exception e){};\n" + 
    			"String[] newIp=ipParam.split(' ')\n" + 
    			"return '<input type=\"button\" value=\"Validate\"><input type=\"hidden\" name=\"value\" class=\"setting-input\" value=\"'+newIp.join(\",\")+'\">'\n" + 
    			"  '''\n" + 
    			"          properties([\n" + 
    			"            parameters([\n" + 
    			"              [$class: 'DynamicReferenceParameter', choiceType: 'ET_FORMATTED_HTML',\n" + 
    			"                description: 'IP description', name: 'INTERESTED_PARTIES',\n" + 
    			"                omitValueField: true, script: [\n" + 
    			"                  $class: 'GroovyScript', fallbackScript: [\n" + 
    			"                    classpath: [], sandbox: true, script: 'return \\'<input name=\"value\" value=\"'+errorValue+'\" class=\"setting-input\" type=\"text\">\\''\n" + 
    			"                  ], script: [\n" + 
    			"                    classpath: [], sandbox: true, script: ip\n" + 
    			"                  ]\n" + 
    			"                ]\n" + 
    			"              ],\n" + 
    			"              [$class: 'DynamicReferenceParameter', choiceType: 'ET_FORMATTED_HTML',\n" + 
    			"                description: '',\n" + 
    			"                name: 'User Validation', referencedParameters: 'INTERESTED_PARTIES', omitValueField: true,\n" + 
    			"                script: [\n" + 
    			"                  $class: 'GroovyScript', fallbackScript: [\n" + 
    			"                    classpath: [], sandbox: true, script: 'return [\"'+errorValue+'\"]'\n" + 
    			"                  ],\n" + 
    			"                  script: [\n" + 
    			"                    classpath: [], sandbox: true, script: get_email_addresses\n" + 
    			"                  ]\n" + 
    			"                ]\n" + 
    			"              ],\n" + 
    			"            ])\n" + 
    			"          ])\n" + 
    			"        }\n" + 
    			"      }\n" + 
    			"    }\n" + 
    			"    stage('Output') {\n" + 
    			"      steps {\n" + 
    			"        script {\n" + 
    			"          echo params.INTERESTED_PARTIES\n" + 
    			"        }\n" + 
    			"      }\n" + 
    			"    }\n" + 
    			"  }\n" + 
    			"}\n");
    	sampleRepo.git("add","Jenkinsfile");
    	sampleRepo.git("commit", "--all", "--message=flow");
    	WorkflowMultiBranchProject mp = jenkins.createProject(WorkflowMultiBranchProject.class, "p");
    	mp.getSourcesList().add(new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false)));
    	WorkflowJob p = scheduleAndFindBranchProject(mp, "master");
    	assertEquals(1, mp.getItems().size());
    	jenkins.waitUntilNoActivity();
    	WorkflowRun b1 = p.getLastBuild();
    	assertEquals(1, b1.getNumber());
    	jenkins.assertLogContains("it worked", b1);
    }
    public static void main(String[] args) {
		System.out.println(ip);
	}

}
