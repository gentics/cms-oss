package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for management of datasources
 */
@Path("/datasource")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface StagedDatasourceResource {
	/**
	 * Get the implementation hash for a datasource
	 * @param id datasource ID
	 * @return response with implementation hash
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/hash")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Datasource {id} exists."),
		@ResponseCode(code = 404, condition = "Datasource {id} does not exist.")
	})
	ImplementationHashResponse implementationHash(@PathParam("id") String id) throws Exception;
}
