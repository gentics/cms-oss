package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for handling Portal Implementations
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/portalimplementations")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface PortalImplementationResource {
	/**
	 * List available Portal Implementations.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>type</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>globalId</code></li>
	 * <li><code>keyword</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * <li><code>type</code></li>
	 * <li><code>cdate</code></li>
	 * <li><code>edate</code></li>
	 * </ul>
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return List of Portal Implementations
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Portal Implementation list is returned.")
	})
	PortalImplementationListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create a new Portal Implementation.<br>
	 * The attributes <code>keyword</code>, <code>name</code> and <code>type</code> are mandatory. <code>keyword</code> and <code>name</code> must be unique.
	 * The <code>globalId</code> is optional and will be generated, if not provided.
	 * @param item Portal Implementation to create
	 * @return created Portal Implementation
	 * @throws Exception
	 */
	@POST
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Portal Implementation was created."),
		@ResponseCode(code = 400, condition = "A mandatory attribute is missing."),
		@ResponseCode(code = 409, condition = "The keyword, name or globalId is already used by another Portal Implementation.")
	})
	PortalImplementationResponse add(PortalImplementationModel item) throws Exception;

	/**
	 * Get the Portal Implementation with given id
	 * @param id internal or external Portal Implementation ID
	 * @return Response containing the Portal Implementation
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Portal Implementation {id} exists."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist.")
	})
	PortalImplementationResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update Portal Implementation with given id. Only attributes, which are set in the request, will be changed.
	 * @param id internal or external Portal Implementation ID
	 * @param item updated Portal Implementation
	 * @return updated Portal Implementation
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Portal Implementation {id} was updated."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist."),
		@ResponseCode(code = 409, condition = "The keyword or name is already used by another Portal Implementation.")
	})
	PortalImplementationResponse update(@PathParam("id") String id, PortalImplementationModel item) throws Exception;

	/**
	 * Delete the Portal Implementation with given id
	 * @param id internal or external Portal Implementation ID
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Portal Implementation {id} was deleted."),
		@ResponseCode(code = 404, condition = "Portal Implementation {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;
}
