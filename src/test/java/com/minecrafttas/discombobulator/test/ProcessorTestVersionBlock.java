package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor2;

class ProcessorTestVersionBlock {

	
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
	
	
	private Processor2 processor=new Processor2(allVersions, null);

	/**
	 * TargetVersion: 1.18.1
	 * Expected: 1.18.1
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingExact() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test1/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test1/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.18.1", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.16.5
	 *	Expected: 1.16.1
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingAbove() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test1/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test1/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.16.5", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: infinity
	 *	Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingAboveDefault() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test1/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test1/Expected3.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("infinity", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.14.4
	 *	Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingDefault() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test1/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test1/Expected3.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.14.4", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.21
	 *	Expected: Fail
	 * @throws IOException
	 */
	@Test
	void testTargetVersionTooHigh() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test1/Actual.java"), StandardCharsets.UTF_8);
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.21", linesBase, "Actual");
		});

		assertEquals("The target version 1.21 was not found", exception.getMessage());
	}

	/**
	 *	TargetVersion: 1.16.1
	 *	Expected: Fail
	 * @throws IOException
	 */
	@Test
	void testNonExistingVersion() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test2/Actual.java"), StandardCharsets.UTF_8);
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.16.1", linesBase, "Actual");
		});

		assertEquals("The specified version CrazyVersionName in Actual in line 6 was not found", exception.getMessage());
	}
}
