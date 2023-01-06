package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.FileWatcher;
import com.minecrafttas.discombobulator.utils.MapPair;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the source code on file change
 * @author Pancake
 */
public class TaskPreprocessWatch extends DefaultTask {

	private List<Thread> threads = new ArrayList<>();

	private long timeout = -1;

	@TaskAction
	public void preprocessWatch() {
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		List<Map<String, String>> versionsConfig = Discombobulator.config.getVersions().get();
		
		List<Pair<String, Path>> versions = new ArrayList<>();
		
		for (Map<String, String> version : versionsConfig) {
			String path = MapPair.getRight(version);
			if(new File(path, "build.gradle").exists()) {
				String ver = MapPair.getLeft(version);
				versions.add(Pair.of(ver, new File(path).toPath().toAbsolutePath()));
			}
		}
		
		for (Pair<String, Path> version : versions)
			this.watch(version.right(), versions);

		// Wait for user input and cancel the task
		try {
			System.out.println("Press enter to stop the file watcher");
			System.in.read();
		} catch (IOException e) {
			// don't care
		}
		for (Thread thread : this.threads)
			thread.interrupt();
		lock.unlock();
	}

	/**
	 * Watches and preproceses a source folder
	 * @param file Source folder
	 * @param versions Map of versions
	 */
	private void watch(Path file, List<Pair<String, Path>> versions) {
		var thread = new Thread(() -> {
			var version = file.getParent().getFileName().toString();
			FileWatcher watcher = null;
			try {
				
				watcher = new FileWatcher(file) {

					@Override
					protected void onNewFile(Path path) {

					}

					@Override
					protected void onModifyFile(Path path) {
						String filename = path.getFileName().toString();
						
						long passed = System.currentTimeMillis() - timeout;
						timeout += 100;
						if (passed <= 1000)
							return;
						timeout = System.currentTimeMillis();

						Path relativeFile = file.relativize(path);
						try {
							// Modify this file in other versions too
							List<String> inLines = Files.readAllLines(path);
							for (Entry<String, Path> subVersion : versions.entrySet()) {
								if (subVersion.getKey().equals(version))
									continue;
								List<String> lines = Discombobulator.processor.preprocess(subVersion.getKey(), inLines, filename);
								Path outFile = subVersion.getValue().resolve(relativeFile);
								Files.createDirectories(outFile.getParent());
								SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
								Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
							}
							// Modify this file in base project
							List<String> lines = Discombobulator.processor.preprocess(null, inLines, filename);
							Path outFile = new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath().toAbsolutePath().resolve(relativeFile);
							Files.createDirectories(outFile.getParent());
							SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
							Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
							System.out.println("Processed " + path.getFileName());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					@Override
					protected void onDeleteFile(Path path) {
						var relativeFile = file.relativize(path);
						// Delete this file in other versions too
						for (Entry<String, Path> subVersion : versions.entrySet()) {
							if (subVersion.getKey().equals(version))
								continue;
							SafeFileOperations.delete(subVersion.getValue().resolve(relativeFile).toFile());
						}
						// Delete this file in base project
						SafeFileOperations.delete(new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath().toAbsolutePath().resolve(relativeFile).toFile());
						System.out.println("Deleted " + path.getFileName());
					}
				};
				watcher.watch();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Shutting down FileWatcher for " + version);
				if (watcher != null)
					watcher.close();
			}
		});
		thread.setName("FileWatcher-" + file.getFileName());
		thread.setDaemon(true);
		thread.start();
	}

}
