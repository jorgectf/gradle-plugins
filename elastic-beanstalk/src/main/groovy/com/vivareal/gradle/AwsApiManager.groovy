package com.vivareal.gradle

import com.amazonaws.regions.Region
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.s3.AmazonS3Client

class AwsApiManager {

    def propertiesHelper

    def getAutoscalingClient() {
	if (autoscalingClientSingleInstance == null) {
	    def region = getAwsRegion()
	    def client = new AmazonAutoScalingClient(propertiesHelper.getAwsRegionName())
	    client.setRegion(region)
	    autoscalingClientSingleInstance = client
	}
	autoscalingClientSingleInstance
    }

    def getEc2Client() {
	if (ec2ClientSingleInstance == null) {
	    def region = getAwsRegion()
	    def client = new AmazonEC2Client(propertiesHelper.getAwsRegionName())
	    client.setRegion(region)
	    ec2ClientSingleInstance = client
	}
	ec2ClientSingleInstance
    }

    def getElasticBeanstalkClient() {
	if (elasticBeanstalkClientSingleInstance == null) {
	    def region = getAwsRegion()
	    def client = new AWSElasticBeanstalkClient(propertiesHelper.getAwsCredentials())
	    client.setRegion(region)
	    elasticBeanstalkClientSingleInstance = client
	}
	elasticBeanstalkClientSingleInstance
    }
    
    def getS3Client() {
	if (s3ClientSingleInstance == null) {
	    def region = getAwsRegion()
	    def client = new AmazonS3Client(propertiesHelper.getAwsCredentials())
	    client.setRegion(region)
	    s3ClientSingleInstance = client
	}
	s3ClientSingleInstance
    }
    
    def elasticLoadBalancerClient() {
	if (elasticLoadBalancerClientSingleInstance == null) {
	    def region = getAwsRegion()
	    def client = new AmazonElasticLoadBalancingClient(propertiesHelper.getAwsCredentials())
	    client.setRegion(region)
	    elasticLoadBalancerClientSingleInstance = client
	}
	elasticLoadBalancerClientSingleInstance
    }

    def getAwsRegion() {
	if (awsRegionSingleInstance == null) {
	    // The getRegion operation will perform live requests against AWS
	    def region = Region.getRegion(propertiesHelper.getAwsRegionName())
	    if (region == null) {
		throw new ElasticbeanstalkPluginException("Failed to obtain AWS region")
	    }
	    awsRegionSingleInstance = region
	}
	return awsRegionSingleInstance
    }

    def static awsRegionSingleInstance = null
    def static autoscalingClientSingleInstance = null
    def static elasticBeanstalkClientSingleInstance = null
    def static elasticLoadBalancerClientSingleInstance = null
    def static ec2ClientSingleInstance = null
    def static s3ClientSingleInstance = null

}
