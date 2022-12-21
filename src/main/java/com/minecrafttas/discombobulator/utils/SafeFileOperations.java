package com.minecrafttas.discombobulator.utils;

import java.awt.Desktop;
import java.io.File;

/**
 * Safety improvements to some file operations
 * @author Pancake
 */
public class SafeFileOperations {

	/**
	 * Moves a file to recycle bin instead of deleting it
	 * @param f File
	 */
	public static void delete(File f) {
		if (Desktop.isDesktopSupported())
			Desktop.getDesktop().moveToTrash(f);
		else
			f.delete();
	}

}
