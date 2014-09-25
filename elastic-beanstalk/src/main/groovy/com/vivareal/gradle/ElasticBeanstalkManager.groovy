package com.vivareal.gradle

import groovy.transform.TimedInterrupt

import java.util.concurrent.TimeUnit

import org.codehaus.groovy.runtime.StackTraceUtils

import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult
import com.amazonaws.services.ec2.model.InstanceStatus
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest
import com.amazonaws.services.elasticloadbalancing.model.CrossZoneLoadBalancing
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest

class ElasticBeanstalkManager {

    static final def INTERVAL_BETWEEN_ENVIRONMENT_HEALTH_CHECKS = 20000
    static final def INTERVAL_BETWEEN_AUTOSCALE_CHECKS = 20000
    static final def SLEEP_AFTER_SCALING = 60000
    
    def propertiesHelper

    def applicationVersionIsDeployed = {
	def search = createDescribeEnvironmentsRequest()
	def result = awsApi.elasticBeanstalkClient.describeEnvironments(search)
	!result.environments.empty
    }

    def applicationVersionAlreadyExists = {
	def search = createDescribeApplicationVersionsRequest()
	def result = awsApi.elasticBeanstalkClient.describeApplicationVersions(search)
	if (result == null) return false
	if (result.applicationVersions == null || result.applicationVersions.empty) return false
	def versionFound = result.applicationVersions.find {
	    search.getVersionLabels().contains(it.versionLabel) && it.applicationName.equals(search.applicationName)
	}
	versionFound != null
    }

    def createDescribeApplicationVersionsRequest = {
	def applicationName = propertiesHelper.getBeanstalkApplicationName()
	def versionLabel = propertiesHelper.getVersionLabel()
	new DescribeApplicationVersionsRequest(applicationName: applicationName, versionLabels: [versionLabel])
    }

    def createDescribeEnvironmentsRequest = {
	def applicationName = propertiesHelper.getBeanstalkApplicationName()
	def versionLabel = propertiesHelper.getVersionLabel()
	new DescribeEnvironmentsRequest(applicationName: applicationName, versionLabel: versionLabel)
    }
    
    def enableCrossZoneLoadBalancing(environmentName) {
	// assume the beanstalk environment has only one load balancer since it is default behavior
	try {
	    environmentIsReady(environmentName)
	} catch (Exception e){
	    StackTraceUtils.sanitize(e).printStackTrace()
	    throw new ElasticbeanstalkPluginException()
	}

	def request = new DescribeEnvironmentResourcesRequest(environmentName: environmentName)
	def response = awsApi.elasticBeanstalkClient.describeEnvironmentResources(request)
	def loadBalancers = response.environmentResources.loadBalancers.name
	def loadBalancerName = loadBalancers[0]

	def attributes = new LoadBalancerAttributes()
	attributes.crossZoneLoadBalancing = new CrossZoneLoadBalancing(enabled: true)

	request = new ModifyLoadBalancerAttributesRequest(loadBalancerName: loadBalancerName, loadBalancerAttributes: attributes)
	response = awsApi.loadBalancingClient.modifyLoadBalancerAttributes(request)

	response
    }

    @TimedInterrupt(value = 20L, unit = TimeUnit.MINUTES, applyToAllClasses=false)
    def environmentIsReady(environmentName) {
	def done = false
	while ( !done ) {
	    println("Checking if new environment is ready/green")
	    def search = new DescribeEnvironmentsRequest(environmentNames: environmentName)
	    def result = awsApi.elasticBeanstalkClient.describeEnvironments(search)
	    println(result.environments.status.toString())
	    if (result.environments.status.toString() == "[Ready]") {
		done = true
	    }
	    sleep(INTERVAL_BETWEEN_ENVIRONMENT_HEALTH_CHECKS)
	}
    }

    @TimedInterrupt(value = 5L, unit = TimeUnit.MINUTES, applyToAllClasses=false)
    def allInstancesHealthy(autoScalingGroupName, desiredNumberOfInstances) {
	def done = false
	while(!done) {
	    done = true
	    println("Checking if all instances in autoscaling group [" + autoScalingGroupName + "] are ready")

	    DescribeAutoScalingGroupsRequest asRequest = new DescribeAutoScalingGroupsRequest(autoScalingGroupNames: [autoScalingGroupName])
	    DescribeAutoScalingGroupsResult asResult = awsApi.autoscalingClient.describeAutoScalingGroups(asRequest)

	    List<Instance> instances = asResult.autoScalingGroups.get(0).instances
	    def instanceIds = instances*.instanceId

	    DescribeInstanceStatusRequest statusRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds)
	    DescribeInstanceStatusResult statusResult = awsApi.ec2Client.describeInstanceStatus(statusRequest)
	    List<InstanceStatus> instanceStatuses = statusResult.instanceStatuses

	    if(instanceStatuses.size() >= desiredNumberOfInstances) {
		instanceStatuses.eachWithIndex() { obj, i ->
		    println ">> instance " + instanceIds[i] + " status is " + obj.instanceStatus.details[0].status
		    if(obj.instanceStatus.details[0].status != "passed") {
			done = false
		    }
		}
	    } else {
		println ">> not enough instances running"
		done = false
	    }
	    sleep(INTERVAL_BETWEEN_AUTOSCALE_CHECKS)
	}
	println ">> waiting for " + SLEEP_AFTER_SCALING + " ms after scaling activity and new instance passing status checks"
	sleep(SLEEP_AFTER_SCALING)
    }
    
    def getAwsApi() {
	if (awsApiManagerSingleInstance == null) {
	    def apiManager = new AwsApiManager(propertiesHelper: propertiesHelper)
	    awsApiManagerSingleInstance = apiManager
	}
	awsApiManagerSingleInstance
    }
    
    def static awsApiManagerSingleInstance = null


}
