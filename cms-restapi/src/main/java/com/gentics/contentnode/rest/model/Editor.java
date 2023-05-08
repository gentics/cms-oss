package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Editor of a node
 */
@XmlEnum(String.class)
public enum Editor {
	AlohaEditor(1), LiveEditor(0);

	/**
	 * Internal code of the editor
	 */
	private int code;

	/**
	 * Create an instance with a code
	 * @param code code
	 */
	Editor(int code) {
		this.code = code;
	}

	/**
	 * Get the code
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the editor by code
	 * @param code code
	 * @return editor or null
	 */
	public static Editor getByCode(int code) {
		for (Editor e : Editor.values()) {
			if (e.code == code) {
				return e;
			}
		}

		return null;
	}
}
