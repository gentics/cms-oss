package com.gentics.contentnode.factory.object;

import static com.gentics.contentnode.perm.PermHandler.PERM_CHANGE_PERM;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_DELETE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.RemovePermsTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Factory for {@link PortalImplementation}
 */
@DBTables({ @DBTable(clazz = PortalImplementation.class, name = C.Tables.PORTAL_IMPLEMENTATION) })
public class PortalImplementationFactory extends AbstractFactory {
	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.PORTAL_IMPLEMENTATION, PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, true, FactoryPortalImplementation.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create an instance
	 */
	public PortalImplementationFactory() {
		super();
	}

	/**
	 * Get the Portal Implementation with the given local ID, global ID or keyword
	 * @param idOrKeyword local ID, global ID or keyword
	 * @return Portal Implementation or null, if not found
	 * @throws NodeException
	 */
	public static PortalImplementation get(String idOrKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PortalImplementation impl = t.getObject(PortalImplementation.class, idOrKeyword);
		if (impl != null) {
			return impl;
		}
		Integer id = DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION + " WHERE keyword = ?", st -> st.setString(1, idOrKeyword),
				rs -> rs.next() ? rs.getInt("id") : null);
		return id != null ? t.getObject(PortalImplementation.class, id) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (PortalImplementation.class.equals(clazz)) {
			return (T) new EditableFactoryPortalImplementation(handle.createObjectInfo(PortalImplementation.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs,
			List<Integer>[] idLists) throws SQLException, NodeException {
		if (PortalImplementation.class.equals(clazz)) {
			return (T) new FactoryPortalImplementation(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs, C.Tables.PORTAL_IMPLEMENTATION));
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
		if (object instanceof FactoryPortalImplementation) {
			return (T) new EditableFactoryPortalImplementation((FactoryPortalImplementation) object, info, false);
		} else {
			throw new NodeException("PortalImplementationFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, PortalImplementation.class)) {
			@SuppressWarnings("unchecked")
			Collection<PortalImplementation> deleted = getDeleteList(t, PortalImplementation.class);

			for (PortalImplementation impl : deleted) {
				ActionLogger.logCmd(ActionLogger.DEL, PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, impl.getId(), 0, "PortalImplementation.delete");
				Events.trigger(impl, null, Events.DELETE);
			}

			// delete the portal implementations
			flushDelete("DELETE FROM " + C.Tables.PORTAL_IMPLEMENTATION + " WHERE id IN", PortalImplementation.class);

			// permissions
			flushDelete("DELETE FROM perm WHERE o_type = " + PortalImplementation.TYPE_PORTAL_IMPLEMENTATION + " AND o_id IN", PortalImplementation.class);
			// update the PermissionStore
			for (PortalImplementation impl : deleted) {
				t.addTransactional(new RemovePermsTransactional(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, ObjectTransformer.getInt(impl.getId(), 0)));
			}
		}
	}

	/**
	 * Set the initial permissions for the (just created) portal implementation for the groups of the transaction user
	 * (or the given group) and all their parent groups. This is done like for ContentRepositories.
	 * @param impl portal implementation
	 * @param startUserGroup optional group
	 * @throws NodeException
	 */
	private static void setInitialPermissions(PortalImplementation impl, UserGroup startUserGroup) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<UserGroup> groups = null;
		if (startUserGroup == null) {
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			groups = user.getAllGroupsWithParents();
		} else {
			groups = startUserGroup.getParents();
			groups.add(startUserGroup);
		}

		for (UserGroup userGroup : groups) {
			PermHandler groupPermHandler = t.getGroupPermHandler(userGroup.getId());
			String perm = null;

			// Check if the group has permission to grant someone else permissions on portal implementations.
			if (groupPermHandler.checkPermissionBit(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, null, PERM_CHANGE_PERM)) {
				// Give View, Modify, Grant & Delete permissions
				perm = Permissions.get(PERM_VIEW, PERM_CHANGE_PERM, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE).toString();
			} else {
				// Give View, Modify & Delete, but no Grant permission
				perm = Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE).toString();
			}

			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, impl.getId(), Arrays.asList(userGroup), perm);
		}
	}

	/**
	 * Readonly factory implementation of {@link PortalImplementation}
	 */
	private static class FactoryPortalImplementation extends AbstractContentObject implements PortalImplementation {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 7354520986452983140L;

		@DataField("keyword")
		@Updateable
		protected String keyword;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("description")
		@Updateable
		protected String description;

		@DataField("type")
		@Updateable
		protected String type;

		@DataField("cdate")
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("creator")
		protected int creatorId = 0;

		@DataField("edate")
		@Updateable
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		protected int editorId = 0;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryPortalImplementation(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 * @throws NodeException
		 */
		protected FactoryPortalImplementation(Integer id, NodeObjectInfo info, Map<String, Object> dataMap, int udate, GlobalId globalId)
				throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.udate = udate;
			this.globalId = globalId;
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
			return null;
		}

		@Override
		public String getKeyword() {
			return keyword;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public PortalImplementationModel.Type getType() {
			if (StringUtils.isEmpty(type)) {
				return null;
			}
			return PortalImplementationModel.Type.valueOf(type);
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);
		}

		@Override
		public List<PortalImplementationFile> getFiles(String folder) throws NodeException {
			if (isEmptyId(getId())) {
				return Collections.emptyList();
			}
			return TransactionManager.getCurrentTransaction().getObjects(PortalImplementationFile.class,
					PortalImplementationFileFactory.getFileIds(getId(), PortalImplementationFileFactory.normalize(folder)));
		}

		@Override
		public PortalImplementationFile getFile(String path) throws NodeException {
			if (isEmptyId(getId())) {
				return null;
			}
			Integer fileId = PortalImplementationFileFactory.getFileId(getId(), PortalImplementationFileFactory.normalize(path));
			return fileId != null ? TransactionManager.getCurrentTransaction().getObject(PortalImplementationFile.class, fileId) : null;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			if (isEmptyId(getId())) {
				return Collections.emptyList();
			}
			Set<Integer> nodeIds = DBUtils.select("SELECT node_id id FROM " + C.Tables.NODE_PORTAL_IMPLEMENTATION + " WHERE portal_implementation_id = ?",
					st -> st.setInt(1, getId()), DBUtils.IDS);
			return TransactionManager.getCurrentTransaction().getObjects(Node.class, nodeIds);
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			// delete the contained files
			for (PortalImplementationFile file : getFiles(null)) {
				file.delete(force);
			}
			deletedFactoryObject(this);
		}

		@Override
		public Object get(String key) {
			try {
				switch (key) {
				case "keyword":
					return getKeyword();
				case "name":
					return getName();
				case "description":
					return getDescription();
				case "type":
					return type;
				case "cdate":
					return getCDate().getIntTimestamp();
				case "edate":
					return getEDate().getIntTimestamp();
				case "creator":
					return getCreator();
				case "editor":
					return getEditor();
				default:
					return super.get(key);
				}
			} catch (NodeException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return String.format("PortalImplementation {id: %d, keyword: %s}", id, keyword);
		}
	}

	/**
	 * Editable factory implementation of {@link PortalImplementation}
	 */
	private static class EditableFactoryPortalImplementation extends FactoryPortalImplementation {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2405283658931724468L;

		/**
		 * Flag to mark modified objects
		 */
		private boolean modified = false;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryPortalImplementation(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param impl original
		 * @param info info
		 * @param asNewObject true for a new object, false for an editable version of the given object
		 * @throws NodeException
		 */
		protected EditableFactoryPortalImplementation(FactoryPortalImplementation impl, NodeObjectInfo info, boolean asNewObject) throws NodeException {
			super(asNewObject ? null : impl.getId(), info, getDataMap(impl), asNewObject ? -1 : impl.getUdate(),
					asNewObject ? null : impl.getGlobalId());
			if (asNewObject) {
				modified = true;
			}
		}

		@Override
		public void setKeyword(String keyword) throws ReadOnlyException {
			if (!StringUtils.equals(this.keyword, keyword)) {
				this.keyword = keyword;
				this.modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.equals(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public void setDescription(String description) throws ReadOnlyException {
			if (!StringUtils.equals(this.description, description)) {
				this.description = description;
				this.modified = true;
			}
		}

		@Override
		public void setType(PortalImplementationModel.Type type) throws ReadOnlyException {
			String typeName = type != null ? type.name() : null;
			if (!StringUtils.equals(this.type, typeName)) {
				this.type = typeName;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			return save(null);
		}

		@Override
		public boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			keyword = StringUtils.trimToEmpty(keyword);
			name = StringUtils.trimToEmpty(name);
			description = StringUtils.defaultString(description);

			boolean isNew = isEmptyId(getId());
			boolean isModified = this.modified;

			if (isModified) {
				validate();

				// set creator data for new objects
				if (isNew) {
					creatorId = t.getUserId();
					cDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				// set the editor data
				editorId = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());

				saveFactoryObject(this);
				this.modified = false;

				if (isNew) {
					UserGroup userGroup = null;
					if (userGroupId != null) {
						userGroup = t.getObject(UserGroup.class, userGroupId);
					}
					setInitialPermissions(this, userGroup);

					ActionLogger.logCmd(ActionLogger.CREATE, PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, getId(), 0, "PortalImplementation.create");
					t.addTransactional(new TransactionalTriggerEvent(PortalImplementation.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, getId(), 0, "PortalImplementation.update");
					t.addTransactional(new TransactionalTriggerEvent(PortalImplementation.class, getId(), null, Events.UPDATE));
				}

				t.dirtObjectCache(PortalImplementation.class, getId());
			}

			return isModified;
		}

		/**
		 * Validate the data: keyword, name and type are mandatory, keyword and name must not exceed the maximum length and must be unique
		 * @throws NodeException if validation fails
		 */
		protected void validate() throws NodeException {
			assertNotEmpty("keyword", keyword);
			assertNotEmpty("name", name);
			assertNotEmpty("type", type);
			assertMaxLength("keyword", keyword);
			assertMaxLength("name", name);
			assertUnique("keyword", keyword);
			assertUnique("name", name);
		}

		/**
		 * Assert that the given value is not empty
		 * @param attribute attribute name
		 * @param value value
		 * @throws RestMappedException if the value is empty
		 */
		protected void assertNotEmpty(String attribute, String value) throws RestMappedException {
			if (StringUtils.isEmpty(value)) {
				throw new RestMappedException(I18NHelper.get("portal_implementation.missing", attribute)).setMessageType(Message.Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}

		/**
		 * Assert that the given value does not exceed the maximum length
		 * @param attribute attribute name
		 * @param value value
		 * @throws RestMappedException if the value is too long
		 */
		protected void assertMaxLength(String attribute, String value) throws RestMappedException {
			if (value.length() > MAX_LENGTH) {
				throw new RestMappedException(I18NHelper.get("portal_implementation.toolong", attribute, Integer.toString(MAX_LENGTH)))
						.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}

		/**
		 * Assert that no other portal implementation uses the given value for the attribute
		 * @param attribute attribute name (column)
		 * @param value value
		 * @throws NodeException if another portal implementation uses the value
		 */
		protected void assertUnique(String attribute, String value) throws NodeException {
			int ownId = ObjectTransformer.getInt(getId(), -1);
			boolean duplicate = !DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION + " WHERE " + attribute + " = ? AND id != ?", ps -> {
				ps.setString(1, value);
				ps.setInt(2, ownId);
			}, DBUtils.IDS).isEmpty();

			if (duplicate) {
				throw new RestMappedException(I18NHelper.get("portal_implementation.duplicate." + attribute, value)).setMessageType(Message.Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}
		}
	}
}
