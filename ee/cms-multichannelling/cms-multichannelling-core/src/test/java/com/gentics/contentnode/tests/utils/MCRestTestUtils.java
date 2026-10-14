package com.gentics.contentnode.tests.utils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Utilities for tests, which call multichannelling REST resources directly
 */
public class MCRestTestUtils {
	/**
	 * Execute the given action and return its response. If the action throws a {@link RestMappedException},
	 * the response of the exception (which would be sent to the client) is returned.
	 * @param action action
	 * @return response
	 * @throws NodeException
	 */
	public static <T extends GenericResponse> GenericResponse responseOf(Supplier<T> action) throws NodeException {
		try {
			return action.supply();
		} catch (RestMappedException e) {
			return e.getRestResponse();
		}
	}
}
