package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.minecrafttas.discombobulator.Processor;

class ProcessorTest {

	// @formatter:off
	List<String> allVersions = Arrays.asList(
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

	Map<String, Map<String, String>> patterns = Map.of(
		"GetLevel", Map.of(
			"1.14.4", "Level level = mc.level",
			"1.12.2", "World level = mc.world"
		),
		"GetMinecraft", Map.of(
			"1.14.4", "Minecraft.getInstance()",
			"1.12.2", "Minecraft.getMinecraft()"
		)
	);
	// @formatter:on

	@Test
	void testPreprocessFirst() throws Exception {
		var processor = new Processor();
		processor.initialize(this.allVersions, null);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile1.java"), StandardCharsets.UTF_8);

		var actual = processor.preprocess("1.18.1", lines, "TestFile1");

		List<String> expected = List.of("public class TestFile1 {", "	//# 1.18.1", "	// Code for 1.18.1 and up", "	//# 1.16.1", "//$$	// Code for 1.16.1 and up", "	//# end", "}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessSecond() throws Exception {
		var processor = new Processor();
		processor.initialize(this.allVersions, null);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile1.java"), StandardCharsets.UTF_8);

		var actual = processor.preprocess("1.16.1", lines, "TestFile1");

		List<String> expected = List.of("public class TestFile1 {", "	//# 1.18.1", "//$$	// Code for 1.18.1 and up", "	//# 1.16.1", "	// Code for 1.16.1 and up", "	//# end", "}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessTooNew() throws Exception {
		var processor = new Processor();
		processor.initialize(this.allVersions, null);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile1.java"), StandardCharsets.UTF_8);

		var actual = processor.preprocess("1.19.2", lines, "TestFile1");

		List<String> expected = List.of("public class TestFile1 {", "	//# 1.18.1", "	// Code for 1.18.1 and up", "	//# 1.16.1", "//$$	// Code for 1.16.1 and up", "	//# end", "}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessTooOld() throws Exception {
		var processor = new Processor();
		processor.initialize(this.allVersions, null);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile1.java"), StandardCharsets.UTF_8);

		var actual = processor.preprocess("infinity", lines, "TestFile1");

		List<String> expected = List.of("public class TestFile1 {", "	//# 1.18.1", "//$$	// Code for 1.18.1 and up", "	//# 1.16.1", "//$$	// Code for 1.16.1 and up", "	//# end", "}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessFail() throws Exception {
		List<String> notQuiteAllVersions = new ArrayList<>(this.allVersions);
		notQuiteAllVersions.remove(5);

		var processor = new Processor();
		processor.initialize(notQuiteAllVersions, null);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile1.java"), StandardCharsets.UTF_8);

		var exception = assertThrows(RuntimeException.class, () -> {
			processor.preprocess("1.16.1", lines, "TestFile1");
		});

		assertEquals(exception.getMessage(), "The specified version 1.18.1 in TestFile1 in line 2 was not found");
	}

	// TESTS FOR TestFile3.java
	@Test
	void testPreprocess3Pattern() throws Exception {
		var processor = new Processor();
		processor.initialize(this.allVersions, this.patterns);

		var lines = FileUtils.readLines(new File("src/test/resources/TestFile3.java"), StandardCharsets.UTF_8);

		var actual = processor.preprocess("1.14.4", lines, "TestFile3");

		List<String> expected = List.of("public class TestFile3 {", "	//	Minecraft mc = Minecraft.getInstance(); // @GetMinecraft", "	//	Level level = mc.level; // @GetLevel", "}");
		assertEquals(expected, actual);
	}

}
