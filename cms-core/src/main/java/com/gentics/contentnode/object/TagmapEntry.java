package com.gentics.contentnode.object;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Class for tagmap entry objects
 */
@SuppressWarnings("serial")
@TType(TagmapEntry.TYPE_TAGMAPENTRY)
public abstract class TagmapEntry extends AbstractContentObject implements NamedNodeObject {

	/**
	 * TType for tagmap entry
	 */
	public final static int TYPE_TAGMAPENTRY = 10206;

	/**
	 * Function that transforms the node model into the given rest model
	 */
	public final static BiFunction<TagmapEntry, TagmapEntryModel, TagmapEntryModel> NODE2REST = (nodeEntry, entry) -> {
		entry.setId(nodeEntry.getId());
		entry.setGlobalId(nodeEntry.getGlobalId().toString());
		entry.setMapname(nodeEntry.getMapname());
		entry.setTagname(nodeEntry.getTagname());
		entry.setObject(nodeEntry.getObject());
		entry.setAttributeType(nodeEntry.getAttributeTypeId());
		entry.setFilesystem(nodeEntry.isFilesystem());
		entry.setMultivalue(nodeEntry.isMultivalue());
		entry.setOptimized(nodeEntry.isOptimized());
		entry.setReserved(nodeEntry.isStatic());
		entry.setTargetType(nodeEntry.getTargetType());
		entry.setForeignlinkAttribute(nodeEntry.getForeignlinkAttribute());
		entry.setForeignlinkAttributeRule(nodeEntry.getForeignlinkAttributeRule());
		entry.setCategory(nodeEntry.getCategory());
		entry.setNoIndex(nodeEntry.isNoIndex());
		ContentRepository cr = nodeEntry.getContentRepository();
		if (cr != null && cr.getCrType() == Type.mesh) {
			entry.setSegmentfield(nodeEntry.isSegmentfield());
			entry.setDisplayfield(nodeEntry.isDisplayfield());
			entry.setUrlfield(nodeEntry.isUrlfield());
			if (!StringUtils.isEmpty(nodeEntry.getElasticsearch())) {
				ObjectMapper mapper = new ObjectMapper();
				try {
					entry.setElasticsearch(mapper.readValue(nodeEntry.getElasticsearch(), JsonNode.class));
				} catch (IOException e) {
					NodeLogger.getNodeLogger(TagmapEntry.class).error(String.format("Error while deserializing '%s'", nodeEntry.getElasticsearch()), e);
				}
			}
			entry.setMicronodeFilter(nodeEntry.getMicronodeFilter());
		}
		CrFragment fragment = nodeEntry.getContentRepositoryFragment();
		if (fragment != null) {
			entry.setFragmentName(fragment.getName());
		}
		return entry;
	};

	/**
	 * Function that transforms the rest model into the node object
	 */
	public final static BiFunction<TagmapEntryModel, TagmapEntry, TagmapEntry> REST2NODE = (entry, nodeEntry) -> {
		if (entry.getMapname() != null) {
			nodeEntry.setMapname(entry.getMapname());
		}
		if (entry.getTagname() != null) {
			nodeEntry.setTagname(entry.getTagname());
		}
		if (entry.getObject() != null) {
			nodeEntry.setObject(entry.getObject());
		}
		if (entry.getAttributeType() != null) {
			nodeEntry.setAttributeTypeId(entry.getAttributeType());
		}
		if (entry.getFilesystem() != null) {
			nodeEntry.setFilesystem(entry.getFilesystem());
		}
		if (entry.getMultivalue() != null) {
			nodeEntry.setMultivalue(entry.getMultivalue());
		}
		if (entry.getOptimized() != null) {
			nodeEntry.setOptimized(entry.getOptimized());
		}
		if (entry.getReserved() != null) {
			nodeEntry.setStatic(entry.getReserved());
		}
		if (entry.getTargetType() != null) {
			nodeEntry.setTargetType(entry.getTargetType());
		}
		if (entry.getForeignlinkAttribute() != null) {
			nodeEntry.setForeignlinkAttribute(entry.getForeignlinkAttribute());
		}
		if (entry.getForeignlinkAttributeRule() != null) {
			nodeEntry.setForeignlinkAttributeRule(entry.getForeignlinkAttributeRule());
		}
		if (entry.getCategory() != null) {
			nodeEntry.setCategory(entry.getCategory());
		}
		if (entry.getSegmentfield() != null) {
			nodeEntry.setSegmentfield(entry.getSegmentfield());
		}
		if (entry.getDisplayfield() != null) {
			nodeEntry.setDisplayfield(entry.getDisplayfield());
		}
		if (entry.getUrlfield() != null) {
			nodeEntry.setUrlfield(entry.getUrlfield());
		}
		if (entry.getElasticsearch() != null) {
			nodeEntry.setElasticsearch(entry.getElasticsearch().toString());
		}
		if (entry.getMicronodeFilter() != null) {
			nodeEntry.setMicronodeFilter(entry.getMicronodeFilter());
		}
		if (entry.getNoIndex() != null) {
			nodeEntry.setNoIndex(entry.getNoIndex());
		}
		return nodeEntry;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<TagmapEntry, TagmapEntryModel> TRANSFORM2REST = nodeEntry -> {
		return NODE2REST.apply(nodeEntry, new TagmapEntryModel());
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<TagmapEntry>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<TagmapEntry>>();
		resolvableProperties.put("globalId", new NodeObjectProperty<>((o, key) -> o.getGlobalId().toString()));
		resolvableProperties.put("tagname", new NodeObjectProperty<>((o, key) -> o.getTagname()));
		resolvableProperties.put("mapname", new NodeObjectProperty<>((o, key) -> o.getMapname()));
		resolvableProperties.put("object", new NodeObjectProperty<>((o, key) -> o.getObject()));
		resolvableProperties.put("attributeType", new NodeObjectProperty<>((o, key) -> o.getAttributeTypeId()));
		resolvableProperties.put("targetType", new NodeObjectProperty<>((o, key) -> o.getTargetType()));
		resolvableProperties.put("multivalue", new NodeObjectProperty<>((o, key) -> o.isMultivalue()));
		resolvableProperties.put("optimized", new NodeObjectProperty<>((o, key) -> o.isOptimized()));
		resolvableProperties.put("reserved", new NodeObjectProperty<>((o, key) -> o.isStatic()));
		resolvableProperties.put("filesystem", new NodeObjectProperty<>((o, key) -> o.isFilesystem()));
		resolvableProperties.put("foreignlinkAttribute", new NodeObjectProperty<>((o, key) -> o.getForeignlinkAttribute()));
		resolvableProperties.put("foreignlinkAttributeRule", new NodeObjectProperty<>((o, key) -> o.getForeignlinkAttributeRule()));
		resolvableProperties.put("category", new NodeObjectProperty<>((o, key) -> o.getCategory()));
		resolvableProperties.put("segmentfield", new NodeObjectProperty<>((o, key) -> o.isSegmentfield()));
		resolvableProperties.put("displayfield", new NodeObjectProperty<>((o, key) -> o.isDisplayfield()));
		resolvableProperties.put("urlfield", new NodeObjectProperty<>((o, key) -> o.isUrlfield()));
		resolvableProperties.put("fragmentName", new NodeObjectProperty<>((o, key) -> o.getContentRepositoryFragmentName()));
		resolvableProperties.put("micronodeFilter", new NodeObjectProperty<>((o, key) -> o.getMicronodeFilter()));
		resolvableProperties.put("elasticsearch", new NodeObjectProperty<>((o, key) -> o.getElasticsearch()));
		resolvableProperties.put("noindex", new NodeObjectProperty<>((o, key) -> o.isNoIndex()));
	}

	/**
	 * Create an instance
	 * @param id id
	 * @param info info
	 */
	protected TagmapEntry(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<TagmapEntry> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * Get the tagname
	 * @return tagname
	 */
	@FieldGetter("tagname")
	public abstract String getTagname();

	/**
	 * Set the tagname
	 * @param tagname tagname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("tagname")
	public void setTagname(String tagname) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the mapname
	 * @return mapname
	 */
	@FieldGetter("mapname")
	public abstract String getMapname();

	/**
	 * Set the mapname
	 * @param mapname mapname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mapname")
	public void setMapname(String mapname) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the object type to which this entry belongs
	 * @return object type
	 */
	@FieldGetter("object")
	public abstract int getObject();

	/**
	 * Set the object type tp which this entry belongs to
	 * @param object type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("object")
	public void setObject(int object) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the target object type
	 * @return target object type
	 */
	@FieldGetter("objtype")
	public abstract int getTargetType();

	/**
	 * Set the target object type
	 * @param targetType target object type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("objtype")
	public void setTargetType(int targetType) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the attributetype
	 * @return attributetype
	 */
	public abstract AttributeType getAttributetype();

	/**
	 * Get the attribute type id
	 * @return attribute type id
	 */
	@FieldGetter("attributetype")
	public abstract int getAttributeTypeId();

	/**
	 * Set the attribute type id
	 * @param attributeTypeId attribute type id
	 * @throws ReadOnlyException
	 */
	@FieldSetter("attributetype")
	public void setAttributeTypeId(int attributeTypeId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether the entry is multivalue
	 * @return true for multivalue
	 */
	@FieldGetter("multivalue")
	public abstract boolean isMultivalue();

	/**
	 * Set whether the entry is multivalue
	 * @param multivalue true for multivalue
	 * @throws ReadOnlyException
	 */
	@FieldSetter("multivalue")
	public void setMultivalue(boolean multivalue) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether the entry is static
	 * @return true for static
	 */
	@FieldGetter("static")
	public abstract boolean isStatic();

	/**
	 * Set whether the entry is static
	 * @param stat true for static
	 * @throws ReadOnlyException
	 */
	@FieldSetter("static")
	public void setStatic(boolean stat) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether the entry is optimized
	 * @return true for optimized
	 */
	@FieldGetter("optimized")
	public abstract boolean isOptimized();

	/**
	 * Set whether the entry is optimized
	 * @param optimized true for optimized
	 * @throws ReadOnlyException
	 */
	@FieldSetter("optimized")
	public void setOptimized(boolean optimized) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get whether the attribute data shall be written into the filesystem
	 * @return true for publishing into the file
	 */
	@FieldGetter("filesystem")
	public abstract boolean isFilesystem();

	/**
	 * Set the filesystem flag for the entry
	 * @param filesystem
	 * @throws ReadOnlyException
	 */
	@FieldSetter("filesystem")
	public void setFilesystem(boolean filesystem) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the foreign link attribute
	 * @return foreign link attribute
	 */
	@FieldGetter("foreignlinkattribute")
	public abstract String getForeignlinkAttribute();

	/**
	 * Set the foreignlink attribute
	 * @param foreignlinkAttribute foreign link attribute
	 * @throws ReadOnlyException
	 */
	@FieldSetter("foreignlinkattribute")
	public void setForeignlinkAttribute(String foreignlinkAttribute) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the foreign link attribute rule
	 * @return foreign link attribute rule
	 */
	@FieldGetter("foreignlinkattributerule")
	public abstract String getForeignlinkAttributeRule();

	/**
	 * Set the foreign link attribute rule
	 * @param rule foreign link attribute rule
	 * @throws ReadOnlyException
	 */
	@FieldSetter("foreignlinkattributerule")
	public void setForeignlinkAttributeRule(String rule) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the contentrepository of this entry
	 * @return contentrepository
	 * @throws NodeException
	 */
	public abstract ContentRepository getContentRepository() throws NodeException;

	/**
	 * Get the fragment, this entry belongs to, or null
	 * @return fragment or null
	 * @throws NodeException
	 */
	public CrFragment getContentRepositoryFragment() throws NodeException {
		// the default implementation returns null, since per default, Tagmap Entries belong to the ContentRepository
		return null;
	}

	/**
	 * Get the name of the fragment or null
	 * @return name of the fragment
	 * @throws NodeException
	 */
	public String getContentRepositoryFragmentName() throws NodeException {
		CrFragment fragment = getContentRepositoryFragment();
		if (fragment != null) {
			return fragment.getName();
		} else {
			return null;
		}
	}

	/**
	 * Set the contentrepository id
	 * @param contentRepositoryId contentrepository id
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setContentRepositoryId(Integer contentRepositoryId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the category of this entry
	 * @return category of the entry
	 */
	@FieldGetter("category")
	public abstract String getCategory();

	/**
	 * Set the category
	 * @param category category
	 * @throws ReadOnlyException
	 */
	@FieldSetter("category")
	public void setCategory(String category) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether the tagmap entry is the segmentfield
	 * @return true for segmentfield
	 */
	@FieldGetter("segmentfield")
	public abstract boolean isSegmentfield();

	/**
	 * Set segmentfield
	 * @param segmentfield true for segmentfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("segmentfield")
	public void setSegmentfield(boolean segmentfield) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Check whether the tagmap entry is the displayfield
	 * @return true for displayfield
	 */
	@FieldGetter("displayfield")
	public abstract boolean isDisplayfield();

	/**
	 * Set displayfield
	 * @param displayfield true for displayfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("displayfield")
	public void setDisplayfield(boolean displayfield) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Check whether the tagmap entry is a urlfield
	 * @return
	 */
	@FieldGetter("urlfield")
	public abstract boolean isUrlfield();

	/**
	 * Set url field
	 * @param urlfield true for urlfield
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("urlfield")
	public void setUrlfield(boolean urlfield) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Check whether the tagmap entry should be excluded from indexing
	 * @return
	 */
	@FieldGetter("noindex")
	public abstract boolean isNoIndex();

	/**
	 * Set 'exclude from index' flag
	 * @param noIndex true for no indexing
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("noindex")
	public void setNoIndex(boolean noIndex) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get elasticsearch specific configuration for Mesh CR
	 * The configuration is expected to be in JSON format
	 * @return elasticsearch configuration
	 */
	@FieldGetter("elasticsearch")
	public abstract String getElasticsearch();

	/**
	 * Set elasticsearch specific configuration for Mesh CR
	 * @param elasticsearch
	 * @throws ReadOnlyException
	 */
	@FieldSetter("elasticsearch")
	public void setElasticsearch(String elasticsearch) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the micronode filter for entries of type {@link AttributeType#micronode}
	 * @return micronode filter
	 */
	@FieldGetter("micronode_filter")
	public abstract String getMicronodeFilter();

	/**
	 * Set the micronode filter for entries of type {@link AttributeType#micronode}
	 * @param micronodeFilter micronode filter
	 * @throws ReadOnlyException
	 */
	@FieldSetter("micronode_filter")
	public void setMicronodeFilter(String micronodeFilter) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public String getName() {
		return getMapname();
	}

	@Override
	public String toString() {
		switch (getAttributetype()) {
		case link:
			return String.format("%d: %s -> %s (%s to %d, mul: %b, stat: %b, opt: %b, fs: %b, cat: %s)", getObject(), getTagname(), getMapname(),
					getAttributetype(), getTargetType(), isMultivalue(), isStatic(), isOptimized(), isFilesystem(), getCategory());
		case foreignlink:
			return String.format("%d: %s -> %s (%s from %d.%s, mul: %b, stat: %b, opt: %b, fs: %b, cat: %s)", getObject(), getTagname(), getMapname(),
					getAttributetype(), getTargetType(), getForeignlinkAttribute(), isMultivalue(), isStatic(), isOptimized(), isFilesystem(), getCategory());
		default:
			return String.format("%d: %s -> %s (%s, mul: %b, stat: %b, opt: %b, fs: %b, cat: %s)", getObject(), getTagname(), getMapname(), getAttributetype(),
					isMultivalue(), isStatic(), isOptimized(), isFilesystem(), getCategory());
		}
	}

	/**
	 * Enumeration of attribute types
	 */
	public static enum AttributeType {
		/**
		 * Short text
		 */
		text(1, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr, ContentRepositoryModel.Type.mesh),

		/**
		 * Object link
		 */
		link(2, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr, ContentRepositoryModel.Type.mesh),

		/**
		 * Integer
		 */
		integer(3, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr, ContentRepositoryModel.Type.mesh),

		/**
		 * Old (deprecated) binary
		 */
		oldbinary(4, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr),

		/**
		 * Long Text
		 */
		longtext(5, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr),

		/**
		 * Binary data
		 */
		binary(6, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr, ContentRepositoryModel.Type.mesh),

		/**
		 * Foreign link
		 */
		foreignlink(7, ContentRepositoryModel.Type.cr, ContentRepositoryModel.Type.mccr),

		/**
		 * Date
		 */
		date(10, ContentRepositoryModel.Type.mesh),

		/**
		 * Boolean
		 */
		bool(11, ContentRepositoryModel.Type.mesh),

		/**
		 * Micronode
		 */
		micronode(12, ContentRepositoryModel.Type.mesh);

		/**
		 * numerical type
		 */
		protected int type;

		/**
		 * Array of ContentRepository types for which this attribute type can be used
		 */
		protected ContentRepositoryModel.Type[] crTypes;

		/**
		 * Create an instance
		 * @param type numerical type
		 */
		private AttributeType(int type, ContentRepositoryModel.Type...crTypes) {
			this.type = type;
			this.crTypes = crTypes;
		}

		/**
		 * Get the numerical type
		 * @return numerical type
		 */
		public int getType() {
			return type;
		}

		/**
		 * Check whether the attribute type is valid for the given CR type
		 * @param type CR type
		 * @return true iff the attribute type is valid for the CR type
		 */
		public boolean validFor(ContentRepositoryModel.Type type) {
			for (ContentRepositoryModel.Type t : crTypes) {
				if (t == type) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Get the attributetype for the given numerical value
		 * @param type numerical value
		 * @return attribute type or null
		 */
		public static AttributeType getForType(int type) {
			for (AttributeType aType : values()) {
				if (aType.getType() == type) {
					return aType;
				}
			}
			return null;
		}
	}
}
