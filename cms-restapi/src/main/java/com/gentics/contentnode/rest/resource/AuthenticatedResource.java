package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.QueryParam;

public interface AuthenticatedResource {

	/**
	 * Set the sessionId for this request.
	 * 
	 * @param sessionId Id of the session to use for this ContentNodeResource
	 */
	@QueryParam("sid")
	void setSessionId(String sessionId);
}
