package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import org.apache.commons.lang3.StringUtils;

public class TranslationConfigUtil {

	/**
	 * Reads the authentication token from the configuration.
	 *
	 * @return The token used to authenticate the deepL client.
	 * @throws NodeException if the token cannot be read or is empty.
	 */
	public static String getAuthTokenFromConfig(String className) throws NodeException {
		var authToken = ConfigurationValue.TRANSLATION_AUTH_TOKEN.get();

		if (StringUtils.isBlank(authToken)) {
			throw new NodeException(
					"Invalid authentication token provided for: " +  className);
		}

		return authToken;
	}
}
