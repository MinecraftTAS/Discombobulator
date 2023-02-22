package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class ProcessorTestVersionBlockInverted extends TestBase{

	private List<String> allVersions = Arrays.asList(
			"1.12.2",
			"1.12",
			"1.11.2",
			"1.8.9",
			"1.7.10"
	);
	
	
	private Processor processor=new Processor(allVersions, null, true);
	
	/**
	 * TargetVersion: 1.8.9
	 * Expected: 1.8.9
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingExact() throws Exception {
		String folder = "TestVersionInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.8.9.txt";
		String targetVersion = "1.8.9";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 *	TargetVersion: 1.7.10
	 *	Expected: 1.8.9
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingBelow() throws Exception {
		String folder = "TestVersionInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.8.9.txt";
		String targetVersion = "1.7.10";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 *	TargetVersion: 1.12
	 *	Expected: 1.12.2
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingBelowDefault() throws Exception {
		String folder = "TestVersionInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = "1.12";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 *	TargetVersion: 1.12.2
	 *	Expected: 1.12.2
	 * @throws Exception
	 */
	@Test
	void testTargetVersionBeingDefault() throws Exception {
		String folder = "TestVersionInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = "1.12.2";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}

	/**
	 *	TargetVersion: 1.1
	 *	Expected: Fail
	 * @throws Exception
	 */
	@Test
	void testTargetVersionTooLow() throws Exception {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.1", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/TestVersionInverted/Actual.java"));
		});

		assertEquals("The target version 1.1 was not found", exception.getMessage());
	}
}
