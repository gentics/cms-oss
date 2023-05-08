package com.gentics.contentnode.rest.model.scheduler;

/**
 * Interval units for schedules.
 */
public enum IntervalUnit {
	/** Execute every X mintues. */
	minute,
	/** Execute every X hours. */
	hour,
	/** Execute every X days. */
	day,
	/** Execute every X weeks. */
	week,
	/** Execute every X months. */
	month
}
