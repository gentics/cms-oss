package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which datasources can be sorted
 */
@XmlEnum(String.class)
public enum DatasourceSortAttribute {
	/**
	 * Sort by name
	 */
	name
}
