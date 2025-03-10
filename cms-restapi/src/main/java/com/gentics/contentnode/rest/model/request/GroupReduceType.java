package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Types of reducing group lists
 */
@XmlEnum(String.class)
public enum GroupReduceType {

	/**
	 * For groups that form a hierarchy, only the child remains
	 */
	child, /**
	 * For groups that form a hierarchy, only the parent remains
	 */ parent
}
