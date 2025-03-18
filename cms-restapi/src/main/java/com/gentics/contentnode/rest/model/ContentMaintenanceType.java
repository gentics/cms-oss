package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Types of objects, which can be handled with maintenance actions
 */
@XmlEnum(String.class)
public enum ContentMaintenanceType {
	/**
	 * File
	 */
	file(10008),

	/**
	 * Folder
	 */
	folder(10002),

	/**
	 * Page
	 */
	page(10007),

	/**
	 * Form
	 */
	form(10050);

	/**
	 * Object code
	 */
	private int code;

	/**
	 * Create an instance with object type code
	 * @param code code
	 */
	ContentMaintenanceType(int code) {
		this.code = code;
	}

	/**
	 * Get the object type code
	 * @return code
	 */
	public int getCode() {
		return code;
	}
}
