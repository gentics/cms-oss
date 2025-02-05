package com.gentics.contentnode.rest.resource.impl.internal;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing the job status for a background job
 */
@XmlRootElement
public class JobStatus extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5578240465432431245L;

	/**
	 * Flag whether the job is currently running
	 */
	protected boolean running = false;

	/**
	 * Progress
	 */
	protected JobProgress progress;

	/**
	 * Empty constructor
	 */
	public JobStatus() {
	}

	/**
	 * Create an instance with running flag and (automatic) message
	 * @param message job status
	 * @param running running flag
	 */
	public JobStatus(String message, boolean running) {
		super(new Message(Type.INFO, message), new ResponseInfo(ResponseCode.OK, ""));
		this.running = running;
	}

	/**
	 * True when the job is running, false if not
	 * @return true for running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Set whether the job is currently running
	 * @param running true for runnin job
	 * @return fluent API
	 */
	public JobStatus setRunning(boolean running) {
		this.running = running;
		return this;
	}

	/**
	 * Get job progress
	 * @return progress
	 */
	public JobProgress getProgress() {
		return progress;
	}

	/**
	 * Set job progress
	 * @param progress progress
	 * @return fluent API
	 */
	public JobStatus setProgress(JobProgress progress) {
		this.progress = progress;
		return this;
	}
}
