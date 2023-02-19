package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.PathLock;
import com.minecrafttas.discombobulator.utils.FileWatcher;
import com.minecrafttas.discombobulator.utils.Pair;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the source code on file change
 * 
 * @author Pancake
 */
public class TaskPreprocessWatch extends DefaultTask {

	private List<FileWatcherThread> threads = new ArrayList<>();

	@TaskAction
	public void preprocessWatch() {
		System.out.println("\n"
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
				+ "			 This is fine...\n"
				+ "		Created by Pancake and Scribble\n\n");
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		// Left=Version, Right=Path to gradle folder
		List<Pair<String, String>> versionsConfig = Discombobulator.getVersionPairs();

		List<Pair<String, Path>> versions = new ArrayList<>();

		for (Pair<String, String> versionConf : versionsConfig) {
			String path = versionConf.right();
			if (path == null) {
				path = versionConf.left();
			}
			File rootDir = new File(this.getProject().getProjectDir() + File.separator + path);
			if (new File(rootDir, "build.gradle").exists()) {
				String ver = versionConf.left();
				versions.add(Pair.of(ver, new File(rootDir, "src").toPath().toAbsolutePath()));
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
		for (FileWatcherThread thread : this.threads)
			thread.close();
		lock.unlock();
	}

	/**
	 * Watches and preproceses a source folder
	 * 
	 * @param file     Source folder
	 * @param versions Map of versions
	 */
	private void watch(Path file, List<Pair<String, Path>> versions) {
		String version = file.getParent().getFileName().toString();
		FileWatcher watcher = null;
		try {
			watcher = constructFileWatcher(file, versions, version);
		} catch (IOException e) {
			e.printStackTrace();
		}
		threads.add(new FileWatcherThread(watcher, version));
	}

	private FileWatcher constructFileWatcher(Path file, List<Pair<String, Path>> versions, String version) throws IOException {
		return new FileWatcher(file) {

			@Override
			protected void onNewFile(Path path) {

			}

			@Override
			protected void onModifyFile(Path path) {
				// Get the filename that is getting prerprocessed
				String filename = path.getFileName().toString();

				PathLock schedule = Discombobulator.pathLock;
				if (schedule.isLocked(path))
					return;

				// Get path relative to the root dir
				Path relativeFile = file.relativize(path);
				try {
					// Modify this file in other versions too

					// Read the original file
					List<String> inLines = Files.readAllLines(path);

					// Iterate through all versions
					for (Pair<String, Path> subVersion : versions) {
						// If the version equals the original version, then skip it
						if (subVersion.right().equals(file)) {
							continue;
						}

						System.out.println(path);
						
						// Preprocess the lines
						String[] split = path.getFileName().toString().split("\\.");
						List<String> lines = Discombobulator.processor.preprocess(subVersion.left(), inLines, filename, split[split.length-1]);

						// Write file
						Path outFile = subVersion.right().resolve(relativeFile);

						schedule.scheduleAndLock(outFile);
						Files.createDirectories(outFile.getParent());
						SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
						Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
					}
					// Modify this file in base project
					String[] split = path.getFileName().toString().split("\\.");
					List<String> lines = Discombobulator.processor.preprocess(null, inLines, filename, split[split.length-1]);
					Path outFile = new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath().toAbsolutePath().resolve(relativeFile);
					Files.createDirectories(outFile.getParent());
					SafeFileOperations.write(outFile, lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
					Files.setLastModifiedTime(outFile, Files.getLastModifiedTime(path));
					System.out.println(String.format("Processed %s in %s", path.getFileName(), version));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected void onDeleteFile(Path path) {
				var relativeFile = file.relativize(path);
				// Delete this file in other versions too
				// Iterate through all versions
				for (Pair<String, Path> subVersion : versions) {
					if (subVersion.right().equals(file))
						continue;
					SafeFileOperations.delete(subVersion.right().resolve(relativeFile).toFile());
				}
				// Delete this file in base project
				SafeFileOperations.delete(new File(TaskPreprocessWatch.this.getProject().getProjectDir(), "src").toPath().toAbsolutePath().resolve(relativeFile).toFile());
			}
		};
	}

	/**
	 * Custom closable FileWatcher Thread
	 * <p>Previously the threads kept running in the background, even after the main thread closed. With this, we can close the threads for good.
	 * @author Scribble
	 *
	 */
	private class FileWatcherThread extends Thread {

		private FileWatcher watcher;

		public FileWatcherThread(FileWatcher watcher, String version) {
			super("FileWatcher-" + version);
			System.out.println("Started watching "+version);
			this.watcher = watcher;
			this.setDaemon(true);
			this.start();
		}

		@Override
		public void run() {
			try {
				watcher.watch();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Interrupting "+this.getName());
				if (watcher != null)
					watcher.close();
				e.printStackTrace();
			} catch (ClosedWatchServiceException e) {
				System.out.println("Shutting down "+this.getName());
			}
		}
		
		public void close() {
			if(watcher!=null)
				watcher.close();
		}
	}

}
