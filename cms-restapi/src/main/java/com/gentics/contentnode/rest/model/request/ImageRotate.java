package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Possible directions for rotating images
 */
@XmlEnum(String.class)
public enum ImageRotate {
	/**
	 * Clockwise
	 */
	cw,

	/**
	 * Counter-Clockwise
	 */
	ccw
}
