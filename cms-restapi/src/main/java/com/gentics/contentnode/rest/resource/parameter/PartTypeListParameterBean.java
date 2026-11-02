package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for part type list requests
 */
public class PartTypeListParameterBean {
	/**
	 * Flag for filtering deprecated part types
	 */
	@QueryParam("deprecated")
	public Boolean deprecated;
}
