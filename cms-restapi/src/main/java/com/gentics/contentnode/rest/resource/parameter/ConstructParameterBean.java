package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for filtering constructs
 */
public class ConstructParameterBean {
	/**
	 * True to only get changeable constructs, false for only getting not changeable items. Leave empty to get all.
	 */
	@QueryParam("changeable")
	public Boolean changeable;

	/**
	 * ID of the page form which to get constructs.
	 */
	@QueryParam("pageId")
	public Integer pageId;

	/**
	 * ID of the node for getting constructs linked to a node.
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * ID of the category for filtering.
	 */
	@QueryParam("category")
	public Integer categoryId;

	/**
	 * IDs of part types for filtering.
	 */
	@QueryParam("partTypeId")
	public List<Integer> partTypeId;
}
