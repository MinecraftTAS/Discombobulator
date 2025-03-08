package com.minecrafttas.discombobulator.tasks;

import static com.minecrafttas.discombobulator.utils.Colors.CYAN;
import static com.minecrafttas.discombobulator.utils.Colors.WHITE;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch.CurrentFilePreprocessAction;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.PortLock;

/**
 * <p>Preprocesses one version into all other versions including the base source
 * 
 * <p>(If the task was run on 1.14.4)
 * <pre>
 *                                 rootDir/src
 * rootDir/1.14.4/src/   --&gt;       rootDir/1.12.2/src
 *                                 rootDir/1.8.9/src
 * 
 * </pre>
 * 
 * @author Scribble
 */
public class TaskPreprocessVersion extends DefaultTask {

	@TaskAction
	public void preprocessVersion() throws Exception {
		System.out.println(Discombobulator.getSplash());

		// Lock port
		PortLock lock = new PortLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		Path baseProjectDir = this.getProject().getParent().getProjectDir().toPath();
		Path baseSourceDir = baseProjectDir.resolve("src");

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

		Path versionProjectDir = this.getProject().getProjectDir().toPath();
		Pair<String, Path> masterVersion = null;
		// Find current version in version config

		for (Entry<String, Path> entry : versionsConfig.entrySet()) {
			if (entry.getValue().equals(versionProjectDir)) {
				masterVersion = Pair.of(entry.getKey(), entry.getValue());
			}
		}

		if (masterVersion == null) {
			throw new Exception("Version could not be found in build.gradle");
		}

		String masterVersionName = masterVersion.left();

		LineFeedHelper.printMessage();

		System.out.println(String.format("Preprocessing version %s...", masterVersionName));

		Path versionSourceDir = versionProjectDir.resolve("src");
		if (!Files.exists(versionSourceDir))
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(versionSourceDir, path -> {
			Path inFile = versionSourceDir.resolve(path);
			String extension = FilenameUtils.getExtension(path.getFileName().toString());

			try {
				// Preprocess version dir
				CurrentFilePreprocessAction action = Discombobulator.fileProcessor.preprocessVersions(inFile, versionsConfig, extension, versionSourceDir, true);

				// Preprocess in base dir
				Path outFile = baseSourceDir.resolve(path);
				Discombobulator.fileProcessor.preprocessFile(inFile, outFile, null, extension);

				/* Action has to run after the base dir preprocessing,
				 * as runFileAction locks the file, and base dir preprocessing silently would fail*/
				if (action != null) {
					System.out.println(String.format("into version %s%s%s", CYAN, masterVersionName, WHITE));
					TaskPreprocessWatch.runFileAction(action);
				}
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
	}
}
