package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response containing the publish queue counts for nodes
 */
@XmlRootElement
public class PublishQueueResponse extends GenericResponse {
	/**
	 * Serial Version UId
	 */
	private static final long serialVersionUID = -236086423430860880L;

	protected Map<Integer, PublishQueueCounts> nodes;

	/**
	 * Map of node IDs to publish counts
	 * @return map
	 */
	public Map<Integer, PublishQueueCounts> getNodes() {
		return nodes;
	}

	/**
	 * Set the nodes map
	 * @param nodes map
	 * @return fluent API
	 */
	public PublishQueueResponse setNodes(Map<Integer, PublishQueueCounts> nodes) {
		this.nodes = nodes;
		return this;
	}
}
