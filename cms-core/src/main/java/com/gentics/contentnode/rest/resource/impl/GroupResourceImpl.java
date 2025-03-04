/*
 * @author norbert
 * @date 17.03.2011
 * @version $Id: GroupResource.java,v 1.1.2.2 2011-03-17 15:17:20 norbert Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.rest.util.MiscUtils.expectInstances;
import static com.gentics.contentnode.rest.util.MiscUtils.expectNoInstances;
import static com.gentics.contentnode.rest.util.MiscUtils.getPermPattern;
import static com.gentics.contentnode.rest.util.MiscUtils.getPermType;
import static com.gentics.contentnode.rest.util.MiscUtils.getPermissionItems;
import static com.gentics.contentnode.rest.util.MiscUtils.getRoleItems;
import static com.gentics.contentnode.rest.util.MiscUtils.getTypePermissionList;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jakarta.ws.rs.BeanParam;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserGroup.ReductionType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.request.GroupReduceType;
import com.gentics.contentnode.rest.model.request.GroupSortAttribute;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TypePermissionRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.model.response.GroupsResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TypePermissionList;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.AbstractNodeObjectFilter;
import com.gentics.contentnode.rest.util.AndFilter;
import com.gentics.contentnode.rest.util.IntFilter;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.OrFilter;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.rest.util.StringFilter;
import com.gentics.contentnode.rest.util.StringFilter.Case;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Resource to get groups
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/group")
public class GroupResourceImpl implements GroupResource {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Recursively get all given groups (and all subgroups) to the given group list
	 *
	 * @param groups
	 *            list of groups, which will have all found groups added
	 *            (uniquely)
	 * @param groupsToAdd
	 *            groups to be added
	 * @throws NodeException
	 */
	public static void recursiveAddGroups(List<UserGroup> groups,
			List<UserGroup> groupsToAdd) throws NodeException {
		// iterate over all groups
		for (UserGroup userGroup : groupsToAdd) {
			// if not yet in the list, add the group
			if (!groups.contains(userGroup)) {
				groups.add(userGroup);
			}

			// recurse into the subgroups
			recursiveAddGroups(groups, userGroup.getChildGroups());
		}
	}

	@Override
	@GET
	@Path("/load")
	public GroupsResponse load(@BeanParam PermsParameterBean perms) {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			GroupsResponse response = new GroupsResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched groups"));

			List<UserGroup> userGroups = UserGroup.reduceUserGroups(user.getUserGroups(), ReductionType.PARENT);

			List<Group> restGroups = new Vector<Group>(userGroups.size());

			for (UserGroup group : userGroups) {
				restGroups.add(ModelBuilder.getGroup(group, true));
			}
			response.setGroups(restGroups);

			optionallyAddPermissions(response, perms);

			trx.success();
			return response;
		} catch (NodeException e) {
			logger.error("Error while getting groups", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GroupsResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting groups: " + e.getLocalizedMessage()));
		}
	}

	@Override
	@GET
	@Path("/list")
	@Deprecated
	public GroupsResponse list(
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
			@BeanParam PermsParameterBean perms
			) {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			// get the authenticated user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());

			// add all groups of the user and the subgroups
			List<UserGroup> groups = new Vector<UserGroup>();

			recursiveAddGroups(groups, user.getUserGroups());

			// filter groups
			AndFilter filter = new AndFilter();

			// filter by group id
			if (!ObjectTransformer.isEmpty(ids)) {
				OrFilter idFilter = new OrFilter();

				filter.addFilter(idFilter);
				for (Integer id : ids) {
					idFilter.addFilter(new IntFilter(id, UserGroup.class.getMethod("getId")));
				}
			}
			// filter by group name
			if (!ObjectTransformer.isEmpty(names)) {
				OrFilter nameFilter = new OrFilter();

				filter.addFilter(nameFilter);
				for (String name : names) {
					nameFilter.addFilter(new StringFilter(name, UserGroup.class.getMethod("getName"), true, Case.INSENSITIVE));
				}
			}
			// filter by member login
			if (!ObjectTransformer.isEmpty(memberLogins)) {
				OrFilter memberLoginFilter = new OrFilter();

				filter.addFilter(memberLoginFilter);
				for (final String memberLogin : memberLogins) {
					memberLoginFilter.addFilter(new AbstractNodeObjectFilter() {
						public boolean matches(NodeObject object) throws NodeException {
							UserGroup group = (UserGroup) object;
							List<SystemUser> members = group.getMembers();

							for (SystemUser member : members) {
								if (memberLogin.equals(member.getLogin())) {
									return true;
								}
							}
							return false;
						}
					});
				}
			}
			// filter by member id
			if (!ObjectTransformer.isEmpty(memberIds)) {
				OrFilter memberIdFilter = new OrFilter();

				filter.addFilter(memberIdFilter);
				for (final Integer memberId : memberIds) {
					memberIdFilter.addFilter(new AbstractNodeObjectFilter() {
						public boolean matches(NodeObject object) throws NodeException {
							UserGroup group = (UserGroup) object;
							List<SystemUser> members = group.getMembers();

							for (SystemUser member : members) {
								if (member.getId().equals(memberId)) {
									return true;
								}
							}
							return false;
						}
					});
				}
			}
			// filter by child groups
			if (!ObjectTransformer.isEmpty(childGroupIds)) {
				final List<UserGroup> possibleMatches = new Vector<UserGroup>();

				for (Integer childGroupId : childGroupIds) {
					UserGroup childGroup = t.getObject(UserGroup.class, childGroupId);

					possibleMatches.addAll(childGroup.getParents());
				}
				filter.addFilter(new AbstractNodeObjectFilter() {
					public boolean matches(NodeObject object) throws NodeException {
						return possibleMatches.contains(object);
					}
				});
			}

			// filter by privileges
			if (!ObjectTransformer.isEmpty(privFolderIds) && !ObjectTransformer.isEmpty(privileges)) {
				filter.addFilter(new AbstractNodeObjectFilter() {
					public boolean matches(NodeObject object) throws NodeException {
						// initialize the perm handler for the specific group
						PermHandler permHandler = new PermHandler();

						permHandler.initForGroup(ObjectTransformer.getInt(object.getId(), 0));

						// check all privileges
						for (Privilege priv : privileges) {
							// check for all given folders
							for (Integer privFolderId : privFolderIds) {
								// if the requested permbit is not set, filter the object
								if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER, privFolderId, priv.getPermBit())) {
									return false;
								}
							}
						}

						// all requested privileges found for the group, it is not filtered
						return true;
					}
				});
			}

			// now apply the filters
			filter.filter(groups);

			// optionally reduce the list of groups (if requested)
			if (reduce != null) {
				switch (reduce) {
				case child:
					groups = UserGroup.reduceUserGroups(groups, ReductionType.CHILD);
					break;

				case parent:
					groups = UserGroup.reduceUserGroups(groups, ReductionType.PARENT);
					break;
				}
			}

			// sort groups
			if (sortBy != null) {
				Collections.sort(groups, new Comparator<UserGroup>() {
					public int compare(UserGroup group1, UserGroup group2) {
						switch (sortBy) {
						case id:
							int id1 = ObjectTransformer.getInt(group1.getId(), 0);
							int id2 = ObjectTransformer.getInt(group2.getId(), 0);

							return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? id1 - id2 : id2 - id1;

						case name:
							String value1 = group1.getName();
							String value2 = group2.getName();

							return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? value1.compareToIgnoreCase(value2) : value2.compareToIgnoreCase(value1);

						default:
							return 0;
						}
					}
				});
			}

			// page groups
			if (skipCount > 0 || maxItems >= 0) {
				int startIndex = Math.min(skipCount, groups.size());
				int endIndex = 0;

				if (maxItems >= 0) {
					endIndex = Math.min(startIndex + maxItems, groups.size());
				} else {
					endIndex = groups.size();
				}
				groups = groups.subList(startIndex, endIndex);
			}

			GroupsResponse response = new GroupsResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched groups"));
			List<Group> restGroups = new Vector<Group>(groups.size());

			for (UserGroup group : groups) {
				restGroups.add(ModelBuilder.getGroup(group, false));
			}
			response.setGroups(restGroups);

			optionallyAddPermissions(response, perms);

			trx.success();
			return response;
		} catch (Exception e) {
			logger.error("Error while getting groups", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GroupsResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting groups: " + e.getLocalizedMessage()));
		}
	}

	@Override
	@GET
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			// get the authenticated user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());

			// add all groups of the user and the subgroups
			List<UserGroup> groups = new ArrayList<>();
			recursiveAddGroups(groups, user.getUserGroups());

			GroupList response = ListBuilder.from(groups, UserGroup.TRANSFORM2REST)
				.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "description"))
				.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete, ObjectPermission.setperm, ObjectPermission.userassignment))
				.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "description"))
				.page(paging)
				.to(new GroupList());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupLoadResponse get(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);
			GroupLoadResponse response = new GroupLoadResponse(null, ResponseInfo.ok("Successfully loaded group"), UserGroup.TRANSFORM2REST.apply(group));
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/groups")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupList subgroups(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);
			GroupList response = ListBuilder.from(group.getChildGroups(), UserGroup.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "description"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete, ObjectPermission.setperm, ObjectPermission.userassignment))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "description"))
					.page(paging)
					.to(new GroupList());

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/groups")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupLoadResponse add(@PathParam("id") String id, Group group) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, UserGroup.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(UserGroup.class)), id), null, null,
						UserGroup.TYPE_USERGROUP, 0, PermType.creategroup);
			}

			UserGroup mother = MiscUtils.load(UserGroup.class, id);

			UserGroup newGroup = UserGroup.REST2NODE.apply(group, t.createObject(UserGroup.class));
			newGroup.setMotherId(mother.getId());
			newGroup.save();

			GroupLoadResponse response = new GroupLoadResponse(null, ResponseInfo.ok("Successfully created group"),
					UserGroup.TRANSFORM2REST.apply(t.getObject(newGroup)));

			trx.success();
			return response;
		}
	}

	@PUT
	@Path("/{id}/groups/{subgroupId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupLoadResponse move(@PathParam("id") String id, @PathParam("subgroupId") String subgroupId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			UserGroup moved = MiscUtils.load(UserGroup.class, subgroupId, ObjectPermission.delete);
			UserGroup target = MiscUtils.load(UserGroup.class, id);

			if (!t.getPermHandler().canCreate(null, UserGroup.class, null)) {
				throw new InsufficientPrivilegesException(
						I18NHelper.get(String.format("%s.nopermission", t.getTable(UserGroup.class)),
								String.format("%s (%s)", I18NHelper.getName(target), id)),
						null, null, UserGroup.TYPE_USERGROUP, 0, PermType.creategroup);
			}

			moved = t.getObject(moved, true);
			moved.move(target);

			GroupLoadResponse response = new GroupLoadResponse(null, ResponseInfo.ok("Successfully moved group"),
					UserGroup.TRANSFORM2REST.apply(t.getObject(moved)));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public Response delete(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			UserGroup group = MiscUtils.load(UserGroup.class, id, ObjectPermission.delete);
			group = t.getObject(group, true);
			group.delete();

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@POST
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupLoadResponse update(@PathParam("id") String id, Group group) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			UserGroup updated = MiscUtils.load(UserGroup.class, id, ObjectPermission.edit);

			updated = UserGroup.REST2NODE.apply(group, t.getObject(updated, true));
			updated.save();

			GroupLoadResponse response = new GroupLoadResponse(null, ResponseInfo.ok("Successfully updated group"),
					UserGroup.TRANSFORM2REST.apply(t.getObject(updated)));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/users")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserList users(
			@PathParam("id") String id,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);
			List<SystemUser> members = group.getMembers();

			Map<String, String> fieldMap = new HashMap<>();
			fieldMap.put("firstName", "firstname");
			fieldMap.put("lastName", "lastname");

			UserList response = ListBuilder.from(members, SystemUser.TRANSFORM2REST)
					.filter(ResolvableFilter.get(filter, fieldMap, "id", "globalId", "firstName", "lastName", "login",
							"email"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit,
							ObjectPermission.delete))
					.sort(
							ResolvableComparator.get(sorting, fieldMap, "id", "globalId", "firstName", "lastName", "login",
									"email"))
					.embed(embed, "group", SystemUser.EMBED_GROUPS)
					.page(paging)
					.to(new UserList());

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/users")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserLoadResponse createUser(@PathParam("id") String id, User user) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, SystemUser.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(UserGroup.class)), id), null, null,
						SystemUser.TYPE_SYSTEMUSER, 0, PermType.createuser);
			}

			UserGroup group = MiscUtils.load(UserGroup.class, id, ObjectPermission.userassignment);

			SystemUser systemUser = SystemUser.REST2NODE.apply(user, t.createObject(SystemUser.class));
			systemUser.setActive(true);
			systemUser.getUserGroups().add(group);
			systemUser.save();

			if (!ObjectTransformer.isEmpty(user.getPassword())) {
				int userId = ObjectTransformer.getInt(systemUser.getId(), -1);
				systemUser.setPassword(SystemUserFactory.hashPassword(user.getPassword(),userId));
				systemUser.save();
			}

			UserLoadResponse response = new UserLoadResponse(null, ResponseInfo.ok("Successfully created user in group"),
					SystemUser.TRANSFORM2REST.apply(t.getObject(systemUser)));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/users/{userId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserLoadResponse addUser(@PathParam("id") String id, @PathParam("userId") String userId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			UserGroup group = MiscUtils.load(UserGroup.class, id, ObjectPermission.userassignment);

			SystemUser user = MiscUtils.load(SystemUser.class, userId);

			group = t.getObject(group, true);
			group.getMembers().add(user);
			group.save();

			UserLoadResponse response = new UserLoadResponse(null, ResponseInfo.ok("Successfully added user to group"),
					SystemUser.TRANSFORM2REST.apply(t.getObject(user)));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}/users/{userId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public Response removeUser(@PathParam("id") String id, @PathParam("userId") String userId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			UserGroup group = MiscUtils.load(UserGroup.class, id, ObjectPermission.userassignment);

			SystemUser user = MiscUtils.load(SystemUser.class, userId);

			group = t.getObject(group, true);
			group.getMembers().remove(user);
			group.save();

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/perms")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public TypePermissionList getPerms(@PathParam("id") String id, @QueryParam("parentType") String parentType, @QueryParam("parentId") Integer parentId,
			@QueryParam("channelId") Integer channelId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);

			if (StringUtils.isEmpty(parentType)) {
				TypePermissionList response = getTypePermissionList(group, TypePerms.getRootTypes(), null, null);
				trx.success();
				return response;
			} else {
				TypePerms type = getPermType(parentType);

				if (parentId != null) {
					expectInstances(type);

					// check permission to see the parent instance
					if (!type.canView(parentId)) {
						throw new InsufficientPrivilegesException(I18NHelper.get("perm.instance.nopermission", type.toString(), Integer.toString(parentId)),
								null, null, type.type(), parentId, PermType.read);
					}

					// special case, when children of a Node are requested, we set the channelId
					if (type == TypePerms.node) {
						Folder folder = trx.getTransaction().getObject(Folder.class, parentId);
						if (folder != null && folder.isRoot()) {
							Node channel = folder.getChannel();
							if (channel != null) {
								channelId = channel.getId();
							} else {
								channelId = folder.getNode().getId();
							}
						} else {
							I18nString message = new CNI18nString("folder.notfound");
							throw new EntityNotFoundException(message.toString());
						}
					}

					TypePermissionList response = getTypePermissionList(group, type.getChildTypes(), parentId, channelId);
					trx.success();
					return response;
				} else {
					expectNoInstances(type);

					// check permission to see the parent type
					if (!type.canView()) {
						throw new InsufficientPrivilegesException(I18NHelper.get("perm.type.nopermission", type.toString()), null, null, type.type(), 0,
								PermType.read);
					}

					TypePermissionList response = getTypePermissionList(group, type.getChildTypes(), null, null);
					trx.success();
					return response;
				}
			}
		}
	}

	@Override
	@GET
	@Path("/{id}/perms/{type}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public TypePermissionResponse getTypePerms(@PathParam("id") String id, @PathParam("type") String type) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);
			TypePerms permType = getPermType(type);
			expectNoInstances(permType);

			if (!permType.canView()) {
				throw new InsufficientPrivilegesException(I18NHelper.get("perm.type.nopermission", permType.toString()), null, null, permType.type(), 0,
						PermType.read);
			}

			PermHandler permHandler = trx.getTransaction().getGroupPermHandler(group.getId());
			PermissionPair permissions = permHandler.getPermissions(permType.type(), null, -1, -1);
			boolean editable = trx.getTransaction().getPermHandler().canSetPerms(group) && permType.canSetPerms();
			TypePermissionResponse response = new TypePermissionResponse(null, ResponseInfo.ok(""))
					.setPerms(getPermissionItems(permType, permissions.getGroupPermissions(), editable));

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}/perms/{type}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GenericResponse setTypePerms(@PathParam("id") String id, @PathParam("type") String type, @QueryParam("wait") @DefaultValue("0") long waitMs,
			TypePermissionRequest request) throws NodeException {
		TypePerms permType = getPermType(type);
		expectNoInstances(permType);

		SetPermsRequest req = new SetPermsRequest();
		req.setGroupId(Integer.parseInt(id));
		req.setSubGroups(request.isSubGroups());
		req.setSubObjects(request.isSubObjects());
		req.setPerm(getPermPattern(request.getPerms()));

		try (Trx trx = ContentNodeHelper.trx()) {
			GenericResponse response = Operator.executeRethrowing(I18NHelper.get("assign_user_permissions"), waitMs, () -> {
				return doSetPermissions(permType, req);
			});
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/perms/{type}/{instanceId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public TypePermissionResponse getInstancePerms(@PathParam("id") String id, @PathParam("type") String type, @PathParam("instanceId") Integer instanceId)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserGroup group = MiscUtils.load(UserGroup.class, id);
			TypePerms permType = getPermType(type);
			expectInstances(permType);

			if (!permType.canView(instanceId)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("perm.instance.nopermission", permType.toString(), Integer.toString(instanceId)), null,
						null, permType.type(), instanceId, PermType.read);
			}

			PermHandler permHandler = trx.getTransaction().getGroupPermHandler(group.getId());
			PermissionPair permissions = permHandler.getPermissions(permType.type(), instanceId, -1, -1);
			boolean editable = trx.getTransaction().getPermHandler().canSetPerms(group) && permType.canSetPerms(instanceId);
			TypePermissionResponse response = new TypePermissionResponse(null, ResponseInfo.ok(""))
					.setPerms(getPermissionItems(permType, permissions.getGroupPermissions(), editable));

			if (permType.isRoles()) {
				response.setRoles(getRoleItems(group, permType, instanceId, editable));
			}

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}/perms/{type}/{instanceId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GenericResponse setInstancePerms(@PathParam("id") String id, @PathParam("type") String type, @PathParam("instanceId") Integer instanceId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, TypePermissionRequest request) throws NodeException {
		TypePerms permType = getPermType(type);
		expectInstances(permType);

		try (Trx trx = ContentNodeHelper.trx()) {
			SetPermsRequest req = new SetPermsRequest();
			req.setGroupId(Integer.parseInt(id));
			req.setSubGroups(request.isSubGroups());
			req.setSubObjects(request.isSubObjects());
			req.setPerm(getPermPattern(request.getPerms()));
			if (permType.isRoles() && request.getRoles() != null) {
				UserGroup group = MiscUtils.load(UserGroup.class, id);
				Set<Integer> setRoles = new HashSet<>(PermHandler.getRoles(permType.type(), instanceId, group));
				for (RoleItem role : request.getRoles()) {
					if (role.isValue()) {
						setRoles.add(role.getId());
					} else {
						setRoles.remove(role.getId());
					}
				}
				req.setRoleIds(setRoles);
			}

			GenericResponse response = Operator.executeRethrowing(I18NHelper.get("assign_user_permissions"), waitMs, () -> {
				return doSetPermissions(permType, instanceId, req);
			});
			trx.success();
			return response;
		}
	}

	protected void optionallyAddPermissions(GroupsResponse response, PermsParameterBean perms) throws NodeException {
		Function<NodeObject, Pair<Integer, Set<Permission>>> permissionFunction = permFunction(perms, ObjectPermission.view,
				ObjectPermission.edit, ObjectPermission.delete, ObjectPermission.setperm,
				ObjectPermission.userassignment);
		if (permissionFunction != null) {
			Map<Integer, Set<Permission>> permsMap = new HashMap<>();
			response.setPerms(permsMap);

			recursivelyAddPermissions(permsMap, response.getGroups(), permissionFunction);
		}
	}

	protected void recursivelyAddPermissions(Map<Integer, Set<Permission>> permsMap, List<Group> groups,
			Function<NodeObject, Pair<Integer, Set<Permission>>> permissionFunction) throws NodeException {
		if (groups == null) {
			return;
		}

		for (Group group : groups) {
			UserGroup userGroup = TransactionManager.getCurrentTransaction().getObject(UserGroup.class, group.getId());
			if (userGroup != null) {
				Pair<Integer, Set<Permission>> permInfo = permissionFunction.apply(userGroup);
				if (permInfo != null && ObjectTransformer.getInt(permInfo.getKey(), 0) != 0) {
					permsMap.put(permInfo.getKey(), permInfo.getValue());
				}
			}
			recursivelyAddPermissions(permsMap, group.getChildren(), permissionFunction);
		}
	}
}
