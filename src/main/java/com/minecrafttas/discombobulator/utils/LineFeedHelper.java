package com.minecrafttas.discombobulator.utils;

import static com.minecrafttas.discombobulator.utils.Colors.GREEN;
import static com.minecrafttas.discombobulator.utils.Colors.WHITE;
import static com.minecrafttas.discombobulator.utils.Colors.YELLOW;

public class LineFeedHelper {

	public static void printMessage() {
		String property = System.getProperty("line.seperator");
		if ("\\n".equals(property) || "\n".equals(property)) {
			System.out.println(String.format("Preprocessing with line seperator %s\\n%s\n", GREEN, WHITE));
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			System.out.println(String.format("Preprocessing with line seperator %s\\r\\n%s\n", YELLOW, WHITE));
		} else {
			System.out.println(String.format("Preprocessing with default line seperator %s%s%s\nTo change this, add %s-Dline.seperator=\"\\n\"%s to VM arguments\n", YELLOW, System.lineSeparator().equals(property) ? "\\r\\n" : "\\n", WHITE, YELLOW, WHITE));
		}
	}

	public static String newLine() {
		String out = System.lineSeparator();
		String property = System.getProperty("line.seperator");
		if ("\\n".equals(property) || "\n".equals(property)) {
			out = "\n";
		} else if ("\\r\\n".equals(property) || "\r\n".equals(property)) {
			out = "\r\n";
		}
		return out;
	}
}
