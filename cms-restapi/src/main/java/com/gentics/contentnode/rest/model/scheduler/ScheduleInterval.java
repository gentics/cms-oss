package com.gentics.contentnode.rest.model.scheduler;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Interval definition for {@link ScheduleData}.
 */
public class ScheduleInterval {

	/**
	 * The duration of the interval in {@link #unit interval units}.
	 */
	private int value;

	/**
	 * The interval unit.
	 */
	private IntervalUnit unit;

	/**
	 * Get the interval duration.
	 * @return The interval duration.
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Set the interval duration.
	 * @param value The interval duration.
	 * @return fluent API
	 */
	public ScheduleInterval setValue(int value) {
		this.value = value;
		return this;
	}

	/**
	 * Get the interval time unit.
	 * @return The interval time unit.
	 */
	public IntervalUnit getUnit() {
		return unit;
	}

	/**
	 * Set the interval time unit.
	 *
	 * @param unit The interval time unit.
	 * @return fluent API
	 */
	public ScheduleInterval setUnit(IntervalUnit unit) {
		this.unit = unit;
		return this;
	}

	/**
	 * Whether this interval definition is valid.
	 *
	 * <p>
	 *     A valid interval has a {@link #value} greater than zero, and has a
	 *     non-null {@link #unit} set.
	 * </p>
	 *
	 * @return Whether this interval definition is valid.
	 */
	@JsonIgnore
	public boolean isValid() {
		return value > 0 && unit != null;
	}

	/**
	 * Check if this interval is due for execution.
	 *
	 * @param lastTimestamp The timestamp of the last execution.
	 * @param timestamp The current timestamp.
	 * @return Whether the associated schedule should is due for execution based
	 * 		on this interval.
	 */
	@JsonIgnore
	public boolean isDue(int lastTimestamp, int timestamp) {
		if (unit == null || value <= 0) {
			return false;
		}

		if (lastTimestamp <= 0) {
			return true;
		}

		Calendar now = Calendar.getInstance();
		now.setTimeInMillis(timestamp * 1000L);

		Calendar next = Calendar.getInstance();
		next.setTimeInMillis(lastTimestamp * 1000L);
		switch (unit) {
		case day:
			next.add(Calendar.DAY_OF_YEAR, value);
			break;
		case hour:
			next.add(Calendar.HOUR_OF_DAY, value);
			break;
		case minute:
			next.add(Calendar.MINUTE, value);
			break;
		case month:
			next.add(Calendar.MONTH, value);
			break;
		case week:
			next.add(Calendar.WEEK_OF_YEAR, value);
			break;
		default:
			break;
		}

		return !next.after(now);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScheduleInterval) {
			ScheduleInterval other = (ScheduleInterval) obj;
			return (this.value == other.value) && (this.unit == other.unit);
		} else {
			return false;
		}
	}
}
