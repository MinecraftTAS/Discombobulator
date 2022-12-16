package com.minecrafttas.discombobulator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class ProcessorTest {
	
	String[] allVersions= {
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
			};

	@Test
	void testPreprocessFirst() throws Exception {
		List<String> lines = FileUtils.readLines(new File("src/test/java/resources/testfiles/TestFile1.java"), StandardCharsets.UTF_8);
		
		List<String> actual = Processor.preprocess("1.18.1", allVersions, lines, "TestFile1");
		
		List<String> expected =List.of("package resources.testfiles;"
				, ""
				,"public class TestFile1 {"
				,"	//# 1.18.1"
				,"	// Code for 1.18.1 and up"
				,"	//# 1.16.1"
				,"//$$	// Code for 1.16.1 and up"
				,"	//# end"
				,"}");
		assertEquals(expected, actual);
	}
	
	@Test
	void testPreprocessSecond() throws Exception {
		List<String> lines = FileUtils.readLines(new File("src/test/java/resources/testfiles/TestFile1.java"), StandardCharsets.UTF_8);
		
		List<String> actual = Processor.preprocess("1.16.1", allVersions, lines, "TestFile1");
		
		List<String> expected =List.of("package resources.testfiles;"
				, ""
				,"public class TestFile1 {"
				,"	//# 1.18.1"
				,"//$$	// Code for 1.18.1 and up"
				,"	//# 1.16.1"
				,"	// Code for 1.16.1 and up"
				,"	//# end"
				,"}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessTooNew() throws Exception {
		List<String> lines = FileUtils.readLines(new File("src/test/java/resources/testfiles/TestFile1.java"), StandardCharsets.UTF_8);
		
		List<String> actual = Processor.preprocess("1.19.2", allVersions, lines, "TestFile1");
		
		List<String> expected =List.of("package resources.testfiles;"
				, ""
				,"public class TestFile1 {"
				,"	//# 1.18.1"
				,"	// Code for 1.18.1 and up"
				,"	//# 1.16.1"
				,"//$$	// Code for 1.16.1 and up"
				,"	//# end"
				,"}");
		assertEquals(expected, actual);
	}

	@Test
	void testPreprocessTooOld() throws Exception {
		List<String> lines = FileUtils.readLines(new File("src/test/java/resources/testfiles/TestFile1.java"), StandardCharsets.UTF_8);
		
		List<String> actual = Processor.preprocess("infinity", allVersions, lines, "TestFile1");
		
		List<String> expected =List.of("package resources.testfiles;"
				, ""
				,"public class TestFile1 {"
				,"	//# 1.18.1"
				,"//$$	// Code for 1.18.1 and up"
				,"	//# 1.16.1"
				,"//$$	// Code for 1.16.1 and up"
				,"	//# end"
				,"}");
		assertEquals(expected, actual);
	}
	
	@Test
	void testPreprocessFail() throws Exception {
		List<String> lines = FileUtils.readLines(new File("src/test/java/resources/testfiles/TestFile1.java"), StandardCharsets.UTF_8);
		
		String[] removed = allVersions.clone();
		
		removed[5]="";
		
	    Exception exception = assertThrows(Exception.class, () -> {
	    	Processor.preprocess("1.16.1", removed, lines, "TestFile1");
	    });
	    
	    assertEquals(exception.getMessage(), "The specified version 1.18.1 in TestFile1 in line 4 was not found");
	}
}
