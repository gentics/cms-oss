package com.gentics.contentnode.rest.model.scheduler;

/**
 * Types of schedules
 */
public enum ScheduleType {
	/**
	 * The task is executed once at a given date/time
	 */
	once,

	/**
	 * The task is executed at a given interval
	 */
	interval,

	/**
	 * The task is executed following one or multiple other tasks
	 */
	followup,

	/**
	 * The task is executed manually
	 */
	manual
}
