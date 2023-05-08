package com.gentics.contentnode.object.search;

/**
 * POJO for replaying an Indexer Task
 */
public class ReplayTask {
	/**
	 * Entry ID
	 */
	private int id;

	/**
	 * Index type
	 */
	private IndexType indexType;

	/**
	 * Index language
	 */
	private String indexLanguage;

	/**
	 * Object ID
	 */
	private int objId;

	/**
	 * Indexer Action
	 */
	private IndexerAction action;

	/**
	 * Get the ID
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
	public ReplayTask setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Get the index type
	 * @return type
	 */
	public IndexType getIndexType() {
		return indexType;
	}

	/**
	 * Set the index type
	 * @param indexType type
	 * @return fluent API
	 */
	public ReplayTask setIndexType(IndexType indexType) {
		this.indexType = indexType;
		return this;
	}

	/**
	 * Get the index language (may be null)
	 * @return language (or null)
	 */
	public String getIndexLanguage() {
		return indexLanguage;
	}

	/**
	 * Set the index language
	 * @param indexLanguage language
	 * @return fluent API
	 */
	public ReplayTask setIndexLanguage(String indexLanguage) {
		this.indexLanguage = indexLanguage;
		return this;
	}

	/**
	 * Get the object ID
	 * @return object ID
	 */
	public int getObjId() {
		return objId;
	}

	/**
	 * Set the object ID
	 * @param objId ID
	 * @return fluent API
	 */
	public ReplayTask setObjId(int objId) {
		this.objId = objId;
		return this;
	}

	/**
	 * Get the Indexer action
	 * @return action
	 */
	public IndexerAction getAction() {
		return action;
	}

	/**
	 * Set the Indexer action
	 * @param action action
	 * @return fluent API
	 */
	public ReplayTask setAction(IndexerAction action) {
		this.action = action;
		return this;
	}
}
