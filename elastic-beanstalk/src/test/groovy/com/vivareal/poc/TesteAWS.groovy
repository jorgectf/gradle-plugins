package com.vivareal.poc

import org.gradle.testfixtures.ProjectBuilder

import com.vivareal.gradle.ElasticBeanstalkManager
import com.vivareal.gradle.ProjectPropertiesHelper

class TesteAWS {

    static main(args) {
	def project = ProjectBuilder.builder().build()
	project.ext["accessKey"] = "02XFKC6QM39PM8HPV082"
	project.ext["secretKey"] = "fZ/6R/5vn6UWpIhnfFqMpytXZ0Dhht+wMs+Zskt8"
	project.ext["versionLabel"] = "1.0.201408221923-d502f62"
	project.ext["applicationName"] = "VivaRealPublishersAdmin"

	def elasticBeanstalkManager = new ElasticBeanstalkManager(propertiesHelper: new ProjectPropertiesHelper(project: project))
	println elasticBeanstalkManager.applicationVersionAlreadyExists()
    }
}
