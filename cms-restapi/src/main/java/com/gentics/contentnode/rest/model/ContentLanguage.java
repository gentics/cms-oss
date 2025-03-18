package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Rest Model of a content language
 */
@XmlRootElement
public class ContentLanguage implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1021329590138157419L;

	/**
	 * ID
	 */
	protected Integer id;

	/**
	 * Global ID
	 */
	protected String globalId;

	/**
	 * Name
	 */
	protected String code;

	/**
	 * Code
	 */
	protected String name;

	/**
	 * Create empty instance
	 */
	public ContentLanguage() {
	}

	/**
	 * Language ID
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the language ID
	 * @param id ID
	 * @return fluent API
	 */
	public ContentLanguage setId(Integer id) {
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
	public ContentLanguage setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Language code
	 * @return code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Set the language code
	 * @param code code
	 * @return fluent API
	 */
	public ContentLanguage setCode(String code) {
		this.code = code;
		return this;
	}

	/**
	 * Language name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the language name
	 * @param name name
	 * @return fluent API
	 */
	public ContentLanguage setName(String name) {
		this.name = name;
		return this;
	}
}
