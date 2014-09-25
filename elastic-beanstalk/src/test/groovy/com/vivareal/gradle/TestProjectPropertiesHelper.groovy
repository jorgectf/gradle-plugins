package com.vivareal.gradle;

import static org.junit.Assert.*

import org.gradle.api.tasks.StopExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.Regions

class TestProjectPropertiesHelper {
    
    @Test
    void testApplicationName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["applicationName"] = "TestApplication"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("TestApplication", instance.getBeanstalkApplicationName())
    }
    
    @Test(expected=StopExecutionException)
    void testBadApplicationName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["applicationName"] = "Test Application"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	instance.getBeanstalkApplicationName()
    }
    
    @Test
    void testConfigTemplateName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["configTemplate"] = "VR-TEST-TEMPLATE"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("VR-TEST-TEMPLATE", instance.getBeanstalkConfigTemplateName())
    }
    
    @Test
    void testWarFilePath() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["warFilePath"] = "/xyz/test.war"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("/xyz/test.war", instance.getWarFilePath())
    }
    
    @Test
    void testEnvironmentName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["newEnvironmentName"] = "qa-some-env"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("qa-some-env", instance.getBeanstalkEnvironmentName())
    }
    
    @Test
    void testFinalEnvironmentNameWithNewName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["newEnvironmentName"] = "qa-some-env"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("qa-some-env", instance.getBeanstalkFinalEnvironmentName())
    }
    
    @Test
    void testFinalEnvironmentNameWithPreviousName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["currentEnvironment"] = "old-env-name"
	fakeProject.ext["newEnvironmentName"] = "new-env-name"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("old-env-name", instance.getBeanstalkFinalEnvironmentName())
    }
    
    @Test
    void testPreviousEnvironmentName() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["currentEnvironment"] = "qa-some-env"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("qa-some-env", instance.getBeanstalkPreviousEnvironmentName())
    }
    
    @Test
    void testVersionLabel() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["versionLabel"] = "1.0"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	assertEquals("1.0", instance.getVersionLabel())
    }
    
    @Test
    void testXmxParameter() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["beanstalk.jvm.Xmx"] = "250m"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def optionsSettings = instance.getBeanstalkOptionsSettings()
	assertEquals(1, optionsSettings.size())
	def option = optionsSettings[0]
	assertEquals("aws:elasticbeanstalk:container:tomcat:jvmoptions", option.namespace)
	assertEquals("Xmx", option.optionName)
	assertEquals("250m", option.value)
    }
    
    @Test
    void testPermSizeParameter() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["beanstalk.jvm.XX:MaxPermSize"] = "250m"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def optionsSettings = instance.getBeanstalkOptionsSettings()
	assertEquals(1, optionsSettings.size())
	def option = optionsSettings[0]
	assertEquals("aws:elasticbeanstalk:container:tomcat:jvmoptions", option.namespace)
	assertEquals("XX:MaxPermSize", option.optionName)
	assertEquals("250m", option.value)
    }
    
    @Test
    // TODO We are not very sure if this is the right way to specify an agent
    // We have to confirm this by 
    void testAgentParameter() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["beanstalk.jvm.javaagent"] = "/usr/share/tomcat7/webapps/ROOT/WEB-INF/lib/newrelic-agent-3.7.0.jar"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def optionsSettings = instance.getBeanstalkOptionsSettings()
	assertEquals(1, optionsSettings.size())
	def option = optionsSettings[0]
	assertEquals("aws:elasticbeanstalk:container:tomcat:jvmoptions", option.namespace)
	assertEquals("JVM Options", option.optionName)
	assertEquals("-javaagent:/usr/share/tomcat7/webapps/ROOT/WEB-INF/lib/newrelic-agent-3.7.0.jar", option.value)
    }
    
    @Test
    void testAwsCredentialsOrFail() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["accessKey"] = "ABCDEFG"
	fakeProject.ext["secretKey"] = "A1B2C3D4E5F6G7"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def credentials = instance.getAwsCredentialsOrFail()
    	assertTrue(credentials instanceof AWSCredentials)
    }
    
    @Test(expected=StopExecutionException)
    void testMissingAwsCredentials() {
	def fakeProject = ProjectBuilder.builder().build()
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	instance.getAwsCredentialsOrFail()
    }
    
    @Test
    void testDefaultAwsRegion() {
	def fakeProject = ProjectBuilder.builder().build()
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def result = instance.getAwsRegionName()
	assertEquals(Regions.SA_EAST_1, result)
    }
    
    @Test
    void testSpecificAwsRegion() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["awsRegion"] = "US_EAST_1"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def result = instance.getAwsRegionName()
	assertEquals(Regions.US_EAST_1, result)
    }
    
    @Test(expected=StopExecutionException)
    void testInvalidAwsRegion() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["awsRegion"] = "XXXXX"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def result = instance.getAwsRegionName()
	assertEquals(Regions.US_EAST_1, result)
    }
    
    @Test
    void testTags() {
	def fakeProject = ProjectBuilder.builder().build()
	fakeProject.ext["beanstalk.env.tags"] = "publishers,listings"
	def instance = new ProjectPropertiesHelper(project: fakeProject)
	def tags = instance.getBeanstalkTags()
	assertTrue(tags.contains("publishers"))
	assertTrue(tags.contains("listings"))
    }

}
