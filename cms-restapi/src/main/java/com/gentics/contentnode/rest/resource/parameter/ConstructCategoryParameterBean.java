package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for filtering construct categories
 */
public class ConstructCategoryParameterBean {
	/**
	 * ID of the page for which to get construct categories.
	 */
	@QueryParam("pageId")
	public Integer pageId;

	/**
	 * ID of the node for getting constructs linked to a node.
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

}
