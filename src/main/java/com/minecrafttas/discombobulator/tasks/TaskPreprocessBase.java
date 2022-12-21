package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the base source code into all versions.
 * @author Pancake
 */
public class TaskPreprocessBase extends DefaultTask {

	@TaskAction
	public void preprocessBase() {
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		List<String> versions = new ArrayList<>();
		for (File subDir : this.getProject().getProjectDir().listFiles())
			if (new File(subDir, "build.gradle").exists())
				versions.add(subDir.getName());

		// Preprocess all files from base source
		System.out.println("Preprocessing base source...");

		var baseSourceDir = new File(this.getProject().getProjectDir(), "src");
		if (!baseSourceDir.exists())
			throw new RuntimeException("Base source folder not found");

		BetterFileWalker.walk(baseSourceDir.toPath(), path -> {
			System.out.println("Preprocessing " + path.getFileName());
			try {
				for (String version : versions) {
					// Find input and output file
					var inFile = baseSourceDir.toPath().resolve(path);
					var outFile = new File(baseSourceDir.getParent(), version + File.separatorChar + "src").toPath().resolve(path);

					// Preprocess file
					var lines = Discombobulator.processor.preprocess(version, Files.readAllLines(inFile), version);

					// Write file and update last modified date
					Files.createDirectories(outFile.getParent());
					Files.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(inFile));
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not write to filesystem.", e);
			}
		});

		// Delete all excess file in version folders
		for (String version : versions) {
			var subSourceDir = new File(baseSourceDir.getParent(), version + File.separatorChar + "src").toPath();
			BetterFileWalker.walk(subSourceDir, path -> {
				// Verify if file exists in base source dir
				var originalFile = baseSourceDir.toPath().resolve(path);
				if (!Files.exists(originalFile)) {
					System.out.println("Deleting " + path.getFileName() + " in " + version);
					SafeFileOperations.delete(subSourceDir.resolve(path).toFile());
				}
			});
		}

		// Unlock port
		lock.unlock();
	}

}
