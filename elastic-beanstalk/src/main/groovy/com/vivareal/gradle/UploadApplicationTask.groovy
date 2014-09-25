package com.vivareal.gradle

import org.apache.commons.lang.builder.ToStringBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest
import com.amazonaws.services.elasticbeanstalk.model.S3Location

class UploadApplicationTask extends DefaultTask {

    def propertiesHelper = new ProjectPropertiesHelper(project: getProject())
    def awsApi = new AwsApiManager(propertiesHelper: propertiesHelper)
    def ebManager = new ElasticBeanstalkManager(propertiesHelper: propertiesHelper)

    @TaskAction
    def uploadApplicationAction() {
	def versionLabel = propertiesHelper.getVersionLabel()
	versionLabel = versionLabel ? versionLabel : project.version.toString()
	def applicationName = propertiesHelper.getBeanstalkApplicationNameOrFail()
	if (propertiesHelper.shouldSkipUpload()) {
	    println "Skipping upload ..."
	} else {
	    // Delete existing application
	    if (ebManager.applicationVersionAlreadyExists()) {
		println "Application version already exists. Deletting ..."
		def deleteRequest = new DeleteApplicationVersionRequest(applicationName: applicationName,
		versionLabel: versionLabel, deleteSourceBundle: true)
		awsApi.elasticBeanstalkClient.deleteApplicationVersion(deleteRequest)
	    }
	    def warFile = propertiesHelper.getWarFileOrFail()
	    if (!warFile.exists()) {
		throw new ElasticbeanstalkPluginException("Application artifact does not exist: ${warFile.path}")
	    }
	    String key = URLEncoder.encode("${project.name}-${versionLabel}.war", 'UTF-8')
	    println "Uploading application artifact to S3 ..."
	    def storageLocation = awsApi.elasticBeanstalkClient.createStorageLocation()
	    String bucketName = storageLocation.getS3Bucket()
	    def s3Result = awsApi.s3Client.putObject(bucketName, key, warFile)
	    println "Successfully uploaded artifact to S3.\n\tS3 Content MD5: ${s3Result.contentMd5}"
	    // Register a new application version
	    println "Creating application version ..."
	    def createApplicationVersionRequest = new CreateApplicationVersionRequest(
		    applicationName: applicationName, versionLabel: versionLabel,
		    description: propertiesHelper.getBuildDescription(),
		    autoCreateApplication: true, sourceBundle: new S3Location(bucketName, key)
		    )
	    def createApplicationVersionResult = awsApi.elasticBeanstalkClient.createApplicationVersion(createApplicationVersionRequest)
	    println "Successfully created application version: ${createApplicationVersionResult.applicationVersion.versionLabel}"
	    println "\tS3 Bucket: ${createApplicationVersionResult.applicationVersion.sourceBundle.s3Bucket}"
	    println "\tS3 Key: ${createApplicationVersionResult.applicationVersion.sourceBundle.s3Key}"
	}
    }

}
