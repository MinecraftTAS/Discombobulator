package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public static final int PORT_LOCK = 8762;

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
		TaskPreprocessBase baseTask = project.getTasks().register("preprocessBase", TaskPreprocessBase.class).get();
		baseTask.setGroup("dicombobulator");
		baseTask.setDescription("Split base source into seperate version folders");
		
		TaskPreprocessWatch watchTask = project.getTasks().register("preprocessWatch", TaskPreprocessWatch.class).get();
		watchTask.setGroup("dicombobulator");
		watchTask.setDescription("Starts a watch session. Preprocesses files into other versions on file change.");
		
		project.afterEvaluate(_project -> {
			List<String> versions =  new ArrayList<>();
			// Initialize Processor
			List<Map<String, String>> mapsnpaths = config.getVersions().get();
			for(Map<String, String> ver : mapsnpaths) {
				Set<String> keys = ver.keySet();
				for(String key : keys) {
					versions.add(key);
					break;
				}
			}
			processor.initialize(versions, config.getPatterns().get());
		});
	}

}
