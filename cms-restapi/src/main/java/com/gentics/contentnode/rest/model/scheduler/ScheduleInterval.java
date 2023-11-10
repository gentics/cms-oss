package com.gentics.contentnode.rest.model.scheduler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicLong;

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
	 * @param startTimestamp start timestamp of the schedule
	 * @param lastTimestamp The timestamp of the last execution.
	 * @param timestamp The current timestamp.
	 * @return Whether the associated schedule should is due for execution based
	 * 		on this interval.
	 */
	@JsonIgnore
	public boolean isDue(int startTimestamp, int lastTimestamp, int timestamp) {
		if (unit == null || value <= 0) {
			return false;
		}

		if (lastTimestamp <= 0) {
			return true;
		}

		ZonedDateTime start = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startTimestamp), ZoneId.systemDefault());
		ZonedDateTime last = ZonedDateTime.ofInstant(Instant.ofEpochSecond(lastTimestamp), ZoneId.systemDefault());
		ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

		TemporalUnit temporalUnit = null;
		switch(unit) {
		case day:
			temporalUnit = ChronoUnit.DAYS;
			break;
		case hour:
			temporalUnit = ChronoUnit.HOURS;
			break;
		case minute:
			temporalUnit = ChronoUnit.MINUTES;
			break;
		case month:
			temporalUnit = ChronoUnit.MONTHS;
			break;
		case week:
			temporalUnit = ChronoUnit.WEEKS;
			break;
		default:
			break;
		}

		ZonedDateTime next = start;

		// calculate the number of temporal units between the start and the last execution
		long diff = start.until(last, temporalUnit);
		AtomicLong factor = new AtomicLong(diff / value);

		while (!next.isAfter(last)) {
			next = start.plus(factor.getAndIncrement() * value, temporalUnit);
		}

		return !next.isAfter(now);
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

	@Override
	public String toString() {
		if (value == 1) {
			return String.format("every %s", unit);
		} else {
			return String.format("every %d %ss", value, unit);
		}
	}
}
