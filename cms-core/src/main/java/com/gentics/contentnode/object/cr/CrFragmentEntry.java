package com.gentics.contentnode.object.cr;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.object.NamedNodeObject;
import com.gentics.contentnode.object.NodeObjectWithModel;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;

/**
 * Interface for CR Fragment Entry NodeObjects
 */
@TType(CrFragmentEntry.TYPE_CR_FRAGMENT_ENTRY)
public interface CrFragmentEntry extends NodeObjectWithModel<ContentRepositoryFragmentEntryModel>, Resolvable, NamedNodeObject {
	public static final int TYPE_CR_FRAGMENT_ENTRY = 10302;

	/**
	 * Get the CR Fragment ID
	 * @return CR Fragment ID
	 */
	@FieldGetter("cr_fragment_id")
	int getCrFragmentId();

	/**
	 * Set the CR Fragment ID
	 * @param crFragmentId CR Fragment ID
	 * @throws NodeException
	 */
	@FieldSetter("cr_fragment_id")
	default void setCrFragmentId(int crFragmentId) throws NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the tagname
	 * @return tagname
	 */
	@FieldGetter("tagname")
	String getTagname();

	/**
	 * Set the tagname
	 * @param tagname tagname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("tagname")
	default void setTagname(String tagname) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the mapname
	 * @return mapname
	 */
	@FieldGetter("mapname")
	String getMapname();

	/**
	 * Set the mapname
	 * @param mapname mapname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mapname")
	default void setMapname(String mapname) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the object type to which this entry belongs
	 * @return object type
	 */
	@FieldGetter("obj_type")
	int getObjType();

	/**
	 * Set the object type to which this entry belongs to
	 * @param objType type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("obj_type")
	default void setObjType(int objType) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the target object type
	 * @return target object type
	 */
	@FieldGetter("target_type")
	int getTargetType();

	/**
	 * Set the target object type
	 * @param targetType target object type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("target_type")
	default void setTargetType(int targetType) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the attributetype
	 * @return attributetype
	 */
	AttributeType getAttributetype();

	/**
	 * Get the attribute type id
	 * @return attribute type id
	 */
	@FieldGetter("attribute_type")
	int getAttributeTypeId();

	/**
	 * Set the attribute type id
	 * @param attributeTypeId attribute type id
	 * @throws ReadOnlyException
	 */
	@FieldSetter("attribute_type")
	default void setAttributeTypeId(int attributeTypeId) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get whether the entry is multivalue
	 * @return true for multivalue
	 */
	@FieldGetter("multivalue")
	boolean isMultivalue();

	/**
	 * Set whether the entry is multivalue
	 * @param multivalue true for multivalue
	 * @throws ReadOnlyException
	 */
	@FieldSetter("multivalue")
	default void setMultivalue(boolean multivalue) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get whether the entry is optimized
	 * @return true for optimized
	 */
	@FieldGetter("optimized")
	boolean isOptimized();

	/**
	 * Set whether the entry is optimized
	 * @param optimized true for optimized
	 * @throws ReadOnlyException
	 */
	@FieldSetter("optimized")
	default void setOptimized(boolean optimized) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get whether the attribute data shall be written into the filesystem
	 * @return true for publishing into the file
	 */
	@FieldGetter("filesystem")
	boolean isFilesystem();

	/**
	 * Set the filesystem flag for the entry
	 * @param filesystem
	 * @throws ReadOnlyException
	 */
	@FieldSetter("filesystem")
	default void setFilesystem(boolean filesystem) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the foreign link attribute
	 * @return foreign link attribute
	 */
	@FieldGetter("foreignlink_attribute")
	String getForeignlinkAttribute();

	/**
	 * Set the foreignlink attribute
	 * @param foreignlinkAttribute foreign link attribute
	 * @throws ReadOnlyException
	 */
	@FieldSetter("foreignlink_attribute")
	default void setForeignlinkAttribute(String foreignlinkAttribute) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the foreign link attribute rule
	 * @return foreign link attribute rule
	 */
	@FieldGetter("foreignlink_attribute_rule")
	String getForeignlinkAttributeRule();

	/**
	 * Set the foreign link attribute rule
	 * @param rule foreign link attribute rule
	 * @throws ReadOnlyException
	 */
	@FieldSetter("foreignlink_attribute_rule")
	default void setForeignlinkAttributeRule(String rule) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the category of this entry
	 * @return category of the entry
	 */
	@FieldGetter("category")
	String getCategory();

	/**
	 * Set the category
	 * @param category category
	 * @throws ReadOnlyException
	 */
	@FieldSetter("category")
	default void setCategory(String category) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Check whether the tagmap entry is the segmentfield
	 * @return true for segmentfield
	 */
	@FieldGetter("segmentfield")
	boolean isSegmentfield();

	/**
	 * Set segmentfield
	 * @param segmentfield true for segmentfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("segmentfield")
	default void setSegmentfield(boolean segmentfield) throws ReadOnlyException, NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Check whether the tagmap entry is the displayfield
	 * @return true for displayfield
	 */
	@FieldGetter("displayfield")
	boolean isDisplayfield();

	/**
	 * Set displayfield
	 * @param displayfield true for displayfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("displayfield")
	default void setDisplayfield(boolean displayfield) throws ReadOnlyException, NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Check whether the tagmap entry is a urlfield
	 * @return
	 */
	@FieldGetter("urlfield")
	boolean isUrlfield();

	/**
	 * Set url field
	 * @param urlfield true for urlfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("urlfield")
	default void setUrlfield(boolean urlfield) throws ReadOnlyException, NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get elasticsearch specific configuration for Mesh CR
	 * The configuration is expected to be in JSON format
	 * @return elasticsearch configuration
	 */
	@FieldGetter("elasticsearch")
	String getElasticsearch();

	/**
	 * Set elasticsearch specific configuration for Mesh CR
	 * @param elasticsearch
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("elasticsearch")
	default void setElasticsearch(String elasticsearch) throws ReadOnlyException, NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the micronode filter for entries of type {@link AttributeType#micronode}
	 * @return micronode filter
	 */
	@FieldGetter("micronode_filter")
	String getMicronodeFilter();

	/**
	 * Set the micronode filter for entries of type {@link AttributeType#micronode}
	 * @param micronodeFilter micronode filter
	 * @throws ReadOnlyException
	 */
	@FieldSetter("micronode_filter")
	default void setMicronodeFilter(String micronodeFilter) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	@Override
	default String getName() {
		return getMapname();
	}
}
