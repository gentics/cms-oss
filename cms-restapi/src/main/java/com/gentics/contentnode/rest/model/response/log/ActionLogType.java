package com.gentics.contentnode.rest.model.response.log;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model of the type of logged objects
 */
@XmlRootElement
public class ActionLogType implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1227596689447943081L;

	private String name;

	private String label;

	/**
	 * Create instance
	 */
	public ActionLogType() {
	}

	/**
	 * Type name
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
	public ActionLogType setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Translated label
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
	public ActionLogType setLabel(String label) {
		this.label = label;
		return this;
	}
}
