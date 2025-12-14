package com.minecrafttas.discombobulator.tasks;

import static com.minecrafttas.discombobulator.utils.Colors.CYAN;
import static com.minecrafttas.discombobulator.utils.Colors.GREEN;
import static com.minecrafttas.discombobulator.utils.Colors.PURPLE;
import static com.minecrafttas.discombobulator.utils.Colors.PURPLE_BRIGHT;
import static com.minecrafttas.discombobulator.utils.Colors.RED;
import static com.minecrafttas.discombobulator.utils.Colors.RED_BRIGHT;
import static com.minecrafttas.discombobulator.utils.Colors.WHITE;
import static com.minecrafttas.discombobulator.utils.Colors.YELLOW;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.Colors;
import com.minecrafttas.discombobulator.utils.FileWatcher;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.PathLock;
import com.minecrafttas.discombobulator.utils.PortLock;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;

/**
 * Starts a file watcher in the source foulders and preprocesses any changed file,<br>
 * then copies the changes into other versions + base folder
 * 
 * @author Pancake, Scribble
 */
public abstract class TaskPreprocessWatch extends DefaultTask {

	private List<FileWatcherThread> threads = new ArrayList<>();

	private CurrentFilePreprocessAction currentFileAction = null;

	private boolean msgSeen = false;

	@Input
	abstract Property<String> getProjectName();

	@TaskAction
	public void preprocessWatch() throws Exception {
		System.out.println(Discombobulator.getSplash());
		// Lock port
		var lock = new PortLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Prepare list of physical version folders
		Path baseProjectDir = Discombobulator.BASE_PROJECT_DIR;

		LineFeedHelper.printMessage();

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

		versionsConfig.put("Base", baseProjectDir);

		for (Entry<String, Path> versionPair : versionsConfig.entrySet()) {
			Path subSourceDir = versionPair.getValue().resolve("src");
			this.watchVersion(subSourceDir, versionsConfig);
		}
		//this.watchBase(versionsConfig);

		// Wait for user input and cancel the task

		Scanner sc = new Scanner(System.in);
		System.out.println(String.format("Press %sENTER%s to stop the file watcher", GREEN, WHITE));
		String in;
		try {
			while (!(in = sc.nextLine()).isBlank()) {
				if (!in.isBlank()) {
					if (currentFileAction == null) {
						System.out.println("No recent file exists...\n");
						continue;
					}

					TaskPreprocessWatch.runFileAction(currentFileAction);
					System.out.println(String.format("Preprocessed the recently edited file %s%s%s\n", PURPLE, currentFileAction.outFile().getFileName(), WHITE));

					currentFileAction = null;
				}
			}
		} catch (IOException e1) {
		}
		sc.close();
		for (FileWatcherThread thread : this.threads)
			thread.close();
		lock.unlock();
	}

	/**
	 * Watches and preprocesses a version source folder
	 * 
	 * @param subSourceDir Source folder of the sub project
	 * @param targetSet Map of target versions
	 */
	private void watchVersion(Path subSourceDir, Map<String, Path> targetSet) {
		String version = subSourceDir.getParent().getFileName().toString();
		FileWatcher watcher = null;
		try {
			watcher = constructFileWatcher(subSourceDir, targetSet, version);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (version.equals(getProjectName().get())) {
			version = "Base";
		}

		threads.add(new FileWatcherThread(watcher, version));
	}

	private FileWatcher constructFileWatcher(Path subSourceDir, Map<String, Path> versions, String version) throws IOException {
		return new FileWatcher(subSourceDir) {

			@Override
			protected void onNewFile(Path path) {

			}

			@Override
			protected void onModifyFile(Path path) {

				PathLock pathLock = Discombobulator.pathLock;
				if (pathLock.isLocked(path))
					return;

				// Get path relative to the root dir
				String extension = FilenameUtils.getExtension(path.getFileName().toString());
				try {
					System.out.println(String.format("[%s%s%s]", PURPLE_BRIGHT, TaskPreprocessWatch.findVersionFromPath(path, versions), WHITE));
					// Preprocess in all sub versions
					currentFileAction = Discombobulator.fileProcessor.preprocessVersions(path, versions, extension, subSourceDir, true);

					if (msgSeen == false) {
						System.out.println(Colors.YELLOW + "Type 1 to also preprocess this file" + Colors.WHITE + "\n");
						msgSeen = true;
					}
				} catch (MalformedInputException e) {
					Discombobulator.printError(String.format("Can't process file, probably not a text file...\n Maybe add ignoredFileFormats = [\"*.%s\"] to the build.gradle?", extension), path.getFileName().toString());
					return;
				} catch (Exception e) {
					Discombobulator.printError(e.getMessage(), path.getFileName().toString());
					return;
				}
			}

			@Override
			protected void onDeleteFile(Path path) {

				if (Discombobulator.pathLock.isLocked(path))
					return;

				String version = findVersionFromPath(path, versions);

				Path relativeFile = subSourceDir.relativize(path);

				System.out.println(String.format("[%s%s%s]", RED_BRIGHT, version, WHITE));
				System.out.println(String.format("Deleting %s%s%s%s%s", relativeFile.getParent(), File.separator, RED, relativeFile.getFileName().toString(), WHITE));
				// Delete this file in other versions too
				// Iterate through all versions
				for (Entry<String, Path> versionPair : versions.entrySet()) {
					Path targetProject = versionPair.getValue();
					Path targetSourceDir = targetProject.resolve("src");

					if (targetSourceDir.equals(subSourceDir))
						continue;

					Path targetPathToDelete = targetSourceDir.resolve(relativeFile);

					Discombobulator.pathLock.scheduleAndLock(targetPathToDelete);

					System.out.println(String.format("from version %s%s%s", CYAN, versionPair.getKey(), WHITE));
					if (version.equals("Base")) {
						SafeFileOperations.nuke(targetPathToDelete);
					} else {
						SafeFileOperations.delete(targetPathToDelete);
					}

					Path parentDir = targetPathToDelete.getParent();
					boolean isEmpty;
					try {
						isEmpty = Files.isDirectory(parentDir) && Files.list(parentDir).count() == 0L;
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
					if (isEmpty) {
						if (version.equals("Base")) {
							SafeFileOperations.nuke(parentDir);
						} else {
							SafeFileOperations.delete(parentDir);
						}
					}
				}
			}
		};
	}

	/**
	 * Custom closable FileWatcher Thread
	 * <p>
	 * Previously the threads kept running in the background, even after the main
	 * thread closed. With this, we can close the threads for good.
	 * 
	 * @author Scribble
	 *
	 */
	private class FileWatcherThread extends Thread {

		private FileWatcher watcher;

		public FileWatcherThread(FileWatcher watcher, String version) {
			super("FileWatcher-" + version);
			System.out.println(String.format("Started watching %s%s%s", GREEN, version, WHITE));
			this.watcher = watcher;
			this.setDaemon(true);
			this.start();
		}

		@Override
		public void run() {
			try {
				watcher.watch();
			} catch (IOException e) {
//				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Interrupting " + YELLOW + this.getName().replace("FileWatcher-", "") + WHITE);
				if (watcher != null)
					watcher.close();
				e.printStackTrace();
			} catch (ClosedWatchServiceException e) {
				System.out.println("Shutting down " + GREEN + this.getName().replace("FileWatcher-", "") + WHITE);
			}
		}

		public void close() {
			if (watcher != null)
				watcher.close();
		}
	}

	/**
	 *  Stores data used for preprocessing the file that was edited most recently.
	 *
	 * This fixes an issue where the IDE will behave weirdly, when trying to
	 * preprocess and replace a file, that is currently being worked on. So when
	 * saving a file, The file watcher would also replace the file that you just
	 * saved, leading to discrepancies and annoyances.
	 *
	 * With this, you can execute the preprocessing at a later time.
	 *
	 * @author Scribble
	 */
	public static record CurrentFilePreprocessAction(List<String> outLines, Path inFile, Path outFile) {
	}

	/**
	 * Runs the {@link CurrentFilePreprocessAction}
	 * @param currentFileAction The {@link CurrentFilePreprocessAction} to run
	 */
	public static void runFileAction(CurrentFilePreprocessAction currentFileAction) throws IOException {
		Path outFile = currentFileAction.outFile();
		List<String> outLines = currentFileAction.outLines();

		Discombobulator.pathLock.scheduleAndLock(outFile.getParent());
		Discombobulator.pathLock.scheduleAndLock(outFile);
		Files.createDirectories(outFile.getParent());
		SafeFileOperations.write(outFile, outLines, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
	}

	public static String findVersionFromPath(Path path, Map<String, Path> versions) {
		for (Entry<String, Path> version : versions.entrySet()) {
			if (path.startsWith(version.getValue())) {
				return version.getKey();
			}
		}
		return null;
	}
}
