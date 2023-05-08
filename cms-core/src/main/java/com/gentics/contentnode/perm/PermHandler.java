/*
 * @author norbert
 * @date 06.04.2009
 * @version $Id: PermHandler.java,v 1.5.4.1 2011-02-08 14:14:41 norbert Exp $
 */
package com.gentics.contentnode.perm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang3.tuple.Triple;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RefreshPermHandler;
import com.gentics.contentnode.factory.RefreshRoleHandler;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.perm.CreatePermType;
import com.gentics.contentnode.factory.perm.DeletePermType;
import com.gentics.contentnode.factory.perm.EditPermType;
import com.gentics.contentnode.factory.perm.PublishPermType;
import com.gentics.contentnode.factory.perm.ViewPermType;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInFolder;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.PublishableNodeObjectInFolder;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserGroup.ReductionType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Permission handler class. Provides static and non-static methods to check
 * content.node permissions
 */
public class PermHandler {
	public final static int TYPE_ADMIN = 1;
	public final static int TYPE_ACTIONLOG = 8;
	public final static int TYPE_ERRORLOG = 16;
	public final static int TYPE_CONTENTNODE = 10000;
	public final static int TYPE_CONADMIN = 10010;
	public final static int TYPE_MAINTENCANCE = 1042;
	public final static int TYPE_ACTIVITI = 110;
	public final static int TYPE_DEVTOOLS_PACKAGES = 12000;
	public final static int TYPE_SCHEDULER_ADMIN = 36;
	public final static int TYPE_TASK = 37;
	public final static int TYPE_TASK_TEMPLATE = 38;
	public final static int TYPE_JOB = 39;
	public final static int TYPE_AUTOUPDATE = 141;

	public final static int TYPE_CUSTOM_TOOLS = 90000;
	public final static int TYPE_CUSTOM_TOOL = 90001;

	public final static int TYPE_USERSNAP = 90100;

	public final static int TYPE_OBJPROP_MAINTENANCE = 140;

	public final static int TYPE_CONTENT_MAINTENANCE = 10032;

	public final static int TYPE_CONTENTSTAGING = 10401;

	public final static int PERM_PAGE_VIEW = 11;
	public final static int PERM_PAGE_CREATE = 12;
	public final static int PERM_PAGE_UPDATE = 13;
	public final static int PERM_PAGE_DELETE = 14;
	public final static int PERM_PAGE_PUBLISH = 19;
	public static final int PERM_FOLDER_DELETE = 10;
	public static final int PERM_FOLDER_UPDATE = 9;
	public static final int PERM_FOLDER_CREATE = 8;
	public static final int PERM_TEMPLATE_VIEW = 15;
	public static final int PERM_TEMPLATE_CREATE = 16;
	public static final int PERM_TEMPLATE_UPDATE = 17;
	public static final int PERM_TEMPLATE_DELETE = 18;
	public static final int PERM_TEMPLATE_LINK = 21;

	public static final int ROLE_VIEW = 10;
	public static final int ROLE_CREATE = 11;
	public static final int ROLE_UPDATE = 12;
	public static final int ROLE_DELETE = 13;
	public static final int ROLE_PUBLISH = 14;
	public static final int ROLE_TRANSLATE = 15;

	public static final int PERM_NODE_CONSTRUCT_MODIFY = 20;

	public static final int PERM_VIEW = 0;

	public static final int PERM_CHANGE_PERM = 1;

	public static final int PERM_NODE_CREATE = 8;

	public static final int PERM_USER_VIEW = PERM_VIEW;
	public static final int PERM_USER_CREATE = 8;
	public static final int PERM_USER_UPDATE = 9;
	public static final int PERM_USER_DELETE = 10;

	public static final int PERM_CONTENTPACKAGE_VIEW = PERM_VIEW;
	public static final int PERM_CONTENTPACKAGE_CREATE = 8;
	public static final int PERM_CONTENTPACKAGE_UPDATE = 2;
	public static final int PERM_CONTENTPACKAGE_DELETE = 3;
	public static final int PERM_CONTENTPACKAGE_MODIFYCONTENT = 11;

	public static final int PERM_GROUP_CREATE = 8;
	public static final int PERM_GROUP_UPDATE = 9;
	public static final int PERM_GROUP_DELETE = 10;
	public static final int PERM_GROUP_USERADD = 11;
	public static final int PERM_GROUP_USERUPDATE = 12;

	public static final int PERM_CONTENTREPOSITORY_CREATE = 8;

	public static final int PERM_CONTENTREPOSITORY_UPDATE = 2;

	public static final int PERM_CONTENTREPOSITORY_DELETE = 3;

	public static final int PERM_OBJPROP_UPDATE = 2;

	public static final int PERM_OBJPROPS_UPDATE = 2;

	public static final int PERM_CHANNEL_SYNC = 27;

	public static final int PERM_INHERITANCE = 29;

	public static final int PERM_CONSTRUCT_UPDATE = 2;

	public static final int PERM_CHANGE_GROUP_PERM = 13;

	public static final int PERM_NODE_WASTEBIN = 28;

	public static final int PERM_ACTITIVIT_UPDATE = 2;

	public static final int PERM_SCHEDULER_SUSPEND = 2;

	public static final int PERM_CONTENT_MAINTENANCE_UPDATE = 2;

	public static final int PERM_SCHEDULER_TASK_VIEW = 10;

	public static final int PERM_SCHEDULER_TASK_UPDATE = 11;

	public static final int PERM_SCHEDULER_SCHEDULE_UPDATE = 11;

	/**
	 * Empty permission bits
	 */
	public final static String EMPTY_PERM = StringUtils.repeat("0", 32);

	/**
	 * Full permission bits
	 */
	public final static String FULL_PERM = StringUtils.repeat("1", 32);

	/**
	 * User Id of the user, if the PermHandler was initialized for a user (-1 if not initialized for a user)
	 */
	protected int userId = -1;

	/**
	 * Map of group Ids of the user per node
	 */
	protected Map<Integer, List<Integer>> userGroupIds = new HashMap<Integer, List<Integer>>();

	/**
	 * Group Id of the group, if the PermHandler was initialized for a group (-1 if not initialized for a group)
	 */
	protected int groupId = -1;

	/**
	 * Resolve the rest permission enum for the given type.
	 *
	 * @param restPerm
	 * @param type
	 * @return Resolved permission bit offset or -1 if the correct permission
	 *         could not be identified
	 */
	public static int resolvePermission(com.gentics.contentnode.rest.model.request.Permission restPerm, int type) {
		switch (restPerm) {
		case create:
			switch (type) {
			case Node.TYPE_NODE:
				return PERM_NODE_CREATE;
			case Folder.TYPE_FOLDER:
				return PERM_FOLDER_CREATE;
			case Page.TYPE_PAGE:
			case ContentFile.TYPE_FILE:
			case ContentFile.TYPE_IMAGE:
				return PERM_PAGE_CREATE;
			case Template.TYPE_TEMPLATE:
				return PERM_TEMPLATE_CREATE;
			case SystemUser.TYPE_SYSTEMUSER:
				return PERM_USER_CREATE;
			}
		case view:
			switch (type) {
			case Page.TYPE_PAGE:
			case ContentFile.TYPE_FILE:
			case ContentFile.TYPE_IMAGE:
				return PERM_PAGE_VIEW;
			case Template.TYPE_TEMPLATE:
				return PERM_TEMPLATE_VIEW;
			default:
				return PERM_VIEW;
			}
		case edit:
			switch (type) {
			case Page.TYPE_PAGE:
			case ContentFile.TYPE_FILE:
			case ContentFile.TYPE_IMAGE:
				return PERM_PAGE_UPDATE;
			case Folder.TYPE_FOLDER:
				return PERM_FOLDER_UPDATE;
			case Template.TYPE_TEMPLATE:
				return PERM_TEMPLATE_UPDATE;
			case SystemUser.TYPE_SYSTEMUSER:
				return PERM_USER_UPDATE;
			}
		case delete:
			switch (type) {
			case Page.TYPE_PAGE:
			case ContentFile.TYPE_FILE:
			case ContentFile.TYPE_IMAGE:
				return PERM_PAGE_DELETE;
			case Folder.TYPE_FOLDER:
				return PERM_FOLDER_DELETE;
			case Template.TYPE_TEMPLATE:
				return PERM_TEMPLATE_DELETE;
			case SystemUser.TYPE_SYSTEMUSER:
				return PERM_USER_DELETE;
			}
		case publish:
			switch (type) {
			case Page.TYPE_PAGE:
				return PERM_PAGE_PUBLISH;
			}
		}
		return -1;
	}

	/**
	 * Initialize the instance to resolve the permissions of the given user
	 * @param userId user id
	 * @throws NodeException
	 */
	public void initForUser(int userId) throws NodeException {
		this.userId = userId;
		userGroupIds.clear();
	}

	/**
	 * Initialize the instance to resolve the permissions of the given user
	 * @param conn database connection
	 * @param userId user id
	 * @throws NodeException
	 * @deprecated
	 */
	public void initForUser(Connection conn, int userId) throws NodeException {
		initForUser(userId);
	}

	/**
	 * Initialize the instance to resolve the permissions of the given group
	 * @param groupId group id
	 * @throws NodeException
	 */
	public void initForGroup(int groupId) throws NodeException {
		this.groupId = groupId;
		this.userGroupIds.clear();
	}

	/**
	 * Initialize the instance to resolve the permissions of the given group
	 * @param conn database connection
	 * @param groupId group id
	 * @throws NodeException
	 * @deprecated
	 */
	public void initForGroup(Connection conn, int groupId) throws NodeException {
        // TODO remove this
	}

	/**
	 * Get the group ids for the user
	 * @param nodeId node id, if the groups shall be restricted to a node id, 0 for not restricting
	 * @return list of group ids
	 * @throws NodeException
	 */
	public List<Integer> getGroupIds(final int nodeId) throws NodeException {
		if (userId > 1) {
			if (!userGroupIds.containsKey(nodeId)) {
				final List<Integer> groupIds = new ArrayList<Integer>();
				if (nodeId == 0) {
					DBUtils.executeStatement("select usergroup_id id from user_group where user_id = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, userId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								groupIds.add(rs.getInt("id"));
							}
						}
					});
				} else {
					DBUtils.executeStatement("select user_group.usergroup_id id from user_group left join user_group_node on user_group.id = user_group_node.user_group_id where user_group.user_id = ? and (user_group_node.node_id = ? or user_group_node.node_id is null)", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, userId);
							stmt.setInt(2, nodeId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								groupIds.add(rs.getInt("id"));
							}
						}
					});
				}
				userGroupIds.put(nodeId, groupIds);
			}
			return userGroupIds.get(nodeId);
		} else if (groupId > 0) {
			return Arrays.asList(groupId);
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Get the user permissions on the given object
	 * @param objType object type
	 * @param objId object id
	 * @param checkType type, for which the permissions shall be checked (for getting the correct role permissions)
	 * @param checkLangId language id for which the permissions shall be checked (for getting the correct role permissions)
	 * @return permissions
	 * @throws NodeException
	 */
	public PermissionPair getPermissions(Object objType, Integer objId, int checkType, int checkLangId) throws NodeException {
		if (userId == 0 || userId == 1) {
			return new PermissionPair(Permissions.get(FULL_PERM));
		} else if (userId < 0 && groupId < 0) {
			return new PermissionPair(Permissions.get(EMPTY_PERM));
		} else {
			int nodeId = 0;
			int oType = ObjectTransformer.getInt(objType, -1);
			if (objId == null) {
				// if no object id is given, we check for the type permissions
				return new PermissionPair(PermissionStore.getInstance().getMergedPermissions(getGroupIds(nodeId), oType));
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = null;
			// collect the group IDs for checking permissions here
			Set<Integer> groupIds = new HashSet<>();

			// if the type is "Folder" or "Node", we get the folder.
			// if the folde is a root folder, we set the type to "Node" (or "Channel" for channel root folders)
			switch (oType) {
			case Folder.TYPE_FOLDER:
			case Node.TYPE_NODE:
				folder = t.getObject(Folder.class, objId, -1, false);
				if (folder != null) {
					if (folder.isRoot()) {
						objType = Node.TYPE_NODE;
						oType = Node.TYPE_NODE;
					}

					if (t.isCheckAnyChannel()) {
						// check permissions for any channel
						Node node = folder.getOwningNode();
						groupIds.addAll(getGroupIds(node.getId()));
						for (Node channel : node.getAllChannels()) {
							groupIds.addAll(getGroupIds(channel.getId()));
						}
					} else {
						// we will restrict to the node of the folder. If a
						// channel is set, it will be considered automatically
						nodeId = ObjectTransformer.getInt(t.getChannelId(), 0);
						if (nodeId == 0) {
							Node node = folder.getChannel();
							if (node == null) {
								node = folder.getNode();
							}
							nodeId = ObjectTransformer.getInt(node.getId(), 0);
						}
						// only check in the groups of the current node
						groupIds.addAll(getGroupIds(nodeId));
					}

					// always check permissions on the master object
					if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
						folder = folder.getMaster();
						objId = folder.getId();
					}
				}
				break;
			default:
				groupIds.addAll(getGroupIds(nodeId));
				break;
			}

			return PermissionStore.getInstance().getMergedPermissions(new ArrayList<>(groupIds), oType, ObjectTransformer.getInt(objId, 0), checkType, checkLangId);
		}
	}

	/**
	 * Check whether the given permission bit is set for the given object.
	 * When no obj id is given, check whether at least one entry grants the permission
	 * @param objType Type to which permissions are assigned (for example folder and <strong>not</strong> the page)
	 * @param objId Object to which permissions are assigned (for example the folder and <strong>not</strong> the page)
	 * @param bit permission bit to check
	 * @return true when the permission bit is set, false if not
	 */
	public boolean checkPermissionBit(Integer objType, Integer objId, int bit) {
		return checkPermissionBit(objType, objId, bit, -1, -1, -1);
	}

	/**
	 * Check whether the given permission bit is set for the given object.
	 * When no obj id is given, check whether at least one entry grants the permission
	 * @param objType Type to which permissions are assigned (for example folder and <strong>not</strong> the page)
	 * @param objId Object to which permissions are assigned (for example the folder and <strong>not</strong> the page)
	 * @param bit permission bit to check
	 * @param checkType type for which the permissions shall be checked
	 * @param checkLangId language for which the permission shall be checked
	 * @param roleBit role bit
	 * @return true when the permission bit is set, false if not
	 */
	public boolean checkPermissionBit(Integer objType, Integer objId, int bit, int checkType, int checkLangId, int roleBit) {
		try {
			PermissionPair perms = getPermissions(objType, objId, checkType, checkLangId);
			return perms.checkPermissionBits(bit, roleBit);
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while checking permission for " + objType + "." + objId, e);
			return false;
		}
	}

	/**
	 * Check if all the given permission bits are set on the given object.
	 * If no bits are given, the check succeeds
	 * @param objType object type
	 * @param objId object id
	 * @param permBits list of permission bits to check
	 * @return true if all permission bits are set, false if not
	 */
	protected boolean checkPermissionBits(Integer objType, Integer objId, Integer... permBits) {
		for (Integer bit : permBits) {
			if (!checkPermissionBit(objType, objId, bit)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the given user has the permission (identified by the bit)
	 * on the requested object
	 * @param conn database connection
	 * @param userId user id
	 * @param objType object type
	 * @param objId object id
	 * @param bit permission bit
	 * @return true when the user has the permission, false if not
	 * @throws NodeException
	 * @deprecated use an initialized PermHandler instance instead
	 */
	public static boolean perm(Connection conn, int userId, int objType, Object objId, int bit) throws NodeException {
		// allow everything for the system user
		if (userId == 1) {
			return true;
		}
		StringBuffer sql = new StringBuffer(
				"SELECT perm.perm FROM user_group,perm WHERE perm.usergroup_id = user_group.usergroup_id AND user_group.user_id = ? AND perm.o_type = ?");
		boolean searchObjId = false;

		if (ObjectTransformer.getInt(objId, -1) > -1) {
			sql.append(" AND perm.o_id = ?");
			searchObjId = true;
		}

		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = conn.prepareStatement(sql.toString());
			pst.setInt(1, userId);
			pst.setInt(2, objType);
			if (searchObjId) {
				pst.setObject(3, objId);
			}

			res = pst.executeQuery();

			while (res.next()) {
				String perm = res.getString("perm");

				if (perm.matches("^[0-1]*$")) {
					int c = perm.length();

					if (bit <= c) {
						if ("1".equals(perm.substring(bit, bit + 1))) {
							return true;
						}
					}
				}
			}

			return false;
		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			DB.close(res);
			DB.close(pst);
		}
	}

	/**
	 * Get group IDs that have the given permission bit on the given type
	 * @param objType object type
	 * @param bit permission bit
	 * @return list of group IDs
	 * @throws NodeException
	 */
	public static List<Integer> getGroupsWithPermissionBit(int objType, int bit) throws NodeException {
		return DBUtils.select("SELECT distinct usergroup_id FROM perm WHERE o_type = ? AND SUBSTR(perm, ?, 1) = '1'", pst -> {
			pst.setInt(1, objType);
			pst.setInt(2, bit + 1);
		}, DBUtils.allInt("usergroup_id"));
	}

	/**
	 * Get the group id's that have the given permission bit on the given object
	 * @param conn db connection
	 * @param objType object type
	 * @param objId object id
	 * @param bit permission bit
	 * @return List of group id's
	 * @throws NodeException
	 */
	public static List<Integer> getGroupsWithPermissionBit(int objType, Object objId,
			int bit) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("select distinct usergroup_id from perm where o_type = ? and o_id = ? and substr(perm, ?, 1) = '1'");
			pst.setInt(1, objType);
			pst.setObject(2, objId);
			pst.setInt(3, bit + 1);
			res = pst.executeQuery();
			List<Integer> groupIds = new Vector<Integer>();

			while (res.next()) {
				groupIds.add(res.getInt("usergroup_id"));
			}
			return groupIds;
		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Get all groups, that have the permission on the given object
	 * @param object object
	 * @param type permission type
	 * @return list of group IDs
	 * @throws NodeException
	 */
	public static List<Integer> getGroupsWithPermission(NodeObject object, PermType type) throws NodeException {
		return getGroupsWithPermissionBit(object.getTType(), object.getId(), type.getBit());
	}

	/**
	 * Duplicate the permissions from the given source to the target
	 * @param sourceType source type
	 * @param sourceId source id
	 * @param targetType target type
	 * @param targetId target id
	 * @throws NodeException
	 */
	public static void duplicatePermissions(final int sourceType, final int sourceId, final int targetType, final int targetId) throws NodeException {
		// first read all already existing permissions
		final List<Integer> existingGroups = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT usergroup_id FROM perm WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, targetType);
				stmt.setInt(2, targetId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					existingGroups.add(rs.getInt("usergroup_id"));
				}
			}
		});

		// now get all permission settings from the source object
		final Map<Integer, String> sourcePerms = new HashMap<Integer, String>();

		DBUtils.executeStatement("SELECT usergroup_id, perm FROM perm WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, sourceType);
				stmt.setInt(2, sourceId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					sourcePerms.put(rs.getInt("usergroup_id"), rs.getString("perm"));
				}
			}
		});

		// finally insert/update the permissions
		for (Map.Entry<Integer, String> permEntry : sourcePerms.entrySet()) {
			int usergroupId = permEntry.getKey();
			String perm = permEntry.getValue();

			if (existingGroups.contains(usergroupId)) {
				// do an update
				DBUtils.executeUpdate("UPDATE perm SET perm = ? WHERE o_type = ? AND o_id = ? AND usergroup_id = ?",
						new Object[] { perm, targetType, targetId, usergroupId});
			} else {
				// do an insert
				DBUtils.executeUpdate("INSERT INTO perm (o_type, o_id, usergroup_id, perm) VALUES (?, ?, ?, ?)",
						new Object[] { targetType, targetId, usergroupId, perm});
			}
		}

		//duplicate Roles
		final Set<Integer> roleUserGroupIDs = new HashSet<Integer>();

		//get roles from source
		DBUtils.executeStatement(
				"SELECT role_usergroup.id FROM role_usergroup INNER JOIN role_usergroup_assignment "
				+ "ON role_usergroup.id = role_usergroup_assignment.role_usergroup_id "
				+ "WHERE role_usergroup_assignment.obj_type = ? AND role_usergroup_assignment.obj_id = ?",
				new SQLExecutor() {

					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, sourceType);
						stmt.setInt(2, sourceId);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							roleUserGroupIDs.add(rs.getInt("id"));
						}
					}
				});

		//delete existing role assignments
		DBUtils.executeUpdate("DELETE FROM role_usergroup_assignment WHERE obj_id = ? AND obj_type = ?",
				new Object[] {targetId, targetType });

		//set roles for target
		for (Integer groupRoleId : roleUserGroupIDs) {
			DBUtils.executeInsert("INSERT INTO role_usergroup_assignment (role_usergroup_id, obj_id, obj_type) values (?, ?, ?)",
						new Object[] {groupRoleId, targetId, targetType });
		}

		// let the perm handler refresh
		Transaction t = TransactionManager.getCurrentTransaction();

		// refresh the PermissionStore for the given object
		t.addTransactional(new RefreshPermHandler(targetType, targetId));
	}

	/**
	 * Set the default permissions to the given object for the given groups
	 * @param type type of the object
	 * @param id id of the object
	 * @param groups groups for which the perms shall be set
	 * @throws NodeException
	 */
	public static void setPermissions(int type, int id, List<UserGroup> groups) throws NodeException {
		setPermissions(type, id, groups, "11000000111111111111111111111111");
	}

	/**
	 * Set the permission for the tree item (actually ALL tree items) of a given type
	 * @param type type
	 * @param groups groups
	 * @param perm perm to set
	 * @throws NodeException
	 */
	public static void setPermissions(int type, List<UserGroup> groups, String perm) throws NodeException {
		Set<Integer> ids = DBUtils.select("SELECT id FROM tree WHERE type_id = ?", ps -> {
			ps.setInt(1, type);
		}, DBUtils.IDS);

		if (ids.isEmpty()) {
			PermHandler.setPermissions(type, 0, groups, perm);
		} else {
			for (int id: ids) {
				PermHandler.setPermissions(type, id, groups, perm);
			}
		}
	}

	/**
	 * Set the permission
	 * @param type type of the object
	 * @param id id of the object
	 * @param groups groups for which the perms shall be set
	 * @param perm perm bits (may be a pattern to change existing perms)
	 * @throws NodeException
	 */
	public static void setPermissions(int type, int id, List<UserGroup> groups, String perm) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int updateCount = 0;

		// don't set permissions with not bits set
		if (EMPTY_PERM.equals(perm)) {
			// delete the permissions
			for (UserGroup group : groups) {
				updateCount += DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ? AND o_type = ? AND o_id = ?", new Object[] {group.getId(), type, id});

				// logcmd
				ActionLogger.logCmd(ActionLogger.PERM, type, id, group.getId(), "PermHandler.setPermissions()");

			}
		} else {
			for (UserGroup group : groups) {
				String existing = DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ? AND o_id = ?", st -> {
					st.setInt(1, group.getId());
					st.setInt(2, type);
					st.setInt(3, id);
				}, DBUtils.firstString("perm"));

				if (existing == null) {
					Permissions newPerm = Permissions.change(null, perm);
					if (newPerm != null) {
						// insert
						DBUtils.executeInsert("INSERT INTO perm (usergroup_id, o_type, o_id, perm) VALUES (?, ?, ?, ?)",
								new Object[] { group.getId(), type, id, newPerm.toString() });
						updateCount++;
					}
				} else {
					Permissions oldPerm = Permissions.get(existing);
					Permissions newPerm = Permissions.change(oldPerm, perm);
					if (newPerm == null) {
						// delete
						updateCount += DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ? AND o_type = ? AND o_id = ?", new Object[] { group.getId(), type, id });
					} else if (!Objects.equals(oldPerm, newPerm)) {
						// update
						updateCount += DBUtils.executeUpdate("UPDATE perm SET perm = ? WHERE usergroup_id = ? AND o_type = ? AND o_id = ?",
								new Object[] { newPerm.toString(), group.getId(), type, id });
					}
				}

				// logcmd
				ActionLogger.logCmd(ActionLogger.PERM, type, id, group.getId(), "PermHandler.setPermissions()");
			}
		}

		// refresh the PermissionStore for the given object
		if (updateCount > 0) {
			// refresh perm handler. It is important that this is done before the event is triggered, because event handlers (such as Indexer) might
			// already need to updated permissions
			t.addTransactional(new RefreshPermHandler(type, id));

			// trigger event
			if (type == Folder.TYPE_FOLDER || type == Node.TYPE_NODE) {
				t.addTransactional(new TransactionalTriggerEvent(Folder.class, id, new String[] { "permissions" }, Events.UPDATE));
			}
		}
	}

	/**
	 * Set the given permissions on the role
	 * @param roleId role ID
	 * @param perms permissions
	 * @throws NodeException
	 */
	public static void setRolePermissions(int roleId, RolePermissions perms) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Set<Integer> langIds = DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS);

		// get current permissions
		RolePermissions current = PermissionStore.getInstance().getRolePerm(roleId);

		// determine diff and save updates
		boolean changed = false;
		if (!Objects.equals(perms.getFilePerm(), current.getFilePerm())) {
			saveRolePermissions(roleId, File.TYPE_FILE, null, perms.getFilePerm());
			changed = true;
		}
		if (!Objects.equals(perms.getPagePerm(), current.getPagePerm())) {
			saveRolePermissions(roleId, Page.TYPE_PAGE, null, perms.getPagePerm());
			changed = true;
		}
		for (int langId : langIds) {
			if (!Objects.equals(perms.getPagePerm(langId), current.getPagePerm(langId))) {
				saveRolePermissions(roleId, Page.TYPE_PAGE, langId, perms.getPagePerm(langId));
				changed = true;
			}
		}

		if (changed) {
			ActionLogger.logCmd(ActionLogger.EDIT, Role.TYPE_ROLE, roleId, 0, "Role.permissions");
			t.addTransactional(new RefreshRoleHandler(roleId));

			t.addTransactional(
					new TransactionalTriggerEvent(Role.class, roleId, new String[] { "permissions" }, Events.UPDATE));
		}
	}

	/**
	 * Save the role permissions
	 * @param roleId role ID
	 * @param objType object type
	 * @param langId optional language ID
	 * @param perm Permissions String (may be null)
	 * @throws NodeException
	 */
	protected static void saveRolePermissions(int roleId, int objType, Integer langId, Permissions perm) throws NodeException {
		/*
		 * Role permissions are stored in tables roleperm and roleperm_obj:
		 * 1. roleperm contains the bitmasks for the permissions set on
		 *  * pages (in general)
		 *  * files (in general)
		 *  * pages in every possible language
		 * 2. empty bitmasks (everything set to 0) are NOT stored in roleperm
		 * 3. every entry in roleperm has one corresponding entry in roleperm_obj, that specifies the obj_type (page or file) to which the roleperm entry belongs
		 * 4. every entry in roleperm might have an additional corresponding entry in roleperm_obj, that specifies the language, the entry belongs to
		 */

		// select matching roleperm entries
		String sql = "SELECT\n" +
				"	roleperm.id\n" +
				"FROM roleperm\n" +
				"	INNER JOIN roleperm_obj AS obj_type\n" +
				"		ON obj_type.roleperm_id = roleperm.id\n" +
				"			AND obj_type.obj_id IS NULL\n" +
				"			AND obj_type.obj_type = ?\n" +
				"	LEFT JOIN roleperm_obj AS obj_lang\n" +
				"		ON obj_lang.roleperm_id = roleperm.id\n" +
				"			AND obj_lang.obj_type = ?\n" +
				"WHERE roleperm.role_id = ?\n";

		if (langId == null) {
			sql += "AND obj_lang.obj_id IS NULL";
		} else {
			sql += "AND obj_lang.obj_id = ?";
		}

		List<Integer> rolePermIds = new ArrayList<>(DBUtils.select(sql, ps -> {
			ps.setInt(1, objType);
			ps.setInt(2, ContentLanguage.TYPE_CONTENTGROUP);
			ps.setInt(3, roleId);
			if (langId != null) {
				ps.setInt(4, langId);
			}
		}, DBUtils.IDS));

		// if entries found: delete them (including the corresponding roleperm_obj entries)
		if (!rolePermIds.isEmpty()) {
			DBUtils.selectAndDelete("roleperm_obj", "SELECT id FROM roleperm_obj WHERE roleperm_id IN", rolePermIds);
			DBUtils.executeMassStatement("DELETE FROM roleperm WHERE id IN", rolePermIds, 1, null);
		}

		// if perm was not empty, store the new perms
		if (perm != null) {
			List<Integer> newIds = DBUtils.executeInsert("INSERT INTO roleperm (role_id, perm) VALUES (?, ?)", new Object[] { roleId, perm.toString() });
			if (newIds.size() > 0) {
				int rolePermId = newIds.get(0);
				DBUtils.executeInsert("INSERT INTO roleperm_obj (roleperm_id, obj_type) VALUES (?, ?)", new Object[] {rolePermId, objType});
				if (langId != null) {
					DBUtils.executeInsert("INSERT INTO roleperm_obj (roleperm_id, obj_type, obj_id) VALUES (?, ?, ?)",
							new Object[] { rolePermId, ContentLanguage.TYPE_CONTENTGROUP, langId });
				}
			}
		}
	}

	/**
	 * A pair class with equals() and hashCode() for group IDs and role IDs, to be used in setRoles below.
	 * @author escitalopram
	 */
	private static class GroupRolePair {
		private final int groupId;
		private final int roleId;

		public GroupRolePair(int groupId, int roleId) {
			this.groupId = groupId;
			this.roleId = roleId;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GroupRolePair) {
				GroupRolePair other = (GroupRolePair) obj;
				return groupId == other.groupId
						&& roleId == other.roleId;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return 1031 * roleId + 1033 * groupId;
		}
	}

	/**
	 * Get active roles for the group on an object
	 * @param objType object type
	 * @param objId object ID
	 * @param group group
	 * @return set of role IDs
	 * @throws NodeException
	 */
	public static Set<Integer> getRoles(final int objType, int objId, UserGroup group) throws NodeException {
		return DBUtils.select(
				"SELECT ru.role_id id FROM role_usergroup ru, role_usergroup_assignment rua WHERE ru.id = rua.role_usergroup_id AND rua.obj_type = ? AND rua.obj_id = ? AND ru.usergroup_id = ?",
				st -> {
					st.setInt(1, objType);
					st.setInt(2, objId);
					st.setInt(3, group.getId());
				}, DBUtils.IDS);
	}

	/**
	 * Set active roles on objects
	 * @param obj_type	type of the object
	 * @param obj_id	id of the object
	 * @param groups	groups for which the role should be set. Must not be null
	 * @param roleIds	the roles that should be set. Must not be null
	 * @param usergroupRoleUpdateDone whether usergroup_role has already been updated. set true to optimize folder hierarchy recursive requests
	 */
	public static void setRoles(final int obj_type, final int obj_id, final List<UserGroup> groups, final Set<Integer> roleIds) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int updateCount = 0;

		if (groups.isEmpty()) {
			return;
		}

		// ensure there's a role_usergroup entry for each required combination
		final Set<GroupRolePair> foundGroupRoleCombos = new HashSet<GroupRolePair>();

		final List<Integer> removeRUEntries = new ArrayList<Integer>();
		final List<Integer> addRUEntries = new ArrayList<Integer>();

		final String assignmentIdsSQL = "SELECT id, role_id, usergroup_id from role_usergroup where usergroup_id in "
				+ DBUtils.makeSqlPlaceHolders(groups.size(), "");
		DBUtils.executeStatement(assignmentIdsSQL, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				for (int i = 0; i < groups.size(); ++i) {
					stmt.setInt(i + 1, groups.get(i).getId());
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int ruid = rs.getInt(1);
					int roleId = rs.getInt(2);
					int groupId = rs.getInt(3);

					if (roleIds.contains(roleId)) {
						foundGroupRoleCombos.add(new GroupRolePair(groupId, roleId));
						addRUEntries.add(ruid);
					} else {
						removeRUEntries.add(ruid);
					}
				}
			}
		});

		final String addRUEntrySQL = "insert into role_usergroup (usergroup_id, role_id) values (?, ?)";
		for (Integer roleId : roleIds) {
			for (UserGroup group : groups) {
				GroupRolePair needle = new GroupRolePair(group.getId(), roleId);
				if (foundGroupRoleCombos.contains(needle)) {
					continue;
				}
				List<Integer> insertIds = DBUtils.executeInsert(addRUEntrySQL, new Object[] { group.getId(), roleId });
				addRUEntries.addAll(insertIds);
				updateCount += insertIds.size();
			}
		}

		if (!removeRUEntries.isEmpty()) {
			List<Object> whereParams = new ArrayList<>();
			whereParams.add(obj_type);
			whereParams.add(obj_id);
			whereParams.addAll(removeRUEntries);
			updateCount += DBUtils.deleteWithPK("role_usergroup_assignment", "id",
					"obj_type = ? and obj_id = ? and role_usergroup_id in " + DBUtils.makeSqlPlaceHolders(removeRUEntries.size(), ""),
					(Object[]) whereParams.toArray(new Object[whereParams.size()]));
		}

		final String queryAssignmentSQL = "select role_usergroup_id from role_usergroup_assignment where obj_type = ? and obj_id = ? and role_usergroup_id in "
				+ DBUtils.makeSqlPlaceHolders(addRUEntries.size(), "");
		final Set<Integer> remainingAssignments = new HashSet<Integer>(addRUEntries);

		if (!addRUEntries.isEmpty()) {
			DBUtils.executeStatement(queryAssignmentSQL, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, obj_type);
					stmt.setInt(2, obj_id);
					for (int i = 0; i < addRUEntries.size(); ++i) {
						stmt.setInt(3 + i, addRUEntries.get(i));
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int ruid = rs.getInt(1);
						remainingAssignments.remove(ruid);
					}
				}
			});
		}

		final String addAssignmentSQL = "insert into role_usergroup_assignment (role_usergroup_id, obj_type, obj_id) values (?, ?, ?)";
		for (Integer ruid : remainingAssignments) {
			DBUtils.executeInsert(addAssignmentSQL, new Object[] { ruid, obj_type, obj_id });
			updateCount++;
		}

		if (updateCount > 0) {
			t.addTransactional(new RefreshPermHandler(obj_type, obj_id));

			// trigger event
			if (obj_type == Folder.TYPE_FOLDER || obj_type == Node.TYPE_NODE) {
				t.addTransactional(new TransactionalTriggerEvent(Folder.class, obj_id, new String[] { "permissions" }, Events.UPDATE));
			}
		}
	}

	/**
	 * Check whether the perm handler allows viewing the given object
	 * @param objType object type
	 * @param objId object ID
	 * @return true for view permission
	 * @throws NodeException
	 */
	public boolean canView(int objType, int objId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Class<? extends NodeObject> clazz = t.getClass(objType);
		if (clazz != null) {
			NodeObject object = t.getObject(clazz, objId);
			return canView(object);
		} else {
			switch (objType) {
			case TYPE_CUSTOM_TOOL:
				return checkPermissionBit(TYPE_CUSTOM_TOOL, objId, PERM_VIEW)
						|| (checkPermissionBit(TYPE_CUSTOM_TOOLS, null, PERM_VIEW) && checkPermissionBit(TYPE_CUSTOM_TOOLS, null, PERM_CHANGE_PERM));
			default:
				return false;
			}
		}
	}

	/**
	 * Check whether the perm handler allows viewing the given object.
	 * This method is currently implemented for Constructs, ConstructCategories, ObjectTagDef, Folders, Pages, Images, Files and Templates
	 * @param object an object
	 * @return true if the perm handler allows viewing the object, false if not
	 * @throws NodeException
	 */
	public boolean canView(NodeObject object) throws NodeException {
		// do object type specific tests now
		if (object instanceof NodeObjectInFolder) {
			return ((NodeObjectInFolder) object).canView(this);
		} else if (object instanceof Folder) {
			return checkPermissionBit(Folder.TYPE_FOLDER, object.getId(), PERM_VIEW);
		} else if (object instanceof Node) {
			return checkPermissionBit(Folder.TYPE_FOLDER, ((Node) object).getFolder().getId(), PERM_VIEW);
		} else if (object instanceof Page || object instanceof File) {
			NodeObject folder = null;
			try (ChannelTrx trx = new ChannelTrx()) {
				folder = object.getParentObject();
			}

			if (folder == null) {
				return false;
			} else {
				if (!canView(folder)) {
					return false;
				}
				int languageId = -1;

				if (object instanceof Page) {
					languageId = ObjectTransformer.getInt(((Page) object).getLanguageId(), -1);
				}

				return checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), PERM_PAGE_VIEW, ObjectTransformer.getInt(object.getTType(), -1), languageId, ROLE_VIEW);
			}
		} else if (object instanceof Template) {
			Set<Folder> folders = getFoldersToCheck((Template) object);

			// if the template is linked to no folders at all, we allow access for users that have permission on the devtools
			if (folders.isEmpty() && checkPermissionBit(TYPE_DEVTOOLS_PACKAGES, null, PERM_VIEW)) {
				return true;
			}

			for (Folder folder : folders) {
				// at least for one folder, the template must be visible
				if (checkPermissionBits(Folder.TYPE_FOLDER, folder.getId(), PERM_VIEW, PERM_TEMPLATE_VIEW)) {
					return true;
				}
			}
			return false;
		} else if (object instanceof Construct) {
			// first check the permission to view constructs in the Content.Admin
			if (!canView(null, Construct.class, null)) {
				return false;
			}

			Construct construct = (Construct) object;
			// get all nodes the construct is assigned to
			List<Node> nodes = construct.getNodes();

			for (Node node : nodes) {
				// when the user can see the node, he can see the construct
				if (checkPermissionBit(Node.TYPE_NODE_INTEGER, node.getFolder().getId(), PERM_VIEW)) {
					return true;
				}
				// it may be possible, that the user can see a channel only
				for (Node channel : node.getAllChannels()) {
					// when the user can see the node, he can see the construct
					if (checkPermissionBit(Node.TYPE_NODE_INTEGER, channel.getFolder().getId(), PERM_VIEW)) {
						return true;
					}
				}
			}

			if (nodes.size() == 0) {
				// construct is not assigned to a single node, so just show it
				return true;
			} else {
				// user could see no node
				return false;
			}
		} else if (object instanceof ConstructCategory) {
			// check the permission to view construct categories in the Content.Admin
			return canView(null, ConstructCategory.class, null);
		} else if (object instanceof ObjectTag) {
			// The permissions of object properties are fetched from their definitions.
			// However object properties on pages and files are special, because they also inherit
			// the permissions from their folder and template (page only).
			return checkObjectPropertyInheritedPermissions((ObjectTag)object, PERM_VIEW);
		} else if (object instanceof ObjectTagDefinition) {
			return canView(null, ObjectTagDefinition.class, null, ((ObjectTagDefinition) object).getTargetType())
					&& checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF, object.getId(), PERM_VIEW);
		} else if (object instanceof ObjectTagDefinitionCategory) {
			return canView(null, ObjectTagDefinitionCategory.class, null);
		} else if (object instanceof ContentLanguage) {
			return canView(null, ContentLanguage.class, null);
		} else if (object instanceof ContentRepository) {
			return canView(null, ContentRepository.class, null) && checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORY, object.getId(), PERM_VIEW);
		} else if (object instanceof CrFragment) {
			return canView(null, CrFragment.class, null) && checkPermissionBit(CrFragment.TYPE_CR_FRAGMENT, object.getId(), PERM_VIEW);
		} else if (object instanceof Datasource) {
			if (!canView(null, Datasource.class, null)) {
				return false;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.DATASOURCE_PERM)) {
				return checkPermissionBit(Datasource.TYPE_SINGLE_DATASOURCE, object.getId(), PERM_VIEW);
			} else {
				return true;
			}
		} else if (object instanceof SystemUser) {
			Transaction t = TransactionManager.getCurrentTransaction();
			// The user is always allowed to view himself
			if (t.getUserId()== object.getId()) {
				return true;
			}
			// check permission to view users in the Content.Admin
			if (!canView(null, SystemUser.class, null)) {
				return false;
			}

			// system user may do everything
			if (t.getUserId() == 1) {
				return true;
			}
			// Users may view other users if they are in the same group or in subgroups
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			if (user == null) {
				throw new NodeException("Cannot check permission for inexistent user");
			}
			SystemUser other = (SystemUser)object;

			return !Collections.disjoint(user.getAllGroupsWithChildren(p -> p.canView(null, SystemUser.class, null), true), other.getUserGroups());
		} else if (object instanceof UserGroup) {
			// check permission to view groups in the Content.Admin
			if (!canView(null, UserGroup.class, null)) {
				return false;
			}
			// also check whether the user is in a higher group (or the group itself)
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			if (user == null) {
				throw new NodeException("Cannot check permission for inexistent user");
			}
			List<UserGroup> groups = user.getAllGroupsWithChildren(true);
			return groups.contains(object);
		} else if (object instanceof Role) {
			return checkPermissionBit(Role.TYPE_ROLE, null, PERM_VIEW);
		} else if (object instanceof SchedulerTask) {
			// check permission to view the scheduler
			if (!canView(null, SchedulerTask.class, null)) {
				return false;
			}

			// either the user has permission to view all tasks or at least this specific task
			return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PermType.readtasks.getBit())
				|| checkPermissionBit(SchedulerTask.TYPE_SCHEDULER_TASK, object.getId(), PermType.read.getBit());
		} else if (object instanceof SchedulerSchedule) {
			// check permission to view the scheduler
			if (!canView(null, SchedulerSchedule.class, null)) {
				return false;
			}

			// either the user has permission to view all schedules or at least this specific schedule
			return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PermType.readschedules.getBit())
				|| checkPermissionBit(SchedulerSchedule.TYPE_SCHEDULER_SCHEDULE, object.getId(), PermType.read.getBit());
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows editing the given object. This also implies the permission to view the object.
	 * This method is currently implemented for Constructs, ConstructCategory, ObjTagDef, Folders, Pages, Images, Files and Templates
	 * @param object an object
	 * @return true if the perm handler allows editing the object, false if not
	 * @throws NodeException
	 */
	public boolean canEdit(NodeObject object) throws NodeException {
		Transaction t = null;
		boolean setChannelId = false;
		try {
			t = TransactionManager.getCurrentTransaction();
			setChannelId = changeTransactionChannel(object);

			// check view permission
			if (!canView(object)) {
				return false;
			}

			// do object type specific tests now
			if (object instanceof NodeObjectInFolder) {
				return ((NodeObjectInFolder) object).canEdit(this);
			} else if (object instanceof Folder) {
				return checkPermissionBits(Folder.TYPE_FOLDER, object.getId(), PERM_FOLDER_UPDATE);
			} else if (object instanceof Node) {
				return checkPermissionBit(Folder.TYPE_FOLDER, ((Node) object).getFolder().getId(), PERM_FOLDER_UPDATE);
			} else if (object instanceof Page || object instanceof File) {
				NodeObject folder = object.getParentObject();

				if (folder == null) {
					return false;
				} else {
					int languageId = -1;

					if (object instanceof Page) {
						languageId = ObjectTransformer.getInt(((Page) object).getLanguageId(), -1);
					}
					return checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), PERM_PAGE_UPDATE, ObjectTransformer.getInt(object.getTType(), -1),
							languageId, ROLE_UPDATE);
				}
			} else if (object instanceof Template) {
				Set<Folder> folders = getFoldersToCheck((Template) object);

				// if the template is linked to no folders at all, we allow access for users that have permission on the devtools
				if (folders.isEmpty() && checkPermissionBit(TYPE_DEVTOOLS_PACKAGES, null, PERM_VIEW)) {
					return true;
				}

				for (Folder folder : folders) {
					// at least for one folder, the template must be editable
					if (checkPermissionBits(Folder.TYPE_FOLDER, folder.getId(), PERM_TEMPLATE_UPDATE)) {
						return true;
					}
				}
				return false;
			} else if (object instanceof Construct) {
				// check the general edit permission for constructs
				if (!checkPermissionBit(Construct.TYPE_CONSTRUCTS_INTEGER, null, PERM_CONSTRUCT_UPDATE)) {
					return false;
				}

				// get all nodes, the construct is assigned to
				Construct construct = (Construct) object;
				List<Node> nodes = construct.getNodes();

				for (Node node : nodes) {
					// if the user cannot see one of the nodes or may not edit
					// constructs, he may not edit this construct
					if (!checkPermissionBit(Node.TYPE_NODE_INTEGER, node.getFolder().getId(), PERM_VIEW)
							|| !checkPermissionBit(Node.TYPE_NODE_INTEGER, node.getFolder().getId(), PERM_NODE_CONSTRUCT_MODIFY)) {
						return false;
					}
				}

				return true;
			} else if (object instanceof ConstructCategory) {
				// there are no specific edit permissions for categories required,
				// if the user can see them (which is checked above), he may edit
				// them
				return true;
			} else if (object instanceof SystemUser) {
				// A user is always allowed to edit himself
				if (t.getUserId() == object.getId()) {
					return true;
				}

				// check permission to edit users in the Content.Admin
				if (!canEdit(null, SystemUser.class, null)) {
					return false;
				}

				// system user may do everything
				if (t.getUserId() == 1) {
					return true;
				}

				SystemUser user = t.getObject(SystemUser.class, t.getUserId());
				if (user == null) {
					throw new NodeException("Cannot check permission for inexistent user");
				}
				SystemUser other = (SystemUser)object;

				// The user can edit the other user, if all groups of the other user are subgroups of groups that grant the user permission to edit users
				List<UserGroup> grantingGroups = UserGroup.reduceUserGroups(user.getUserGroups(p -> p.canEdit(null, SystemUser.class, null)), ReductionType.PARENT);
				List<UserGroup> checkedGroups = UserGroup.reduceUserGroups(other.getUserGroups(), ReductionType.PARENT);

				// no groups -> no permission
				if (grantingGroups.isEmpty() || checkedGroups.isEmpty()) {
					return false;
				}

				// check, whether all groups of the user are children of the user's groups
				for (UserGroup check : checkedGroups) {
					if (Collections.disjoint(check.getParents(), grantingGroups)) {
						return false;
					}
				}

				return true;
			} else if (object instanceof UserGroup) {
				return checkGroupPerm((UserGroup)object, p -> p.canEdit(null, UserGroup.class, null));
			} else if (object instanceof ObjectTag) {
				// The permissions of object properties are fetched from their definitions.
				// However object properties on pages/files are special, because they also inherit
				// the permissions from their folder and template (pages only).
				return checkObjectPropertyInheritedPermissions((ObjectTag)object, PERM_OBJPROP_UPDATE);
			} else if (object instanceof ObjectTagDefinition) {
				return checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF, object.getId(), PERM_OBJPROP_UPDATE);
			} else if (object instanceof ObjectTagDefinitionCategory) {
				// there are no specific edit permissions for categories required,
				// if the user can see them (which is checked above), he may edit
				// them
				return true;
			} else if (object instanceof ContentLanguage) {
				return true;
			} else if (object instanceof ContentRepository) {
				return checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORY, object.getId(), PERM_CONTENTREPOSITORY_UPDATE);
			} else if (object instanceof CrFragment) {
				return checkPermissionBit(CrFragment.TYPE_CR_FRAGMENT, object.getId(), PERM_CONTENTREPOSITORY_UPDATE);
			} else if (object instanceof Datasource) {
				return true;
			} else if (object instanceof Role) {
				return true;
			} else if (object instanceof SchedulerTask) {
				return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PERM_SCHEDULER_TASK_UPDATE);
			} else if (object instanceof SchedulerSchedule) {
				return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PERM_SCHEDULER_SCHEDULE_UPDATE);
			} else {
				return false;
			}
		} finally {
			if (setChannelId) {
				t.resetChannel();
			}
		}
	}

	/**
	 * Check whether the perm handler allows deleting the given object. This also implies the permission to view the object.
	 * This method is currently implemented for Folders, Pages, Images, Files and Templates.
	 * The check will be done in the scope of the object's channel (regardless of the currently set channel)
	 * @param object an object
	 * @return true if the perm handler allows deleting the object, false if not
	 * @throws NodeException
	 */
	public boolean canDelete(NodeObject object) throws NodeException {
		return canDelete(object, true);
	}

	/**
	 * Check whether the perm handler allows deleting the given object. This also implies the permission to view the object.
	 * This method is currently implemented for Folders, Pages, Images, Files and Templates
	 * @param object an object
	 * @param checkForObjectChannel true to do the check in the scope if the object's channel, false to check in the current scope
	 * @return true if the perm handler allows deleting the object, false if not
	 * @throws NodeException
	 */
	public boolean canDelete(NodeObject object, boolean checkForObjectChannel) throws NodeException {
		Transaction t = null;
		boolean setChannelId = false;
		try {
			t = TransactionManager.getCurrentTransaction();
			if (checkForObjectChannel) {
				setChannelId = changeTransactionChannel(object);
			}

			// check view permission
			if (!canView(object)) {
				return false;
			}

			// do object type specific tests now
			if (object instanceof NodeObjectInFolder) {
				return ((NodeObjectInFolder) object).canDelete(this);
			} else if (object instanceof Folder) {
				return checkPermissionBits(Folder.TYPE_FOLDER, object.getId(), PERM_FOLDER_DELETE);
			} else if (object instanceof Node) {
				return checkPermissionBit(Folder.TYPE_FOLDER, ((Node) object).getFolder().getId(), PERM_FOLDER_DELETE);
			} else if (object instanceof Page || object instanceof File) {
				NodeObject folder = null;
				try (ChannelTrx trx = new ChannelTrx()) {
					folder = object.getParentObject();
				}

				if (folder == null) {
					return false;
				} else {
					int languageId = -1;

					if (object instanceof Page) {
						languageId = ObjectTransformer.getInt(((Page) object).getLanguageId(), -1);
					}
					return checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), PERM_PAGE_DELETE, ObjectTransformer.getInt(object.getTType(), -1),
							languageId, ROLE_DELETE);
				}
			} else if (object instanceof Template) {
				Set<Folder> folders = getFoldersToCheck((Template) object);

				// if the template is linked to no folders at all, we allow access for users that have permission on the devtools
				if (folders.isEmpty() && checkPermissionBit(TYPE_DEVTOOLS_PACKAGES, null, PERM_VIEW)) {
					return true;
				}

				for (Folder folder : folders) {
					// at least for one folder, the template must be deletable
					if (checkPermissionBits(Folder.TYPE_FOLDER, folder.getId(), PERM_TEMPLATE_DELETE)) {
						return true;
					}
				}
				return false;
			} else if (object instanceof ConstructCategory) {
				return true;
			} else if (object instanceof Construct) {
				return canEdit(object);
			} else if (object instanceof ContentLanguage) {
				return true;
			} else if (object instanceof ContentRepository) {
				return checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORY, object.getId(), PERM_CONTENTREPOSITORY_DELETE);
			} else if (object instanceof CrFragment) {
				return checkPermissionBit(CrFragment.TYPE_CR_FRAGMENT, object.getId(), PERM_CONTENTREPOSITORY_DELETE);
			} else if (object instanceof Datasource) {
				return true;
			} else if (object instanceof ObjectTagDefinition) {
				return canEdit(object);
			} else if (object instanceof ObjectTagDefinitionCategory) {
				return canEdit(object);
			} else if (object instanceof SystemUser) {
				if (!canDelete(null, SystemUser.class, null)) {
					return false;
				}

				// system user may do everything
				if (t.getUserId() == 1) {
					return true;
				}

				SystemUser user = t.getObject(SystemUser.class, t.getUserId());
				if (user == null) {
					throw new NodeException("Cannot check permission for inexistent user");
				}
				SystemUser other = (SystemUser)object;

				// The user can delete the other user, if all groups of the other user are subgroups of groups that grant the user permission to delete users
				List<UserGroup> grantingGroups = UserGroup.reduceUserGroups(user.getUserGroups(p -> p.canDelete(null, SystemUser.class, null)), ReductionType.PARENT);
				List<UserGroup> checkedGroups = UserGroup.reduceUserGroups(other.getUserGroups(), ReductionType.PARENT);

				// no groups -> no permission
				if (grantingGroups.isEmpty() || checkedGroups.isEmpty()) {
					return false;
				}

				// check, whether all groups of the user are children of the user's groups
				for (UserGroup check : checkedGroups) {
					if (Collections.disjoint(check.getParents(), grantingGroups)) {
						return false;
					}
				}

				return true;
			} else if (object instanceof UserGroup) {
				return checkGroupPerm((UserGroup)object, p -> p.canDelete(null, UserGroup.class, null));
			} else if (object instanceof Role) {
				return canView(null, Role.class, null);
			} else if (object instanceof SchedulerTask) {
				return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PERM_SCHEDULER_TASK_UPDATE);
			} else if (object instanceof SchedulerSchedule) {
				return checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PERM_SCHEDULER_SCHEDULE_UPDATE);
			} else {
				return false;
			}
		} finally {
			if (setChannelId) {
				t.resetChannel();
			}
		}
	}

	/**
	 * Check whether the perm handler allows publishing the given object. This also implies the permission to view the object.
	 * This method is only implemented for pages, for all other objects, it will always return true.
	 * @param object an object
	 * @return true if the perm handler allows publishing the object, false if not
	 * @throws NodeException
	 */
	public boolean canPublish(NodeObject object) throws NodeException {
		// check view permission
		if (!canView(object)) {
			return false;
		}

		if (object instanceof PublishableNodeObjectInFolder) {
			return ((PublishableNodeObjectInFolder) object).canPublish(this);
		} else if (object instanceof Page) {
			Folder folder = ((Page) object).getFolder();

			if (folder == null) {
				return false;
			} else {
				int languageId = ObjectTransformer.getInt(((Page) object).getLanguageId(), -1);

				return checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), PERM_PAGE_PUBLISH, ObjectTransformer.getInt(object.getTType(), -1), languageId,
						ROLE_PUBLISH);
			}
		} else {
			return true;
		}
	}

	/**
	 * Check whether the perm handler grants permission defined by the given PermType. For pages and files, this will also consider the defined role permissions.
	 * @param f folder
	 * @param clazz object class
	 * @param languageId language ID (-1 for checking without language)
	 * @param permType perm type
	 * @return true if permission is granted, false if not
	 * @throws NodeException
	 */
	public boolean check(Folder f, Class<? extends NodeObject> clazz, int languageId, PermType permType) throws NodeException {
		if (Page.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), permType.getBit(), Page.TYPE_PAGE, languageId, permType.getPageRoleBit());
		} else if (File.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), permType.getBit(), File.TYPE_FILE, languageId, permType.getFileRoleBit());
		} else {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), permType.getBit());
		}
	}

	/**
	 * Check whether the perm handler allows viewing objects of given class in the given folder.
	 * This method is currently implemented for Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if the perm handler allows viewing objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canView(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
		return canView(f, clazz, language, -1);
	}

	/**
	 * Check whether the perm handler allows viewing objects of given class in the given folder.
	 * This method is currently implemented for Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @param objTagDefType objtag definition type, when checking for an objectag definition
	 * @return true if the perm handler allows viewing objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canView(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language, int objTagDefType) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// check view permission
		if (f != null && !canView(f)) {
			return false;
		}

		int languageId = Optional.ofNullable(language).map(ContentLanguage::getId).orElse(-1);

		ViewPermType annotation = clazz.getAnnotation(ViewPermType.class);
		if (annotation != null) {
			return check(f, clazz, languageId, annotation.value());
		}

		// do object type specific tests now
		if (Page.class.isAssignableFrom(clazz) || File.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), PERM_PAGE_VIEW, t.getTType(clazz), languageId, ROLE_VIEW);
		} else if (Template.class.isAssignableFrom(clazz)) {
			return checkPermissionBits(Folder.TYPE_FOLDER, f.getId(), PERM_TEMPLATE_VIEW);
		} else if (ConstructCategory.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(Construct.TYPE_CONSTRUCTS_INTEGER, null, PERM_VIEW)
					&& checkPermissionBit(ConstructCategory.TYPE_CONSTRUCT_CATEGORIES, null, PERM_VIEW));
		} else if (Construct.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(Construct.TYPE_CONSTRUCTS_INTEGER, null, PERM_VIEW));
		} else if (ContentLanguage.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(ContentLanguage.TYPE_CONTENTLANGUAGE, null, PERM_VIEW));
		} else if (ContentRepository.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORIES, null, PERM_VIEW));
		} else if (CrFragment.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(CrFragment.TYPE_CR_FRAGMENTS, null, PERM_VIEW));
		} else if (Datasource.class.isAssignableFrom(clazz)) {
			return (checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(Datasource.TYPE_DATASOURCE, null, PERM_VIEW));
		} else if (Node.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_CONTENTNODE, null, PERM_VIEW);
		} else if (ObjectTagDefinition.class.isAssignableFrom(clazz)) {
			boolean perm = checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEFS, null, PERM_VIEW);

			if (objTagDefType > 0) {
				perm &= checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF_FOR_TYPE, objTagDefType, PERM_VIEW);
			}

			return perm;
		} else if (ObjectTagDefinitionCategory.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_CONADMIN, null, PERM_VIEW)
					&& checkPermissionBit(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, null, PERM_VIEW);
		} else if (SystemUser.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(SystemUser.TYPE_USERADMIN, null, PERM_VIEW);
		} else if (UserGroup.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_VIEW);
		} else if (Role.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(Role.TYPE_ROLE, null, PERM_VIEW);
		} else if (SchedulerSchedule.class.isAssignableFrom(clazz) || SchedulerTask.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_ADMIN, null, PERM_VIEW) && checkPermissionBit(TYPE_SCHEDULER_ADMIN, null, PERM_VIEW);
		} else {
			return true;
		}
	}

	/**
	 * Check whether the perm handler allows creating an object of given class in the given folder.
	 * This also implies the permission to view the objects.
	 * This method is currently implemented for Folders, Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if the perm handler allows creating objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canCreate(Folder f, Class<? extends NodeObject> clazz,
			ContentLanguage language) throws NodeException {
		return canCreate(f, clazz, language, -1);
	}

	/**
	 * Check whether the perm handler allows creating an object of given class in the given folder.
	 * This also implies the permission to view the objects.
	 * This method is currently implemented for Folders, Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @param objTagDefType objecttag definition type id
	 * @return true if the perm handler allows creating objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canCreate(Folder f, Class<? extends NodeObject> clazz,
			ContentLanguage language, int objTagDefType) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// check view permission
		if (!canView(f, clazz, language, objTagDefType)) {
			return false;
		}

		int languageId = Optional.ofNullable(language).map(ContentLanguage::getId).orElse(-1);

		CreatePermType annotation = clazz.getAnnotation(CreatePermType.class);
		if (annotation != null) {
			return check(f, clazz, languageId, annotation.value());
		}

		// do object type specific tests now
		if (Folder.class.isAssignableFrom(clazz)) {
			return checkPermissionBits(Folder.TYPE_FOLDER, f.getId(), PERM_FOLDER_CREATE);
		} else if (Page.class.isAssignableFrom(clazz) || File.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), PERM_PAGE_CREATE, t.getTType(clazz), languageId, ROLE_CREATE);
		} else if (Template.class.isAssignableFrom(clazz)) {
			return checkPermissionBits(Folder.TYPE_FOLDER, f.getId(), PERM_TEMPLATE_CREATE);
		} else if (ConstructCategory.class.isAssignableFrom(clazz)) {
			return true;
		} else if (Construct.class.isAssignableFrom(clazz)) {
			return f != null && checkPermissionBit(Construct.TYPE_CONSTRUCTS_INTEGER, null, PERM_CONSTRUCT_UPDATE)
					&& checkPermissionBit(Node.TYPE_NODE, f.getId(), PERM_NODE_CONSTRUCT_MODIFY);
		} else if (ContentLanguage.class.isAssignableFrom(clazz)) {
			return true;
		} else if (ContentRepository.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(ContentRepository.TYPE_CONTENTREPOSITORIES, null, PERM_CONTENTREPOSITORY_CREATE);
		} else if (CrFragment.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(CrFragment.TYPE_CR_FRAGMENTS, null, PERM_CONTENTREPOSITORY_CREATE);
		} else if (Datasource.class.isAssignableFrom(clazz)) {
			return true;
		} else if (Node.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(TYPE_CONTENTNODE, null, PERM_NODE_CREATE);
		} else if (ObjectTagDefinition.class.isAssignableFrom(clazz)) {
			if (objTagDefType > 0) {
				return checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF_FOR_TYPE, objTagDefType, PERM_OBJPROP_UPDATE);
			} else {
				return true;
			}
		} else if (SystemUser.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(SystemUser.TYPE_USERADMIN, null, PERM_USER_CREATE);
		} else if (UserGroup.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_GROUP_CREATE);
		} else if (Role.class.isAssignableFrom(clazz)) {
			return true;
		} else if (ObjectTagDefinitionCategory.class.isAssignableFrom(clazz)) {
			return true;
		} else if (SchedulerTask.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(PermHandler.TYPE_SCHEDULER_ADMIN, null, PermType.updatetasks.getBit());
		} else if (SchedulerSchedule.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(PermHandler.TYPE_SCHEDULER_ADMIN, null, PermType.updateschedules.getBit());
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows editing objects of given class in the given folder.
	 * This also implies the permission to view the objects.
	 * This method is currently implemented for Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if the perm handler allows editing objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canEdit(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
		// get the perm handler of the current transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		// check view permission
		if (!canView(f, clazz, language)) {
			return false;
		}

		int languageId = Optional.ofNullable(language).map(ContentLanguage::getId).orElse(-1);

		EditPermType annotation = clazz.getAnnotation(EditPermType.class);
		if (annotation != null) {
			return check(f, clazz, languageId, annotation.value());
		}

		// do object type specific tests now
		if (Page.class.isAssignableFrom(clazz) || File.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), PERM_PAGE_UPDATE, t.getTType(clazz), languageId, ROLE_UPDATE);
		} else if (Template.class.isAssignableFrom(clazz)) {
			return checkPermissionBits(Folder.TYPE_FOLDER, f.getId(), PERM_TEMPLATE_UPDATE);
		} else if (ContentLanguage.class.isAssignableFrom(clazz)) {
			return true;
		} else if (Datasource.class.isAssignableFrom(clazz)) {
			return true;
		} else if (SystemUser.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(SystemUser.TYPE_USERADMIN, null, PERM_USER_UPDATE) && checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_GROUP_USERUPDATE);
		} else if (UserGroup.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_GROUP_UPDATE);
		} else if (ObjectTagDefinition.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF, null, PERM_OBJPROP_UPDATE);
		} else if (ObjectTagDefinitionCategory.class.isAssignableFrom(clazz)) {
			return true;
		} else if (ConstructCategory.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(ConstructCategory.TYPE_CONSTRUCT_CATEGORY, null, PERM_OBJPROP_UPDATE);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows deleting objects of given class in the given folder.
	 * This also implies the permission to view the objects.
	 * This method is currently implemented for Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if the perm handler allows deleting objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canDelete(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
		// get the perm handler of the current transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		// check view permission
		if (!canView(f, clazz, language)) {
			return false;
		}

		int languageId = Optional.ofNullable(language).map(ContentLanguage::getId).orElse(-1);

		DeletePermType annotation = clazz.getAnnotation(DeletePermType.class);
		if (annotation != null) {
			return check(f, clazz, languageId, annotation.value());
		}

		// do object type specific tests now
		if (Page.class.isAssignableFrom(clazz) || File.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), PERM_PAGE_DELETE, t.getTType(clazz), languageId, ROLE_DELETE);
		} else if (Template.class.isAssignableFrom(clazz)) {
			return checkPermissionBits(Folder.TYPE_FOLDER, f.getId(), PERM_TEMPLATE_DELETE);
		} else if (ContentLanguage.class.isAssignableFrom(clazz)) {
			return true;
		} else if (Datasource.class.isAssignableFrom(clazz)) {
			return true;
		} else if (SystemUser.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(SystemUser.TYPE_USERADMIN, null, PERM_USER_DELETE);
		} else if (UserGroup.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_USER_DELETE);
		} else if (ObjectTagDefinition.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF, null, PERM_OBJPROP_UPDATE);
		} else if (ObjectTagDefinitionCategory.class.isAssignableFrom(clazz)) {
			return true;
		} else if (ConstructCategory.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(ConstructCategory.TYPE_CONSTRUCT_CATEGORY, null, PERM_OBJPROP_UPDATE);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows publishing objects of given class in the given folder.
	 * This also implies the permission to view the objects.
	 * This method is currently implemented for Pages, Images, Files and Templates
	 * @param f folder
	 * @param clazz object class
	 * @param language language to do the check for
	 * @return true if the perm handler allows publishing objects of the class, false if not
	 * @throws NodeException
	 */
	public boolean canPublish(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
		// check view permission
		if (!canView(f, clazz, language)) {
			return false;
		}

		int languageId = Optional.ofNullable(language).map(ContentLanguage::getId).orElse(-1);

		PublishPermType annotation = clazz.getAnnotation(PublishPermType.class);
		if (annotation != null) {
			return check(f, clazz, languageId, annotation.value());
		}

		// do object type specific tests now
		if (Page.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, f.getId(), PERM_PAGE_PUBLISH, Page.TYPE_PAGE, languageId, ROLE_PUBLISH);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows translating object of given class in the given folder into the given language
	 * @param f folder
	 * @param clazz object class
	 * @param language language to translate to
	 * @return true if the perm handler allows translating objects into the language, false if not
	 * @throws NodeException
	 */
	public boolean canTranslate(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
		// check view permission
		if (!canView(f, clazz, language)) {
			return false;
		}

		if (language == null) {
			throw new NodeException("Cannot check translate permission without target language");
		}
		Integer folderId = f.getId();
		int languageId = ObjectTransformer.getInt(language.getId(), 0);

		// do object type specific tests now
		if (Page.class.isAssignableFrom(clazz)) {
			return checkPermissionBit(Folder.TYPE_FOLDER, folderId, PERM_PAGE_CREATE, Page.TYPE_PAGE, languageId, ROLE_CREATE)
					|| checkPermissionBit(Folder.TYPE_FOLDER, folderId, PERM_PAGE_CREATE, Page.TYPE_PAGE, languageId, ROLE_TRANSLATE);
		} else {
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows setting permissions for the given object
	 * This also implies the permission to view the object
	 * @param object object
	 * @return true if the permission is granted, false if not
	 * @throws NodeException
	 */
	public boolean canSetPerms(NodeObject object) throws NodeException {
		// check view permission
		if (!canView(object)) {
			return false;
		}

		// do object type specific tests now
		if (object instanceof Folder) {
			return checkPermissionBit(object.getTType(), object.getId(), PERM_CHANGE_PERM);
		} else if (object instanceof UserGroup) {
			if (!checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PERM_CHANGE_GROUP_PERM)) {
				return false;
			}

			// check whether the user is member of a higher group
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			if (user == null) {
				throw new NodeException("Cannot check permissions for inexistent user");
			}
			return user.getAllGroupsWithChildren(false).contains(object);
		} else {
			Integer tType = object.getTType();
			if (tType != null) {
				TypePerms typePerms = TypePerms.get(Integer.toString(tType));
				if (typePerms != null) {
					return typePerms.canSetPerms(object.getId());
				}
			}
			return false;
		}
	}

	/**
	 * Check whether the perm handler allows viewing and restoring objects in the waste bin of the given node
	 * @param node node
	 * @return true if the permission is granted, false if not
	 * @throws NodeException
	 */
	public boolean canWastebin(Node node) throws NodeException {
		if (node == null) {
			return false;
		}

		return checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PERM_NODE_WASTEBIN);
	}

	/**
	 * Check permission on a group.
	 * The general rule for granting permissions on groups is, that
	 * a user is granted a permission on all subgroups of groups granting the permission (if the user is member of the granting group)
	 * @param group group
	 * @param checker checker function
	 * @return true if permission is granted.
	 * @throws NodeException
	 */
	public boolean checkGroupPerm(UserGroup group, Function<PermHandler, Boolean> checker) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// system user may do everything
		if (t.getUserId() == 1) {
			return true;
		}

		// check general permission
		if (!checker.apply(this)) {
			return false;
		}

		SystemUser user = t.getObject(SystemUser.class, t.getUserId());
		if (user == null) {
			throw new NodeException("Cannot check permission for inexistent user");
		}

		// The user has the permission in question on all subgroups of groups that grant the permission
		List<UserGroup> grantingGroups = UserGroup.reduceUserGroups(user.getUserGroups(checker::apply), ReductionType.PARENT);

		for (UserGroup granting : grantingGroups) {
			if (group.getParents().contains(granting)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Change the current transaction channel to the channel of the given object, if the object is a channel object, or the owning node for master objects in non-channel nodes.
	 * For templates, the current channel is set to null
	 * @param object object
	 * @return true if the channel has been set (and needs to be reset), false if not
	 * @throws NodeException
	 */
	protected boolean changeTransactionChannel(NodeObject object) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (object instanceof LocalizableNodeObject<?>) {
			@SuppressWarnings("unchecked")
			LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>)object;
			Node channel = locObject.getChannel();
			if (channel == null) {
				channel = locObject.getOwningNode();
			}
			if (channel != null) {
				t.setChannelId(channel.getId());
				return true;
			} else if (object instanceof Template) {
				t.setChannelId(null);
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the folders which shall be checked for permissions on the given template
	 * @param template template
	 * @return set of folders to check
	 * @throws NodeException
	 */
	protected Set<Folder> getFoldersToCheck(Template template) throws NodeException {
		// get all folders the template is linked to
		Set<Folder> folders = new HashSet<>(template.getFolders());

		// add the root folders of nodes, the template is assigned to
		for (Node node : template.getAssignedNodes()) {
			folders.add(node.getFolder());
		}

		// Special case for wastebin: If a template is only linked to wastebin folders,
		// the above call to .getFolders will return no folder. In this case we also want to
		// get wastebin folders in order to be able to check the template permission.
		if (folders.isEmpty()) {
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				folders.addAll(template.getFolders());
			}
		}

		return folders;
	}

	/**
	 * Checks is the user of the current transaction has a specific permission on the passed objectTag.
	 * Since object tag permissions are based on the object tag definitions. It gets the permissions from
	 * the object tags definitions.
	 * For object tags on pages/files, template/folder permission inheritance is also taken into account.
	 * For files, only the folder hands down object property permissions.
	 * See: "Permission system" in contentnode dev-guides
	 * @param objectTag   Object property
	 * @param permission  This should either be PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE or PermHandler.PERM_CHANGE_PERM
	 * @return  True if permission is granted, false otherwise
	 * @throws NodeException
	 */
	boolean checkObjectPropertyInheritedPermissions(ObjectTag objectTag, int permission) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		if (objectTag == null) {
			return false;
		}

		final String objectTagName = objectTag.getName();

		if (objectTag.isIntag()) {
			// ugly hack for data inconsistency, which is caused by a bug in import/export
			// column "intag" of table "objecttag" might be just 1, instead of the parent objtag id
			// in this case, we cannot determine the parent objecttag anymore, so we allow access
			if (ObjectTransformer.getInt(objectTag.getInTagId(), 0) == 1) {
				return true;
			}
			ObjectTag parentTag = objectTag.getInTagObject();

			if (parentTag == null) {
				return true;
			}

			return this.checkObjectPropertyInheritedPermissions(parentTag, permission);
		}

		NodeObject nodeObject = objectTag.getNodeObject();
		if (nodeObject == null) {
			throw new NodeException("Could not get owning nodeObject for object property {"
					+ " id: " + objectTag.getId() + " name: " + objectTagName
					+ " type: " + objectTag.getObjType() + " }");
		}

		ObjectTagDefinition objectTagDefinition = objectTag.getDefinition();
		if (objectTagDefinition == null) {
			// It's valid that some object tags don't have a definition,
			// this is the case for example when it is an "intag".
			// Since we can't check the permission in this case, just return true.
			return true;
		}

		boolean permissionGranted = permHandler.checkPermissionBit(
				ObjectTagDefinition.TYPE_OBJTAG_DEF, objectTagDefinition.getId(), permission);

		// Object tag permissions on pages & files are special, because the permissions
		// are inherited also from the folder and the template to the page.
		if (!permissionGranted
				&& (nodeObject instanceof Page || nodeObject instanceof File || nodeObject instanceof ImageFile)) {
			Folder folder = null;
			if (nodeObject instanceof Page) {
				folder = ((Page)nodeObject).getFolder();
			} else {
				folder = ((File)nodeObject).getFolder();
			}

			String objTypeCondition;
			if (nodeObject instanceof Page) {
				objTypeCondition = "objtag.obj_type IN (" + Folder.TYPE_FOLDER + ", "
						+ Template.TYPE_TEMPLATE + ")";
			} else {
				objTypeCondition = "objtag.obj_type = " + Folder.TYPE_FOLDER;
			}

			final int pageNodeId = ObjectTransformer.getInt(folder.getNode().getMaster().getId(), 0);
			final List<Integer> objectTagDefinitionIds = new Vector<Integer>();

			// Load the object tag definitions for the folder and the template of the same name
			// This also takes object property definition node restrictions into account.
			DBUtils.executeStatement("SELECT objtag.id FROM objtag"
					+ " LEFT JOIN objprop ON objprop.objtag_id = objtag.id"
					+ " LEFT JOIN objprop_node ON objprop_node.objprop_id = objprop.id"
					+ " WHERE " + objTypeCondition + " AND objtag.obj_id = 0"
					+ " AND objtag.name = ?"
					+ " AND (objprop_node.node_id IS NULL OR objprop_node.node_id = ?)",
					new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement preparedStatement) throws SQLException {
					preparedStatement.setString(1, objectTagName);
					preparedStatement.setInt(2, pageNodeId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						objectTagDefinitionIds.add(rs.getInt("id"));
					}
				}
			});

			// Check if the template or the folder object tag bequeaths permission to the page
			List<ObjectTagDefinition> objectTagDefinitions = t.getObjects(
					ObjectTagDefinition.class, objectTagDefinitionIds);
			for (ObjectTagDefinition folderOrTemplateObjectTagDefinition : objectTagDefinitions) {
				if (permHandler.checkPermissionBit(ObjectTagDefinition.TYPE_OBJTAG_DEF,
						folderOrTemplateObjectTagDefinition.getId(), permission)) {
					permissionGranted = true;
					break;
				}
			}
		}

		return permissionGranted;
	}

	/**
	 * Inner class holding permission bits
	 */
	public static class Permission {

		public Permission() {}

		/**
		 * here are the bits stored
		 */
		protected byte[] bits;

		/**
		 * Create an instance of the Permission with the given bits (set as string of "0" and "1")
		 * @param bitsAsString bits
		 */
		public Permission(String bitsAsString) {
			if (bitsAsString.matches("^[0-1]*$")) {
				int c = bitsAsString.length();

				bits = new byte[bitsAsString.length()];
				for (int i = 0; i < c; ++i) {
					bits[i] = "1".equals(bitsAsString.substring(i, i + 1)) ? (byte) 1 : (byte) 0;
				}
			}
		}

		/**
		 * Create a Permission instance with the given bits (set as bit indices)
		 * @param bitsAsString bits
		 */
		public Permission(int... permissionbit) {
			bits = new byte[32];
			for (int bit : permissionbit) {
				bits[bit] = 1;
			}
		}

		/**
		 * Merge the given bits into the permission settings.
		 * @param bitsAsString bits
		 */
		public void mergeBits(String bitsAsString) {
			if (bitsAsString.matches("^[0-1]*$")) {
				int c = bitsAsString.length();

				if (bits == null) {
					bits = new byte[bitsAsString.length()];
				}
				if (c > bits.length) {
					byte[] newBits = new byte[c];

					System.arraycopy(bits, 0, newBits, 0, bits.length);
					bits = newBits;
				}

				for (int i = 0; i < c; ++i) {
					bits[i] |= "1".equals(bitsAsString.substring(i, i + 1)) ? (byte) 1 : (byte) 0;
				}
			}
		}

		/**
		 * Check whether the given permission bit is set
		 * @param bit permission bit
		 * @return true when the bit is set, false if not
		 */
		public boolean checkPermissionBit(int bit) {
			if (bit < 0 || bit >= bits.length) {
				return false;
			} else {
				return bits[bit] == (byte) 1;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();

			for (int i = 0; i < bits.length; i++) {
				buffer.append((int) bits[i]);
			}
			return buffer.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Permission) {
				return toString().equals(obj.toString());
			} else {
				return false;
			}
		}
	}

	/**
	 * Enumeration of effective object permissions
	 */
	public static enum ObjectPermission {
		/**
		 * View an object
		 */
		view(PermType.read,
				(t, o) -> t.canView(o),
				(t, triple) -> t.canView(triple.getLeft(), triple.getMiddle(), triple.getRight()),
				com.gentics.contentnode.rest.model.request.Permission.view),

		read(PermType.read,
			(t, o) -> t.canView(o),
			(t, triple) -> t.canView(triple.getLeft(), triple.getMiddle(), triple.getRight()),
			com.gentics.contentnode.rest.model.request.Permission.read),

		/**
		 * Create an object
		 */
		create(PermType.create,
				(t, o) -> {
					NodeObject parent = o.getParentObject();

					if (parent instanceof Folder) {
						ContentLanguage language = null;

						if (o instanceof Page) {
							language = ((Page) o).getLanguage();
						}
						return t.canCreate((Folder) parent, o.getObjectInfo().getObjectClass(), language);
					} else {
						return false;
					}
				},
				(t, triple) -> t.canCreate(triple.getLeft(), triple.getMiddle(), triple.getRight()),
				com.gentics.contentnode.rest.model.request.Permission.create),

		/**
		 * Edit an object
		 */
		edit(PermType.update,
				(t, o) -> t.canEdit(o),
				(t, triple) -> t.canEdit(triple.getLeft(), triple.getMiddle(), triple.getRight()),
				com.gentics.contentnode.rest.model.request.Permission.edit),

		update(edit),

		/**
		 * Delete an object
		 */
		delete(PermType.delete,
				 (t, o) -> t.canDelete(o),
				 (t, triple) -> t.canDelete(triple.getLeft(), triple.getMiddle(), triple.getRight()),
				 com.gentics.contentnode.rest.model.request.Permission.delete) {
			@Override
			public boolean checkObject(NodeObject o, Node channel) throws NodeException {
				try (ChannelTrx trx = new ChannelTrx(channel)) {
					Transaction t = TransactionManager.getCurrentTransaction();
					return t.getPermHandler().canDelete(o, false);
				}
			}
		},

		createfolder(PermType.create, checkPermissionOnNodeOrFolder(PermType.create), com.gentics.contentnode.rest.model.request.Permission.createfolder),

		updatefolder(PermType.updatefolder, checkPermissionOnNodeOrFolder(PermType.updatefolder), com.gentics.contentnode.rest.model.request.Permission.updatefolder),

		deletefolder(PermType.deletefolder, checkPermissionOnNodeOrFolder(PermType.deletefolder), com.gentics.contentnode.rest.model.request.Permission.deletefolder),

		linkoverview(PermType.linkoverview, checkPermissionOnNodeOrFolder(PermType.linkoverview), com.gentics.contentnode.rest.model.request.Permission.linkoverview),

		createoverview(PermType.createoverview, checkPermissionOnNodeOrFolder(PermType.createoverview), com.gentics.contentnode.rest.model.request.Permission.createoverview),

		readitems(PermType.readitems, checkPermissionOnNodeOrFolder(PermType.readitems), com.gentics.contentnode.rest.model.request.Permission.readitems),

		createitems(PermType.createitems, checkPermissionOnNodeOrFolder(PermType.createitems), com.gentics.contentnode.rest.model.request.Permission.createitems),

		updateitems(PermType.updateitems, checkPermissionOnNodeOrFolder(PermType.updateitems), com.gentics.contentnode.rest.model.request.Permission.updateitems),

		deleteitems(PermType.deleteitems, checkPermissionOnNodeOrFolder(PermType.deleteitems), com.gentics.contentnode.rest.model.request.Permission.deleteitems),

		importitems(PermType.importitems, checkPermissionOnNodeOrFolder(PermType.importitems), com.gentics.contentnode.rest.model.request.Permission.importitems),

		/**
		 * Publish an object
		 */
		publish(PermType.publishpages,
				(t, o) -> t.canPublish(o),
				(t, triple) -> t.canPublish(triple.getLeft(), triple.getMiddle(), triple.getRight()),
				com.gentics.contentnode.rest.model.request.Permission.publish),

		publishpages(PermType.publishpages, checkPermissionOnNodeOrFolder(PermType.publishpages), com.gentics.contentnode.rest.model.request.Permission.publishpages),

		translatepages(PermType.translatepages, checkPermissionOnNodeOrFolder(PermType.translatepages), com.gentics.contentnode.rest.model.request.Permission.translatepages),

		viewform(PermType.viewform, checkPermissionOnNodeOrFolder(PermType.viewform), com.gentics.contentnode.rest.model.request.Permission.viewform),

		createform(PermType.createform, checkPermissionOnNodeOrFolder(PermType.createform), com.gentics.contentnode.rest.model.request.Permission.createform),

		updateform(PermType.updateform, checkPermissionOnNodeOrFolder(PermType.updateform), com.gentics.contentnode.rest.model.request.Permission.updateform),

		deleteform(PermType.deleteform, checkPermissionOnNodeOrFolder(PermType.deleteform), com.gentics.contentnode.rest.model.request.Permission.deleteform),

		publishform(PermType.publishform, checkPermissionOnNodeOrFolder(PermType.publishform), com.gentics.contentnode.rest.model.request.Permission.publishform),

		formreport(PermType.formreport, checkPermissionOnNodeOrFolder(PermType.formreport), com.gentics.contentnode.rest.model.request.Permission.formreport),

		readtemplates(PermType.readtemplates, checkPermissionOnNodeOrFolder(PermType.readtemplates), com.gentics.contentnode.rest.model.request.Permission.readtemplates),

		createtemplates(PermType.createtemplates, checkPermissionOnNodeOrFolder(PermType.createtemplates), com.gentics.contentnode.rest.model.request.Permission.createtemplates),

		updatetemplates(PermType.updatetemplates, checkPermissionOnNodeOrFolder(PermType.updatetemplates), com.gentics.contentnode.rest.model.request.Permission.updatetemplates),

		deletetemplates(PermType.deletetemplates, checkPermissionOnNodeOrFolder(PermType.deletetemplates), com.gentics.contentnode.rest.model.request.Permission.deletetemplates),

		linktemplates(PermType.linktemplates, checkPermissionOnNodeOrFolder(PermType.linktemplates), com.gentics.contentnode.rest.model.request.Permission.linktemplates),

		updateconstructs(PermType.updateconstructs, checkPermissionOnNodeOrFolder(PermType.updateconstructs), com.gentics.contentnode.rest.model.request.Permission.updateconstructs),

		channelsync(PermType.channelsync, checkPermissionOnNodeOrFolder(PermType.channelsync), com.gentics.contentnode.rest.model.request.Permission.channelsync),

		/**
		 * Change inheritance of the object
		 */
		inheritance(PermType.updateinheritance,
				(t, o) -> {
					if (o instanceof Disinheritable<?>) {
						return t.getPermHandler().checkPermissionBit(Node.TYPE_NODE_INTEGER, ((Disinheritable<?>) o).getNode().getFolder().getMaster().getId(),
								PERM_INHERITANCE);
					} else {
						return false;
					}
				},
				(t, triple) -> {
					return t.getPermHandler().checkPermissionBit(Node.TYPE_NODE_INTEGER, triple.getLeft().getNode().getFolder().getMaster().getId(), PERM_INHERITANCE);
				},
				com.gentics.contentnode.rest.model.request.Permission.inheritance),

		updateinheritance(inheritance),

		/**
		 * View and restore wastebin
		 */
		wastebin(PermType.wastebin,
				(t, o) -> {
					if (o instanceof LocalizableNodeObject<?>) {
						LocalizableNodeObject<?> locObject = (LocalizableNodeObject<?>)o;
						Node node = locObject.getChannel();
						if (node == null) {
							node = locObject.getOwningNode();
						}
						return TransactionManager.getCurrentTransaction().canWastebin(node);
					} else if (o instanceof NodeObjectInFolder) {
						NodeObjectInFolder contained = (NodeObjectInFolder) o;
						return TransactionManager.getCurrentTransaction().canWastebin(contained.getFolder().getOwningNode());
					} else if (o instanceof Node) {
						return TransactionManager.getCurrentTransaction().canWastebin((Node) o);
					} else {
						return false;
					}
				},
				(t, triple) -> t.canWastebin(triple.getLeft().getOwningNode()),
				com.gentics.contentnode.rest.model.request.Permission.wastebin),

		/**
		 * Set permissions
		 */
		setperm(PermType.setperm, (t, o) -> t.getPermHandler().canSetPerms(o), com.gentics.contentnode.rest.model.request.Permission.setperm),

		/**
		 * Change user assignment
		 */
		userassignment(PermType.userassignment, (t, o) -> {
			if (o instanceof UserGroup) {
				UserGroup group = (UserGroup) o;
				return t.getPermHandler().checkGroupPerm(group,
						p -> p.checkPermissionBit(UserGroup.TYPE_GROUPADMIN, null, PermHandler.PERM_GROUP_USERADD));
			} else {
				return false;
			}
		}, com.gentics.contentnode.rest.model.request.Permission.userassignment)

		;

		/**
		 * Get the ObjectPermission instance with the same name as the given REST permission
		 * @param perm permission
		 * @return instance or null, if not found
		 */
		public static ObjectPermission get(com.gentics.contentnode.rest.model.request.Permission perm) {
			try {
				return ObjectPermission.valueOf(perm.name());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		/**
		 * Return a function that checks the permission bit from the perm type for the node
		 * @param permType permission type
		 * @return function
		 */
		private static BiFunction<Transaction, NodeObject, Boolean> checkPermissionOnNodeOrFolder(PermType permType) {
			return (t, o) -> {
				if (o instanceof Node) {
					return t.getPermHandler().checkPermissionBit(Node.TYPE_NODE_INTEGER, ((Node) o).getFolder().getId(),
							permType.getBit());
				} else if (o instanceof Folder) {
					return t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER_INTEGER, o.getId(), permType.getBit());
				} else {
					return false;
				}
			};
		}

		/**
		 * Perm Type
		 */
		private PermType permType;

		/**
		 * Object checker function
		 */
		private final BiFunction<Transaction, NodeObject, Boolean> objectChecker;

		/**
		 * Class checker function (for object classes in folders)
		 */
		private final BiFunction<Transaction, Triple<Folder, Class<? extends NodeObject>, ContentLanguage>, Boolean> classChecker;

		/**
		 * Effective object permission
		 */
		private final com.gentics.contentnode.rest.model.request.Permission effectivePermission;

		/**
		 * Create instance with only object checker
		 * @param permType perm type
		 * @param objectChecker object checker function
		 * @param effectivePermission effective permission
		 */
		ObjectPermission(PermType permType, BiFunction<Transaction, NodeObject, Boolean> objectChecker, com.gentics.contentnode.rest.model.request.Permission effectivePermission) {
			this(permType, objectChecker, (t, triple) -> false, effectivePermission);
		}

		/**
		 * Create instance
		 * @param permType perm type
		 * @param objectChecker object checker function
		 * @param classChecker class checker function
		 * @param effectivePermission effective permission
		 */
		ObjectPermission(PermType permType, BiFunction<Transaction, NodeObject, Boolean> objectChecker,
				BiFunction<Transaction, Triple<Folder, Class<? extends NodeObject>, ContentLanguage>, Boolean> classChecker, com.gentics.contentnode.rest.model.request.Permission effectivePermission) {
			this.permType = permType;
			this.objectChecker = objectChecker;
			this.classChecker = classChecker;
			this.effectivePermission = effectivePermission;
		}

		/**
		 * Create instance as alias of the given instance
		 * @param alias alias
		 */
		ObjectPermission(ObjectPermission alias) {
			this.permType = alias.permType;
			this.objectChecker = alias.objectChecker;
			this.classChecker= alias.classChecker;
			this.effectivePermission = alias.effectivePermission;
		}

		/**
		 * Check the given object against the permission
		 * @param o object to check
		 * @return true when the current transaction grants the permission, false if not
		 * @throws NodeException
		 */
		public boolean checkObject(NodeObject o) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return objectChecker.apply(t, o);
		}

		/**
		 * Check the given object against the permission in the given channel
		 * @param o object to check
		 * @param channel channel to check (null to check without channel)
		 * @return true when the current transaction grants the permission, false if not
		 * @throws NodeException
		 */
		public boolean checkObject(NodeObject o, Node channel) throws NodeException {
			try (ChannelTrx trx = new ChannelTrx(channel)) {
				return checkObject(o);
			}
		}

		/**
		 * Check the given object class in the given folder against the permission
		 * @param f folder
		 * @param clazz object class
		 * @param language language to check the permission for
		 * @return true when the current transaction grants the permission, false if not
		 * @throws NodeException
		 */
		public boolean checkClass(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			return classChecker.apply(t, Triple.of(f, clazz, language));
		}

		/**
		 * Check the given object class in the given folder against the permission
		 * @param f folder
		 * @param clazz object class
		 * @param language language to check the permission for
		 * @param channel channel to check in
		 * @return true when the current transaction grants the permission, false if not
		 * @throws NodeException
		 */
		public boolean checkClass(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language, Node channel) throws NodeException {
			try (ChannelTrx trx = new ChannelTrx(channel)) {
				return checkClass(f, clazz, language);
			}
		}

		/**
		 * Get the perm type
		 * @return perm type
		 */
		public PermType getPermType() {
			return permType;
		}

		/**
		 * Get the effective permission
		 * @return effective permission
		 */
		public com.gentics.contentnode.rest.model.request.Permission getEffectivePermission() {
			return effectivePermission;
		}
	}
}
