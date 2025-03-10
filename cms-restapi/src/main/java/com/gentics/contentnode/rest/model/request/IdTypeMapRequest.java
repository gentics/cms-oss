package com.gentics.contentnode.rest.model.request;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request containing a map of objecttype-IDs.
 */
@XmlRootElement
public class IdTypeMapRequest {

	private Map<String, List<String>> ids;

	/**
	 * true to add language variants to pages
	 */
	@QueryParam("langvars")
	@DefaultValue("false")
	public boolean languageVariants;

	/**
	 * Get the type-IDs map.
	 * @return
	 */
	public Map<String, List<String>> getIds() {
		return ids;
	}

	/**
	 * Set the type-IDs map.
	 * @param ids
	 */
	public void setIds(Map<String, List<String>> ids) {
		this.ids = ids;
	}
}
