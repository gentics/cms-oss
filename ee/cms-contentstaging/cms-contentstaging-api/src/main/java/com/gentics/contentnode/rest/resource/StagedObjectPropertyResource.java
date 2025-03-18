package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for management of object property definitions
 */
@Path("/objectproperty")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface StagedObjectPropertyResource {
	/**
	 * Get the implementation hash for an object property
	 * @param id object property ID
	 * @return response with implementation hash
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/hash")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Object Property {id} exists."),
		@ResponseCode(code = 404, condition = "Object Property {id} does not exist.")
	})
	ImplementationHashResponse implementationHash(@PathParam("id") String id) throws Exception;
}
