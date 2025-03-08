package com.minecrafttas.discombobulator.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * <p>Task class for stopping a task.
 * <p>{@link TaskPreprocessVersion} is intended for <strong>subprojects only!</strong><br>
 * And since gradle is such a cool program, it automatically creates a task in the root project as well,<br>
 * that you can not disable apparently.
 * <p>But you can register separate tasks to the root project, that will execute before everything else, so to stop it,<br>
 * we just need to throw an exception and it will stop all following tasks. Thanks Gradle! 
 */
public class TaskPreprocessVersionError extends DefaultTask {

	/**
	 * Just throws an exception. Nothing else
	 * @throws Exception
	 */
	@TaskAction
	public void preprocessVersion() throws Exception {
		throw new Exception("\n\n!!!!!!!!!! Do not use this task on the root project !!!!!!!!!!\n\n"
				+ "This task is intended for subprojects only,\n"
				+ "so this exception will stop it from running on all subprojects.\n"
				+ "This was the only way I could find to disable the task for the root project, thanks Gradle!\n");
	}
}
