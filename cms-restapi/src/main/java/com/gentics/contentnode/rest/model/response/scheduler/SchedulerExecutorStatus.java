package com.gentics.contentnode.rest.model.response.scheduler;

/**
 * The status of the scheduler executor.
 */
public enum SchedulerExecutorStatus {
	/** Executor is running normally. */
	RUNNING,

	/** Executor is not running. */
	NOT_RUNNING,

	/** Executor was restarted during the current check request. */
	RESTARTED
}
