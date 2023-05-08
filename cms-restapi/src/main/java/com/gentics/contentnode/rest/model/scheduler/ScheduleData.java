package com.gentics.contentnode.rest.model.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * Definition when a schedule should be executed.
 */
public class ScheduleData {

	/**
	 * The schedule type.
	 */
	private ScheduleType type;

	/**
	 * The start of the interval this schedule should be considered for execution.
	 */
	private int startTimestamp;

	/**
	 * The end of the interval this schedule should be considered for execution.
	 */
	private int endTimestamp;

	/**
	 * The interval definition (only used when {@link #type} is {@code interval}).
	 */
	private ScheduleInterval interval;

	/**
	 * The follow definition (only used when {@link #type} is {@code followup}.
	 */
	private ScheduleFollow follow;

	/**
	 * Get the schedule type.
	 * @return The schedule type.
	 */
	public ScheduleType getType() {
		return type;
	}

	/**
	 * Set the schedule type.
	 * @param type The schedule type.
	 * @return fluent API
	 */
	public ScheduleData setType(ScheduleType type) {
		this.type = type;
		return this;
	}

	/**
	 * Get the start timestamp.
	 * @return The start timestamp.
	 */
	public int getStartTimestamp() {
		return startTimestamp;
	}

	/**
	 * Set the start timestamp.
	 * @param startTimestamp The start timestamp.
	 * @return fluent API
	 */
	public ScheduleData setStartTimestamp(int startTimestamp) {
		this.startTimestamp = startTimestamp;
		return this;
	}

	/**
	 * Get the end timestamp.
	 * @return The end timestamp.
	 */
	public int getEndTimestamp() {
		return endTimestamp;
	}

	/**
	 * Set the end timestamp.
	 * @param endTimestamp The end timestamp.
	 * @return fluent API
	 */
	public ScheduleData setEndTimestamp(int endTimestamp) {
		this.endTimestamp = endTimestamp;
		return this;
	}

	/**
	 * Get the interval definition.
	 * @return The interval definition.
	 */
	public ScheduleInterval getInterval() {
		return interval;
	}

	/**
	 * Set the interval definition.
	 * @param interval The interval definition.
	 * @return fluent API
	 */
	public ScheduleData setInterval(ScheduleInterval interval) {
		this.interval = interval;
		return this;
	}

	/**
	 * Get the followup definition.
	 * @return The followup definition.
	 */
	public ScheduleFollow getFollow() {
		return follow;
	}

	/**
	 * Set the followup definition.
	 * @param follow The followup definition.
	 * @return fluent API
	 */
	public ScheduleData setFollow(ScheduleFollow follow) {
		this.follow = follow;
		return this;
	}

	/**
	 * Check if the schedule data is valid.
	 *
	 * <p>
	 *     Validation depends on the schedule {@code type}:
	 *
	 *     <ul>
	 *         <li>{@code once}: the {@code startTimestamp} must be greater than zero</li>
	 *         <li>{@code interval}: the {@code #getInterval() interval} must be set and be valid</li>
	 *         <li>{@code followup}: the {@link #getFollow() follow up} must be set and contain at least one ID</li>
	 *         <li>{@code manual}: always valid</li>
	 *     </ul>
	 * </p>
	 * @return
	 */
	@JsonIgnore
	public boolean isValid() {
		switch (type) {
		case once:
			return startTimestamp > 0;

		case interval:
			return interval != null && interval.isValid();

		case followup:
			return getFollow() != null && !getFollow().getScheduleId().isEmpty();

		case manual:
			return true;

		default:
			return false;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScheduleData) {
			ScheduleData other = (ScheduleData) obj;
			return (this.type == other.type) && (this.startTimestamp == other.startTimestamp)
					&& (this.endTimestamp == other.endTimestamp) && Objects.deepEquals(this.interval, other.interval)
					&& Objects.deepEquals(this.follow, other.follow);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format(
			"{%s start: %d, end: %d, type: %s}",
			getClass().getSimpleName(),
			getStartTimestamp(),
			getEndTimestamp(),
			getType().name());
	}
}
