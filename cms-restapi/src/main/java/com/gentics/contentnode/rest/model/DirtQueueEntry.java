package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * REST Model of a dirt queue entry
 */
public class DirtQueueEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5191637373736561255L;

	protected int id;

	protected int objType;

	protected int objId;

	protected int timestamp;

	protected String label;

	protected boolean failed;

	protected String failReason;

	/**
	 * Entry ID
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
	public DirtQueueEntry setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Object type
	 * @return object type
	 */
	public int getObjType() {
		return objType;
	}

	/**
	 * Set the object type
	 * @param objType object type
	 * @return fluent API
	 */
	public DirtQueueEntry setObjType(int objType) {
		this.objType = objType;
		return this;
	}

	/**
	 * Object ID
	 * @return object ID
	 */
	public int getObjId() {
		return objId;
	}

	/**
	 * Set the object ID
	 * @param objId object ID
	 * @return fluent API
	 */
	public DirtQueueEntry setObjId(int objId) {
		this.objId = objId;
		return this;
	}

	/**
	 * Timestamp
	 * @return timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp
	 * @param timestamp timestamp
	 * @return fluent API
	 */
	public DirtQueueEntry setTimestamp(int timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Action label
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set the label
	 * @param label label
	 * @return fluent API
	 */
	public DirtQueueEntry setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * True, if the dirt queue event failed
	 * @return failed flag
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Set the failed flag
	 * @param failed flag
	 * @return fluent API
	 */
	public DirtQueueEntry setFailed(boolean failed) {
		this.failed = failed;
		return this;
	}

	/**
	 * Failure reason description
	 * @return failure reason
	 */
	public String getFailReason() {
		return failReason;
	}

	/**
	 * Set the failure reason
	 * @param failReason reason
	 * @return fluent API
	 */
	public DirtQueueEntry setFailReason(String failReason) {
		this.failReason = failReason;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DirtQueueEntry) {
			DirtQueueEntry other = (DirtQueueEntry) obj;
			return Objects.equals(id, other.id) && Objects.equals(objType, other.objType)
					&& Objects.equals(objId, other.objId) && Objects.equals(timestamp, other.timestamp)
					&& Objects.equals(label, other.label) && Objects.equals(failed, other.failed)
					&& Objects.equals(failReason, other.failReason);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, objType, objId, timestamp, label, failed, failReason);
	}

	@Override
	public String toString() {
		return String.format("id: %d, objType: %d, objId: %d, timestamp: %d, label: %s, failed: %b, failReason %s", id,
				objType, objId, timestamp, label, failed, failReason);
	}
}
