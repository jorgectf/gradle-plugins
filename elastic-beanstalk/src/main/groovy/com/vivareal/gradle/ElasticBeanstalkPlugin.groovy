package com.vivareal.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest
import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest

class ElasticBeanstalkPlugin implements Plugin<Project> {

    void apply(Project project) {

	def propertiesHelper = new ProjectPropertiesHelper(project: project)
	def ebManager = new ElasticBeanstalkManager(propertiesHelper: propertiesHelper)
	def awsApi = new AwsApiManager(propertiesHelper: propertiesHelper)

	project.task('testBeanstalk', type: TestBeanstalkTask)
	project.task('uploadNewVersion', type: UploadApplicationTask)
	project.task([
	    dependsOn: 'uploadNewVersion',
	    description: "deploys a new version to an existing Elastic Beanstalk environment",
	    type: DeployBeanstalkTask], 'deployBeanstalk')

	project.task([
	    description: "deploys a new version to a new Elastic Beanstalk environment with zero downtime"],'deployBeanstalkZeroDowntime') << {

	    def environmentName = propertiesHelper.getBeanstalkEnvironmentName()
	    def applicationName = propertiesHelper.getBeanstalkApplicationName()
	    def configTemplate = propertiesHelper.getBeanstalkConfigTemplateName()
	    def versionLabel = propertiesHelper.getVersionLabel()
	    //Create new Environment
	    println "Create new environment for new application version"
	    def createEnvironmentRequest = new CreateEnvironmentRequest(applicationName: applicationName, environmentName: environmentName, versionLabel: versionLabel, templateName: configTemplate)
	    createEnvironmentRequest.setCNAMEPrefix(createEnvironmentRequest.getEnvironmentName())
	    createEnvironmentRequest.setOptionSettings(propertiesHelper.getBeanstalkOptionsSettings())
	    def createEnvironmentResult = awsApi.elasticBeanstalkClient.createEnvironment(createEnvironmentRequest)
	    println "Created environment $createEnvironmentResult"

	    if (!createEnvironmentResult.environmentId)
		throw new ElasticbeanstalkPluginException()

	    ebManager.enableCrossZoneLoadBalancing(environmentName)
	    println "Added Cross-zone load balancing to environment ${environmentName}"

	    //If it gets here, environment is ready. Check health next

	    println("Checking that new environment's health is Green before swapping urls")

	    def search = new DescribeEnvironmentsRequest(environmentNames: [environmentName])
	    def result = awsApi.elasticBeanstalkClient.describeEnvironments(search)

	    println(result.environments.health.toString())
	    if (result.environments.health.toString() != "[Green]")
		throw new ElasticbeanstalkPluginException("Environment is not Green, cannot continue")

	    println("Environment's health is Green")

	    // Swap new environment with previous environment
	    // NOTE: Envirnments take too long to be green. Swapping right after deployment is not a good idea.
	    println "Swap environment Url"
	    def swapEnviromentRequest = new SwapEnvironmentCNAMEsRequest(destinationEnvironmentName: propertiesHelper.getPreviousEnvironmentName() , sourceEnvironmentName: environmentName)

	    try{
		awsApi.elasticBeanstalkClient.swapEnvironmentCNAMEs(swapEnviromentRequest)
		println "Swaped CNAMES Successfully"

	    }catch(Exception e){
		println("En error ocurred while swapping environment CNAMEs " + e)
	    }

	}

	project.task([description: "deploys an existing version to an existing Elastic Beanstalk environment"],'deployBeanstalkVersion') << {
	    def previousEnvironmentName = propertiesHelper.getBeanstalkPreviousEnvironmentName()
	    def applicationName = propertiesHelper.getBeanstalkApplicationName()
	    def versionLabel = propertiesHelper.getVersionLabel()
	    try {
		//Deploy the existing version to the new environment
		println "Update environment with existing application version ${versionLabel}"
		def updateEnviromentRequest = new UpdateEnvironmentRequest(environmentName: previousEnvironmentName, versionLabel: versionLabel)
		updateEnviromentRequest.setOptionSettings(propertiesHelper.getBeanstalkOptionsSettings())
		def updateEnviromentResult = awsApi.elasticBeanstalkClient.updateEnvironment(updateEnviromentRequest)
		println "Updated environment $updateEnviromentResult"
	    } catch (Exception ipe) {
		def configTemplate = propertiesHelper.getBeanstalkConfigTemplateName()
		println("Environment doesn't exist. creating environment")
		def createEnvironmentRequest = new CreateEnvironmentRequest(applicationName: applicationName, environmentName:  previousEnvironmentName, versionLabel: versionLabel, templateName: configTemplate)
		createEnvironmentRequest.setCNAMEPrefix(createEnvironmentRequest.getEnvironmentName())
		createEnvironmentRequest.setOptionSettings(propertiesHelper.getBeanstalkOptionsSettings())
		def createEnvironmentResult = awsApi.elasticBeanstalkClient.createEnvironment(createEnvironmentRequest)
		println "Created environment $createEnvironmentResult"
		ebManager.enableCrossZoneLoadBalancing(previousEnvironmentName)
		println "Added Cross-zone load balancing to environment ${previousEnvironmentName}"
	    }

	}

	project.task([description: "deploys an existing version to an existing Elastic Beanstalk environment with no downtime"],'deployBeanstalkVersionZeroDowntime') << {
	    def previousEnvironmentName = propertiesHelper.getBeanstalkPreviousEnvironmentName()
	    def versionLabel = propertiesHelper.getVersionLabel()
	    try{
		//Deploy the existing version to the new environment
		println "Update environment with existing application version ${versionLabel}"
		// increase desired capacity
		DescribeEnvironmentResourcesRequest request = new DescribeEnvironmentResourcesRequest(environmentName: previousEnvironmentName)
		DescribeEnvironmentResourcesResult result = awsApi.elasticBeanstalkClient.describeEnvironmentResources(request)
		def autoScalingGroups = result.environmentResources.autoScalingGroups
		def autoScalingGroupName = autoScalingGroups[0].name

		DescribeAutoScalingGroupsRequest describeAsRequest = new  DescribeAutoScalingGroupsRequest(autoScalingGroupNames: [autoScalingGroupName])
		DescribeAutoScalingGroupsResult describeAsResponse = awsApi.autoscalingClient.describeAutoScalingGroups(describeAsRequest)
		AutoScalingGroup asGroup = describeAsResponse.autoScalingGroups[0]

		def newDesiredCapacity = (asGroup.desiredCapacity + 1 > asGroup.maxSize) ? asGroup.maxSize : asGroup.desiredCapacity + 1

		UpdateAutoScalingGroupRequest updateAsRequest =
			new UpdateAutoScalingGroupRequest(autoScalingGroupName: autoScalingGroupName, desiredCapacity: newDesiredCapacity)
		awsApi.autoscalingClient.updateAutoScalingGroup(updateAsRequest)

		println "Desired capacity of auto scaling group increased"

		ebManager.allInstancesHealthy(autoScalingGroupName, newDesiredCapacity)

		def updateEnviromentRequest = new UpdateEnvironmentRequest(environmentName:  previousEnvironmentName, versionLabel: versionLabel)
		def updateEnviromentResult = awsApi.elasticBeanstalkClient.updateEnvironment(updateEnviromentRequest)
		println "Updated environment $updateEnviromentResult"
	    }catch(Exception ipe){
		println ipe
		println("Environment doesn't exist")
	    }

	}

    }

}

