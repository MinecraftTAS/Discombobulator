package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class TestVersionNesting extends TestBase {

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
	 * TargetVersion: 1.16.1
	 * Expected: 1.16.1 only
	 * @throws Exception
	 */
	@Test
	void testTargetVersion() throws Exception {
		String folder = "TestNesting/first";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.16.5
	 * Expected: 1.16.1 and 1.16.5
	 * @throws Exception
	 */
	@Test
	void testTargetAndNestingVersion() throws Exception {
		String folder = "TestNesting/first";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.16.5";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.17.1
	 * Expected: 1.16.1 and 1.16.5
	 * @throws Exception
	 */
	@Test
	void testTargetAndNestingVersionAbove() throws Exception {
		String folder = "TestNesting/first";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.17.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.14.4
	 * Expected: 1.14.4
	 * @throws Exception
	 */
	@Test
	void testTargetAndNestingDefault() throws Exception {
		String folder = "TestNesting/first";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.14.4";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	// =======================================================
	
	/**
	 * TargetVersion: 1.16.1
	 * Expected: 1.16.1 only
	 * @throws Exception
	 */
	@Test
	void testTripleNesting() throws Exception {
		String folder = "TestNesting/second";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.1.txt";
		String targetVersion = "1.16.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.16.5
	 * Expected: 1.16.1, 1.16.5
	 * @throws Exception
	 */
	@Test
	void testTripleNesting2() throws Exception {
		String folder = "TestNesting/second";
		String actualName = "Actual.java";
		String expectedName = "Expected1.16.5.txt";
		String targetVersion = "1.16.5";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.17.1
	 * Expected: 1.16.1, 1.16.5, 1.17.1
	 * @throws Exception
	 */
	@Test
	void testTripleNesting3() throws Exception {
		String folder = "TestNesting/second";
		String actualName = "Actual.java";
		String expectedName = "Expected1.17.1.txt";
		String targetVersion = "1.17.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.18.2
	 * Expected: 1.16.1, 1.16.5, 1.18.2
	 * @throws Exception
	 */
	@Test
	void testTripleNesting4() throws Exception {
		String folder = "TestNesting/second";
		String actualName = "Actual.java";
		String expectedName = "Expected1.18.2.txt";
		String targetVersion = "1.18.2";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
}
