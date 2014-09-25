package com.vivareal.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class TestBeanstalkTask extends DefaultTask {

    @TaskAction
    def printInfoAction() {
	def propertiesHelper = new ProjectPropertiesHelper(project: getProject())
	def applicationName = propertiesHelper.getBeanstalkApplicationName()
	def environmentName = propertiesHelper.getBeanstalkEnvironmentName()
	def previousEnvironmentName = propertiesHelper.getBeanstalkPreviousEnvironmentName()
	def awsCredentials = propertiesHelper.getAwsCredentialsForLog()
	def awsRegionName = propertiesHelper.getAwsRegionName()
	def configTemplate = propertiesHelper.getBeanstalkConfigTemplateName()
	def versionLabel = propertiesHelper.getVersionLabel()
	def project = getProject()
	println "AWS region: ${awsRegionName}"
	println "AWS credentials: ${awsCredentials?awsCredentials:'MISSING'}"
	println "Elasticbeanstalk application name: ${applicationName?applicationName:'MISSING'}"
	println "Elasticbeanstalk new environment name: ${environmentName?environmentName:'MISSING'}"
	println "Elasticbeanstalk current environment: ${previousEnvironmentName?previousEnvironmentName:'MISSING'}"
	println "Elasticbeanstalk config template: ${configTemplate?configTemplate:'MISSING'}"
	println "Project's version: ${project.version}"
	println "Root project version: ${project.rootProject.version}"
	println "Version label: ${versionLabel?versionLabel:'MISSING'}"
    }
}
