package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for permissions
 */
public class PermsParameterBean {
	/**
	 * Flag to add permission information for the returned items.
	 */
	@QueryParam("perms")
	@DefaultValue("false")
	@McpToolParam(description = "Set to true to add permission information for the returned items.", required = false)
	public boolean perms = false;

	public PermsParameterBean setPerms(boolean perms) {
		this.perms = perms;
		return this;
	}
}
