package com.gentics.contentnode.rest.resource;

import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource for monitoring endpoints.
 */
@Path("/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface MonitoringResource {

	/**
	 * Check if the CMS is live.
	 *
	 * @return Response with status code 200 if the CMS is live, and 503 if not.
	 */
	@GET
	@Path("/health/live")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "CMS is live"),
		@ResponseCode(code = 503, condition = "CMS is not live")
	})
	Response live() throws Exception;

	/**
	 * Check if the CMS is ready to serve requests.
	 *
	 * @return Response with status code 200 if the CMS is ready, and 503 if not.
	 */
	@GET
	@Path("/health/ready")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "CMS is ready"),
		@ResponseCode(code = 503, condition = "CMS is not ready")
	})
	Response ready() throws Exception;
}
