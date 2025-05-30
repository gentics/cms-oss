package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which the constructs can be sorted
 */
@XmlEnum(String.class)
public enum TemplateSortAttribute {
	/**
	 * Sort by name
	 */
	name
}
