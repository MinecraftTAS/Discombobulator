package com.minecrafttas.discombobulator.utils;

import static com.minecrafttas.discombobulator.utils.Colors.GREEN;
import static com.minecrafttas.discombobulator.utils.Colors.WHITE;
import static com.minecrafttas.discombobulator.utils.Colors.YELLOW;

import com.minecrafttas.discombobulator.Discombobulator;

/**
 * Helper for making the line seperator, which is used by the preprocessor, configurable
 * 
 * @author Scribble
 */
public class LineFeedHelper {

	///
	/// Prints information about the current line feed character to the console.
	///
	/// Including whether Disco is using the default line seperator or not
	/// @throws Exception
	///
	public static void printMessage() throws Exception {
		LineFeedHelper.checkLineFeed(Discombobulator.DEFAULT_LINE_FEED);
		String property = System.getProperty("line.seperator");
		if ("\\n".equals(property) || "\n".equals(property)) {
			System.out.println(String.format("Preprocessing with line seperator %s\\n%s\n", GREEN, WHITE));
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			System.out.println(String.format("Preprocessing with line seperator %s\\r\\n%s\n", YELLOW, WHITE));
		} else {
			property = Discombobulator.DEFAULT_LINE_FEED;
			System.out.println(String.format("Preprocessing with default line seperator %s%s%s\nTo change this, add %s-Dline.seperator=\"%s\"%s to VM arguments\n", YELLOW, Discombobulator.DEFAULT_LINE_FEED.equals("\r\n") ? "\\r\\n" : "\\n", WHITE, YELLOW, Discombobulator.DEFAULT_LINE_FEED.equals("\r\n") ? "\\n" : "\\r\\n", WHITE));
		}
	}

	///
	/// Checks the line feed character
	/// @param lineFeed The {@link Discombobulator#DEFAULT_LINE_FEED}
	/// @throws Exception If the defaultLineFeed is not a line feed character
	///
	public static void checkLineFeed(String lineFeed) throws Exception {
		if (!isLineFeed(lineFeed)) {
			throw new Exception("Property defaultLineFeed is neither \"\\r\\n\" nor \"\\n\"");
		}
	}

	/**
	 * <p>Get's the currently active line feed character(s),<br>
	 * based on the <code>line.separator</code> system property,<br>
	 * or the {@link Discombobulator#DEFAULT_LINE_FEED} gradle property
	 * 
	 * @return Either "\r\n" or "\n"
	 */
	public static String newLine() {
		String out = System.lineSeparator();
		String property = System.getProperty("line.seperator");
		if (property == null) {
			property = Discombobulator.DEFAULT_LINE_FEED;
		}
		if ("\\n".equals(property) || "\n".equals(property)) {
			out = "\n";
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			out = "\r\n";
		}
		return out;
	}

	/**
	 * Checks if the given string is one of 2 line feed characters
	 * 
	 * @param chars The string
	 * @return True if line feed
	 */
	public static boolean isLineFeed(String chars) {
		return switch (chars) {
			case "\n":
			case "\r\n":
				yield true;
			default:
				yield false;
		};
	}
}
