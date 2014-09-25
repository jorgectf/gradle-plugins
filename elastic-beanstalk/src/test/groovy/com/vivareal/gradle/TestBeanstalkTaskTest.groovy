package com.vivareal.gradle;

import static org.junit.Assert.*

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class TestBeanstalkTaskTest {
    
    @Test
    public void canAddTaskToProject() {
	Project project = ProjectBuilder.builder().build()
	def task = project.task('testBeanstalk', type: TestBeanstalkTask)
	assertTrue(task instanceof TestBeanstalkTask)
    }
    
    @Test
    public void executeTask() {
	Project project = ProjectBuilder.builder().withName("Teste").build()
	def task = project.task('testBeanstalk', type: TestBeanstalkTask)
	task.printInfo();
    }

}
