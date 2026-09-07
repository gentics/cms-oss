package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for filtering action log entries
 */
public class ActionLogParameterBean {
	/**
	 * Search string for filtering by user. The string may be contained in the <code>firstname</code>, <code>lastname</code> or <code>login</code> of the user.
	 */
	@QueryParam("user")
	@McpToolParam(description = "Search string for filtering by user. May be contained in the firstname, lastname or login of the user.", required = false)
	public String user;

	/**
	 * List of action names for filtering
	 */
	@QueryParam("action")
	@McpToolParam(description = "Action names to filter by.", required = false)
	public Set<String> action;

	/**
	 * List of object type names for filtering
	 */
	@QueryParam("type")
	@McpToolParam(description = "Object type names to filter by.", required = false)
	public Set<String> type;

	/**
	 * Object ID for filtering
	 */
	@QueryParam("objId")
	@McpToolParam(description = "Object ID to filter by.", required = false)
	public Integer objId;

	/**
	 * Start timestamp for filtering
	 */
	@QueryParam("start")
	@McpToolParam(description = "Start timestamp (inclusive, epoch seconds) to filter by.", required = false)
	public Integer start;

	/**
	 * End timestamp for filtering
	 */
	@QueryParam("end")
	@McpToolParam(description = "End timestamp (inclusive, epoch seconds) to filter by.", required = false)
	public Integer end;
}
