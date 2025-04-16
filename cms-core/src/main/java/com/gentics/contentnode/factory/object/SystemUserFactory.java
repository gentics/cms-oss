/*
 * @author clemens
 * @date 23.01.2007
 * @version $Id: SystemUserFactory.java,v 1.6.2.1.4.1 2011-03-14 15:12:25 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractSystemUser;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

@DBTables({ @DBTable(clazz = SystemUser.class, name = "systemuser") })
public class SystemUserFactory extends AbstractFactory {

	/**
	 * SQL Statement for inserting a new systemuser
	 */
	protected static final String INSERT_SYSTEMUSER_SQL
	= "INSERT INTO systemuser (firstname, lastname, login, password, email, bonus, active, creator,"
			+ " cdate, editor, edate, description, isldapuser, inboxtoemail, support_user)"
			+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	/**
	 * SQL Statement for updating a systemuser
	 */
	protected static final String UPDATE_SYSTEMUSER_SQL
	= "UPDATE systemuser SET firstname = ?, lastname = ?, login = ?, password = ?, email = ?,"
			+ " bonus = ?, active = ?, editor = ?, edate = ?, description = ?, isldapuser = ?,"
			+ " inboxtoemail = ? WHERE id = ?";

	/**
	 * The number of log rounds to hash passwords. 10 = default.
	 * On my system this took ~100ms to hash a single password.
	 * This value can be changed at any time, because the number is part
	 * of the password hash string.
	 */
	public static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	/**
	 * Prefix used for marking legacy bcrypt(md5()) hashed passwords
	 * in the database
	 */
	public static final String LEGACY_PASSWORD_PREFIX = "leg-";

	/**
	 * Length of above
	 */
	public static final int LEGACY_PASSWORD_PREFIX_LENGTH = LEGACY_PASSWORD_PREFIX.length();

	/**
	 * Instance of BCryptPasswordEncoder
	 */
	private static BCryptPasswordEncoder bCryptPasswordEncoder
	= new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);


	private static class FactorySystemUser extends AbstractSystemUser {

		protected String firstname;
		protected String lastname;
		protected String login;
		protected String password;
		protected String email;
		protected String description;
		protected int bonus;
		protected int active;
		protected int creatorId;
		protected ContentNodeDate cdate;
		protected int editorId;
		protected ContentNodeDate edate;
		protected int isLdapUser;
		protected boolean inboxToEmail;
		protected boolean supportUser;

		/**
		 * List of usergroup ids of the user
		 */
		protected List<Integer> userGroupIds;

		/**
		 * Create a new, empty instance of a systemuser
		 * @param info
		 */
		protected FactorySystemUser(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 *
		 * @param id
		 * @param firstname
		 * @param lastname
		 * @param login
		 * @param password
		 * @param email
		 * @param description
		 * @param bonus
		 * @param active
		 * @param creatorId
		 * @param cdate
		 * @param editorId
		 * @param edate
		 * @param isLdapUser
		 * @param inboxToEmail
		 */
		public FactorySystemUser(Integer id, NodeObjectInfo info, String firstname, String lastname, String login, String password,
				String email, String description, int bonus, int active, int creatorId,
				ContentNodeDate cdate, int editorId, ContentNodeDate edate, int isLdapUser, boolean inboxToEmail, boolean supportUser) {
			super(id, info);

			this.firstname = firstname;
			this.lastname = lastname;
			this.login = login;
			this.password = password;
			this.email = email;
			this.description = description;
			this.bonus = bonus;
			this.active = active;
			this.creatorId = creatorId;
			this.cdate = cdate;
			this.editorId = editorId;
			this.edate = edate;
			this.isLdapUser = isLdapUser;
			this.inboxToEmail = inboxToEmail;
			this.supportUser = supportUser;
		}

		/**
		 * well, it's an id...
		 */
		private static final long serialVersionUID = -3422495155334964553L;

		@Override
		public String getFirstname() {
			return firstname;
		}

		@Override
		public String getLastname() {
			return lastname;
		}

		@Override
		public String getLogin() {
			return login;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public String getEmail() {
			return email;
		}

		@Override
		public int getBonus() {
			return bonus;
		}

		@Override
		public int getActive() {
			return active;
		}

		@Override
		public int getCreator() {
			return creatorId;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cdate;
		}

		@Override
		public int getEditor() {
			return editorId;
		}

		@Override
		public ContentNodeDate getEDate() {
			return edate;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public int getIsLDAPUser() {
			return isLdapUser;
		}

		@Override
		public boolean isInboxToEmail() {
			return inboxToEmail;
		}

		@Override
		public boolean isSupportUser() {
			return supportUser;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "SystemUser {" + getFirstname() + " " + getLastname() + ", " + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		@Override
		public NodeObject copy() throws NodeException {
			// TODO implement this
			return null;
		}

		/**
		 * Get the list of usergroup ids
		 * @return list of usergroup ids
		 */
		protected synchronized List<Integer> getUserGroupIds() throws NodeException {
			if (userGroupIds == null) {
				if (isEmptyId(getId())) {
					userGroupIds = new ArrayList<>();
				} else {
					userGroupIds = DBUtils.select("SELECT usergroup_id id FROM user_group WHERE user_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}

			return userGroupIds;
		}

		@Override
		public List<UserGroup> getUserGroups(Function<PermHandler, Boolean> permissionFilter) throws NodeException {
			List<UserGroup> groups = new ArrayList<>(getUserGroups((Node)null));

			if (permissionFilter != null) {
				Transaction t = TransactionManager.getCurrentTransaction();
				for (Iterator<UserGroup> iterator = groups.iterator(); iterator.hasNext();) {
					UserGroup userGroup = iterator.next();
					if (!permissionFilter.apply(t.getGroupPermHandler(userGroup.getId()))) {
						iterator.remove();
					}
				}
			}

			return groups;
		}

		@Override
		public List<UserGroup> getUserGroups(Node node) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = new ArrayList<UserGroup>(t.getObjects(UserGroup.class, getUserGroupIds()));

			if (node != null) {
				Map<Integer, Set<Integer>> restrictions = getGroupNodeRestrictions();
				for (Iterator<UserGroup> i = groups.iterator(); i.hasNext();) {
					UserGroup group = i.next();
					if (restrictions.containsKey(group.getId())) {
						if (!restrictions.get(group.getId()).contains(node.getId())) {
							i.remove();
						}
					}
				}
			}

			return groups;
		}

		@Override
		public Map<Integer, Set<Integer>> getGroupNodeRestrictions() throws NodeException {
			final Map<Integer, Set<Integer>> restrictionMap = new HashMap<Integer, Set<Integer>>();
			DBUtils.executeStatement(
					"select ug.usergroup_id, ugn.node_id from user_group ug, user_group_node ugn where ug.id = ugn.user_group_id and ug.user_id = ?",
					new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setObject(1, getId()); // ug.user_id = ?
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								int groupId = rs.getInt("usergroup_id");
								int nodeId = rs.getInt("node_id");

								Set<Integer> nodeSet = restrictionMap.get(groupId);
								if (nodeSet == null) {
									nodeSet = new HashSet<Integer>();
									restrictionMap.put(groupId, nodeSet);
								}
								nodeSet.add(nodeId);
							}
						}
					});
			return Collections.unmodifiableMap(restrictionMap);
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			// deactivate user
			DBUtils.executeUpdate("UPDATE systemuser SET active = ?, login = ? WHERE id = ?", new Object[] {0, "", getId()});

			List<Integer> groupIds = getUserGroupIds();

			// remove user from all groups
			DBUtils.deleteWithPK("user_group", "id", "user_id = ?", new Object[] {getId()});

			ActionLogger.logCmd(ActionLogger.DEL, SystemUser.TYPE_SYSTEMUSER, getId(), 0, "SystemUser.delete");
			Transaction t = TransactionManager.getCurrentTransaction();
			t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, getId(), null, Events.DELETE));
			for (Integer groupId : groupIds) {
				t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, groupId, null, Events.UPDATE));
			}
		}
	}

	/**
	 * Editable class for SystemUsers
	 */
	private static class EditableFactorySystemUser extends FactorySystemUser {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 6704662938754630006L;

		/**
		 * Flag to mark whether the systemuser has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		protected boolean modified = false;

		/**
		 * list of usergroups the user is currently assigned to
		 */
		private List<UserGroup> userGroups = null;

		/**
		 * Restrictions to nodes for assignments to groups
		 */
		private Map<Integer, Set<Integer>> usergroupNodeRestrictions = null;

		/**
		 * Create a new systemuser instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactorySystemUser(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Constructor for creating an editable copy of the given systemuser
		 * @param systemUser systemuser
		 * @param info object info
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactorySystemUser(FactorySystemUser systemUser, NodeObjectInfo info) throws ReadOnlyException, NodeException {
			super(systemUser.getId(), info, systemUser.firstname, systemUser.lastname, systemUser.login, systemUser.password, systemUser.email,
					systemUser.description, systemUser.bonus, systemUser.active, systemUser.creatorId, systemUser.cdate, systemUser.editorId, systemUser.edate,
					systemUser.isLdapUser, systemUser.inboxToEmail, systemUser.supportUser);
		}

		@Override
		public List<UserGroup> getUserGroups() throws NodeException {
			if (userGroups == null) {
				userGroups = new Vector<UserGroup>(super.getUserGroups());
			}
			return userGroups;
		}

		@Override
		public Map<Integer, Set<Integer>> getGroupNodeRestrictions() throws NodeException {
			if (usergroupNodeRestrictions == null) {
				usergroupNodeRestrictions = new HashMap<Integer, Set<Integer>>(super.getGroupNodeRestrictions());
			}
			return usergroupNodeRestrictions;
		}

		@Override
		public void setActive(boolean active) throws ReadOnlyException {
			if (active && this.active != 1) {
				this.active = 1;
				this.modified = true;
			} else if (!active && this.active != 0) {
				this.active = 0;
				this.modified = true;
			}
		}

		@Override
		public void setDescription(String description) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.description, description)) {
				this.description = description;
				this.modified = true;
			}
		}

		@Override
		public void setEmail(String email) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.email, email)) {
				this.email = email;
				this.modified = true;
			}
		}

		@Override
		public void setFirstname(String firstname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.firstname, firstname)) {
				this.firstname = firstname;
				this.modified = true;
			}
		}

		@Override
		public void setInboxToEmail(boolean inboxToEmail) throws ReadOnlyException {
			if (this.inboxToEmail != inboxToEmail) {
				this.inboxToEmail = inboxToEmail;
				this.modified = true;
			}
		}

		@Override
		public void setSupportUser(boolean supportUser) throws ReadOnlyException {
			if (this.supportUser != supportUser) {
				this.supportUser = supportUser;
				this.modified = true;
			}
		}

		@Override
		public void setIsLDAPUser(boolean ldapUser) throws ReadOnlyException {
			if (ldapUser && this.isLdapUser != 1) {
				this.isLdapUser = 1;
				this.modified = true;
			} else if (!ldapUser && this.isLdapUser != 0) {
				this.isLdapUser = 0;
				this.modified = true;
			}
		}

		@Override
		public void setLastname(String lastname) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.lastname, lastname)) {
				this.lastname = lastname;
				this.modified = true;
			}
		}

		@Override
		public void setLogin(String login) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.login, login)) {
				this.login = login;
				this.modified = true;
			}
		}

		@Override
		public void setPassword(String password) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.password, password) && !supportUser) {
				this.password = password;
				this.modified = true;
			}
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public boolean save()
				throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();
			boolean isModified = modified;
			boolean isNew = isEmptyId(getId());

			login = ObjectTransformer.getString(login, "").trim();
			if (StringUtils.isEmpty(login)) {
				throw new MissingFieldException("login");
			}
			// check uniqueness of login
			boolean loginConflict = false;
			if (isNew) {
				loginConflict = UniquifyHelper.findConflictingValues(login, "SELECT login FROM systemuser WHERE login = ?");
			} else {
				loginConflict = UniquifyHelper.findConflictingValues(login, "SELECT login FROM systemuser WHERE id != ? AND login = ?", getId());
			}
			if (loginConflict) {
				throw new DuplicateValueException("login", login);
			}

			firstname = ObjectTransformer.getString(firstname, "").trim();
			lastname = ObjectTransformer.getString(lastname, "").trim();
			email = ObjectTransformer.getString(email, "").trim();

			// now check whether the object has been modified
			if (isModified) {
				// object is modified, so update it
				saveSystemUserObject(this);
				modified = false;
				// dirt the file cache
				t.dirtObjectCache(SystemUser.class, getId());
			}

			saveGroupAssignments();
			saveNodeRestrictions();

			return isModified;

		}

		/**
		 * Save the modified group assignments of the user
		 * @throws NodeException
		 */
		protected void saveGroupAssignments() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// find differences in the groups and save them
			List<Integer> groupIdsToAdd = new ArrayList<>();
			List<Integer> groupIdsToRemove = new ArrayList<>();

			// new list of usergroups
			List<UserGroup> userGroups = getUserGroups();

			// currently set usergroups
			List<Integer> userGroupIds = getUserGroupIds();

			if (userGroupIds == null) {
				userGroupIds = Collections.emptyList();
			}
			groupIdsToRemove.addAll(userGroupIds);

			// find groups, which shall be added or removed
			for (UserGroup group : userGroups) {
				groupIdsToRemove.remove(group.getId());
				if (!userGroupIds.contains(group.getId())) {
					groupIdsToAdd.add(getId());
					groupIdsToAdd.add(group.getId());
					groupIdsToAdd.add(t.getUnixTimestamp());
					groupIdsToAdd.add(t.getUserId());
				}
			}

			// now add the missing group assignments
			if (groupIdsToAdd.size() > 0) {
				DBUtils.executeInsert(
						"INSERT INTO user_group (user_id, usergroup_id, cdate, creator) VALUES " + StringUtils.repeat("(?,?,?,?)", groupIdsToAdd.size() / 4, ","),
						groupIdsToAdd.toArray(new Object[groupIdsToAdd.size()]));
				this.userGroupIds = null;

				for (Integer id : groupIdsToAdd) {
					t.dirtObjectCache(UserGroup.class, id);
				}
			}

			// ... and remove the surplus group assignments
			if (groupIdsToRemove.size() > 0) {
				groupIdsToRemove.add(0, getId());
				DBUtils.executeUpdate(
						"DELETE FROM user_group WHERE user_id = ? AND usergroup_id IN (" + StringUtils.repeat("?", groupIdsToRemove.size() - 1, ",") + ")",
						groupIdsToRemove.toArray(new Object[groupIdsToRemove.size()]));
				this.userGroupIds = null;

				for (Integer id : groupIdsToRemove) {
					t.dirtObjectCache(UserGroup.class, id);
				}

			}
		}

		/**
		 * Save the modified node restrictions of user/group assignments
		 * @throws NodeException
		 */
		protected void saveNodeRestrictions() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// when the node restrictions were not changed, there is no need to store something
			if (usergroupNodeRestrictions == null) {
				return;
			}

			// remove all restrictions to nodes that do not exist
			for (Iterator<Map.Entry<Integer, Set<Integer>>> restrictionIter = usergroupNodeRestrictions.entrySet().iterator(); restrictionIter.hasNext(); ) {
				Map.Entry<Integer, Set<Integer>> entry = restrictionIter.next();
				Set<Integer> nodeIds = entry.getValue();
				for (Iterator<Integer> nodeIdIter = nodeIds.iterator(); nodeIdIter.hasNext(); ) {
					int nodeId = nodeIdIter.next();
					if (t.getObject(Node.class, nodeId) == null) {
						logger.warn("Cannot restrict assignment of " + this + " to group " + entry.getKey() + " to node " + nodeId + ": Node does not exist");
						nodeIdIter.remove();
					}
				}

				if (nodeIds.isEmpty()) {
					restrictionIter.remove();
				}
			}

			// get the currently stored restrictions
			Map<Integer, Set<Integer>> currentRestrictions = super.getGroupNodeRestrictions();

			// get the current assignment to groups (keys are the group IDs, values are the entry IDs)
			final Map<Integer, Integer> groupAssignment = new HashMap<Integer, Integer>();
			DBUtils.executeStatement("SELECT id, usergroup_id FROM user_group WHERE user_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId()); // user_id = ?
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						groupAssignment.put(rs.getInt("usergroup_id"), rs.getInt("id"));
					}
				}
			});

			for (Map.Entry<Integer, Set<Integer>> entry : usergroupNodeRestrictions.entrySet()) {
				int groupId = entry.getKey();
				// if the user is not assigned to the group, we ignore the node restriction
				if (!groupAssignment.containsKey(groupId)) {
					continue;
				}
				int assignmentId = groupAssignment.get(groupId);
				Set<Integer> nodeIds = entry.getValue();

				if (currentRestrictions.containsKey(groupId)) {
					Set<Integer> toRemove = new HashSet<Integer>(currentRestrictions.get(groupId));
					toRemove.removeAll(nodeIds);

					if (!toRemove.isEmpty()) {
						List<Integer> params = new ArrayList<Integer>();
						params.add(assignmentId);
						params.addAll(toRemove);
						DBUtils.executeUpdate(
								"DELETE FROM user_group_node WHERE user_group_id = ? AND node_id IN (" + StringUtils.repeat("?", toRemove.size(), ",") + ")",
								params.toArray(new Integer[params.size()]));
					}

					Set<Integer> toInsert = new HashSet<Integer>(nodeIds);
					toInsert.removeAll(currentRestrictions.get(groupId));

					for (int insertNodeId : toInsert) {
						DBUtils.executeInsert("INSERT INTO user_group_node (user_group_id, node_id) VALUES (?, ?)", new Object[] {assignmentId, insertNodeId});
					}
				} else {
					for (int nodeId : nodeIds) {
						DBUtils.executeInsert("INSERT INTO user_group_node (user_group_id, node_id) VALUES (?, ?)", new Object[] {assignmentId, nodeId});
					}
				}
			}

			Set<Integer> removeAssignments = new HashSet<Integer>();
			for (Map.Entry<Integer, Set<Integer>> entry : currentRestrictions.entrySet()) {
				int groupId = entry.getKey();
				if (usergroupNodeRestrictions.containsKey(groupId)) {
					continue;
				}
				if (!groupAssignment.containsKey(groupId)) {
					continue;
				}
				removeAssignments.add(groupAssignment.get(groupId));
			}

			if (!removeAssignments.isEmpty()) {
				DBUtils.executeUpdate("DELETE FROM user_group_node WHERE user_group_id IN (" + StringUtils.repeat("?", removeAssignments.size(), ",") + ")",
						removeAssignments.toArray(new Integer[removeAssignments.size()]));
			}
		}
	}

	public SystemUserFactory() {
		super();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		String firstname = rs.getString("firstname");
		String lastname = rs.getString("lastname");
		String login = rs.getString("login");
		String password = rs.getString("password");
		String email = rs.getString("email");
		String description = rs.getString("description");
		int bonus = rs.getInt("bonus");
		int active = rs.getInt("active");
		int creatorId = rs.getInt("creator");
		ContentNodeDate cdate = new ContentNodeDate(rs.getInt("cdate"));
		int editorId = rs.getInt("editor");
		ContentNodeDate edate = new ContentNodeDate(rs.getInt("edate"));
		int isLdapUser = rs.getInt("isldapuser");
		boolean inboxToEmail = rs.getBoolean("inboxtoemail");
		boolean supportUser = rs.getBoolean("support_user");

		return (T) new FactorySystemUser(id, info, firstname, lastname, login, password, email, description, bonus, active, creatorId, cdate, editorId, edate,
				isLdapUser, inboxToEmail, supportUser);
	}

	@Override
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM systemuser WHERE id IN " + idSql);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (SystemUser.class.equals(clazz)) {
			return (T) new EditableFactorySystemUser(handle.createObjectInfo(SystemUser.class, true));
		} else {
			return null;
		}
	}

	@Override
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, "SELECT * FROM systemuser WHERE id = ?", null, null);
	}

	/**
	 * Get the systemuser with given login and password. If the password is null, just get the user (without checking password)
	 * @param login login
	 * @param password password (may be null)
	 * @param checkPassword Whether to check the passed password for match or not
	 * @return systemuser or null if no match found
	 * @throws NodeException
	 */
	public SystemUser getSystemUser(String login, String password, boolean checkPassword)
			throws NodeException {
		PreparedStatement pst = null;
		ResultSet res = null;
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer id = null;

		try {
			// first get the user with given login
			pst = t.prepareStatement("SELECT id FROM systemuser WHERE login = ? AND active = 1");
			pst.setString(1, login);
			res = pst.executeQuery();

			if (res.first()) {
				// found the user, so get the id
				id = res.getInt("id");
			} else {
				// did not find (active) user, return null
				return null;
			}
			t.closeResultSet(res);
			res = null;
			t.closeStatement(pst);
			pst = null;

			if (checkPassword) {
				// Don't allow null or empty passwords.
				if (password == null || password.isEmpty()) {
					return null;
				}

				// now check for the password
				pst = t.prepareStatement("SELECT id, password FROM systemuser WHERE id = ?");
				pst.setObject(1, id);

				res = pst.executeQuery();

				if (!res.first()) {
					// User not found
					return null;
				}

				int userId                  = res.getInt("id");
				String storedHashedPassword = res.getString("password");

				if (storedHashedPassword == null) {
					return null;
				}

				String givenPassword = password;
				boolean isPasswordLegacy = false;

				// Legacy password logic
				// If the user hasn't logged since the bcrypt password change,
				// his password is bcrypt(md5(password)) encoded. We have to update
				// the password hash without md5() encoding -> bcrypt(password).
				// "leg-" is the shortcut for legacy.
				if (storedHashedPassword.startsWith(LEGACY_PASSWORD_PREFIX)) {
					isPasswordLegacy = true;
					// Md5 hash the user ID with the password and convert to lower case
					// in order to get the old password hash.
					givenPassword = StringUtils.md5(userId + password).toLowerCase();
					// And remove the leg- of the stored one
					storedHashedPassword = storedHashedPassword.substring(LEGACY_PASSWORD_PREFIX_LENGTH);
				}

				if (!passwordMatches(givenPassword, storedHashedPassword)) {
					// Password does not match
					return null;
				}

				if (isPasswordLegacy) {
					// Legacy password transformation
					pst = t.prepareUpdateStatement("UPDATE systemuser SET password = ? WHERE id = ?");
					pst.setString(1, hashPassword(
							password, ObjectTransformer.getInt(id, 0)));
					pst.setObject(2, id);
					pst.executeUpdate();
				}
			}

			// return the systemuser object
			return t.getObject(SystemUser.class, id);
		} catch (SQLException e) {
			throw new NodeException("Error while getting systemuser", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T getEditableCopy(final T object,
			NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactorySystemUser) {
			EditableFactorySystemUser editableCopy = new EditableFactorySystemUser((FactorySystemUser) object, info);

			return (T) editableCopy;
		} else {
			throw new NodeException("SystemUserFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Save the given systemUser object
	 * @param systemUser systemUser
	 * @throws NodeException
	 */
	private static void saveSystemUserObject(EditableFactorySystemUser systemUser) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = AbstractSystemUser.isEmptyId(systemUser.getId());

		if (isNew) {
			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_SYSTEMUSER_SQL,
					new Object[] {
					systemUser.firstname, systemUser.lastname, systemUser.login, systemUser.password, systemUser.email, systemUser.bonus, systemUser.active,
					t.getUserId(), t.getUnixTimestamp(), t.getUserId(), t.getUnixTimestamp(), ObjectTransformer.getString(systemUser.description, ""),
					systemUser.isLdapUser, systemUser.inboxToEmail, systemUser.supportUser});

			if (keys.size() != 1) {
				throw new NodeException("Error while inserting new systemuser, could not get the insertion id");
			}

			// set the new page id
			systemUser.setId(keys.get(0));

			ActionLogger.logCmd(ActionLogger.CREATE, SystemUser.TYPE_SYSTEMUSER, systemUser.getId(), null, "SystemUser.create");
			t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, systemUser.getId(), null, Events.CREATE));
		} else {
			SystemUser origSystemUser = t.getObject(SystemUser.class, systemUser.getId());

			// update existing systemuser
			DBUtils.executeUpdate(UPDATE_SYSTEMUSER_SQL,
					new Object[] {
					systemUser.firstname, systemUser.lastname, systemUser.login, systemUser.password, systemUser.email, systemUser.bonus, systemUser.active,
					t.getUserId(), t.getUnixTimestamp(), ObjectTransformer.getString(systemUser.description, ""), systemUser.isLdapUser, systemUser.inboxToEmail,
					systemUser.getId()});

			ActionLogger.logCmd(ActionLogger.EDIT, SystemUser.TYPE_SYSTEMUSER, systemUser.getId(), 0, "SystemUser.update");

			// Find all attributes that have changed between both systemusers
			List<String> attributes = getChangedProperties(origSystemUser, systemUser);

			t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, systemUser.getId(), attributes.toArray(new String[0]), Events.UPDATE));
		}
	}

	/**
	 * Get list of properties which are different between the two systemuser instances
	 * @param original original systemuser
	 * @param updated update systemuser
	 * @return list of changed properties
	 * @throws NodeException
	 */
	private static List<String> getChangedProperties(SystemUser original, SystemUser updated) throws NodeException {
		List<String> modified = new Vector<String>();

		if (original == null || updated == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("systemuser to compare with was null returning empty property set");
			}
			return modified;
		}

		if (original.getActive() != updated.getActive()) {
			modified.add("active");
		}
		if (original.getBonus() != updated.getBonus()) {
			modified.add("bonus");
		}
		if (!StringUtils.isEqual(original.getDescription(), updated.getDescription())) {
			modified.add("description");
		}
		if (original.getEditor() != updated.getEditor()) {
			modified.add("editor");
		}
		modified.add("edate");
		if (!StringUtils.isEqual(original.getEmail(), updated.getEmail())) {
			modified.add("email");
		}
		if (!StringUtils.isEqual(original.getFirstname(), updated.getFirstname())) {
			modified.add("firstname");
		}
		if (original.getIsLDAPUser() != updated.getIsLDAPUser()) {
			modified.add("isldapuser");
		}
		if (!StringUtils.isEqual(original.getLastname(), updated.getLastname())) {
			modified.add("lastname");
		}
		if (!StringUtils.isEqual(original.getLogin(), updated.getLogin())) {
			modified.add("login");
		}
		if (!StringUtils.isEqual(original.getPassword(), updated.getPassword())) {
			modified.add("password");
		}
		modified.add("edate");

		return modified;
	}

	/**
	 * Hash the given plain text password
	 * @param password  Plain text password
	 * @param userId    User ID of the user
	 * @return Hashed password
	 */
	public static String hashPassword(String password, int userId) {
		return bCryptPasswordEncoder.encode(password);
	}

	/**
	 * Verify the encoded password obtained from storage matches the
	 * submitted raw password after it too is encoded.
	 * Returns true if the passwords match, false if they do not.
	 * The stored password itself is never decoded.
	 *
	 * @param rawPassword the raw password to encode and match
	 * @param encodedPassword the encoded password from storage to compare with
	 * @return true if the raw password, after encoding, matches the encoded password from storage
	 */
	public static boolean passwordMatches(String rawPassword, String encodedPassword) {
		return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
	}
}
