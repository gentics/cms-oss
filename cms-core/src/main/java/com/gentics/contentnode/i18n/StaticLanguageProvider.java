package com.gentics.contentnode.i18n;

import com.gentics.api.lib.i18n.Language;
import com.gentics.lib.i18n.LanguageProvider;

/**
 * Implementation of {@link LanguageProvider} that always provides a static language
 */
public class StaticLanguageProvider implements LanguageProvider {
	/**
	 * Provided language
	 */
	protected Language language;

	/**
	 * Create an instance
	 * @param language provided language
	 */
	public StaticLanguageProvider(Language language) {
		this.language = language;
	}

	@Override
	public Language getLanguage() {
		return language;
	}
}
