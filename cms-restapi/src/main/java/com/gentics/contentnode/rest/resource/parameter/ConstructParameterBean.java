package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for filtering constructs
 */
public class ConstructParameterBean {
	/**
	 * True to only get changeable constructs, false for only getting not changeable items. Leave empty to get all.
	 */
	@QueryParam("changeable")
	@McpToolParam(description = "True to only get changeable constructs, false for only getting not changeable "
			+ "items. Leave unset to get all.", required = false)
	public Boolean changeable;

	/**
	 * ID of the page form which to get constructs.
	 */
	@QueryParam("pageId")
	@McpToolParam(description = "ID of the page from which to get constructs.", required = false)
	public Integer pageId;

	/**
	 * ID of the node for getting constructs linked to a node.
	 */
	@QueryParam("nodeId")
	@McpToolParam(description = "ID of the node for getting constructs linked to a node.", required = false)
	public Integer nodeId;

	/**
	 * ID of the category for filtering.
	 */
	@QueryParam("category")
	@McpToolParam(description = "ID of the category for filtering.", required = false)
	public Integer categoryId;

	/**
	 * IDs of part types for filtering.
	 */
	@QueryParam("partTypeId")
	@McpToolParam(description = "IDs of part types for filtering.", required = false)
	public List<Integer> partTypeId;
}
