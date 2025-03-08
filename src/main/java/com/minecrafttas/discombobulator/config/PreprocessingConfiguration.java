package com.minecrafttas.discombobulator.config;

import java.util.Map;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import com.minecrafttas.discombobulator.utils.PortLock;

/**
 * Configuration for preprocessing
 * @author Pancake
 */
public abstract class PreprocessingConfiguration {

	/**
	 * All versions for the project
	 * <pre>
	 * versions = [
	 * 	"1.14.4":"",
	 * 	"1.12.2":"",
	 * 	"1.8.9": "AlternateFolderName1.8.9"
	 * ]
	 * </pre>
	 * 
	 * <p>A subproject will be created, only if a version is present in the list. 
	 * 
	 * <p>Defaults to []
	 * @return Ordered list of versions
	 */
	public abstract MapProperty<String, String> getVersions();

	/**
	 * All patterns for the project.
	 * <pre>
	 * patterns = [
	 * 	GetMinecraft: [
	 * 		"1.14.4": "Minecraft.getMinecraft().getPlayer()",
	 *		"1.12.2": "Minecraft.getMinecraft().player",
	 *		"def": "Minecraft.getMinecraft().thePlayer"
	 * 	]
	 * ]
	 * </pre>
	 * <p>Anything annotated by @key will be adjusted according to the version where "1.14.4" would refer to any version above or equal to 1.14.4<br>
	 * {@code Minecraft.getMinecraft().getPlayer(); // @GetMinecraft} would be adjusted to {@code Minecraft.getMinecraft().thePlayer} in versions below 1.12.2
	 * 
	 * <p>Defaults to []
	 * 
	 * @return Map of patterns
	 */
	public abstract MapProperty<String, Map<String, String>> getPatterns();

	/**
	 * <p>If the default version should be inverted
	 * 
	 * <p>This makes it, so the highest version is considered the default version
	 * <pre>
	 * versions = [
	 *	"1.12.2":"", &lt;- Default version if inverted is true
	 *	"1.11.2":"",
	 *	"1.10.2":"",
	 *	"1.9.4":"",
	 *	"1.8.9": "" &lt;- Default version if inverted is false
	 * ]
	 * </pre>
	 * 
	 * <pre>
	 * inverted = false
	 * </pre>
	 * 
	 * <p>Defaults to false
	 * 
	 * @return True if inverted
	 */
	public abstract Property<Boolean> getInverted();

	/**
	 * If true, disables ANSI colors
	 * 
	 * <pre>
	 * 	disableAnsi = false
	 * </pre>
	 *
	 * <p>Defaults to false
	 * 
	 * @return True if disabled
	 */
	public abstract Property<Boolean> getDisableAnsi();

	/**
	 * The port for the {@link PortLock}
	 * 
	 * <pre>
	 * 	port = 8762
	 * </pre>
	 * 
	 * <p>Defaults to 8762
	 * 
	 * @return The port number
	 */
	public abstract Property<Integer> getPort();

	/**
	 * <p>Filetypes which should be ignored by the preprocessor and just copied.<br>
	 * Usually binary file formats should be excluded in this list
	 * 
	 * <pre>
	 * 	ignoredFileFormats = ["*.png"]
	 * </pre>
	 *  
	 * <p>Defaults to []	
	 * @return A string array of file filter wildcards
	 */
	public abstract ListProperty<String> getIgnoredFileFormats();

	/**
	 * <p>Set's the default line feed character
	 * 
	 * <p>If this option is not set, the {@link System#lineSeparator()} is used instead.
	 * <p>This option can also be overwritten by the system property "line.seperator"
	 * <pre>
	 * 	defaultLineFeed = "\n"
	 * </pre>
	 * @return The line feed string
	 */
	public abstract Property<String> getDefaultLineFeed();
}
