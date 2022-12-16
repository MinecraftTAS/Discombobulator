package com.minecrafttas.discombobulator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Processor {
	
	private static int linecount;
	
	private static String filename;
	
	/**
	 * <p>Used in version detection.
	 * <p>
	 * <p>If you your target is e.g. 1.16.5,
	 * <p>but you do not have exactly 1.16.5 in the preprocessor block,
	 * <p>the next lowest version is used:
	 * <p>
	 * <p> (Target is 1.16.5)
	 * <p>
	 * <pre> 
	 * 	//# 1.18.1
	 * //$$ Code for 1.18.1 and up
	 *  	//# 1.16.1
	 *  Code for 1.16.1 and up
	 *  	//# 1.15.2
	 * //$$ Code for 1.15.2 and up
	 * </pre>
	 * 
	 * Overflow is true, when the current version goes past the target and the next lowest is found
	 */
	private static boolean overFlow = false;
	
	/**
	 * True whem the versions match up perfectly
	 */
	private static boolean perfectMatch = false;

	public static List<String> preprocess(String targetVersion, String[] allVersions, List<String> lines, String filename) throws Exception{
		
		Pattern regex = Pattern.compile("\\/\\/ *# (.+)");
		
		String currentVersion=null;
		
		List<String> out = new ArrayList<>();
		
		Processor.filename = filename;
		
		// Switch version blocks
		for (String line : lines) {
			linecount++;
			// Pattern detection
			Matcher match = regex.matcher(line);
			if(match.find()) {
				String ver = match.group(1);
				if(ver.equalsIgnoreCase("end")) {
					currentVersion=null;
					overFlow = false;
					perfectMatch = false;
				} else {
					currentVersion = ver;
				}
				out.add(line);
				continue;
			}
			
			// Change lines accordingly
			if(currentVersion!=null) {
				if(isVersionEnabled(allVersions, targetVersion, currentVersion)) {
					
					String changedLine = line;
					if(line.startsWith("//$$")) {
						changedLine = line.replace("//$$", "");
					}
					out.add(changedLine);
				} else {
					
					String changedLine = line;
					if(!line.startsWith("//$$")) {
						changedLine = "//$$" + line;
					}
					out.add(changedLine);
				}
				continue;
			}
			out.add(line);
		}
		return out;
	}

	private static boolean isVersionEnabled(String[] allVersions, String targetVersion, String currentVersion) throws Exception{
		int targetVer = -1;
		int currentVer = -1;
		for (int i = 0; i < allVersions.length; i++) {
			String ver = allVersions[i];
			if(ver.equals(targetVersion)) {
				targetVer = i;
			}
			if(ver.equals(currentVersion)) {
				currentVer = i;
			}
		}
		
		if(currentVer == -1) {
			throw new Exception(String.format("The specified version %s in %s in line %s was not found", currentVersion, filename, linecount-1));
		}
		
		if (targetVer>currentVer) {
			return false;
		}else if(targetVer == currentVer){
			perfectMatch = true;
			return true;
		}else {
			if(perfectMatch)
				return false;
			if(overFlow)
				return false;
			else {
				overFlow = true;
				return true;
			}
		}
	}
}
