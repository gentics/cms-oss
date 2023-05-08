package com.gentics.contentnode.rest.model.perm;

import java.io.Serializable;

/**
 * Role item
 *
 */
public class RoleItem implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private int id;

	private String label;

	private String description;

	private boolean value;

	private boolean editable;

	/**
	 * Role ID
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the role ID
	 * @param id ID
	 * @return fluent API
	 */
	public RoleItem setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Role label
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
	public RoleItem setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Role description
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
	public RoleItem setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Role assignment. True if role is assigned, false if not
	 * @return value
	 */
	public boolean isValue() {
		return value;
	}

	/**
	 * Set role assignment
	 * @param value flag
	 * @return fluent API
	 */
	public RoleItem setValue(boolean value) {
		this.value = value;
		return this;
	}

	/**
	 * True, when the role may be changed, false if not
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
	public RoleItem setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}
}
