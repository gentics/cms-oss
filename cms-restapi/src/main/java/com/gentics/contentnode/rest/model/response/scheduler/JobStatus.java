package com.gentics.contentnode.rest.model.response.scheduler;

/**
 * Model of the status of a scheduler job
 */
public class JobStatus {
	protected int id;

	protected int start;

	protected int end;

	protected int returnValue;

	protected String name;

	protected boolean active;

	/**
	 * Job ID
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the Job ID
	 * @param id ID
	 * @return fluent API
	 */
	public JobStatus setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Timestamp of the job start
	 * @return timestamp
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Set start timestamp
	 * @param start timestamp
	 * @return fluent API
	 */
	public JobStatus setStart(int start) {
		this.start = start;
		return this;
	}

	/**
	 * Timestamp of the job end
	 * @return timestamp
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * Set end timestamp
	 * @param end timestamp
	 * @return fluent API
	 */
	public JobStatus setEnd(int end) {
		this.end = end;
		return this;
	}

	/**
	 * Return value of the job execution
	 * @return return value
	 */
	public int getReturnValue() {
		return returnValue;
	}

	/**
	 * Set return value
	 * @param returnValue return value
	 * @return fluent API
	 */
	public JobStatus setReturnValue(int returnValue) {
		this.returnValue = returnValue;
		return this;
	}

	/**
	 * Job Name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the job name
	 * @param name name
	 * @return fluent API
	 */
	public JobStatus setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * True, when the job is currently active (eligible for execution)
	 * @return active flag
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Set active flag
	 * @param active flag
	 * @return fluent API
	 */
	public JobStatus setActive(boolean active) {
		this.active = active;
		return this;
	}
}
