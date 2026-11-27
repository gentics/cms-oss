package com.gentics.contentnode.rest.model.task;

/**
 * Status of a task
 */
public enum TaskStatus {
	/**
	 * Nobody works on the task yet
	 */
	open,

	/**
	 * Someone works on the task, e.g. after a group member claimed it
	 */
	inProgress,

	/**
	 * The task is done
	 */
	done,

	/**
	 * The task was dismissed without being done
	 */
	dismissed;

	/**
	 * Whether the status ends the task
	 * @return true for {@link #done} and {@link #dismissed}
	 */
	public boolean isFinal() {
		return this == done || this == dismissed;
	}
}
