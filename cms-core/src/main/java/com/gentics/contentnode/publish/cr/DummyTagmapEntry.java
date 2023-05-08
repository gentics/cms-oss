package com.gentics.contentnode.publish.cr;

/**
 * Dummy Tagmap Entry
 */
public class DummyTagmapEntry implements TagmapEntryRenderer {
	/**
	 * mapname
	 */
	protected String mapname;

	/**
	 * tagname
	 */
	protected String tagname;

	/**
	 * attribute type
	 */
	protected int attributeType;

	/**
	 * object type
	 */
	protected int objectType;

	/**
	 * target type (if the attribute is a link)
	 */
	protected int targetType;

	/**
	 * Create a new instance of the tagmap entry
	 * @param objectType object type
	 * @param tagname tagname
	 * @param mapname mapname
	 * @param attributeType attribute type
	 * @param targetType target type for link attributes
	 */
	public DummyTagmapEntry(int objectType, String tagname, String mapname, int attributeType, int targetType) {
		this.mapname = mapname;
		this.tagname = tagname;
		this.attributeType = attributeType;
		this.objectType = objectType;
		this.targetType = targetType;
	}

	@Override
	public String getMapname() {
		return mapname;
	}

	@Override
	public String getTagname() {
		return tagname;
	}

	@Override
	public int getObjectType() {
		return objectType;
	}

	@Override
	public int getTargetType() {
		return targetType;
	}

	@Override
	public int getAttributeType() {
		return attributeType;
	}

	@Override
	public boolean isMultivalue() {
		return false;
	}

	@Override
	public boolean canSkip() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("%d: %s -> %s (%s, mul: %b)", getObjectType(), getTagname(), getMapname(), getAttributeType(), isMultivalue());
	}
}
