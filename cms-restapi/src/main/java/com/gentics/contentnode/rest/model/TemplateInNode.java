package com.gentics.contentnode.rest.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Template in a node
 */
@XmlRootElement
public class TemplateInNode extends Template {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6004725271352087256L;

	private int nodeId;

	private List<String> assignedNodes;

	/**
	 * ID of the node to which the template is assigned
	 * @return node ID
	 */
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * List of node names to which this template is assigned
	 * @return node names
	 */
	public List<String> getAssignedNodes() {
		return assignedNodes;
	}

	/**
	 * Set assigned node names
	 * @param assignedNodes node names
	 */
	public void setAssignedNodes(List<String> assignedNodes) {
		this.assignedNodes = assignedNodes;
	}
}
