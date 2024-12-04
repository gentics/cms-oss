package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST Model of an object property definition
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectProperty implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 704668008005335937L;

	/**
	 * Id of the construct
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Descriptive name
	 */
	private String name;

	/**
	 * Description
	 */
	private String description;

	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Type
	 */
	private Integer type;

	/**
	 * Construct ID
	 */
	private Integer constructId;

	/**
	 * Construct
	 */
	private Construct construct;

	/**
	 * Flag for required
	 */
	private Boolean required;

	/**
	 * Flag for inheritable
	 */
	private Boolean inheritable;

	/**
	 * Flag for synchronizing contentsets
	 */
	private Boolean syncContentset;

	/**
	 * Flag for synchronizing channelsets
	 */
	private Boolean syncChannelset;

	/**
	 * Flag for synchronizing variants
	 */
	private Boolean syncVariants;

	/**
	 * Flag for restricted
	 */
	private Boolean restricted;

	/**
	 * Category ID
	 */
	private Integer categoryId;

	/**
	 * Category
	 */
	private ObjectPropertyCategory category;

	/**
	 * Map of names, separated by language code
	 */
	private Map<String, String> nameI18n;

	/**
	 * Map of descriptions, separated by language code
	 */
	private Map<String, String> descriptionI18n;

	/**
	 * Create empty instance
	 */
	public ObjectProperty() {
	}

	/**
	 * Internal ID of the object property definition
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public ObjectProperty setId(Integer id) {
		this.id = id;
		return this;
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
	 * @return fluent API
	 */
	public ObjectProperty setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Name in the current language
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public ObjectProperty setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Description in the current language
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description description
	 * @return fluent API
	 */
	public ObjectProperty setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Keyword
	 * @return keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Set the keyword
	 * @param keyword keyword
	 * @return fluent API
	 */
	public ObjectProperty setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Type of objects, this object property definition is for
	 * @return type
	 */
	public Integer getType() {
		return type;
	}

	/**
	 * Object type
	 * @param type type
	 * @return fluent API
	 */
	public ObjectProperty setType(Integer type) {
		this.type = type;
		return this;
	}

	/**
	 * Internal construct ID
	 * @return construct ID
	 */
	public Integer getConstructId() {
		return constructId;
	}

	/**
	 * Set the construct ID
	 * @param constructId construct ID
	 * @return fluent API
	 */
	public ObjectProperty setConstructId(Integer constructId) {
		this.constructId = constructId;
		return this;
	}

	/**
	 * Construct used by the object property (may be null, if not embedded in the response)
	 * @return construct
	 */
	public Construct getConstruct() {
		return construct;
	}

	/**
	 * Set the construct if embedded or null
	 * @param construct construct or null
	 * @return fluent API
	 */
	public ObjectProperty setConstruct(Construct construct) {
		this.construct = construct;
		return this;
	}

	/**
	 * True if the object property is required, false if not
	 * @return true for required
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Set whether required
	 * @param required true for required
	 * @return fluent API
	 */
	public ObjectProperty setRequired(Boolean required) {
		this.required = required;
		return this;
	}

	/**
	 * True if the object property is inheritable, false if not
	 * @return true for inheritable
	 */
	public Boolean getInheritable() {
		return inheritable;
	}

	/**
	 * Set true for inheritable
	 * @param inheritable true for inheritable
	 * @return fluent API
	 */
	public ObjectProperty setInheritable(Boolean inheritable) {
		this.inheritable = inheritable;
		return this;
	}

	/**
	 * True if the object property is synchronized for all languages (only for pages)
	 * @return true for synchronized
	 */
	public Boolean getSyncContentset() {
		return syncContentset;
	}

	/**
	 * Set true for synchronization for all languages
	 * @param syncContentset flag
	 * @return fluent API
	 */
	public ObjectProperty setSyncContentset(Boolean syncContentset) {
		this.syncContentset = syncContentset;
		return this;
	}

	/**
	 * True if the object property is synchronized for all channel variants
	 * @return true for synchronized
	 */
	public Boolean getSyncChannelset() {
		return syncChannelset;
	}

	/**
	 * Set true for synchronization for all channel variants
	 * @param syncChannelset flag
	 * @return fluent API
	 */
	public ObjectProperty setSyncChannelset(Boolean syncChannelset) {
		this.syncChannelset = syncChannelset;
		return this;
	}

	/**
	 * True if the object property is synchronized for all page variants
	 * @return true for synchronized
	 */
	public Boolean getSyncVariants() {
		return syncVariants;
	}

	/**
	 * Set true for synchronization for all page variants
	 * @param syncVariants
	 * @return fluent API
	 */
	public ObjectProperty setSyncVariants(Boolean syncVariants) {
		this.syncVariants = syncVariants;
		return this;
	}

	/**
	 * True if the object property is restricted
	 * @return true for restricted
	 */
	public Boolean getRestricted() {
		return restricted;
	}

	/**
	 * Set true for restricted
	 * @param restricted flag
	 * @return fluent API
	 */
	public ObjectProperty setRestricted(Boolean restricted) {
		this.restricted = restricted;
		return this;
	}

	/**
	 * Get the category ID (may be null)
	 * @return category ID
	 */
	public Integer getCategoryId() {
		return categoryId;
	}

	/**
	 * Set the category ID (may be null)
	 * @param categoryId category ID
	 * @return fluent API
	 */
	public ObjectProperty setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
		return this;
	}

	/**
	 * Category used by the object property (may be null, if not embedded in the response)
	 * @return construct
	 */
	public ObjectPropertyCategory getCategory() {
		return category;
	}

	/**
	 * Set the category
	 * @param category category or null
	 * @return fluent API
	 */
	public ObjectProperty setCategory(ObjectPropertyCategory category) {
		this.category = category;
		return this;
	}

	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	public ObjectProperty setNameI18n(Map<String, String> i18nName) {
		this.nameI18n = i18nName;
		return this;
	}

	public ObjectProperty setName(String name, String language) {
		if (this.nameI18n == null) {
			this.nameI18n = new HashMap<>();
		}
		this.nameI18n.put(language, name);
		return this;
	}

	public Map<String, String> getDescriptionI18n() {
		return descriptionI18n;
	}

	public ObjectProperty setDescriptionI18n(Map<String, String> i18nDescription) {
		this.descriptionI18n = i18nDescription;
		return this;
	}

	public ObjectProperty setDescription(String name, String language) {
		if (this.descriptionI18n == null) {
			this.descriptionI18n = new HashMap<>();
		}
		this.descriptionI18n.put(language, name);
		return this;
	}
}
