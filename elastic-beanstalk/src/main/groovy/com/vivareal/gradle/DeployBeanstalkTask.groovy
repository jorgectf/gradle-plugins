package com.vivareal.gradle

import org.apache.commons.lang.builder.ToStringBuilder;
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest

class DeployBeanstalkTask extends DefaultTask {
    
    def static final TERMINATED = "Terminated"
    def static final READY = "Ready"
    def static final LAUNCHING = "Launching"
    def static final TERMINATING = "Terminating"

    def propertiesHelper = new ProjectPropertiesHelper(project: getProject())
    def awsApi = new AwsApiManager(propertiesHelper: propertiesHelper)
    def ebManager = new ElasticBeanstalkManager(propertiesHelper: propertiesHelper)

    @TaskAction
    def deployBeanstalk() {
	def finalEnvName = propertiesHelper.getBeanstalkFinalEnvironmentNameOrFail()
	def applicationName = propertiesHelper.getBeanstalkApplicationNameOrFail()
	def versionLabel = propertiesHelper.getVersionLabelOrFail()
	println "Checking whether environment ${finalEnvName} exists ..."
	def search = new DescribeEnvironmentsRequest(environmentNames: [finalEnvName], applicationName:applicationName, includeDeleted: false)
	def result = awsApi.elasticBeanstalkClient.describeEnvironments(search)
	def nextSteps = analiseDescribeEnvironmentsResult(result)
	nextSteps.each { it() }
    }
    
    def analiseDescribeEnvironmentsResult(result) {
	if (result == null || result.environments == null || result.environments.empty) {
	   return createNewEnvironment
	}
	def notTerminatedEnvs = result.environments.findAll { !it.status.equalsIgnoreCase(TERMINATED) }
	if (notTerminatedEnvs.empty) {
	    def terminatedEnvs = result.environments.findAll { it.status.equalsIgnoreCase(TERMINATED) }
	    def ids = terminatedEnvs.collect { it.environmentId }
	    println "Environments recently terminated. Ids: ${ids}"
	    return createNewEnvironment
	}
	def readyEnvs = notTerminatedEnvs.findAll { it.status.equalsIgnoreCase(READY) }
	if (readyEnvs.size() == 1) {
	    return updateEnvironment.curry(readyEnvs[0])
	}
	def launchingEnvs = notTerminatedEnvs.findAll { it.status.equalsIgnoreCase(LAUNCHING) }
	if (!launchingEnvs.empty) {
	    def ids = launchingEnvs.collect { it.environmentId }
	    throw new ElasticbeanstalkPluginException("Environments are lauching. Ids: ${ids}")
	}
	def terminatingEnvs = notTerminatedEnvs.findAll { it.status.equalsIgnoreCase(TERMINATING) }
	if (!terminatingEnvs.empty) {
	    def ids = launchingEnvs.collect { it.environmentId }
	    throw new ElasticbeanstalkPluginException("Environments are terminating. Ids: ${ids}")
	}
	throw new ElasticbeanstalkPluginException("Could not determine environment state.")
    }
    
    def createNewEnvironment = {
	def applicationName = propertiesHelper.getApplicationNameOrFail()
	def finalEnvName = propertiesHelper.getFinalEnvironmentNameOrFail()
	def configTemplate = propertiesHelper.getBeanstalkConfigTemplateNameOrFail()
	def versionLabel = propertiesHelper.getVersionLabelOrFail()
	def optionsSettings = propertiesHelper.getBeanstalkOptionsSettings()
	def tags = propertiesHelper.getBeanstalkTags()
	def createEnvironmentRequest = new CreateEnvironmentRequest(applicationName: applicationName, environmentName:  finalEnvName, versionLabel: versionLabel, templateName: configTemplate)
	createEnvironmentRequest.setCNAMEPrefix(createEnvironmentRequest.getEnvironmentName())
	if (tags) {	    
	    createEnvironmentRequest.setTags(tags)
	}
	if (optionsSettings) {	    
	    createEnvironmentRequest.setOptionSettings(optionsSettings)
	}
	println "Creating new environment ..."
	def createEnvironmentResult = awsApi.elasticBeanstalkClient.createEnvironment(createEnvironmentRequest)
	println "Created environment $createEnvironmentResult"
	ebManager.enableCrossZoneLoadBalancing(finalEnvName)
	println "Added Cross-zone load balancing to environment ${finalEnvName}"
    }
    
    def updateEnvironment = { existingEnvironment ->
	def versionLabel = propertiesHelper.getVersionLabelOrFail()
	def optionsSettings = propertiesHelper.getBeanstalkOptionsSettings()
	def existingEnvironmentName = existingEnvironment.getEnvironmentName()
	def updateEnviromentRequest = new UpdateEnvironmentRequest(environmentName: existingEnvironmentName, versionLabel: versionLabel)
	if (optionsSettings) {
	    updateEnviromentRequest.setOptionSettings(optionsSettings)
	}
	println "Updating environment ${existingEnvironment.environmentId} with application version ${versionLabel} ..."
	def updateEnviromentResult = awsApi.elasticBeanstalkClient.updateEnvironment(updateEnviromentRequest)
	println "Successfully environment ${updateEnviromentResult.environmentId} with application version ${versionLabel}"
    }
    
}