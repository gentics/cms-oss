package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Type for rendering links
 */
@XmlEnum(String.class)
public enum LinksType {
	backend, frontend
}
