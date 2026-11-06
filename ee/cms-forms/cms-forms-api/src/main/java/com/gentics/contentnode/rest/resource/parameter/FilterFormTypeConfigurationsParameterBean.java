package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

public class FilterFormTypeConfigurationsParameterBean extends FilterParameterBean {
	/**
	 * Get only form type configurations assigned to specific node
	 */
	@QueryParam("nodeId")
	public String nodeId;

	/**
	 * Filter by external flag
	 */
	@QueryParam("external")
	public Boolean external;

	/**
	 * Filter by submitOnce flag
	 */
	@QueryParam("submitOnce")
	public Boolean submitOnce;
}
