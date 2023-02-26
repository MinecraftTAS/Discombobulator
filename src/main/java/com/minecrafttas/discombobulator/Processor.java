package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minecrafttas.discombobulator.utils.Pair;

public class Processor {
	private final Pattern regexBlocks = Pattern.compile("^\\s*\\/\\/ *(#+) *(.+)");
	private final Pattern regexHashtag = Pattern.compile("^\\s*# *(#+) *(.+)");
	private final Pattern regexPatterns = Pattern.compile("^.+\\/\\/ *@(.+);");
	
	private List<String> versions;
	private Map<String, Map<String, String>> patterns;
	
	private List<VersionState> states = new ArrayList<>();

	/**
	 * The current version of the preprocessor block that is processed. <br>
	 * Empty if the lines are outside a preprocessor block, filled if inside and the size dictates the nesting layer
	 */
	private List<String> currentVersionsNesting = new ArrayList<>();
	
	/**
	 * If the list should be inverted with the first one being the default
	 */
	private final boolean inverted;
	
	/**
	 * Debug filename for errors during preprocessing
	 */
	private String filename = "undefined";
	
	/**
	 * Debug linecount for errors during preprocessing
	 */
	private int linenumber = 0;
	private boolean versionEnabled = true;
	private boolean nestingReturn = false;
	
	private int debugCounter = 0; // TODO REMOVE
	
	/**
	 * Creates a new processor. The default will be the lowest version.
	 * 
	 * <pre>
	 * [
	 * "1.20",
	 * "1.19",
	 * "1.18",
	 * "1.16" <- default version since it's the lowest
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public Processor(List<String> versions, Map<String, Map<String, String>> patterns) {
		this(versions, patterns, false);
	}
	
	/**
	 * Creates a new processor. The order can be inverted.
	 * <p> If inverted is true:
	 * 
	 * <pre>
	 * [
	 * "1.20", <- default version since it's the highest
	 * "1.19",
	 * "1.18",
	 * "1.16"
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public Processor(List<String> versions, Map<String, Map<String, String>> patterns, boolean inverted) {
		if(versions == null) {
			throw new NullPointerException("Versions can't be null!");
		}
		this.versions = versions;
		this.patterns = patterns;
		this.inverted = inverted;
	}
	
	/**
	 * Preprocesses the lines to a targeted version.
	 * <p>Preprocessing can happen in 2 ways:<br>
	 * <ol>
	 * <li>Preprocessing a block of code</li>
	 * <li>Preprocessing a specific line of code with patterns</li>
	 * </ol>
	 * <p>The advantage of pattern processing is that you can save yourself some work making a version block for common patterns like <code>Minecraft.getMinecraft().player</code> in 1.12 to <code>Minecraft.getMinecraft().thePlayer</code> in e.g. 1.10
	 *
	 * <h2>VersionBlock</h2>
	 * <p>A version block allows you to uncomment part of the code in certain versions via comments:
	 * <pre>
	 * //# 1.12.2
	 * 	The code you want to write in 1.12.2
	 * //# 1.9.4
	 * //$$	The code you want to write in 1.9.4
	 * //# def
	 * //$$	Default code that is below 1.9.4
	 * //# end
	 * </pre>
	 * <p>Blocks start with a //# mcversion and end with a //# end. If the targeted version is not defined in the block, the next lowest version will be chosen for preprocessing (e.g. target is 1.11, doesn't exist, so 1.9.4 block will be preprocessed).<br>
	 * You can define a default block that defaults to the lowest version you specified (e.g. 1.9.2 is lower than 1.9.4, no other version is lower, so it uses default). <br>
	 * 
	 * <p>You can't define these blocks out of order (e.g. first 1.9.4 then 1.12.2)... This will lead to some unintended side effects.
	 * 
	 * <h3>Nesting</h3>
	 * 
	 * <p>Sometimes you have a lot of changes in one version but a tiny amount is added in the following versions. In these cases, you would need to copy all the large changes again into the newest version:
	 * 
	 * <pre>
	 * //# 1.16.5
	 * New things in 1.16.5
	 * 
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * 
	 * //# 1.16.1
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * //# end
	 * </pre>
	 * 
	 * <p>Here you need to double the code to achieve your goal. An easier way can be achieved with nesting.
	 * <p>You can define a nested block inside a version block by adding hashtags to the definition:
	 * 
	 * <pre>
	 * 
	 * //# 1.16.1
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * 
	 * //## 1.16.5		<-- 2 hashtags define the nested version
	 * New things in 1.16.5
	 * //## end		<-- The end of the nested block is defined with 2 hashtags as well
	 * 
	 * //# end
	 * </pre>
	 * 
	 * <p>To nest inside a nesting block you can add more hashtags.
	 * 
	 * <p>Some rules to follow:
	 * <ul>
	 * <li>Every nested block needs an 'end' statement</li>
	 * <li>The nested version can't be lower (if inverted, higher) than the parent version.<br>
	 * In the example above, you can't nest 1.15.2 inside a 1.16.1 block.</li>
	 * <li>You cannot skip nesting levels. Defining level 3 nesting after a level 1 nesting is not allowed</li>
	 * <li>Using def in a nested block will use the parent version</li>
	 * </ul>
	 * 
	 * <h2>Patterns</h2>
	 * Patterns are defined in the build.gradle:
	 * 
	 * <pre>
	 * patterns = [
	 * 	GetWindow: [
	 *		"def": "mc.window",
	 *		"1.15.2": "mc.getWindow()"
	 *	]
	 * ]
	 * </pre>
	 * <p> Each pattern has a name and multiple versions, with a default under "def".<br>
	 * To apply patterns, we have to write a <code>//@ patternname;</code> in the line we want to apply. Note that the pattern is terminated via semicolon.
	 * 
	 * <pre>
	 * mc.window; // @GetWindow;
	 * </pre>
	 * 
	 * <p>We can also apply multiple patterns in one line, seperated via comma:
	 * 
	 * <pre>
	 * Minecraft.getMinecraft().setWindow(mc.window); // @GetWindow,GetMinecraft;
	 * </pre>
	 * 
	 * <p>Similar to version blocks, if you specify a targetVersion for a pattern that does not specify that version, 
	 * the next lowest (or highest if inverted) version will be chosen as a pattern, the default pattern if nothing was found.
	 * 
	 * @param targetVersion The version for which lines should be enabled
	 * @param lines The lines to preprocess
	 * @param filename Debug filename for errors during preprocessing
	 * @return The preprocessed lines of the file
	 * @throws Exception 
	 */
	public List<String> preprocess(String targetVersion, List<String> lines, String filename, String fileending) throws Exception {
		nestingReturn = false;
		
		List<String> out = new ArrayList<>();
		this.filename = filename;
		this.linenumber = 0;
		
		boolean useHashtags = shouldUseHashTag(fileending);
		ConcurrentLinkedQueue<Boolean> enabledQueue = new ConcurrentLinkedQueue<>();
		
		for (String line : lines) {
			linenumber++;
			
			if((useHashtags && Pattern.matches(regexHashtag.pattern(), line)) || (!useHashtags && Pattern.matches(regexBlocks.pattern(), line))) {
				if(enabledQueue.isEmpty()) {
					enabledQueue = generateEnabledQueue(lines, useHashtags, targetVersion);
				}
			}
			
			line = preprocessVersionBlock(line, targetVersion, enabledQueue, useHashtags);

			if (patterns != null) {
				line = preprocessPattern(line, targetVersion);
			}
			out.add(line);
		}
		
		return out;
	}

	/**
	 * Searches through the text in advance to generate a queue that tells {@link #preprocessVersionBlock(String, String, ConcurrentLinkedQueue, boolean)} if that block should be enabled or not.
	 * @param lines
	 * @param useHashTag
	 * @return A queue with booleans matching the number of version statements in the version block
	 */
	private ConcurrentLinkedQueue<Boolean> generateEnabledQueue(List<String> lines, boolean useHashTag, String targetVersion) {
		ConcurrentLinkedQueue<Boolean> out = new ConcurrentLinkedQueue<>();
		if(debugCounter>0) {
			return out;
		}
		VersionBlockList blockList = generateBlockList(lines, linenumber-1, useHashTag, 1).right();
		blockList.sort();
		System.out.println("==============\n"+blockList);
		System.out.println("Size:"+blockList.size());
		debugCounter=blockList.size();
		return out;
	}
	
	/**
	 * Parses the lines with the given start line recursively and returns a {@link VersionBlockList} with all nested {@link VersionBlock} inside.
	 * From here, we can sort and order the versions
	 * @param lines
	 * @param startLine
	 * @param useHashTag
	 * @param nestingLevel
	 * @return A pair with the line numbers on left, and the block numbers on right
	 */
	private Pair<Integer, VersionBlockList> generateBlockList(List<String> lines, int startLine, boolean useHashTag, int nestingLevel) {
		
		VersionBlockList blockList = new VersionBlockList();
		int index = 0;
		int lineCount;
		
		VersionBlock currentBlock=null;
		
		/*Reading ahead and storing the version statements in blockList*/
		for(lineCount = startLine; lineCount<lines.size(); lineCount++) {
			
			String line = lines.get(lineCount);
			
			/*Generate different matchers depending if the file uses hashtag as comments or not*/
			Matcher matcher;
			if(!useHashTag) {
				matcher = regexBlocks.matcher(line);
			}else {
				matcher = regexHashtag.matcher(line);
			}
			
			if(!matcher.find()) {	// Skip if it can't find a version statement in this line. e.g. //# 1.12.2
				continue;
			}
			
			/*Read the version statement*/
			String version = matcher.group(2);
			int level = matcher.group(1).length();
			
			Pair<Integer, VersionBlockList> nestedVersions = null;
			
			if(level == nestingLevel+1) {
				nestedVersions = generateBlockList(lines, lineCount, useHashTag, level);
				lineCount = nestedVersions.left();
				
				if(!nestedVersions.right().isEmpty()) {
					currentBlock.addNestedVersionBlockList(nestedVersions.right());
				}
			}

			if(level == nestingLevel) {
				if(currentBlock!=null) {
					index++; // Increase the blockList index
					
					blockList.addBlock(currentBlock);
				}
				currentBlock = new VersionBlock(version, index, level);
			}
			
			/*Leave the loop when the version block ends*/
			if(version.equals("end") && level == nestingLevel) {	
//				blockList.addBlock(currentBlock);
				break;
			}
			
			
			
		}
		return Pair.of(lineCount, blockList);
	}

	private class VersionBlock {
		private String version;
		private int index;
		private List<VersionBlockList> nestedBlockList = new ArrayList<>(); 
		private int debugLevel;
		
		public VersionBlock(String version, int index, int level) {
			this.version = version;
			this.index = index;
			this.debugLevel = level;
		}
		
		public void addNestedVersionBlockList(VersionBlockList blocks) {
			nestedBlockList.add(blocks);
		}
		
		@Override
		public String toString() {
			String out = "   ".repeat(debugLevel-1)+version+"-"+index+"\n";
			for(VersionBlockList list : nestedBlockList) {
				out=out.concat(list.toString());
			}
			return out;
		}
		
		public int size() {
			int i = 1; // This block
			for(VersionBlockList list : nestedBlockList) {
				i=i+list.size();
			}
			return i;
		}
		
		public void sort() {
			for(VersionBlockList list : nestedBlockList) {
				list.sort();
			}
		}
	}
	
	private class VersionBlockList {
		private List<VersionBlock> blocks = new ArrayList<>();
		
		public void addBlock(VersionBlock block) {
			blocks.add(block);
		}
		
		public boolean isEmpty() {
			return blocks.isEmpty();
		}
		
		@Override
		public String toString() {
			String out="";
			for(VersionBlock block : blocks) {
				out=out.concat(block.toString());
			}
			return out;
		}
		
		public int size() {
			int i = 0;
			for(VersionBlock block : blocks) {
				i = i + block.size();
			}
			return i;
		}
		
		public void sort() {
			blocks.sort((left, right)->{
				String verLeft = left.version;
				String verRight = right.version;
				if("def".equals(verLeft)) {
					verLeft=versions.get(versions.size()-1);
				}
				if("def".equals(verRight)) {
					verRight=versions.get(versions.size()-1);
				}
				int indexLeft = versions.indexOf(verLeft);
				int indexRight = versions.indexOf(verRight);
				int compare = Integer.compare(indexLeft, indexRight);
				return compare;
			});
			for(VersionBlock block : blocks) {
				block.sort();
			}
		}
	}

	
	private boolean shouldUseHashTag(String fileending) {
		return "accesswidener".equals(fileending);
	}
	
	private String preprocessVersionBlock(String line, String targetVersion, ConcurrentLinkedQueue<Boolean> enabledQueue, boolean useHashTag) throws Exception {
		if (updateCurrentVersion(line, useHashTag)) {
			updateEnabled(targetVersion);
			return line;
		}
		return enableLine(line, this.versionEnabled, useHashTag);
	}
	
	/**
	 * Updates the enabled status for the current block
	 * If the target version is smaller or equal than the {@link #currentVersionsNesting}, {@link #versionEnabled} will be true. (If {@link #inverted} is false)
	 * @param targetVersion
	 */
	private void updateEnabled(String targetVersion) {
		if(currentVersionsNesting.size() == 0) { // Check if we are outside a version block
			versionEnabled = true;
			return;
		}
		if(targetVersion == null) {	// Check if nothing should be enabled and the target version is null
			versionEnabled = false;
			return;
		}
		
		/*Interpret the current version state*/
		VersionState state = states.get(states.size()-1);
		
		if (state == VersionState.ENABLED) { // Check if the target version was already found
			if(!nestingReturn) {
				states.set(states.size()-1, VersionState.DISABLED);
				versionEnabled = false;
			} else {
				nestingReturn = false;
				versionEnabled = true;
			}
		}
		else if(state == VersionState.DISABLED) {
			if(nestingReturn) {	// We already searched when we are returning from nesting
				nestingReturn = false;
				return;
			}
			versionEnabled = false;
		}
		else if (state == VersionState.SEARCHING) {
			if(nestingReturn) {	// We already searched when we are returning from nesting
				nestingReturn = false;
				return;
			}
			
			if ("def".equals(targetVersion)) { // Set the default version as the target version
				targetVersion = getDefault();
			}
			// Getting the target and current index in the version. The higher the index the
			// "lower" the mc version
			
			String currentVersion = currentVersionsNesting.get(currentVersionsNesting.size()-1);
			
			int targetIndex = versions.indexOf(targetVersion);
			int currentIndex = versions.indexOf(currentVersion);

			if (currentIndex == -1) {
				throw new RuntimeException(String.format("The specified version %s in %s in line %s was not found", currentVersion, filename, linenumber));
			}
			if(targetIndex == -1) {
				throw new RuntimeException(String.format("The target version %s was not found", targetVersion, filename, linenumber));
			}
			if (targetIndex > currentIndex && !inverted) {
				versionEnabled = false;
			}
			else if (targetIndex < currentIndex && inverted) {
				versionEnabled = false;
			}
			else {								// If the targetIndex is equal or smaller than the currentIndex
				states.set(states.size()-1, VersionState.ENABLED);
				versionEnabled = true;
			}
		}
	}

	/**
	 * Comments out or uncomments a line.
	 * @param line The line to change
	 * @param enable True if line should be enabled
	 * @return The new line
	 */
	private String enableLine(String line, boolean enable, boolean useHashTag) {
		
		String commentChars = "//";
		if(useHashTag) {
			commentChars = "#";
		}
		
		boolean isDisabled = line.startsWith(commentChars+"$$");
		if (enable)
			return isDisabled ? line.replace(commentChars+"$$", "") : line;
		else
			return isDisabled ? line : commentChars+"$$" + line;
	}

	/**
	 * If the line contains any version block statements detected by {@linkplain #regexBlocks}, the {@link #currentVersionsNesting} will be updated
	 * 
	 * @param line The line to check
	 * @param useHashTag 
	 * @return If the line contains a versionBlock statement
	 * @throws Exception 
	 */
	private boolean updateCurrentVersion(String line, boolean useHashTag) throws Exception {
		// Block detection
		Matcher match;
		if(useHashTag)
			match = regexHashtag.matcher(line);
		else
			match = regexBlocks.matcher(line);
		
		
		if (match.find()) {
			String detectedVersion = match.group(2);
			int nestingLevel = match.group(1).length();
			
			if (detectedVersion.equalsIgnoreCase("end")) {	// Process end
				
				if(currentVersionsNesting.isEmpty()) {
					throw new Exception(String.format("Unexpected 'end' found in line %s in %s", linenumber, filename));
				}
				
				if(nestingLevel == currentVersionsNesting.size()) { 				// If the nesting level of the end is actually the one for this nesting block
					currentVersionsNesting.remove(currentVersionsNesting.size() - 1); // Return to previous nesting statement
					states.remove(states.size() - 1); // Return states to previous nesting statement
					
					if(!currentVersionsNesting.isEmpty()) {
						nestingReturn = true;
					}
				} else {
					throw new Exception(String.format("Unexpected 'end' in nested block found in line %s in %s", linenumber, filename));
				}
//				debugCounter--;
				return true;
			}
			else if(detectedVersion.equalsIgnoreCase("def")) { // Process default version
				debugCounter--;
				nestVersion(getDefault(), nestingLevel);
				return true;
			}
			else {
				debugCounter--;
				nestVersion(detectedVersion, nestingLevel);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Applies nesting levels to {@link #currentVersionsNesting}
	 * @param newVersion
	 * @param nestingLevel
	 * @throws Exception
	 */
	private void nestVersion(String newVersion, int nestingLevel) throws Exception {
		
		if (currentVersionsNesting.size()+1 == nestingLevel) {
			
			checkForNestingErrors(newVersion, nestingLevel);

			currentVersionsNesting.add(newVersion);
			if(states.isEmpty() || states.get(states.size()-1) == VersionState.ENABLED) {
				states.add(VersionState.SEARCHING);
			} else {
				states.add(VersionState.DISABLED);
			}
		} 
		else if(currentVersionsNesting.size()-1 == nestingLevel) {
			throw new Exception(String.format("Missing an end for nesting before line %s in %s", linenumber, filename));
		} 
		else if(currentVersionsNesting.size() == nestingLevel) {
			currentVersionsNesting.set(currentVersionsNesting.size()-1, newVersion);
		}
		else {
			throw new Exception(String.format("Unexpected nesting level in line %s in %s", linenumber, filename));
		}
	}
	
	private void checkForNestingErrors(String ver, int nesting) throws Exception {
		
		if(currentVersionsNesting.isEmpty())
			return;
		
		String currentVersion = currentVersionsNesting.get(currentVersionsNesting.size()-1);
		
		int targetIndex = versions.indexOf(ver);
		int currentIndex = versions.indexOf(currentVersion);
		
		if(targetIndex>currentIndex && !inverted) {
			throw new Exception(String.format("The version in the nesting block is smaller than in the parent block. Nested: %s, Parent: %s, Line: %s, File: %s", ver, currentVersion, linenumber, filename));
		}
		else if(targetIndex<currentIndex && inverted) {
			throw new Exception(String.format("The version in the nesting block is greater than in the parent block. Nested: %s, Parent: %s, Line: %s, File: %s", ver, currentVersion, linenumber, filename));
		}
	}

	private String preprocessPattern(String line, String targetVersion) {
		
		Matcher match = this.regexPatterns.matcher(line);
		if (!match.find()) {
			return line;
		}

		// find pattern
		String patternNames = match.group(1);
		List<Map<String, String>> patterns = getPatterns(patternNames);

		for (Map<String, String> pattern : patterns) { // Iterate through multiple patterns
			String replacement = findReplacement(pattern, targetVersion);
			if (replacement == null) {
				throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found for target version %s", patternNames, filename, linenumber, targetVersion));
			}

			if (line.contains(replacement)) { // Optimization, if the targetversion is already the correct
				return line;
			}

			String replaceable = null;

			for (Entry<String, String> entry : pattern.entrySet()) {
				if (line.contains(entry.getValue())) {
					replaceable = entry.getValue();
					break;
				}
			}

			if (replaceable == null)
				throw new RuntimeException(String.format("The specified pattern %s in %s in line %s was not found for any version", patternNames, filename, line));

			line = line.replace(replaceable, replacement);
		}

		return line;
	}
	
	/**
	 * Split the pattern names and get the patterns
	 * @param patternnames Names to split
	 * @return A list of mapped patterns
	 */
	private List<Map<String, String>> getPatterns(String patternnames){
		
		List<Map<String, String>> out = new ArrayList<>();
		
		String[] split = patternnames.split(","); 			// Split the names

		for(String names : split) {
			names = names.trim();	// trim any spaces
			
			Map<String, String> pattern = this.patterns.get(names);
			if (pattern == null) {
				System.out.println(String.format("The specified pattern %s in %s in line %s was not found", names, filename, linenumber));
				continue;
			}
			out.add(pattern);
		}
		return out;
	}
	
	/**
	 * @return The default version
	 */
	private String getDefault() {
		if(currentVersionsNesting.size()==1) {
			return versions.get(getDefaultIndex());
		} else {
			return currentVersionsNesting.get(currentVersionsNesting.size()-2); // Get default version
		}
	}
	
	/**
	 * @return The default index
	 */
	private int getDefaultIndex() {
		return inverted ? 0 : versions.size()-1;
	}
	
	/**
	 * The processor tries to search for a matching version in the lines.<br>
	 * Under certain circumstances the version changes depending on the state.
	 * 
	 * @author Scribble
	 *
	 */
	private enum VersionState{
		/**
		 * The "none" state indicates that the target version does not match the current version and we still have to look for a suitable version.
		 */
		SEARCHING,
		/**
		 * The "found" state indicates that the target version has been found and all versions beyond that can be disabled.
		 */
		ENABLED,
		DISABLED
	}
	
	/**
	 * Searches through the patterns to find the replacement text for any given target version.
	 * 
	 * If the version is not in the patterns, it finds the next lowest (highest if {@link #inverted} is true) version.
	 * 
	 * If the version is too low it uses the replacement defined in the "def" block.
	 * 
	 * If no version and no def block was found it returns null.
	 * 
	 * @param pattern The pattern to search through
	 * @param targetVersion The version we want the replacement for
	 * @return The string that should replace the pattern in the line. Null if no pattern was found.
	 */
	private String findReplacement(Map<String, String> pattern, String targetVersion) {
		
		String replacement = pattern.get(targetVersion);
		
		if(replacement!=null) { // Optimization if the target version has a matching pattern
			return replacement;
		}
		
		int targetIndex = this.versions.indexOf(targetVersion); // Get the targetIndex
		
		if(!inverted) {
			replacement = searchPatterns(targetIndex, pattern);
		} else {
			replacement = searchPatternsInverted(targetIndex, pattern);
		}
		
		if(replacement==null) {	// If there was no version found
			if(pattern.containsKey("def")) {	// Use default as replacement
				replacement = pattern.get("def");
			}
		}
		
		return replacement;
	}
	
	/**
	 * Search through versions lower than the targetVersion for a fitting version.
	 * 
	 * The higher the index, the lower the version. 
	 * This loop starts at the lowest version and goes higher up to one below the target version
	 * 
	 * @param targetIndex The index we want to stop on
	 * @param pattern The pattern to search through
	 * @return The string that should replace the pattern in the line
	 */
	private String searchPatterns(int targetIndex, Map<String, String> pattern) {
		String replacement = null;

		for (int currentIndex = versions.size()-1; currentIndex > targetIndex; currentIndex--) {
			String currentVersion = versions.get(currentIndex);
			
			if(pattern.containsKey(currentVersion)) {
				replacement=pattern.get(currentVersion);
			}
		}
		return replacement;
	}
	
	/**
	 * Search through versions higher than the targetVersion for a fitting version.
	 * 
	 * The higher the index, the lower the version. 
	 * This loop starts at the highest version and goes lower up to one above the target version
	 * 
	 * @param targetIndex The index we want to stop on
	 * @param pattern The pattern to search through
	 * @return The string that should replace the pattern in the line
	 */
	private String searchPatternsInverted(int targetIndex, Map<String, String> pattern) {
		String replacement = null;

		for (int currentIndex = 0; currentIndex < targetIndex; currentIndex++) {
			String currentVersion = versions.get(currentIndex);
			
			if(pattern.containsKey(currentVersion)) {
				replacement=pattern.get(currentVersion);
			}
		}
		return replacement;
	}
	
//	/**
//	 * Finds the lowest pattern replacement for any target version
//	 * @param pattern Pattern
//	 * @param targetVersion Target version
//	 * @return Replacement
//	 */
//	private String findLowestReplacement(Map<String, String> pattern, String targetVersion) {
//		
//		String replacement = pattern.get(targetVersion);
//		
//		if(replacement!=null) { // Optimization if the target version has a matching pattern
//			return replacement;
//		}
//		int targetIndex = this.versions.indexOf(targetVersion);
//		
//		// Find index of target version
//		for (Entry<String, String> entry : pattern.entrySet()) {
//			
//			int currentIndex = this.versions.indexOf(entry.getKey());
//			if ("def".equals(entry.getKey())) {
//				currentIndex = getDefaultIndex();
//			}
//			
//			System.out.println(currentIndex);
//			
//			// Break if version too high
//			if (targetIndex > currentIndex)
//				break;
//			
//			replacement = entry.getValue();
//		}
//		return replacement;
//	}
	
}
