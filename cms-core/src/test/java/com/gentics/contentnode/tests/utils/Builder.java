package com.gentics.contentnode.tests.utils;

import java.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.PublishableNodeObject;

/**
 * Builder for creating/updating NodeObjects
 *
 * @param <T> type of the NodeObject
 */
public class Builder<T extends NodeObject> {
	protected Class<T> clazz;

	protected T object;

	protected NodeObjectHandler<T> handler;

	protected int timestamp;

	protected boolean save = true;

	protected boolean publish = false;

	protected int publishAt = 0;

	protected boolean takeOffline = false;

	protected int takeOfflineAt = 0;

	protected boolean unlock = false;

	/**
	 * Create an object of given type
	 * @param <T> type
	 * @param clazz object class
	 * @param handler creation handler
	 * @return builder instance
	 */
	public static <T extends NodeObject> Builder<T> create(Class<T> clazz, NodeObjectHandler<T> handler) {
		Objects.requireNonNull(clazz, "Class must not be null");
		return new Builder<>(clazz, null, handler);
	}

	/**
	 * Update an object
	 * @param <T> type
	 * @param object object to update
	 * @param handler update handler
	 * @return builder instance
	 */
	public static <T extends NodeObject> Builder<T> update(T object, NodeObjectHandler<T> handler) {
		Objects.requireNonNull(object, "Object must not be null");
		return new Builder<>(null, object, handler);
	}

	/**
	 * Create an instance
	 * @param clazz object class
	 * @param object object
	 * @param handler handler
	 */
	protected Builder(Class<T> clazz, T object, NodeObjectHandler<T> handler) {
		Objects.requireNonNull(handler, "Handler must not be null");
		this.clazz = clazz;
		this.handler = handler;
		this.object = object;
	}

	/**
	 * Set the transaction time, if set to 0 (which is the default), the current time will be used
	 * @param timestamp timestamp
	 * @return fluent API
	 */
	public Builder<T> at(int timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Enable saving the object after creation/modification (which is the default)
	 * @return fluent API
	 */
	public Builder<T> save() {
		this.save = true;
		return this;
	}

	/**
	 * Disable saving the object
	 * @return fluent API
	 */
	public Builder<T> doNotSave() {
		this.save = false;
		return this;
	}

	/**
	 * Enable publishing the object (if it can be published)
	 * @return fluent API
	 */
	public Builder<T> publish() {
		return publish(0);
	}

	/**
	 * Enable publishing the object (if it can be published)
	 * @param timestamp publish timestamp
	 * @return fluent API
	 */
	public Builder<T> publish(int timestamp) {
		this.publish = true;
		this.publishAt = timestamp;
		return this;
	}

	/**
	 * Enable taking the object offline (if it can be published)
	 * @return fluent API
	 */
	public Builder<T> takeOffline() {
		return takeOffline(0);
	}

	/**
	 * Enable taking the object offline (if it can be published)
	 * @param timestamp offline timestamp
	 * @return fluent API
	 */
	public Builder<T> takeOffline(int timestamp) {
		this.takeOffline = true;
		this.takeOfflineAt = timestamp;
		return this;
	}

	/**
	 * Disable publishing the object (if it can be published), which is the default
	 * @return fluent API
	 */
	public Builder<T> doNotPublish() {
		this.publish = false;
		this.publishAt = 0;
		return this;
	}

	/**
	 * Unlock the object after updating/creating (if the object supports locking)
	 * @return fluent API
	 */
	public Builder<T> unlock() {
		this.unlock = true;
		return this;
	}

	/**
	 * Build the object
	 * @return created/updated object
	 * @throws NodeException
	 */
	public T build() throws NodeException {
		T modifiedObject = null;
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t == null) {
			try (Trx trx = new Trx()) {
				if (timestamp > 0) {
					trx.at(timestamp);
				}
				modifiedObject = build(trx.getTransaction());
				trx.success();
			}
		} else {
			long oldTimestamp = t.getTimestamp();
			if (timestamp > 0) {
				t.setTimestamp(timestamp * 1000L);
			}
			try {
				modifiedObject = build(t);
			} finally {
				t.setTimestamp(oldTimestamp);
			}
		}

		return modifiedObject;
	}

	/**
	 * Internal method to build the object with the given transaction
	 * @param t transaction
	 * @return created/updated object
	 * @throws NodeException
	 */
	protected T build(Transaction t) throws NodeException {
		T modifiedObject = null;
		if (clazz != null) {
			modifiedObject = t.createObject(clazz);
		} else {
			modifiedObject = t.getObject(object, true);
		}
		handler.handle(modifiedObject);
		if (save) {
			modifiedObject.save();

			if (publish && modifiedObject instanceof PublishableNodeObject) {
				((PublishableNodeObject) modifiedObject).publish(publishAt, false);
			}

			if (takeOffline && modifiedObject instanceof PublishableNodeObject) {
				((PublishableNodeObject) modifiedObject).takeOffline(takeOfflineAt);
			}

			if (unlock) {
				modifiedObject.unlock();
			}

			modifiedObject = t.getObject(modifiedObject);
		}
		return modifiedObject;
	}
}
