package com.vivareal.gradle

import static org.junit.Assert.*

import org.gradle.api.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.*

class EC2PluginTest {

	private Project project

	@Before
	public void setup() {
		project = ProjectBuilder.builder().build()
		project.pluginManager.apply 'com.vivareal.gradle.ec2'
	}

	@Test
	public void addingTaskToProject() {
		assert project.tasks.getByPath("launchEC2Instance") instanceof Task
	}

	@Test
	public void checkMissingProperty() {
		try {
			project.tasks.getByPath("launchEC2Instance").execute()
		} catch(Exception e) {
			assert e.getCause().getClass() == MissingPropertyException.class
		}
	}
}
