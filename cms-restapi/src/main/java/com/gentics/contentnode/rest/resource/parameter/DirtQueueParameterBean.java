package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.QueryParam;

/**
 * Paramter bean for filtering dirt queue entries
 */
public class DirtQueueParameterBean {
	/**
	 * True for returning only failed entries, false for returning not failed entries, null for returning all entries.
	 */
	@QueryParam("failed")
	public Boolean failed;

	/**
	 * Start timestamp for filtering
	 */
	@QueryParam("start")
	public Integer start;

	/**
	 * End timestamp for filtering
	 */
	@QueryParam("end")
	public Integer end;
}
