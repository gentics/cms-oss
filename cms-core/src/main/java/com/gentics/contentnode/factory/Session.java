package com.gentics.contentnode.factory;

import java.io.Serializable;

import com.gentics.lib.i18n.LanguageProvider;

/**
 * Interface for Sessions. Implementations may be backed by a DB session or an Api Token
 */
public interface Session extends LanguageProvider, Serializable {
	/**
	 * Get the ID of the session
	 * @return ID of the session
	 */
	int getId();

	/**
	 * User ID of the session
	 * @return user ID
	 */
	int getUserId();

	/**
	 * @return the language id for this session. This will be the id for
	 * the language which the user selected in the user preferences.
	 */
	int getLanguageId();
}
