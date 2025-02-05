package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.GroupReduceType;
import com.gentics.contentnode.rest.model.request.GroupSortAttribute;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TypePermissionRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.model.response.GroupsResponse;
import com.gentics.contentnode.rest.model.response.TypePermissionList;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource to get groups
 */
@Path("/group")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface GroupResource {

	/**
	 * Load the groups the user may see
	 *
	 * @param perms permissions parameters
	 * @return response containing groups
	 */
	@GET
	@Path("/load")
	GroupsResponse load(@BeanParam PermsParameterBean perms);

	/**
	 * Get a list of groups, optionally filtered sorted and paged.
	 * @param skipCount number of groups skipped in the list (paging)
	 * @param maxItems maximum number of groups returned (paging)
	 * @param ids id of the group to return (filter)
	 * @param names name or name pattern of the group(s) to return (filter)
	 * @param memberLogins login name of the group member for the group(s) to return (filter)
	 * @param memberIds id of the group member for the group(s) to return (filter)
	 * @param childGroupIds ids of child groups for the group(s) to return (filter)
	 * @param privFolderIds ids of folders for filtering by folder permissions (together with "privileges")
	 * @param privileges list of privileges for filtering by folder permissions (together with "folder")
	 * @param reduce if set, the list of groups will be reduced: for nested groups, only parent group(s) or child group(s) will be returned
	 * @param sortBy name of the sorted attribute (sorting)
	 * @param sortOrder sortorder (sorting)
	 * @param perms permissions parameters
	 * @return response containing the groups
	 * @deprecated because new endpoint exists. use {@link #list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)} instead
	 */
	@GET
	@Path("/list")
	@Deprecated
	GroupsResponse list(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("id") List<Integer> ids,
			@QueryParam("name") List<String> names,
			@QueryParam("memberlogin") List<String> memberLogins,
			@QueryParam("memberid") List<Integer> memberIds,
			@QueryParam("children") List<Integer> childGroupIds,
			@QueryParam("folder") final List<Integer> privFolderIds,
			@QueryParam("privileges") final List<Privilege> privileges,
			@QueryParam("reduce") GroupReduceType reduce,
			@QueryParam("sortby") final GroupSortAttribute sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") final SortOrder sortOrder,
			@BeanParam PermsParameterBean perms);

	/**
	 * List groups.<br>
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
	 * @return list of groups
	 * @throws Exception
	 */
	@GET
	GroupList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Load group with given ID
	 * @param id local group ID
	 * @return group response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Group {id} exists."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	GroupLoadResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * List subgroups of the given group.<br>
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
	 * @param id local group ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @return list of subgroups
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/groups")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Group {id} exists."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	GroupList subgroups(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Create subgroup
	 * @param id local group ID
	 * @param group group
	 * @return created group
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/groups")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "Group was created."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	GroupLoadResponse add(@PathParam("id") String id, Group group) throws Exception;

	/**
	 * Move subgroup
	 * @param id local mother group ID
	 * @param group group
	 * @return created group
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/groups/{subgroupId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Group was moved."),
		@ResponseCode(code = 404, condition = "Group {id} or subgroup {subgroupId} does not exist."),
		@ResponseCode(code = 409, condition = "Group {subgroupId} cannot be moved to group {id}.")
	})
	GroupLoadResponse move(@PathParam("id") String id, @PathParam("subgroupId") String subgroupId) throws Exception;

	/**
	 * Delete a group
	 * @param id local group ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Group {id} was deactivated."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Update a group
	 * @param id local group ID
	 * @param group new group data
	 * @return updated group
	 * @throws Exception
	 */
	@POST
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Group {id} was updated."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	GroupLoadResponse update(@PathParam("id") String id, Group group) throws Exception;

	/**
	 * List users of given group.<br>
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
	 * @param id local group ID
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param perms permissions parameters
	 * @param embed optionally embed the referenced objects (group)
	 * @return list of group members
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/users")
	@StatusCodes({
			@ResponseCode(code = 200, condition = "Group {id} exists."),
			@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	UserList users(
			@PathParam("id") String id,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed) throws Exception;

	/**
	 * Create user in the given group
	 * @param id local group ID
	 * @param user user to create
	 * @return created user
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/users")
	@StatusCodes({
		@ResponseCode(code = 201, condition = "User was created."),
		@ResponseCode(code = 404, condition = "Group {id} does not exist.")
	})
	UserLoadResponse createUser(@PathParam("id") String id, User user) throws Exception;

	/**
	 * Add existing user to the given group
	 * @param id local group ID
	 * @param userId local user ID
	 * @return user
	 * @throws Exception
	 */
	@PUT
	@Path("/{id}/users/{userId}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "User was added."),
		@ResponseCode(code = 404, condition = "Group {id} or user {userId} does not exist.")
	})
	UserLoadResponse addUser(@PathParam("id") String id, @PathParam("userId") String userId) throws Exception;

	/**
	 * Remove user from a group
	 * @param id local group ID
	 * @param userId local user ID
	 * @return empty response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}/users/{userId}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "User was removed."),
		@ResponseCode(code = 404, condition = "Group {id} or user {userId} does not exist.")
	})
	Response removeUser(@PathParam("id") String id, @PathParam("userId") String userId) throws Exception;

	/**
	 * Get permissions set for a group. If no parentType is given, only root-level permissions will be returned.
	 * Otherwise, the permissions on child types/instances of the given parentType/parentId will be returned.
	 * @param id group ID
	 * @param parentType optional parent type
	 * @param parentId optional parent ID
	 * @param channelId optional channel ID
	 * @return Permissions response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/perms")
	TypePermissionList getPerms(@PathParam("id") String id, @QueryParam("parentType") String parentType, @QueryParam("parentId") Integer parentId,
			@QueryParam("channelId") Integer channelId) throws Exception;

	/**
	 * Get type permissions for a group
	 * @param id group ID
	 * @param type type
	 * @return Permissions response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/perms/{type}")
	TypePermissionResponse getTypePerms(@PathParam("id") String id, @PathParam("type") String type) throws Exception;

	/**
	 * Set type permissions for a group
	 * @param id group ID
	 * @param type type
	 * @param waitMs wait timeout in milliseconds
	 * @param request request
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/perms/{type}")
	GenericResponse setTypePerms(@PathParam("id") String id, @PathParam("type") String type, @QueryParam("wait") @DefaultValue("0") long waitMs,
			TypePermissionRequest request) throws Exception;

	/**
	 * Get instance permissions for a group
	 * @param id group ID
	 * @param type type
	 * @param instanceId instance ID
	 * @return Permissions response
	 * @throws Exception
	 */
	@GET
	@Path("/{id}/perms/{type}/{instanceId}")
	TypePermissionResponse getInstancePerms(@PathParam("id") String id, @PathParam("type") String type, @PathParam("instanceId") Integer instanceId)
			throws Exception;

	/**
	 * Set instance permissions for a group
	 * @param id group ID
	 * @param type type
	 * @param instanceId instance ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request request
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{id}/perms/{type}/{instanceId}")
	GenericResponse setInstancePerms(@PathParam("id") String id, @PathParam("type") String type, @PathParam("instanceId") Integer instanceId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, TypePermissionRequest request) throws Exception;
}
