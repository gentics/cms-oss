package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.QueryParam;

/**
 * Parameter bean for multichannelling
 */
public class MultichannellingParameterBean {
	/**
	 * Node id of the channel when used in multichannelling
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * True to only return inherited items in the given node, false to only get local/localized items, null to get local and inherited items.
	 */
	@QueryParam("inherited")
	public Boolean inherited;
}
