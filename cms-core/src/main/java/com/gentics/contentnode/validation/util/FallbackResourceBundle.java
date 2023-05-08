/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: FallbackResourceBundle.java,v 1.1.2.1 2011-02-10 13:43:39 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * {@link ResourceBundle#getBundle(String,Locale)} only degrades to
 * less specific resources, that don't have the variant, country or language
 * parts in the file/class name.
 *
 * We have to provide a catch-all resource for each language, so that
 * a {@link Locale} can be used with
 * {@link ResourceBundle#getBundle(String,Locale)} that only has a
 * language part set.
 * 
 * Otherwise, if a {@link Locale}, that only has the language part set,
 * is used to load a resource, more specific resources that have an
 * additional country or variant part will be completely ignored.
 */
abstract public class FallbackResourceBundle extends ResourceBundle {
    
	private final ResourceBundle fallback;
    
	public FallbackResourceBundle(String baseName, Locale locale) {
		fallback = ResourceBundle.getBundle(baseName, locale);
	}

	@Override
	public Locale getLocale() {
		return fallback.getLocale();
	}

	@Override
	public Enumeration<String> getKeys() {
		return fallback.getKeys();
	}

	@Override
	protected Object handleGetObject(String key) {
		return fallback.getObject(key);
	}
}
