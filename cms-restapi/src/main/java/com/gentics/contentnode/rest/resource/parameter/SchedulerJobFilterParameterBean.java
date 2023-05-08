package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.QueryParam;

/**
 * Scheduler jobs filter parameter bean
 */
public class SchedulerJobFilterParameterBean {
	/**
	 * If set to <code>true</code>, only active jobs will be returned. If set to <code>false</code>, only inactive jobs will be returned.
	 * If not set (default), all jobs will be returned.
	 */
	@QueryParam("active")
	public Boolean active;

	/**
	 * If set to <code>true</code>, only failed jobs will be returned. If set to <code>false</code>, only successful jobs will be returned (include jobs that were not yet executed).
	 * If not set (default), all jobs will be returned.
	 */
	@QueryParam("failed")
	public Boolean failed;
}
