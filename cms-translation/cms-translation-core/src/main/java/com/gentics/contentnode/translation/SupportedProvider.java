package com.gentics.contentnode.translation;


import com.gentics.api.lib.exception.NodeException;

/**
 * List of all supported translation providers.
 */
public enum SupportedProvider {
	DEEPL;

	public static SupportedProvider fromString(String value) throws NodeException {
		var val = value.toUpperCase();

		try {
			return valueOf(val);
		}
		catch (IllegalArgumentException e) {
			throw new NodeException("Invalid provider value: " + value);
		}
	}

}
