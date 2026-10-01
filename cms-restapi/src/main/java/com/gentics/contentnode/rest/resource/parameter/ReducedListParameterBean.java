package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for the reduced lists
 */
public class ReducedListParameterBean {

	/**
	 * true if the list should be reduced to the unique template occurrences only. valid only when {@link InFolderParameterBean#recursive} flag is set.
	 */
	@QueryParam("reduce")
	@DefaultValue("false")
	@McpToolParam(description = "True if the list should be reduced to the unique template occurrences only. Only "
			+ "valid when the 'recursive' flag is set.", required = false)
	public boolean reduce = false;

	public ReducedListParameterBean setReduce(boolean reduce) {
		this.reduce = reduce;
		return this;
	}
}
