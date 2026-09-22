package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;
import com.gentics.contentnode.rest.model.ObjectPropertyType;

/**
 * Parameter bean for filtering object properties
 */
public class ObjectPropertyParameterBean {
	/**
	 * Filter by object type(s)
	 */
	@QueryParam("type")
	@McpToolParam(description = "Object type(s) to filter by.", required = false)
	public Set<ObjectPropertyType> types;
}
