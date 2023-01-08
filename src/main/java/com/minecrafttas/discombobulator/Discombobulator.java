package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.minecrafttas.discombobulator.extensions.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessBase;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch;
import com.minecrafttas.discombobulator.utils.Pair;

/**
 * Gradle plugin main class
 * @author Pancake
 */
public class Discombobulator implements Plugin<Project> {

	public static final int PORT_LOCK = 8762;

	public static PreprocessingConfiguration config;

	public static Processor processor;
	
	public static PathLock pathLock;

	/**
	 * Apply the gradle plugin to the project
	 */
	@Override
	public void apply(Project project) {
		// Make buildscript extension for preprocessor
		config = project.getExtensions().create("discombobulator", PreprocessingConfiguration.class);
		// Create Processor
		processor = new Processor();
		// Create schedule
		pathLock = new PathLock();
		// Register tasks
		TaskPreprocessBase baseTask = project.getTasks().register("preprocessBase", TaskPreprocessBase.class).get();
		baseTask.setGroup("dicombobulator");
		baseTask.setDescription("Split base source into seperate version folders");
		
		TaskPreprocessWatch watchTask = project.getTasks().register("preprocessWatch", TaskPreprocessWatch.class).get();
		watchTask.setGroup("dicombobulator");
		watchTask.setDescription("Starts a watch session. Preprocesses files into other versions on file change.");
		
		project.afterEvaluate(_project -> {
			
			processor.initialize(getVersion(), config.getPatterns().get());
		});
	}
	
	public static List<Pair<String, String>> getVersionPairs(){
		List<Pair<String, String>> out = new ArrayList<>();
		List<String> verPre = config.getVersions().get();
		Pattern regex = Pattern.compile("([\\w\\.]+)(:\\s*(.+))?");
		for (String ver : verPre) {
			Matcher matcher = regex.matcher(ver);
			if(matcher.find()) {
				out.add(Pair.of(matcher.group(1), matcher.group(3)));
			}
		}
		return out;
	}
	
	public static List<String> getVersion() {
		List<String> versions =  new ArrayList<>();
		Pattern regex = Pattern.compile("([\\w\\.]+)(:\\s*(.+))?");
		// Initialize Processor
		List<String> mapsnpaths = config.getVersions().get();
		for(String ver : mapsnpaths) {
			Matcher matcher = regex.matcher(ver);
			if(matcher.find()) {
				versions.add(matcher.group(1));
			}
		}
		return versions;
	}

}
