/*
 * @author herbert
 * @date 29.03.2006
 * @version $Id: PLinkInformation.java,v 1.4 2006-05-29 11:22:09 laurin Exp $
 */
package com.gentics.api.portalnode.connector;

import java.util.Map;

/**
 * A small bean which holds information about a plink.
 * @see com.gentics.api.portalnode.connector.PortalConnectorHelper#replacePLinks(String,
 *      PLinkReplacer)
 */
public class PLinkInformation {

	/**
	 * The content id this plink references.
	 */
	String contentId;

	/**
	 * Additional attributes of a plink.
	 */
	private Map attributes;

	/**
	 * Creates a new instance of PLinkInformation.
	 * @param contentId
	 * @param attributes
	 */
	PLinkInformation(String contentId, Map attributes) {
		this.contentId = contentId;
		this.attributes = attributes;
	}

	/**
	 * Returns the contentid this plink references.
	 * @return contentid of the plink.
	 */
	public String getContentId() {
		return contentId;
	}

	/**
	 * Returns all additional attributes of a plink. (attribute id is not part
	 * of this map and has to be retrieved using {@link #getContentId()})
	 * @return Map of all additional attributes (without contentid).
	 */
	public Map getAttributes() {
		return attributes;
	}
}
