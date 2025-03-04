package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Types of objects having object properties
 */
@XmlEnum(String.class)
public enum ObjectPropertyType {
	/**
	 * Folder
	 */
	folder(10002),

	/**
	 * Template
	 */
	template(10006),

	/**
	 * Page
	 */
	page(10007),

	/**
	 * File
	 */
	file(10008),

	/**
	 * Image
	 */
	image(10011);

	/**
	 * Internal code
	 */
	private int code;

	/**
	 * Create an instance
	 * @param code code
	 */
	ObjectPropertyType(int code) {
		this.code = code;
	}

	/**
	 * Get the code
	 * @return code
	 */
	public int getCode() {
		return code;
	}
}
