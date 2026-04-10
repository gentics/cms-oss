package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter Bean for filter parameter of forms
 */
public class FilterFormParameterBean extends FilterParameterBean {
	/**
	 * Filter by formType
	 */
	@QueryParam("formType")
	public String formType;

	/**
	 * True to return external forms, false for internal, empty for all forms
	 */
	@QueryParam("external")
	public Boolean external;
}
