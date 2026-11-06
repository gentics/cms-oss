package com.gentics.contentnode.cluster;

import java.io.Serializable;

/**
 * Cluster message for instant publishing (to synchronize with publish process running on master)
 */
public class InstantPublishSyncMessage implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3290907456442175133L;

	protected boolean failed;

	protected int objectId;

	protected int objectType;

	/**
	 * True, if instant publishing failed
	 * @return true for failed instant publishing
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Set whether instant publishing failed
	 * @param failed true if instant publishing failed
	 * @return fluent API
	 */
	public InstantPublishSyncMessage setFailed(boolean failed) {
		this.failed = failed;
		return this;
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
	 * @return fluent API
	 */
	public InstantPublishSyncMessage setObjectId(int objectId) {
		this.objectId = objectId;
		return this;
	}

	/**
	 * Object Type
	 * @return object type
	 */
	public int getObjectType() {
		return objectType;
	}

	/**
	 * Set the object type
	 * @param objectType object type
	 * @return fluent API
	 */
	public InstantPublishSyncMessage setObjectType(int objectType) {
		this.objectType = objectType;
		return this;
	}

	@Override
	public String toString() {
		return String.format("InstantPublishSyncMessage object: %d.%d, failed: %b", objectType, objectId, failed);
	}
}
