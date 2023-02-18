package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;

class ProcessorTestVersionBlockInverted {

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
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingExact() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.8.9", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.7.10
	 *	Expected: 1.8.9
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingBelow() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.7.10", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.12
	 *	Expected: 1.12.2
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingBelowDefault() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.12", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}
	
	/**
	 *	TargetVersion: 1.12.2
	 *	Expected: 1.12.2
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingDefault() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.12.2", linesBase, "Actual");
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(actual, expected);
	}

	/**
	 *	TargetVersion: 1.1
	 *	Expected: Fail
	 * @throws IOException
	 */
	@Test
	void testTargetVersionTooLow() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestVersionInverted/Actual.java"), StandardCharsets.UTF_8);
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.1", linesBase, "Actual");
		});

		assertEquals("The target version 1.1 was not found", exception.getMessage());
	}
}
