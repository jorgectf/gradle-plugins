package com.vivareal.gradle

import java.util.concurrent.TimeUnit

import org.gradle.api.*

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.ec2.*
import com.amazonaws.services.ec2.model.*

class EC2Plugin implements Plugin<Project> {

    void apply(Project project) {

        project.task('launchEC2Instance') << {

            def terminationBehavior = project.ext.has("terminationBehavior") ? project.ext.terminationBehavior : "stop"

            def credentials
            if (project.ext.accessKey && project.ext.secretKey)
                credentials = new BasicAWSCredentials(project.ext.accessKey, project.ext.secretKey)

            AmazonEC2 ec2 = new AmazonEC2Client(credentials);
            ec2.setEndpoint("https://ec2." + project.ext.region + ".amazonaws.com");

            def instanceIds = []
            def ec2InstanceStarter

            println "desired number of instances:" + project.ext.desiredNumInstance
            try {
                for (i in 1..project.ext.desiredNumInstance) {
                    def subnetId
                    if (i % 2 == 0) {
                        subnetId = project.ext.primarySubnetId
                    } else {
                        subnetId = project.ext.secondarySubnetId
                    }

                    RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                            .withInstanceType("${project.ext.instanceType}")
                            .withImageId("${project.ext.imageId}")
                            .withMinCount(1)
                            .withMaxCount(1)
                            .withSubnetId("${subnetId}")
                            .withSecurityGroupIds("${project.ext.securityGroupId}".split(","))
                            .withKeyName("${project.ext.keyName}")
                            .withUserData(org.apache.commons.codec.binary.Base64.encodeBase64String("${project.ext.userData}".getBytes()))
                            .withInstanceInitiatedShutdownBehavior("${terminationBehavior}")

                    ec2InstanceStarter = new EC2InstanceStarter(runInstancesRequest: runInstancesRequest, ec2: ec2)
                    instanceIds.add(ec2InstanceStarter.runInstance(project.ext.tag, i))
                }
            } catch (Exception e) {
                instanceIds.add(ec2InstanceStarter.instanceId)
                ec2.terminateInstances(new TerminateInstancesRequest(instanceIds))
                org.codehaus.groovy.runtime.StackTraceUtils.sanitize(e).printStackTrace()
                throw new org.gradle.api.tasks.StopExecutionException()
            }
        }
    }

    class EC2InstanceStarter {

        def runInstancesRequest
        def ec2
        def instanceId

        def runInstance(tag, sequence) throws Exception {
            RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest)
            instanceId = runInstances.reservation.instances.get(0).instanceId

            println("launchEC2Instance: instanceID = ${instanceId}")

            environmentIsReady(instanceId)
            CreateTagsRequest createTagsRequest = new CreateTagsRequest();
            createTagsRequest.withResources(instanceId) //
                    .withTags(new Tag("Name", tag + "-" + sequence));
            ec2.createTags(createTagsRequest);

            println("launchEC2Instance: Instance ${instanceId} with tag ${tag} launched successfully")
        }

        @groovy.transform.TimedInterrupt(value = 5L, unit = TimeUnit.MINUTES, applyToAllMembers = false, checkOnMethodStart = true)
        private environmentIsReady(instanceId) {
            def done = false
            try {
                while (!done) {
                    println("launchEC2Instance: Checking if instance is in [passed] state")

                    def request = new DescribeInstanceStatusRequest().withInstanceIds(instanceId)
                    def result = ec2.describeInstanceStatus(request)

                    // only query first instance (since we only pass one id only one instance should be returned)
                    while (result.instanceStatuses.size() < 1) {
                        result = ec2.describeInstanceStatus(request)
                        sleep(5000)
                    }

                    def status = result.instanceStatuses[0].instanceStatus.details[0].status

                    println("launchEC2Instance: Current status is [${status}]")

                    // if instance is not running then sleep for 20 seconds and query again
                    if (status == "passed")
                        done = true
                    else sleep(20000)
                }
            }
            catch (e) {
                throw e
            }
        }

    }
}

