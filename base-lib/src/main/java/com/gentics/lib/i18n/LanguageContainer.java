/*
 * @author laurin
 * @date 27.04.2005
 * @version $Id: LanguageContainer.java,v 1.3 2006-08-02 11:33:37 norbert Exp $
 */
package com.gentics.lib.i18n;

import com.gentics.api.lib.i18n.Language;

/**
 * language container used to reference to a changeable language object.
 * @author laurin
 */
public class LanguageContainer {

	private Language language;

	public LanguageContainer(Language l) {
		setLanguage(l);
	}

	public Language getLanguage() {
		return language;
	}

	/**
	 * change language to the given language
	 * @param language
	 */
	public void setLanguage(Language language) {
		this.language = language;
	}
}
