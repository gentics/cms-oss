package com.gentics.api.imagestore;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

/**
 * Used by the {com.gentics.portalnode.GenticsImageStoreServlet} to map image
 * requests to the locations where the images should be fetched from.
 * 
 * The GenticsImageStore servlet can be configured to use an implementation of this interface
 * by setting the uriMapper servlet parameter to the full class name the implementation.
 */
public interface ImageUriMapper {

	/**
	 * Maps a request for an image to an URI where the image can be fetched from.
	 * 
	 * This method should have no side effects and should always return the same URI
	 * for the given imageUri and the parameters in the given servletRequest.
	 * 
	 * Further, this method should be idempotent such that passing back the returned URI
	 * to this method again returns the given URI unchanged.
	 * 
	 * @param servletRequest
	 * 		  The request that fetches the image.
	 * @param imageUri
	 * 		  The URI of the image to fetch.
	 * @return
	 * 		  May either return the given URI unchanged, or an alternate URI
	 * 		  where the image will be fetched from instead.
	 */
	URI mapImageUri(HttpServletRequest servletRequest, URI imageUri);
}
