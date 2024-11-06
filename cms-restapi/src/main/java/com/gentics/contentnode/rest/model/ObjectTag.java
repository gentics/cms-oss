package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectTag extends Tag {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5242980924749351305L;

	private String displayName;

	private String description;

	private Boolean required;

	private Boolean inheritable;

	private Integer categoryId;

	private String categoryName;

	private Integer sortOrder;

	private Boolean readOnly;

	/**
	 * Display name
	 * @return display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the display name
	 * @param displayName display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set description
	 * @param description description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * True when the objectag is required
	 * @return required flag
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Set required flag
	 * @param required flag
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	/**
	 * True when the object tag can be inherited onto other objects (e.g. from a folder to objects contained in the folder, or from a template to pages)
	 * @return inheritable flag
	 */
	public Boolean getInheritable() {
		return inheritable;
	}

	/**
	 * Inheritable flag
	 * @param inheritable flag
	 */
	public void setInheritable(Boolean inheritable) {
		this.inheritable = inheritable;
	}

	/**
	 * Category ID
	 * @return category ID
	 */
	public Integer getCategoryId() {
		return categoryId;
	}

	/**
	 * Set the category ID
	 * @param categoryId category ID
	 */
	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	/**
	 * Category name
	 * @return category name
	 */
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * Set the category name
	 * @param categoryName category name
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * Sort order of the object tag (for display in the object tag list while editing)
	 * @return sort order
	 */
	public Integer getSortOrder() {
		return sortOrder;
	}

	/**
	 * Set the sort order
	 * @param sortOrder sort order
	 */
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * True when the objecttag cannot be modified
	 * @return readonly flag
	 */
	public Boolean getReadOnly() {
		return readOnly;
	}

	/**
	 * Set readOnly flag
	 * @param readOnly flag
	 */
	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}
}
