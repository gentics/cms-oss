package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which the users can be sorted
 */
@XmlEnum(String.class)
public enum UserSortAttribute {
	/**
	 * Sort by ID
	 */
	id,

	/**
	 * Sort by login
	 */
	login, /**
	 * Sort by firstname
	 */ firstname, /**
	 * Sort by lastname
	 */ lastname, /**
	 * Sort by email
	 */ email
}
