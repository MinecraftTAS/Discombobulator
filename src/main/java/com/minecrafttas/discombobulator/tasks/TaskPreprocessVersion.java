package com.minecrafttas.discombobulator.tasks;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * Task for preprocessing one version into all other versions including the base source
 * 
 * @author Scribble
 */
public class TaskPreprocessVersion extends DefaultTask {

	@TaskAction
	public void preprocessVersion() throws Exception {
		System.out.println(Discombobulator.getSplash());

		// Lock port
		SocketLock lock = new SocketLock(Discombobulator.PORT_LOCK);
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

		LineFeedHelper.printMessage();

		System.out.println(String.format("Preprocessing version %s...", masterVersion.left()));

		Path versionSourceDir = versionProjectDir.resolve("src");
		if (!Files.exists(versionSourceDir))
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(versionSourceDir, path -> {
			Path inFile = versionSourceDir.resolve(path);
			String extension = FilenameUtils.getExtension(path.getFileName().toString());

			try {
				// Preprocess version dir
				Discombobulator.fileProcessor.preprocessVersions(inFile, versionsConfig, extension, versionSourceDir, false);

				// Preprocess in base dir
				Path outFile = baseSourceDir.resolve(path);
				Discombobulator.fileProcessor.preprocessFile(path, outFile, null, extension);
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
