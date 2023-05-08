package com.gentics.contentnode.rest.model.scheduler;

/**
 * The current status of a schedule.
 */
public enum ScheduleStatus {
	/** Not running, and not due for execution. **/
	IDLE,

	/** The schedule is due for execution. */
	DUE,

	/** An execution for the schedule is currently running. */
	RUNNING
}
