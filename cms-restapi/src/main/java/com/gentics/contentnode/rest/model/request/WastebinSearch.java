package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Type for getting objects from wastebin
 */
@XmlEnum(String.class)
public enum WastebinSearch {
	exclude,
	include,
	only
}
