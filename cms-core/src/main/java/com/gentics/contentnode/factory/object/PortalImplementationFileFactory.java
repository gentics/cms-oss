package com.gentics.contentnode.factory.object;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.io.FileManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.util.FileUtil;

/**
 * Factory for {@link PortalImplementationFile}
 */
@DBTables({ @DBTable(clazz = PortalImplementationFile.class, name = C.Tables.PORTAL_IMPLEMENTATION_FILE) })
public class PortalImplementationFileFactory extends AbstractFactory {
	/**
	 * Name of the subfolder of the dbfiles folder, which holds the contents of the files
	 */
	public final static String DBFILES_SUBFOLDER = "portalimplementations";

	/**
	 * Escape character used in LIKE clauses
	 */
	protected final static char LIKE_ESCAPE = '!';

	/**
	 * Maximum length of the info written to the action log (length of the column logcmd.info)
	 */
	protected final static int LOG_INFO_MAX_LENGTH = 255;

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.PORTAL_IMPLEMENTATION_FILE, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE, true,
					FactoryPortalImplementationFile.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create an instance
	 */
	public PortalImplementationFileFactory() {
		super();
	}

	/**
	 * Normalize the given path by removing leading and trailing separators
	 * @param path path (may be null)
	 * @return normalized path (empty for the root folder)
	 */
	public static String normalize(String path) {
		return StringUtils.strip(StringUtils.defaultString(path), PortalImplementationFile.SEPARATOR);
	}

	/**
	 * Validate the given (normalized) path.
	 * The path must not be empty or longer than {@link PortalImplementationFile#MAX_PATH_LENGTH}, must not contain backslashes or control characters,
	 * and every segment must be non-empty, not <code>.</code> or <code>..</code> and must not start or end with whitespace.
	 * @param path path
	 * @throws RestMappedException if the path is invalid
	 */
	public static void validatePath(String path) throws RestMappedException {
		if (StringUtils.isEmpty(path)) {
			throw invalidPath(path);
		}
		if (path.length() > PortalImplementationFile.MAX_PATH_LENGTH) {
			throw new RestMappedException(I18NHelper.get("portal_implementation_file.toolong", path, Integer.toString(PortalImplementationFile.MAX_PATH_LENGTH)))
					.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		if (path.contains("\\") || path.chars().anyMatch(Character::isISOControl)) {
			throw invalidPath(path);
		}
		for (String segment : StringUtils.splitPreserveAllTokens(path, PortalImplementationFile.SEPARATOR)) {
			if (StringUtils.isEmpty(segment) || ".".equals(segment) || "..".equals(segment) || !segment.equals(segment.strip())) {
				throw invalidPath(path);
			}
		}
	}

	/**
	 * Get the IDs of the files of the portal implementation, which are contained in the given folder (including subfolders)
	 * @param portalImplementationId portal implementation ID
	 * @param folder normalized folder path (empty for the root folder)
	 * @return set of file IDs
	 * @throws NodeException
	 */
	public static Set<Integer> getFileIds(int portalImplementationId, String folder) throws NodeException {
		if (StringUtils.isEmpty(folder)) {
			return DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION_FILE + " WHERE portal_implementation_id = ?", ps -> {
				ps.setInt(1, portalImplementationId);
			}, DBUtils.IDS);
		} else {
			return DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION_FILE + " WHERE portal_implementation_id = ? AND path LIKE ? ESCAPE '"
					+ LIKE_ESCAPE + "'", ps -> {
						ps.setInt(1, portalImplementationId);
						ps.setString(2, likePrefix(folder));
					}, DBUtils.IDS);
		}
	}

	/**
	 * Get the ID of the file with given path
	 * @param portalImplementationId portal implementation ID
	 * @param path normalized path
	 * @return file ID or null
	 * @throws NodeException
	 */
	public static Integer getFileId(int portalImplementationId, String path) throws NodeException {
		return DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION_FILE + " WHERE portal_implementation_id = ? AND path = ?", ps -> {
			ps.setInt(1, portalImplementationId);
			ps.setString(2, path);
		}, DBUtils.IDS).stream().findFirst().orElse(null);
	}

	/**
	 * Get the pattern for a LIKE clause, which matches all paths in the given folder
	 * @param folder folder
	 * @return LIKE pattern
	 */
	protected static String likePrefix(String folder) {
		StringBuilder pattern = new StringBuilder();
		for (char c : folder.toCharArray()) {
			if (c == '%' || c == '_' || c == LIKE_ESCAPE) {
				pattern.append(LIKE_ESCAPE);
			}
			pattern.append(c);
		}
		return pattern.append(PortalImplementationFile.SEPARATOR).append("%").toString();
	}

	/**
	 * Get the folder, which holds the contents of the files
	 * @return folder
	 */
	protected static java.io.File getContentsDir() {
		return new java.io.File(ConfigurationValue.DBFILES_PATH.get(), DBFILES_SUBFOLDER);
	}

	/**
	 * Get the info for the action log entry
	 * @param action action
	 * @param path path of the file
	 * @return info, abbreviated to the maximum length
	 */
	protected static String logInfo(String action, String path) {
		return StringUtils.abbreviate(String.format("PortalImplementationFile.%s: %s", action, path), LOG_INFO_MAX_LENGTH);
	}

	/**
	 * Create the exception for an invalid path
	 * @param path path
	 * @return exception
	 */
	protected static RestMappedException invalidPath(String path) {
		return new RestMappedException(I18NHelper.get("portal_implementation_file.invalidpath", StringUtils.defaultString(path)))
				.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
	}

	/**
	 * Create the exception for a conflict with an existing file or folder
	 * @param path path
	 * @param existing path of the existing file or folder
	 * @return exception
	 */
	protected static RestMappedException conflict(String path, String existing) {
		return new RestMappedException(I18NHelper.get("portal_implementation_file.conflict", path, existing)).setMessageType(Message.Type.CRITICAL)
				.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
	}

	/**
	 * Trigger an update event for the Portal Implementation (at the end of the transaction), because the given file was changed.
	 * The event will be triggered only once per transaction and Portal Implementation. This causes the Portal Implementation
	 * to be synchronized into the devtool packages containing it.
	 * @param t transaction
	 * @param file changed file
	 */
	protected static void triggerPortalImplementationUpdate(Transaction t, PortalImplementationFile file) {
		String key = String.format("%s.update.%d", PortalImplementationFile.class.getName(), file.getPortalImplementationId());
		if (t.getAttributes().putIfAbsent(key, Boolean.TRUE) == null) {
			t.addTransactional(new TransactionalTriggerEvent(PortalImplementation.class, file.getPortalImplementationId(), null, Events.UPDATE) {
				@Override
				public boolean onTransactionCommit(Transaction t) {
					// remove the marker, so that changes done after the commit (in the same transaction) will trigger the event again
					t.getAttributes().remove(key);
					return super.onTransactionCommit(t);
				}

				@Override
				public void onTransactionRollback(Transaction t) {
					t.getAttributes().remove(key);
					super.onTransactionRollback(t);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (PortalImplementationFile.class.equals(clazz)) {
			return (T) new EditableFactoryPortalImplementationFile(handle.createObjectInfo(PortalImplementationFile.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs,
			List<Integer>[] idLists) throws SQLException, NodeException {
		if (PortalImplementationFile.class.equals(clazz)) {
			return (T) new FactoryPortalImplementationFile(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs, C.Tables.PORTAL_IMPLEMENTATION_FILE));
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
		if (object instanceof FactoryPortalImplementationFile) {
			return (T) new EditableFactoryPortalImplementationFile((FactoryPortalImplementationFile) object, info);
		} else {
			throw new NodeException("PortalImplementationFileFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, PortalImplementationFile.class)) {
			@SuppressWarnings("unchecked")
			Collection<PortalImplementationFile> deleted = getDeleteList(t, PortalImplementationFile.class);

			FileManager fileManager = new FileManager();
			t.addTransactional(fileManager);

			for (PortalImplementationFile file : deleted) {
				ActionLogger.logCmd(ActionLogger.DEL, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE, file.getId(),
						file.getPortalImplementationId(), logInfo("delete", file.getPath()));
				// the contents are removed from the dbfiles folder after the transaction was committed
				fileManager.deleteFile(file.getBinFile());
				triggerPortalImplementationUpdate(t, file);
			}

			flushDelete("DELETE FROM " + C.Tables.PORTAL_IMPLEMENTATION_FILE + " WHERE id IN", PortalImplementationFile.class);
		}
	}

	/**
	 * Readonly factory implementation of {@link PortalImplementationFile}
	 */
	private static class FactoryPortalImplementationFile extends AbstractContentObject implements PortalImplementationFile {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2894337059413816471L;

		@DataField("portal_implementation_id")
		protected int portalImplementationId;

		@DataField("path")
		@Updateable
		protected String path;

		@DataField("mimetype")
		@Updateable
		protected String mimetype;

		@DataField("filesize")
		@Updateable
		protected long filesize;

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
		protected FactoryPortalImplementationFile(NodeObjectInfo info) {
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
		protected FactoryPortalImplementationFile(Integer id, NodeObjectInfo info, Map<String, Object> dataMap, int udate, GlobalId globalId)
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
		public int getPortalImplementationId() {
			return portalImplementationId;
		}

		@Override
		public PortalImplementation getPortalImplementation() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(PortalImplementation.class, portalImplementationId);
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public String getMimetype() {
			return mimetype;
		}

		@Override
		public long getFilesize() {
			return filesize;
		}

		@Override
		public java.io.File getBinFile() {
			return new java.io.File(getContentsDir(), getId() + ".bin");
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
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			deletedFactoryObject(this);
		}

		@Override
		public Object get(String key) {
			try {
				switch (key) {
				case "path":
					return getPath();
				case "name":
					return getName();
				case "folder":
					return getFolder();
				case "mimetype":
					return getMimetype();
				case "filesize":
					return getFilesize();
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
			return String.format("PortalImplementationFile {id: %d, path: %s}", id, path);
		}
	}

	/**
	 * Editable factory implementation of {@link PortalImplementationFile}
	 */
	private static class EditableFactoryPortalImplementationFile extends FactoryPortalImplementationFile {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 8601925561622853025L;

		/**
		 * Flag to mark modified objects
		 */
		private boolean modified = false;

		/**
		 * Path of the file when it was loaded (null for new files)
		 */
		private String originalPath;

		/**
		 * Temporary file holding the new contents (if contents were set)
		 */
		private transient java.io.File tmpFile;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected EditableFactoryPortalImplementationFile(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given object
		 * @param file original
		 * @param info info
		 * @throws NodeException
		 */
		protected EditableFactoryPortalImplementationFile(FactoryPortalImplementationFile file, NodeObjectInfo info) throws NodeException {
			super(file.getId(), info, getDataMap(file), file.getUdate(), file.getGlobalId());
			originalPath = file.getPath();
		}

		@Override
		public void setPortalImplementationId(int portalImplementationId) throws ReadOnlyException {
			if (!isEmptyId(getId())) {
				throw new ReadOnlyException("Cannot change the Portal Implementation of an existing file");
			}
			if (this.portalImplementationId != portalImplementationId) {
				this.portalImplementationId = portalImplementationId;
				this.modified = true;
			}
		}

		@Override
		public void setPath(String path) throws ReadOnlyException {
			if (!StringUtils.equals(this.path, path)) {
				this.path = path;
				this.modified = true;
			}
		}

		@Override
		public void setMimetype(String mimetype) throws ReadOnlyException {
			if (!StringUtils.equals(this.mimetype, mimetype)) {
				this.mimetype = mimetype;
				this.modified = true;
			}
		}

		@Override
		public void setContents(InputStream contents) throws NodeException {
			if (contents == null) {
				throw new NodeException("Cannot set contents of " + this + " from null stream");
			}
			deleteTmpFile();
			try {
				tmpFile = java.io.File.createTempFile("portalimplementationfile", ".tmp", new java.io.File(ConfigurationValue.TMP_PATH.get()));
				filesize = Files.copy(contents, tmpFile.toPath(), REPLACE_EXISTING);
			} catch (IOException e) {
				deleteTmpFile();
				throw new NodeException("Error while storing contents of " + this + " into temporary file", e);
			}
			this.modified = true;
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isNew = isEmptyId(getId());
			boolean isModified = this.modified;
			boolean contentsChanged = tmpFile != null;

			try {
				if (isModified) {
					path = normalize(path);
					validatePath(path);
					if (!StringUtils.equals(path, originalPath)) {
						assertNoConflicts();
					}

					if (StringUtils.isBlank(mimetype)) {
						mimetype = detectMimetype();
					}

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

					if (contentsChanged || isNew) {
						storeContents();
					}

					if (isNew) {
						ActionLogger.logCmd(ActionLogger.CREATE, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE, getId(), portalImplementationId,
								logInfo("create", path));
					} else {
						ActionLogger.logCmd(ActionLogger.EDIT, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE, getId(), portalImplementationId,
								logInfo("update", path));
					}

					originalPath = path;
					t.dirtObjectCache(PortalImplementationFile.class, getId());
					triggerPortalImplementationUpdate(t, this);
				}
			} finally {
				deleteTmpFile();
			}

			return isModified;
		}

		/**
		 * Assert that the path does not conflict with existing files or folders:
		 * <ul>
		 * <li>No other file must have the same path</li>
		 * <li>No file must exist "below" the path (path is used as folder)</li>
		 * <li>No folder in the path must be an existing file</li>
		 * </ul>
		 * @throws NodeException
		 */
		protected void assertNoConflicts() throws NodeException {
			Integer existingId = getFileId(portalImplementationId, path);
			if (existingId != null && !existingId.equals(getId())) {
				throw conflict(path, path);
			}

			if (!getFileIds(portalImplementationId, path).isEmpty()) {
				throw conflict(path, path);
			}

			String folder = path;
			while (folder.contains(SEPARATOR)) {
				folder = StringUtils.substringBeforeLast(folder, SEPARATOR);
				if (getFileId(portalImplementationId, folder) != null) {
					throw conflict(path, folder);
				}
			}
		}

		/**
		 * Detect the mimetype from the (new) contents and the filename
		 * @return mimetype
		 * @throws NodeException
		 */
		protected String detectMimetype() throws NodeException {
			java.io.File source = tmpFile != null ? tmpFile : (isEmptyId(getId()) ? null : getBinFile());
			if (source == null || !source.exists()) {
				return FileUtil.getMimeTypeByExtension(getName());
			}
			try (InputStream in = new FileInputStream(source)) {
				return FileUtil.getMimeType(in, getName());
			} catch (IOException e) {
				throw new NodeException("Error while detecting mimetype of " + this, e);
			}
		}

		/**
		 * Store the contents in the dbfiles folder. If no contents were set (for a new file), an empty file is created
		 * @throws NodeException
		 */
		protected void storeContents() throws NodeException {
			java.io.File targetFile = getBinFile();
			java.io.File contentsDir = targetFile.getParentFile();
			if (!contentsDir.exists() && !contentsDir.mkdirs()) {
				throw new NodeException("Could not create folder " + contentsDir.getAbsolutePath());
			}
			try {
				if (tmpFile != null) {
					Files.move(tmpFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
					tmpFile = null;
				} else {
					Files.write(targetFile.toPath(), new byte[0]);
					filesize = 0;
				}
			} catch (IOException e) {
				throw new NodeException("Error while storing contents of " + this + " into " + targetFile.getAbsolutePath(), e);
			}
		}

		/**
		 * Delete the temporary file, if one exists
		 */
		protected void deleteTmpFile() {
			if (tmpFile != null) {
				try {
					Files.deleteIfExists(tmpFile.toPath());
				} catch (IOException e) {
					logger.warn("Could not delete temporary file " + tmpFile.getAbsolutePath(), e);
				}
				tmpFile = null;
			}
		}
	}
}
