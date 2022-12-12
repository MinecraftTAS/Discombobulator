package com.minecrafttas.discombobulator;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.minecrafttas.discombobulator.extensions.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.tasks.TaskProcess;

/**
 * Gradle plugin main class
 * @author Pancake
 */
public class Discombobulator implements Plugin<Project> {

	public static PreprocessingConfiguration config;

	/**
	 * Apply the gradle plugin to the project
	 */
	@Override
	public void apply(Project project) {
		// Make buildscript extension for preprocessor
		config = project.getExtensions().create("discombobulator", PreprocessingConfiguration.class);
		// Register synchronization task
		project.getTasks().register("process", TaskProcess.class).get().setGroup("dicombobulator");
	}

}
