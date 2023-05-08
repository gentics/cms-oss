/*
 * @author norbert
 * @date 09.02.2007
 * @version $Id: I18nConstantLanguageStringImpl.java,v 1.2 2007-04-13 09:42:29 norbert Exp $
 */
package com.gentics.lib.i18n;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.api.lib.i18n.Language;

/**
 * @author norbert
 *
 */
@SuppressWarnings("serial")
public class I18nConstantLanguageStringImpl extends I18nString implements LanguageProvider {

	/**
	 * constant language
	 */
	protected Language language;

	/**
	 * @param key key to translate
	 * @param language translation language
	 */
	public I18nConstantLanguageStringImpl(String key, Language language) {
		super(key, null);
		languageProvider = this;
		this.language = language;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.i18n.LanguageProvider#getLanguage()
	 */
	public Language getLanguage() {
		return language;
	}
}
