/*
 * @author norbert
 * @date 20.01.2006
 * @version $Id: I18nStringImpl.java,v 1.2 2008-05-26 15:05:57 norbert Exp $
 */
package com.gentics.lib.i18n;

import com.gentics.api.lib.i18n.I18nString;

/**
 * Specific implementation of a I18nString. The Portal will instantiate this class when a new I18nString is created.
 * @author norbert
 */
public class I18nStringImpl extends I18nString {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 2010005496955190321L;

	/**
	 * Create an instance of a I18nString
	 * @param key the key of the i18n item. the syntax is [a-z0-9.]+, and will
	 *        generate a warning if violated
	 * @param languageProvider the languageprovider to use, for fetching the
	 *        language during toString.
	 */
	public I18nStringImpl(String key, LanguageProvider languageProvider) {
		super(key, languageProvider);
	}
}
