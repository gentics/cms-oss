package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for exporting form data
 */
public class FormDataExportParameterBean {
	/**
	 * Export format (CSV)
	 */
	@QueryParam("format")
	public String format;

	/**
	 * Options for the export format
	 */
	@QueryParam("options")
	public String options;

	/**
	 * Optional language codes
	 */
	@QueryParam("lang")
	public String language;
}
