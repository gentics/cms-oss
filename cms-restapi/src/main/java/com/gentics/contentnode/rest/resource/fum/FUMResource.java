package com.gentics.contentnode.rest.resource.fum;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.fum.FUMResult;
import com.gentics.contentnode.rest.model.fum.FUMStatusResponse;

/**
 * Resource for handling of postponed FUM requests
 */
@Path("/fum")
public interface FUMResource {
	/**
	 * Get the contents of the temporary file
	 * @param filename filename
	 * @return contents
	 */
	@GET
	@Path("/{filename}")
	Response fetch(@PathParam("filename") String filename);

	/**
	 * Post the result of the postponed FUM
	 * @param filename filename
	 * @param result FUM result
	 * @return response
	 */
	@POST
	@Path("/{filename}")
	FUMStatusResponse done(@PathParam("filename") String filename, FUMResult result);
}
