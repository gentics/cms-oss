package com.gentics.contentnode.rest.resource;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for proxying requests to the Mesh instance of a Mesh Content.Repository.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/contentrepositories")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions."),
	@ResponseCode(code = 404, condition = "Content.Repository with given ID does not exist."),
	@ResponseCode(code = 409, condition = "Content.Repository with given ID is not a Mesh Content.Repository or has invalid URL."),
	@ResponseCode(code = 502, condition = "Accessing the Mesh instance is not possible.")
})
public interface ContentRepositoryProxyResource {
	/**
	 * DELETE request without extra path
	 * @param id CR id
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/proxy")
	Response deleteNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * GET request without extra path
	 * @param id CR id
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/proxy")
	Response getNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * HEAD request without extra path
	 * @param id CR id
	 * @return response
	 * @throws Exception
	 */
	@HEAD
	@Path("/{id}/proxy")
	Response headNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * OPTIONS request without extra path
	 * @param id CR id
	 * @return response
	 * @throws Exception
	 */
	@OPTIONS
	@Path("/{id}/proxy")
	Response optionsNoPath(@PathParam("id") String id) throws Exception;

	/**
	 * POST request without extra path
	 * @param id CR id
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/proxy")
	Response postNoPath(@PathParam("id") String id, InputStream requestBody) throws Exception;

	/**
	 * PUT request without extra path
	 * @param id CR id
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/proxy")
	Response putNoPath(@PathParam("id") String id, InputStream requestBody) throws Exception;

	/**
	 * DELETE request
	 * @param id CR id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/proxy/{path: .*}")
	Response delete(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * GET request
	 * @param id CR id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/proxy/{path: .*}")
	Response get(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * HEAD request
	 * @param id CR id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@HEAD
	@Path("/{id}/proxy/{path: .*}")
	Response head(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * OPTIONS request
	 * @param id CR id
	 * @param path request path
	 * @return response
	 * @throws Exception
	 */
	@OPTIONS
	@Path("/{id}/proxy/{path: .*}")
	Response options(@PathParam("id") String id, @PathParam("path") String path) throws Exception;

	/**
	 * POST request
	 * @param id CR id
	 * @param path request path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/proxy/{path: .*}")
	Response post(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws Exception;

	/**
	 * PUT request
	 * @param id CR id
	 * @param path request path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/proxy/{path: .*}")
	Response put(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws Exception;

	/**
	 * Perform a login with the credentials stored in the Content.Repository.
	 * @param id CR id
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/proxylogin")
	Response login(@PathParam("id") String id) throws Exception;
}
