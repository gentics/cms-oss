package com.gentics.contentnode.events;

import com.gentics.contentnode.i18n.I18NHelper;

/**
 * Enum for possible queue entry types
 */
public enum QueueEntryType {
	/**
	 * Unset queue entry
	 */
	unset(""),

	/**
	 * Logging event
	 */
	log("dirtqueue_log"),

	/**
	 * Dirt event
	 */
	dirt("dirtqueue_dirt"),

	/**
	 * Maintenance event
	 */
	maintenance("dirtqueue_maintenance"),

	/**
	 * ContentRepository maintenance event
	 */
	cr("dirtqueue_cr"),

	/**
	 * Publish event
	 */
	publish("dirtqueue_publish");

	/**
	 * I18n key
	 */
	private String i18nKey;

	/**
	 * Create instance with i18n key
	 * @param i18nKey i18n key
	 */
	QueueEntryType(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	/**
	 * Get translated label
	 * @return translated label
	 */
	public String getLabel() {
		return I18NHelper.get(i18nKey);
	}
}