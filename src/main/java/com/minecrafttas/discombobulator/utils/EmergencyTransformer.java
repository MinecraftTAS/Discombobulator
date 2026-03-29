package com.minecrafttas.discombobulator.utils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

public class EmergencyTransformer {

	private Map<String, Map<String, Pair<String, String>>> config;

	public EmergencyTransformer(Map<String, Map<String, Pair<String, String>>> config, List<String> versionStrings) {
		this.config = config;
	}

	public List<String> transform(String sourceVersion, String targetVersion, Path file, List<String> lines) {

		for (Entry<String, Map<String, Pair<String, String>>> filePattern : config.entrySet()) {
			WildcardFileFilter filter = WildcardFileFilter.builder().setWildcards(filePattern.getKey()).get();
			if (filter.matches(file)) {
				Map<String, Pair<String, String>> versions = filePattern.getValue();
				replaceVersion(targetVersion, lines, versions);
			}
		}

		return lines;
	}

	private void replaceVersion(String targetVersion, List<String> lines, Map<String, Pair<String, String>> versions) {
		for (Entry<String, Pair<String, String>> version : versions.entrySet()) {
			if (targetVersion.equals(version.getKey())) {
				replaceLines(lines, version);
			}
		}
	}

	private void replaceLines(List<String> lines, Entry<String, Pair<String, String>> version) {
		Pair<String, String> searchReplace = version.getValue();
		String search = searchReplace.left();
		String replace = searchReplace.right();

		lines.forEach(line -> {
			line = line.replace(search, replace);
		});
	}
}
