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

	public static int PORT_LOCK = 8762;

	public static PreprocessingConfiguration config;

	public static Processor processor;
	
	public static PathLock pathLock;
	
	public static final String discoVersion = "1.1.1-SNAPSHOT";
	
	public static final String splash = "\n"
			+ " (                                                                 \n"
			+ " )\\ )                         )         )      (         )         \n"
			+ "(()/( (               )    ( /(      ( /(   (  )\\   ) ( /(    (    \n"
			+ " /(_)))\\ (   (  (    (     )\\())  (  )\\()) ))\\((_| /( )\\())(  )(   \n"
			+ "(_))_((_))\\  )\\ )\\   )\\  '((_)\\   )\\((_)\\ /((_)_ )(_)|_))/ )\\(()\\  \n"
			+ " |   \\(_|(_)((_|(_)_((_)) | |(_) ((_) |(_|_))(| ((_)_| |_ ((_)((_) \n"
			+ " | |) | (_-< _/ _ \\ '  \\()| '_ \\/ _ \\ '_ \\ || | / _` |  _/ _ \\ '_| \n"
			+ " |___/|_/__|__\\___/_|_|_| |_.__/\\___/_.__/\\_,_|_\\__,_|\\__\\___/_|   \n"
			+ "                                                                   \n"
			+ "\n"
			+ getCenterText("Now with less bugs, I think...")+"\n"
			+ "		Created by Pancake and Scribble\n"
			+ getCenterText(discoVersion)+"\n\n";

	/**
	 * Apply the gradle plugin to the project
	 */
	@Override
	public void apply(Project project) {
		// Make buildscript extension for preprocessor
		config = project.getExtensions().create("discombobulator", PreprocessingConfiguration.class);
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
			boolean inverted = config.getInverted().getOrElse(false);
			PORT_LOCK = config.getPort().getOrElse(8762);
			processor = new Processor(getVersion(), config.getPatterns().get(), inverted);
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

	private static String getCenterText(String text) {
		int length = text.length();
		int total = 31;
		if(length%2==0) {
			total = 32;
		}
		return String.format("%s%s", " ".repeat(total-length/2), text);
	}
}
