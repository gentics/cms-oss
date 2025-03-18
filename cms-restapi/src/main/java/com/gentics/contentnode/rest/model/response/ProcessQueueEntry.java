package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents an entry in the process queue.
 */
@XmlRootElement
public class ProcessQueueEntry implements Serializable {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 5779024253995313258L;

	/**
	 * The ID of this entry which is unique across the different process
	 * queue tables.
	 */
	private String id;
	/**
	 * The ID of the entry in its process queue.
	 */
	private int entryId;
	/**
	 * The ID of the object.
	 */
	private int objectId;
	/**
	 * The type of the object.
	 */
	private String objectType;
	/**
	 * The process key.
	 */
	private String processKey;
	/**
	 * Addionial data for the process.
	 */
	private String data;
	/**
	 * The state of the entry in the process queue.
	 */
	private String state;
	/**
	 * The creation timestamp of the entry.
	 */
	private long timestamp;

	public ProcessQueueEntry() {
	}

	public ProcessQueueEntry(int entryId, int objectId, String objectType, String processKey, String data, String state, long timestamp) {
		this.id = objectType + "/" + objectId;
		this.entryId = entryId;
		this.objectId = objectId;
		this.objectType = objectType;
		this.processKey = processKey;
		this.data = data;
		this.state = state;
		this.timestamp = timestamp;
	}

	/**
	 * The ID of this entry which is unique across the different process
	 * queue tables.
	 * 
	 * In contrast to {@link #getEntryId getEntryId()} this ID contains the
	 * type of the object, so the correct process queue table can be identified.
	 *
	 * This is expected to be {@link #objectType objectType} <code>+ "/" +</code> {@link #entryId entryId}.
	 * 
	 * @return The ID of this entry which is unique across the different process
	 * queue tables.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Set the ID.
	 *
	 * In contrast to {@link #setEntryId setEntryId()} this ID contains the
	 * type of the object, so the correct process queue table can be identified.
	 *
	 * This is expected to be {@link #objectType objectType} <code>+ "/" +</code> {@link #entryId entryId}.
	 * 
	 * @param id The ID of this entry.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The ID of the entry in its process queue.
	 *
	 * In contrast to {@link #getId getId()} this is just the ID of the entry in its
	 * own process queue table.
	 *
	 * @return The ID of the entry in its process queue.
	 */
	public int getEntryId() {
		return entryId;
	}

	/**
	 * Set the ID.
	 *
	 * In contrast to {@link #getId getId()} this is just the ID of the entry in its
	 * own process queue table.
	 *
	 * @param entryId The ID of this entry.
	 */
	public void setEntryId(int entryId) {
		this.entryId = entryId;
	}

	/**
	 * The ID of the object.
	 *
	 * @return The ID of the object.
	 */
	public int getObjectId() {
		return objectId;
	}

	/**
	 * Set the object ID.
	 *
	 * @param objectId The object ID.
	 */
	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	/**
	 * The type of the object.
	 *
	 * @return The type of the object.
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * Set the object type.
	 *
	 * @param objectType The object type.
	 */
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	/**
	 * The process key.
	 *
	 * @return The process key.
	 */
	public String getProcessKey() {
		return processKey;
	}

	/**
	 * Set the process key.
	 *
	 * @param processKey The process key.
	 */
	public void setProcessKey(String processKey) {
		this.processKey = processKey;
	}

	/**
	 * Addionial data for the process.
	 * 
	 * @return Addionial data for the process.
	 */
	public String getData() {
		return data;
	}

	/**
	 * Set process data.
	 *
	 * @param data The data for the process.
	 */
	public void setData(String data) {
		this.data = data;
	}

	/**
	 * The state of the entry in the process queue.
	 *
	 * @return The state of the entry in the process queue.
	 */
	public String getState() {
		return state;
	}

	/**
	 * Set the state of the entry.
	 *
	 * @param state The state of the entry.
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * The creation timestamp of the entry.
	 *
	 * @return The creation timestamp of the entry.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp.
	 *
	 * @param timestamp The timestamp.
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Returns a string representation of this object containing
	 * most of its fields.
	 * 
	 * @return A string representation of this process queue entry.
	 */
	@Override
	public String toString() {
		return "{ProcessQueueEntry id: " + entryId
			+ ", object id: " + objectId
			+ ", object type: " + objectType
			+ ", process key: " + processKey
			+ ", timestamp: " + timestamp + "}";
	}
}
