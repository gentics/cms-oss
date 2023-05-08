package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which the constructs can be sorted
 */
@XmlEnum(String.class)
public enum ConstructSortAttribute {

	/**
	 * Sort by name
	 */
	name, /**
	 * Sort by keyword
	 */ keyword, /**
	 * Sort by description
	 */ description, /**
	 * Sort by category
	 */ category
}
