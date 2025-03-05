package com.minecrafttas.discombobulator.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.minecrafttas.discombobulator.Discombobulator;

/**
 * Safety improvements to some file operations
 * @author Pancake
 */
public class SafeFileOperations {

	/**
	 * Moves a file to recycle bin instead of deleting it
	 * @param file The file to remove
	 */
	public static void delete(Path file) {
		if (!Files.exists(file)) {
			Discombobulator.printError("Can't delete file as it does not exist: " + file);
			return;
		}
		if (Desktop.isDesktopSupported())
			Desktop.getDesktop().moveToTrash(file.toFile());
		else
			try {
				Files.delete(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	///
	/// For the times when you don't want to fill your trashbin, do the opposite of "safe"
	///
	public static void nuke(Path file) {
		if (!Files.exists(file)) {
			Discombobulator.printError("Can't nuke file as it does not exist: " + file);
			return;
		}
		try {
			Files.delete(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes to a file but clears it first
	 * @param path File to write to
	 * @param lines Lines to write to file
	 * @param options Options for writing to file
	 */
	public static void write(Path path, List<String> lines, StandardOpenOption... options) {
		try {
			// Clear file
			var w = new PrintWriter(path.toFile());
			w.print("");
			w.close();
			// Write to file
			Files.write(path, lines, options);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
