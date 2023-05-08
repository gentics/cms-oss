package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Attributes, by which object properties can be sorted
 */
@XmlEnum(String.class)
public enum ObjectPropertySortAttribute {
	/**
	 * Sort by name
	 */
	name
}
