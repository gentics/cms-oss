package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model for UI languages
 */
@XmlRootElement
public class UILanguage implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3951316038999095250L;

	private String code;

	private String name;

	/**
	 * Language Code. This also serves as ID of the language
	 * @return language code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Set the language code
	 * @param code code
	 * @return fluent API
	 */
	public UILanguage setCode(String code) {
		this.code = code;
		return this;
	}

	/**
	 * Language name (in its own language)
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
	public UILanguage setName(String name) {
		this.name = name;
		return this;
	}
}
