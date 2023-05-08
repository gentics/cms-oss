package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.EditableI18nString;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Factory for roles
 */
@DBTables({ @DBTable(clazz = Role.class, name = C.Tables.ROLE)})
public class RoleFactory extends AbstractFactory {
	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.ROLE, Role.TYPE_ROLE, false, FactoryRole.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create an instance
	 */
	public RoleFactory() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (Role.class.equals(clazz)) {
			return (T) new EditableFactoryRole(handle.createObjectInfo(Role.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists)
			throws SQLException, NodeException {
		if (Role.class.equals(clazz)) {
			return (T) new FactoryRole(id, info, rs.getValues());
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryRole) {
			EditableFactoryRole editableCopy = new EditableFactoryRole((FactoryRole) object, info, false);

			return (T) editableCopy;
		} else {
			throw new NodeException("RoleFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	@Override
	public void flush() throws NodeException {
		Collection<Role> deleteList = getDeleteList(Role.class);
		if (!deleteList.isEmpty()) {
			Transaction t =  TransactionManager.getCurrentTransaction();
			Set<Integer> roleIds = deleteList.stream().map(Role::getId).collect(Collectors.toSet());
			List<Integer> outputIds = deleteList.stream().flatMap(r -> Stream.of(r.getNameId(), r.getDescriptionId())).collect(Collectors.toList());

			String questionMarks = StringUtils.repeat("?", roleIds.size(), ",");
			Object[] ids = (Object[]) roleIds.toArray(new Object[roleIds.size()]);

			// delete role_usergroup assignments
			List<Integer> roleUserGroupIds = new ArrayList<>(DBUtils.select("SELECT id FROM role_usergroup WHERE role_id IN (" + questionMarks + ")", ps -> {
				for (int i = 0; i < ids.length; i++) {
					ps.setObject(i + 1, ids[i]);
				}
			}, DBUtils.IDS));
			DBUtils.executeMassStatement("DELETE FROM role_usergroup_assignment WHERE role_usergroup_id IN ", null, roleUserGroupIds, 1, null,
					Transaction.DELETE_STATEMENT);
			DBUtils.executeMassStatement("DELETE FROM role_usergroup WHERE id IN", null, roleUserGroupIds, 1, null, Transaction.DELETE_STATEMENT);

			// delete role_perms
			List<Integer> rolePermIds = new ArrayList<>(DBUtils.select("SELECT id FROM roleperm WHERE role_id IN (" + questionMarks + ")", ps -> {
				for (int i = 0; i < ids.length; i++) {
					ps.setObject(i + 1, ids[i]);
				}
			}, DBUtils.IDS));
			DBUtils.executeMassStatement("DELETE FROM roleperm_obj WHERE roleperm_id IN ", null, rolePermIds, 1, null,
					Transaction.DELETE_STATEMENT);
			DBUtils.executeMassStatement("DELETE FROM roleperm WHERE id IN", null, rolePermIds, 1, null, Transaction.DELETE_STATEMENT);

			// delete dictionary entries
			DBUtils.selectAndDelete("dicuser", "SELECT id FROM dicuser WHERE output_id IN", outputIds);
			DBUtils.selectAndDelete("outputuser", "SELECT id FROM outputuser WHERE id IN", outputIds);

			// delete roles
			DBUtils.executeUpdate("DELETE FROM role WHERE id IN (" + questionMarks + ")", ids);

			// logcmd and events for deleted roles
			for (Integer id : roleIds) {
				ActionLogger.logCmd(ActionLogger.DEL, Role.TYPE_ROLE, id, 0, "Role.delete");
				t.addTransactional(new TransactionalTriggerEvent(Role.class, id, null, Events.DELETE));
			}
		}
	}

	/**
	 * Factory implementation of Roles
	 */
	private static class FactoryRole extends AbstractContentObject implements Role {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -6085914802016225234L;

		@DataField("name_id")
		protected int nameId;

		protected I18nString name;

		@DataField("description_id")
		protected int descriptionId;

		protected I18nString description;

		/**
		 * Create an empty instance
		 * @param info info
		 * @throws NodeException
		 */
		protected FactoryRole(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 */
		protected FactoryRole(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.name = new CNI18nString(Integer.toString(nameId));
			this.description = new CNI18nString(Integer.toString(descriptionId));
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
		public NodeObject copy() throws NodeException {
			throw new NodeException("Copy not implemented for Roles");
		}

		@Override
		public I18nString getName() {
			return name;
		}

		@Override
		public int getNameId() {
			return nameId;
		}

		@Override
		public I18nString getDescription() {
			return description;
		}

		@Override
		public int getDescriptionId() {
			return descriptionId;
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			RoleFactory factory = (RoleFactory) t.getObjectFactory(Role.class);
			factory.getDeleteList(Role.class).add(this);
		}

		@Override
		public Object get(String key) {
			if ("name".equals(key)) {
				return getName().toString();
			} else if ("description".equals(key)) {
				return getDescription().toString();
			} else {
				return super.get(key);
			}
		}
	}

	/**
	 * Editable factory object
	 */
	private static class EditableFactoryRole extends FactoryRole {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -25735501997362327L;

		/**
		 * Flag to mark whether the systemuser has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		protected boolean modified = false;

		/**
		 * Editable name
		 */
		protected EditableI18nString editableName = new EditableI18nString();

		/**
		 * Editable description
		 */
		protected EditableI18nString editableDescription = new EditableI18nString();

		/**
		 * Create a new group instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactoryRole(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param role role
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryRole(FactoryRole role,
				NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : role.getId(), info, getDataMap(role));

			// read the names and descriptions
			editableName.init(nameId);
			editableDescription.init(descriptionId);
			if (asNewObject) {
				modified = true;
				// reset the name and description id
				nameId = -1;
				descriptionId = -1;
			}
		}

		@Override
		public I18nString getName() {
			return editableName;
		}

		@Override
		public void setName(String name, int language) throws ReadOnlyException {
			editableName.put(language, name);
		}

		@Override
		public I18nString getDescription() {
			return editableDescription;
		}

		@Override
		public void setDescription(String description, int language) throws ReadOnlyException {
			editableDescription.put(language, description);
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean dicEntriesChanged = false;

			boolean isNew = isEmptyId(getId());

			// save names
			if (nameId <= 0) {
				nameId = CNDictionary.createNewOutputId();
				this.modified = true;
			}
			for (UserLanguage lang : UserLanguageFactory.getActive()) {
				int id = lang.getId();
				// make the name unique
				editableName.put(id, CNDictionary.makeUnique(nameId, id, ObjectTransformer.getString(editableName.get(id), ""), C.Tables.ROLE, "name_id"));
				dicEntriesChanged |= CNDictionary.saveDicUserEntry(nameId, id, ObjectTransformer.getString(editableName.get(id), ""));
			}

			// save descriptions
			if (descriptionId <= 0) {
				descriptionId = CNDictionary.createNewOutputId();
				this.modified = true;
			}
			for (UserLanguage lang : UserLanguageFactory.getActive()) {
				int id = lang.getId();
				dicEntriesChanged |= CNDictionary.saveDicUserEntry(descriptionId, id, ObjectTransformer.getString(editableDescription.get(id), ""));
			}

			boolean isModified = this.modified || dicEntriesChanged;

			if (this.modified) {
				saveFactoryObject(this);
				this.modified = false;
			}

			// logcmd and event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, Role.TYPE_ROLE, getId(), 0, "Role.create");
					t.addTransactional(new TransactionalTriggerEvent(Role.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, Role.TYPE_ROLE, getId(), 0, "Role.update");
					t.addTransactional(new TransactionalTriggerEvent(Role.class, getId(), null, Events.UPDATE));
				}
				t.dirtObjectCache(Role.class, getId());
			}

			return isModified;
		}
	}
}
