package com.gentics.contentnode.rest.model.response.scheduler;

/**
 * Possible status values of the scheduler
 */
public enum SchedulerStatus {
	/**
	 * Scheduler is running
	 */
	running,

	/**
	 * Scheduler is suspending (jobs are still running)
	 */
	suspending,

	/**
	 * Scheduler is suspended
	 */
	suspended
}
