package com.gentics.contentnode.rest.model.task;

import java.io.Serializable;
import java.util.Objects;

/**
 * CMS object a task concerns
 */
public class TaskObject implements Serializable {
	private static final long serialVersionUID = 5096406120734316853L;

	private String type;

	private int id;

	private Integer nodeId;

	private String name;

	/**
	 * Object type (<code>page</code>, <code>folder</code>, <code>file</code>, <code>image</code>)
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public TaskObject setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Object ID
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the object ID
	 * @param id ID
	 * @return fluent API
	 */
	public TaskObject setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Node (channel) ID
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
	public TaskObject setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * Object name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public TaskObject setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, id, nodeId);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TaskObject other && Objects.equals(type, other.type) && id == other.id
				&& Objects.equals(nodeId, other.nodeId);
	}
}
