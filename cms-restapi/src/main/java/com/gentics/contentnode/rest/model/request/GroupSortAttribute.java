package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which the groups can be sorted
 */
@XmlEnum(String.class)
public enum GroupSortAttribute {

	/**
	 * Sort groups by ID
	 */
	id, /**
	 * Sort groups by name
	 */ name
}
