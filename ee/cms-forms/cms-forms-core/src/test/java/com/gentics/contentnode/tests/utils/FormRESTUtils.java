package com.gentics.contentnode.tests.utils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;

/**
 * Static helper class for use in Form tests
 */
public class FormRESTUtils {

	/**
	 * Get a form resource
	 * @return form resource
	 * @throws NodeException
	 */
	public static FormResourceImpl getFormResource() throws NodeException {
		return new FormResourceImpl();
	}
}
