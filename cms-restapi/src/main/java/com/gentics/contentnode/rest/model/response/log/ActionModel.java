package com.gentics.contentnode.rest.model.response.log;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Model of a logged action
 */
@XmlRootElement
public class ActionModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8470363660320363760L;

	private String name;

	private String label;

	/**
	 * Create instance
	 */
	public ActionModel() {
	}

	/**
	 * Action name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
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
	 */
	public void setLabel(String label) {
		this.label = label;
	}
}
