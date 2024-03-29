package com.gentics.contentnode.rest.model.response;

/**
 * REST Model for that status of a single template tag
 */
public class TagStatus {
	protected String name;

	protected int constructId;

	protected String constructName;

	protected int inSync;

	protected int outOfSync;

	protected int incompatible;

	protected int missing;

	/**
	 * Tag name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Construct ID
	 * @return construct ID
	 */
	public int getConstructId() {
		return constructId;
	}

	/**
	 * Construct name
	 * @return construct name
	 */
	public String getConstructName() {
		return constructName;
	}

	/**
	 * Number of pages in sync with this template tag
	 * @return pages in sync
	 */
	public int getInSync() {
		return inSync;
	}

	/**
	 * Number of pages, that have the tag with a different construct ID (including incompatible pages)
	 * @return pages out of sync
	 */
	public int getOutOfSync() {
		return outOfSync;
	}

	/**
	 * Number of pages, that have the tag with a different construct ID, which is not compatible with the construct ID (and therefore cannot be migrated automatically without possible losing data).
	 * @return incompatible pages
	 */
	public int getIncompatible() {
		return incompatible;
	}

	/**
	 * Number of pages, that do not have this tag
	 * @return pages missing the tag
	 */
	public int getMissing() {
		return missing;
	}

	/**
	 * Set name
	 * @param name name
	 * @return fluent API
	 */
	public TagStatus setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Set construct ID
	 * @param constructId construct ID
	 * @return fluent API
	 */
	public TagStatus setConstructId(int constructId) {
		this.constructId = constructId;
		return this;
	}

	/**
	 * Set construct name
	 * @param constructName construct name
	 * @return fluent API
	 */
	public TagStatus setConstructName(String constructName) {
		this.constructName = constructName;
		return this;
	}

	/**
	 * Set pages in sync
	 * @param inSync pages in sync
	 * @return fluent API
	 */
	public TagStatus setInSync(int inSync) {
		this.inSync = inSync;
		return this;
	}

	/**
	 * Set pages out of sync
	 * @param outOfSync pages out of sync
	 * @return fluent API
	 */
	public TagStatus setOutOfSync(int outOfSync) {
		this.outOfSync = outOfSync;
		return this;
	}

	/**
	 * Set incompatible pages
	 * @param incompatible incompatible pages
	 * @return fluent API
	 */
	public TagStatus setIncompatible(int incompatible) {
		this.incompatible = incompatible;
		return this;
	}

	/**
	 * Set pages missing the tag
	 * @param missing pages missing the tag
	 * @return fluent API
	 */
	public TagStatus setMissing(int missing) {
		this.missing = missing;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Tag %s: in snyc: %d, out of sync: %d, incompatible: %d, missing: %d", constructName, inSync, outOfSync, incompatible, missing);
	}
}
