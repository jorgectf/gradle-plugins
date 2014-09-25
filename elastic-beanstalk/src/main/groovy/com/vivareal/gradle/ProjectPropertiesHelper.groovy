package com.vivareal.gradle

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting
import com.amazonaws.services.elasticbeanstalk.model.Tag;

class ProjectPropertiesHelper {

    static final def APP_ENV_NAMESPACE = "aws:elasticbeanstalk:application:environment"
    static final def JVM_OPTS_NAMESPACE = "aws:elasticbeanstalk:container:tomcat:jvmoptions"
    static final def BEANSTALK_ENV_PROPERTY_PREFIX = "beanstalk.env."
    static final def BEANSTALK_JVM_PARAMETER_PREFIX = "beanstalk.jvm."
    static final def BEANSTALK_TAG_PARAMETER_PREFIX = "beanstalk.tag."
    static final def JAVA_AGENT = "javaagent"
    static final def JVM_OPTIONS = "JVM Options"

    static final def APPLICATION_NAME = 'applicationName'
    static final def CONFIG_TEMPLATE = 'configTemplate'
    static final def WAR_FILE_PATH = 'warFilePath'
    static final def NEW_ENVIRONMENT_NAME = 'newEnvironmentName'
    static final def VERSION_LABEL = 'versionLabel'
    static final def PREVIOUS_ENVIRONMENT_NAME = 'currentEnvironment'
    static final def SKIP_UPLOAD = 'skipUpload'

    static final def AWS_ACCESS_KEY = "accessKey"
    static final def AWS_SECRET_KEY = "secretKey"
    static final def AWS_REGION = "awsRegion"

    def project

    def appNameValidator = new ApplicationNameValidator()

    def getBeanstalkPreviousEnvironmentName() {
	project.ext.has(PREVIOUS_ENVIRONMENT_NAME) ? project.ext[PREVIOUS_ENVIRONMENT_NAME] : null
    }

    def getBeanstalkApplicationName() {
	project.ext.has(APPLICATION_NAME) ? project.ext[APPLICATION_NAME] : null
    }
    
    def getBeanstalkApplicationNameOrFail() {
	def appName = getBeanstalkApplicationName()
	if (appName == null) {
	    throw new ElasticbeanstalkPluginException("Missing Beanstalk application name")
	}
	appName
    }

    def getBeanstalkConfigTemplateName() {
	project.ext.has(CONFIG_TEMPLATE) ? project.ext[CONFIG_TEMPLATE] : null
    }

    def getWarFilePath() {
	project.ext.has(WAR_FILE_PATH) ? project.ext[WAR_FILE_PATH] : null
    }
    
    def getWarFileOrFail() {
    	def warFile = getWarFile()
	if (warFile == null) {
	    throw new ElasticbeanstalkPluginException("Missing WAR file")
	}
	return warFile
    }
    
    def getWarFile() {
	def warFilePath = getWarFilePath()
	// If no war file path was specified, look in /build/libs
	if (!warFilePath) { 
	    def versionLabel = getVersionLabel()
	    if (!versionLabel) {
		return null
	    }
	    if (!project.buildDir) {
		return null
	    }
	    if (!project.name) {
		return null
	    }
	    if (!versionLabel) {
		return null
	    }
	    return new File(project.buildDir, "libs/${project.name}-${versionLabel}.war")
	}
	new File("${warFilePath}")
    }

    def getBeanstalkEnvironmentName() {
	project.ext.has(NEW_ENVIRONMENT_NAME) ? project.ext[NEW_ENVIRONMENT_NAME] : null
    }

    def getVersionLabel() {
	project.ext.has(VERSION_LABEL) ? project.ext[VERSION_LABEL] : null
    }
    
    def getVersionLabelOrFail() {
	def versionLabel = getVersionLabel()
	if (versionLabel == null) {
	    throw new ElasticbeanstalkPluginException("Missing version label")
	}
	versionLabel
    }

    def getBeanstalkFinalEnvironmentName() {
	def previousName = getBeanstalkPreviousEnvironmentName()
	previousName ? previousName : getBeanstalkEnvironmentName()
    }

    def getBeanstalkFinalEnvironmentNameOrFail() {
	def finalName = getBeanstalkFinalEnvironmentName()
	if (finalName == null) {
	    throw new ElasticbeanstalkPluginException("Could not determine the target environment name")
	}
	finalName
    }

    def getBeanstalkOptionsSettings() {
	def envOptions = getBeanstalkEnvironmentOptions()
	def jvmOptions = getBeanstalkJvmOptions()
	def jvmAgent = getBeanstalkJvmAgentOptions()
	envOptions.addAll(jvmOptions)
	envOptions.addAll(jvmAgent)
	envOptions
    }

    def getBeanstalkEnvironmentOptions() {
	def selectedProperties = project.ext.properties.findAll {
	    it.key.startsWith(BEANSTALK_ENV_PROPERTY_PREFIX)
	}
	def environmentOptions = []
	selectedProperties.each { key, value ->
	    def realKey = key.substring(BEANSTALK_ENV_PROPERTY_PREFIX.length())
	    def config = new ConfigurationOptionSetting()
	    config.setNamespace(APP_ENV_NAMESPACE)
	    config.setOptionName(realKey)
	    config.setValue(value)
	    environmentOptions << config
	}
	environmentOptions
    }

    def getBeanstalkJvmOptions() {
	def selectedProperties = project.ext.properties.findAll {
	    it.key.startsWith(BEANSTALK_JVM_PARAMETER_PREFIX) && !it.key.endsWith(JAVA_AGENT)
	}
	def jvmOptions = []
	selectedProperties.each { key, value ->
	    def realKey = key.substring(BEANSTALK_JVM_PARAMETER_PREFIX.length())
	    def config = new ConfigurationOptionSetting()
	    config.setNamespace(JVM_OPTS_NAMESPACE)
	    config.setOptionName(realKey)
	    config.setValue(value)
	    jvmOptions << config
	}
	jvmOptions
    }

    def getBeanstalkJvmAgentOptions() {
	def selectedProperties = project.ext.properties.findAll {
	    it.key.startsWith(BEANSTALK_JVM_PARAMETER_PREFIX) && it.key.endsWith(JAVA_AGENT)
	}
	def jvmOptions = []
	selectedProperties.each { key, value ->
	    def realKey = key.substring(BEANSTALK_JVM_PARAMETER_PREFIX.length())
	    def config = new ConfigurationOptionSetting()
	    config.setNamespace(JVM_OPTS_NAMESPACE)
	    config.setOptionName(JVM_OPTIONS)
	    config.setValue("-${JAVA_AGENT}:${value}")
	    jvmOptions << config
	}
	jvmOptions
    }
    
    def getAwsCredentials() {
	if (project.ext.has(AWS_ACCESS_KEY) && project.ext.has(AWS_SECRET_KEY)) {
	    return new BasicAWSCredentials(project.ext[AWS_ACCESS_KEY], project.ext[AWS_SECRET_KEY])
	}
    	null
    }
    
    def getAwsCredentialsForLog() {
	def credentials = getAwsCredentials()
	if (credentials) {
	    def pieceOfAccessKey = credentials.AWSAccessKeyId.substring(0, 4)
	    def pieceOfSecretKey = credentials.AWSSecretKey.substring(0, 4)
	    return "Access Key: ${pieceOfAccessKey}[..], Secret Key: ${pieceOfSecretKey}[..]"
	}
	return null
    }

    def getAwsCredentialsOrFail() {
	def credentials = getAwsCredentials()
	if (credentials == null) {
	    throw new ElasticbeanstalkPluginException("Missing AWS credentials")
	}
	credentials
    }

    def getAwsRegionName() {
	if (project.ext.has(AWS_REGION)) {
	    def regionName = project.ext[AWS_REGION]
	    if (regionName != null) {
		regionName = regionName.trim()
		if (!regionName.isEmpty()) {
		    try {
			return Regions.valueOf(regionName)
		    } catch (IllegalArgumentException) {
			throw new ElasticbeanstalkPluginException("Invalid AWS region. Valid options are: " + Regions.values())
		    }
		}
	    }
	}
	Regions.SA_EAST_1
    }

    def getBeanstalkTags() {
	def selectedProperties = project.ext.properties.findAll {
	    it.key.equals(BEANSTALK_TAGS_PROPERTY)
	}
	def tags = []
	selectedProperties.each { key, value ->
	    def realKey = key.substring(BEANSTALK_TAG_PARAMETER_PREFIX.length())
	    Tag beanstalkTag = new Tag()
	    beanstalkTag.setKey(realKey)
	    beanstalkTag.setValue(value)
	    tags.addAll(beanstalkTag)
	}
	return tags
    }
    
    def getBuildDescription() {
	def applicationName = getBeanstalkApplicationName()
	if (applicationName == null) {
	    return null
	}
	"${applicationName} via 'gradle' build on ${new Date().format('yyyy-MM-dd')}"
    }
    
    def shouldSkipUpload() {
	project.ext.has(SKIP_UPLOAD) && project.ext[SKIP_UPLOAD]
    }
    
}
