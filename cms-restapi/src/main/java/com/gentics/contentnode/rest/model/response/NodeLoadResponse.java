package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Node;

/**
 * Response to a request to load a single node
 */
@XmlRootElement
public class NodeLoadResponse extends GenericResponse {

	/**
	 * Node returned in this response
	 */
	private Node node;

	/**
	 * Create an empty instance
	 */
	public NodeLoadResponse() {}

	/**
	 * Create an instance with message, response info and node
	 * @param message message
	 * @param responseInfo response info
	 * @param node node
	 */
	public NodeLoadResponse(Message message, ResponseInfo responseInfo, Node node) {
		super(message, responseInfo);
		this.node = node;
	}

	/**
	 * Returned node (may be null)
	 * @return node
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * Set the node
	 * @param node node
	 */
	public void setNode(Node node) {
		this.node = node;
	}
}
