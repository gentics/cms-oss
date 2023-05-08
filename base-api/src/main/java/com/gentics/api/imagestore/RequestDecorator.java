package com.gentics.api.imagestore;

import javax.servlet.http.HttpServletRequest;

/**
 * Used by the {com.gentics.portalnode.GenticsImageStoreServlet} to modify headers and data in requests to the GenticsImageStore
 * 
 * The GenticsImageStore servlet can be configured to use an implementation of this interface
 * by setting the requestDecorator servlet parameter to the full class name the implementation.
 */
public interface RequestDecorator {
	
	/**
	 * Provides the ability to alter parameters of requests sent to the GenticsImageStore
	 * 
	 * @param gisRequest
	 * 		  The GenticsImageStoreRequest Bean containing request information
	 * @param request
	 * 		  The HttpServletRequest object containing request information
	 */
	void decorateRequest(GenticsImageStoreRequest gisRequest, HttpServletRequest request);
	
}
