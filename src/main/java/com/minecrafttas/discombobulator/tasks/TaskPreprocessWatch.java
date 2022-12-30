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
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the source code on file change
 * @author Pancake
 */
public class TaskPreprocessWatch extends DefaultTask {

	private List<Thread> threads = new ArrayList<>();

	private Map<String, Long> timeout = new HashMap<>();

	@TaskAction
	public void preprocessWatch() {
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Start a filewatcher thread for every physical version folder
		Map<String, Path> versions = new HashMap<>();
		for (File subDir : this.getProject().getProjectDir().listFiles())
			if (new File(subDir, "build.gradle").exists())
				versions.put(subDir.getName(), new File(subDir, "src").toPath().toAbsolutePath());

		for (Entry<String, Path> version : versions.entrySet())
			this.watch(version.getValue(), versions);

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
	private void watch(Path file, Map<String, Path> versions) {
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
						var filename = path.getFileName().toString();
						if (TaskPreprocessWatch.this.timeout.containsKey(filename)) {
							long time = TaskPreprocessWatch.this.timeout.get(filename);
							var passed = System.currentTimeMillis() - time;
							if (passed <= 1000)
								return;
						}
						TaskPreprocessWatch.this.timeout.put(filename, System.currentTimeMillis());

						var relativeFile = file.relativize(path);
						try {
							// Modify this file in other versions too
							var inLines = Files.readAllLines(path);
							for (Entry<String, Path> subVersion : versions.entrySet()) {
								if (subVersion.getKey().equals(version))
									continue;
								var lines = Discombobulator.processor.preprocess(subVersion.getKey(), inLines, filename);
								var outFile = subVersion.getValue().resolve(relativeFile);
								Files.createDirectories(outFile.getParent());
								SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
								Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
							}
							// Modify this file in base project
							var lines = Discombobulator.processor.preprocess(null, inLines, filename);
							var outFile = new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath().toAbsolutePath().resolve(relativeFile);
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
