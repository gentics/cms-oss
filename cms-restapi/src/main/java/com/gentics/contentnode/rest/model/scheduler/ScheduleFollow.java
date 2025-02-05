package com.gentics.contentnode.rest.model.scheduler;

import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Definition for follow up execution.
 */
public class ScheduleFollow {
	/**
	 * The IDs after which the associated schedule should be executed.
	 */
	private Set<Integer> scheduleId = new HashSet<>();

	/**
	 * Whether this follow-up execution should only occur if the schedules in
	 * {@link #scheduleId} executed successfully.
	 */
	private boolean onlyAfterSuccess;

	/**
	 * Get the set of schedule IDs after which the associated schedule should
	 * be executed.
	 * @return Set of followed schedule IDs.
	 */
	@NotNull
	public Set<Integer> getScheduleId() {
		if (scheduleId == null) {
			scheduleId = new HashSet<>();
		}

		return scheduleId;
	}

	/**
	 * Set the IDs after which the associated schedule should be executed.
	 * @param scheduleId Set of followed IDs.
	 * @return fluent API
	 */
	public ScheduleFollow setScheduleId(Set<Integer> scheduleId) {
		this.scheduleId = scheduleId;
		return this;
	}

	/**
	 * Whether the associated schedule should only be executed if the followed
	 * schedules were executed successfully.
	 * @return Whether the associated schedule should only be executed if the followed
	 * schedules were executed successfully.
	 */
	public boolean isOnlyAfterSuccess() {
		return onlyAfterSuccess;
	}

	/**
	 * Set whether the associated schedule should only be executed if the followed
	 * schedules were executed successfully.
	 *
	 * @param onlyAfterSuccess Whether the associated schedule should only be executed if the followed
	 * schedules were executed successfully.
	 * @return fluent API
	 */
	public ScheduleFollow setOnlyAfterSuccess(boolean onlyAfterSuccess) {
		this.onlyAfterSuccess = onlyAfterSuccess;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScheduleFollow) {
			ScheduleFollow other = (ScheduleFollow) obj;
			return Objects.deepEquals(this.scheduleId, other.scheduleId) && (this.onlyAfterSuccess == other.onlyAfterSuccess);
		} else {
			return false;
		}
	}
}
