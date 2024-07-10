package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model of an entry in a ContentRepository Fragment
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class ContentRepositoryFragmentEntryModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7196337282550611615L;

	/**
	 * Local ID
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	private String tagname;

	private String mapname;

	private Integer objType;

	private Integer attributeType;

	private Boolean multivalue;

	private Boolean optimized;

	private Boolean filesystem;

	private Integer targetType;

	private String foreignlinkAttribute;

	private String foreignlinkAttributeRule;

	private String category;

	private Boolean displayfield;

	private Boolean segmentfield;

	private Boolean urlfield;

	private Boolean noIndex;

	private JsonNode elasticsearch;

	private String micronodeFilter;

	/**
	 * Local ID
	 * @return local ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
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
	 * @param globalId global ID
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
	public Integer getObjType() {
		return objType;
	}

	/**
	 * Set the object type
	 * @param object type
	 */
	public void setObjType(Integer objType) {
		this.objType = objType;
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
	 * Set optimized flag
	 * @param optimized flag
	 */
	public void setOptimized(Boolean optimized) {
		this.optimized = optimized;
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
	 * True when the entry should be excluded from indexing
	 * @return true for no indexing
	 */
	public Boolean getNoIndex() {
		return noIndex;
	}

	/**
	 * Set the 'exclude from indexing' flag
	 * @param noIndex flag
	 */
	public void setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
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
	 * Get the micronode filter (for entries of type micronode)
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
}
