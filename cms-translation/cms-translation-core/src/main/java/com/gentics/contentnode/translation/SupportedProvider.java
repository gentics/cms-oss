package com.gentics.contentnode.translation;


import com.gentics.api.lib.exception.NodeException;
import java.util.Arrays;

/**
 * List of all supported translation providers.
 */
public enum SupportedProvider {
	DEEPL;

	public static SupportedProvider fromString(String value) throws NodeException {
		try {
			var val = value.toUpperCase();
			return valueOf(val);
		}
		catch (IllegalArgumentException  | NullPointerException e ) {
			var errorMessage = String.format("Invalid provider value: '%s'. Supported translation providers are %s." , value,
					Arrays.toString(SupportedProvider.values()));
			throw new NodeException(errorMessage);
		}
	}

}
