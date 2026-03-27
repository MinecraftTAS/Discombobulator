package com.minecrafttas.discombobulator.utils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

public class EmergencyTransformer {

	private Map<String, Map<String, Pair<String, String>>> config;

	public EmergencyTransformer(Map<String, Map<String, Pair<String, String>>> config) {
		this.config = config;
	}

	public List<String> transform(String targetVersion, Path file, List<String> lines) {
		Map<String, Pair<String, String>> version = config.getOrDefault(targetVersion, new HashMap<>());

		for (Entry<String, Pair<String, String>> filePatterns : version.entrySet()) {
			String pattern = filePatterns.getKey();
			Pair<String, String> searchReplace = filePatterns.getValue();
			String search = searchReplace.left();
			String replace = searchReplace.right();

			replaceLines(file, lines, pattern, search, replace);
		}

		return lines;
	}

	public List<String> reverseTransform(String targetVersion, Path file, List<String> lines) {
		Map<String, Pair<String, String>> version = config.getOrDefault(targetVersion, new HashMap<>());

		for (Entry<String, Pair<String, String>> filePatterns : version.entrySet()) {
			String pattern = filePatterns.getKey();
			Pair<String, String> searchReplace = filePatterns.getValue();
			String search = searchReplace.left();
			String replace = searchReplace.right();

			replaceLines(file, lines, pattern, replace, search);
		}

		return lines;
	}

	private void replaceLines(Path file, List<String> lines, String pattern, String search, String replace) {
		WildcardFileFilter filter = WildcardFileFilter.builder().setWildcards(pattern).get();
		if (filter.matches(file)) {
			lines.forEach(line -> {
				line = line.replace(search, replace);
			});
		}
	}
}
