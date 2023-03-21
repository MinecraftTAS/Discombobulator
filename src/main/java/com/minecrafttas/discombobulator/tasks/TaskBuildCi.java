package com.minecrafttas.discombobulator.tasks;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

/**
 * This task builds and collects all version
 * @author Pancake
 */
public class TaskBuildCi extends DefaultTask {

	/**
	 * List of all build dirs
	 */
	private Map<String, File> buildDirs = new HashMap<>();
	
	@TaskAction
	public void buildCi() {
		File collectDir = new File(getProject().getBuildDir(), "collect");
		collectDir.mkdirs();
		for (Entry<String, File> entry : buildDirs.entrySet()) {
			File buildDir = entry.getValue();
			String suffix = "-mc" + entry.getKey() + ".";
			for (File artifact : buildDir.listFiles()) {
				if (new File(collectDir, artifact.getName()).exists()) {
					String[] fragments = artifact.getName().split("\\.", 2);
					String extension = "." + fragments[fragments.length-1];
					
					artifact.renameTo(new File(collectDir, artifact.getName().replace(extension, suffix + extension)));
				} else {
					artifact.renameTo(new File(collectDir, artifact.getName()));
				}
			}
		}
	}
	
	/**
	 * Updates this task with all compile tasks
	 * @param compileTasks List of compile tasks
	 */
	public void updateCompileTasks(List<Task> compileTasks) {
		for (Task task : compileTasks) {
			Project project = task.getProject();
			this.buildDirs.put(project.getName(), new File(project.getBuildDir(), "libs"));
		}
		this.setDependsOn(compileTasks);
	}
	
}
