package com.minecrafttas.discombobulator.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Task for preprocessing one version into all other versions including the base source
 * 
 * @author Scribble
 */
public class TaskPreprocessVersion extends DefaultTask {

	@TaskAction
	public void preprocessVersion() {
		System.out.println("Test");
	}
}
