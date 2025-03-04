package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.UserSaveRequest;
import com.gentics.contentnode.rest.model.request.UserSortAttribute;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.model.response.NodeRestrictionResponse;
import com.gentics.contentnode.rest.model.response.UserDataResponse;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource to get Users. The list of users returned will always be filtered by
 * user permission: A user may only see users that are members of the same
 * groups or subgroups of the groups, the user is member of
 */
@Path("/user")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface UserResource {
	/**
	 * Get the current user (me)
	 * By default, the user's groups are not returned.
	 *
	 * @param groups
	 *            Whether to list the user's groups
	 * @return current user
	 */
	@GET
	@Path("/me")
	UserLoadResponse getMe(@QueryParam("groups") @DefaultValue("false") boolean groups);

	/**
	 * Get a list of users, optionally filtered, sorted and paged
	 *
	 * @param skipCount
	 *            number of elements to be skipped (paging)
	 * @param maxItems
	 *            maximum number of elements returned (paging)
	 * @param ids
	 *            id(s) for filtering
	 * @param logins
	 *            login string(s) for filtering
	 * @param firstNames
	 *            firstname string(s) for filtering
	 * @param lastNames
	 *            lastname string(s) for filtering
	 * @param eMails
	 *            email string(s) for filtering
	 * @param groupIds
	 *            group id(s) for filtering
	 * @param search
	 *            additionally search in users logins, firstNames, lastNames and
	 *            emails
	 * @param sortBy
	 *            name of an attribute to sort
	 * @param sortOrder
	 *            sort order
	 * @param addGroups
	 *            true to add groups to the users, false (default) otherwise
	 * @return list of users
	 * @deprecated because new endpoint exists. use {@link #list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)} instead
	 */
	@GET
	@Path("/list")
	@Deprecated
	UserListResponse list(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("id") List<Integer> ids,
			@QueryParam("login") List<String> logins,
			@QueryParam("firstname") List<String> firstNames,
			@QueryParam("lastname") List<String> lastNames,
			@QueryParam("email") List<String> eMails,
			@QueryParam("group") List<Integer> groupIds,
			@QueryParam("search") String search,
			@QueryParam("sortby") final UserSortAttribute sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") final SortOrder sortOrder,
			@QueryParam("groups") @DefaultValue("false") boolean addGroups);

	/**
	 * Saves the user into GCN.
	 *
	 * @deprecated Use {@link #update(String, User) PUT /rest/user/{id}} instead
	 * @param id
	 *            Id of the user to save.
	 * @param request
	 *            user save request
	 * @return GenericResponse
	 */
	@POST
	@Path("/save/{id}")
	@Deprecated
	GenericResponse save(@PathParam("id") Integer id, UserSaveRequest request);

	/**
	 * Save user data for the given key
	 * @param key key of the user data to save
	 * @param jsonData user data in JSON format
	 * @return Generic response
	 */
	@POST
	@Path("/me/data/{key}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	GenericResponse saveUserData(@PathParam("key") String key, JsonNode jsonData);

	/**
	 * Get user data for the given key
	 * @param key key of the user data
	 * @return Response containing the value
	 */
	@GET
	@Path("/me/data/{key}")
	@Produces({MediaType.APPLICATION_JSON})
	UserDataResponse getUserData(@PathParam("key") String key);

	/**
	 * Delete the user data for the given key
	 * @param key key of the user data
	 * @return Generic response
	 */
	@DELETE
	@Path("/me/data/{key}")
	@Produces({MediaType.APPLICATION_JSON})
	GenericResponse deleteUserData(@PathParam("key") String key);

	/**
	 * Get complete user data
	 * @return Response containing the complete user data
	 */
	@GET
	@Path("/me/data")
	@Produces({MediaType.APPLICATION_JSON})
	UserDataResponse getAllUserData();

	/**
	 * List users.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>login</code></li>
	 * <li><code>firstName</code></li>
	 * <li><code>lastName</code></li>
	 * <li><code>email</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>login</code></li>
	 * <li><code>firstName</code></li>
	 * <li><code>lastName</code></li>
	 * <li><code>email</code></li>
	 * </ul>
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms perms parameters
	 * @param embed optionally embed the referenced object (group)
	 * @return user list
	 * @throws Exception in case of errors
	 */
	@GET
	UserList list(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed
	) throws Exception;

	/**
	 * Load user with given ID
	 * @param id local user ID
	 * @param embed optionally embed the referenced object (group)
	 * @return user response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "User {id} exists."),
		@ResponseCode(code = 404, condition = "User {id} does not exist.")
	})
	UserLoadResponse get(
			@PathParam("id") String id,
			@BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Update user with given ID
	 * @param id local user ID
	 * @param item new user data
	 * @return updated user
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "User {id} was updated."),
		@ResponseCode(code = 404, condition = "User {id} does not exist.")
	})
	UserLoadResponse update(@PathParam("id") String id, User item) throws Exception;

	/**
	 * Deactivate the user with given ID
	 * @param id local user ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "User {id} was deactivated."),
		@ResponseCode(code = 404, condition = "User {id} does not exist.")
	})
	Response deactivate(@PathParam("id") String id) throws Exception;

	/**
	 * List groups of given user.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * </ul>
	 * @param id local user ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return list of groups
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/groups")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "User {id} exists."),
		@ResponseCode(code = 404, condition = "User {id} does not exist.")
	})
	GroupList groups(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Add user to the group
	 * @param id local user ID
	 * @param groupId local group ID
	 * @return group
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/groups/{groupId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "User was added."),
		@ResponseCode(code = 404, condition = "User {id} or group {groupId} does not exist.")
	})
	GroupLoadResponse addToGroup(@PathParam("id") String id, @PathParam("groupId") String groupId) throws Exception;

	/**
	 * Remove user from the group
	 * @param id local user ID
	 * @param groupId local group ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/groups/{groupId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "User was removed."),
		@ResponseCode(code = 404, condition = "User {id} or group {groupId} does not exist.")
	})
	Response removeFromGroup(@PathParam("id") String id, @PathParam("groupId") String groupId) throws Exception;

	/**
	 * Get node restrictions for the assignment of the user to the group
	 * @param id user ID
	 * @param groupId group ID
	 * @return response containing the node restrictions
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/groups/{groupId}/nodes")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Node restriction is returned."),
		@ResponseCode(code = 404, condition = "User {id} or group {groupId} does not exist, or the user is not assigned to the group.")
	})
	NodeRestrictionResponse getGroupNodeRestrictions(@PathParam("id") String id, @PathParam("groupId") String groupId) throws Exception;

	/**
	 * Add node restriction to the assignment of the user to the group
	 * @param id user ID
	 * @param groupId group ID
	 * @param nodeId node ID
	 * @return response containing the (possibly updated) node restrictions
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/groups/{groupId}/nodes/{nodeId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Node restriction was added."),
		@ResponseCode(code = 404, condition = "User {id} or group {groupId} or node {nodeId} does not exist, or the user is not assigned to the group.")
	})
	NodeRestrictionResponse addGroupNodeRestriction(@PathParam("id") String id, @PathParam("groupId") String groupId, @PathParam("nodeId") String nodeId)
			throws Exception;

	/**
	 * Remove node restriction from the assignment of the user to the group
	 * @param id user ID
	 * @param groupId group ID
	 * @param nodeId node ID
	 * @return response containing the (possible updated) node restrictions
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/groups/{groupId}/nodes/{nodeId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Node restriction was removed."),
		@ResponseCode(code = 404, condition = "User {id} or group {groupId} or node {nodeId} does not exist, or the user is not assigned to the group.")
	})
	NodeRestrictionResponse removeGroupNodeRestriction(@PathParam("id") String id, @PathParam("groupId") String groupId, @PathParam("nodeId") String nodeId)
			throws Exception;
}
