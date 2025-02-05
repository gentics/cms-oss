package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
 * Request to create a new template
 */
@XmlRootElement
public class TemplateCreateRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -761088334916125916L;

	/**
	 * Node ID
	 */
	private Integer nodeId;

	/**
	 * Set of folder IDs (may be globalIds)
	 */
	protected Set<String> folderIds = new HashSet<String>();

	/**
	 * The template to create
	 */
	private Template template;

	/**
	 * Optional node ID to create template in a channel
	 * @return node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 * @return fluent API
	 */
	public TemplateCreateRequest setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * IDs of folders, where the template shall be created (at least one folder has to be set)
	 * @return folder IDs
	 */
	public Set<String> getFolderIds() {
		return folderIds;
	}

	/**
	 * Set folder IDs
	 * @param folderIds folder IDs
	 * @return fluent API
	 */
	public TemplateCreateRequest setFolderIds(Set<String> folderIds) {
		this.folderIds = folderIds;
		return this;
	}

	/**
	 * Template to create
	 * @return template
	 */
	public Template getTemplate() {
		return template;
	}

	/**
	 * Set the template
	 * @param template template
	 * @return fluent API
	 */
	public TemplateCreateRequest setTemplate(Template template) {
		this.template = template;
		return this;
	}
}
