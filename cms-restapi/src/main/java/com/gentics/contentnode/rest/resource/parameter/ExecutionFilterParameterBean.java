package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Scheduler execution filter parameter bean
 */
public class ExecutionFilterParameterBean {

	/**
	 * Filter for result status.
	 *
	 * <p>
	 *     If set to <code>true</code>, only failed executions will be returned.
	 *     If set to <code>false</code>, only successful executions will be
	 *     returned (include executions that were not yet executed). If not set
	 *     (default), all jobs will be returned.
	 * </p>
	 */
	@QueryParam("failed")
	public Boolean failed;

	/**
	 * Filter for minimum starting time.
	 *
	 * <p>
	 *     Only executions which have been started <em>after</em> the given
	 *     timestamp will be returned.
	 * </p>
	 */
	@QueryParam("ts_min")
	public Integer timestampMin;

	/**
	 * Filter for maximum starting time.
	 *
	 * <p>
	 *     Only executions which have been started <em>before</em> the given
	 *     timestamp will be returned.
	 * </p>
	 *
	 * <p>
	 *     <strong>Note</strong> that if {@link #timestampMax} is less than
	 *     {@link #timestampMin} it will be ignored.
	 * </p>
	 */
	@QueryParam("ts_max")
	public Integer timestampMax;
}
