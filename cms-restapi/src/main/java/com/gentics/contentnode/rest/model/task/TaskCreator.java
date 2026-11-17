package com.gentics.contentnode.rest.model.task;

import java.io.Serializable;

/**
 * Creator of a task
 */
public class TaskCreator implements Serializable {
	private static final long serialVersionUID = 2290640713434547126L;

	/**
	 * Type of a creator
	 */
	public static enum Type {
		/**
		 * A user (GenAIx acts as the user of its session)
		 */
		user,

		/**
		 * The system
		 */
		system
	}

	private Type type;

	private Integer id;

	private String name;

	/**
	 * Type of the creator
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
	public TaskCreator setType(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * User ID, null for the system
	 * @return user ID or null
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the user ID
	 * @param id user ID
	 * @return fluent API
	 */
	public TaskCreator setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Display name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the display name
	 * @param name name
	 * @return fluent API
	 */
	public TaskCreator setName(String name) {
		this.name = name;
		return this;
	}
}
