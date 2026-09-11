package com.gentics.contentnode.rest.resource;

import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for proxying requests to the preview target of a Node.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/node")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions."),
	@ResponseCode(code = 404, condition = "Node with given ID does not exist."),
	@ResponseCode(code = 409, condition = "Node with given ID has no valid preview URL configured."),
	@ResponseCode(code = 502, condition = "Accessing the preview target is not possible.")
})
public interface NodePreviewProxyResource {
	/**
	 * DELETE request without extra path
	 * @param id node id
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/preview/proxy")
	Response deleteNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * GET request without extra path
	 * @param id node id
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/preview/proxy")
	Response getNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * HEAD request without extra path
	 * @param id node id
	 * @return response
	 * @throws Exception
	 */
	@HEAD
	@Path("/{id}/preview/proxy")
	Response headNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * OPTIONS request without extra path
	 * @param id node id
	 * @return response
	 * @throws Exception
	 */
	@OPTIONS
	@Path("/{id}/preview/proxy")
	Response optionsNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * POST request without extra path
	 * @param id node id
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/preview/proxy")
	Response postNoPath(@PathParam("id") String id, InputStream requestBody) throws Exception;

	/**
	 * PUT request without extra path
	 * @param id node id
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/preview/proxy")
	Response putNoPath(@PathParam("id") String id, InputStream requestBody) throws Exception;

	/**
	 * DELETE request
	 * @param id node id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/preview/proxy/{path: .*}")
	Response delete(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * GET request
	 * @param id node id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/preview/proxy/{path: .*}")
	Response get(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * HEAD request
	 * @param id node id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@HEAD
	@Path("/{id}/preview/proxy/{path: .*}")
	Response head(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * OPTIONS request
	 * @param id node id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@OPTIONS
	@Path("/{id}/preview/proxy/{path: .*}")
	Response options(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * POST request
	 * @param id node id
	 * @param path request path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/preview/proxy/{path: .*}")
	Response post(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws Exception;

	/**
	 * PUT request
	 * @param id node id
	 * @param path request path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/preview/proxy/{path: .*}")
	Response put(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws Exception;
}
