package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling constructs
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/construct")
public interface StagedConstructResource {
	/**
	 * Get the implementation hash for a construct
	 * @param id construct ID
	 * @return response with implementation hash
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/hash")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Construct {id} exists."),
		@ResponseCode(code = 404, condition = "Construct {id} does not exist.")
	})
	ImplementationHashResponse implementationHash(@PathParam("id") String id) throws Exception;
}
