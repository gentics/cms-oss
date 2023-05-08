package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import javax.ws.rs.QueryParam;

/**
 * Parameter bean for filtering action log entries
 */
public class ActionLogParameterBean {
	/**
	 * Search string for filtering by user. The string may be contained in the firstname, lastname or login of the user.
	 */
	@QueryParam("user")
	public String user;

	/**
	 * List of action names for filtering
	 */
	@QueryParam("action")
	public Set<String> action;

	/**
	 * List of object type names for filtering
	 */
	@QueryParam("type")
	public Set<String> type;

	/**
	 * Object ID for filtering
	 */
	@QueryParam("objId")
	public Integer objId;

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
