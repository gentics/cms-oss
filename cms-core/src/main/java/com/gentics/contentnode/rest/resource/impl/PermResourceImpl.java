package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.rest.util.MiscUtils.expectInstances;
import static com.gentics.contentnode.rest.util.MiscUtils.expectNoInstances;
import static com.gentics.contentnode.rest.util.MiscUtils.getPermType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupsPermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.PermResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Resource implementation for loading and saving permission bits and roles
 */
@Path("/perm")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class PermResourceImpl implements PermResource {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	@Override
	@GET
	@Path("/{type}")
	public PermBitsResponse getPermissions(@PathParam("type") String objType, @QueryParam("map") @DefaultValue("false") boolean privilegeMap) throws NodeException {
		TypePerms permType = getPermType(objType);
		expectNoInstances(permType);
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			PermissionPair perms = permType.getTypePermissions();

			if (logger.isInfoEnabled()) {
				StringBuilder info = new StringBuilder("PermBits for user ");
				info.append(t.getUserId()).append(" for ").append(objType);
				info.append(" is ").append(perms.toString());
				logger.info(info.toString());
			}

			PermBitsResponse response = new PermBitsResponse(ObjectTransformer.getString(perms.getGroupPermissions(), null),
					ObjectTransformer.getString(perms.getRolePermissions(), null));

			if (privilegeMap) {
				response.setPermissionsMap(permType.getPermissionMap(0));
			}

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{type}/{id}")
	public PermBitsResponse getPermissions(@PathParam("type") String objType, @PathParam("id") int objId, @QueryParam("nodeId") @DefaultValue("0") int nodeId,
			@QueryParam("type") @DefaultValue("-1") int checkType, @QueryParam("lang") @DefaultValue("0") int languageId,
			@QueryParam("map") @DefaultValue("false") boolean privilegeMap) throws NodeException {
		TypePerms permType = getPermType(objType);
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			Transaction t = TransactionManager.getCurrentTransaction();

			PermissionPair perms = null;
			if (objId > 0) {
				perms = permType.getInstancePermissions(objId, checkType, languageId);
			} else {
				perms = permType.getTypePermissions();
			}

			if (logger.isInfoEnabled()) {
				StringBuilder info = new StringBuilder("PermBits for user ");
				info.append(t.getUserId()).append(" for ").append(objType);
				if (objId > 0) {
					info.append(".").append(objId);
				}
				if (nodeId > 0) {
					info.append(" in node ").append(nodeId);
				}
				info.append(" is ").append(perms.toString());
				logger.info(info.toString());
			}

			PermBitsResponse response =  new PermBitsResponse(ObjectTransformer.getString(perms.getGroupPermissions(), null), ObjectTransformer.getString(perms.getRolePermissions(), null));

			if (privilegeMap) {
				response.setPrivilegeMap(ModelBuilder.getPrivileges(permType.type(), objId));
				response.setPermissionsMap(permType.getPermissionMap(objId));
			}

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{perm}/{type}/{id}")
	public PermResponse getObjectPermission(@PathParam("perm") Permission perm, @PathParam("type") String objType, @PathParam("id") int objId,
			@QueryParam("nodeId") @DefaultValue("0") int nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			Transaction t = TransactionManager.getCurrentTransaction();

			// try to parse the given type as number and find the object class
			Class<? extends NodeObject> clazz = null;
			TypePerms permType = null;
			try {
				int intType = TypePerms.normalize(Integer.parseInt(objType));
				clazz = t.getClass(intType);

				if (clazz == null) {
					permType = getPermType(objType);
				}
			} catch (NumberFormatException nfe) {
				permType = getPermType(objType);
				clazz = t.getClass(permType.type());
			}

			trx.success();
			if (clazz != null) {
				// get the instance and check permission
				NodeObject object = t.getObject(clazz, objId);
				ObjectPermission objectPermission = ObjectPermission.get(perm);
				if (objectPermission != null) {
					return new PermResponse(objectPermission.checkObject(object));
				} else {
					return new PermResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid perm " + perm + " given"));
				}
			} else if (permType != null) {
				switch(perm) {
				case view:
					return new PermResponse(t.getPermHandler().canView(permType.type(), objId));
				default:
					return new PermResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid perm " + perm + " given"));
				}
			} else {
				throw new EntityNotFoundException(I18NHelper.get("perm.type.notfound", objType));
			}
		}
	}

	@Override
	@POST
	@Path("/{type}")
	public GenericResponse setPermissions(@PathParam("type") String type, @QueryParam("wait") @DefaultValue("0") long waitMs, SetPermsRequest req)
			throws NodeException {
		TypePerms permType = getPermType(type);
		expectNoInstances(permType);
		try (Trx trx = ContentNodeHelper.trx()) {
			GenericResponse response = Operator.executeRethrowing(I18NHelper.get("assign_user_permissions"), waitMs, () -> {
				return doSetPermissions(permType, req);
			});
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{type}/{id}")
	public GenericResponse setPermissions(@PathParam("type") String objType, @PathParam("id") int objId, @QueryParam("wait") @DefaultValue("0") long waitMs,
			SetPermsRequest req) throws NodeException {
		TypePerms permType = getPermType(objType);
		expectInstances(permType);
		try (Trx trx = ContentNodeHelper.trx()) {
			GenericResponse response = Operator.executeRethrowing(I18NHelper.get("assign_user_permissions"), waitMs, () -> {
				return doSetPermissions(permType, objId, req);
			});
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/list/{type}")
	public GroupsPermBitsResponse list(@PathParam("type") String objType) throws NodeException {
		TypePerms permType = getPermType(objType);
		String logObjectTag = "{" + objType + "}";

		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getPermHandler().checkPermissionBit(
					UserGroup.TYPE_GROUPADMIN,null, PermHandler.PERM_VIEW)) {
				I18nString message = new CNI18nString("groupadmin.nopermission");
				throw new InsufficientPrivilegesException(message.toString(), null, null, UserGroup.TYPE_GROUPADMIN, 0, PermType.read);
			}

			// Check permission to view the type
			if (!t.getPermHandler().checkPermissionBit(permType.type(), null, PermHandler.PERM_VIEW)) {
				I18nString message = new CNI18nString("object.nopermission");
				message.setParameter("0", logObjectTag);
				throw new InsufficientPrivilegesException(message.toString(), null, null, permType.type(), 0, PermType.read);
			}

			// Get the currently authenticated user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			// Add all groups of the user and the subgroups
			List<UserGroup> groups = new Vector<UserGroup>();
			// Fetch all groups the user is in, and their subgroups
			GroupResourceImpl.recursiveAddGroups(groups, user.getUserGroups());
			// Create a map for the rest response
			Map<Integer, String> groupMap = new HashMap<Integer, String>();

			// Go trough all groups
			for (UserGroup group : groups) {
				Integer groupId = ObjectTransformer.getInteger(group.getId(), 0);

				// Get the permissions for this single group on the given object.
				Permissions permissions = PermissionStore.getInstance().getMergedPermissions(
						Arrays.asList(groupId), permType.type());

				groupMap.put(groupId, permissions.toString());
			}

			trx.success();
			// Return the response
			return new GroupsPermBitsResponse(
					null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched folder group permission bits"),
					groupMap);
		}
	}

	@Override
	@GET
	@Path("/list/{type}/{id}")
	public GroupsPermBitsResponse list(@PathParam("type") String objType, @PathParam("id") int objId) throws NodeException {
		TypePerms permType = getPermType(objType);
		String logObjectTag = "{" + objType + "." + objId + "}";

		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getPermHandler().checkPermissionBit(
					UserGroup.TYPE_GROUPADMIN,null, PermHandler.PERM_VIEW)) {
				I18nString message = new CNI18nString("groupadmin.nopermission");
				throw new InsufficientPrivilegesException(message.toString(), null, null, UserGroup.TYPE_GROUPADMIN, 0, PermType.read);
			}

			// Get the object type
			Class<? extends NodeObject> objClass = t.getClass(permType.type());
			if (objClass == null) {
				throw new EntityNotFoundException("Could not find object type " + objType);
			}

			if (objClass.equals(Node.class)) {
				objClass = Folder.class;
			}

			NodeObject object = t.getObject(objClass, objId);
			if (object == null) {
				throw new EntityNotFoundException("Could not find object " + logObjectTag);
			}

			// Check permission to view the object
			if (!PermHandler.ObjectPermission.view.checkObject(object)) {
				I18nString message = new CNI18nString("object.nopermission");
				message.setParameter("0", logObjectTag);
				throw new InsufficientPrivilegesException(message.toString(), object, PermType.read);
			}

			// Get the currently authenticated user
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			// Add all groups of the user and the subgroups
			List<UserGroup> groups = new Vector<UserGroup>();
			// Fetch all groups the user is in, and their subgroups
			GroupResourceImpl.recursiveAddGroups(groups, user.getUserGroups());
			// Create a map for the rest response
			Map<Integer, String> groupMap = new HashMap<Integer, String>();

			// Go trough all groups
			for (UserGroup group : groups) {
				Integer groupId = ObjectTransformer.getInteger(group.getId(), 0);

				// Get the permissions for this single group on the given object.
				PermissionPair permissionPair = PermissionStore.getInstance().getMergedPermissions(
						Arrays.asList(groupId), permType.type(), objId, -1, -1);

				groupMap.put(groupId, permissionPair.getGroupPermissions().toString());
			}

			trx.success();
			// Return the response
			return new GroupsPermBitsResponse(
					null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched folder group permission bits"),
					groupMap);
		}
	}
}
