package com.gentics.contentnode.staging;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Kind of an error
 */
public enum StagingErrorKind implements Serializable {
	EXCEPTION,
	MISMATCH,
	DUPLICATION,
	NO_BINARY,
	INTEGRITY,
	CONFIGURATION,
	OTHER;

	public static StagingErrorKind fromValue(String value) {
		if (value == null) {
			return null;
		}
		return Arrays.stream(StagingErrorKind.values())
				.filter(v -> v.name().equalsIgnoreCase(value))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Unknown staging error kind: " + value));
	}
}
