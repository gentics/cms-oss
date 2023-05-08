/*
 * @author laurin
 * @date 25.05.2005
 * @version $Id: LanguageProvider.java,v 1.3 2006-08-02 11:33:37 norbert Exp $
 */
package com.gentics.lib.i18n;

import com.gentics.api.lib.i18n.Language;

/**
 * Provider for language objects, to resolve languages in runtime. must get
 * language through languagecontainer.
 * @author laurin
 */
public interface LanguageProvider {

	/**
	 * Get the active language of the current provider.
	 * @return the language
	 */
	Language getLanguage();

}
