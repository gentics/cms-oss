package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Template;

/**
 * This is a simple data class that references an object's entry in the database
 * as well as all of its localized objects.
 *
 * @author escitalopram
 *
 */
public class DisinheritableObjectReference {
	/**
	 * Returns the referenced object's ID
	 * @return the referenced object's ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the type ID of the referenced object
	 * @return the type ID of the referenced object
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns whether the referenced object is a master object
	 * @return true iff the referenced object is a master object
	 */
	public boolean isMaster() {
		return master;
	}

	/**
	 * Returns whether the referenced object is disinheritable.
	 * @return true iff the referenced object is disinheritable
	 */
	public boolean isDisinheritable() {
		return type != Template.TYPE_TEMPLATE;
	}

	/**
	 * Creates a new instance.
	 * @param id ID of the referenced object
	 * @param type type ID of the referenced object
	 * @param channel_id channel ID of the referenced object
	 * @param master true iff the references object is a master object
	 */
	public DisinheritableObjectReference(int id, int type, int channel_id, boolean master) {
		super();
		this.id = id;
		this.type = type;
		this.channelId = channel_id;
		this.master = master;
	}

	/**
	 * Returns the referenced object's channel ID.
	 * @return the referenced object's channel ID
	 */
	public int getChannelId() {
		return channelId;
	}

	/**
	 * Get the referenced object
	 * @return referenced object
	 * @throws NodeException
	 */
	public NodeObject getObject() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(t.getClass(type), id, -1, false);
	}

	private final int id;
	private final int type;
	private final int channelId;
	private final boolean master;

	@Override
	public boolean equals(Object other) {
		if (other instanceof DisinheritableObjectReference) {
			DisinheritableObjectReference objectref = (DisinheritableObjectReference) other;
			return id == objectref.id
					&& type == objectref.type
					&& channelId == objectref.channelId
					&& master == objectref.master;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id * 17 + type * 41 + channelId * 241 + (master ? 101 : 0);
	}

	@Override
	public String toString() {
		return "{Type: " + type + ", ID: " + id + ". Channel " + channelId + "}";
	}
}
