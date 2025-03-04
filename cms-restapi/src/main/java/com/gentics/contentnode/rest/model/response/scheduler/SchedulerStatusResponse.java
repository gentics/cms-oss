package com.gentics.contentnode.rest.model.response.scheduler;

import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Scheduler status
 */
@XmlRootElement
public class SchedulerStatusResponse extends GenericResponse {
	protected SchedulerStatus status;
	protected SchedulerExecutorStatus executorStatus;

	protected Set<Integer> allowRun;

	/**
	 * Scheduler status
	 * @return status
	 */
	public SchedulerStatus getStatus() {
		return status;
	}

	/**
	 * Set the status
	 * @param status status
	 * @return fluent API
	 */
	public SchedulerStatusResponse setStatus(SchedulerStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * The scheduler executor status.
	 *
	 * @return The scheduler executor status.
	 */
	public SchedulerExecutorStatus getExecutorStatus() {
		return executorStatus;
	}

	/**
	 * Set the scheduler executor status.
	 *
	 * @param executorStatus The scheduler executor status.
	 * @return fluent API
	 */
	public SchedulerStatusResponse setExecutorStatus(SchedulerExecutorStatus executorStatus) {
		this.executorStatus = executorStatus;
		return this;
	}

	/**
	 * IDs of Jobs that are allowed to run, although the scheduler is suspended
	 * @return job IDs
	 */
	public Set<Integer> getAllowRun() {
		return allowRun;
	}

	/**
	 * Set allowed Jobs IDs
	 * @param allowRun job IDs
	 * @return fluent API
	 */
	public SchedulerStatusResponse setAllowRun(Set<Integer> allowRun) {
		this.allowRun = allowRun;
		return this;
	}
}
