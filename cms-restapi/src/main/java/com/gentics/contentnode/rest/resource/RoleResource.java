package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.RoleModel;
import com.gentics.contentnode.rest.model.RolePermissionsModel;
import com.gentics.contentnode.rest.model.response.role.RoleListResponse;
import com.gentics.contentnode.rest.model.response.role.RolePermResponse;
import com.gentics.contentnode.rest.model.response.role.RoleResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for management of roles
 */
@Path("/role")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface RoleResource {
	/**
	 * Get list of roles.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>description</code></li>
	 * </ul>
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return list response
	 * @throws Exception
	 */
	@GET
	@StatusCodes({ @ResponseCode(code = 200, condition = "List of roles is returned.") })
	RoleListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create new role
	 * @param role role
	 * @return response containing created role
	 * @throws Exception
	 */
	@PUT
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Role {id} was created.")
	})
	RoleResponse create(RoleModel role) throws Exception;

	/**
	 * Get existing role
	 * @param id role ID
	 * @return response containing the role
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Role {id} exists."),
		@ResponseCode(code = 404, condition = "Role {id} does not exist.")
	})
	RoleResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update a role
	 * @param id role ID
	 * @param role updated role data
	 * @return response containing the updated role
	 * @throws Exception
	 */
	@POST
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Role {id} was updated."),
		@ResponseCode(code = 404, condition = "Role {id} does not exist.")
	})
	RoleResponse update(@PathParam("id") String id, RoleModel role) throws Exception;

	/**
	 * Delete a role
	 * @param id role ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Role {id} was deleted."),
		@ResponseCode(code = 404, condition = "Role {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Get role permissions
	 * @param id role ID
	 * @return role permissions
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/perm")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Role {id} exists."),
		@ResponseCode(code = 404, condition = "Role {id} does not exist.")
	})
	RolePermResponse getPerm(@PathParam("id") String id) throws Exception;

	/**
	 * Update role permissions
	 * @param id role ID
	 * @param updatedPerms permissions
	 * @return updated permissions
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/perm")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Role {id} permissions were updated."),
		@ResponseCode(code = 404, condition = "Role {id} does not exist.")
	})
	RolePermResponse updatePerm(@PathParam("id") String id, RolePermissionsModel updatedPerms) throws Exception;
}
