package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Processor2 {
	private final Pattern regexBlocks = Pattern.compile("\\/\\/ *# (.+)");
	private final Pattern regexPatterns = Pattern.compile("\\/\\/ *@(.+)");
	
	private List<String> versions;
	private Map<String, Map<String, String>> patterns;
	
	private VersionState state = VersionState.NONE;

	/**
	 * The current version of the preprocessor block that is processed. <br>
	 * Null if the lines are outside of a preprocessor block.
	 */
	private String currentVersion = null;
	
	/**
	 * If the list should be inverted with the first one being the default
	 */
	private final boolean inverted;
	
	/**
	 * Debug filename for errors during preprocessing
	 */
	private String filename = "undefined";
	
	/**
	 * Debug linecount for errors during preprocessing
	 */
	private int linecount = 0;
	private boolean versionEnabled;
	
	/**
	 * Creates a new processor. The default will be the lowest version.
	 * 
	 * <pre>
	 * [
	 * "1.20",
	 * "1.19",
	 * "1.18",
	 * "1.16" <- default version since it's the lowest
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public Processor2(List<String> versions, Map<String, Map<String, String>> patterns) {
		this(versions, patterns, false);
	}
	
	/**
	 * Creates a new processor. The order can be inverted.
	 * <p> If inverted is true:
	 * 
	 * <pre>
	 * [
	 * "1.20", <- default version since it's the highest
	 * "1.19",
	 * "1.18",
	 * "1.16"
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public Processor2(List<String> versions, Map<String, Map<String, String>> patterns, boolean inverted) {
		this.versions = versions;
		this.patterns = patterns;
		this.inverted = inverted;
	}
	
	/**
	 * Preprocesses the lines to a targeted version.
	 * <p>Preprocessing can happen in 2 ways:<br>
	 * <ol>
	 * <li>Preprocessing a block of code</li>
	 * <li>Preprocessing a specific line of code with patterns</li>
	 * </ol>
	 * <p>The advantage of pattern processing is that you can save yourself some work making a version block for common patterns like <code>Minecraft.getMinecraft().player</code> in 1.12 to <code>Minecraft.getMinecraft().thePlayer</code> in e.g. 1.10
	 *
	 * <h2>VersionBlock</h2>
	 * <p>A version block allows you to uncomment part of the code in certain versions via comments:
	 * <pre>
	 * //# 1.12.2
	 * 	The code you want to write in 1.12.2
	 * //# 1.9.4
	 * //$$	The code you want to write in 1.9.4
	 * //# def
	 * //$$	Default code that is below 1.9.4
	 * //# end
	 * </pre>
	 * <p>Blocks start with a //# mcversion and end with a //# end. If the targeted version is not defined in the block, the next lowest version will be chosen for preprocessing (e.g. target is 1.11, doesn't exist, so 1.9.4 block will be preprocessed).<br>
	 * You can define a default block that defaults to the lowest version you specified (e.g. 1.9.2 is lower than 1.9.4, no other version in lower, so it uses default). <br>
	 * 
	 * <p>You can define these blocks out of order (e.g. first 1.9.4 then 1.12.2), but the order is ultimately defined in the build.gradle under discombobulator.
	 * 
	 * <h2>Patterns</h2>
	 * Patterns are defined in the build.gradle:
	 * 
	 * <pre>
	 * patterns = [
	 * 	GetWindow: [
	 *		"def": "mc.window",
	 *		"1.15.2": "mc.getWindow()"
	 *	]
	 * ]
	 * </pre>
	 * <p> Each pattern has a name and multiple versions, with a default under "def".<br>
	 * To apply patterns, we have to write a <code>//@ patternname</code> in the line we want to apply:
	 * 
	 * <pre>
	 * mc.window; // @GetWindow
	 * </pre>
	 * 
	 * @param targetVersion The version for which lines should be enabled
	 * @param lines The lines to preprocess
	 * @param filename Debug filename for errors during preprocessing
	 * @return The preprocessed lines of the file
	 */
	public List<String> preprocess(String targetVersion, List<String> lines, String filename) {
		List<String> out = new ArrayList<>();
		this.filename = filename;
		this.linecount = 0;
		
		for (String line : lines) {
			linecount++;
			line = preprocessVersionBlock(line, targetVersion);
			line = preprocessPattern(line);
			out.add(line);
		}
		
		return out;
	}

	private String preprocessVersionBlock(String line, String targetVersion) {
		if(updateCurrentVersion(line)) {
			updateEnabled(targetVersion);
			return line;
		}
		return enableLine(line, this.versionEnabled);
	}
	
	/**
	 * Updates the enabled status for the current block
	 * If the target version is smaller or equal than the {@link #currentVersion}, {@link #versionEnabled} will be true. (If {@link #inverted} is false)
	 * @param targetVersion
	 */
	private void updateEnabled(String targetVersion) {
		if(currentVersion == null) { // Check if we are outside a version block
			versionEnabled = false;
			return;
		}
		if (state == VersionState.FOUND) { // Check if the target version was already found
			versionEnabled = false;
			return;
		}
		else if (state == VersionState.NONE) {
			if (targetVersion.equals("def")) { // Set the default version as the target version
				targetVersion = getDefault();
			}
			// Getting the target and current index in the version. The higher the index the
			// "lower" the mc version
			int targetIndex = versions.indexOf(targetVersion);
			int currentIndex = versions.indexOf(currentVersion);

			if (currentIndex == -1) {
				throw new RuntimeException(String.format("The specified version %s in %s in line %s was not found", currentVersion, filename, linecount));
			}
			if (targetIndex > currentIndex && !inverted) {
				versionEnabled = false;
			}
			else if (targetIndex < currentIndex && inverted) {
				versionEnabled = false;
			}
			else {								// If the targetIndex is equal or smaller than the currentIndex
				state = VersionState.FOUND;
				versionEnabled = true;
			}
		}
	}

	/**
	 * Comments out or uncomments a line.
	 * @param line The line to change
	 * @param enable True if line should be enabled
	 * @return The new line
	 */
	private String enableLine(String line, boolean enable) {
		boolean isDisabled = line.startsWith("//$$");
		if (enable)
			return isDisabled ? line.replace("//$$", "") : line;
		else
			return isDisabled ? line : "//$$" + line;
	}

	/**
	 * If the line contains any version block statements detected by {@linkplain #regexBlocks}, the {@link #currentVersion} will be updated
	 * 
	 * @param line The line to check
	 * @return If the line contains a versionBlock statement
	 */
	private boolean updateCurrentVersion(String line) {
		// Block detection
		Matcher match = this.regexBlocks.matcher(line);
		if (match.find()) {
			String detectedVersion = match.group(1);
			
			if (detectedVersion.equalsIgnoreCase("end")) {	// Process end
				state = VersionState.NONE;
				currentVersion = null;
				return true;
			}
			else if(detectedVersion.equalsIgnoreCase("def")) { // Process default version
				currentVersion = getDefault();
				return true;
			}
			else {
				currentVersion = detectedVersion;
				return true;
			}
		}
		return false;
	}
	
	private String preprocessPattern(String line) {
		Matcher match = this.regexPatterns.matcher(line);
		if (!match.find()) {
			return line;
		}
		
		// find pattern
		String patternName = match.group(1);
		Map<String, String> pattern = this.patterns.get(patternName);
		if (pattern == null)
			throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found", patternName, filename, linecount));
		// TODO continue here
		return line;
	}
	
	private String getDefault() {
		if(inverted) {
			return versions.get(0);
		} else {
			return versions.get(versions.size()-1);
		}
	}
	
	/**
	 * The processor tries to search for a matching version in the lines.<br>
	 * Under certain circumstances the version changes depending on the state.
	 * <p>(Yes this enum can be swapped out for a boolean but I think this is better for understanding)
	 * 
	 * @author Scribble
	 *
	 */
	private enum VersionState{
		/**
		 * The "none" state indicates that the target version does not match the current version and we still have to look for a suitable version.
		 */
		NONE,
		/**
		 * The "found" state indicates that the target version has been found and all versions beyond that can be disabled.
		 */
		FOUND
	}
	
}