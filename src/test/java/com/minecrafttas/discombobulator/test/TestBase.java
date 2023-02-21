package com.minecrafttas.discombobulator.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import com.minecrafttas.discombobulator.utils.Pair;

public class TestBase {
	
	protected Pair<List<String>, List<String>> getLines(String folder, String actualName, String expectedName) throws IOException{
		
		List<String> linesBase = null;
		if(actualName!=null) {
			File actualFile = new File(String.format("src/test/resources/%s/%s", folder, actualName));
			linesBase = FileUtils.readLines(actualFile, StandardCharsets.UTF_8);
		}
		
		List<String> linesExpected = null;
		if(expectedName!=null) {
			File expectedFile = new File(String.format("src/test/resources/%s/%s", folder, expectedName));
			linesExpected = FileUtils.readLines(expectedFile, StandardCharsets.UTF_8);
		}
		
		
		return Pair.of(linesBase, linesExpected);
	}
}
