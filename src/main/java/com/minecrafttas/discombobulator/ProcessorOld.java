package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class ProcessorOld {

	private Pattern regexBlocks;
	private Pattern regexPatterns;

	private List<String> versions;
	private Map<String, Map<String, String>> patterns;

	/**
	 * Creates a preprocessor and initializes the regex
	 */
	public ProcessorOld() {
		this.regexBlocks = Pattern.compile("\\/\\/ *# (.+)");
		this.regexPatterns = Pattern.compile("\\/\\/ *@(.+)");
	}

	/**
	 * Initializes the preprocessor with a list of versions and patterns
	 * @param versions List of versions
	 * @param patterns Map of patterns
	 */
	public void initialize(List<String> versions, Map<String, Map<String, String>> patterns) {
		this.versions = versions;
		this.patterns = patterns;
	}

	/**
	 * Preprocesses the given lines to targetVersion
	 * @param targetVersion Preprocess target
	 * @param lines Lines to preprocess
	 * @param filename Filename
	 * @return Preprocessed Lines
	 */
	public List<String> preprocess(String targetVersion, List<String> lines, String filename) {
		// Specific version for current line or null
		String currentVersion = null;
		int master = -1;
		
		List<String> blocks = new ArrayList<>();

		// Switch version blocks
		for (String line : lines) {
			// Pattern detection
			Matcher match = this.regexBlocks.matcher(line);
			if (match.find()) {
				String ver = currentVersion = match.group(1);
				master = -1;
				if (ver.equalsIgnoreCase("end")) {
					currentVersion = null;
					this.overflow = false;
					this.perfectMatch = false;
				}
				blocks.add(line);
				continue;
			}

			// Change lines accordingly
			if (currentVersion != null) {
				if (master == -1) {
					master = this.isVersionEnabled(targetVersion, currentVersion, filename, lines.indexOf(line)) ? 2 : 1;
				}
				if (master == 2 && targetVersion != null) {
					String changedLine = line;
					if (line.startsWith("//$$"))
						changedLine = line.replace("//$$", "");

					blocks.add(changedLine);
				} else {
					String changedLine = line;
					if (!line.startsWith("//$$"))
						changedLine = "//$$" + line;

					blocks.add(changedLine);
				}
				continue;
			}
			blocks.add(line);
		}

		List<String> out = new ArrayList<>();

		// Apply patterns
		for (String line : blocks) {
			Matcher match = this.regexPatterns.matcher(line);
			if (match.find()) {
				// find pattern
				String type = match.group(1);
				Map<String, String> pattern = this.patterns.get(type);
				if (pattern == null)
					throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found", type, filename, line));
				// find current version
				String toReplace = null;
				for (Entry<String, String> entry : pattern.entrySet())
					if (line.contains(entry.getValue())) {
						toReplace = entry.getValue();
						break;
					}
				if (toReplace == null)
					throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found for any version", type, filename, line));
				// find replacement
				String toReplaceWith = findLowestReplacement(pattern, targetVersion);
				if (toReplaceWith == null)
					throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found for target version %s", type, filename, line, targetVersion));
				out.add(line.replace(toReplace, toReplaceWith));
				continue;
			}
			out.add(line);
		}

		return out;
	}

	/**
	 * Finds the lowest pattern replacement for any target version
	 * @param pattern Pattern
	 * @param targetVersion Target version
	 * @return Replacement
	 */
	private String findLowestReplacement(Map<String, String> pattern, String targetVersion) {
		String replacement = null;
		int targetVer = this.versions.indexOf(targetVersion);
		
		// Find index of target version
		for (Entry<String, String> entry : pattern.entrySet()) {
			int i = this.versions.indexOf(entry.getKey());
			if ("def".equals(entry.getKey())) {
				i = this.versions.size() - 1;
			}
			
			// Break if version too high
			if (targetVer > i)
				break;
			
			replacement = entry.getValue();
		}
		return replacement;
	}

	/**
	 * If you your target is e.g. 1.16.5,
	 * but you do not have exactly 1.16.5 in the preprocessor block,
	 * the next lowest version is used:
	 *
	 * (Target is 1.16.5)
	 *
	 * <pre>
	 * //# 1.18.1
	 * //$$ Code for 1.18.1 and up
	 * //# 1.16.1
	 * Code for 1.16.1 and up
	 * //# 1.15.2
	 * //$$ Code for 1.15.2 and up
	 * </pre>
	 *
	 * Overflow is true, when the current version goes past the target and the next lowest is found
	 * @see {@link #isVersionEnabled(String, String)}
	 */
	private boolean overflow = false;

	/**
	 * True when the versions match up perfectly
	 * @see {@link #isVersionEnabled(String, String)}
	 */
	private boolean perfectMatch = false;

	/**
	 * Returns whether version specific code should be enabled on the target version or not
	 * @param targetVersion Target version
	 * @param currentVersion Version of the version specific code
	 * @param filename Filename
	 * @param line Current line in the file
	 * @return Is version enabled?
	 */
	private boolean isVersionEnabled(String targetVersion, String currentVersion, String filename, int line) {
		var targetVer = -1;
		var currentVer = -1;

		// Find index of current and target version
		for (String version : this.versions) {
			var i = this.versions.indexOf(version);
			if (version.equals(targetVersion))
				targetVer = i;
			if (version.equals(currentVersion))
				currentVer = i;
		}
		if ("def".equals(currentVersion))
			currentVer = this.versions.size() - 1;

		if (currentVer == -1)
			throw new RuntimeException(String.format("The specified version %s in %s in line %s was not found", currentVersion, filename, line));

		if (targetVer > currentVer)
			return false;

		if (targetVer == currentVer) {
			this.perfectMatch = true;
			return true;
		}

		if (this.perfectMatch || this.overflow)
			return false;

		this.overflow = true;
		return true;
	}
}
