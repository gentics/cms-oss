package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.JsonNode;

@XmlRootElement
public class TagmapEntryModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6818830924268029623L;

	protected Integer id;

	protected String globalId;

	protected String tagname;

	protected String mapname;

	protected Integer object;

	protected Integer attributeType;

	protected Integer targetType;

	protected Boolean multivalue;

	protected Boolean optimized;

	protected Boolean reserved;

	protected Boolean filesystem;

	protected String foreignlinkAttribute;

	protected String foreignlinkAttributeRule;

	protected String category;

	protected Boolean segmentfield;

	protected Boolean displayfield;

	protected Boolean urlfield;

	protected Boolean noindex;

	protected JsonNode elasticsearch;

	protected String micronodeFilter;

	protected String fragmentName;

	/**
	 * Create an empty instance
	 */
	public TagmapEntryModel() {
	}

	/**
	 * Internal ID
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the internal ID
	 * @param id ID
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Global ID
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId ID
	 */
	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	/**
	 * Tag name (property to resolve for the object)
	 * @return tag name
	 */
	public String getTagname() {
		return tagname;
	}

	/**
	 * Set the tag name
	 * @param tagname tag name
	 */
	public void setTagname(String tagname) {
		this.tagname = tagname;
	}

	/**
	 * Map name (name of the attribute in the ContentRepository)
	 * @return map name
	 */
	public String getMapname() {
		return mapname;
	}

	/**
	 * Set the map name
	 * @param mapname map name
	 */
	public void setMapname(String mapname) {
		this.mapname = mapname;
	}

	/**
	 * Type of the object
	 * @return object type
	 */
	public Integer getObject() {
		return object;
	}

	/**
	 * Set the object type
	 * @param object type
	 */
	public void setObject(Integer object) {
		this.object = object;
	}

	/**
	 * Attribute Type
	 * @return attribute type
	 */
	public Integer getAttributeType() {
		return attributeType;
	}

	/**
	 * Set the attribute type
	 * @param attributeType attribute type
	 */
	public void setAttributeType(Integer attributeType) {
		this.attributeType = attributeType;
	}

	/**
	 * Type of the target object for link attributes
	 * @return target type
	 */
	public Integer getTargetType() {
		return targetType;
	}

	/**
	 * Set the target type
	 * @param targetType target type
	 */
	public void setTargetType(Integer targetType) {
		this.targetType = targetType;
	}

	/**
	 * Multivalue flag
	 * @return multivalue
	 */
	public Boolean getMultivalue() {
		return multivalue;
	}

	/**
	 * Set multivalue flag
	 * @param multivalue flag
	 */
	public void setMultivalue(Boolean multivalue) {
		this.multivalue = multivalue;
	}

	/**
	 * Optimized flag
	 * @return optimized
	 */
	public Boolean getOptimized() {
		return optimized;
	}

	/**
	 * Optimized flag
	 * @param optimized flag
	 */
	public void setOptimized(Boolean optimized) {
		this.optimized = optimized;
	}

	/**
	 * Reserved flag
	 * @return reserved flag
	 */
	public Boolean getReserved() {
		return reserved;
	}

	/**
	 * Set reserved flag
	 * @param reserved flag
	 */
	public void setReserved(Boolean reserved) {
		this.reserved = reserved;
	}

	/**
	 * Filesystem flag
	 * @return filesystem flag
	 */
	public Boolean getFilesystem() {
		return filesystem;
	}

	/**
	 * Set filesystem flag
	 * @param filesystem flag
	 */
	public void setFilesystem(Boolean filesystem) {
		this.filesystem = filesystem;
	}

	/**
	 * Name of the foreign attribute for foreignlink attributes
	 * @return attribute name
	 */
	public String getForeignlinkAttribute() {
		return foreignlinkAttribute;
	}

	/**
	 * Set foreign attribute name
	 * @param foreignlinkAttribute attribute name
	 */
	public void setForeignlinkAttribute(String foreignlinkAttribute) {
		this.foreignlinkAttribute = foreignlinkAttribute;
	}

	/**
	 * Rule for restricting foreign linked objects
	 * @return rule
	 */
	public String getForeignlinkAttributeRule() {
		return foreignlinkAttributeRule;
	}

	/**
	 * Set foreign link rule
	 * @param foreignlinkAttributeRule rule
	 */
	public void setForeignlinkAttributeRule(String foreignlinkAttributeRule) {
		this.foreignlinkAttributeRule = foreignlinkAttributeRule;
	}

	/**
	 * Entry category
	 * @return category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Set the category
	 * @param category category
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * True when the entry is a segmentfield (of a Mesh ContentRepository)
	 * @return true for segmentfield
	 */
	public Boolean getSegmentfield() {
		return segmentfield;
	}

	/**
	 * Set segmentfield flag
	 * @param segmentfield flag
	 */
	public void setSegmentfield(Boolean segmentfield) {
		this.segmentfield = segmentfield;
	}

	/**
	 * True when the entry is a displayfield (of a Mesh ContentRepository)
	 * @return true for displayfield
	 */
	public Boolean getDisplayfield() {
		return displayfield;
	}

	/**
	 * Set displayfield flag
	 * @param displayfield flag
	 */
	public void setDisplayfield(Boolean displayfield) {
		this.displayfield = displayfield;
	}

	/**
	 * True when the entry is a urlfield (of a Mesh ContentRepository)
	 * @return true for url field
	 */
	public Boolean getUrlfield() {
		return urlfield;
	}

	/**
	 * Set the urlfield flag
	 * @param urlfield flag
	 */
	public void setUrlfield(Boolean urlfield) {
		this.urlfield = urlfield;
	}

	/**
	 * True when the entry should be excluded from the indexing
	 * @return true for no indexing
	 */
	public Boolean getNoIndex() {
		return noindex;
	}

	/**
	 * Set the 'exclude from indexing' flag
	 * @param noIndex flag
	 */
	public void setNoIndex(Boolean noIndex) {
		this.noindex = noIndex;
	}

	/**
	 * Get the elasticsearch specific configuration of a Mesh CR
	 * @return elasticsearch config
	 */
	public JsonNode getElasticsearch() {
		return elasticsearch;
	}

	/**
	 * Set elasticsearch config
	 * @param elasticsearch config
	 */
	public void setElasticsearch(JsonNode elasticsearch) {
		this.elasticsearch = elasticsearch;
	}

	/**
	 * Get the micronode filter (for entries of type "micronode")
	 * @return micronode filter
	 */
	public String getMicronodeFilter() {
		return micronodeFilter;
	}

	/**
	 * Set the micronode filter
	 * @param micronodeFilter micronode filter
	 */
	public void setMicronodeFilter(String micronodeFilter) {
		this.micronodeFilter = micronodeFilter;
	}

	/**
	 * Name of the CR Fragment, this entry belongs to. Null, if the entry directly belongs to the ContentRepository.
	 * @return fragment name or null
	 */
	public String getFragmentName() {
		return fragmentName;
	}

	/**
	 * Set the fragment name
	 * @param fragmentName fragment name or null
	 */
	public void setFragmentName(String fragmentName) {
		this.fragmentName = fragmentName;
	}
}
