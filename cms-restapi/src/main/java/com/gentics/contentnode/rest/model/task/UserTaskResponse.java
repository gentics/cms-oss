package com.gentics.contentnode.rest.model.task;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing a single task
 */
@XmlRootElement
public class UserTaskResponse extends GenericResponse {
	private static final long serialVersionUID = -5361432389542017006L;

	private UserTaskModel task;

	/**
	 * Create an empty instance
	 */
	public UserTaskResponse() {
	}

	/**
	 * Create an instance
	 * @param task task
	 * @param responseInfo response info
	 */
	public UserTaskResponse(UserTaskModel task, ResponseInfo responseInfo) {
		super(null, responseInfo);
		this.task = task;
	}

	/**
	 * The task
	 * @return task
	 */
	public UserTaskModel getTask() {
		return task;
	}

	/**
	 * Set the task
	 * @param task task
	 * @return fluent API
	 */
	public UserTaskResponse setTask(UserTaskModel task) {
		this.task = task;
		return this;
	}
}
