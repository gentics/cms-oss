package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Node;

/**
 * Request for creating a new node.
 */
@XmlRootElement
public class NodeSaveRequest {
	/**
	 * REST model of the node to save.
	 */
	private Node node;

	/**
	 * The description of the node.
	 *
	 * The description of the node is actually the description of its
	 * root folder, but this description would be the only reason to
	 * send a REST model of the root folder with each save request.
	 */
	private String description;

	public NodeSaveRequest() {
	}

	/**
	 * The REST model of the node to save.
	 * 
	 * @return The REST model of the node to save.
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * Set the REST model of the node to save.
	 *
	 * @param node Set the REST model of the node to save.
	 */
	public void setNode(Node node) {
		this.node = node;
	}

	/**
	 * The description of the node.
	 *
	 * @return The description of the node.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description of the node.
	 *
	 * @param description Set the description of the node.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
