package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Datasource types
 */
@XmlEnum(String.class)
public enum DatasourceType {
	/**
	 * Static datasource
	 */
	STATIC,

	/**
	 * Siteminder datasource
	 */
	SITEMINDER
}
