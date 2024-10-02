package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;

/**
 * Node object contract, that allows redefining its creation and/or edit timestamp metadata.
 */
public interface CustomMetaDateNodeObject extends MetaDateNodeObject {

	/**
	 * Get the custom creation date
	 * @return custom creation date
	 */
	@FieldGetter("custom_cdate")
	ContentNodeDate getCustomCDate();

	/**
	 * Get the custom creation date (if set != 0) or the default (real) creation date
	 * @return either custom or default creation date
	 */
	default ContentNodeDate getCustomOrDefaultCDate() {
		if (getCustomCDate().getIntTimestamp() != 0) {
			return getCustomCDate();
		} else {
			return getCDate();
		}
	}

	/**
	 * Get the custom edit date
	 * @return custom edit date
	 */
	@FieldGetter("custom_edate")
	ContentNodeDate getCustomEDate();

	/**
	 * Get the custom edit date (if set != 0) or the default (real) edit date
	 * @return either custom or default edit date
	 */
	default ContentNodeDate getCustomOrDefaultEDate() {
		if (getCustomEDate().getIntTimestamp() != 0) {
			return getCustomEDate();
		} else {
			return getEDate();
		}
	}

	/**
	 * Sets the custom creation date as a unix timestamp.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("custom_cdate")
	void setCustomCDate(int timestamp) throws ReadOnlyException;

	/**
	 * Sets the custom edit date as a unix timestamp.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("custom_edate")
	void setCustomEDate(int timestamp) throws ReadOnlyException;
}
