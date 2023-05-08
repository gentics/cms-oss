/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Folder.java,v 1.34.4.4.2.4 2011-03-17 17:57:42 norbert Exp $
 */
package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.MiscUtils.getMatchingSystemUsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.lib.etc.StringUtils;

/**
 * This is a Folder of the object layer.
 */
@TType(Folder.TYPE_FOLDER)
public interface Folder extends ObjectTagContainer, StackResolvable, LocalizableNodeObject<Folder>, Disinheritable<Folder>, Resolvable, StageableChanneledNodeObject, NamedNodeObject, StackResolvableNodeObject {
	public static final int TYPE_FOLDER = 10002;

	public static final int TYPE_INHERITED_FOLDER = 10034;

	public final static Integer TYPE_FOLDER_INTEGER = new Integer(TYPE_FOLDER);

	/**
	 * name of the "folders" property
	 */
	public final static String FOLDERS_PROPERTY = "folders";

	/**
	 * name of the "templates" property
	 */
	public final static String TEMPLATES_PROPERTY = "templates";

	/**
	 * name of the "pages" property
	 */
	public final static String PAGES_PROPERTY = "pages";

	/**
	 * name of the "files" property
	 */
	public final static String FILES_PROPERTY = "files";

	/**
	 * name of the "images" property
	 */
	public final static String IMAGES_PROPERTY = "images";

	/**
	 * name of the "filesandimages" property
	 */
	public final static String FILESANDIMAGES_PROPERTY = "filesandimages";

	/**
	 * render keys for folders
	 */
	public static final String[] RENDER_KEYS = new String[] { "folder", "ordner", "object"};

	/**
	 * constant for the setting, whether the channelset is currently dirted
	 */
	public final static String DIRTING_CHANNELSET = "dirtingchannelset";

	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Maximum length for descriptions
	 */
	public final static int MAX_DESCRIPTION_LENGTH = 255;

	/**
	 * Maximum length for pub_dirs
	 */
	public final static int MAX_PUB_DIR_LENGTH = 255;

	/**
	 * Reduce the given list of folders to not contain any pairs where one
	 * folder is direct or indirect parent of the other.
	 * @param list list of folders to reduce
	 * @param type which folders shall be kept.
	 * @return reduced list
	 * @throws NodeException
	 */
	public static List<Folder> reduceFolders(List<Folder> list, ReductionType type) throws NodeException {
		List<Folder> reducedList = new Vector<Folder>();

		if (type == ReductionType.CHILD) {
			// add all folders to the list
			reducedList.addAll(list);

			// iterate through all folders, and remove all parents from the list
			for (Folder folder : list) {
				reducedList.removeAll(folder.getParents());
			}
		} else if (type == ReductionType.PARENT) {
			// iterate over the folders
			for (Folder folder : list) {
				// check whether the folder shall be added to the reduced list.
				// It shall not be added, if one of its parent folders is also in
				// the list

				// get the parents of the folder
				List<Folder> parents = folder.getParents();

				// keep the parents, which are in the original folder list
				parents.retainAll(list);

				// only add the folder to the reduced list, if no parents are left (where in the original list)
				if (parents.size() == 0) {
					reducedList.add(folder);
				}
			}
		}
		return reducedList;
	}

	/**
	 * get the name of the folder.
	 * @return the name of the folder.
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Folders don't have a filename.
	 * @return null
	 */
	default String getFilename() {
		return null;
	}

	/**
	 * @return fully qualified name of this folder with full path information
	 * @throws NodeException
	 */
	String getQualifiedName() throws NodeException;

	/**
	 * set folder name
	 * @param name
	 * @return old name
	 * @throws ReadOnlyException when the folder was not fetched for updating
	 */
	@FieldSetter("name")
	String setName(String name) throws ReadOnlyException;

	/**
	 * get the description of the folder.
	 * @return the description of the folder.
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * set folder description
	 * @param new description
	 * @return old description
	 * @throws ReadOnlyException when the folder was not fetched for updating
	 */
	@FieldSetter("description")
	String setDescription(String description) throws ReadOnlyException;

	/**
	 * get the parent folder of this folder. If this folder is a root folder,
	 * return null.
	 * @return the parent folder, or null if this is a root folder.
	 * @throws NodeException in case of a problem
	 */
	Folder getMother() throws NodeException;

	/**
	 * Get the master folder of this folder (if this is the root folder of a channel node). Otherwise return null.
	 * @return the master folder, or null if there is no master.
	 * @throws NodeException
	 */
	Folder getChannelMaster() throws NodeException;

	/**
	 * Set the master folder for this folder, to make this the root folder of a channel.
	 * @param folder master folder (which must be the root folder of a node/channel)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setChannelMaster(Folder folder) throws ReadOnlyException, NodeException;

	/**
	 * set the folder's parent folder
	 * @param folderId to be set as parent
	 * @return old folder id
	 * @throws ReadOnlyException when the folder was not fetched for updating
	 */
	Integer setMotherId(Integer folderId) throws NodeException, ReadOnlyException;

	/**
	 * get the node to which this folder belongs to.
	 * @return the node of this folder.
	 * @throws NodeException in case of a problem
	 */
	Node getNode() throws NodeException;

	/**
	 * Set the node id for new root folders
	 * @param id
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setNodeId(Integer id) throws ReadOnlyException, NodeException;

	/**
	 * Get the path of this folder, relative to the {@link Node#getPublishDir()}
	 * .
	 * @return the publishpath relative to the node's publishdir.
	 */
	@FieldGetter("pub_dir")
	String getPublishDir();

	/**
	 * set publish dir
	 * @param pubDir
	 * @return old publish dir
	 * @throws ReadOnlyException when the folder was not fetched for updating
	 */
	@FieldSetter("pub_dir")
	String setPublishDir(String pubDir) throws ReadOnlyException, NodeException;

	/**
	 * Get the path of this folder, relative to the {@link Node#getPublishDir()}.
	 * When the feature {@link Feature#PUB_DIR_SEGMENT} is not activated, this method will return the same as {@link #getPublishDir()}.
	 * When the feature is activated, the path will consist of the values returned by {@link #getPublishDir()} of this folder, and all its mother folders.
	 *
	 * @return the publishpath relative to the node's publishdir.
	 * @throws NodeException
	 */
	String getPublishPath() throws NodeException;

	/**
	 * Get a list of all direct subfolders of this folder.
	 * @return a list of all subfolders in this folder.
	 * @throws NodeException in case of a problem
	 */
	List<Folder> getChildFolders() throws NodeException;

	/**
	 * Get the number of child folders in this folder
	 * @return number of child folders
	 * @throws NodeException
	 */
	int getChildFoldersCount() throws NodeException;

	/**
	 * Return a list of templates within this folder that satisfy the given search String
	 * @param search for searching templates (may be null)
	 * @return a list of Template objects.
	 * @throws NodeException
	 */
	List<Template> getTemplates(TemplateSearch search) throws NodeException;

	/**
	 * Return a list of all templates within this folder
	 * @return a list of Template objects
	 * @throws NodeException
	 */
	default List<Template> getTemplates() throws NodeException {
		return getTemplates(null);
	}

	/**
	 * Get the number of templates in this folder
	 * @return number of templates
	 * @throws NodeException
	 */
	int getTemplatesCount() throws NodeException;

	/**
	 * Set the list of templates linked to this folder
	 * @param templates list of templates
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setTemplates(List<Template> templates) throws ReadOnlyException, NodeException;

	/**
	 * Returns a list of all pages within this folder that satisfy the given search
	 * @param search for searching pages (may be null)
	 * @return a list of Page objects.
	 * @throws NodeException
	 */
	List<Page> getPages(PageSearch search) throws NodeException;

	/**
	 * Returns a list of all pages within this folder
	 * @return a list of Page objects
	 * @throws NodeException
	 */
	public default List<Page> getPages() throws NodeException {
		return getPages(null);
	}

	/**
	 * Get the number of pages in this folder
	 * @return number of pages
	 * @throws NodeException
	 */
	int getPagesCount() throws NodeException;

	/**
	 * Returns a list of all files within this folder that satisfy the given search
	 * @param search for searching files (may be null)
	 * @return a list of ContentFile objects.
	 * @throws NodeException
	 */
	List<File> getFiles(FileSearch search) throws NodeException;

	/**
	 * Return a list of all files within this folder
	 * @return a list of all files
	 * @throws NodeException
	 */
	public default List<File> getFiles() throws NodeException {
		return getFiles(null);
	}

	/**
	 * Get the number of files in this folder
	 * @return number of files in this folder
	 * @throws NodeException
	 */
	int getFilesCount() throws NodeException;

	/**
	 * Returns a list of all images within this folder that satisfy the given search.
	 * @param search for searching images (may be null)
	 * @return a list of ImageFiles objects.
	 * @throws NodeException
	 */
	List<ImageFile> getImages(FileSearch search) throws NodeException;

	/**
	 * Returns a list of all images within this folder.
	 * @return a list of ImageFiles objects
	 * @throws NodeException
	 */
	public default List<ImageFile> getImages() throws NodeException {
		return getImages(null);
	}

	/**
	 * Get the number of images in this folder
	 * @return number of images in this folder
	 * @throws NodeException
	 */
	int getImagesCount() throws NodeException;

	/**
	 * Returns a list of all file and image objects within this folder that satisfy the given search.
	 * @param search Search string (may be null)
	 * @return a list of ContentFile objects.
	 * @throws NodeException
	 */
	List<File> getFilesAndImages(FileSearch search) throws NodeException;

	/**
	 * Returns a list of all file and image objects within this folder.
	 * @return a list of ContentFile objects
	 * @throws NodeException
	 */
	public default List<File> getFilesAndImages() throws NodeException {
		return getFilesAndImages(null);
	}

	/**
	 * Returns a list of all forms within this folder, that satisfies the given search
	 * @param search for searching forms (may be null)
	 * @return list of Forms objects
	 * @throws NodeException
	 */
	List<Form> getForms(FormSearch search) throws NodeException;

	/**
	 * return the creation date of the folder as a unix timestamp
	 * @return creation date as a unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * return the edit date of the folder as a unix timestamp
	 * @return edit date as a unix timestamp
	 */
	ContentNodeDate getEDate();

	/**
	 * retrieve folder creator
	 * @return creator of the folder
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * retrieve folder editor
	 * @return last editor of the folder
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * Unlinks a template from this folder.
	 * If this was the last reference to the Template it is also removed.
	 * @param template id of the template to unlink
	 * @throws InsufficientPrivilegesException If the transaction doesn't have the right to unlink templates from this folder
	 */
	void unlinkTemplate(Integer templateId) throws InsufficientPrivilegesException, NodeException;

	/**
	 * retrieve the folder's startpage or null if there is none
	 * @return folder start page or null if not defined
	 */
	Page getStartpage();

	/**
	 * Dirt the folder
	 * @param channelId channel id
	 * @throws NodeException
	 */
	public default void dirtFolder(int channelId) throws NodeException {
		PublishQueue.dirtObject(this, Action.DEPENDENCY, channelId);
		// TODO log
		ActionLogger.logCmd(ActionLogger.MODIFY, TransactionManager.getCurrentTransaction().getTType(Folder.class), getId(), new Integer(0),
				"folder dirted by JP");
	}

	/**
	 * Get the name of the relationship property of this folder to the given
	 * child element
	 * @param child child element (should be a Page, Folder or ContentFile)
	 * @return name of the relationship property or null if the child is none of
	 *         (Page, Folder, ContentFile)
	 * @throws NodeException
	 */
	public default String[] getRelationshipProperty(NodeObject child) throws NodeException {
		if (child instanceof Page) {
			return new String[] { PAGES_PROPERTY};
		} else if (child instanceof Folder) {
			return new String[] { FOLDERS_PROPERTY};
		} else if (child instanceof ContentFile) {
			// we do not distinguish between files and images here
			return new String[] { FILES_PROPERTY, IMAGES_PROPERTY};
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getObject()
	 */
	public default Folder getObject() {
		return this;
	}

	/**
	 * Get true if the folder has a channel set, false if not
	 * @return true if the folder is a channel object, false if not
	 * @throws NodeException
	 */
	boolean hasChannel() throws NodeException;

	/**
	 * Set the channel information for the folder. The channel information consist
	 * of the channelId and the channelSetId. The channelId identifies the
	 * channel, for which the object is valid. If set to 0 (which is the
	 * default), the object is not a localized copy and no local object in a
	 * channel, but is a normal object in a node. The channelSetId groups the
	 * master object and all its localized copies in channel together. This
	 * method may only be called for new files.
	 *
	 * @param channel
	 *            id new channel id. If set to 0, this object will be a master
	 *            object in a node and the channelSetId must be given as null
	 *            (which will create a new channelSetId)
	 * @param channelSetId
	 *            id of the channelset. If set to null, a new channelSetId will
	 *            be generated and the object will be a master (in a node or in
	 *            a channel)
	 * @throws ReadOnlyException when the object is not editable
	 * @throws NodeException in case of other errors
	 */
	public default void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {
		setChannelInfo(channelId, channelSetId, false);
	}

	/**
	 * Get true, if this folder is the root folder of a channel, false if not
	 * @return true for root folders of channels, false for other folders
	 * @throws NodeException
	 */
	boolean isChannelRoot() throws NodeException;

	/**
	 * Check whether the folder is a root folder (has no mother)
	 * @return true for root folders, false for non-root folders
	 * @throws NodeException
	 */
	boolean isRoot() throws NodeException;

	/**
	 * Get the master folder, if this folder is a localized copy. If this
	 * folder is not a localized copy or multichannelling is not activated,
	 * returns this folder
	 *
	 * @return master folder for localized copies or this folder
	 * @throws NodeException
	 */
	public default Folder getMaster() throws NodeException {
		return MultichannellingFactory.getMaster(this);
	}

	/**
	 * Gets master Folder Name
	 * @return
	 * @throws NodeException
	 */
	public default String getMasterNodeFolderName() throws NodeException {
		return getChannelMasterNode().getFolder().getName();
	}

	/**
	 * Gets the channel master Node or the master.
	 * @return channel master node or the master in case is not a channel.
	 * @throws NodeException
	 */
	public default Node getChannelMasterNode() throws NodeException {
		Node masterNode = getMaster().getChannel();

		if (masterNode != null) {
			return masterNode;
		}

		Node node = getNode();
		List<Node> masterNodes = node.getMasterNodes();

		if (masterNodes.size() > 0) {
			return masterNodes.get(masterNodes.size() - 1);
		} else {
			return node;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getNextHigherObject()
	 */
	public default Folder getNextHigherObject() throws NodeException {
		return MultichannellingFactory.getNextHigherObject(this);
	}

	/**
	 * Push this folder into the given master
	 * @param master master node to push this folder to
	 * @return target folder
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public default Folder pushToMaster(Node master) throws ReadOnlyException, NodeException {
		if (!isChannelRoot()) {
			return MultichannellingFactory.pushToMaster(this, master).getObject();
		} else {
			return this;
		}
	}

	/**
	 * Get all parent folders of this folder
	 * @return list of parent folders
	 * @throws NodeException
	 */
	public default List<Folder> getParents() throws NodeException {
		List<Folder> parents = new Vector<Folder>();
		Folder parent = getMother();

		while (parent != null) {
			parents.add(parent);
			parent = parent.getMother();
		}

		return parents;

	}

	/**
	 * Add the dependency on the resolved object (when it was resolved)
	 * @param property resolved property
	 * @param resolvedObject object (value of the resolved property)
	 */
	void addDependency(String property, Object resolvedObject);

	/**
	 * Move this folder into the target folder in the target channel.
	 * The implementation will do all necessary checks, including permission checks.
	 * @param target target folder, must not be null
	 * @param targetChannelId target channel id. 0 to move the folder into the master node of the given folder, > 0 to move the folder into a channel.
	 * @return operation result
	 * @throws ReadOnlyException if this folder instance is not editable
	 * @throws NodeException if the move operation cannot be performed due to other reasons
	 */
	OpResult move(Folder target, int targetChannelId) throws ReadOnlyException, NodeException;

	@Override
	default boolean isContainer() {
		return true;
	}

	@Override
	default String getFullPublishPath(boolean trailingSlash, boolean includeNodePublishDir) throws NodeException {
		return getFullPublishPath(null, trailingSlash, includeNodePublishDir);
	}

	@Override
	default String getFullPublishPath(Folder folder, boolean trailingSlash, PageLanguageCode pageLanguageCode,
			boolean includeNodePublishDir) throws NodeException {
		List<String> segments = new ArrayList<>();

		if (includeNodePublishDir) {
			Node node = getNode();
			ContentRepository cr = node.getContentRepository();

			if (cr == null || !cr.ignoreNodePublishDir()) {
				segments.add(node.getPublishDir());
			}
		}

		segments.add(getPublishPath());

		return FilePublisher.getPath(
			true,
			trailingSlash,
			segments.toArray(new String[0]));
	}

	/**
	 * Get the translated folder names
	 * @return i18n map
	 */
	I18nMap getNameI18n();

	/**
	 * Set the translated folder names
	 * @param nameI18n i18n map
	 * @throws ReadOnlyException
	 */
	default void setNameI18n(I18nMap nameI18n) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the translated folder descriptions
	 * @return i18n map
	 */
	I18nMap getDescriptionI18n();

	/**
	 * Set the translated folder descriptions
	 * @param descriptionI18n i18n map
	 * @throws ReadOnlyException
	 */
	default void setDescriptionI18n(I18nMap descriptionI18n) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the translated folder publish directories
	 * @return i18n map
	 */
	I18nMap getPublishDirI18n();

	/**
	 * Set the translated folder publish directories
	 * @param publishDirI18n i18n map
	 * @throws NodeException
	 */
	default void setPublishDirI18n(I18nMap publishDirI18n) throws NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Enum for the reduction type used in method {@link Folder#reduceUserGroups(List, com.gentics.contentnode.object.Folder.ReductionType)}
	 */
	public static enum ReductionType {
		CHILD, PARENT
	}

	@Override
	default String getSuffix() {
		return ".folder";
	}

	@Override
	default Folder getFolder() throws NodeException {
		return this;
	}

	@Override
	default Optional<Node> maybeGetChannel() throws NodeException {
		return Optional.ofNullable(getChannel());
	}

	@Override
	default void setFolder(Node node, Folder parent) throws NodeException {
		if (isNew()) {
			setMotherId(parent.getId());
		} else {
			OpResult moveResult = move(parent, Optional.ofNullable(node).map(Node::getId).orElse(0));
			if (!moveResult.isOK()) {
				String message = moveResult.getMessages().stream().map(NodeMessage::getMessage).collect(Collectors.joining(", "));
				throw new NodeException(message);
			}
		}
	}

	/**
	 * PageSearch class. Instances are passed to the method {@link Folder#getPages(com.gentics.contentnode.object.Folder.PageSearch)} to refine a page search.
	 */
	public static class PageSearch {

		/**
		 * Search string (may be null).
		 * If set, this is matched against id, name and description
		 */
		protected String searchString;

		/**
		 * When set to true, also the content is searched
		 */
		protected boolean searchContent = false;

		/**
		 * Pattern to match filenames (may be null)
		 */
		protected String fileNameSearch;

		/**
		 * Regex for nice URL search
		 */
		protected String niceUrlSearch;

		/**
		 * When set true, only pages that are assigned to the current user in a publish workflow are returned
		 */
		protected boolean workflowOwn = false;

		/**
		 * When set true, only pages that are watched by the current user,
		 * because they have been set to a workflow step are returned
		 */
		protected boolean workflowWatch = false;

		/**
		 * When set true, only pages last edited by the current user are returned
		 */
		protected boolean editor = false;

		/**
		 * When set true, only pages created by the current user are returned
		 */
		protected boolean creator = false;

		/**
		 * When set true, only pages last published by the current user are returned
		 */
		protected boolean publisher = false;

		/**
		 * When set, only pages last edited by one of the users are returned
		 */
		protected List<SystemUser> editors = null;

		/**
		 * When set, only pages created by one of the users are returned
		 */
		protected List<SystemUser> creators = null;

		/**
		 * When set, only pages published by one of the users are returned
		 */
		protected List<SystemUser> publishers = null;

		/**
		 * When set, only pages to which all of the permissions are granted will be returned
		 */
		protected List<Permission> permissions = Collections.emptyList();

		/**
		 * When set > 0, only pages with the given priority are returned
		 */
		protected int priority = 0;

		/**
		 * When set, only pages with one of the given templates are returned
		 */
		protected List<Integer> templateIds = Collections.emptyList();

		/**
		 * When set > 0, only pages that were edited before the given timestamp are returned
		 */
		protected int editedBefore = 0;

		/**
		 * When set > 0, only pages that were edited since the given timestamp are returned
		 */
		protected int editedSince = 0;

		/**
		 * When set > 0, only pages, that were created before the given timestamp are returned
		 */
		protected int createdBefore = 0;

		/**
		 * When set > 0, only pages, that were created since the given timestamp are returned
		 */
		protected int createdSince = 0;

		/**
		 * When set > 0, only pages, that were published before the given timestamp are returned
		 */
		protected int publishedBefore = 0;

		/**
		 * When set > 0, only pages, that were published since the given timestamp are returned
		 */
		protected int publishedSince = 0;

		/**
		 * When true, the search is done recursively
		 */
		protected boolean recursive = false;

		/**
		 * When true, only get pages that are online
		 */
		protected Boolean online = null;

		/**
		 * Flag for searching modified pages
		 */
		protected Boolean modified = null;

		/**
		 * Flag for searching queued pages
		 */
		protected Boolean queued = null;

		/**
		 * Flag for searching planned pages
		 */
		protected Boolean planned = null;

		/**
		 * When true, only get inherited pages, when false only get local pages
		 */
		protected Boolean inherited = null;

		/**
		 * When true, only pages in the wastebin will be fetched, when false, there is no restriction
		 */
		protected boolean wastebin = false;

		/**
		 * When set, only pages having templates with the given mlIds are returned
		 */
		protected List<Integer> includeMlIds = Collections.emptyList();

		/**
		 * When set, only pages having templates with other than the given mlIds are returned
		 */
		protected List<Integer> excludeMlIds = Collections.emptyList();

		/**
		 * Static method to create an empty page search
		 * @return empty page search
		 */
		public static PageSearch create() {
			return new PageSearch();
		}

		/**
		 * Create an empty page search (will find all pages)
		 */
		public PageSearch() {}

		/**
		 * Set the search string
		 * @param searchString new search string
		 * @return this object (for chaining)
		 */
		public PageSearch setSearchString(String searchString) {
			this.searchString = searchString;
			return this;
		}

		/**
		 * Set to true, if the content shall be searched, false if not
		 * @param searchContent true for searching the content
		 * @return this object (for chaining)
		 */
		public PageSearch setSearchContent(boolean searchContent) {
			this.searchContent = searchContent;
			return this;
		}

		/**
		 * Set the filename search pattern
		 * @param fileNameSearch filename search pattern
		 * @return this object (for chaining)
		 */
		public PageSearch setFileNameSearch(String fileNameSearch) {
			this.fileNameSearch = fileNameSearch;
			return this;
		}

		/**
		 * Set the nice URL search regex
		 * @param niceUrlSearch nice URL search regex
		 * @return this object (for chaining)
		 */
		public PageSearch setNiceUrlSearch(String niceUrlSearch) {
			this.niceUrlSearch = niceUrlSearch;
			return this;
		}

		/**
		 * Set whether pages owned in a workflow step shall be returned
		 * @param workflowOwn true for restricting pages, false for not
		 * @return this object (for chaining)
		 */
		public PageSearch setWorkflowOwn(boolean workflowOwn) {
			this.workflowOwn = workflowOwn;
			return this;
		}

		/**
		 * Set whether pages watched by the user, because they were set to a workflow step by the user shall be returned
		 * @param workflowWatch true for restricting pages, false for not
		 * @return this object (for chaining)
		 */
		public PageSearch setWorkflowWatch(boolean workflowWatch) {
			this.workflowWatch = workflowWatch;
			return this;
		}

		/**
		 * Set whether only pages last edited by the user shall be returned
		 * @param editor true for restricting pages
		 * @return this object (for chaining)
		 */
		public PageSearch setEditor(boolean editor) {
			this.editor = editor;
			return this;
		}

		/**
		 * Set whether only pages created by the user shall be returned
		 * @param creator true for restricting pages
		 * @return this object (for chaining)
		 */
		public PageSearch setCreator(boolean creator) {
			this.creator = creator;
			return this;
		}

		/**
		 * Set whether only pages last published by the user shall be returned
		 * @param creator true for restricting pages
		 * @return this object (for chaining)
		 */
		public PageSearch setPublisher(boolean publisher) {
			this.publisher = publisher;
			return this;
		}

		/**
		 * Set the editors for restricting pages
		 * @param editors list of editors
		 * @return this object (for chaining)
		 */
		public PageSearch setEditors(List<SystemUser> editors) {
			if (editors == null) {
				this.editors = null;
			} else {
				this.editors = Collections.unmodifiableList(editors);
			}
			return this;
		}

		/**
		 * Set the creators for restricting pages
		 * @param creators list of creators
		 * @return this object (for chaining)
		 */
		public PageSearch setCreators(List<SystemUser> creators) {
			if (creators == null) {
				this.creators = null;
			} else {
				this.creators = Collections.unmodifiableList(creators);
			}
			return this;
		}

		/**
		 * Set the publishers for restricting pages
		 * @param publishers list of publishers
		 * @return this object (for chaining)
		 */
		public PageSearch setPublishers(List<SystemUser> publishers) {
			if (publishers == null) {
				this.publishers = null;
			} else {
				this.publishers = Collections.unmodifiableList(publishers);
			}
			return this;
		}

		/**
		 * Set the page permissions for restricting pages.
		 * @param permissions
		 * @return
		 */
		public PageSearch setPermissions(List<Permission> permissions) {
			this.permissions = Collections.unmodifiableList(permissions);
			return this;
		}

		/**
		 * Set the template ids for restricting pages
		 * @param templateIds list of restricted template ids
		 * @return this object (for chaining)
		 */
		public PageSearch setTemplateIds(List<Integer> templateIds) {
			this.templateIds = Collections.unmodifiableList(templateIds);
			return this;
		}

		/**
		 * Set the page priority for restricting pages
		 * @param priority priority (0 for not restricting priority)
		 * @return this object (for chaining)
		 */
		public PageSearch setPriority(int priority) {
			this.priority = priority;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were edited before a given timestamp
		 * @param editedBefore timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setEditedBefore(int editedBefore) {
			this.editedBefore = editedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were edited since a given timestamp
		 * @param editedSince timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setEditedSince(int editedSince) {
			this.editedSince = editedSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were created before a given timestamp
		 * @param createdBefore timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setCreatedBefore(int createdBefore) {
			this.createdBefore = createdBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were created since a given timestamp
		 * @param createdSince timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setCreatedSince(int createdSince) {
			this.createdSince = createdSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were published before a given timestamp
		 * @param publishedBefore timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setPublishedBefore(int publishedBefore) {
			this.publishedBefore = publishedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to pages which were published since a given timestamp
		 * @param publishedSince timestamp
		 * @return this object (for chaining)
		 */
		public PageSearch setPublishedSince(int publishedSince) {
			this.publishedSince = publishedSince;
			return this;
		}

		/**
		 * Set whether the search shall be done recursively
		 * @param recursive true for recursive search, false for search only in one folder
		 * @return this object (for chaining)
		 */
		public PageSearch setRecursive(boolean recursive) {
			this.recursive = recursive;
			return this;
		}

		/**
		 * Set whether the search shall be restricted to online pages
		 * @param online true if restricting to online pages, false for searching offline, null for no restriction
		 * @return this object (for chaining)
		 */
		public PageSearch setOnline(Boolean online) {
			this.online = online;
			return this;
		}

		/**
		 * Set whether searching for modified/unmodified pages
		 * @param modified modified flag
		 * @return fluent API
		 */
		public PageSearch setModified(Boolean modified) {
			this.modified = modified;
			return this;
		}

		/**
		 * Set whether searching for planned/unplanned pages
		 * @param planned planned flag
		 * @return fluent API
		 */
		public PageSearch setPlanned(Boolean planned) {
			this.planned = planned;
			return this;
		}

		/**
		 * Set whether searching for queued/unqueued pages
		 * @param queued queued flag
		 * @return fluent API
		 */
		public PageSearch setQueued(Boolean queued) {
			this.queued = queued;
			return this;
		}

		/**
		 * Set whether local, inherited or all pages shall be returned
		 * @param inherited
		 *            true for getting only inherited pages, false for getting only
		 *            local pages, null for getting all pages
		 * @return this object (for chaining)
		 */
		public PageSearch setInherited(Boolean inherited) {
			this.inherited = inherited;
			return this;
		}

		/**
		 * Set true tp restrict search to objects in the wastebin
		 * @param wastebin true for wastebin only
		 * @return this object (for chaining)
		 */
		public PageSearch setWastebin(boolean wastebin) {
			this.wastebin = wastebin;
			return this;
		}

		/**
		 * Set markupLanguage IDs for restricting pages
		 * @param includeMlIds included mlIds
		 * @return fluent API
		 */
		public PageSearch setIncludeMlIds(List<Integer> includeMlIds) {
			this.includeMlIds = includeMlIds;
			return this;
		}

		/**
		 * Set markupLanguage IDs for restricting pages
		 * @param excludeMlIds excluded mlIds
		 * @return fluent API
		 */
		public PageSearch setExcludeMlIds(List<Integer> excludeMlIds) {
			this.excludeMlIds = excludeMlIds;
			return this;
		}

		/**
		 * Check whether the search is empty (meaning: does not restrict the pages)
		 * @return true when the search is empty, false if not
		 */
		public boolean isEmpty() {
			return !workflowOwn && !workflowWatch && StringUtils.isEmpty(searchString) && StringUtils.isEmpty(fileNameSearch) && !editor && !creator
					&& !publisher && editors == null && creators == null && publishers == null && priority == 0 && ObjectTransformer.isEmpty(templateIds)
					&& editedBefore == 0 && editedSince == 0 && createdBefore == 0 && createdSince == 0 && publishedBefore == 0 && publishedSince == 0
					&& !recursive && online == null && modified == null && queued == null && planned == null && inherited == null && !wastebin
					&& StringUtils.isEmpty(niceUrlSearch) && ObjectTransformer.isEmpty(includeMlIds) && ObjectTransformer.isEmpty(excludeMlIds);
		}

		/**
		 * Get the search string
		 * @return search string
		 */
		public String getSearchString() {
			return searchString;
		}

		/**
		 * Check whether the content shall be searched
		 * @return true if the content shall be searched, false if not
		 */
		public boolean isSearchContent() {
			return searchContent;
		}

		/**
		 * Get the fileName search pattern
		 * @return fileName search pattern
		 */
		public String getFileNameSearch() {
			return fileNameSearch;
		}

		/**
		 * Get the nice URL search regex
		 * @return nice URL search regex
		 */
		public String getNiceUrlSearch() {
			return niceUrlSearch;
		}

		/**
		 * Get true when the search shall restrict to pages in current workflow steps
		 * @return true or false
		 */
		public boolean isWorkflowOwn() {
			return workflowOwn;
		}

		/**
		 * Get true when the search shall restrict to pages watched by the user
		 * @return true or false
		 */
		public boolean isWorkflowWatch() {
			return workflowWatch;
		}

		/**
		 * Get true when the search shall restrict to pages last edited by the user
		 * @return true or false
		 */
		public boolean isEditor() {
			return editor;
		}

		/**
		 * Get true when the search shall restrict to pages created by the user
		 * @return true or false
		 */
		public boolean isCreator() {
			return creator;
		}

		/**
		 * Get true when the search shall restrict to pages last published by the user
		 * @return true or false
		 */
		public boolean isPublisher() {
			return publisher;
		}

		/**
		 * Get the list of editors
		 * @return list of editors
		 */
		public List<SystemUser> getEditors() {
			return editors;
		}

		/**
		 * Get the list of creators
		 * @return list of creators
		 */
		public List<SystemUser> getCreators() {
			return creators;
		}

		/**
		 * Get the list of publishers
		 * @return list of publishers
		 */
		public List<SystemUser> getPublishers() {
			return publishers;
		}

		/**
		 * Get the list of restricting page permissions or an empty list, if not restricting pages by permission.
		 *
		 * @return List of permissions
		 */
		public List<Permission> getPermissions() {
			return permissions;
		}

		/**
		 * Get the list of restricted template ids or an empty list, if not restricting by template ids
		 * @return list of restricted template ids or empty list
		 */
		public List<Integer> getTemplateIds() {
			return templateIds;
		}

		/**
		 * Get the priority for restricting pages (0 for not restricting by priority)
		 * @return priority
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Get the timestamp for restricting to pages which were edited before a given timestamp
		 * @return timestamp
		 */
		public int getEditedBefore() {
			return editedBefore;
		}

		/**
		 * Get the timestamp for restricting to pages which were edited since a given timestamp
		 * @return timestamp
		 */
		public int getEditedSince() {
			return editedSince;
		}

		/**
		 * Get the timestamp for restricting to pages which were created before a given timestamp
		 * @return timestamp
		 */
		public int getCreatedBefore() {
			return createdBefore;
		}

		/**
		 * Get the timestamp for restricting to pages which were created since a given timestamp
		 * @return timestamp
		 */
		public int getCreatedSince() {
			return createdSince;
		}

		/**
		 * Get the timestamp for restricting to pages which were published before a given timestamp
		 * @return timestamp
		 */
		public int getPublishedBefore() {
			return publishedBefore;
		}

		/**
		 * Get the timestamp for restricting to pages which were published since a given timestamp
		 * @return timestamp
		 */
		public int getPublishedSince() {
			return publishedSince;
		}

		/**
		 * Get true, if the search is recursive, false if not
		 * @return true for recursive search, false for normal search
		 */
		public boolean isRecursive() {
			return recursive;
		}

		/**
		 * Get true, if the search shall restrict to online pages, false for offline pages, null for no restriction
		 * @return true for restricting to online pages
		 */
		public Boolean getOnline() {
			return online;
		}

		/**
		 * True for searching for modified pages, false for unmodified, null for no restriction
		 * @return true, false or null
		 */
		public Boolean getModified() {
			return modified;
		}

		/**
		 * True for searching for planned pages, false for unplanned, null for no restriction
		 * @return true, false or null
		 */
		public Boolean getPlanned() {
			return planned;
		}

		/**
		 * True for searching for queued pages, false for unqueued, null for no restriction
		 * @return true, false or null
		 */
		public Boolean getQueued() {
			return queued;
		}

		/**
		 * Get true, of the search shall restrict to inherited pages, false for restricting to local pages, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getInherited() {
			return inherited;
		}

		/**
		 * True, if the search shall be restricted to object in the wastebin
		 * @return true for restricting to wastebin
		 */
		public boolean isWastebin() {
			return wastebin;
		}

		/**
		 * Get the list of included mlIds or empty list if not restricting pages
		 * @return list of included mlIds
		 */
		public List<Integer> getIncludeMlIds() {
			return includeMlIds;
		}

		/**
		 * Get the list of excluded mlIds or empty list if not restricting pages
		 * @return list of excluded mlIds
		 */
		public List<Integer> getExcludeMlIds() {
			return excludeMlIds;
		}
	}

	/**
	 * FileSearch class. Instances are passed to the methods
	 * {@link Folder#getImages(FileSearch)} and
	 * {@link Folder#getFiles(FileSearch)} to refine a image/file search.
	 */
	public static class FileSearch {

		/**
		 * Search string (may be null).
		 * If set, this is matched against id, name and description
		 */
		protected String searchString;

		/**
		 * Regex for nice URL search
		 */
		protected String niceUrlSearch;

		/**
		 * When set, only images/files last edited by one of the users are returned
		 */
		protected List<SystemUser> editors = null;

		/**
		 * When set, only images/files created by one of the users are returned
		 */
		protected List<SystemUser> creators = null;

		/**
		 * When set > 0, only images/files that were edited before the given timestamp are returned
		 */
		protected int editedBefore = 0;

		/**
		 * When set > 0, only images/files that were edited since the given timestamp are returned
		 */
		protected int editedSince = 0;

		/**
		 * When set > 0, only images/files, that were created before the given timestamp are returned
		 */
		protected int createdBefore = 0;

		/**
		 * When set > 0, only images/files, that were created since the given timestamp are returned
		 */
		protected int createdSince = 0;

		/**
		 * When true, the search is done recursively
		 */
		protected boolean recursive = false;

		/**
		 * When true, only get inherited files, when false only get local files
		 */
		protected Boolean inherited = null;

		/**
		 * When true, only get online online files, when false get only offline files, when null get online and offline files
		 */
		protected Boolean online = null;

		/**
		 * When true, only get broken files, when false only get non-broken files, when null get all files
		 */
		protected Boolean broken = null;

		/**
		 * When true, only get used files, when false only get unused files, when null get all files
		 */
		protected Boolean used = null;

		/**
		 * When {@link #used} is not null, this list can extend the search to specific channels
		 */
		protected List<Integer> usedIn = null;

		/**
		 * When true, only pages in the wastebin will be fetched, when false, there is no restriction
		 */
		protected boolean wastebin = false;

		/**
		 * Static method to create an empty file search
		 * @return empty file search
		 */
		public static FileSearch create() {
			return new FileSearch();
		}

		/**
		 * Create an empty file search (will find all files)
		 */
		public FileSearch() {}

		/**
		 * Set the search string
		 * @param searchString new search string
		 * @return this object (for chaining)
		 */
		public FileSearch setSearchString(String searchString) {
			this.searchString = searchString;
			return this;
		}

		/**
		 * Set the nice URL search regex
		 * @param niceUrlSearch nice URL search regex
		 * @return this object (for chaining)
		 */
		public FileSearch setNiceUrlSearch(String niceUrlSearch) {
			this.niceUrlSearch = niceUrlSearch;
			return this;
		}

		/**
		 * Set the editors for restricting files
		 * @param editors list of editors
		 * @return this object (for chaining)
		 */
		public FileSearch setEditors(List<SystemUser> editors) {
			if (editors == null) {
				this.editors = null;
			} else {
				this.editors = Collections.unmodifiableList(editors);
			}
			return this;
		}

		/**
		 * Set the creators for restricting files
		 * @param creators list of creators
		 * @return this object (for chaining)
		 */
		public FileSearch setCreators(List<SystemUser> creators) {
			if (creators == null) {
				this.creators = null;
			} else {
				this.creators = Collections.unmodifiableList(creators);
			}
			return this;
		}

		/**
		 * Set the timestamp for restricting to files which were edited before a given timestamp
		 * @param editedBefore timestamp
		 * @return this object (for chaining)
		 */
		public FileSearch setEditedBefore(int editedBefore) {
			this.editedBefore = editedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to files which were edited since a given timestamp
		 * @param editedSince timestamp
		 * @return this object (for chaining)
		 */
		public FileSearch setEditedSince(int editedSince) {
			this.editedSince = editedSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to files which were created before a given timestamp
		 * @param createdBefore timestamp
		 * @return this object (for chaining)
		 */
		public FileSearch setCreatedBefore(int createdBefore) {
			this.createdBefore = createdBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to files which were created since a given timestamp
		 * @param createdSince timestamp
		 * @return this object (for chaining)
		 */
		public FileSearch setCreatedSince(int createdSince) {
			this.createdSince = createdSince;
			return this;
		}

		/**
		 * Set whether the search shall be done recursively
		 * @param recursive true for recursive search, false for search only in one folder
		 * @return this object (for chaining)
		 */
		public FileSearch setRecursive(boolean recursive) {
			this.recursive = recursive;
			return this;
		}

		/**
		 * Set whether local, inherited or all files shall be returned
		 * @param inherited
		 *            true for getting only inherited files, false for getting only
		 *            local files, null for getting all files
		 * @return this object (for chaining)
		 */
		public FileSearch setInherited(Boolean inherited) {
			this.inherited = inherited;
			return this;
		}

		/**
		 * Set whether online, offline or all files shall be returned
		 *
		 * @param online
		 *            true for getting online, false for getting offline, null for all files
		 * @return this object (for chaining)
		 */
		public FileSearch setOnline(Boolean online) {
			this.online = online;
			return this;
		}

		/**
		 * Set whether broken, non-broken or all files shall be returned
		 * @param broken true for getting broken, false for getting non-broken, null for all files
		 * @return this object (for chaining)
		 */
		public FileSearch setBroken(Boolean broken) {
			this.broken = broken;
			return this;
		}

		/**
		 * Set whether used, not used or all files shall be returned
		 * @param broken true for getting used, false for getting unused, null for all files
		 * @return this object (for chaining)
		 */
		public FileSearch setUsed(Boolean used) {
			this.used = used;
			return this;
		}

		/**
		 * Set list of channels to extend the search for used/unused files
		 * @param usedIn list of channels
		 * @return this object (for chaining)
		 */
		public FileSearch setUsedIn(List<Integer> usedIn) {
			this.usedIn = usedIn;
			return this;
		}

		/**
		 * True, if the search shall be restricted to object in the wastebin
		 * @return true for restricting to wastebin
		 */
		public FileSearch setWastebin(boolean wastebin) {
			this.wastebin = wastebin;
			return this;
		}

		/**
		 * Check whether the search is empty (meaning: does not restrict the files)
		 * @return true when the search is empty, false if not
		 */
		public boolean isEmpty() {
			return StringUtils.isEmpty(searchString) && StringUtils.isEmpty(niceUrlSearch) && editors == null && creators == null
					&& editedBefore == 0 && editedSince == 0 && createdBefore == 0 && createdSince == 0
					&& !recursive && inherited == null && online == null && broken == null && used == null && ObjectTransformer.isEmpty(usedIn) && !wastebin;
		}

		/**
		 * Get the search string
		 * @return search string
		 */
		public String getSearchString() {
			return searchString;
		}

		/**
		 * Get the nice URL search regex
		 * @return nice URL search regex
		 */
		public String getNiceUrlSearch() {
			return niceUrlSearch;
		}

		/**
		 * Get the list of editors
		 * @return list of editors
		 */
		public List<SystemUser> getEditors() {
			return editors;
		}

		/**
		 * Get the list of creators
		 * @return list of creators
		 */
		public List<SystemUser> getCreators() {
			return creators;
		}

		/**
		 * Get the timestamp for restricting to files which were edited before a given timestamp
		 * @return timestamp
		 */
		public int getEditedBefore() {
			return editedBefore;
		}

		/**
		 * Get the timestamp for restricting to files which were edited since a given timestamp
		 * @return timestamp
		 */
		public int getEditedSince() {
			return editedSince;
		}

		/**
		 * Get the timestamp for restricting to files which were created before a given timestamp
		 * @return timestamp
		 */
		public int getCreatedBefore() {
			return createdBefore;
		}

		/**
		 * Get the timestamp for restricting to files which were created since a given timestamp
		 * @return timestamp
		 */
		public int getCreatedSince() {
			return createdSince;
		}

		/**
		 * Get true, if the search is recursive, false if not
		 * @return true for recursive search, false for normal search
		 */
		public boolean isRecursive() {
			return recursive;
		}

		/**
		 * Get true, of the search shall restrict to inherited files, false for restricting to local files, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getInherited() {
			return inherited;
		}

		/**
		 * Get true to search only online files, false for only offline files, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getOnline() {
			return online;
		}

		/**
		 * Get true to search only for broken files, false for only non-broken files, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getBroken() {
			return broken;
		}

		/**
		 * Get true to search only for used files, false for only unused files, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getUsed() {
			return used;
		}

		/**
		 * Get list of channels to extend the search for used/unused files to
		 * @return list of channels or null
		 */
		public List<Integer> getUsedIn() {
			return usedIn;
		}

		/**
		 * True, if the search shall be restricted to object in the wastebin
		 * @return true for restricting to wastebin
		 */
		public boolean isWastebin() {
			return wastebin;
		}
	}

	/**
	 *
	 * @author norbert
	 *
	 */
	public static class TemplateSearch {

		/**
		 * Search string (may be null).
		 * If set, this is matched against id, name and description
		 */
		protected String searchString;

		/**
		 * When set, only templates last edited by one of the users are returned
		 */
		protected List<SystemUser> editors = null;

		/**
		 * When set, only templates created by one of the users are returned
		 */
		protected List<SystemUser> creators = null;

		/**
		 * When set > 0, only templates that were edited before the given timestamp are returned
		 */
		protected int editedBefore = 0;

		/**
		 * When set > 0, only templates that were edited since the given timestamp are returned
		 */
		protected int editedSince = 0;

		/**
		 * When set > 0, only templates that were created before the given timestamp are returned
		 */
		protected int createdBefore = 0;

		/**
		 * When set > 0, only templates that were created since the given timestamp are returned
		 */
		protected int createdSince = 0;

		/**
		 * When true, only get inherited pages, when false only get local pages
		 */
		protected Boolean inherited = null;

		/**
		 * Static method to create an empty template search
		 * @return empty template search
		 */
		public static TemplateSearch create() {
			return new TemplateSearch();
		}

		/**
		 * Create an empty template search (will find all templates)
		 */
		public TemplateSearch() {}

		/**
		 * Set the search string
		 * @param searchString new search string
		 * @return this object (for chaining)
		 */
		public TemplateSearch setSearchString(String searchString) {
			this.searchString = searchString;
			return this;
		}

		/**
		 * Set the editors for restricting templates
		 * @param editors list of editors
		 * @return this object (for chaining)
		 */
		public TemplateSearch setEditors(List<SystemUser> editors) {
			if (editors == null) {
				this.editors = null;
			} else {
				this.editors = Collections.unmodifiableList(editors);
			}
			return this;
		}

		/**
		 * Set the creators for restricting templates
		 * @param creators list of creators
		 * @return this object (for chaining)
		 */
		public TemplateSearch setCreators(List<SystemUser> creators) {
			if (creators == null) {
				this.creators = null;
			} else {
				this.creators = Collections.unmodifiableList(creators);
			}
			return this;
		}

		/**
		 * Set the timestamp for restricting to templates which were edited before a given timestamp
		 * @param editedBefore timestamp
		 * @return this object (for chaining)
		 */
		public TemplateSearch setEditedBefore(int editedBefore) {
			this.editedBefore = editedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to templates which were edited since a given timestamp
		 * @param editedSince timestamp
		 * @return this object (for chaining)
		 */
		public TemplateSearch setEditedSince(int editedSince) {
			this.editedSince = editedSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to templates which were created before a given timestamp
		 * @param createdBefore timestamp
		 * @return this object (for chaining)
		 */
		public TemplateSearch setCreatedBefore(int createdBefore) {
			this.createdBefore = createdBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to templates which were created since a given timestamp
		 * @param createdSince timestamp
		 * @return this object (for chaining)
		 */
		public TemplateSearch setCreatedSince(int createdSince) {
			this.createdSince = createdSince;
			return this;
		}

		/**
		 * Set whether local, inherited or all templates shall be returned
		 * @param inherited
		 *            true for getting only inherited templates, false for getting only
		 *            local templates, null for getting all templates
		 * @return this object (for chaining)
		 */
		public TemplateSearch setInherited(Boolean inherited) {
			this.inherited = inherited;
			return this;
		}

		/**
		 * Check whether the search is empty (meaning: does not restrict the templates)
		 * @return true when the search is empty, false if not
		 */
		public boolean isEmpty() {
			return StringUtils.isEmpty(searchString) && editors == null && creators == null
				&& editedBefore == 0 && editedSince == 0 && createdBefore == 0 && createdSince == 0
				&& inherited == null;
		}

		/**
		 * Get the search string
		 * @return search string
		 */
		public String getSearchString() {
			return searchString;
		}

		/**
		 * Get the list of editors
		 * @return list of editors
		 */
		public List<SystemUser> getEditors() {
			return editors;
		}

		/**
		 * Get the list of creators
		 * @return list of creators
		 */
		public List<SystemUser> getCreators() {
			return creators;
		}

		/**
		 * Get the timestamp for restricting to templates which were edited before a given timestamp
		 * @return timestamp
		 */
		public int getEditedBefore() {
			return editedBefore;
		}

		/**
		 * Get the timestamp for restricting to templates which were edited since a given timestamp
		 * @return timestamp
		 */
		public int getEditedSince() {
			return editedSince;
		}

		/**
		 * Get the timestamp for restricting to templates which were created before a given timestamp
		 * @return timestamp
		 */
		public int getCreatedBefore() {
			return createdBefore;
		}

		/**
		 * Get the timestamp for restricting to templates which were created since a given timestamp
		 * @return timestamp
		 */
		public int getCreatedSince() {
			return createdSince;
		}

		/**
		 * Get true, of the search shall restrict to inherited templates, false for restricting to local templates, null for not restricting
		 * @return true, false or null
		 */
		public Boolean getInherited() {
			return inherited;
		}
	}

	/**
	 * FormSearch class. Instances are passed to the method {@link Folder#getForms(com.gentics.contentnode.object.Folder.FormSearch)} to refine a form search.
	 */
	public static class FormSearch {
		/**
		 * Search string (may be null).
		 * If set, this is matched against id, name and description
		 */
		protected String searchString;

		/**
		 * When set true, only forms last edited by the current user are returned
		 */
		protected boolean editor = false;

		/**
		 * When set true, only forms created by the current user are returned
		 */
		protected boolean creator = false;

		/**
		 * When set true, only forms last published by the current user are returned
		 */
		protected boolean publisher = false;

		/**
		 * When set, only forms last edited by one of the users are returned
		 */
		protected List<SystemUser> editors = null;

		/**
		 * When set, only forms created by one of the users are returned
		 */
		protected List<SystemUser> creators = null;

		/**
		 * When set, only forms published by one of the users are returned
		 */
		protected List<SystemUser> publishers = null;

		/**
		 * When set, only forms to which all of the permissions are granted will be returned
		 */
		protected List<Permission> permissions = Collections.emptyList();

		/**
		 * When set > 0, only forms that were edited before the given timestamp are returned
		 */
		protected int editedBefore = 0;

		/**
		 * When set > 0, only forms that were edited since the given timestamp are returned
		 */
		protected int editedSince = 0;

		/**
		 * When set > 0, only forms, that were created before the given timestamp are returned
		 */
		protected int createdBefore = 0;

		/**
		 * When set > 0, only forms, that were created since the given timestamp are returned
		 */
		protected int createdSince = 0;

		/**
		 * When set > 0, only forms, that were published before the given timestamp are returned
		 */
		protected int publishedBefore = 0;

		/**
		 * When set > 0, only forms, that were published since the given timestamp are returned
		 */
		protected int publishedSince = 0;

		/**
		 * When true, the search is done recursively
		 */
		protected boolean recursive = false;

		/**
		 * When true, only get forms that are online
		 */
		protected Boolean online = null;

		/**
		 * Flag for searching modified forms
		 */
		protected Boolean modified = null;

		/**
		 * Flag for searching planned forms
		 */
		protected Boolean planned = null;

		/**
		 * Wastebin filter setting
		 */
		protected Wastebin wastebin = Wastebin.EXCLUDE;

		/**
		 * Static method to create an empty form search
		 * @return empty form search
		 */
		public static FormSearch create() {
			return new FormSearch();
		}

		/**
		 * Create an empty form search (will find all forms)
		 */
		public FormSearch() {}

		/**
		 * Set the search string
		 * @param searchString new search string
		 * @return this object (for chaining)
		 */
		public FormSearch setSearchString(String searchString) {
			this.searchString = searchString;
			return this;
		}

		/**
		 * Set whether only forms last edited by the user shall be returned
		 * @param editor true for restricting forms
		 * @return this object (for chaining)
		 */
		public FormSearch setEditor(boolean editor) {
			this.editor = editor;
			return this;
		}

		/**
		 * Set whether only forms created by the user shall be returned
		 * @param creator true for restricting forms
		 * @return this object (for chaining)
		 */
		public FormSearch setCreator(boolean creator) {
			this.creator = creator;
			return this;
		}

		/**
		 * Set whether only forms last published by the user shall be returned
		 * @param creator true for restricting forms
		 * @return this object (for chaining)
		 */
		public FormSearch setPublisher(boolean publisher) {
			this.publisher = publisher;
			return this;
		}

		/**
		 * Set the editors for restricting forms
		 * @param editors list of editors
		 * @return this object (for chaining)
		 */
		public FormSearch setEditors(List<SystemUser> editors) {
			if (editors == null) {
				this.editors = null;
			} else {
				this.editors = Collections.unmodifiableList(editors);
			}
			return this;
		}

		/**
		 * Set the creators for restricting forms
		 * @param creators list of creators
		 * @return this object (for chaining)
		 */
		public FormSearch setCreators(List<SystemUser> creators) {
			if (creators == null) {
				this.creators = null;
			} else {
				this.creators = Collections.unmodifiableList(creators);
			}
			return this;
		}

		/**
		 * Set the publishers for restricting forms
		 * @param publishers list of publishers
		 * @return this object (for chaining)
		 */
		public FormSearch setPublishers(List<SystemUser> publishers) {
			if (publishers == null) {
				this.publishers = null;
			} else {
				this.publishers = Collections.unmodifiableList(publishers);
			}
			return this;
		}

		/**
		 * Set the form permissions for restricting forms.
		 * @param permissions
		 * @return
		 */
		public FormSearch setPermissions(List<Permission> permissions) {
			this.permissions = Collections.unmodifiableList(permissions);
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were edited before a given timestamp
		 * @param editedBefore timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setEditedBefore(int editedBefore) {
			this.editedBefore = editedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were edited since a given timestamp
		 * @param editedSince timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setEditedSince(int editedSince) {
			this.editedSince = editedSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were created before a given timestamp
		 * @param createdBefore timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setCreatedBefore(int createdBefore) {
			this.createdBefore = createdBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were created since a given timestamp
		 * @param createdSince timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setCreatedSince(int createdSince) {
			this.createdSince = createdSince;
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were published before a given timestamp
		 * @param publishedBefore timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setPublishedBefore(int publishedBefore) {
			this.publishedBefore = publishedBefore;
			return this;
		}

		/**
		 * Set the timestamp for restricting to forms which were published since a given timestamp
		 * @param publishedSince timestamp
		 * @return this object (for chaining)
		 */
		public FormSearch setPublishedSince(int publishedSince) {
			this.publishedSince = publishedSince;
			return this;
		}

		/**
		 * Set whether the search shall be done recursively
		 * @param recursive true for recursive search, false for search only in one folder
		 * @return this object (for chaining)
		 */
		public FormSearch setRecursive(boolean recursive) {
			this.recursive = recursive;
			return this;
		}

		/**
		 * Set whether the search shall be restricted to online forms
		 * @param online true if restricting to online forms, false for searching offline, null for no restriction
		 * @return this object (for chaining)
		 */
		public FormSearch setOnline(Boolean online) {
			this.online = online;
			return this;
		}

		/**
		 * Set whether searching for modified/unmodified forms
		 * @param modified modified flag
		 * @return fluent API
		 */
		public FormSearch setModified(Boolean modified) {
			this.modified = modified;
			return this;
		}

		/**
		 * Set whether searching for planned/unplanned forms
		 * @param planned planned flag
		 * @return fluent API
		 */
		public FormSearch setPlanned(Boolean planned) {
			this.planned = planned;
			return this;
		}

		/**
		 * Set the wastebin filter
		 * @param wastebin filter
		 * @return fluent API
		 */
		public FormSearch setWastebin(Wastebin wastebin) {
			if (wastebin != null) {
				this.wastebin = wastebin;
			}
			return this;
		}

		/**
		 * Set wastebin search
		 * @param wastebin search
		 * @return this object (for chaining)
		 */
		public FormSearch setWastebin(WastebinSearch wastebinSearch) {
			if (wastebinSearch != null) {
				switch (wastebinSearch) {
				case exclude:
					this.wastebin = Wastebin.EXCLUDE;
					break;
				case include:
					this.wastebin = Wastebin.INCLUDE;
					break;
				case only:
					this.wastebin = Wastebin.ONLY;
					break;
				default:
					this.wastebin = Wastebin.EXCLUDE;
					break;
				}
			}
			return this;
		}

		/**
		 * Set the values from the bean to the search
		 * @param bean bean to set
		 * @return fluent API
		 * @throws NodeException
		 */
		public FormSearch set(PublishableParameterBean bean) throws NodeException {
			if (bean != null) {
				setCreator(bean.isCreator);
				setCreatedBefore(bean.createdBefore);
				setCreatedSince(bean.createdSince);
				setCreators(getMatchingSystemUsers(bean.creator, bean.creatorIds));

				setEditor(bean.isEditor);
				setEditedBefore(bean.editedBefore);
				setEditedSince(bean.editedSince);
				setEditors(getMatchingSystemUsers(bean.editor, bean.editorIds));

				setPublisher(bean.isPublisher);
				setPublishedBefore(bean.publishedBefore);
				setPublishedSince(bean.publishedSince);
				setPublishers(getMatchingSystemUsers(bean.publisher, bean.publisherIds));

				setModified(bean.modified);
				setOnline(bean.online);
			}
			return this;
		}

		/**
		 * Check whether the search is empty (meaning: does not restrict the forms)
		 * @return true when the search is empty, false if not
		 */
		public boolean isEmpty() {
			return StringUtils.isEmpty(searchString) && !editor && !creator
					&& !publisher && editors == null && creators == null && publishers == null
					&& editedBefore == 0 && editedSince == 0 && createdBefore == 0 && createdSince == 0 && publishedBefore == 0 && publishedSince == 0
					&& !recursive && online == null && modified == null && planned == null && wastebin == Wastebin.EXCLUDE;
		}

		/**
		 * Get the search string
		 * @return search string
		 */
		public String getSearchString() {
			return searchString;
		}

		/**
		 * Get true when the search shall restrict to forms last edited by the user
		 * @return true or false
		 */
		public boolean isEditor() {
			return editor;
		}

		/**
		 * Get true when the search shall restrict to forms created by the user
		 * @return true or false
		 */
		public boolean isCreator() {
			return creator;
		}

		/**
		 * Get true when the search shall restrict to forms last published by the user
		 * @return true or false
		 */
		public boolean isPublisher() {
			return publisher;
		}

		/**
		 * Get the list of editors
		 * @return list of editors
		 */
		public List<SystemUser> getEditors() {
			return editors;
		}

		/**
		 * Get the list of creators
		 * @return list of creators
		 */
		public List<SystemUser> getCreators() {
			return creators;
		}

		/**
		 * Get the list of publishers
		 * @return list of publishers
		 */
		public List<SystemUser> getPublishers() {
			return publishers;
		}

		/**
		 * Get the list of restricting form permissions or an empty list, if not restricting forms by permission.
		 *
		 * @return List of permissions
		 */
		public List<Permission> getPermissions() {
			return permissions;
		}

		/**
		 * Get the timestamp for restricting to forms which were edited before a given timestamp
		 * @return timestamp
		 */
		public int getEditedBefore() {
			return editedBefore;
		}

		/**
		 * Get the timestamp for restricting to forms which were edited since a given timestamp
		 * @return timestamp
		 */
		public int getEditedSince() {
			return editedSince;
		}

		/**
		 * Get the timestamp for restricting to forms which were created before a given timestamp
		 * @return timestamp
		 */
		public int getCreatedBefore() {
			return createdBefore;
		}

		/**
		 * Get the timestamp for restricting to forms which were created since a given timestamp
		 * @return timestamp
		 */
		public int getCreatedSince() {
			return createdSince;
		}

		/**
		 * Get the timestamp for restricting to forms which were published before a given timestamp
		 * @return timestamp
		 */
		public int getPublishedBefore() {
			return publishedBefore;
		}

		/**
		 * Get the timestamp for restricting to forms which were published since a given timestamp
		 * @return timestamp
		 */
		public int getPublishedSince() {
			return publishedSince;
		}

		/**
		 * Get true, if the search is recursive, false if not
		 * @return true for recursive search, false for normal search
		 */
		public boolean isRecursive() {
			return recursive;
		}

		/**
		 * Get true, if the search shall restrict to online forms, false for offline forms, null for no restriction
		 * @return true for restricting to online forms
		 */
		public Boolean getOnline() {
			return online;
		}

		/**
		 * True for searching for modified forms, false for unmodified, null for no restriction
		 * @return true, false or null
		 */
		public Boolean getModified() {
			return modified;
		}

		/**
		 * True for searching for planned forms, false for unplanned, null for no restriction
		 * @return true, false or null
		 */
		public Boolean getPlanned() {
			return planned;
		}

		/**
		 * Get the wastebin filter
		 * @return wastebin filter
		 */
		public Wastebin getWastebin() {
			return wastebin;
		}
	}
}
