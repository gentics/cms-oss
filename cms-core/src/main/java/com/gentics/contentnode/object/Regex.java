package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.TType;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Regular expression definition for validating textual input for tag parts
 */
@TType(Regex.TYPE_REGEX)
public interface Regex extends NodeObject, I18nNamedNodeObject {
	public final static int TYPE_REGEX = 2;

	/**
	 * Name of the Regex
	 * @return the name of the regex
	 */
	default I18nString getName() {
		return new CNI18nString(Integer.toString(getNameId()));
	}

	/**
	 * Name ID of the Regex
	 * @return name ID
	 */
	int getNameId();

	/**
	 * Description of the Regex
	 * @return description
	 */
	default I18nString getDescription() {
		return new CNI18nString(Integer.toString(getDescriptionId()));
	}

	/**
	 * Description ID of the Regex
	 * @return description ID
	 */
	int getDescriptionId();

	/**
	 * Get the regex itself
	 * @return regex
	 */
	String getExpression();

	/**
	 * Regex creator
	 * @return creator
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * Regex editor
	 * @return editor
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * get the creation date as a unix timestamp 
	 * @return creation date unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * get the edit date as a unix timestamp 
	 * @return edit date unix timestamp
	 */
	ContentNodeDate getEDate();
}
