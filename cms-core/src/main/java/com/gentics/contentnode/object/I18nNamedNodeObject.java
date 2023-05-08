package com.gentics.contentnode.object;

import com.gentics.api.lib.i18n.I18nString;

/**
 * Interface for objects that have translatable names
 */
public interface I18nNamedNodeObject extends NodeObject {
	/**
	 * Get the translatable name
	 * @return name as I18nString
	 */
	I18nString getName();
}
