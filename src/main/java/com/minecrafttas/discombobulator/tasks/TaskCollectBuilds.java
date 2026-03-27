package com.minecrafttas.discombobulator.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;

/**
 * Builds and moves all built versions into one directory for easier access
 * 
 * @author Pancake
 */
@CacheableTask
public abstract class TaskCollectBuilds extends DefaultTask {

	/**
	 * List of all build dirs
	 */
	@Input
	abstract MapProperty<String, Path> getBuildDirectories();

	/**
	 * The main task component
	 */
	@TaskAction
	public void collectBuilds() {
		Path collectDir = Discombobulator.BUILD_DIR;

		try {
			if (!Files.exists(collectDir))
				Files.createDirectory(collectDir);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (Entry<String, Path> entry : getBuildDirectories().get().entrySet()) {
			Path buildDir = entry.getValue();
			Stream<Path> stream;

			try {
				stream = Files.list(buildDir);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			stream.forEach(path -> {
				Path targetFile = collectDir.resolve(path.getFileName());

				try {
					Files.move(path, targetFile, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			});

			stream.close();
		}
	}
}
