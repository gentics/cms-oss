package com.gentics.contentnode.cluster;

import java.io.Serializable;

import com.gentics.contentnode.object.NodeObject;

/**
 * Cluster message for cache synchronization
 */
public class CacheSyncMessage implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8273634581421172985L;

	/**
	 * Message type
	 */
	protected CacheSyncMessageType type;

	/**
	 * Object class
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * Object ID
	 */
	protected Integer id;

	/**
	 * Get the message type
	 * @return type
	 */
	public CacheSyncMessageType getType() {
		return type;
	}

	/**
	 * Set the message type
	 * @param type type
	 * @return fluent API
	 */
	public CacheSyncMessage setType(CacheSyncMessageType type) {
		this.type = type;
		return this;
	}

	/**
	 * Get the object class
	 * @return object class
	 */
	public Class<? extends NodeObject> getClazz() {
		return clazz;
	}

	/**
	 * Set the object class
	 * @param clazz object class
	 * @return fluent API
	 */
	public CacheSyncMessage setClazz(Class<? extends NodeObject> clazz) {
		this.clazz = clazz;
		return this;
	}

	/**
	 * Get the object ID
	 * @return object ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the object ID
	 * @param id object ID
	 * @return fluent API
	 */
	public CacheSyncMessage setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Execute the handler for the message
	 */
	void execute() {
		if (type != null) {
			type.execute(this);
		}
	}
}
