package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * REST Model of an Item Version
 */
@XmlRootElement
public class ItemVersion implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9007436347923979683L;

	/**
	 * Version Number
	 */
	private String number;

	/**
	 * Timestamp
	 */
	private int timestamp;

	/**
	 * User who created the version
	 */
	private User editor;

	/**
	 * Default constructor
	 */
	public ItemVersion() {}

	/**
	 * Version number
	 * @return version number
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * Version timestamp
	 * @return version timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Editor of the version
	 * @return editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Set the version number
	 * @param number version number
	 * @return fluent API
	 */
	public ItemVersion setNumber(String number) {
		this.number = number;
		return this;
	}

	/**
	 * Set the version timestamp
	 * @param timestamp version timestamp
	 * @return fluent API
	 */
	public ItemVersion setTimestamp(int timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Set the editor
	 * @param editor editor
	 * @return fluent API
	 */
	public ItemVersion setEditor(User editor) {
		this.editor = editor;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Version %s @%d", number, timestamp);
	}
}
