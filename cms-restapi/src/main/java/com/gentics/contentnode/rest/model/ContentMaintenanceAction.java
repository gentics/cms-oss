package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Maintenance actions
 */
@XmlEnum(String.class)
public enum ContentMaintenanceAction {
	/**
	 * Mark objects for (re-)publishing.
	 */
	dirt,

	/**
	 * Delay dirted objects.
	 */
	delay,

	/**
	 * Republish delayed objects.
	 */
	publishDelayed,

	/**
	 * Mark dirted objects as "published"
	 */
	markPublished;
}
