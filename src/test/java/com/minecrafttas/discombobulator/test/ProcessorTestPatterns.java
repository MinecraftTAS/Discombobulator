package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gradle.internal.impldep.org.apache.commons.compress.utils.FileNameUtils;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;

class ProcessorTestPatterns {

	Map<String, Map<String, String>> patterns = Map.of(
			"GetLevel", Map.of(
				"1.14.4", "level",
				"def", "world"
			),
			"GetMinecraft", Map.of(
				"1.14.4", "Minecraft.getInstance()",
				"def", "Minecraft.getMinecraft()"
			)
		);
	
	private List<String> allVersions = Arrays.asList(
			"1.15.2",
			"1.14.4",
			"1.13.2",
			"1.12.2",
			"1.11.2"
	);

	private Processor processor=new Processor(allVersions, patterns);
	
	/**
	 * TargetVersion: 1.14.4
	 * Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testPattern1() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test3/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test3/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.14.4", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/Test3/Actual.java"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.15.2
	 * Expected: 1.14.4
	 * @throws IOException
	 */
	@Test
	void testPattern2() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test3/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test3/Expected1.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.15.2", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/Test3/Actual.java"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.12.2
	 * Expected: 1.12.2
	 * @throws IOException
	 */
	@Test
	void testPattern3() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test3/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test3/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.12.2", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/Test3/Actual.java"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.11.2
	 * Expected: 1.12.2
	 * @throws IOException
	 */
	@Test
	void testPattern4() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test3/Actual.java"), StandardCharsets.UTF_8);
		List<String> linesExpected = FileUtils.readLines(new File("src/test/resources/Test3/Expected2.txt"), StandardCharsets.UTF_8);
		
		List<String> linesActual = processor.preprocess("1.11.2", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/Test3/Actual.java"));
		
		String actual = String.join("\n", linesActual);
		String expected = String.join("\n", linesExpected);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * TargetVersion: 1.14.2
	 * Expected: Fail
	 * @throws IOException
	 */
	@Test
	void testNonExistingPattern() throws IOException {
		List<String> linesBase = FileUtils.readLines(new File("src/test/resources/Test4/Actual3.java"), StandardCharsets.UTF_8);
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.14.4", linesBase, "Actual", FileNameUtils.getExtension("src/test/resources/Test3/Actual.java"));
		});

		assertEquals("The specified pattern  GetMinecraft , GetLevel in Actual in line 		Minecraft.getInstance(); // @ GetMinecraft , GetLevel; was not found for any version", exception.getMessage());
	}

}
