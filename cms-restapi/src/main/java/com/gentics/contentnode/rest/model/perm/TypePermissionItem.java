package com.gentics.contentnode.rest.model.perm;

import java.io.Serializable;

/**
 * Permission item
 */
public class TypePermissionItem implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8682772682067292227L;

	private PermType type;

	private String label;

	private String description;

	private String category;

	private boolean value;

	private boolean editable;

	/**
	 * Permission type
	 * @return permission type
	 */
	public PermType getType() {
		return type;
	}

	/**
	 * Set type
	 * @param type type
	 * @return fluent API
	 */
	public TypePermissionItem setType(PermType type) {
		this.type = type;
		return this;
	}

	/**
	 * Label
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set label
	 * @param label label
	 * @return fluent API
	 */
	public TypePermissionItem setLabel(String label) {
		this.label = label;
		return this;
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
	 * @return fluent API
	 */
	public TypePermissionItem setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Category
	 * @return category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Set category
	 * @param category category
	 * @return fluent API
	 */
	public TypePermissionItem setCategory(String category) {
		this.category = category;
		return this;
	}

	/**
	 * Permission value. True if permission is granted, false if not
	 * @return value
	 */
	public boolean isValue() {
		return value;
	}

	/**
	 * Set value
	 * @param value value
	 * @return fluent API
	 */
	public TypePermissionItem setValue(boolean value) {
		this.value = value;
		return this;
	}

	/**
	 * True, when the permission may be changed, false if not
	 * @return editable flag
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Set editable flag
	 * @param editable flag
	 * @return fluent API
	 */
	public TypePermissionItem setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	@Override
	public String toString() {
		return String.format("%s, value: %b, editable: %b", type, value, editable);
	}
}
