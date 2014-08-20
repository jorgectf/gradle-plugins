package com.vivareal.gradle

import java.util.concurrent.TimeUnit

import org.apache.commons.codec.binary.Base64
import org.gradle.api.*
import org.gradle.api.plugins.*

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.ec2.*
import com.amazonaws.services.ec2.model.*

class EC2Plugin implements Plugin<Project> {

    def accessKey
    def secretKey
    def instanceType
    def imageId
    def primarySubnetId
    def secondarySubnetId
    def securityGroupId
    def userData
    def keyName
    def region
    def desiredNumInstance
    def tag

    void apply(Project project) {

	project.task('launchEC2Instance') << {

	    // required parameters passed from jenkins
	    accessKey = project.ext.has('accessKey')?project.ext.accessKey:null
	    secretKey = project.ext.has('secretKey')?project.ext.secretKey:null
	    instanceType = project.ext.has('instanceType')?project.ext.instanceType:null
	    imageId = project.ext.has('imageId')?project.ext.imageId:null
	    primarySubnetId = project.ext.has('primarySubnetId')?project.ext.primarySubnetId:null
	    secondarySubnetId = project.ext.has('secondarySubnetId')?project.ext.secondarySubnetId:null
	    securityGroupId = project.ext.has('securityGroupId')?project.ext.securityGroupId:null
	    userData = project.ext.has('userData')?project.ext.userData:""
	    keyName = project.ext.has('keyName')?project.ext.keyName:null
	    region = project.ext.has('region')?project.ext.region:"sa-east-1";
	    desiredNumInstance = project.ext.has('desiredNumInstance') ? Integer.parseInt(project.ext.desiredNumInstance) : 1;
	    tag = project.ext.has('tag') ? project.ext.tag : null;

	    def credentials
	    if (accessKey && secretKey)
		credentials = new BasicAWSCredentials(accessKey, secretKey)

	    AmazonEC2 ec2 = new AmazonEC2Client(credentials);
	    ec2.setEndpoint("https://ec2." + region + ".amazonaws.com");

	    for(i in 1..desiredNumInstance) {
		def subnetId
		if(i % 2 == 0) {
		    subnetId = primarySubnetId
		} else {
		    subnetId = secondarySubnetId
		}

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
			.withInstanceType("${instanceType}")
			.withImageId("${imageId}")
			.withMinCount(1)
			.withMaxCount(1)
			.withSubnetId("${subnetId}")
			.withSecurityGroupIds("${securityGroupId}")
			.withKeyName("${keyName}")
			.withUserData(Base64.encodeBase64String("${userData}".getBytes()))

		if(securityGroupId) {
		    def groupIds = []
		    groupIds.add(securityGroupId)
		    runInstancesRequest.setSecurityGroupIds(groupIds)
		}

		RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest)
		def instanceId = runInstances.reservation.instances.get(0).instanceId

		println("launchEC2Instance: instanceID = ${instanceId}")

		try{
		    environmentIsReady(ec2,instanceId)
		    CreateTagsRequest createTagsRequest = new CreateTagsRequest();
		    createTagsRequest.withResources(instanceId) //
			    .withTags(new Tag("Name", tag + "-" + i));
		    ec2.createTags(createTagsRequest);

		    println("launchEC2Instance: Instance ${instanceId} with tag ${tag} launched successfully")
		}catch(Exception e){
		    org.codehaus.groovy.runtime.StackTraceUtils.sanitize(e).printStackTrace()
		    throw new org.gradle.api.tasks.StopExecutionException()
		}
	    }

	}
    }

    @groovy.transform.TimedInterrupt(value = 5L, unit = TimeUnit.MINUTES)
    private environmentIsReady(ec2, instanceId) {
	def done = false
	try {
	    while( !done ) {
		println("launchEC2Instance: Checking if instance is in [running] state")

		def request = new DescribeInstanceStatusRequest().withInstanceIds(instanceId)
		def result = ec2.describeInstanceStatus(request)

		// only query first instance (since we only pass one id only one instance should be returned)
		while (result.instanceStatuses.size() < 1) {
		    result = ec2.describeInstanceStatus(request)
		    sleep(5000)
		}

		def status = result.instanceStatuses[0]

		println("launchEC2Instance: Current status is [${status.instanceState.name}]")

		// if instance is not running then sleep for 20 seconds and query again
		if (status.instanceState.name == "running")
		    done = true
		else sleep(20000)
	    }
	}
	catch( e ) {
	    throw e
	}
    }

}

