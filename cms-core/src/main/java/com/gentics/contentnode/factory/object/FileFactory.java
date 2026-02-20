/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: FileFactory.java,v 1.46.2.1.2.6 2011-02-11 10:41:16 clemens Exp $
 */
package com.gentics.contentnode.factory.object;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CountingInputStream;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.url.AlternateUrlsContainer;
import com.gentics.contentnode.factory.url.ContentFileAlternateUrlsContainer;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.io.FileManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.string.CNStringUtils;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.image.JavaImageUtils;
import com.gentics.lib.util.FileUtil;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5InputStream;

/**
 * An objectfactory to create {@link File} and {@link ImageFile} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = File.class, name = "contentfile"), @DBTable(clazz = ImageFile.class, name = "contentfile", alias = "contentimagefile"), @DBTable(clazz = ContentFile.class, name = "contentfile") })
public class FileFactory extends AbstractFactory {

	public static final String DEFAULT_MAX_DIMENSIONS = "4000x4000";

	protected final static String OBJECTTAGS = "objecttags";

	/**
	 * SQL Statement to insert a binarycontent record
	 */
	protected final static String INSERT_CONTENTFILEDATA_SQL = "INSERT INTO contentfiledata (contentfile_id,binarycontent) (?,?)";

	/**
	 * SQL Statement to select a binarycontent record
	 */
	protected final static String SELECT_CONTENTFILEDATA_SQL = "SELECT binarycontent from contentfiledata WHERE contentfile_id = ?";

	/**
	 * SQL Statement to insert a new contentfile
	 */
	protected final static String INSERT_CONTENTFILE_SQL = "INSERT INTO contentfile"
			+ " (name,nice_url,filetype,folder_id,filesize,creator,cdate,custom_cdate,editor,edate,custom_edate,description,sizex,sizey,md5,dpix,dpiy,fpx,fpy,channelset_id,channel_id,is_master,force_online, mc_exclude,disinherit_default,uuid)"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to update a new contentfile
	 */
	protected final static String UPDATE_CONTENTFILE_SQL = "UPDATE contentfile"
			+ " SET name = ?, nice_url = ?, filetype = ? , folder_id = ? , filesize = ? , editor = ? , custom_cdate = ? , edate = ?, custom_edate = ? ,description = ? , sizex = ? , sizey = ? , md5 = ? , dpix = ? , dpiy = ?, fpx = ?, fpy = ?, channelset_id = ?, channel_id = ?, force_online = ?"
			+ " WHERE id = ?";

	/**
	 * SQL Statement to update the delete-flag of a contentfile
	 */
	protected final static String UPDATE_CONTENTFILE_DELETEFLAG = "UPDATE contentfile SET deleted = ?, deletedby = ? WHERE id = ?";

	/**
	 * SQL statement to delete entries from contentfile_online
	 */
	protected final static String DELETE_CONTENTFILE_ONLINE = "DELETE FROM contentfile_online WHERE contentfile_id = ?";

	/**
	 * SQL Statement to select a single file
	 */
	protected final static String SELECT_FILE_SQL = createSelectStatement("contentfile");

	/**
	 * SQL Statement for batchloading files
	 */
	protected final static String BATCHLOAD_FILE_SQL = createBatchLoadStatement("contentfile");

	/**
	 * List of mimetypes supported by the Gentics Image Store for resizing
	 */
	protected final static String CONFIGURATION_MIMETYPES_SUPPORTED = "images_mimetypes_supported";

	/**
	 * Max supported image dimentions
	 */
	protected final static String CONFIGURATION_MAX_DIMENSIONS = "images_maxdimensions";

	/**
	 * Loader for {@link FileService}s
	 */
	protected final static ServiceLoaderUtil<FileService> fileFactoryServiceLoader = ServiceLoaderUtil
			.load(FileService.class);

	/**
	 * Directory for the database files on the filesystem
	 */
	private java.io.File dbFilesDir;

	private static class FactoryFile extends ContentFile implements DisinheritableInternal<ContentFile> {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 5433084700403159839L;

		protected String name;
		protected String niceUrl;
		protected String filetype;
		protected Integer folderId;
		protected int filesize;
		protected String description;
		protected int sizeX;
		protected int sizeY;
		protected int dpiX;
		protected int dpiY;
		protected float fpX = 0.5f;
		protected float fpY = 0.5f;
		protected String md5;
		protected Integer editorId = 0;
		protected Integer creatorId = 0;
		protected ContentNodeDate cDate = new ContentNodeDate(0);
		protected ContentNodeDate customCDate = new ContentNodeDate(0);
		protected ContentNodeDate eDate = new ContentNodeDate(0);
		protected ContentNodeDate customEDate = new ContentNodeDate(0);
		protected boolean forceOnline;

		/**
		 * id of the channelset, if the object is master or local copy in multichannelling
		 */
		protected int channelSetId;

		/**
		 * id of the channel, if the object is a local copy in multichannelling
		 */
		protected int channelId;

		/**
		 * True if this object is the master, false if it is a localized copy
		 */
		protected boolean master = true;

		/**
		 * Map holding the localized variants of this file
		 */
		protected Map<Wastebin, Map<Integer, Integer>> channelSet;

		protected List<Integer> objectTagIds;

		/**
		 * This input stream will be used when the node object will be saved.
		 */
		protected InputStream updateInputStream;

		protected transient NodeObjectInfo info;

		/**
		 * whether multichannelling inheritance is excluded for this file
		 */
		protected boolean excluded = false;

		/**
		 * Flag indicating whether this file is disinherited by default in new channels (default: false).
		 */
		protected boolean disinheritDefault = false;

		/**
		 * whether the currently saved state has the excluded flag set
		 */
		private Boolean excludedInSavedObject;

		/**
		 * IDs of channels that are disinherited for this file
		 */
		protected Set<Integer> disinheritedChannelIds = null;

		/**
		 * Timestamp of deletion, if the object was deleted (and pub into the wastebin), 0 if object is not deleted
		 */
		protected int deleted;

		/**
		 * ID of the user who put the object into the wastebin, 0 if not deleted
		 */
		protected int deletedBy;

		/**
		 * Map of alternate URLs to their internal IDs in the DB (maps to null, if entry is new)
		 */
		protected ContentFileAlternateUrlsContainer alternateUrlsContainer;

		/**
		 * Create a new empty instance of a file
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected FactoryFile(NodeObjectInfo info) throws NodeException {
			super(null, info);
			// also generate an empty content
			Transaction t = TransactionManager.getCurrentTransaction();

			description = "";
		}

		/**
		 * Constructor for creating a copy of the given file
		 * @param id
		 * @param info
		 * @param name
		 * @param niceUrl nice URL
		 * @param filetype
		 * @param folderId
		 * @param filesize
		 * @param description
		 * @param sizeX
		 * @param sizeY
		 * @param dpiX
		 * @param dpiY
		 * @param fpX
		 * @param fpY
		 * @param md5
		 * @param creatorId
		 * @param editorId
		 * @param cDate
		 * @param eDate
		 * @param channelSetId
		 * @param channelId
		 * @param master
		 * @param forceOnline
		 * @param excluded
		 * @param disinheritDefault
		 * @param deleted
		 * @param deletedBy
		 * @param udate
		 * @param globalId
		 */
		public FactoryFile(Integer id, NodeObjectInfo info, String name, String niceUrl,
				String filetype, Integer folderId, int filesize, String description, int sizeX, int sizeY, int dpiX,
				int dpiY, float fpX, float fpY, String md5, int creatorId, int editorId, ContentNodeDate cDate, ContentNodeDate customCDate,
				ContentNodeDate eDate, ContentNodeDate customEDate, Integer channelSetId, Integer channelId, boolean master, boolean forceOnline,
				boolean excluded, boolean disinheritDefault, int deleted, int deletedBy, int udate, GlobalId globalId) {
			super(id, info);
			this.info = new NodeObjectInfoWrapper(info);
			this.name = name;
			this.niceUrl = niceUrl;
			this.filetype = ObjectTransformer.getString(filetype, "");
			this.folderId = folderId;
			this.filesize = filesize;
			this.description = description;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.dpiX = dpiX;
			this.dpiY = dpiY;
			this.fpX = fpX;
			this.fpY = fpY;
			this.md5 = md5;
			this.creatorId = creatorId;
			this.editorId = editorId;
			this.cDate = cDate;
			this.customCDate = customCDate;
			this.eDate = eDate;
			this.customEDate = customEDate;
			this.channelSetId = ObjectTransformer.getInt(channelSetId, 0);
			this.channelId = ObjectTransformer.getInt(channelId, 0);
			this.master = master;
			this.forceOnline = forceOnline;
			this.excluded = excluded;
			this.disinheritDefault = disinheritDefault;
			this.deleted = deleted;
			this.deletedBy = deletedBy;
			this.udate = udate;
			this.globalId = globalId;
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

		// public NodeObjectInfo getObjectInfo() {
		// return info;
		// }

		/**
		 * Returns the name of the file
		 */
		public String getName() {
			return name;
		}

		@Override
		public String getNiceUrl() {
			return niceUrl;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setDescription(java.lang
		 * .String)
		 */
		public String setName(String name) throws NodeException {
			failReadOnly();
			return null;
		}

		/**
		 * Returns the filetype of the file
		 * @return filetype
		 */
		public String getFiletype() {
			return filetype;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setFiletype(java.lang.String)
		 */
		public String setFiletype(String filetype) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#getFileStream()
		 */
		public InputStream getFileStream() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

			return fileFactory.loadFileContents(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#getFilesizeFromBinary()
		 */
		@Override
		public long getFilesizeFromBinary() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

			return fileFactory.loadFilesize(this);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#isBroken()
		 */
		public boolean isBroken() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
			return ObjectTransformer.isEmpty(filetype) || !fileFactory.getBinFile(this).exists();
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#isUsed(java.util.Map, java.util.Collection)
		 */
		public boolean isUsed(Map<Integer, Set<Integer>> usageMap, Collection<Integer> nodeIds) throws NodeException {
			Collection<Node> nodes = null;
			Transaction t = TransactionManager.getCurrentTransaction();
			if (ObjectTransformer.isEmpty(nodeIds)) {
				Node node = t.getChannel();
				if (node == null) {
					node = this.getNode();
				}
				nodes = Collections.singleton(node);
			} else {
				try (NoMcTrx noMc = new NoMcTrx()) {
					nodes = t.getObjects(Node.class, nodeIds);
				}
			}
			for (Node node : nodes) {
				if (DependencyManager.isFileUsed(usageMap, this, node)) {
					return true;
				}
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setFileStream(java.io.InputStream)
		 */
		public void setFileStream(InputStream stream) throws NodeException, ReadOnlyException {
			failReadOnly();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#performDelete()
		 */
		protected void performDelete() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);

			fileFactory.deleteFile(this);
		}

		@Override
		protected void putIntoWastebin() throws NodeException {
			if (isDeleted()) {
				return;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			Set<File> toPutIntoWastebin = new HashSet<File>();
			toPutIntoWastebin.add(this);
			if (isMaster()) {
				toPutIntoWastebin.addAll(t.getObjects(File.class, getChannelSet().values(), false, false));
			}

			for (File f : toPutIntoWastebin) {
				// mark file as being deleted
				DBUtils.executeUpdate(UPDATE_CONTENTFILE_DELETEFLAG, new Object[] {t.getUnixTimestamp(), t.getUserId(), f.getId()});
				DBUtils.executeUpdate(DELETE_CONTENTFILE_ONLINE, new Object[] { getId() });

				ActionLogger.logCmd(ActionLogger.WASTEBIN, File.TYPE_FILE, f.getId(), null, "File.delete()");
				t.dirtObjectCache(File.class, f.getId(), true);

				try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
					f = f.reload();
				}

				t.addTransactional(TransactionalTriggerEvent.deleteIntoWastebin(getFolder().getNode(), f));
			}

			// if the file is a localized copy, it was hiding other files (which are now "created")
			if (!isMaster()) {
				unhideFormerHiddenObjects(ContentFile.TYPE_FILE, getId(), getChannel(), getChannelSet());
			}
		}

		@Override
		public void restore() throws NodeException {
			if (!isDeleted()) {
				return;
			}

			// we need to restore the folder of this file as well (if in wastebin)
			getFolder().restore();

			// if this is a localized copy, we need to restore its master first
			// (if the master is in the wastebin too)
			if (!isMaster()) {
				getMaster().restore();
			}

			// restore the object
			Transaction t = TransactionManager.getCurrentTransaction();
			DBUtils.executeUpdate(UPDATE_CONTENTFILE_DELETEFLAG, new Object[] {0, 0, getId()});
			deleted = 0;
			deletedBy = 0;
			ActionLogger.logCmd(ActionLogger.WASTEBINRESTORE, File.TYPE_FILE, getId(), null, "File.restore()");
			channelSet = null;
			t.dirtObjectCache(File.class, getId(), true);
			t.addTransactional(new TransactionalTriggerEvent(this, null, Events.CREATE));

			if (!isMaster()) {
				hideFormerInheritedObjects(ContentFile.TYPE_FILE, getId(), getChannel(), getChannelSet());
			}

			// make the name unique
			FactoryFile editableFile = t.getObject(this, true);
			DisinheritUtils.makeUniqueDisinheritable(editableFile, SeparatorType.none, File.MAX_NAME_LENGTH);
			editableFile.save();
			editableFile.unlock();
		}

		public Folder getFolder() throws NodeException {
			Folder folder = (Folder) TransactionManager.getCurrentTransaction().getObject(Folder.class, folderId);

			// check for consistent data
			assertNodeObjectNotNull(folder, folderId, "folder");
			return folder;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setFolderId(int)
		 */
		public Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
			failReadOnly();
			return 0;
		}

		public int getFilesize() {
			return filesize;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setDescription(int)
		 */
		public int setFilesize(int filesize) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		public String getDescription() {
			return description;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setDescription(java.lang
		 * .String)
		 */
		public String setDescription(String description) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		public int getSizeX() {
			return sizeX;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setSizeX(int)
		 */
		public int setSizeX(int sizex) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		public int getSizeY() {
			return sizeY;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setSizeY(int)
		 */
		public int setSizeY(int sizey) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		public int getDpiX() {
			return dpiX;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setDpiX(int)
		 */
		public int setDpiX(int dpix) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		public int getDpiY() {
			return dpiY;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setDpiY(int)
		 */
		public int setDpiY(int dpiy) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		@Override
		public float getFpX() {
			return fpX;
		}

		@Override
		public float setFpX(float fpx) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		@Override
		public float getFpY() {
			return fpY;
		}

		@Override
		public float setFpY(float fpy) throws ReadOnlyException {
			failReadOnly();
			return 0;
		}

		/**
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ImageFile#isGisResizable()
		 */
		public boolean isGisResizable() {
			try {
				String[] mimeTypesSupported = TransactionManager.getCurrentTransaction().getNodeConfig()
						.getDefaultPreferences().getProperties(CONFIGURATION_MIMETYPES_SUPPORTED);

				if (mimeTypesSupported == null) {
					return false;
				}

				return Arrays.asList(mimeTypesSupported).contains(getFiletype());
			} catch (TransactionException e) {
				return false;
			}
		}

		public String getMd5() {
			return md5;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setMd5(java.lang.String)
		 */
		public String setMd5(String hash) throws ReadOnlyException {
			failReadOnly();
			return null;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#isForceOnline()
		 */
		public boolean isForceOnline() {
			return forceOnline;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#setForceOnline(boolean)
		 */
		public void setForceOnline(boolean forceOnline) throws ReadOnlyException {
			failReadOnly();
		}

		public Map<String, ObjectTag> getObjectTags() throws NodeException {

			// use level2 cache
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, ObjectTag> objectTags = (Map<String, ObjectTag>) t.getFromLevel2Cache(this, OBJECTTAGS);

			if (objectTags == null) {
				objectTags = loadObjectTags();
				t.putIntoLevel2Cache(this, OBJECTTAGS, objectTags);
			}
			return objectTags;
			// return loadObjectTags();
		}

		private synchronized void loadObjectTagIds() throws NodeException {
			if (objectTagIds == null) {
				if (isEmptyId(getId())) {
					objectTagIds = new ArrayList<Integer>();
				} else {
					objectTagIds = DBUtils.select("SELECT t.id as id"
							+ " FROM objtag t LEFT JOIN construct c ON c.id = t.construct_id"
							+ " WHERE obj_id = ? AND obj_type = ? AND c.id IS NOT NULL", ps -> {
						ps.setInt(1, getId());
						if (isImage()) {
							ps.setInt(2, TYPE_IMAGE);
						} else {
							ps.setInt(2, TYPE_FILE);
						}
					}, DBUtils.IDLIST);
				}
			}
		}

		protected Map<String, ObjectTag> loadObjectTags() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			Map<String, ObjectTag> objectTags = new HashMap<String, ObjectTag>();

			loadObjectTagIds();

			Node owningNode = getOwningNode();

			List<ObjectTag> tags = t.getObjects(ObjectTag.class, objectTagIds, getObjectInfo().isEditable());
			for (ObjectTag tag : tags) {
				ObjectTagDefinition def = tag.getDefinition();
				if (def != null && !def.isVisibleIn(owningNode)) {
					continue;
				}

				String name = tag.getName();

				if (name.startsWith("object.")) {
					name = name.substring(7);
				}

				objectTags.put(name, tag);
			}

			// when the file is editable, get all objecttags which are assigned to the file's node
			if (getObjectInfo().isEditable() && folderId != null) {
				List<ObjectTagDefinition> fileDefs = TagFactory.load(getTType(), Optional.ofNullable(owningNode));

				for (ObjectTagDefinition def : fileDefs) {
					// get the name (without object. prefix)
					String defName = def.getObjectTag().getName();

					if (defName.startsWith("object.")) {
						defName = defName.substring(7);
					}

					// if no objtag of that name exists for the file,
					// generate a copy and add it to the map of objecttags
					if (!objectTags.containsKey(defName)) {
						ObjectTag newObjectTag = (ObjectTag) def.getObjectTag().copy();

						newObjectTag.setNodeObject(this);
						newObjectTag.setEnabled(false);
						objectTags.put(defName, newObjectTag);
					}
				}

				// migrate object tags to new constructs, if they were changed
				for (ObjectTag tag : objectTags.values()) {
					tag.migrateToDefinedConstruct();
				}
			}

			return objectTags;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "ContentFile {" + getName() + ", " + getId() + "}";
		}

		public SystemUser getCreator() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "SystemUser");
			return creator;
		}

		public SystemUser getEditor() throws NodeException {
			SystemUser creator = (SystemUser) TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, editorId, "SystemUser");
			return creator;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public ContentNodeDate getCustomCDate() {
			return customCDate;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public ContentNodeDate getCustomEDate() {
			return customEDate;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#isChangedSince(int)
		 */
		public boolean isChangedSince(int timestamp) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement st = null;
			ResultSet res = null;

			try {
				// search for EDIT and CREATE in logcmd (for this file)
				// TODO use ActionLogger class to query logs
				st = t.prepareStatement(
						"SELECT count(*) c FROM logcmd WHERE " + "o_type in (?, ?) AND o_id = ? AND cmd_desc_id IN (?, ?, ?, ?, ?) AND logcmd.timestamp >= ?");
				int propCounter = 1;

				st.setInt(propCounter++, File.TYPE_FILE);
				st.setInt(propCounter++, ImageFile.TYPE_IMAGE);
				st.setObject(propCounter++, getId());
				st.setInt(propCounter++, ActionLogger.EDIT);
				st.setInt(propCounter++, ActionLogger.CREATE);
				st.setInt(propCounter++, ActionLogger.MOVE);
				st.setInt(propCounter++, ActionLogger.MC_HIDE);
				st.setInt(propCounter++, ActionLogger.MC_UNHIDE);
				st.setInt(propCounter++, timestamp);
				res = st.executeQuery();
				boolean changed = false;

				if (res.next()) {
					changed = (res.getInt("c") > 0);
				}

				return changed;
			} catch (SQLException e) {
				throw new NodeException("Error while checking whether {" + this + "} has been changed since {" + timestamp + "}");
			} finally {
				t.closeResultSet(res);
				t.closeStatement(st);
			}
		}

		/**
		 * Wrapper class for the object info, to return the correct class
		 */
		public class NodeObjectInfoWrapper implements NodeObjectInfo {

			/**
			 * wrapped objectinfo
			 */
			protected NodeObjectInfo wrapped;

			/**
			 * Create a wrapper instance
			 * @param wrapped wrapped object info
			 */
			public NodeObjectInfoWrapper(NodeObjectInfo wrapped) {
				this.wrapped = wrapped;
			}

			public NodeConfig getConfiguration() {
				return wrapped.getConfiguration();
			}

			public int getEditUserId() {
				return wrapped.getEditUserId();
			}

			public NodeFactory getFactory() {
				return wrapped.getFactory();
			}

			public String getHashKey() {
				return wrapped.getHashKey();
			}

			public Class getObjectClass() {
				return isImage() ? ImageFile.class : File.class;
			}

			public boolean isEditable() {
				return wrapped.isEditable();
			}

			public int getVersionTimestamp() {
				return wrapped.getVersionTimestamp();
			}

			public boolean isCurrentVersion() {
				return wrapped.isCurrentVersion();
			}

			public NodeObjectInfo getSubInfo(Class<? extends NodeObject> clazz) {
				return wrapped.getSubInfo(clazz);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			Class clazz = getObjectInfo().getObjectClass();

			return new EditableFactoryFile(this, getFactory().getFactoryHandle(clazz).createObjectInfo(clazz, true), true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#getChannelSet()
		 */
		public Map<Integer, Integer> getChannelSet() throws NodeException {
			Map<Wastebin, Map<Integer, Integer>> cSet = loadChannelSet();

			return new HashMap<>(cSet.get(TransactionManager.getCurrentTransaction().getWastebinFilter()));
		}

		/**
		 * Internal method to load the channelset
		 * @return channelset
		 * @throws NodeException
		 */
		protected synchronized Map<Wastebin, Map<Integer, Integer>> loadChannelSet() throws NodeException {
			if (!isEmptyId(channelSetId) && MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
			}
			// check for incomplete channelsets
			if (channelSet != null && !channelSet.keySet().containsAll(Arrays.asList(Wastebin.INCLUDE, Wastebin.EXCLUDE, Wastebin.ONLY))) {
				channelSet = null;
			}
			if (channelSet == null) {
				channelSet = MultichannellingFactory.loadChannelset(getObjectInfo().getObjectClass(), channelSetId);
			}
			return channelSet;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#getChannel()
		 */
		public Node getChannel() throws NodeException {
			if (!isEmptyId(channelId)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return (Node) t.getObject(Node.class, channelId, -1, false);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#getOwningNode()
		 */
		public Node getOwningNode() throws NodeException {
			try (NoMcTrx noMc = new NoMcTrx()) {
			return getFolder().getOwningNode();
		}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#getChannelSetId(boolean)
		 */
		public Integer getChannelSetId() throws NodeException {
			if (isEmptyId(channelSetId)) {
				throw new NodeException(this + " does not have a valid channelset_id");
			}
			return channelSetId;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#isInherited()
		 */
		public boolean isInherited() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				// multichannelling is not used, so the template cannot be inherited
				return false;
			}

			// determine the current channel
			Node channel = t.getChannel();

			if (channel == null || !channel.isChannel()) {
				return false;
			}
			// the template is inherited if its channelid is different from the current channel
			return ObjectTransformer.getInt(channel.getId(), -1) != ObjectTransformer.getInt(this.channelId, -1);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#isMaster()
		 */
		public boolean isMaster() throws NodeException {
			return master;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#getMasterNodeFolderName()
		 */
		/**
		 * Gets master Folder Name
		 * @return
		 * @throws NodeException
		 */
		public String getMasterNodeFolderName() throws NodeException {
			return getChannelMasterNode().getFolder().getName();
		}

		/**
		 * Gets the channel master Node or the master.
		 * @return channel master node or the master in case is not a channel.
		 * @throws NodeException
		 */
		public Node getChannelMasterNode() throws NodeException {
			Node masterNode = getMaster().getChannel();

			if (masterNode != null) {
				return masterNode;
			}

			Node node = getFolder().getNode();
			List<Node> masterNodes = node.getMasterNodes();

			if (masterNodes.size() > 0) {
				return masterNodes.get(masterNodes.size() - 1);
			} else {
				return node;
			}
		}

		@Override
		public boolean isExcluded() {
			return excluded;
		}

		@Override
		public void setExcluded(boolean value) throws ReadOnlyException {
			this.excluded  = value;
		}

		/**
		 * Indicates whether this file will be disinherited by default in
		 * newly created channels.
		 *
		 * @return <code>true</code> if the file will be disinherited in
		 *		new channels, <code>false</code> otherwise.
		 * @throws NodeException On internal errors
		 */
		@Override
		public boolean isDisinheritDefault() throws NodeException {
			if (isMaster()) {
				return disinheritDefault;
			} else {
				return getMaster().isDisinheritDefault();
			}
		}

		@Override
		public Set<Node> getDisinheritedChannels() throws NodeException {
			return DisinheritUtils.loadDisinheritedChannelsInternal(this);
		}

		@Override
		public Set<Integer> getOriginalDisinheritedNodeIds() {
			return disinheritedChannelIds;
		}

		@Override
		public void setOriginalDisinheritedNodeIds(Set<Integer> nodeIds) {
			this.disinheritedChannelIds = nodeIds;
		}

		@Override
		public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException {
			DisinheritUtils.updateDisinheritedNodeAssociations(this, excluded, disinheritedNodes, false);
		}

		/**
		 * Set whether this file should be disinherited by default in
		 * newly created channels.
		 *
		 * @see DisinheritUtils#updateDisinheritDefault
		 *
		 * @param value Set to <code>true</code> to disinherit this file in
		 *		new channels.
		 * @param recursive Unused for files.
		 */
		@Override
		public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException {
			if (!isMaster()) {
				return;
			}

			DisinheritUtils.updateDisinheritDefault(this, value);
			disinheritDefault = value;
		}

		@Override
		public OpResult move(Folder target, int targetChannelId) throws ReadOnlyException, NodeException {
			OpResult result = moveObject(this, target, targetChannelId, false);
			if (result.isOK()) {
				if (targetChannelId > 0) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Node targetChannel = t.getObject(Node.class, targetChannelId);
					if (targetChannel != null && targetChannel.isChannel()) {
						this.channelId = targetChannelId;
					} else {
						this.channelId = 0;
					}
				}
				this.folderId = target.getId();
			}
			return result;
		}

		@Override
		public FileInformation getFileInformation() throws TransactionException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
			return new FileInformation(fileFactory.getBinFile(this));
		}

		@Override
		public java.io.File getBinFile() throws TransactionException {
			Transaction t = TransactionManager.getCurrentTransaction();
			FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
			return fileFactory.getBinFile(this);
		}

		@Override
		public boolean isDeleted() {
			return deleted > 0;
		}

		@Override
		public int getDeleted() {
			return deleted;
		}

		@Override
		public SystemUser getDeletedBy() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, deletedBy);
		}

		/**
		 * Load alternate URLs if not done so before
		 * @throws NodeException
		 */
		private synchronized void loadAlternateUrls() throws NodeException {
			if (alternateUrlsContainer == null) {
				alternateUrlsContainer = new ContentFileAlternateUrlsContainer(this);
			}
		}

		@Override
		public Set<String> getAlternateUrls() throws NodeException {
			loadAlternateUrls();
			return alternateUrlsContainer.getAlternateUrls();
		}

		@Override
		public List<ExtensibleObjectService<File>> getServices() {
			return StreamSupport.stream(fileFactoryServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	/**
	 * Class for implementation of an editable file
	 */
	private static class EditableFactoryFile extends FactoryFile implements ExtensibleObject<File> {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3066436971501867958L;

		/**
		 * Flag to mark whether the file has been modified (contains changes
		 * which need to be persisted by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Flag to mark whether the filetype has been changed (eg. from image to file or from file to image)
		 */
		private boolean hasFileTypeChanged = false;

		/**
		 * Flag to mark whether the channelset of this page was changed, or not
		 */
		private boolean channelSetChanged = false;

		/**
		 * Flag to mark, whether the filetype was set "from outside" (using {@link #setFiletype(String)}).
		 */
		private boolean fileTypeSet = false;

		/**
		 * editable copy of this file's objecttags
		 */
		private Map<String, ObjectTag> editableObjectTags;

		/**
		 * Create a new empty instance of a file
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected EditableFactoryFile(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
			description = "";
		}

		/**
		 * Constructor for creating a copy of the given file
		 * @param file file
		 * @param info info about the copy
		 * @param asNewFile true when the editable copy shall represent a new
		 *        file, false if it shall be the editable version of the same
		 *        file
		 * @throws NodeException when an internal error occurred
		 * @throws ReadOnlyException when the file could not be fetched for
		 *         update
		 */
		protected EditableFactoryFile(FactoryFile file, NodeObjectInfo info, boolean asNewFile) throws ReadOnlyException, NodeException {
			super(asNewFile ? null : file.getId(), info, file.name, file.niceUrl, file.filetype, file.folderId, file.filesize, file.description, file.sizeX,
					file.sizeY, file.dpiX, file.dpiY, file.fpX, file.fpY, file.md5, file.creatorId, file.editorId, file.cDate, file.customCDate, file.eDate, file.customEDate, asNewFile ? 0 : file.channelSetId,
					file.channelId, file.master, file.forceOnline, file.excluded, file.disinheritDefault, asNewFile ? 0 : file.deleted, asNewFile ? 0 : file.deletedBy, asNewFile ? -1 : file.getUdate(), asNewFile ? null : file.getGlobalId());
			if (asNewFile) {
				// copy the objecttags
				Map<String, ObjectTag> originalObjectTags = file.getObjectTags();

				editableObjectTags = new HashMap<String, ObjectTag>(originalObjectTags.size());
				for (Iterator<Map.Entry<String, ObjectTag>> i = originalObjectTags.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, ObjectTag> entry = i.next();

					editableObjectTags.put(entry.getKey(), (ObjectTag) entry.getValue().copy());
				}

				// copy the file content
				updateInputStream = file.getFileStream();

				// copy the alternate URLs (which are not stored in the contentfile table)
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
					setAlternateUrls(file.getAlternateUrls());
				}

				modified = true;

			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.object.PageFactory.FactoryPage#getObjectTags()
		 */
		public Map<String, ObjectTag> getObjectTags() throws NodeException {
			if (editableObjectTags == null) {
				editableObjectTags = super.getObjectTags();
			}

			return editableObjectTags;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setFiletype(java.lang
		 * .String)
		 */
		public String setFiletype(String filetype) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.filetype, filetype)) {
				String oldFiletype = this.filetype;

				this.modified = true;
				this.fileTypeSet = true;
				this.filetype = filetype;
				return oldFiletype;
			} else {
				return this.filetype;
			}

		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#setName(java.lang.String)
		 */
		public String setName(String name) throws NodeException {
			assertEditable();
			if (StringUtils.isEqual(this.name, name)) {
				return name;
			} else {
				String oldName = this.name;

				this.modified = true;
				this.name = sanitizeName(name);

				return oldName;
			}
		}

		@Override
		public void setNiceUrl(String niceUrl) throws NodeException {
			assertEditable();
			niceUrl = AlternateUrlsContainer.NORMALIZER.apply(niceUrl);

			if (!StringUtils.isEqual(this.niceUrl, niceUrl)) {
				this.niceUrl = niceUrl;
				this.modified = true;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.File#setDescription(java.lang.String)
		 */
		public String setDescription(String description) throws ReadOnlyException {
			assertEditable();
			if (!StringUtils.isEqual(this.description, description)) {
				String oldDescription = this.description;

				this.modified = true;
				this.description = description;
				return oldDescription;
			} else {
				return this.description;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.File#setFolderId(java.lang.Integer)
		 */
		public Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
			// always set the folder id of the master folder
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = t.getObject(Folder.class, folderId);

			folderId = folder.getMaster().getId();

			if (ObjectTransformer.getInt(this.folderId, 0) != ObjectTransformer.getInt(folderId, 0)) {
				Integer oldFolderId = this.folderId;

				this.folderId = folderId;
				modified = true;

				return oldFolderId;
			} else {
				return this.folderId;
			}
		}

		@Override
		public void setCustomCDate(int timestamp) {
			modified |= customCDate.getIntTimestamp() != timestamp;
			customCDate = new ContentNodeDate(timestamp);
		}

		@Override
		public void setCustomEDate(int timestamp) {
			modified |= customEDate.getIntTimestamp() != timestamp;
			customEDate = new ContentNodeDate(timestamp);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setFileStream(java.io.
		 * InputStream)
		 */
		public void setFileStream(InputStream stream) throws NodeException, ReadOnlyException {
			assertEditable();
			if (this.updateInputStream != null) {
				try {
					this.updateInputStream.close();
				} catch (IOException ignored) {}
			}
			this.modified = true;
			this.updateInputStream = stream;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.File#setFilesize(int)
		 */
		public int setFilesize(int filesize) throws ReadOnlyException {
			assertEditable();
			// Check if filesize has changed
			if (!(filesize == this.filesize)) {
				int oldFilesize = this.filesize;

				this.modified = true;
				this.filesize = filesize;
				return oldFilesize;
			} else {
				return this.filesize;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setDpiX(int)
		 */
		public int setDpiX(int dpiX) throws ReadOnlyException {
			assertEditable();
			// Check if dpiX has changed
			if (!(dpiX == this.dpiX)) {
				int oldDpiX = this.dpiX;

				this.modified = true;
				this.dpiX = dpiX;
				return oldDpiX;
			} else {
				return this.dpiX;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setDpiY(int)
		 */
		public int setDpiY(int dpiY) throws ReadOnlyException {
			assertEditable();
			// Check if dpiX has changed
			if (!(dpiY == this.dpiY)) {
				int oldDpiY = this.dpiY;

				this.modified = true;
				this.dpiY = dpiY;
				return oldDpiY;
			} else {
				return this.dpiY;
			}
		}


		@Override
		public float setFpX(float fpX) throws ReadOnlyException {
			assertEditable();
			// Check if fpX has changed
			if (!(fpX == this.fpX)) {
				float oldfpX = this.fpX;

				this.modified = true;
				this.fpX = fpX;
				return oldfpX;
			} else {
				return this.fpX;
			}
		}

		@Override
		public float setFpY(float fpY) throws ReadOnlyException {
			assertEditable();
			// Check if fpY has changed
			if (!(fpY == this.fpY)) {
				float oldfpY = this.fpY;

				this.modified = true;
				this.fpY = fpY;
				return oldfpY;
			} else {
				return this.fpY;
			}
		}



		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setSizeX(int)
		 */
		public int setSizeX(int sizeX) throws ReadOnlyException {
			assertEditable();
			// Check if sizeX has changed
			if (!(sizeX == this.sizeX)) {
				int oldSizeX = this.sizeX;

				this.modified = true;
				this.sizeX = sizeX;
				return oldSizeX;
			} else {
				return this.sizeX;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.ContentFile#setSizeY(int)
		 */
		public int setSizeY(int sizeY) throws ReadOnlyException {
			assertEditable();
			// Check if sizeY has changed
			if (!(sizeY == this.sizeY)) {
				int oldSizeY = this.sizeY;

				this.modified = true;
				this.sizeY = sizeY;
				return oldSizeY;
			} else {
				return this.sizeY;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * com.gentics.contentnode.object.ContentFile#setMd5(java.lang.String)
		 */
		public String setMd5(String hash) throws ReadOnlyException {
			assertEditable();
			// Check if md5 hash has changed
			if (!StringUtils.isEqual(this.md5, hash)) {
				String oldDescription = this.md5;

				this.modified = true;
				this.md5 = hash;
				return oldDescription;
			} else {
				return this.md5;
			}
		}

		@Override
		public void setForceOnline(boolean forceOnline) throws ReadOnlyException {
			assertEditable();
			if (this.forceOnline != forceOnline) {
				this.forceOnline = forceOnline;
				modified = true;
			}
		}



		@Override
		public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
			// ignore this call, if the channelinfo is already set identical
			if (ObjectTransformer.getInt(this.channelId, 0) == ObjectTransformer.getInt(channelId, 0)
					&& this.channelSetId == ObjectTransformer.getInt(channelSetId, 0)) {
				return;
			}

			// check whether the file is new
			if (!isEmptyId(getId()) && (!allowChange || this.channelSetId != ObjectTransformer.getInt(channelSetId, 0))) {
				// the file is not new, so we must not set the channel
				// information
				throw new NodeException("Cannot change channel information for {" + this + "}, because the file is not new");
			}

			if (ObjectTransformer.getInt(channelId, 0) != 0) {
				// check whether channel exists
				Node channel = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId, -1, false);
				if (channel == null) {
					throw new NodeException("Error while setting channel information: channel {" + channelId + "} does not exist");
				}
				// if the channel exists, but is not channel, we set the channelId to 0 instead
				if (!channel.isChannel()) {
					channelId = 0;
				}
			}

			int iChannelId = ObjectTransformer.getInt(channelId, 0);
			int iChannelSetId = ObjectTransformer.getInt(channelSetId, 0);

			// check whether channelId was set to 0 and channelSetId not
			if (iChannelId == 0 && iChannelSetId != 0) {
				throw new NodeException(
						"Error while setting channel information: channelId was set to {" + channelId + "} and channelSetId was set to {" + channelSetId
						+ "}, which is not allowed (when creating master objects in non-channels, the channelSetId must be autogenerated)");
			}
			// set the data
			this.channelId = iChannelId;

			if (iChannelSetId == 0) {
				this.channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			} else {
				this.channelSetId = iChannelSetId;
			}
			channelSet = null;

			// set the "master" flag to false, because we are not yet sure,
			// whether this object is a master or not
			this.master = false;

			// now get the master object
			File master = getMaster();

			if (master == this) {
				this.master = true;
			} else {
				this.master = false;
			}

			modified = true;
			channelSetChanged = true;
		}

		@Override
		public void setAlternateUrls(Set<String> niceUrls) throws NodeException {
			getAlternateUrls();
			this.modified |= this.alternateUrlsContainer.set(niceUrls);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
		 */
		public void modifyChannelId(Integer channelId) throws ReadOnlyException,
					NodeException {
			if (isEmptyId(getId())) {
				throw new NodeException("Cannot modify the channelId for a new file");
			}
			if (isEmptyId(this.channelId)) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, since the file does not belong to a channel");
			}
			if (!isMaster()) {
				throw new NodeException("Cannot modify the channelId for {" + this + "}, because it is no master");
			}

			// read the channelset
			Map<Integer, Integer> channelSet = getChannelSet();
			Integer oldChannelId = this.channelId;

			if (isEmptyId(channelId)) {
				this.channelId = 0;
				modified = true;
			} else {
				List<Node> masterNodes = getChannel().getMasterNodes();
				boolean foundMasterNode = false;

				for (Node node : masterNodes) {
					if (node.getId().equals(channelId)) {
						foundMasterNode = true;
						break;
					}
				}

				if (!foundMasterNode) {
					throw new NodeException(
							"Cannot modify the channelId for {" + this + "} to {" + channelId + "}, because this is no master channel of the file's channel");
				}

				this.channelId = ObjectTransformer.getInt(channelId, 0);
				modified = true;
			}

			// modify the channelset, since this object moved from one channel to another
			channelSet.remove(oldChannelId);
			channelSet.put(this.channelId, getId());
			channelSetChanged = true;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			// first check whether the file is editable
			assertEditable();

			boolean isModified = modified;
			boolean isNew = getId() == null;
			boolean binaryUpdate = (updateInputStream != null);

			Transaction t = TransactionManager.getCurrentTransaction();

			// now check whether the object has been modified
			if (isModified) {
				// object is modified, so update it
				saveFileObject(this);
				modified = false;
			}

			// save all the objecttags and check which tags no longer exist (and need to be removed)
			Map<String, ObjectTag> tags = getObjectTags();
			List<Integer> tagIdsToRemove = new Vector<Integer>();

			if (objectTagIds != null) {
				tagIdsToRemove.addAll(objectTagIds);
			}
			for (Iterator<ObjectTag> i = tags.values().iterator(); i.hasNext();) {
				ObjectTag tag = i.next();

				tag.setNodeObject(this);
				isModified |= tag.save();

				// do not remove the tag, which was saved
				tagIdsToRemove.remove(tag.getId());
			}

			// eventually remove tags which no longer exist
			if (!tagIdsToRemove.isEmpty()) {
				List<ObjectTag> tagsToRemove = t.getObjects(ObjectTag.class, tagIdsToRemove);

				for (Iterator<ObjectTag> i = tagsToRemove.iterator(); i.hasNext();) {
					NodeObject tagToRemove = i.next();

					tagToRemove.delete();
				}
				isModified = true;
			}

			if (isModified) {
				// dirt the file cache
				t.dirtObjectCache(File.class, getId());
			}

			// We need to dirt the parent folder when the filetype was changed.
			// Otherwise the file list / image list will not be displayed correctly
			if (isModified && hasFileTypeChanged) {
				Folder parentFolder = getFolder();
				if (parentFolder != null) {
					t.dirtObjectCache(Folder.class, parentFolder.getId());
				}
			}

			// if the channelset changed, we need to dirt all other files of the channelset as well
			if (channelSetChanged || MultichannellingFactory.isEmpty(channelSet)) {
				channelSet = null;
				Map<Integer, Integer> locChannelSet = getChannelSet();

				// dirt caches for all pages in the channelset
				for (Map.Entry<Integer, Integer> channelSetEntry : locChannelSet.entrySet()) {
					t.dirtObjectCache(getObjectInfo().getObjectClass(), channelSetEntry.getValue());
				}

				channelSetChanged = false;
			}

			if (isModified) {
				onSave(this, isNew, binaryUpdate, t.getUserId());
			}
			if (isNew) {
				updateMissingReferences();
			}

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			ImageFile oFile = (ImageFile) original;

			// set Meta Information and binary content
			setName(oFile.getName());
			setDescription(oFile.getDescription());
			setDpiX(oFile.getDpiX());
			setDpiY(oFile.getDpiY());
			setFpX(oFile.getFpX());
			setFpY(oFile.getFpY());
			setSizeX(oFile.getSizeX());
			setSizeY(oFile.getSizeY());
			setFileStream(oFile.getFileStream());

			// copy object tags
			Map<String, ObjectTag> thisOTags = getObjectTags();
			Map<String, ObjectTag> originalOTags = oFile.getObjectTags();

			for (Map.Entry<String, ObjectTag> entry : originalOTags.entrySet()) {
				String tagName = entry.getKey();
				ObjectTag originalTag = entry.getValue();

				if (thisOTags.containsKey(tagName)) {
					// found the tag in this file, copy the original tag over it
					thisOTags.get(tagName).copyFrom(originalTag);
				} else {
					// did not find the tag, so copy the original
					thisOTags.put(tagName, (ObjectTag) originalTag.copy());
				}
			}

			// remove all tags that do not exist in the original file
			for (Iterator<Map.Entry<String, ObjectTag>> i = thisOTags.entrySet().iterator(); i.hasNext();) {
				Entry<String, ObjectTag> entry = i.next();

				if (!originalOTags.containsKey(entry.getKey())) {
					i.remove();
				}
			}
		}

		@Override
		public Integer getChannelSetId() throws NodeException {
			// check if the object is new and does not have a channelset_id
			if (isEmptyId(channelSetId) && isEmptyId(id)) {
				// create a new channelset_id
				channelSetId = ObjectTransformer.getInt(createChannelsetId(), 0);
			}

			return super.getChannelSetId();
		}
	}

	public FileFactory() {
		super();
		reloadConfiguration();
	}

	/**
	 * Reloads the configuration
	 */
	public synchronized void reloadConfiguration() {
		dbFilesDir = new java.io.File(ConfigurationValue.DBFILES_PATH.get());
		if (!dbFilesDir.exists()) {
			if (!dbFilesDir.mkdirs()) {
				logger.error(String.format("Failed to create dbfiles directory %s", dbFilesDir.getAbsolutePath()));
			}
		}
	}

	/**
	 * Deletes a file
	 * @param file File to delete
	 * @throws NodeException if an error occurs
	 */
	protected void deleteFile(File file) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<File> deleteList = getDeleteList(t, File.class);

		deleteList.add(file);

		// when multichannelling is active and the file is a master, also get all localized objects and
		// remove them
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && file.isMaster()) {
			Map<Integer, Integer> channelSet = file.getChannelSet();

			for (Integer fileId : channelSet.values()) {
				File locFile = (File)t.getObject(file.getObjectInfo().getObjectClass(), fileId, -1, false);

				if (!deleteList.contains(locFile)) {
					deleteList.add(locFile);
				}
			}
		}
	}

	/**
	 * @see #copyFile(File, String)
	 */
	public File copyFile(File orgFile) throws NodeException {
		return copyFile(orgFile, null);
	}

	/**
	 * Create a copy of the given file with the specified name.
	 * @param orgFile The file to copy
	 * @param newFileName The filename for the copy
	 * @return The created copy
	 * @throws NodeException
	 */
	public File copyFile(File orgFile, String newFileName) throws NodeException {
		// Create a copy of the given file
		FactoryFile newFile = (FactoryFile) orgFile.copy();

		// Remove the id from the object. When saving the object this will
		// lead to the creation of a new contentfile record.
		newFile.setId(null);

		Node channel = orgFile.getChannel();

		// set the channel info (create a new master object)
		newFile.setChannelInfo(0, 0);

		if (channel != null) {
			// The first call to setChannelInfo() above is to make sure isMaster is correctly set on the copied file.
			// This call is to make sure the file is in the correct channel when determining a filename later on.
			newFile.setChannelInfo(channel.getId(), 0);
		}

		if (newFileName != null) {
			newFile.setName(newFileName);
		}

		suggestNewFilename(newFile);

		return newFile;
	}

	/**
	 * Get list of properties which are different between the two file instances
	 * @param original original file
	 * @param updated update file
	 * @return list of changed properties
	 * @throws NodeException
	 */
	private static List<String> getChangedProperties(ImageFile original, ImageFile updated) throws NodeException {
		List<String> modified = new Vector<String>();

		if (original == null || updated == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("File to compare with was null returning empty property set");
			}
			return modified;
		}
		if (updated.isImage()) {
			if (original.getSizeX() != updated.getSizeX()) {
				modified.add("sizex");
			}

			if (original.getSizeY() != updated.getSizeY()) {
				modified.add("sizey");
			}

			if (original.getDpiX() != updated.getDpiX()) {
				modified.add("dpix");
			}

			if (original.getDpiY() != updated.getDpiY()) {
				modified.add("dpiy");
			}

			if (original.getFpX() != updated.getFpX()) {
				modified.add("fpx");
			}

			if (original.getFpY() != updated.getFpY()) {
				modified.add("fpy");
			}
		}

		if (!(original.getFilesize() == updated.getFilesize())) {
			modified.add("size");
		}

		if (!StringUtils.isEqual(original.getFiletype(), updated.getFiletype())) {
			modified.add("type");
		}

		if (original.isImage() != updated.isImage()) {
			modified.add("isimage");
			modified.add("isfile");
		}

		if (!StringUtils.isEqual(original.getDescription(), updated.getDescription())) {
			modified.add("description");
		}

		if (!StringUtils.isEqual(original.getName(), updated.getName())) {
			modified.add("name");
		}

		if (!StringUtils.isEqual(original.getMd5(), updated.getMd5())) {
			modified.add("binarycontent");
		}

		if (!original.getEditor().equals(updated.getEditor())) {
			modified.add("editor");
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (!StringUtils.isEqual(original.getNiceUrl(), updated.getNiceUrl())) {
				modified.add("nice_url");
			}
			if (!Objects.deepEquals(original.getAlternateUrls(), updated.getAlternateUrls())) {
				modified.add("alternate_urls");
			}
		}

		modified.add("edate");

		return modified;
	}

	/**
	 * This method will try to determine the MimeType by examining the file header.
	 * The file extension will be used when the mimetype could not be determined
	 * in the file content.
	 *
	 * @param file
	 * @param fileFactory
	 * @throws NodeException
	 * @return Whether the filetype (image or file) of the image got changed
	 */
	private static boolean setDeterminedMimeType(FactoryFile file, FileFactory fileFactory, boolean isNew) throws NodeException {
		// Check whether the filetype changes when we identify the mimetype
		boolean isImage = file.isImage();
		InputStream inputStream = fileFactory.loadFileContents(file);
		String mimeType = FileUtil.getMimeType(inputStream, file.getName());

		try {
			inputStream.close();
		} catch (IOException e1) {
			// We don't care :)
		}

		file.setFiletype(mimeType);

		// The filetype can only be changed for previously existing files
		if (!isNew) {
			boolean hasChanged = isImage != file.isImage();
			return hasChanged;
		}

		return false;
	}

	/**
	 * Save the given file.
	 * @param file
	 * @throws NodeException
	 */
	private static void saveFileObject(EditableFactoryFile file) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		// make sure that the file has a channelset_id
		file.getChannelSetId();

		FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
		boolean isNew = ContentFile.isEmptyId(file.getId());

		File orgFile = null;

		if (!isNew) {
			// Get the original file for comparison
			orgFile = (File) t.getObject(File.class, file.getId());
		}
		// Only save the binary content if a source was defined by setting
		// updateInputStream
		boolean binaryUpdate = (file.updateInputStream != null);

		// set editor data
		file.editorId = t.getUserId();
		file.eDate = new ContentNodeDate(t.getUnixTimestamp());

		if (!StringUtils.isEmpty(file.description)) {
			file.description = file.description.substring(0, Math.min(file.description.length(), File.MAX_DESCRIPTION_LENGTH));
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (!ObjectTransformer.isEmpty(file.niceUrl)) {
				file.niceUrl = makeFileNiceURLUnique(file, file.niceUrl);
			}
			if (file.alternateUrlsContainer != null && !file.alternateUrlsContainer.isEmpty()) {
				final File finalFile = file;
				file.alternateUrlsContainer.modify(url -> makeFileNiceURLUnique(finalFile, url));
			}
		} else {
			file.niceUrl = null;
		}

		// Check if this is a new file
		if (isNew) {
			// make sure, that the channelId is set correctly
			if (file.master) {
				file.channelId = MultichannellingFactory.correctChannelId(ObjectTransformer.getInt(file.folderId, 0), file.channelId);
			}

			// set creator data
			file.creatorId = t.getUserId();
			file.cDate = new ContentNodeDate(t.getUnixTimestamp());

			// Insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_CONTENTFILE_SQL,
					new Object[] {
				file.name, file.niceUrl, file.filetype, file.folderId, file.filesize, file.creatorId, file.cDate.getIntTimestamp(), file.customCDate.getIntTimestamp(), file.editorId,
				file.eDate.getIntTimestamp(), file.customEDate.getIntTimestamp(), file.description, file.sizeX, file.sizeY, "", file.dpiX, file.dpiY, file.fpX, file.fpY, file.channelSetId, file.channelId, file.master,
				file.forceOnline, file.excluded, file.disinheritDefault, ObjectTransformer.getString(file.getGlobalId(), "")});

			if (keys.size() == 1) {
				// set the new file id
				file.setId(keys.get(0));
				synchronizeGlobalId(file);
			} else {
				throw new NodeException("Error while inserting new file, could not get the insertion id");
			}
			ActionLogger.logCmd(ActionLogger.CREATE, File.TYPE_FILE, file.getId(), file.getFolder().getId(), "cmd_file_create-java");

			if (!file.isMaster()) {
				hideFormerInheritedObjects(File.TYPE_FILE, file.getId(), file.getChannel(), file.getChannelSet());
			}

			Disinheritable<?> restrictionSource = file.isMaster() ? file.getFolder().getMaster() : file.getMaster();
			DisinheritUtils.saveNewDisinheritedAssociations(file, restrictionSource.isExcluded(), restrictionSource.getDisinheritedChannels());
			file.setDisinheritDefault(
				file.disinheritDefault || restrictionSource.isDisinheritDefault(),
				false);
		}

		// store nice URLs.
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS) && file.alternateUrlsContainer != null) {
			file.alternateUrlsContainer.save();
			file.alternateUrlsContainer = null;
		}

		// Check if the file is new or moved,renamed
		if (isNew || file.getFolder().getId() != orgFile.getFolder().getId() || !file.getName().equals(orgFile.getName())) {
			// Check if the file must have a new filename to prevent conflicts
			// within pubpath
			suggestNewFilename(file);
		}

		// Check if we need to update/create the binary content as well
		if (binaryUpdate) {
			try {
				file = (EditableFactoryFile) fileFactory.storeFileContents(file.updateInputStream, file, isNew);
			} finally {
				try {
					file.updateInputStream.close();
				} catch (IOException e2) {// ignore this
				}
				file.updateInputStream = null;
			}

			// Identify the mimetype of the file and check if it changed with the new file
			file.hasFileTypeChanged = setDeterminedMimeType(file, fileFactory, isNew);

			ActionLogger.logCmd(ActionLogger.EDIT, File.TYPE_FILE, file.getId(), file.getFolder().getId(), "cmd_file_data-java");

			// If this is an image and if its content has been updated we have to load it and check its attributes
			if (file.isImage()) {
				// Load image dimentions, if possible
				Point dim;
				try (InputStream detect = fileFactory.loadFileContents(file)) {
					dim = JavaImageUtils.getImageDimensions(detect, file.getFiletype());
				} catch (Throwable throwable) {
					logger.info("Could not detect image dimensions for image " + file.getName() + " (id " + file.getId() + ")", throwable);
					dim = null;
				}
				// Check the image dimensions limit
				{
					int[] imageSizeLimits = null;
					NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
					String imagesizelimit = ObjectTransformer.getString(prefs.getProperty(CONFIGURATION_MAX_DIMENSIONS), DEFAULT_MAX_DIMENSIONS);
					if (org.apache.commons.lang3.StringUtils.isNotBlank(imagesizelimit) && imagesizelimit.indexOf("x") > 0) {
						String[] imageSizeLimits1 = imagesizelimit.split("x", 2);
						try {
							imageSizeLimits = new int[] { Integer.parseInt(imageSizeLimits1[0]), Integer.parseInt(imageSizeLimits1[1]) };
						} catch (NumberFormatException e) {
							logger.warn("Could not parse image max dimentions config", e);
						}
					}
					if (dim != null && imageSizeLimits != null && imageSizeLimits.length == 2) {
						if (imageSizeLimits[0] < dim.x || imageSizeLimits[1] < dim.y) {
							throw new NodeException(I18NHelper.get("image.exceeds.maxdimensions", file.getName(), file.getId().toString(), file.getGlobalId().toString(), Integer.toString(dim.x), Integer.toString(dim.y), Integer.toString(imageSizeLimits[0]), Integer.toString(imageSizeLimits[1])));
						}
					}
				}
				try {
					if (dim != null) {
						file.setSizeX(dim.x);
						file.setSizeY(dim.y);

						try (InputStream detect = fileFactory.loadFileContents(file)) {
							// Extract image resolution meta information and store it within file
							// If the DPI metadata information can not be read, it will use default DPI values.
							Point dpi = JavaImageUtils.getImageDpiResolution(detect);

							file.setDpiX(dpi.x);
							file.setDpiY(dpi.y);
						} catch (IOException e) {
							logger.warn("Error while closing Stream to data of {" + file + "} - But this does not really matter.", e);
						}
					} else {
						file.setSizeX(0);
						file.setSizeY(0);
						file.setDpiX(0);
						file.setDpiY(0);
					}
				} catch (NodeException e) {
					logger.warn("Could not parse data stream from file {" + file.getId() + "}", e);

					file.setDpiX(0);
					file.setDpiY(0);
					file.setSizeX(0);
					file.setSizeY(0);
				}
			}
		}

		// Update the contentfile record since some attributes may have changed.
		DBUtils.executeUpdate(UPDATE_CONTENTFILE_SQL,
				new Object[] {
			file.name, file.niceUrl, file.filetype, file.folderId, file.filesize, file.editorId, file.customCDate.getIntTimestamp(), file.eDate.getIntTimestamp(), file.customEDate.getIntTimestamp(), file.description, file.sizeX,
			file.sizeY, file.md5, file.dpiX, file.dpiY, file.fpX, file.fpY, file.channelSetId, file.channelId, file.forceOnline, file.getId()});

		// Throw events
		if (isNew) {
			t.addTransactional(new TransactionalTriggerEvent(File.class, file.getId(), null, Events.CREATE));
		} else {
			ActionLogger.logCmd(ActionLogger.EDIT, File.TYPE_FILE, file.getId(), file.getFolder().getId(), "cmd_file_update-java");

			// Find all attributes that have changed between both files
			List<String> attributes = FileFactory.getChangedProperties((ImageFile) orgFile, (ImageFile) file);

			t.addTransactional(new TransactionalTriggerEvent(File.class, file.getId(), (String[]) attributes.toArray(new String[0]), Events.UPDATE));
		}

		// Check if file was moved
		if (orgFile != null && orgFile.getFolder().getId() != file.getFolder().getId()) {

			String[] attributes = { "files", "images"};

			// Log that the file was moved
			ActionLogger.logCmd(ActionLogger.EDIT, File.TYPE_FILE, file.getId(), file.getFolder().getId(), "cmd_file_move-java");

			// Trigger file move event
			t.addTransactional(new TransactionalTriggerEvent(File.class, file.getId(), null, Events.MOVE));

			// Notify target folder of new file arrival
			t.addTransactional(new TransactionalTriggerEvent(Folder.class, file.getFolder().getId(), attributes, Events.UPDATE));

			// Notify source folder of moved file
			t.addTransactional(new TransactionalTriggerEvent(Folder.class, orgFile.getFolder().getId(), attributes, Events.UPDATE));

		}
	}

	/**
	 * Ensures the specified file has a unique Nice URL (if any)
	 *
	 * @param file file to work on
	 * @param niceUrl nice URL to make unique
	 * @return unique nice URL
	 * @throws NodeException
	 */
	private static String makeFileNiceURLUnique(File file, String niceUrl) throws NodeException {
		// nothing to do, when the page has no nice URL
		if (StringUtils.isEmpty(niceUrl)) {
			return niceUrl;
		}

		Pattern extNiceUrlPattern = Pattern.compile("^(?<path>.*)(?<number>[1-9][0-9]+)?(?<extension>\\.[^\\./]+)$");
		Pattern niceUrlPattern = Pattern.compile("^(?<path>.*)(?<number>[1-9][0-9]+)?$");

		String pathPart = niceUrl;
		int counter = 0;
		String extension = "";

		Matcher m = extNiceUrlPattern.matcher(pathPart);
		if (m.matches()) {
			pathPart = m.group("path");
			counter = Integer.parseInt(ObjectTransformer.getString(m.group("number"), "0"));
			extension = ObjectTransformer.getString(m.group("extension"), "");
		} else {
			m = niceUrlPattern.matcher(pathPart);
			if (m.matches()) {
				pathPart = m.group("path");
				counter = Integer.parseInt(ObjectTransformer.getString(m.group("number"), "0"));
			}
		}

		Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(
				NodeObjectWithAlternateUrls.PATH.apply(niceUrl), new ChannelTreeSegment(file, false));
		NodeObject obstructor = DisinheritUtils.getObjectUsingNiceURL(file, niceUrl, pcf, null);
		while (obstructor != null) {
			counter++;
			String toAdd = String.format("%d%s", counter, extension);
			if (pathPart.length() + toAdd.length() > Page.MAX_NICE_URL_LENGTH) {
				niceUrl = String.format("%s%s", pathPart.substring(0, Page.MAX_NICE_URL_LENGTH - toAdd.length()), toAdd);
			} else {
				niceUrl = String.format("%s%s", pathPart, toAdd);
			}
			obstructor = DisinheritUtils.getObjectUsingNiceURL(file, niceUrl, pcf, null);
		}

		return niceUrl;
	}

	/**
	 * Suggests a new filename for the file if the filename is already reserved
	 * because of an existing file with the same name in the same pubdir.
	 *
	 * @param file
	 * @return
	 */
	public static String suggestNewFilename(File file) throws NodeException {
		ChannelTreeSegment targetSegment = new ChannelTreeSegment(file, false);
		Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(file.getFolder(), targetSegment);

		if (file.getName() == null) {
			throw new NodeException("Filename must not be null. Can't check for duplicate files and suggest a new filename");
		}

		Set<String> obstructors = DisinheritUtils.getUsedFilenames(file, CNStringUtils.escapeRegex(file.getFilename()), pcf, null).keySet();
		String uniqueFilename = UniquifyHelper.makeFilenameUnique(file.getName(), obstructors);
		file.setName(uniqueFilename);
		return uniqueFilename;
	}

	/**
	 * Replace invalid characters in the file name according to the configuration
	 * @param name The filename
	 * @return The cleaned file name
	 * @throws NodeException
	 */
	public static String sanitizeName(String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();

		Map<String, String> sanitizeCharacters = nodePreferences.getPropertyMap(
				"sanitize_character");
		String replacementChararacter = nodePreferences.getProperty(
				"sanitize_replacement_character");
		String[] preservedCharacters = nodePreferences.getProperties(
				"sanitize_allowed_characters");

		if (!StringUtils.isEmpty(name)) {
			name = name.trim();
		}
		return FileUtil.sanitizeName(
				name, sanitizeCharacters, replacementChararacter, preservedCharacters);
	}

	/**
	 * Check whether the file proposed filename is available
	 * @param file file with proposed filename
	 * @return the other object using the filename or null if the filename is available
	 * @throws NodeException
	 */
	public static NodeObject getFilenameObstructor(File file) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot check filename availability without file");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(file, false);
		Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(file.getFolder(), objectSegment);

		return DisinheritUtils.getFilenameObstructor(file, pcf, () -> file.getName());
	}

	/**
	 * Check whether the file's proposed nice URL is available
	 * @param file file for which the nice URL is proposed
	 * @param niceUrl proposed nice URL
	 * @return first found conflicting object or null if the nice URL is available
	 * @throws NodeException
	 */
	public static NodeObject isNiceUrlAvailable(File file, String niceUrl) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot check nice URL availability without file");
		}
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(file, false);
		Set<Folder> pcf = DisinheritUtils
				.getFoldersWithPotentialObstructors(NodeObjectWithAlternateUrls.PATH.apply(niceUrl), objectSegment);
		return DisinheritUtils.getObjectUsingNiceURL(file, niceUrl, pcf, null);
	}

	/**
	 * Stores the contents of a input stream into database or within the
	 * filesystem.
	 * @param stream Stream from with data will be read.
	 * @param file File that should be stored
	 * @param isNew
	 * @return File with new attributes (MD5, Filesize) that were computed during writing phase
	 * @throws NodeException
	 */
	protected File storeFileContents(InputStream stream, File file, boolean isNew) throws NodeException {
		if (stream == null) {
			throw new NodeException("Error while storing binary content for file with id { " + file.getId() + " } in the filesystem. Source stream was null.");
		}

		// Transform stream into hashingStream
		try (MD5InputStream hashingStream = new MD5InputStream(stream)) {
			try (CountingInputStream countingStream = new CountingInputStream(hashingStream)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				// Get GCN Filesizelimit
				NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
				int filesizelimit = ObjectTransformer.getInt(prefs.getProperty("contentnode.maxfilesize"), -1);

				// check whether the current user is member of one of the groups defined in NO_MAXFILESIZE
				String[] noFilesizeLimitGroupIds = prefs.getProperties("no_max_filesize");
				if (!ObjectTransformer.isEmpty(noFilesizeLimitGroupIds)) {
					SystemUser currentUser = t.getObject(SystemUser.class, t.getUserId());
					if (currentUser != null) {
						List<String> groupIds = Arrays.asList(noFilesizeLimitGroupIds);
						if (currentUser.getUserGroups().stream().anyMatch(group -> groupIds.contains(group.getId().toString()) || groupIds.contains(group.getName()))) {
							filesizelimit = 0;
						}
					}
				}

				java.io.File tmpFile = null;
				java.io.File targetBackupFile = null;
				try {
					tmpFile = new java.io.File(System.getProperty("java.io.tmpdir"), file.getId() + ".tmp");
					java.io.File targetFile = new java.io.File(dbFilesDir, file.getId() + ".bin");

					logger.debug("Write content from MD5InputStream into dbfile with id { " + file.getId() + " } -> " + targetFile.getAbsolutePath());

					// Write the temp file
					try (FileOutputStream os = new FileOutputStream(tmpFile)) {
						FileUtil.inputStreamToOutputStream(countingStream, os);
					}

					// Check for 0b files
					if (countingStream.getByteCount() == 0) {
						I18nString i18nMessage = new CNI18nString("rest.file.upload.empty_file");
						throw new FileSizeException(i18nMessage.toString());
					}

					// Check filesizelimit
					if (filesizelimit > 0 && (countingStream.getByteCount() > filesizelimit)) {
						logger.info("Filesize limit exceeded - deleting tmp dbfile");

						I18nString i18nMessage = new CNI18nString("rest.file.upload.limit_reached");

						i18nMessage.setParameter("0", FileUtils.byteCountToDisplaySize(filesizelimit));
						i18nMessage.setParameter("1", FileUtils.byteCountToDisplaySize(countingStream.getByteCount() - filesizelimit));
						throw new FileSizeException(i18nMessage.toString());
					}

					// Check was successful - now swap the files for real
					if (!isNew) {
						// First: move the old dbfile to a save place
						targetBackupFile = new java.io.File(System.getProperty("java.io.tmpdir"), file.getId() + ".org");

						try {
							Files.move(targetFile.toPath(), targetBackupFile.toPath(), REPLACE_EXISTING);
						} catch (IOException e) {
							logger.error("Error while moving file", e);
							throw new NodeException("Could not move old db file " + targetFile.getAbsolutePath() + " to " + targetBackupFile.getAbsolutePath());
						}

						// Second: move the new tmp dbfile to the correct place
						try {
							Files.move(tmpFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
						} catch (IOException e) {
							logger.error("Could not copy new file {" + tmpFile.getAbsolutePath() + "} to dbfiles folder {" + targetFile.getAbsolutePath() + "}");
							// Try to recover the backup file
							Files.move(targetBackupFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
							throw new NodeException(
									"Could not move tmp file " + tmpFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath(), e);
						}
					} else {
						// Move the tmp file to the final dbfile destination
						Files.move(tmpFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
					}
				} catch (IOException e) {
					throw new NodeException("Error while storing binary content for file with id { " + file.getId() + " } in the filesystem.", e);
				} finally {
					for (java.io.File toClean : Arrays.asList(tmpFile, targetBackupFile)) {
						if (toClean != null && toClean.exists()) {
							toClean.delete();
						}
					}
				}

				// The hashing stream calculated the md5 hash while reading from it.
				file.setMd5(MD5.asHex(hashingStream.hash()));
				// The byte count calculated while reading from stream
				file.setFilesize((int) countingStream.getByteCount());
			}
		} catch (IOException e) {
			throw new NodeException("Error while closing streams", e);
		}
		return file;
	}

	/**
	 * Get the binary file for the given content file
	 * @param file content file
	 * @return binary file
	 */
	protected java.io.File getBinFile(File file) {
		return new java.io.File(dbFilesDir, file.getId() + ".bin");
	}

	protected long loadFilesize(File file) throws NodeException {
		boolean contentFileDataInDb = getConfiguration().getDefaultPreferences().getFeature("contentfile_data_to_db");
		if (!contentFileDataInDb) {
			java.io.File dbFile = getBinFile(file);
			return dbFile.length();
		} else {
			logger.debug("Fetching binarycontent length of contentfiledata record for file with id { " + file.getId() + " } ");
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;

			try {
				stmt = t.prepareStatement("SELECT LENGTH(binarycontent) AS length FROM contentfiledata WHERE contentfile_id = ?");
				stmt.setObject(1, file.getId());
				rs = stmt.executeQuery();
				if (rs.next()) {
					return rs.getLong("length");
				} else {
					throw new NodeException("Did not find the binary content for file with id { " + file.getId() + " } in the database");
				}
			} catch (SQLException e) {
				throw new NodeException("Error while loading binary content for file with id { " + file.getId() + " } in the database");
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}
	}

	/**
	 * Loads the contents of a file and returns an InputStream from which the
	 * file contents can be read.
	 * @param file File from which to read the contents.
	 * @return InputStream that contains the content of the file.
	 */
	protected InputStream loadFileContents(File file) throws NodeException {
		boolean contentFileDataInDb = getConfiguration().getDefaultPreferences().getFeature("contentfile_data_to_db");

		if (!contentFileDataInDb) {
			try {
				java.io.File dbFile = getBinFile(file);
				FileInputStream in = new FileInputStream(dbFile);

				return in;
			} catch (FileNotFoundException e) {
				logger.error("Could not load contents of file { " + file.getId() + " }", e);
				return null;
			}
		} else {
			logger.debug("Fetching binarycontent of contentfiledata record for file with id { " + file.getId() + " } ");
			Transaction t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;

			try {
				stmt = t.prepareStatement("SELECT binarycontent FROM contentfiledata WHERE contentfile_id = ?");
				stmt.setObject(1, file.getId());
				rs = stmt.executeQuery();
				if (rs.next()) {
					return new ByteArrayInputStream(rs.getBytes("binarycontent"));
				} else {
					throw new NodeException("Did not find the binary content for file with id { " + file.getId() + " } in the database");
				}
			} catch (SQLException e) {
				throw new NodeException("Error while loading binary content for file with id { " + file.getId() + " } in the database");
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		}
	}

	/**
	 * Flushes all the outstanding operations for this FileFactory
	 * @throws NodeException if an internal error occurs
	 */
	@Override
	public void flush() throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, File.class)) {
			boolean contentFileDataInDb = getConfiguration().getDefaultPreferences().getFeature("contentfile_data_to_db");
			Collection<File> toDelete = getDeleteList(t, File.class);

			List<Integer> fileIds = new ArrayList<Integer>();

			// log command und trigger event
			for (Iterator<File> it = toDelete.iterator(); it.hasNext();) {
				File file = it.next();

				fileIds.add(file.getId());
				ActionLogger.logCmd(ActionLogger.DEL, File.TYPE_FILE, file.getId(), null, "File.delete()");
				Events.trigger(file, new String[] { ObjectTransformer.getString(file.getFolder().getNode().getId(), ""),
						MeshPublisher.getMeshUuid(file), MeshPublisher.getMeshLanguage(file) }, Events.DELETE);

				// if the file is a localized copy, it was hiding other files (which are now "created")
				if (!file.isMaster()) {
					unhideFormerHiddenObjects(ContentFile.TYPE_FILE, file.getId(), file.getChannel(), file.getChannelSet());
				}
			}

			// delete the file from every list
			DBUtils.selectAndDelete("ds_obj",
					"SELECT ds_obj.id AS id FROM ds_obj, ds WHERE " + "ds_obj.templatetag_id = ds.templatetag_id AND "
					+ "ds_obj.contenttag_id = ds.contenttag_id AND " + "ds_obj.objtag_id = ds.objtag_id AND " + "( ds.o_type = " + File.TYPE_FILE + " OR ds.o_type = "
					+ ImageFile.TYPE_IMAGE + " ) AND " + "ds.is_folder != 1 AND " + "ds_obj.o_id IN",
					fileIds);
			// DBUtils.selectAndDelete("ds_obj_nodeversion", "SELECT ds_obj.id AS id FROM ds_obj_nodeversion ds_obj, ds_nodeversion ds WHERE " +
			// "ds_obj.templatetag_id = ds.templatetag_id AND " +
			// "ds_obj.contenttag_id = ds.contenttag_id AND " +
			// "ds_obj.objtag_id = ds.objtag_id AND " +
			// "( ds.o_type = " + File.TYPE_FILE + " OR ds.o_type = " + ImageFile.TYPE_IMAGE + " ) AND " +
			// "ds.is_folder != 1 AND " +
			// "ds_obj.o_id IN", fileIds);

			// Delete from dependency map
			flushDelete("DELETE FROM dependencymap WHERE mod_type = " + File.TYPE_FILE + " AND mod_id IN", File.class);
			flushDelete("DELETE FROM dependencymap WHERE dep_type = " + File.TYPE_FILE + " AND dep_id IN", File.class);

			// Delete the imageresize cache
			final List<String> fileNames = new Vector<String>();

			for (Iterator<File> it = toDelete.iterator(); it.hasNext();) {
				File file = it.next();

				if (contentFileDataInDb) {
					fileNames.add("dbfile_" + file.getId());
				} else {
					fileNames.add(dbFilesDir.getPath() + java.io.File.separator + file.getId() + ".bin");
				}
			}

			// delete the content from the database if the feature is activated
			if (contentFileDataInDb) {
				flushDelete("DELETE FROM contentfiledata WHERE contentfile_id IN", File.class);
			}

			// Update references to this file
			String fileIdSql = buildIdSql(fileIds);
			final List<Integer> urlPartIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM part WHERE type_id IN (6, 8)", new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						urlPartIds.add(new Integer(rs.getInt("id")));
					}
				}
			});

			if (!urlPartIds.isEmpty()) {
				final Vector<Object> valueIds = new Vector<Object>();

				// Dirt the cache of the values
				DBUtils.executeMassStatement("SELECT id FROM value WHERE value_ref IN " + fileIdSql + " AND part_id IN", urlPartIds, 1, new SQLExecutor() {
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Integer id = rs.getInt("id");

							t.dirtObjectCache(Value.class, id);
							valueIds.add(id);
						}
					}
				});
				// Update the values
				if (!valueIds.isEmpty()) {
					DBUtils.executeMassStatement("UPDATE value SET value_ref = 0 WHERE id IN", valueIds, 1, null);
				}
			}

			// delete the contentfile
			flushDelete("DELETE FROM contentfile WHERE id IN", File.class);

			// delete the file from hdd
			if (!contentFileDataInDb) {
				FileManager fileManager = new FileManager();

				t.addTransactional(fileManager);

				for (Iterator<File> it = toDelete.iterator(); it.hasNext();) {
					File file = it.next();
					java.io.File delFile = new java.io.File(dbFilesDir, file.getId() + ".bin");

					fileManager.deleteFile(delFile);
				}
			}
		}
	}

	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (File.class.equals(clazz)) {
			return (T) new EditableFactoryFile(handle.createObjectInfo(File.class, true));
		} else if (ImageFile.class.equals(clazz)) {
			return (T) new EditableFactoryFile(handle.createObjectInfo(ImageFile.class, true));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, SELECT_FILE_SQL, null, null);
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_FILE_SQL);
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {

		String name = rs.getString("name");
		String niceUrl = rs.getString("nice_url");
		String filetype = rs.getString("filetype");
		Integer folderId = new Integer(rs.getInt("folder_id"));
		int filesize = rs.getInt("filesize");
		String description = rs.getString("description");
		int sizeX = rs.getInt("sizex");
		int sizeY = rs.getInt("sizey");
		int dpiX = rs.getInt("dpix");
		int dpiY = rs.getInt("dpiy");
		// Use the image center by default
		float fpX = rs.getFloat("fpx", 0.5f);
		float fpY = rs.getFloat("fpy", 0.5f);
		String md5 = rs.getString("md5");
		int creatorId = rs.getInt("creator");
		int editorId = rs.getInt("editor");
		ContentNodeDate cDate = new ContentNodeDate(rs.getInt("cdate"));
		ContentNodeDate eDate = new ContentNodeDate(rs.getInt("edate"));
		ContentNodeDate customCDate = new ContentNodeDate(rs.getInt("custom_cdate"));
		ContentNodeDate customEDate = new ContentNodeDate(rs.getInt("custom_edate"));
		Integer channelSetId = new Integer(rs.getInt("channelset_id"));
		Integer channelId = new Integer(rs.getInt("channel_id"));
		boolean master = rs.getBoolean("is_master");
		boolean forceOnline = rs.getBoolean("force_online");
		boolean excluded = rs.getBoolean("mc_exclude");
		boolean disinheritDefault = rs.getBoolean("disinherit_default");

		// fix the class in the info, if not matching
		if (ObjectTransformer.getString(filetype, "").startsWith("image") && !ImageFile.class.isAssignableFrom(info.getObjectClass())) {
			FactoryHandle handle = info.getFactory().getFactoryHandle(ImageFile.class);

			if (info.isEditable()) {
				info = handle.createObjectInfo(ImageFile.class, true);
			} else {
				info = handle.createObjectInfo(ImageFile.class, info.getVersionTimestamp());
			}
		}

		return (T) new FactoryFile(id, info, name, niceUrl, filetype, folderId, filesize, description,
				sizeX, sizeY, dpiX, dpiY, fpX, fpY, md5, creatorId, editorId, cDate, customCDate, eDate, customEDate,
				channelSetId, channelId, master, forceOnline, excluded, disinheritDefault, rs.getInt("deleted"), rs.getInt("deletedby"), getUdate(rs), getGlobalId(rs, "contentfile"));
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy
	 * (com.gentics.lib.base.object.NodeObject,
	 * com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public NodeObject getEditableCopy(final NodeObject object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryFile) {
			EditableFactoryFile editableCopy = new EditableFactoryFile((FactoryFile) object, info, false);

			return editableCopy;
		} else {
			throw new NodeException("FileFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}
}
