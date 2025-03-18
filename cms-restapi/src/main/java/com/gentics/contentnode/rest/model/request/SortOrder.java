package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Sort order
 */
@XmlEnum(String.class)
public enum SortOrder {
	/**
	 * Sort ascending
	 */
	asc,
	ASC,

	/**
	 * Sort descending
	 */
	desc,
	DESC,

	/**
	 * No sorting
	 */
	none,
	NONE
}
