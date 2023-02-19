package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;

class ProcessorTestAC {

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
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingExact() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/test.accesswidener"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.14.4", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/TestAccesswidener/test.accesswidener"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}

	/**
	 * TargetVersion: 1.18.1
	 * Expected: 1.16.1
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingAbove() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/test.accesswidener"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.18.1", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/TestAccesswidener/test.accesswidener"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: Null
	 * Expected: None
	 * @throws IOException
	 */
	@Test
	void testTargetVersionBeingNull() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/test.accesswidener"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/TestAccesswidener/Expected3.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess(null, linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/TestAccesswidener/test.accesswidener"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
}
