package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class ProcessorTestAC extends TestBase{

	private List<String> allVersions = Arrays.asList(
			"1.20.0",
			"1.19.3",
			"1.19.2",
			"1.19.0",
			"1.18.2",
			"1.18.1",
			"1.17.1",
			"1.16.5",
			"1.16.1",
			"infinity",
			"1.15.2",
			"1.14.4"
	);
	
	
	private Processor processor=new Processor(allVersions, null);
	
	/**
	 * TargetVersion: 1.14.4
	 * Expected: 1.14.4
	 * @throws Exception 
	 */
	@Test
	void testTargetVersionBeingExact() throws Exception {
		String folder = "TestAccesswidener";
		String actualName = "test.accesswidener";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.14.4";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.18.1
	 * Expected: 1.16.1
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingAbove() throws Exception {
		String folder = "TestAccesswidener";
		String actualName = "test.accesswidener";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.18.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: Null
	 * Expected: None
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingNull() throws Exception {
		String folder = "TestAccesswidener";
		String actualName = "test.accesswidener";
		String expectedName = "ExpectedNone.txt";
		String targetVersion = null;
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
}
