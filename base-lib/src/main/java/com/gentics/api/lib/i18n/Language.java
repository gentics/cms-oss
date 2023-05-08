/*
 * @author laurin
 * @date 27.04.2005
 * @version $Id: Language.java,v 1.2 2010-09-28 17:01:34 norbert Exp $
 */
package com.gentics.api.lib.i18n;

import java.io.Serializable;
import java.util.Locale;
import java.util.Properties;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * language object
 * @author laurin
 */
public class Language implements Serializable, Resolvable {

	String id;

	Properties dic;

	Locale locale;

	/**
	 * load a language object according to given id, locale and dic.
	 * @param id the id string, customizable.
	 * @param locale locale
	 * @param dic dictionary as properties
	 */
	public Language(String id, Locale locale, Properties dic) {
		setId(id);
		setDic(dic);
		setLocale(locale);
	}

	/**
	 * get the id of the language object.
	 * @return the id string, is customizable.
	 */
	public String getId() {
		return id;
	}

	/**
	 * set the id for the current language object. same as constructor.
	 * @param id the id string, is customizable.
	 */
	public void setId(String id) {
		this.id = id;
	}

	public void setDic(Properties dic) {
		this.dic = dic;
	}

	public Properties getDic() {
		return dic;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public boolean canResolve() {
		return true;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	public Object getProperty(String key) {
		if ("id".equals(key)) {
			return getId();
		} else if ("name".equals(key)) {
			return locale.getDisplayName(locale);
		} else if ("language".equals(key)) {
			return locale.getDisplayLanguage(locale);
		} else if ("country".equals(key)) {
			return locale.getDisplayCountry(locale);
		} else if ("variant".equals(key)) {
			return locale.getDisplayVariant(locale);
		} else if ("locale".equals(key)) {
			return locale;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return locale.getDisplayName(locale);
	}
}
