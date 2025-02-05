package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.response.PolicyGroupResponse;
import com.gentics.contentnode.rest.model.response.PolicyResponse;

/**
 * API for reading from the policy map.
 */
@Path("/policyMap")
@Consumes("*/*")
public interface PolicyMapResource extends AuthenticatedResource {
	@GET
	@Path("/partType/{typeId}/policyGroup")
	public PolicyGroupResponse getPolicyGroup(@PathParam("typeId") int typeId) throws Exception;

	@GET
	@Path("/policy")
	public PolicyResponse getPolicy(@QueryParam("uri") String uri) throws Exception;
}
