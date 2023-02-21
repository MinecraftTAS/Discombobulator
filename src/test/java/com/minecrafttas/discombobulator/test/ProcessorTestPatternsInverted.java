package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;
import com.minecrafttas.discombobulator.utils.Pair;

class ProcessorTestPatternsInverted extends TestBase{

	Map<String, Map<String, String>> patterns = Map.of(
			"GetLevel", Map.of(
				"def", "level",
				"1.12.2", "world"
			),
			"GetMinecraft", Map.of(
				"def", "Minecraft.getInstance()",
				"1.12.2", "Minecraft.getMinecraft()"
			)
		);
	
	private List<String> allVersions = Arrays.asList(
			"1.16.1",
			"1.15.2",
			"1.14.4",
			"1.13.2",
			"1.12.2",
			"1.11.2"
	);

	private Processor processor=new Processor(allVersions, patterns, true);
	
	/**
	 * TargetVersion: 1.14.4
	 * Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testPattern1() throws IOException {
		String folder = "TestPatternsInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.14.4";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.13.2
	 * Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testPattern2() throws IOException {
		String folder = "TestPatternsInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.13.2";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.12.2
	 * Expected: 1.11.2
	 * @throws IOException
	 */
	@Test
	void testPattern3() throws IOException {
		String folder = "TestPatternsInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.12.2.txt";
		String targetVersion = "1.11.2";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.16.1
	 * Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testPattern4() throws IOException {
		String folder = "TestPatternsInverted";
		String actualName = "Actual.java";
		String expectedName = "Expected1.14.4.txt";
		String targetVersion = "1.16.1";
		
		Pair<List<String>, List<String>> lines = getLines(folder, actualName, expectedName);
		
		List<String> linesActual = processor.preprocess(targetVersion, lines.left(), "Actual", FileNameUtils.getExtension(actualName));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", lines.right());
		
		assertEquals(expected, actual);
	}
	
}
