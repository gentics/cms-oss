package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.QueryParam;

/**
 * Parameter bean for embed parameters
 */
public class EmbedParameterBean {
	/**
	 * Comma separated list of attributes that contain references to other objects, which shall be embedded into the returned objects.
	 */
	@QueryParam("embed")
	public String embed;

	/**
	 * Set the embed param
	 * @param embed embed
	 * @return fluent API
	 */
	public EmbedParameterBean withEmbed(String embed) {
		this.embed = embed;
		return this;
	}
}
