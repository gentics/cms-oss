package com.gentics.contentnode.rest.resource.impl.internal;

import java.io.Serializable;

/**
 * REST Model of the Job Progress
 */
public class JobProgress implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1752569786223678865L;

	/**
	 * Number of done total
	 */
	protected int done;

	/**
	 * Number of total total
	 */
	protected int total;

	/**
	 * Timestamp, when the job was started
	 */
	protected int started;

	/**
	 * Timestamp, when the job was finished
	 */
	protected int finished;

	/**
	 * Get done total
	 * @return done total
	 */
	public int getDone() {
		return done;
	}

	/**
	 * Set done total
	 * @param done done total
	 * @return fluent API
	 */
	public JobProgress setDone(int done) {
		this.done = done;
		return this;
	}

	/**
	 * Get total number of total
	 * @return total total
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set total number of total
	 * @param total total total
	 * @return fluent API
	 */
	public JobProgress setTotal(int total) {
		this.total = total;
		return this;
	}

	/**
	 * Timestamp, when the job was started
	 * @return start timestamp
	 */
	public int getStarted() {
		return started;
	}

	/**
	 * Set start timestamp
	 * @param started start timestamp
	 * @return fluent API
	 */
	public JobProgress setStarted(int started) {
		this.started = started;
		return this;
	}

	/**
	 * Timestamp, when the job was finished
	 * @return finish timestamp
	 */
	public int getFinished() {
		return finished;
	}

	/**
	 * Set finish timestamp
	 * @param finished timestamp
	 * @return fluent API
	 */
	public JobProgress setFinished(int finished) {
		this.finished = finished;
		return this;
	}

	/**
	 * Increase done counter by 1
	 */
	public synchronized void incDone() {
		done++;
	}

	/**
	 * Increase done counter by delta
	 * @param delta delta
	 */
	public synchronized void incDone(int delta) {
		done += delta;
	}
}
