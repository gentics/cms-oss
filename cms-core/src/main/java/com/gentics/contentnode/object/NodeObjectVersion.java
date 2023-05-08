package com.gentics.contentnode.object;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * the "version" of one NodeObject - currently this can't be loaded straight through a
 * node factory because version numbers are calculated dynamically.
 */
public class NodeObjectVersion implements Resolvable {
	/**
	 * Transform the node object into its rest model. If the version is null, this returns null
	 */
	public final static Function<NodeObjectVersion, ItemVersion> TRANSFORM2REST = version -> {
		if (version == null) {
			return null;
		} else {
			ItemVersion model = new ItemVersion();
			model.setNumber(version.getNumber());
			model.setTimestamp(version.getDate().getIntTimestamp());
			model.setEditor(ModelBuilder.getUser(version.getEditor()));
			return model;
		}
	};

	/**
	 * Record id
	 */
	private int id;

	private String versionNumber;
	private SystemUser editor;
	private ContentNodeDate date;

	private boolean major;

	/**
	 * Flag to mark the current version
	 */
	private boolean current;

	/**
	 * Flag to mark the last published version
	 */
	private boolean published;

	/**
	 * Create an empty instance
	 */
	public NodeObjectVersion() {
	}

	/**
	 * Create instance
	 * @param id record ID
	 * @param versionNumber version Number
	 * @param editor version editor
	 * @param date version date
	 * @param current true for current version
	 * @param published true for published version
	 */
	public NodeObjectVersion(int id, String versionNumber, SystemUser editor, ContentNodeDate date, boolean current, boolean published) {
		this.id = id;
		this.versionNumber = versionNumber;
		this.editor = editor;
		this.date = date;
		this.current = current;
		this.published = published;
		if (!ObjectTransformer.isEmpty(this.versionNumber)) {
			this.major = this.versionNumber.endsWith(".0");
		}
	}

	/**
	 * returns the name of the version as string - e.g. 1.0, 1.1, 1.2, 2.0, etc.
	 */
	public String getNumber() {
		return versionNumber;
	}

	/**
	 * returns the system user who has created this version.
	 * @return
	 */
	public SystemUser getEditor() {
		return editor;
	}

	/**
	 * Get the version date
	 * @return date
	 */
	public ContentNodeDate getDate() {
		return date;
	}

	/**
	 * Set the version date
	 * @param date date
	 * @return fluent API
	 */
	public NodeObjectVersion setDate(ContentNodeDate date) {
		this.date = date;
		return this;
	}

	/**
	 * Set version date as timestamp
	 * @param timestamp timestamp of the version date
	 * @return fluent API
	 */
	public NodeObjectVersion setDate(int timestamp) {
		return setDate(new ContentNodeDate(timestamp));
	}

	@Override
	public boolean canResolve() {
		return true;
	}

	@Override
	public Object get(String key) {
		if ("number".equals(key)) {
			return getNumber();
		} else if ("editor".equals(key)) {
			return getEditor();
		} else if ("date".equals(key)) {
			return getDate();
		} else if ("major".equals(key)) {
			return new Boolean(isMajor());
		} else if ("published".equals(key)) {
			return new Boolean(isPublished());
		}
		return null;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	/**
	 * Set the version number (and the major flag)
	 * @param versionNumber version number
	 * @return fluent API
	 */
	public NodeObjectVersion setNumber(String versionNumber) {
		this.versionNumber = versionNumber;
		if (!ObjectTransformer.isEmpty(this.versionNumber)) {
			this.major = this.versionNumber.endsWith(".0");
		}
		return this;
	}

	/**
	 * Set flag for major version
	 * @param major true for major version
	 * @return fluent API
	 */
	public NodeObjectVersion setMajor(boolean major) {
		this.major = major;
		return this;
	}

	/**
	 * True iff version is a major version
	 * @return true for major version
	 */
	public boolean isMajor() {
		return major;
	}

	/**
	 * Set the published flag
	 * @param published new value for the published flag
	 * @return fluent API
	 */
	public NodeObjectVersion setPublished(boolean published) {
		this.published = published;
		return this;
	}

	/**
	 * Check whether this is the published version
	 * @return true for the published version, false for any other version
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * Get the id
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set a new id
	 * @param id id
	 * @return fluent API
	 */
	public NodeObjectVersion setId(int id) {
		this.id = id;
		return this;
	}

	public String toString() {
		return String.format("PageVersion {number:%s,date:%s,editor:%s,major:%b,published:%b,current:%b}", getNumber(), getDate(), getEditor(), isMajor(),
				isPublished(), isCurrent());
	}

	/**
	 * Return true when this is the current version of a page, false if not
	 * @return true for the current version, false for any other
	 */
	public boolean isCurrent() {
		return current;
	}

	/**
	 * Set flag for current version
	 * @param current true for current version
	 * @return fluent API
	 */
	public NodeObjectVersion setCurrent(boolean current) {
		this.current = current;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeObjectVersion) {
			NodeObjectVersion other = (NodeObjectVersion) obj;
			return id == other.id;
		} else {
			return false;
		}
	}
}
