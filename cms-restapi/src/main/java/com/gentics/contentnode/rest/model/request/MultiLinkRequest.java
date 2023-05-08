package com.gentics.contentnode.rest.model.request;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for linking multiple templates to or unlink multiple templates from folders
 */
@XmlRootElement
public class MultiLinkRequest extends LinkRequest {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6669603136627518071L;

	/**
	 * Set of template IDs (may be globalIds)
	 */
	protected Set<String> templateIds = new HashSet<String>();

	/**
	 * Create an empty instance
	 */
	public MultiLinkRequest() {
		super();
	}

	/**
	 * Template IDs of the templates to be handled. IDs may be local or global IDs
	 * @return template IDs
	 */
	public Set<String> getTemplateIds() {
		return templateIds;
	}

	/**
	 * Set the template IDs
	 * @param templateIds template IDs
	 */
	public void setTemplateIds(Set<String> templateIds) {
		this.templateIds = templateIds;
	}
}
