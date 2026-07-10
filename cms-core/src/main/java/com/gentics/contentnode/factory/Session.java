package com.gentics.contentnode.factory;

import java.io.Serializable;

import com.gentics.lib.i18n.LanguageProvider;

public interface Session extends LanguageProvider, Serializable {

	int getUserId();

	/**
	 * @return the language id for this session. This will be the id for
	 * the language which the user selected in the user preferences.
	 */
	int getLanguageId();

	String getSessionId();
}
