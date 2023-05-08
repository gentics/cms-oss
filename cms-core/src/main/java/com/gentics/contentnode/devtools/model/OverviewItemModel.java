package com.gentics.contentnode.devtools.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for the global IDs of selected objects together with the global IDs of nodes/channels for which they were selected
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverviewItemModel {
	/**
	 * Global node ID
	 */
	private String nodeId;

	/**
	 * Global object ID
	 */
	private String id;

	/**
	 * Get the global node ID
	 * @return ID
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Set the global node ID
	 * @param nodeId ID
	 * @return fluent API
	 */
	public OverviewItemModel setNodeId(String nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * Global object ID
	 * @return ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the global object ID
	 * @param id ID
	 * @return fluent API
	 */
	public OverviewItemModel setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public String toString() {
		if (StringUtils.isEmpty(nodeId)) {
			return id;
		} else {
			return String.format("%s/%s", nodeId, id);
		}
	}
}
