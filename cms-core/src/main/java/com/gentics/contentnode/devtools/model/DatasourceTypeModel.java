package com.gentics.contentnode.devtools.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public enum DatasourceTypeModel {
	statical(1), siteminder(2);

	/**
	 * Get the type for the given value
	 * @param value value
	 * @return type or null
	 */
	public static DatasourceTypeModel fromValue(int value) {
		for (DatasourceTypeModel type : values()) {
			if (type.typeValue == value) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Type value
	 */
	private int typeValue;

	/**
	 * Create an instance
	 * @param typeValue type value
	 */
	private DatasourceTypeModel(int typeValue) {
		this.typeValue = typeValue;
	}

	/**
	 * Get the type value
	 * @return type value
	 */
	public int getTypeValue() {
		return typeValue;
	}
}
