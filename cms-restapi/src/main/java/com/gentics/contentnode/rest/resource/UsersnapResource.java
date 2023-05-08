package com.gentics.contentnode.rest.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.response.UsersnapResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for Usersnap integration
 */
@Path("/usersnap")
@Produces(MediaType.APPLICATION_JSON)
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions."),
	@ResponseCode(code = 405, condition = "Feature usersnap is not activated.")
})
public interface UsersnapResource {
	/**
	 * Get the Usersnap settings
	 * @return response containing the Usersnap settings
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Usersnap settings are returned.")
	})
	UsersnapResponse getSettings() throws Exception;
}
