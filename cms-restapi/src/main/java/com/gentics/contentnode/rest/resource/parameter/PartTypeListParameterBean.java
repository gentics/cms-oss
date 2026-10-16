package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for part type list requests
 */
public class PartTypeListParameterBean {
	/**
	 * Flag for filtering deprecated part types
	 */
	@QueryParam("deprecated")
	@McpToolParam(description = "True/false to filter by whether the part type is deprecated. Leave unset to get all.", required = false)
	public Boolean deprecated;
}
