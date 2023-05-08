package com.gentics.lib.i18n;

/**
 * Wrapper for the language provider. The wrapper is being used by the {@link LanguageProviderFactory} to return the current language provider for the product specific
 * implementation.
 * 
 * @author johannes2
 * 
 */
public interface LanguageProviderWrapper {

	/**
	 * Return the current language provider
	 * @return
	 */
	public LanguageProvider getCurrentProvider();

	/**
	 * Return the current language code 
	 * @return
	 */
	String getCurrentLanguageCode();

}
