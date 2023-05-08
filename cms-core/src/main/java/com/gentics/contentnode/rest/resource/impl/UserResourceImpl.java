package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.UserSaveRequest;
import com.gentics.contentnode.rest.model.request.UserSortAttribute;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.NodeRestrictionResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.UserDataResponse;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.UserResource;
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
import com.gentics.contentnode.rest.util.OrFilter;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.rest.util.StringFilter;
import com.gentics.contentnode.rest.util.StringFilter.Case;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Resource to get Users. The list of users returned will always be filtered by
 * user permission: A user may only see users that are members of the same
 * groups or subgroups of the groups, the user is member of
 */
@Produces({ "application/json; charset=UTF-8", "application/xml; charset=UTF-8"})
@Authenticated
@Path("/user")
public class UserResourceImpl implements UserResource {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Methods to use when user search is used
	 */
	private static final String[] SEARCH_METHODS = new String[] { "getLogin", "getFirstname", "getLastname", "getEmail"};

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.UserResource#getMe(boolean)
	 */
	@GET
	@Path("/me")
	public UserLoadResponse getMe(
			@QueryParam("groups") @DefaultValue("false") boolean groups) {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser systemUser = t.getObject(SystemUser.class, t.getUserId());
			User user = null;

			if (groups) {
				user = ModelBuilder.getUser(systemUser, Reference.GROUPS, Reference.DESCRIPTION, Reference.USER_LOGIN);
			} else {
				user = ModelBuilder.getUser(systemUser, Reference.DESCRIPTION, Reference.USER_LOGIN);
			}

			trx.success();
			return new UserLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded current user"), user);
		} catch (Exception e) {
			logger.error("Error while getting current user", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new UserLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting current user: " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.gentics.contentnode.rest.api.UserResource#save
	 */
	@POST
	@Path("/save/{id}")
	@Override
	@Deprecated
	public GenericResponse save(@PathParam("id") Integer id, UserSaveRequest request) {
		try {
			return update(id.toString(), request.getUser());
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while saving user " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while saving user " + id + ": " + e.getLocalizedMessage()));
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.UserResource#list(java.lang.Integer, java.lang.Integer, java.util.List, java.util.List, java.util.List, java.util.List, java.util.List, java.util.List, java.lang.String, com.gentics.contentnode.rest.model.request.UserSortAttribute, com.gentics.contentnode.rest.model.request.SortOrder, boolean)
	 */
	@GET
	@Path("/list")
	@Deprecated
	public UserListResponse list(
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
			@QueryParam("groups") @DefaultValue("false") boolean addGroups) {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			// get the authenticated user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());

			// add all users in the groups of the user and from subgroups
			List<SystemUser> users = new Vector<>();

			recursiveAddUsers(users, user.getUserGroups());

			List<String> currentSearchMethods = new ArrayList<>(Arrays.asList(SEARCH_METHODS));

			// filter users
			AndFilter filter = new AndFilter();

			filter.addFilter(new IntFilter(1, SystemUser.class.getMethod("getActive")));

			if (!ObjectTransformer.isEmpty(ids)) {
				OrFilter idFilter = new OrFilter();

				filter.addFilter(idFilter);
				for (Integer id : ids) {
					idFilter.addFilter(new IntFilter(id, SystemUser.class.getMethod("getId")));
				}
			}
			if (!ObjectTransformer.isEmpty(logins)) {
				OrFilter loginFilter = new OrFilter();

				filter.addFilter(loginFilter);
				currentSearchMethods.remove("getLogin");
				for (String l : logins) {
					loginFilter.addFilter(new StringFilter(l, SystemUser.class.getMethod("getLogin"), true, Case.INSENSITIVE));
				}
			}
			if (!ObjectTransformer.isEmpty(firstNames)) {
				OrFilter firstNameFilter = new OrFilter();

				filter.addFilter(firstNameFilter);
				currentSearchMethods.remove("getFirstname");
				for (String fn : firstNames) {
					firstNameFilter.addFilter(new StringFilter(fn, SystemUser.class.getMethod("getFirstname"), true, Case.INSENSITIVE));
				}
			}
			if (!ObjectTransformer.isEmpty(lastNames)) {
				OrFilter lastNameFilter = new OrFilter();

				filter.addFilter(lastNameFilter);
				currentSearchMethods.remove("getLastname");
				for (String ln : lastNames) {
					lastNameFilter.addFilter(new StringFilter(ln, SystemUser.class.getMethod("getLastname"), true, Case.INSENSITIVE));
				}
			}
			if (!ObjectTransformer.isEmpty(eMails)) {
				OrFilter eMailFilter = new OrFilter();

				filter.addFilter(eMailFilter);
				currentSearchMethods.remove("getEmail");
				for (String em : eMails) {
					eMailFilter.addFilter(new StringFilter(em, SystemUser.class.getMethod("getEmail"), true, Case.INSENSITIVE));
				}
			}
			if (!ObjectTransformer.isEmpty(groupIds)) {
				OrFilter groupFilter = new OrFilter();

				filter.addFilter(groupFilter);
				for (final Integer gid : groupIds) {
					groupFilter.addFilter(new AbstractNodeObjectFilter() {
						public boolean matches(NodeObject object) throws NodeException {
							List<UserGroup> userGroups = ((SystemUser) object).getUserGroups();

							for (UserGroup group : userGroups) {
								if (group.getId().equals(gid)) {
									return true;
								}
							}

							return false;
						}
					});
				}
			}

			if (!ObjectTransformer.isEmpty(search) && !ObjectTransformer.isEmpty(currentSearchMethods)) {
				OrFilter searchFilter = new OrFilter();

				filter.addFilter(searchFilter);
				for (String method: currentSearchMethods) {
					searchFilter.addFilter(new StringFilter(search, SystemUser.class.getMethod(method), true, Case.INSENSITIVE));
				}
			}
			filter.filter(users);


			// sort users
			if (sortBy != null) {
				Collections.sort(users, new Comparator<SystemUser>() {
					public int compare(SystemUser user1, SystemUser user2) {
						String value1 = null;
						String value2 = null;

						switch (sortBy) {
						case id:
							switch (sortOrder) {
							case asc:
							case ASC:
							default:
								return user1.getId() - user2.getId();
							case desc:
							case DESC:
								return user2.getId() - user1.getId();
							}

						case login:
							value1 = ObjectTransformer.getString(user1.getLogin(), "");
							value2 = ObjectTransformer.getString(user2.getLogin(), "");
							break;

						case firstname:
							value1 = ObjectTransformer.getString(user1.getFirstname(), "");
							value2 = ObjectTransformer.getString(user2.getFirstname(), "");
							break;

						case lastname:
							value1 = ObjectTransformer.getString(user1.getLastname(), "");
							value2 = ObjectTransformer.getString(user2.getLastname(), "");
							break;

						case email:
							value1 = ObjectTransformer.getString(user1.getEmail(), "");
							value2 = ObjectTransformer.getString(user2.getEmail(), "");
							break;

						default:
							return 0;
						}

						return sortOrder == SortOrder.asc ? value1.compareToIgnoreCase(value2) : value2.compareToIgnoreCase(value1);
					}
				});
			}

			// do paging
			if (skipCount > 0 || maxItems >= 0) {
				int startIndex = Math.min(skipCount, users.size());
				int endIndex = 0;

				if (maxItems >= 0) {
					endIndex = Math.min(startIndex + maxItems, users.size());
				} else {
					endIndex = users.size();
				}
				users = users.subList(startIndex, endIndex);
			}

			UserListResponse response = new UserListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched users"));
			List<User> restUsers = new Vector<>(users.size());

			for (SystemUser nodeUser : users) {
				if (addGroups) {
					restUsers.add(ModelBuilder.getUser(nodeUser, Reference.DESCRIPTION, Reference.USER_LOGIN, Reference.GROUPS));
				} else {
					restUsers.add(ModelBuilder.getUser(nodeUser, Reference.DESCRIPTION, Reference.USER_LOGIN));
				}
			}
			response.setUsers(restUsers);

			trx.success();
			return response;
		} catch (Exception e) {
			logger.error("Error while getting users", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new UserListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting users: " + e.getLocalizedMessage()));
		}
	}

	@Override
	@POST
	@Path("/me/data/{key}")
	@Consumes({MediaType.APPLICATION_JSON})
	public GenericResponse saveUserData(@PathParam("key") String key, JsonNode jsonData) {
		try (Trx trx = ContentNodeHelper.trx()) {
			int userId = trx.getTransaction().getUserId();
			Map<String, Object> idMap = new HashMap<>();
			idMap.put("systemuser_id", userId);
			idMap.put("name", key);

			Map<String, Object> dataMap = new HashMap<>();
			dataMap.put("json", ObjectTransformer.getString(jsonData, null));

			DBUtils.updateOrInsert("systemuser_data", idMap, dataMap);
			trx.success();

			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		} catch (NodeException e) {
			logger.error(String.format("Error while saving '%s' for key '%s'", jsonData.toString(), key), e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	@Override
	@GET
	@Path("/me/data/{key}")
	@Produces({MediaType.APPLICATION_JSON})
	public UserDataResponse getUserData(@PathParam("key") String key) {
		try (Trx trx = ContentNodeHelper.trx()) {
			int userId = trx.getTransaction().getUserId();
			String value = DBUtils.select("SELECT json FROM systemuser_data WHERE systemuser_id = ? AND name = ?", new DBUtils.PrepareStatement() {
				@Override
				public void prepare(PreparedStatement stmt) throws SQLException, NodeException {
					stmt.setInt(1, userId);
					stmt.setString(2, key);
				}
			}, new DBUtils.HandleSelectResultSet<String>() {
				@Override
				public String handle(ResultSet res) throws SQLException, NodeException {
					if (res.next()) {
						return res.getString("json");
					} else {
						return null;
					}
				}
			});
			UserDataResponse response = new UserDataResponse();
			if (value != null) {
				response.setData(new ObjectMapper().readValue(value, JsonNode.class));
			} else {
				response.setData(new ObjectMapper().getNodeFactory().nullNode());
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, null));
			trx.success();
			return response;
		} catch (Exception e) {
			logger.error(String.format("Error while getting json for key '%s'", key), e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	@Override
	@DELETE
	@Path("/me/data/{key}")
	public GenericResponse deleteUserData(@PathParam("key") String key) {
		try (Trx trx = ContentNodeHelper.trx()) {
			int userId = trx.getTransaction().getUserId();
			DBUtils.deleteWithPK("systemuser_data", "id", "systemuser_id = ? AND name = ?", new Object[] {userId, key});
			trx.success();

			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		} catch (NodeException e) {
			logger.error(String.format("Error while deleting json for key '%s'", key), e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	@Override
	@GET
	@Path("/me/data")
	@Produces({MediaType.APPLICATION_JSON})
	public UserDataResponse getAllUserData() {
		try (Trx trx = ContentNodeHelper.trx()) {
			int userId = trx.getTransaction().getUserId();
			JsonNode data = DBUtils.select("SELECT name, json FROM systemuser_data WHERE systemuser_id = ?", new DBUtils.PrepareStatement() {
				@Override
				public void prepare(PreparedStatement stmt) throws SQLException, NodeException {
					stmt.setInt(1, userId);
				}
			}, new DBUtils.HandleSelectResultSet<JsonNode>() {
				@Override
				public JsonNode handle(ResultSet res) throws SQLException, NodeException {
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode object = mapper.createObjectNode();

					try {
						while (res.next()) {
							String name = res.getString("name");
							String value = res.getString("json");
							if (value == null) {
								object.putNull(name);
							} else {
								object.put(name, mapper.readValue(value, JsonNode.class));
							}
						}
					} catch (SQLException e) {
						throw e;
					} catch (Exception e) {
						throw new NodeException("Error while reading data from systemuser_data", e);
					}
					return object;
				}
			});

			UserDataResponse response = new UserDataResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, null));
			response.setData(data);
			trx.success();
			return response;
		} catch (NodeException e) {
			logger.error("Error while getting all user data", e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	@Override
	@GET
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserList list(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed
	) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());

			// add all users in the groups of the user and from subgroups
			List<SystemUser> users = new ArrayList<>();
			recursiveAddUsers(users, user.getUserGroups());

			UserList response = ListBuilder.from(users, SystemUser.TRANSFORM2REST)
				.filter(ResolvableFilter.get(filter, "id", "globalId", "firstName", "lastName", "login", "email"))
				.sort(ResolvableComparator.get(sorting, "id", "globalId", "firstName", "lastName", "login", "email"))
				.embed(embed, "group", SystemUser.EMBED_GROUPS)
				.page(paging)
				.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
				.to(new UserList());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserLoadResponse get(
			@PathParam("id") String id,
			@BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			User restUser = SystemUser.TRANSFORM2REST.apply(user);

			if (embeddedParameterContainsAttribute(embed, "group")) {
				SystemUser.EMBED_GROUPS.accept(restUser);
			}
			UserLoadResponse response = new UserLoadResponse(null, ResponseInfo.ok("Successfully loaded SystemUser"), restUser);
			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public UserLoadResponse update(@PathParam("id") String id, User item) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SystemUser user = MiscUtils.load(SystemUser.class, id, ObjectPermission.edit);
			// get editable copy of object
			user = trx.getTransaction().getObject(user, true);
			// change data and save
			SystemUser.REST2NODE.apply(item, user);

			// The REST2NODE transformer intentionally does not process the password field.
			if (!ObjectTransformer.isEmpty(item.getPassword())) {
				int userId = ObjectTransformer.getInt(user.getId(), -1);

				user.setPassword(SystemUserFactory.hashPassword(item.getPassword(), userId));
			}

			user.save();
			UserLoadResponse response = new UserLoadResponse(null, ResponseInfo.ok("Successfully updated SystemUser"), SystemUser.TRANSFORM2REST.apply(user));
			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	public Response deactivate(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			SystemUser user = MiscUtils.load(SystemUser.class, id, ObjectPermission.delete);
			// get editable copy of object
			user = trx.getTransaction().getObject(user, true);
			user.delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/groups")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupList groups(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			List<UserGroup> groups = new ArrayList<>(user.getUserGroups());
			PermFilter.get(ObjectPermission.view).filter(groups);

			GroupList response = ListBuilder.from(groups, UserGroup.TRANSFORM2REST)
				.filter(ResolvableFilter.get(filter, "id", "globalId", "firstName", "lastName", "login", "email"))
				.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete, ObjectPermission.userassignment, ObjectPermission.setperm))
				.sort(ResolvableComparator.get(sorting, "id", "globalId", "firstName", "lastName", "login", "email"))
				.page(paging)
				.to(new GroupList());

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/groups/{groupId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public GroupLoadResponse addToGroup(@PathParam("id") String id, @PathParam("groupId") String groupId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			UserGroup group = MiscUtils.load(UserGroup.class, groupId);
			MiscUtils.check(group, PermType.userassignment, (g, ph) -> {
				return ph.checkGroupPerm(g, p -> p.checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PermHandler.PERM_GROUP_USERADD));
			});

			group = t.getObject(group, true);
			group.getMembers().add(user);
			group.save();

			GroupLoadResponse response = new GroupLoadResponse(null, ResponseInfo.ok("Successfully added user to group"),
					UserGroup.TRANSFORM2REST.apply(group));
			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}/groups/{groupId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public Response removeFromGroup(@PathParam("id") String id, @PathParam("groupId") String groupId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			UserGroup group = MiscUtils.load(UserGroup.class, groupId);
			MiscUtils.check(group, PermType.userassignment, (g, ph) -> {
				return ph.checkGroupPerm(g, p -> p.checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PermHandler.PERM_GROUP_USERADD));
			});

			group = t.getObject(group, true);
			group.getMembers().remove(user);
			group.save();

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/groups/{groupId}/nodes")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public NodeRestrictionResponse getGroupNodeRestrictions(@PathParam("id") String id, @PathParam("groupId") String groupId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			UserGroup group = MiscUtils.load(UserGroup.class, groupId);

			if (!user.getUserGroups().contains(group)) {
				throw new EntityNotFoundException(I18NHelper.get(String.format("%s.notfound", t.getTable(UserGroup.class)), String.valueOf(group.getId())));
			}

			NodeRestrictionResponse response = getNodeRestrictions(user, group);
			response.setResponseInfo(ResponseInfo.ok("Successfully fetched node restrictions"));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/groups/{groupId}/nodes/{nodeId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public NodeRestrictionResponse addGroupNodeRestriction(@PathParam("id") String id, @PathParam("groupId") String groupId, @PathParam("nodeId") String nodeId)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			UserGroup group = MiscUtils.load(UserGroup.class, groupId);
			Node node = MiscUtils.load(Node.class, nodeId);
			MiscUtils.check(group, PermType.userassignment, (g, ph) -> {
				return ph.checkGroupPerm(g, p -> p.checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PermHandler.PERM_GROUP_USERADD));
			});

			if (!user.getUserGroups().contains(group)) {
				throw new EntityNotFoundException(I18NHelper.get(String.format("%s.notfound", t.getTable(UserGroup.class)), String.valueOf(group.getId())));
			}

			user = t.getObject(user, true);
			user.getGroupNodeRestrictions().computeIfAbsent(group.getId(), key -> new HashSet<>()).add(node.getId());
			user.save();

			user = t.getObject(user);

			NodeRestrictionResponse response = getNodeRestrictions(user, group);
			response.setResponseInfo(ResponseInfo.ok("Successfully added node restriction"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}/groups/{groupId}/nodes/{nodeId}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = SystemUser.TYPE_USERADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = UserGroup.TYPE_GROUPADMIN, bit = PermHandler.PERM_VIEW)
	public NodeRestrictionResponse removeGroupNodeRestriction(@PathParam("id") String id, @PathParam("groupId") String groupId, @PathParam("nodeId") String nodeId)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			SystemUser user = MiscUtils.load(SystemUser.class, id);
			UserGroup group = MiscUtils.load(UserGroup.class, groupId);
			Node node = MiscUtils.load(Node.class, nodeId);
			MiscUtils.check(group, PermType.userassignment, (g, ph) -> {
				return ph.checkGroupPerm(g, p -> p.checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PermHandler.PERM_GROUP_USERADD));
			});

			if (!user.getUserGroups().contains(group)) {
				throw new EntityNotFoundException(I18NHelper.get(String.format("%s.notfound", t.getTable(UserGroup.class)), String.valueOf(group.getId())));
			}

			user = t.getObject(user, true);
			Map<Integer, Set<Integer>> assignmentMap = user.getGroupNodeRestrictions();
			if (assignmentMap.containsKey(group.getId())) {
				assignmentMap.get(group.getId()).remove(node.getId());
				user.save();
			}

			user = t.getObject(user);

			NodeRestrictionResponse response = getNodeRestrictions(user, group);
			response.setResponseInfo(ResponseInfo.ok("Successfully removed node restriction"));

			trx.success();
			return response;
		}
	}

	/**
	 * Get the current node restrictions for as user-group assignment
	 * @param user user
	 * @param group group
	 * @return set of node IDs (may be empty, but not null)
	 * @throws NodeException
	 */
	protected NodeRestrictionResponse getNodeRestrictions(SystemUser user, UserGroup group) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Node> nodes = new ArrayList<>(t.getObjects(Node.class, user.getGroupNodeRestrictions().getOrDefault(group.getId(), Collections.emptySet())));
		int total = nodes.size();
		PermFilter.get(ObjectPermission.view).filter(nodes);
		int visible = nodes.size();

		return new NodeRestrictionResponse().setHidden(total - visible).setNodeIds(nodes.stream().map(Node::getId).collect(Collectors.toSet()));
	}

	/**
	 * Recursively get all users from the given groups (and all subgroups) adn
	 * add them to the given user list
	 *
	 * @param users
	 *            list of users, which will have all found users added
	 *            (uniquely)
	 * @param groups
	 *            groups from which the members are added to the list
	 * @throws NodeException
	 */
	protected void recursiveAddUsers(List<SystemUser> users,
			List<UserGroup> groups) throws NodeException {
		// iterate over all groups
		for (UserGroup userGroup : groups) {
			// get the group members
			List<SystemUser> members = userGroup.getMembers();

			// iterate over the group members
			for (SystemUser systemUser : members) {
				// if not yet in the list, add the group member
				if (!users.contains(systemUser)) {
					users.add(systemUser);
				}
			}

			// recurse into the subgroups
			recursiveAddUsers(users, userGroup.getChildGroups());
		}
	}
}
