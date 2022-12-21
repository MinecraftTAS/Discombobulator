package com.minecrafttas.discombobulator;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.minecrafttas.discombobulator.extensions.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessBase;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch;

/**
 * Gradle plugin main class
 * @author Pancake
 */
public class Discombobulator implements Plugin<Project> {

	public static PreprocessingConfiguration config;

	public static Processor processor;

	/**
	 * Apply the gradle plugin to the project
	 */
	@Override
	public void apply(Project project) {
		// Make buildscript extension for preprocessor
		config = project.getExtensions().create("discombobulator", PreprocessingConfiguration.class);
		// Create Processor
		processor = new Processor();
		// Register tasks
		project.getTasks().register("preprocessBase", TaskPreprocessBase.class).get().setGroup("dicombobulator");
		project.getTasks().register("preprocessWatch", TaskPreprocessWatch.class).get().setGroup("dicombobulator");

		project.afterEvaluate(_project -> {
			// Initialize Processor
			processor.initialize(config.getVersions().get(), config.getPatterns().get());
		});
	}

}
