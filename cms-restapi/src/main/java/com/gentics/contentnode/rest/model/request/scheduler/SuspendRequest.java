package com.gentics.contentnode.rest.model.request.scheduler;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to suspend the scheduler
 */
@XmlRootElement
public class SuspendRequest {
	protected Set<Integer> allowRun;

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
	public void setAllowRun(Set<Integer> allowRun) {
		this.allowRun = allowRun;
	}
}
