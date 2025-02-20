package com.minecrafttas.discombobulator.processor;

import static com.minecrafttas.discombobulator.utils.Colors.CYAN;
import static com.minecrafttas.discombobulator.utils.Colors.PURPLE;
import static com.minecrafttas.discombobulator.utils.Colors.RED;
import static com.minecrafttas.discombobulator.utils.Colors.WHITE;
import static com.minecrafttas.discombobulator.utils.Colors.YELLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.tasks.TaskPreprocessWatch.CurrentFilePreprocessAction;
import com.minecrafttas.discombobulator.utils.BetterFileWalker;
import com.minecrafttas.discombobulator.utils.LineFeedHelper;
import com.minecrafttas.discombobulator.utils.SafeFileOperations;

public class FilePreprocessor {

	private final LinePreprocessor processor;
	private final WildcardFileFilter fileFilter;

	public FilePreprocessor(LinePreprocessor processor, WildcardFileFilter fileFilter) {
		this.processor = processor;
		this.fileFilter = fileFilter;
	}

	/**
	 * Preprocesses a single file into the target version
	 * 
	 * @param inFile The path to the file that will be preprocessed  
	 * @param outFile The path to the file where the preprocessed file will be stored stored
	 * @param version The target version to preprocess to            
	 * @param extension The file extension (e.g. ".java") of the file
	 * @throws Exception If the preprocessing fails
	 */
	public void preprocessFile(Path inFile, Path outFile, String version, String extension) throws Exception {

//		System.out.println(inFile);
//		System.out.println(outFile + "\n");

		/*
		 *  Check if file was just preprocessed.
		 *  This is important when using the file watcher
		 *  
		 *  Example:
		 *  1.12
		 *  1.14
		 *  
		 *  If we edit 1.12, the file watcher triggers
		 *  and edits the same file in 1.14.
		 *  
		 *  But this would also trigger the file watcher for 1.14,
		 *  creating an endless loop of preprocessing back and forth.
		 *  
		 *  So we lock the file just before writing.
		 */
		if (Discombobulator.pathLock.isLocked(inFile)) {
			return;
		}

		if (fileFilter != null && fileFilter.accept(inFile.toFile())) {
			System.out.println(String.format("Ignoring %s%s%s", YELLOW, inFile.getFileName().toString(), WHITE));
			Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		List<String> linesToProcess = Files.readAllLines(inFile);

		preprocessLines(linesToProcess, outFile, version, extension);
	}

	/**
	 * Preprocess a file into multiple target versions
	 * 
	 * @param inFile The file to preprocess
	 * @param versions The mapped versions to preprocess to.
	 * @param extension The file extension (e.g. ".java") of the file
	 * @param currentVersionDir The current version directory that where the file that is being worked on lies.<br>
	 * Used for checking if you are trying to preprocess into the current directory, which can lead to issues otherwise.
	 * @param verbose If more info should be printed to the console
	 * @return The {@link CurrentFilePreprocessAction}
	 * @throws Exception If the preprocessing fails
	 */
	public CurrentFilePreprocessAction preprocessVersions(Path inFile, Map<String, Path> versions, String extension, Path currentVersionDir, boolean verbose) throws Exception {

		Path relativeInFile = currentVersionDir.relativize(inFile);
		System.out.println(String.format("Preprocessing %s%s%s%s%s", relativeInFile.getParent(), File.separator, PURPLE, relativeInFile.getFileName().toString(), WHITE));

		boolean ignored = fileFilter != null && fileFilter.accept(inFile.toFile());

		List<String> linesToProcess = null;
		if (!ignored)
			linesToProcess = Files.readAllLines(inFile);
		else
			System.out.println(String.format("Ignoring %s%s%s", YELLOW, inFile.getFileName().toString(), WHITE));

		CurrentFilePreprocessAction out = null;

		// Iterate through all versions
		for (Entry<String, Path> versionPair : versions.entrySet()) {
			String versionName = versionPair.getKey();
			Path targetProject = versionPair.getValue();
			Path targetSubSourceDir = targetProject.resolve("src");
			Path outFile = targetSubSourceDir.resolve(relativeInFile);

			// Stop certain file types to be preprocessed (e.g. ".png")
			if (ignored) {
				if (verbose) {
					System.out.println(String.format("into version %s%s%s", CYAN, versionName, WHITE));
				}
				Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
				continue;
			}

			// Preprocess the lines
			List<String> outLines = processor.preprocess(versionName, linesToProcess, extension);

			// If the version equals the original version, then skip it
			if (targetSubSourceDir.equals(currentVersionDir)) {
				out = new CurrentFilePreprocessAction(outLines, inFile, outFile);
				continue;
			}

			preprocessLines(outLines, outFile, versionName, extension);
			if (verbose) {
				System.out.println(String.format("into version %s%s%s", CYAN, versionName, WHITE));
			}
		}
		return out;
	}

	/**
	 * Preprocesses and writes the inLines to a file
	 * @param inLines The lines to preprocess
	 * @param outFile The file to write to
	 * @param version The version to preprocess to
	 * @param extension The file extension
	 * @return The preprocessed lines
	 * @throws Exception
	 */
	public List<String> preprocessLines(List<String> inLines, Path outFile, String version, String extension) throws Exception {
		List<String> lines = processor.preprocess(version, inLines, extension);
		writeLines(inLines, outFile);
		return lines;
	}

	///
	/// 1. Locks the file to stop the filewatcher from detecting it
	/// 2. Creates any missing directories
	/// 3. Writes the lines with the line feed specified by the `line.seperator` system property
	///
	/// @param inLines
	/// @param outFile
	/// @throws Exception
	///
	private void writeLines(List<String> inLines, Path outFile) throws Exception {
		// Lock the file
		Discombobulator.pathLock.scheduleAndLock(outFile);

		// Write file and update last modified date
		Files.createDirectories(outFile.getParent());

		StringBuilder stringBuilder = new StringBuilder();
		String linefeed = LineFeedHelper.newLine();
		for (String line : inLines) {
			stringBuilder.append(line);
			stringBuilder.append(linefeed);
		}
		Files.write(outFile, stringBuilder.toString().getBytes());
	}

	/**
	 * Compares 2 source directories and deletes all files in otherSourceDir that are not in baseSourceDir
	 * @param baseSourceDir The source dir to compare
	 * @param otherSourceDir The source dir to delete from
	 * @param version The version to check, used in logging
	 */
	public static void deleteExcessFiles(Path baseSourceDir, Path otherSourceDir, String version) {
		BetterFileWalker.walk(otherSourceDir, relativePathToDelete -> {
			// Verify if file exists in base source dir
			Path baseFile = baseSourceDir.resolve(relativePathToDelete);
			if (!Files.exists(baseFile)) {
				System.out.println(String.format("Deleting %s%s%s%s%s in version %s%s%s", relativePathToDelete.getParent(), File.separator, RED, relativePathToDelete.getFileName().toString(), WHITE, CYAN, version, WHITE));
				Path absolutePathToDelete = otherSourceDir.resolve(relativePathToDelete);
				SafeFileOperations.delete(absolutePathToDelete);

				// Delete parentDirectory if it's empty
				Path parentDir = absolutePathToDelete.getParent();
				boolean isEmpty;
				try {
					isEmpty = Files.isDirectory(parentDir) && Files.list(parentDir).count() == 0L;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				if (isEmpty) {
					SafeFileOperations.delete(parentDir);
				}
			}
		});
	}

	public LinePreprocessor getLineProcessor() {
		return processor;
	}

	public WildcardFileFilter getFileFilter() {
		return fileFilter;
	}

}
