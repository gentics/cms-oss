package com.gentics.contentnode.rest.model.task;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing the number of open tasks per source
 */
@XmlRootElement
public class UserTaskCountResponse extends GenericResponse {
	private static final long serialVersionUID = 4416904553271468245L;

	private Map<TaskSource, Integer> counts;

	/**
	 * Create an empty instance
	 */
	public UserTaskCountResponse() {
	}

	/**
	 * Create an instance
	 * @param counts counts per source
	 * @param responseInfo response info
	 */
	public UserTaskCountResponse(Map<TaskSource, Integer> counts, ResponseInfo responseInfo) {
		super(null, responseInfo);
		this.counts = counts;
	}

	/**
	 * Number of open tasks per source
	 * @return counts
	 */
	public Map<TaskSource, Integer> getCounts() {
		return counts;
	}

	/**
	 * Set the counts
	 * @param counts counts
	 * @return fluent API
	 */
	public UserTaskCountResponse setCounts(Map<TaskSource, Integer> counts) {
		this.counts = counts;
		return this;
	}
}
