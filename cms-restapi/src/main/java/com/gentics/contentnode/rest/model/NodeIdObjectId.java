package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Object containing nodeId and objectId of an object selected for an overview
 */
@XmlRootElement
public class NodeIdObjectId implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1258206434210692447L;

	/**
	 * Node ID
	 */
	private int nodeId;

	/**
	 * Object ID
	 */
	private int objectId;

	/**
	 * Create empty instance
	 */
	public NodeIdObjectId() {
	}

	/**
	 * Create instance with node ID and object ID
	 * @param nodeId node ID
	 * @param objectId object ID
	 */
	public NodeIdObjectId(int nodeId, int objectId) {
		this.nodeId = nodeId;
		this.objectId = objectId;
	}

	/**
	 * Node ID
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
	 * Object ID
	 * @return object ID
	 */
	public int getObjectId() {
		return objectId;
	}

	/**
	 * Set the object ID
	 * @param objectId object ID
	 */
	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	@Override
	public String toString() {
		return String.format("nodeId: %d, objectId: %d", nodeId, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof NodeIdObjectId) {
			NodeIdObjectId other = (NodeIdObjectId) obj;

			return nodeId == other.nodeId && objectId == other.objectId;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, objectId);
	}
}
