package com.gentics.contentnode.rest.model.task;

import java.io.Serializable;
import java.util.Objects;

/**
 * Recipient of a task
 */
public class TaskAssignee implements Serializable {
	private static final long serialVersionUID = -4417512313003412690L;

	/**
	 * Type of a recipient
	 */
	public static enum Type {
		/**
		 * A single user
		 */
		user,

		/**
		 * A group, all direct members see the task
		 */
		group
	}

	private Type type;

	private int id;

	private String name;

	/**
	 * Create an empty instance
	 */
	public TaskAssignee() {
	}

	/**
	 * Create an instance
	 * @param type type
	 * @param id ID of the user or group
	 */
	public TaskAssignee(Type type, int id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * Type of the recipient
	 * @return type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public TaskAssignee setType(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * ID of the user or group
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public TaskAssignee setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Name (read only)
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
	public TaskAssignee setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, id);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TaskAssignee other && type == other.type && id == other.id;
	}

	@Override
	public String toString() {
		return "%s %d".formatted(type, id);
	}
}
