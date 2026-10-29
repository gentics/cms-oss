package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for form data filter parameters.
 */
public class FilterFormDataParameterBean extends FilterParameterBean {
	/**
	 * Use only published objects
	 */
	@QueryParam("publishedOnly")
	public Boolean publishedOnly;
}
