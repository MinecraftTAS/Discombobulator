package com.minecrafttas.discombobulator.tasks;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.processor.FilePreprocessor;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.PortLock;

/**
 * Takes all files in the base folder, preprocesses them<br>
 * and copies the changed files into the versions folders
 * 
 * <pre>
 * 
 *                                 rootDir/1.14.4/src
 * rootDir/src/main...   --&gt;       rootDir/1.12.2/src
 *                                 rootDir/1.8.9/src
 * 
 * </pre>
 * 
 * @author Pancake, Scribble
 */
@CacheableTask
public class TaskPreprocessBase extends DefaultTask {

	@TaskAction
	public void preprocessBase() throws Exception {

		System.out.println(Discombobulator.getSplash());

		// Lock port
		PortLock lock = new PortLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		Path baseProjectDir = Discombobulator.BASE_PROJECT_DIR;
		Map<String, Path> versionsConfig;
		try {
			versionsConfig = Discombobulator.getVersionPairs(baseProjectDir);
		} catch (Exception e) {
			if (e.getMessage() != null && !e.getMessage().isEmpty()) {
				Discombobulator.printError(e.getMessage());
			} else {
				e.printStackTrace();
			}
			return;
		}

		LineFeedHelper.printMessage();

		System.out.println("Preprocessing base source...\n");

		Path baseSourceDir = baseProjectDir.resolve("src");
		if (!Files.exists(baseSourceDir))
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(baseSourceDir, path -> {
			Path inFile = baseSourceDir.resolve(path);
			String extension = FilenameUtils.getExtension(path.getFileName().toString());

			try {
				Discombobulator.fileProcessor.preprocessVersions(inFile, versionsConfig, extension, baseSourceDir, false);
			} catch (MalformedInputException e) {
				Discombobulator.printError(String.format("Can't process file, probably not a text file...\n Maybe add ignoredFileFormats = [\"*.%s\"] to the build.gradle?", extension), path.getFileName().toString());
				e.printStackTrace();
				return;
			} catch (Exception e) {
				Discombobulator.printError(e.getMessage(), path.getFileName().toString());
				e.printStackTrace();
				return;
			}
		});

		// Delete all excess files in version folders
		for (Entry<String, Path> versionPair : versionsConfig.entrySet()) {
			String version = versionPair.getKey();
			Path versionProjectDir = versionPair.getValue();
			Path versionSourceDir = versionProjectDir.resolve("src");
			FilePreprocessor.deleteExcessFiles(baseSourceDir, versionSourceDir, version);
		}

		// Unlock port
		lock.unlock();
	}
}
