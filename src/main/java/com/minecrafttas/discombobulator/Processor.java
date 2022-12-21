package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Processor {

	private Pattern regex;
	private List<String> versions;
	@SuppressWarnings("unused") // TODO: Implement patterns
	private Map<String, Map<String, String>> patterns;

	public Processor() {
		this.regex = Pattern.compile("\\/\\/ *# (.+)");
	}

	public void initialize(List<String> versions, Map<String, Map<String, String>> patterns) {
		this.versions = versions;
		this.patterns = patterns;
	}

	public List<String> preprocess(String targetVersion, List<String> lines, String filename) {
		// Specific version for current line or null
		String currentVersion = null;

		List<String> out = new ArrayList<>();

		// Switch version blocks
		for (String line : lines) {
			// Pattern detection
			var match = this.regex.matcher(line);
			if (match.find()) {
				var ver = currentVersion = match.group(1);
				if (ver.equalsIgnoreCase("end")) {
					currentVersion = null;
					this.overflow = false;
					this.perfectMatch = false;
				}
				out.add(line);
				continue;
			}

			// Change lines accordingly
			if (currentVersion != null) {
				if (this.isVersionEnabled(targetVersion, currentVersion, filename, lines.indexOf(line))) {
					var changedLine = line;
					if (line.startsWith("//$$"))
						changedLine = line.replace("//$$", "");

					out.add(changedLine);
				} else {
					var changedLine = line;
					if (!line.startsWith("//$$"))
						changedLine = "//$$" + line;

					out.add(changedLine);
				}
				continue;
			}
			out.add(line);
		}
		return out;
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

		if (currentVer == -1)
			throw new RuntimeException(String.format("The specified version %s in %s in line %s was not found", currentVersion, filename, line));

		if (targetVer == -1)
			throw new RuntimeException(String.format("The specified version %s was not found in the list of all versions", targetVersion));

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
