package com.gentics.lib.i18n;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;

/**
 * Simple language provider factory. You can registered your own language provider here.
 * 
 * @author johannes2
 * 
 */
public class LanguageProviderFactory {

	private static LanguageProviderFactory instance;
	private LanguageProviderWrapper registeredLanguageProvider;
	private static NodeLogger logger = NodeLogger.getNodeLogger(LanguageProviderFactory.class);

	/**
	 * Returns a new instance of the language provider factory
	 * 
	 * @return
	 */
	public static LanguageProviderFactory getInstance() {
		if (instance == null) {
			instance = new LanguageProviderFactory();
		}
		return instance;
	}

	/**
	 * Register the {@link LanguageProviderWrapper}. The wrapper will be used to retrieve the latest language Provider. Please note that only one provider can be
	 * registered. All further registrations will cause an exception.
	 * 
	 * @param wrapper
	 * @throws NodeException
	 */
	public void registerProviderWrapper(LanguageProviderWrapper wrapper) throws NodeException {
		if (registeredLanguageProvider == null && wrapper != null) {
			registeredLanguageProvider = wrapper;
		} else {
			throw new NodeException(
					"The given provider could not be registered because either an existing provider was already set or the given provider was null");
		}
	}

	/**
	 * Return the language provider using the product specific language provider wrapper.
	 * 
	 * @return
	 * @throws NodeException
	 */
	public LanguageProvider getProvider() throws NodeException {
		if (registeredLanguageProvider != null) {
			return registeredLanguageProvider.getCurrentProvider();
		} else {
			throw new NodeException("No product specific language provider has been registered.");
		}

	}

	/**
	 * Retrieves the current language code from the registered provider.
	 * 
	 * @return Null when no current language could be determined.
	 * @throws NodeException
	 */
	public String getCurrentLanguageCode() {
		if (registeredLanguageProvider != null) {
			return registeredLanguageProvider.getCurrentLanguageCode();
		} else {
			logger.error("No product specific language provider has been registered.");
			return null;
		}
	}

	/**
	 * Reset the instance to null.
	 */
	public static void reset() {
		instance = null;
	}

}
