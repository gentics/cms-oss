package com.gentics.contentnode.rest.model.response;

import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing node restrictions of user-group assignments
 */
@XmlRootElement
public class NodeRestrictionResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8188742246145729428L;

	protected Set<Integer> nodeIds;

	protected Integer hidden;

	/**
	 * IDs of nodes to which the group assignment is restricted
	 * @return set of node IDs
	 */
	public Set<Integer> getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set node IDs
	 * @param nodeIds node ID set
	 * @return fluent API
	 */
	public NodeRestrictionResponse setNodeIds(Set<Integer> nodeIds) {
		this.nodeIds = nodeIds;
		return this;
	}

	/**
	 * Number of restrictions to hidden nodes
	 * @return number of hidden nodes
	 */
	public Integer getHidden() {
		return hidden;
	}

	/**
	 * Set number of restrictions to hidden nodes
	 * @param hidden number of hidden nodes
	 * @return fluent API
	 */
	public NodeRestrictionResponse setHidden(Integer hidden) {
		this.hidden = hidden;
		return this;
	}
}
