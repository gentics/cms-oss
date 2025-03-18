package com.gentics.contentnode.rest.model.devtools.dependency;

import jakarta.xml.bind.annotation.XmlType;

/**
 * The dependency type enum
 */
@XmlType(name = "Type")
public enum Type {
	CONSTRUCT,
	TEMPLATE,
	DATASOURCE,
	OBJECT_PROPERTY,
	CONTENT_REPOSITORY,
	TEMPLATE_TAG,
	OBJECT_TAG_DEFINITION,
	UNKNOWN;

	/**
	 * Utility method to obtain the type enum from a given string (case-insensitive)
	 *
	 * @param value the value that should be converted to the type
	 * @return the type enum
	 */
	public static Type fromString(String value) {
		String toUpper = value.toUpperCase();
		try {
			return valueOf(toUpper);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}