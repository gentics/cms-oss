/*
 * @author norbert
 * @date 11.03.2011
 * @version $Id: UserGroupFactory.java,v 1.1.2.1 2011-03-14 15:12:26 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.CollectionUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.exception.MovingGroupNotPossibleException;
import com.gentics.contentnode.exception.UserWithoutGroupException;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.RefreshPermHandler;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionalRemoveGroupPermissionStore;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.etc.StringUtils;

/**
 * Factory for instances of {@link UserGroup}.
 * @author norbert
 */
@DBTables({ @DBTable(clazz = UserGroup.class, name = "usergroup") })
public class UserGroupFactory extends AbstractFactory {
	static {
		// register the factory classes
		try {
			registerFactoryClass(C.Tables.USERGROUP, UserGroup.TYPE_USERGROUP, false, FactoryUserGroup.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Loader for {@link UserGroupService}s
	 */
	protected final static ServiceLoader<UserGroupService> userGroupServiceLoader = ServiceLoader
			.load(UserGroupService.class);

	/**
	 * Create an instance of the factory
	 */
	public UserGroupFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#loadResultSet(java.lang.Class, java.lang.Integer, com.gentics.lib.base.object.NodeObjectInfo, com.gentics.contentnode.factory.object.FactoryDataRow, java.util.List[])
	 */
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		String name = rs.getString("name");
		String description = rs.getString("description");
		int motherId = rs.getInt("mother");
		int creatorId = rs.getInt("creator");
		ContentNodeDate cdate = new ContentNodeDate(rs.getInt("cdate"));
		int editorId = rs.getInt("editor");
		ContentNodeDate edate = new ContentNodeDate(rs.getInt("edate"));

		return (T) new FactoryUserGroup(id, info, name, description, motherId, creatorId, cdate, editorId, edate);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.BatchObjectFactory#batchLoadObjects(java.lang.Class, java.util.Collection, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM usergroup WHERE id IN " + idSql);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (UserGroup.class.equals(clazz)) {
			return (T) new EditableFactoryUserGroup(handle.createObjectInfo(UserGroup.class, true));
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#loadObject(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, "SELECT * FROM usergroup WHERE id = ?", null, null);
	}

	@Override
	public <T extends NodeObject> T getEditableCopy(final T object,
			NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryUserGroup) {
			EditableFactoryUserGroup editableCopy = new EditableFactoryUserGroup((FactoryUserGroup) object, info);

			return (T) editableCopy;
		} else {
			throw new NodeException("UserGroupFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Delete the groups (add them to the delete list)
	 * @param groups groups to delete
	 * @throws NodeException
	 */
	protected void deleteGroups(Collection<UserGroup> groups) throws NodeException {
		getDeleteList(UserGroup.class).addAll(groups);
	}

	@Override
	public void flush() throws NodeException {
		Collection<UserGroup> deleteList = getDeleteList(UserGroup.class);

		if (!deleteList.isEmpty()) {
			Transaction t =  TransactionManager.getCurrentTransaction();
			Set<Integer> groupIds = deleteList.stream().map(UserGroup::getId).collect(Collectors.toSet());
			Set<Integer> mothers = new HashSet<>(deleteList.stream().map(UserGroup::getMotherId).collect(Collectors.toSet()));
			mothers.removeAll(groupIds);

			// collect all members
			Set<Integer> members = new HashSet<>();
			for (UserGroup group : deleteList) {
				members.addAll(group.getMemberIds());
			}

			String questionMarks = StringUtils.repeat("?", groupIds.size(), ",");
			Object[] ids = (Object[]) groupIds.toArray(new Object[groupIds.size()]);

			// delete user_group assignments
			DBUtils.deleteWithPK("user_group", "id", "usergroup_id IN (" + questionMarks + ")", ids);

			// delete usergroups
			DBUtils.executeUpdate("DELETE FROM usergroup WHERE id IN (" + questionMarks + ")", ids);

			// logcmd and events for deleted groups
			for (Integer id : groupIds) {
				ActionLogger.logCmd(ActionLogger.DEL, UserGroup.TYPE_USERGROUP, id, 0, "UserGroup.delete");
				t.addTransactional(new TransactionalRemoveGroupPermissionStore(id));
				t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, id, null, Events.DELETE));
			}

			// events for mother groups
			for (Integer id : mothers) {
				t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, id, new String[] {"groups"}, Events.UPDATE));
			}

			// events for members
			for (Integer memberId : members) {
				t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, memberId, new String[] {"groups"}, Events.UPDATE));
			}
		}
	}

	/**
	 * Inner class for {@link UserGroup} objects created by this factory
	 */
	private static class FactoryUserGroup extends UserGroup {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -1528740739442655838L;

		/**
		 * Group name
		 */
		@DataField("name")
		@Updateable
		protected String name;

		/**
		 * Group description
		 */
		@DataField("description")
		@Updateable
		protected String description;

		/**
		 * Id of the mother group (or 0 for the root group)
		 */
		@DataField("mother")
		@Updateable
		protected int mother;

		/**
		 * Id of the creator
		 */
		@DataField("creator")
		protected int creatorId;

		/**
		 * Creation date
		 */
		@DataField("cdate")
		protected ContentNodeDate cdate = new ContentNodeDate(0);;

		/**
		 * Id of the editor
		 */
		@DataField("editor")
		@Updateable
		protected int editorId;

		/**
		 * Edit date
		 */
		@DataField("edate")
		@Updateable
		protected ContentNodeDate edate = new ContentNodeDate(0);;

		/**
		 * List of child group ids
		 */
		protected List<Integer> childGroupIds;

		/**
		 * List of group member ids
		 */
		protected List<Integer> memberIds;

		/**
		 * Create a new, empty instance of a group
		 * @param info
		 */
		protected FactoryUserGroup(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance of the object
		 * @param id id of the object
		 * @param info info object
		 */
		protected FactoryUserGroup(Integer id, NodeObjectInfo info, String name, String description, int mother, int creatorId,
				ContentNodeDate cdate, int editorId, ContentNodeDate edate) {
			super(id, info);
			this.name = name;
			this.description = description;
			this.mother = mother;
			this.creatorId = creatorId;
			this.cdate = cdate;
			this.editorId = editorId;
			this.edate = edate;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cdate;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser creator = (SystemUser) t.getObject(SystemUser.class, creatorId);

			assertNodeObjectNotNull(creator, creatorId, "creator");
			return creator;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public ContentNodeDate getEDate() {
			return edate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser editor = (SystemUser) t.getObject(SystemUser.class, editorId);

			assertNodeObjectNotNull(editor, editorId, "editor");
			return editor;
		}

		@Override
		public int getMotherId() {
			return mother;
		}

		@Override
		public UserGroup getMother() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			UserGroup motherGroup = (UserGroup) t.getObject(UserGroup.class, mother);

			assertNodeObjectNotNull(motherGroup, mother, "mother", true);
			return motherGroup;
		}

		/**
		 * Get IDs of the child groups
		 * @return List of IDs of the child groups
		 * @throws NodeException
		 */
		protected synchronized List<Integer> getChildGroupIds() throws NodeException {
			if (childGroupIds == null) {
				if (isEmptyId(getId())) {
					childGroupIds = new ArrayList<>();
				} else {
					childGroupIds = DBUtils.select("SELECT id FROM usergroup WHERE mother = ?", ps -> ps.setInt(1, getId()),
							DBUtils.IDLIST);
				}
			}
			return childGroupIds;
		}

		@Override
		public List<UserGroup> getChildGroups() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(UserGroup.class, getChildGroupIds());
		}

		/**
		 * Get the group member ids
		 * @return group member ids
		 * @throws NodeException
		 */
		public synchronized List<Integer> getMemberIds() throws NodeException {
			if (memberIds == null) {
				if (isEmptyId(getId())) {
					memberIds = new ArrayList<>();
				} else {
					memberIds = DBUtils.select("SELECT user_id id FROM user_group WHERE usergroup_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
			return memberIds;
		}

		@Override
		public List<SystemUser> getMembers() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(SystemUser.class, getMemberIds());
		}

		@Override
		public String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			throw new NodeException("Copying of groups is not yet implemented");
		}

		@Override
		public String toString() {
			return "UserGroup {" + getName() + "," + getId() + "}";
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			// fetch all subgroups
			Set<UserGroup> groupsToDelete = allGroups();

			// get all group members of all groups
			Set<SystemUser> members = new HashSet<>();
			for (UserGroup group : groupsToDelete) {
				members.addAll(group.getMembers());
			}

			// check whether a member would lose all its groups
			for (SystemUser member : members) {
				if (CollectionUtils.subtract(member.getUserGroups(), groupsToDelete).isEmpty()) {
					throw new UserWithoutGroupException(
							I18NHelper.get("exception.group_delete.lastgroup", I18NHelper.getName(this), I18NHelper.getName(member)));
				}
			}

			// add all groups to delete list
			Transaction t = TransactionManager.getCurrentTransaction();
			UserGroupFactory factory = (UserGroupFactory) t.getObjectFactory(UserGroup.class);
			factory.deleteGroups(groupsToDelete);

			onDelete(this, false, t.getUserId());
		}

		@Override
		public List<? extends ExtensibleObjectService<UserGroup>> getServices() {
			return StreamSupport.stream(userGroupServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	/**
	 * Editable class for groups
	 */
	private static class EditableFactoryUserGroup extends FactoryUserGroup {
		/**
		 * Flag to mark whether the systemuser has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		protected boolean modified = false;

		/**
		 * list of group members
		 */
		private List<SystemUser> members = null;

		/**
		 * Create a new group instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactoryUserGroup(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Constructor for creating an editable copy of the given group
		 * @param group group
		 * @param info object info
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactoryUserGroup(FactoryUserGroup group, NodeObjectInfo info) throws ReadOnlyException, NodeException {
			super(group.getId(), info, group.name, group.description, group.mother, group.creatorId, group.cdate, group.editorId, group.edate);
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
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
		public void setMotherId(int motherId) throws ReadOnlyException, NodeException {
			if (this.mother != motherId) {
				// TODO check consistency
				this.mother = motherId;
				this.modified = true;
			}
		}

		@Override
		public List<SystemUser> getMembers() throws NodeException {
			if (members == null) {
				members = new ArrayList<>(super.getMembers());
			}
			return members;
		}

		@Override
		public boolean save()
				throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();
			boolean isModified = modified;
			boolean isNew = isEmptyId(getId());

			name = ObjectTransformer.getString(name, "").trim();
			if (StringUtils.isEmpty(name)) {
				throw new MissingFieldException("name");
			}
			description = ObjectTransformer.getString(description, "").trim();

			// check naming uniqueness
			boolean nameConflict = false;
			if (isNew) {
				nameConflict = UniquifyHelper.findConflictingValues(name, "SELECT name FROM usergroup WHERE mother = ? AND name = ?", getMotherId());
			} else {
				nameConflict = UniquifyHelper.findConflictingValues(name, "SELECT name FROM usergroup WHERE mother = ? AND id != ? AND name = ?", getMotherId(),
						getId());
			}
			if (nameConflict) {
				throw new DuplicateValueException("name", name);
			}

			// now check whether the object has been modified
			if (isModified) {
				// set the editor data
				editorId = t.getUserId();
				edate = new ContentNodeDate(t.getUnixTimestamp());

				// for new objects, set creator data
				if (isNew) {
					creatorId = t.getUserId();
					cdate = new ContentNodeDate(t.getUnixTimestamp());
				}

				// object is modified, so update it
				saveFactoryObject(this);
				modified = false;
				// dirt the file cache
				t.dirtObjectCache(UserGroup.class, getId());

				if (isNew) {
					List<Object[]> perms = DBUtils.select("SELECT o_type, o_id, perm FROM perm WHERE usergroup_id = ?", ps -> ps.setInt(1, mother), rs -> {
						List<Object[]> permData = new ArrayList<>();
						while (rs.next()) {
							permData.add(new Object[] { rs.getInt("o_type"), rs.getInt("o_id"), rs.getString("perm"), getId() });
						}
						return permData;
					});
					DBUtils.executeBatchInsert("INSERT INTO perm (o_type, o_id, perm, usergroup_id) VALUES (?, ?, ?, ?)", perms);

					ActionLogger.logCmd(ActionLogger.CREATE, UserGroup.TYPE_USERGROUP, getId(), 0, "UserGroup.create");
					t.addTransactional(new RefreshPermHandler(getId()));
					t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, getId(), null, Events.CREATE));
					t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, getMotherId(), new String[] {"children"}, Events.UPDATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, UserGroup.TYPE_USERGROUP, getId(), 0, "UserGroup.update");
					t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, getId(), null, Events.UPDATE));
				}

				onSave(this, isNew, false, t.getUserId());
			}
			saveUserAssignments();

			return isModified;
		}

		@Override
		public void move(UserGroup target) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// check whether the group should be moved into one of its subgroups (or into itself)
			if (target.equals(this) || target.getParents().contains(this)) {
				throw new MovingGroupNotPossibleException(
						I18NHelper.get("exception.group_move.subgroup", I18NHelper.getName(this), I18NHelper.getName(target)));
			}

			// check naming uniqueness in target
			if (target.getChildGroups().stream().filter(g -> StringUtils.isEqual(getName(), g.getName())).findFirst().isPresent()) {
				throw new MovingGroupNotPossibleException(
						I18NHelper.get("exception.group_move.duplicatename", I18NHelper.getName(this), I18NHelper.getName(target)));
			}

			int oldMother = getMotherId();

			// do the move (by setting a new mother ID)
			DBUtils.executeUpdate("UPDATE usergroup SET mother = ? WHERE id = ?", new Object[] {target.getId(), getId()});

			// TODO fix permissions of subgroups?

			ActionLogger.logCmd(ActionLogger.MOVE, UserGroup.TYPE_USERGROUP, getId(), target.getId(), "UserGroup.move");
			t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, getId(), new String[] {"mother"}, Events.UPDATE));
			t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, oldMother, new String[] {"children"}, Events.UPDATE));
			t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, target.getId(), new String[] {"children"}, Events.UPDATE));
		}

		/**
		 * Save the modified user assignments of the groups
		 * @throws NodeException
		 */
		protected void saveUserAssignments() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// find differences in the members and save them
			Set<Integer> memberIdsToAdd = new HashSet<>();
			Set<Integer> memberIdsToRemove = new HashSet<>();

			// new list of members
			List<SystemUser> members = getMembers();

			// currently set members
			List<Integer> memberIds = getMemberIds();

			if (memberIds == null) {
				memberIds = Collections.emptyList();
			}
			memberIdsToRemove.addAll(memberIds);

			// find users, which shall be added or removed
			for (SystemUser user : members) {
				memberIdsToRemove.remove(user.getId());
				if (!memberIds.contains(user.getId())) {
					memberIdsToAdd.add(user.getId());
				}
			}

			int cdate = t.getUnixTimestamp();
			int creator = t.getUserId();
			int groupId = getId();

			// now add the missing user assignments
			if (memberIdsToAdd.size() > 0) {
				List<Object[]> params = memberIdsToAdd.stream().map(id -> new Object[] { id, groupId, cdate, creator }).collect(Collectors.toList());
				DBUtils.executeBatchInsert("INSERT INTO user_group (user_id, usergroup_id, cdate, creator) VALUES (?, ?, ?, ?)", params);
				this.memberIds = null;

				for (Integer userId : memberIdsToAdd) {
					ActionLogger.logCmd(ActionLogger.EDIT, UserGroup.TYPE_USERGROUP, groupId, userId, "UserGroup.addUser");
					t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, userId, new String[] {"groups"}, Events.UPDATE));
				}
			}

			// ... and remove the surplus user assignments
			if (memberIdsToRemove.size() > 0) {
				// check whether any user would lose the last group
				List<SystemUser> users = t.getObjects(SystemUser.class, memberIdsToRemove);
				for (SystemUser user : users) {
					if (CollectionUtils.subtract(user.getUserGroups(), Arrays.asList(this)).isEmpty()) {
						throw new UserWithoutGroupException(
								I18NHelper.get("exception.user_remove.lastgroup", I18NHelper.getName(user), I18NHelper.getName(this)));
					}
				}

				List<Object[]> params = memberIdsToRemove.stream().map(userId -> new Object[] {groupId, userId}).collect(Collectors.toList());
				DBUtils.executeBatchUpdate("DELETE FROM user_group WHERE usergroup_id = ? AND user_id = ?", params);
				this.memberIds = null;

				for (Integer userId : memberIdsToRemove) {
					ActionLogger.logCmd(ActionLogger.EDIT, UserGroup.TYPE_USERGROUP, groupId, userId, "UserGroup.removeUser");
					t.addTransactional(new TransactionalTriggerEvent(SystemUser.class, userId, new String[] {"groups"}, Events.UPDATE));
				}
			}

			if (!memberIdsToAdd.isEmpty() || !memberIdsToRemove.isEmpty()) {
				t.addTransactional(new TransactionalTriggerEvent(UserGroup.class, id, new String[] {"members"}, Events.UPDATE));
			}
		}
	}
}
