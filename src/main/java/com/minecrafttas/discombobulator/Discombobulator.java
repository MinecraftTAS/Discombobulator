package com.minecrafttas.discombobulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import com.minecrafttas.discombobulator.config.PreprocessingConfiguration;
import com.minecrafttas.discombobulator.processor.FilePreprocessor;
import com.minecrafttas.discombobulator.processor.LinePreprocessor;
import com.minecrafttas.discombobulator.tasks.TaskCollectBuilds;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessBase;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessVersion;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessVersionError;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch;
import com.minecrafttas.discombobulator.utils.Colors;
import com.minecrafttas.discombobulator.utils.EmergencyTransformer;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.PathLock;

/**
 * Gradle plugin main class
 * 
 * @author Pancake, Scribble
 */
public class Discombobulator implements Plugin<Project> {

	/**
	 * Which port to lock
	 */
	public static int PORT_LOCK = 8762;

	/**
	 * The config set in the build.gradle file of the discombobulator
	 */
	public static PreprocessingConfiguration config;

	/**
	 * If true: Disables color output in the console
	 */
	public static boolean DISABLE_ANSI = false;

	/**
	 * Whether to use Windows or Unix style line feeds
	 */
	public static String DEFAULT_LINE_FEED = System.lineSeparator();

	/**
	 * The base dir of the project
	 */
	public static Path BASE_PROJECT_DIR;

	/**
	 * The base build directory, usually {@link #BASE_PROJECT_DIR}/build
	 */
	public static Path BUILD_DIR;

	/**
	 * The {@link FilePreprocessor}
	 */
	public static FilePreprocessor fileProcessor;

	/**
	 * The {@link PathLock}
	 */
	public static PathLock pathLock;

	/**
	 * The version of Discombobulator,
	 * used for the splash in the console
	 */
	private static String discoVersion;

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

		/* preprocessBase */
		TaskPreprocessBase baseTask = project.getTasks().register("preprocessBase", TaskPreprocessBase.class).get();
		baseTask.setGroup("discombobulator");
		baseTask.setDescription("Split base source into seperate version folders");

		/* preprocessWatch */
		TaskPreprocessWatch watchTask = project.getTasks().register("preprocessWatch", TaskPreprocessWatch.class).get();
		watchTask.setGroup("discombobulator");
		watchTask.setDescription("Starts a watch session. Preprocesses files into other versions on file change.");
		watchTask.setProperty("projectName", project.getName());

		/* collectBuilds */
		TaskCollectBuilds collectBuilds = project.getTasks().register("collectBuilds", TaskCollectBuilds.class).get();
		collectBuilds.setGroup("discombobulator");
		collectBuilds.setDescription("Builds, then collects all versions in root/build");

		List<Task> compileTaskList = new ArrayList<>();
		Map<String, Path> buildDirs = new HashMap<>();
		for (Project subProject : project.getSubprojects()) {
			Task compileTask = subProject.getTasksByName("build", false).iterator().next();
			compileTaskList.add(compileTask);

			buildDirs.put(subProject.getName(), getBuildDir(subProject).resolve("libs"));
		}
		collectBuilds.setDependsOn(compileTaskList);
		collectBuilds.setProperty("buildDirectories", buildDirs);

		/* preprocessVersion */
		for (Project subProject : project.getSubprojects()) {
			// Register preprocessVersion task in subProjects
			TaskPreprocessVersion versionTask = subProject.getTasks().register("preprocessVersion", TaskPreprocessVersion.class).get();
			versionTask.setProperty("versionDirectory", subProject.getProjectDir());
			versionTask.setGroup("discombobulator");
			versionTask.setDescription("Preprocesses this version back to the base folder and to versions other than this one");
		}

		// Register preprocessVersion task in root
		TaskPreprocessVersionError versionTaskRoot = project.getTasks().register("preprocessVersion", TaskPreprocessVersionError.class).get();
		versionTaskRoot.setGroup("discombobulator");
		versionTaskRoot.setDescription("Do not use this task! Use it in subprojects!");

		project.afterEvaluate(_project -> {
			boolean inverted = config.getInverted().getOrElse(false);
			PORT_LOCK = config.getPort().getOrElse(8762);
			DISABLE_ANSI = config.getDisableAnsi().getOrElse(false);
			DEFAULT_LINE_FEED = config.getDefaultLineFeed().getOrElse(System.lineSeparator());
			BASE_PROJECT_DIR = _project.getProjectDir().toPath();
			BUILD_DIR = getBuildDir(_project);

			Map<String, Path> versionPairs = null;
			try {
				versionPairs = getVersionPairs(BASE_PROJECT_DIR);
			} catch (Exception e) {
				if (e.getMessage() != null && !e.getMessage().isEmpty()) {
					printError(e.getMessage());
				} else {
					e.printStackTrace();
				}
				return;
			}
			List<String> versionStrings = new ArrayList<>(versionPairs.keySet());
			LinePreprocessor processor = new LinePreprocessor(versionStrings, config.getPatterns().get(), inverted);

			List<String> ignored = config.getIgnoredFileFormats().getOrElse(new ArrayList<>());
			WildcardFileFilter fileFilter = WildcardFileFilter.builder().setWildcards(ignored).get();

			Map<String, Map<String, Pair<String, String>>> emergencyTransformConfig = config.getEmergencyTransform().getOrNull();
			EmergencyTransformer emergencyTransformer = new EmergencyTransformer(emergencyTransformConfig);

			fileProcessor = new FilePreprocessor(processor, fileFilter, emergencyTransformer);

			// Yes this is yoinked from the gradle forums to get the disco version. Is there
			// a better method? Probably. Do I care? Currently, no.
			final Configuration classpath = _project.getBuildscript().getConfigurations().getByName("classpath");
			final String version = classpath.getResolvedConfiguration().getResolvedArtifacts().stream().map(artifact -> artifact.getModuleVersion().getId()).filter(id -> "com.minecrafttas".equalsIgnoreCase(id.getGroup())
					&& "discombobulator".equalsIgnoreCase(id.getName())).findAny().map(ModuleVersionIdentifier::getVersion).orElseThrow(() -> new IllegalStateException("Discombobulator plugin has been deployed with wrong coordinates: expected group to be 'com.minecrafttas' and name to be 'Discombobulator'"));
			discoVersion = version;
		});

	}

	public static String getSplash() {
		return "\n" + (DISABLE_ANSI ? getColorLessSplash() : getColoredSplash()) + "\n\n"
				+ getCenterText(String.format("Now with unobfuscated support!")) + "\n"
				+ getCenterText("Created by Pancake and Scribble") + "\n"
				+ getCenterText(discoVersion) + "\n\n";

	}

	public static LinkedHashMap<String, Path> getVersionPairs(Path baseProjectDir) throws Exception {
		Map<String, String> versionsConfig = config.getVersions().get();
		LinkedHashMap<String, Path> versions = new LinkedHashMap<>();

		for (Entry<String, String> versionConf : versionsConfig.entrySet()) {
			String version = versionConf.getKey();
			String pathString = versionConf.getValue();
			if (pathString == null || pathString.isEmpty()) {
				pathString = version;
			}
			Path subProjectDir = baseProjectDir.resolve(pathString);

			if (Files.exists(subProjectDir.resolve("build.gradle"))) {
				versions.putLast(versionConf.getKey(), subProjectDir);
			} else {
				throw new Exception("Could not find build.gradle in " + subProjectDir.toString());
			}
		}
		return versions;
	}

	/**
	 * Centers any given text
	 * @param text The text to print
	 * @return The centered text
	 */
	private static String getCenterText(String text) {
		int length = text.length();
		return getCenterText(text, length);
	}

	/**
	 * @param project The project to use
	 * @return The build directory from the project
	 */
	public static Path getBuildDir(Project project) {
		return project.getLayout().getBuildDirectory().get().getAsFile().toPath();
	}

	/**
	 * Centers any given text
	 * 
	 * This method is used if ANSI colors are in the text which are not rendered,<br>
	 * but are still counted in String.length();
	 * 
	 * @param text The text to print
	 * @param length The length of the text
	 * @return The centered text
	 */
	private static String getCenterText(String text, int length) {
		int total = 31;
		if (length % 2 == 0) {
			total = 32;
		}
		return String.format("%s%s", " ".repeat(total - length / 2), text);
	}

	/**
	 * Prints an error in red to the console
	 * @param msg The error message to print
	 */
	public static void printError(String msg) {
		System.err.println(Colors.RED + msg + Colors.WHITE);
	}

	/**
	 * Print an error to the console in red together with the file where this error occured in
	 * @param msg The error message to print
	 * @param filename The filename of the file where this error occured
	 */
	public static void printError(String msg, String filename) {
		printError(String.format("[%s] %s", filename, msg));
	}

	/**
	 * @return The splash with ANSI colors
	 */
	private static String getColoredSplash() {
		return "[0m [1;31m([0m\n"
				+ " [1;31m)\\[0m [1;31m)[0m                         [1;31m)[0m         [1;31m)[0m      [1;31m([0m         [1;31m)[0m\n"
				+ "[1;31m([33m()/[31m([0m [1;31m([0m               [1;31m)[0m    [1;31m([0m [1;33m/[31m([0m      [1;31m([0m [1;31m/([0m   [1;31m([0m  [1;31m)\\[0m   [1;31m)[0m [1;31m([0m [1;31m/([0m    [1;31m([0m\n"
				+ " [1;31m/[33m(_)))[31m\\[0m [1;31m([0m   [1;31m([0m  [1;31m([0m    [1;31m([0m     [1;31m)[33m\\()[31m)[0m  [1;31m([0m  [1;31m)[33m\\()[31m)[0m [1;31m)[33m)[31m\\([33m(_[31m|[0m [1;31m/([0m [1;31m)\\[33m()[31m)([0m  [1;31m)([0m\n"
				+ "[1;31m([0;32m_[1;33m))[0;32m_[1;33m((_))[31m\\[0m  [1;31m)\\[0m [1;31m)\\[0m   [1;31m)[33m\\[0m  [1;31m'([33m([0;32m_[1;33m)[31m\\[0m   [1;31m)\\([33m(_)[31m\\[0m [1;31m/[33m((_)[0;32m_[37m [1;31m)([33m_)([0;32m_[1;33m))/[0m [1;33m)[31m\\([33m()[31m\\[0m\n"
				+ " [32m|[37m   [32m\\[1;33m([0;32m_[1;33m|([0;32m_[1;31m)([33m([0;32m_[1;33m|([0;32m_[1;31m)[0;32m_[1;31m([33m([0;32m_[1;33m)[31m)[0m [32m| |[1;33m([0;32m_[1;31m)[0m [1;31m([33m([0;32m_[1;33m)[0m [32m|[1;33m([0;32m_[1;33m|[0;32m_[1;33m))([0;32m|[37m [1;33m(([0;32m_[1;33m)[0;32m_|[37m [32m|_[37m [1;33m(([0;32m_[1;33m)(([0;32m_[1;31m)[0m\n"
				+ " [32m| |) | (_-< _/ _ \\ '  \\[1;31m()[0;32m| '_ \\/ _ \\ '_ \\ || | / _` |  _/ _ \\ '_|[1m\n"
				+ "[0m [32m|___/|_/__|__\\___/_|_|_| |_.__/\\___/_.__/\\_,_|_\\__,_|\\__\\___/_|[37m\n";
	}

	/**
	 * @return The splash without ANSI colors
	 */
	private static String getColorLessSplash() {
		return " (                                                                 \n"
				+ " )\\ )                         )         )      (         )         \n"
				+ "(()/( (               )    ( /(      ( /(   (  )\\   ) ( /(    (    \n"
				+ " /(_)))\\ (   (  (    (     )\\())  (  )\\()) ))\\((_| /( )\\())(  )(   \n"
				+ "(_))_((_))\\  )\\ )\\   )\\  '((_)\\   )\\((_)\\ /((_)_ )(_)(_))/ )\\(()\\  \n"
				+ " |   \\(_|(_)((_|(_)_((_)) | |(_) ((_) |(_|_))(| ((_)_| |_ ((_)((_) \n"
				+ " | |) | (_-< _/ _ \\ '  \\()| '_ \\/ _ \\ '_ \\ || | / _` |  _/ _ \\ '_| \n"
				+ " |___/|_/__|__\\___/_|_|_| |_.__/\\___/_.__/\\_,_|_\\__,_|\\__\\___/_|   \n"
				+ "                                                                   ";
	}
}
