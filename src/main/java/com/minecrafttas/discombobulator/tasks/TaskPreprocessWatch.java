package com.minecrafttas.discombobulator.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.minecrafttas.discombobulator.Discombobulator;
import com.minecrafttas.discombobulator.utils.SocketLock;

/**
 * This task preprocesses the source code on file change
 * @author Pancake
 */
public class TaskPreprocessWatch extends DefaultTask {

	@TaskAction
	public void preprocessWatch() {
		// Lock port
		var lock = new SocketLock(Discombobulator.PORT_LOCK);
		lock.tryLock();

		// Unlock port
		lock.unlock();
	}

}
