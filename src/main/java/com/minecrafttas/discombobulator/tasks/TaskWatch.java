package com.minecrafttas.discombobulator.tasks;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;

public class TaskWatch extends DefaultTask {

	@TaskAction
	public void preprocessWatch() {
		// Preprocessing is split into the following steps:
		// - compare all files and figure out which one's are different
		// - for all different files, figure out which one has the most recent modification date
		// - preprocess the files into the other versions and into the main source folder
		for (var version : Discombobulator.config.getVersions().get()) {
			var sourceDir = new File(this.getProject().getProjectDir(), version + File.separatorChar + "src" + File.separatorChar + "main");
			// TODO: how to continue?
		}
	}

}
