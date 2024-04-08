package com.gentics.contentnode.perm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

/**
 * Static class that holds all permissions (for all groups)
 */
public class PermissionStore {
	/**
	 * Singleton instance
	 */
	protected static PermissionStore singleton = null;

	/**
	 * Check types for role permissions
	 */
	public final static List<Integer> roleCheckTypes = Arrays.asList(Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE);

	/**
	 * Logger instance
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(PermissionStore.class);

	/**
	 * Loader for instances of {@link PermissionStoreService}
	 */
	protected static ServiceLoaderUtil<PermissionStoreService> permissionStoreServiceLoader = ServiceLoaderUtil
			.load(PermissionStoreService.class);

	/**
	 * Get the singleton instance
	 * @return singleton instance
	 */
	public static PermissionStore getInstance() throws NodeException {
		if (singleton == null) {
			throw new NodeException("PermissionStore has not yet been initialized");
		}
		return singleton;
	}

	/**
	 * Initialize the permission store, if not done before
	 * @throws NodeException 
	 */
	public static void initialize() throws NodeException  {
		initialize(false);
	}

	/**
	 * Initialize the permission store, if not done before or the force flag is set
	 * @param force true to re-initialize the permission store, even if it was initialized before
	 * @throws NodeException 
	 */
	public static synchronized void initialize(boolean force) throws NodeException  {
		if (singleton == null || force) {
			long start = System.currentTimeMillis();
			singleton = new PermissionStore();

			if (logger.isInfoEnabled()) {
				long duration = System.currentTimeMillis() - start;
				logger.info("Initialized PermissionStore" + (force ? " by force" : "") + " in " + duration + " ms");
			}
		}
	}

	/**
	 * Nested map holding all permission entries (organized by group/type/id)
	 */
	@SuppressWarnings("unchecked")
	protected Map<Integer, Map<Integer, Map<Integer, Permissions>>> perms = Collections.synchronizedMap(new THashMap());

	/**
	 * Nested map holding role ID sets for a given object permission (organized by group/type id/obj id)
	 */
	@SuppressWarnings("unchecked")
	protected Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> rolePerms = Collections.synchronizedMap(new THashMap());

	/**
	 * Role permissions per ID
	 */
	@SuppressWarnings("unchecked")
	protected Map<Integer, RolePermissions> roles = Collections.synchronizedMap(new THashMap());

	/**
	 * Protected constructor
	 */
	protected PermissionStore() throws NodeException {
		// refresh all groups
		List<Integer> userGroupIds = getAllGroupIds();

		for (final Integer groupId : userGroupIds) {
			refreshGroupLocal(groupId);
		}

		// refresh all roles
		List<Integer> roleIds = getAllRoleIds();

		for (Integer roleId : roleIds) {
			refreshRoleLocal(roleId);
		}
	}

	/**
	 * Get all usergroup ids
	 * @return list of usergroup ids
	 * @throws NodeException
	 */
	protected List<Integer> getAllGroupIds() throws NodeException {
		final List<Integer> userGroupIds = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT id FROM usergroup", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					userGroupIds.add(rs.getInt("id"));
				}
			}
		});
		return userGroupIds;
	}

	/**
	 * Get all role ids
	 * @return list of role ids
	 * @throws NodeException
	 */
	protected List<Integer> getAllRoleIds() throws NodeException {
		final List<Integer> roleIds = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT id FROM role", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					roleIds.add(rs.getInt("id"));
				}
			}
		});
		return roleIds;
	}

	/**
	 * Get the group permissions map for the given group id. If none exists, create a new one.
	 * @param groupId group id
	 * @return group permissions map
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<Integer, Map<Integer, Permissions>> getGroupPerms(int groupId) {
		Map<Integer, Map<Integer, Permissions>> groupPerms = perms.get(groupId);
		if (groupPerms == null) {
			groupPerms = Collections.synchronizedMap(new THashMap());
			perms.put(groupId, groupPerms);
		}
		return groupPerms;
	}

	/**
	 * Get the group role permissions map for the given group id. If non exists, create a new one.
	 * @param groupId group id
	 * @return group permissions map
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<Integer, Map<Integer, Set<Integer>>> getRoleGroupPerms(int groupId) {
		Map<Integer, Map<Integer, Set<Integer>>> groupPerms = rolePerms.get(groupId);
		if (groupPerms == null) {
			groupPerms = Collections.synchronizedMap(new THashMap());
			rolePerms.put(groupId, groupPerms);
		}
		return groupPerms;
	}

	/**
	 * Get the type permissions map from the given group permissions map for the given type. If none exists, create a new one.
	 * @param groupPerms group permissions map
	 * @param objType object type
	 * @return type permissions map
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<Integer, Permissions> getTypePerms(Map<Integer, Map<Integer, Permissions>> groupPerms, int objType) {
		Map<Integer, Permissions> typePerms = groupPerms.get(objType);
		if (typePerms == null) {
			typePerms = Collections.synchronizedMap(new THashMap());
			groupPerms.put(objType, typePerms);
		}
		return typePerms;
	}

	/**
	 * Get the role type permissions map from the given role group permissions map for the given type. If none exists, create a new one.
	 * @param groupRolePerms role group permissions map
	 * @param objType object type
	 * @return role type permissions map
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<Integer, Set<Integer>> getRoleTypePerms(Map<Integer, Map<Integer, Set<Integer>>> groupRolePerms, int objType) {
		Map<Integer, Set<Integer>> typePerms = groupRolePerms.get(objType);
		if (typePerms == null) {
			typePerms = Collections.synchronizedMap(new THashMap());
			groupRolePerms.put(objType, typePerms);
		}
		return typePerms;
	}

	/**
	 * Get the merged permission bits for the given groups on the given object type.
	 * This method must be used on the types that don't have specific object instances, but manage permissions on the types in the tree.
	 * @param groupIds list of group ids
	 * @param objectType object type
	 * @return merged permission bits
	 * @throws NodeException
	 */
	public Permissions getMergedPermissions(List<Integer> groupIds, int objectType) throws NodeException {
		List<Permissions> collectedPerms = new ArrayList<Permissions>();
		for (Integer groupId : groupIds) {
			Map<Integer, Map<Integer, Permissions>> groupPerms = perms.get(groupId);
			if (groupPerms != null) {
				Map<Integer, Permissions> typePerms = groupPerms.get(objectType);
				if (typePerms != null) {
					collectedPerms.addAll(typePerms.values());
				}
			}
		}

		return Permissions.merge(collectedPerms);
	}

	/**
	 * Get the merged permission bits for the given groups on the given object
	 * @param groupIds list of group ids
	 * @param objectType object type
	 * @param objectId object id
	 * @param checkType type, for which the check shall be done (for getting the correct role permissions)
	 * @param checkLangId language id, for which to do the check (for getting the correct role permissions)
	 * @return merged permission bits (for group and role permissions)
	 * @throws NodeException
	 */
	public PermissionPair getMergedPermissions(List<Integer> groupIds, int objectType, int objectId, int checkType, int checkLangId) throws NodeException {
		PermissionPair returnValue = new PermissionPair();

		List<Permissions> collectedPerms = new ArrayList<Permissions>();
		for (Integer groupId : groupIds) {
			Map<Integer, Map<Integer, Permissions>> groupPerms = perms.get(groupId);
			if (groupPerms != null) {
				Map<Integer, Permissions> typePerms = groupPerms.get(objectType);
				if (typePerms != null) {
					Permissions objectPerms = typePerms.get(objectId);
					if (objectPerms != null) {
						collectedPerms.add(objectPerms);
					}
				}
			}
		}

		returnValue.setGroupPermissions(Permissions.merge(collectedPerms));

		if (roleCheckTypes.contains(checkType) && TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.ROLES)) {
			collectedPerms.clear();
			for (Integer groupId : groupIds) {
				Map<Integer, Map<Integer, Set<Integer>>> groupPerms = rolePerms.get(groupId);
				if (groupPerms != null) {
					Map<Integer, Set<Integer>> typePerms = groupPerms.get(objectType);
					if (typePerms != null) {
						Set<Integer> roleIds = typePerms.get(objectId);
						if (roleIds != null) {
							for (int roleId : roleIds) {
								RolePermissions rolePerms = roles.get(roleId);
								if (rolePerms != null) {
									switch (checkType) {
									case File.TYPE_FILE:
									case ImageFile.TYPE_IMAGE:
										Permissions filePerms = rolePerms.getFilePerm();
										if (filePerms != null) {
											collectedPerms.add(filePerms);
										}
										break;
									case Page.TYPE_PAGE:
										Permissions langPerms = rolePerms.getPagePerm(checkLangId);
										if (langPerms != null) {
											collectedPerms.add(langPerms);
										}
										// always add the permissions for all languages
										if (checkLangId != 0) {
											langPerms = rolePerms.getPagePerm();
											if (langPerms != null) {
												collectedPerms.add(langPerms);
											}
										}
										break;
									}
								}
							}
						}
					}
				}
			}
			returnValue.setRolePermissions(Permissions.merge(collectedPerms));
		}

		return returnValue;
	}

	/**
	 * Determine the group IDs for all groups with the required permission
	 * @param objectType object type
	 * @param objectId object ID
	 * @param type permission type
	 * @param checkType type of the object, for which the permission is checked (for checking roles)
	 * @param checkLangId optional checked language (for pages)
	 * @return set of group IDs
	 * @throws NodeException
	 */
	public Set<Integer> getGroupsWithPerm(int objectType, int objectId, PermType type, int checkType, int checkLangId) throws NodeException {
		Set<Integer> groupIds = new HashSet<>();
		for (Entry<Integer, Map<Integer, Map<Integer, Permissions>>> entry : perms.entrySet()) {
			int groupId = entry.getKey();
			Map<Integer, Map<Integer, Permissions>> typeMap = entry.getValue();

			if (Permissions.check(typeMap.getOrDefault(objectType, Collections.emptyMap()).getOrDefault(objectId, null), type.getBit())) {
				groupIds.add(groupId);
			}
		}

		if (roleCheckTypes.contains(checkType) && NodeConfigRuntimeConfiguration.isFeature(Feature.ROLES)) {
			for (Entry<Integer, Map<Integer, Map<Integer, Set<Integer>>>> entry : rolePerms.entrySet()) {
				int groupId = entry.getKey();

				// no need to check the group, if it already is contained in the set
				if (groupIds.contains(groupId)) {
					continue;
				}

				Map<Integer, Map<Integer, Set<Integer>>> typeMap = entry.getValue();
				Set<Integer> roleIdSet = typeMap.getOrDefault(objectType, Collections.emptyMap()).getOrDefault(objectId, Collections.emptySet());
				for (Integer roleId : roleIdSet) {
					RolePermissions rolePermissions = roles.get(roleId);
					if (rolePermissions != null) {
						switch (checkType) {
						case File.TYPE_FILE:
						case ImageFile.TYPE_IMAGE:
							Permissions filePerms = rolePermissions.getFilePerm();
							if (Permissions.check(filePerms, type.getFileRoleBit())) {
								groupIds.add(groupId);
							}
							break;
						case Page.TYPE_PAGE:
							Permissions langPerms = rolePermissions.getPagePerm(checkLangId);
							if (Permissions.check(langPerms, type.getPageRoleBit())) {
								groupIds.add(groupId);
							} else {
								// check the permissions for all languages
								if (checkLangId != 0) {
									langPerms = rolePermissions.getPagePerm();
									if (Permissions.check(langPerms, type.getPageRoleBit())) {
										groupIds.add(groupId);
									}
								}
							}
							break;
						}
					}
				}
			}
		}

		return groupIds;
	}

	/**
	 * Refresh the stored permissions for the given group.
	 * This method expects a currently running transaction
	 * @param groupId group id
	 * @throws NodeException
	 */
	public void refreshGroup(final int groupId) throws NodeException {
		refreshGroupLocal(groupId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.refreshGroup(groupId);
		}
	}

	/**
	 * Refresh the stored permissions for the given group.
	 * This method expects a currently running transaction
	 * @param groupId group id
	 * @throws NodeException
	 */
	public void refreshGroupLocal(final int groupId) throws NodeException {
		long start = System.currentTimeMillis();

		@SuppressWarnings("unchecked")
		final Map<Integer, Map<Integer, Permissions>> groupPerm = Collections.synchronizedMap(new THashMap());

		DBUtils.executeStatement("SELECT o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, groupId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer oType = rs.getInt("o_type");
					Integer oId = rs.getInt("o_id");
					Permissions p = Permissions.get(rs.getString("perm"));

					Map<Integer, Permissions> typePerm = getTypePerms(groupPerm, oType);
					if (p != null) {
						typePerm.put(oId, p);
					} else {
						typePerm.remove(oId);
					}
				}
			}
		});
		perms.put(groupId, groupPerm);

		@SuppressWarnings("unchecked")
		final Map<Integer, Map<Integer, Set<Integer>>> groupRolePerm = Collections.synchronizedMap(new THashMap());

		// refresh roles for the group
		DBUtils.executeStatement("SELECT ru.role_id, rua.obj_id, rua.obj_type FROM role_usergroup ru INNER JOIN "
				+ "role_usergroup_assignment rua ON ru.id = rua.role_usergroup_id WHERE ru.usergroup_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, groupId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer oType = rs.getInt("obj_type");
					Integer oId = rs.getInt("obj_id");
					Integer roleId = rs.getInt("role_id");

					Map<Integer, Set<Integer>> roleTypePerms = getRoleTypePerms(groupRolePerm, oType);
					Set<Integer> roleSet = getRoleSet(roleTypePerms, oId);
					roleSet.add(roleId);
				}
			}
		});
		rolePerms.put(groupId, groupRolePerm);

		if (logger.isInfoEnabled()) {
			long duration = System.currentTimeMillis() - start;
			logger.info("Refreshed permissions for group " + groupId + " in " + duration + " ms");
		}
	}

	/**
	 * Refresh the stored permissions for the given object for all groups
	 * This method expects a currently running transaction and will also call {@link PermissionStoreService#refreshObject(int, int)} on all found instances
	 * @param objType object type
	 * @param objId object id
	 * @throws NodeException
	 */
	public void refreshObject(final int objType, final int objId) throws NodeException {
		refreshObjectLocal(objType, objId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.refreshObject(objType, objId);
		}
	}

	/**
	 * Refresh the stored permissions for the given object for all groups
	 * This method expects a currently running transaction
	 * @param objType object type
	 * @param objId object id
	 * @throws NodeException
	 */
	public void refreshObjectLocal(final int objType, final int objId) throws NodeException {
		long start = System.currentTimeMillis();

		// get all existing group ids
		final List<Integer> groupIds = getAllGroupIds();
		final List<Integer> remainingGroupIds = new ArrayList<Integer>(groupIds);

		// get all existing permissions on the object
		DBUtils.executeStatement("SELECT usergroup_id, perm FROM perm WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, objType);
				stmt.setInt(2, objId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					// set the permissions
					Integer groupId = rs.getInt("usergroup_id");
					if (!groupIds.contains(groupId)) {
						// omit the perm, if the group does not exist
						continue;
					}
					Permissions p = Permissions.get(rs.getString("perm"));
					Map<Integer, Map<Integer, Permissions>> groupPerms = getGroupPerms(groupId);
					Map<Integer, Permissions> typePerms = getTypePerms(groupPerms, objType);
					if (p != null) {
						typePerms.put(objId, p);
					} else {
						typePerms.remove(objId);
					}
					// remove the group from the list of the remaining group ids
					remainingGroupIds.remove(groupId);
				}
			}
		});

		// for all remaining groups (groups for which no permission was found), we remove the setting
		for (Integer groupId : remainingGroupIds) {
			Map<Integer, Map<Integer, Permissions>> groupPerms = getGroupPerms(groupId);
			Map<Integer, Permissions> typePerms = getTypePerms(groupPerms, objType);
			typePerms.remove(objId);
		}

		remainingGroupIds.clear();
		remainingGroupIds.addAll(groupIds);

		// refresh roles
		DBUtils.executeStatement("SELECT ru.role_id, ru.usergroup_id FROM role_usergroup ru INNER JOIN "
				+ "role_usergroup_assignment rua ON ru.id = rua.role_usergroup_id WHERE rua.obj_type = ? AND rua.obj_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, objType);
				stmt.setInt(2, objId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer groupId = rs.getInt("usergroup_id");
					Integer roleId = rs.getInt("role_id");

					Map<Integer, Map<Integer, Set<Integer>>> groupPerms = getRoleGroupPerms(groupId);
					Map<Integer, Set<Integer>> typePerms = getRoleTypePerms(groupPerms, objType);
					Set<Integer> roleSet = getRoleSet(typePerms, objId);
					roleSet.add(roleId);

					// remove the group from the list of the remaining group ids
					remainingGroupIds.remove(groupId);
				}
			}
		});

		// for all remaining groups (groups for which no permission was found), we remove the setting
		for (Integer groupId : remainingGroupIds) {
			Map<Integer, Map<Integer, Set<Integer>>> groupPerms = getRoleGroupPerms(groupId);
			Map<Integer, Set<Integer>> typePerms = getRoleTypePerms(groupPerms, objType);
			typePerms.remove(objId);
		}

		if (logger.isInfoEnabled()) {
			long duration = System.currentTimeMillis() - start;
			logger.info("Refreshed permissions for object " + objType + "." + objId + " in " + duration + " ms");
		}
	}

	/**
	 * Get the set of active roles for a given object out of the typePerms map. If it doesn't exists, a new one is created
	 * @param typePerms type permission map
	 * @param objId object to lookup
	 * @return set of active roles on the requested object
	 */
	protected Set<Integer> getRoleSet(Map<Integer, Set<Integer>> typePerms, int objId) {
		Set<Integer> roleSet = typePerms.get(objId);
		if (roleSet == null) {
			roleSet = Collections.synchronizedSet(new THashSet());
			typePerms.put(objId, roleSet);
		}
		return roleSet;
	}

	/**
	 * Refresh the given role
	 * This will also call {@link PermissionStoreService#refreshRole(int)} on all found instances
	 * @param roleId role id
	 * @throws NodeException
	 */
	public void refreshRole(final int roleId) throws NodeException {
		refreshRoleLocal(roleId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.refreshRole(roleId);
		}
	}

	/**
	 * Refresh the given role
	 * @param roleId role id
	 * @throws NodeException
	 */
	public void refreshRoleLocal(final int roleId) throws NodeException {
		final Map<Integer, Permissions> foundPermissions = new HashMap<Integer, Permissions>();
		final Map<Integer, Integer> permissionType = new HashMap<Integer, Integer>();
		final Map<Integer, Integer> permissionLangs = new HashMap<Integer, Integer>();
		DBUtils.executeStatement(
				"SELECT rp.id, rp.perm, rpo.obj_type, rpo.obj_id FROM roleperm rp INNER JOIN roleperm_obj rpo ON rp.id = rpo.roleperm_id WHERE rp.role_id = ?",
			new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, roleId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int id = rs.getInt("id");
						String perm = rs.getString("perm");
						int type = rs.getInt("obj_type");
						int langId = rs.getInt("obj_id");
						foundPermissions.put(id, Permissions.get(perm));
						switch (type) {
						case Page.TYPE_PAGE:
							if (!permissionType.containsKey(id)) {
								permissionType.put(id, type);
							}
							break;
						case ContentLanguage.TYPE_CONTENTGROUP:
							permissionType.put(id, type);
							permissionLangs.put(id, langId);
							break;
						case File.TYPE_FILE:
							permissionType.put(id, type);
							break;
						}
					}
				}
			});
		RolePermissions rolePerm = new RolePermissions();
		for (Map.Entry<Integer, Permissions> found : foundPermissions.entrySet()) {
			Integer id = found.getKey();
			Permissions perm = found.getValue();
			Integer type = permissionType.get(id);
			switch (type) {
			case Page.TYPE_PAGE:
				rolePerm.setPagePerm(0, perm);
				break;
			case ContentLanguage.TYPE_CONTENTGROUP:
				rolePerm.setPagePerm(permissionLangs.get(id), perm);
				break;
			case File.TYPE_FILE:
				rolePerm.setFilePerm(perm);
				break;
			}
		}
		roles.put(roleId, rolePerm);
	}

	/**
	 * Get permissions set on the given role
	 * @param roleId role ID
	 * @return permissions
	 * @throws NodeException
	 */
	public RolePermissions getRolePerm(int roleId) throws NodeException {
		return roles.getOrDefault(roleId, new RolePermissions());
	}

	/**
	 * Remove all stored permissions for the given group.
	 * This will also call {@link PermissionStoreService#removeGroup(int)} on all found instances
	 * @param groupId group id
	 * @throws NodeException
	 */
	public void removeGroup(int groupId) throws NodeException {
		removeGroupLocal(groupId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.removeGroup(groupId);
		}
	}

	/**
	 * Remove all stored permissions for the given group
	 * @param groupId group id
	 * @throws NodeException
	 */
	public void removeGroupLocal(int groupId) throws NodeException {
		perms.remove(groupId);
		rolePerms.remove(groupId);
	}

	/**
	 * Remove all stored permissions for the given object.
	 * This will also call {@link PermissionStoreService#removeObject(int, int)} on all found instances
	 * @param objType object type
	 * @param objId object id
	 */
	public void removeObject(final int objType, final int objId) {
		removeObjectLocal(objType, objId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.removeObject(objType, objId);
		}
	}

	/**
	 * Remove all stored permissions for the given object
	 * @param objType object type
	 * @param objId object id
	 */
	public void removeObjectLocal(final int objType, final int objId) {
		for (Map<Integer, Map<Integer, Permissions>> groupPerms : perms.values()) {
			Map<Integer, Permissions> typePerms = getTypePerms(groupPerms, objType);
			typePerms.remove(objId);
		}
		for (Map<Integer, Map<Integer, Set<Integer>>> groupPerms : rolePerms.values()) {
			Map<Integer, Set<Integer>> typePerms = getRoleTypePerms(groupPerms, objType);
			typePerms.remove(objId);
		}
	}

	/**
	 * Remove all stored permissions for the given role.
	 * This will also call {@link PermissionStoreService#removeRole(int)} on all found instances
	 * @param roleId role id
	 */
	public void removeRole(final int roleId) {
		removeRoleLocal(roleId);
		for (PermissionStoreService service : permissionStoreServiceLoader) {
			service.removeRole(roleId);
		}
	}

	/**
	 * Remove all stored permissions for the given role
	 * @param roleId role id
	 */
	public void removeRoleLocal(final int roleId) {
		rolePerms.remove(roleId);
		for (Map<Integer, Map<Integer, Set<Integer>>> rolePermForGroup : rolePerms.values()) {
			for (Map<Integer, Set<Integer>> rolePermForGroupAndType : rolePermForGroup.values()) {
				for (Iterator<Map.Entry<Integer, Set<Integer>>> i = rolePermForGroupAndType.entrySet().iterator(); i.hasNext();) {
					Entry<Integer, Set<Integer>> entry = i.next();
					entry.getValue().remove(roleId);
					if (entry.getValue().isEmpty()) {
						i.remove();
					}
				}
			}
		}
	}

	/**
	 * Get the number of permissions stored in the PermissionStore
	 * @return number of stored permissions
	 */
	public int getCount() {
		int count = 0;
		for (Map<Integer, Map<Integer, Permissions>> groupPerms : perms.values()) {
			for (Map<Integer, Permissions> typePerms : groupPerms.values()) {
				count += typePerms.size();
			}
		}
		return count;
	}

	/**
	 * Check consistency of the PermissionStore
	 * @param fullCheck true to do a full check
	 * @param output output builder that will get information appended
	 * @param htmlOutput true to generate html output
	 * @return true if the PermissionStore is consistent, false if not
	 * @throws NodeException
	 */
	public boolean checkConsistency(boolean fullCheck, final StringBuilder output, final boolean htmlOutput) throws NodeException {
		final boolean[] ok = new boolean[] {true};
		final List<Integer> groupIds = getAllGroupIds();

		if (!groupIds.isEmpty()) {
			DBUtils.executeStatement(
					"SELECT count(DISTINCT usergroup_id, o_type, o_id) c FROM perm WHERE perm != ? AND usergroup_id IN ("
							+ StringUtils.repeat("?", groupIds.size(), ",") + ")", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int counter = 1;
							stmt.setString(counter++, StringUtils.repeat("0", 32));

							for (Integer groupId : groupIds) {
								stmt.setInt(counter++, groupId);
							}
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							if (rs.next()) {
								int storedPermissions = rs.getInt("c");
								int cachedPermissions = PermissionStore.getInstance().getCount();
								if (storedPermissions == cachedPermissions) {
									ok[0] = true;
								} else {
									ok[0] = false;
								}
								output.append(" stored ").append(storedPermissions).append(" vs. cached ").append(cachedPermissions);
							} else {
								ok[0] = false;
								output.append("Could not count stored permissions");
							}
						}
					});
			// if a full check was requested, we read and compare all permissions now
			if (fullCheck) {
				for (Integer groupId : groupIds) {
					ok[0] &= checkGroupConsistency(groupId, output, htmlOutput);
				}
			}
		} else {
			ok[0] = true;
			output.append("no groups found, nothing to check");
		}

		return ok[0];
	}

	/**
	 * Check full consistency for the given group
	 * @param groupId group id
	 * @param output output builder that will get information appended
	 * @param htmlOutput true to generate html output
	 * @return true if the PermissionStore is consistent for the given group, false if not
	 * @throws NodeException
	 */
	protected boolean checkGroupConsistency(final int groupId, final StringBuilder output, final boolean htmlOutput) throws NodeException {
		final boolean[] ok = new boolean[] {true};
		final Map<Integer, Map<Integer, Permissions>> groupPerm = getGroupPerms(groupId);

		final Map<Integer, Set<Integer>> foundPerms = new HashMap<Integer, Set<Integer>>();

		// check whether all stored perms are found in the cache
		DBUtils.executeStatement("SELECT o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, groupId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Integer oType = rs.getInt("o_type");
					Integer oId = rs.getInt("o_id");
					Permissions p = Permissions.get(rs.getString("perm"));

					Map<Integer, Permissions> typePerm = getTypePerms(groupPerm, oType);
					if (p != null) {
						// get the set of found id's per type
						Set<Integer> idSet = foundPerms.get(oType);
						if (idSet == null) {
							idSet = new HashSet<Integer>();
							foundPerms.put(oType, idSet);
						}
						idSet.add(oId);
						if (!p.equals(typePerm.get(oId))) {
							ok[0] = false;
							if (htmlOutput) {
								output.append("<br/>");
							} else {
								output.append("\n");
							}
							output.append("mismatch for group ").append(groupId).append(" type ").append(oType).append(" id ").append(oId);
							output.append(": cached ").append(typePerm.get(oId)).append(", stored ").append(p);
						}
					}
				}
			}
		});

		// now do the reverse check, check whether all cached perms are also found in the DB
		for (Map.Entry<Integer, Map<Integer, Permissions>> typePermEntry : groupPerm.entrySet()) {
			Integer oType = typePermEntry.getKey();
			Set<Integer> idSet = foundPerms.get(oType);
			if (idSet == null) {
				idSet = new HashSet<Integer>();
				foundPerms.put(oType, idSet);
			}

			for (Map.Entry<Integer, Permissions> oPermEntry : typePermEntry.getValue().entrySet()) {
				Integer oId = oPermEntry.getKey();
				if (!idSet.contains(oId)) {
					ok[0] = false;
					if (htmlOutput) {
						output.append("<br/>");
					} else {
						output.append("\n");
					}
					output.append("mismatch for group ").append(groupId).append(" type ").append(oType).append(" id ").append(oId);
					output.append(": cached ").append(oPermEntry.getValue()).append(", stored ").append((String)null);
				}
			}
		}

		return ok[0];
	}

	/**
	 * Get all folder IDs that have permissions set for the given group ID
	 * @param groupId group ID
	 * @return set of folder IDs
	 */
	public Set<Integer> getFolderIdsWithPermissions(int groupId) {
		Set<Integer> folderIds = new HashSet<>();
		Map<Integer, Map<Integer, Permissions>> groupMap = perms.getOrDefault(groupId, Collections.emptyMap());
		folderIds.addAll(groupMap.getOrDefault(Node.TYPE_NODE, Collections.emptyMap()).keySet());
		folderIds.addAll(groupMap.getOrDefault(Folder.TYPE_FOLDER, Collections.emptyMap()).keySet());
		folderIds.addAll(groupMap.getOrDefault(Node.TYPE_CHANNEL, Collections.emptyMap()).keySet());

		return folderIds;
	}

	/**
	 * Reset the permission store singleton
	 */
	public static void reset() {
		singleton = null;
	}
}
