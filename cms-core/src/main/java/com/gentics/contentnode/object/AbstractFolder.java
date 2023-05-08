package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NoObjectTagSync;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.ExtensibleObject;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Abstract implementation of Folder
 */
public abstract class AbstractFolder extends AbstractContentObject implements Folder, ExtensibleObject<Folder> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5235370378675854451L;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	/**
	 * Properties, that must create dependencies on the master folders
	 */
	protected static List<String> MASTER_PROPERTIES = Arrays.asList(FOLDERS_PROPERTY, TEMPLATES_PROPERTY, PAGES_PROPERTY, FILES_PROPERTY, IMAGES_PROPERTY,
			FILESANDIMAGES_PROPERTY, "children");

	static {
		resolvableProperties = new HashMap<String, Property>();
		Property folder = new Property(null) {
			public Object get(AbstractFolder folder, String key) {
				return folder;
			}
		};

		resolvableProperties.put("ordner", folder);
		resolvableProperties.put("folder", folder);
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getNameI18n().translate(folder.getName());
			}
		});
		Property description = new Property(new String[] { "description"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getDescriptionI18n().translate(folder.getDescription());
			}
		};

		resolvableProperties.put("description", description);
		resolvableProperties.put("beschreibung", description);
		resolvableProperties.put("mother", new Property(new String[] { "mother"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				Folder mother = folder.getMother();

				if (mother == null) {
					return new Integer(0);
				}

				RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

				if (renderType != null && renderType.doHandleDependencies()) {
					renderType.addDependency(mother, "id");
				}

				return mother.getId();
			}
		});
		resolvableProperties.put("node_id", new Property(new String[] { "node_id"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getNode().getId();
			}
		});
		resolvableProperties.put("node", new Property(new String[] { "node_id"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getNode();
			}
		});
		resolvableProperties.put("parent", new Property(new String[] { "mother"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getMother();
			}
		});
		Property path = new Property(new String[] { "pub_dir"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getPublishDirI18n().translate(folder.getPublishDir());
			}
		};

		resolvableProperties.put("pub_dir", path);
		resolvableProperties.put("path", new Property(new String[] {"pub_dir", "mother"}) {
			@Override
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getPublishPath();
			}
		});

		Property creator = new Property(new String[] { "creator"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getCreator();
			}
		};

		resolvableProperties.put("creator", creator);
		resolvableProperties.put("ersteller", creator);
		Property editor = new Property(new String[] { "editor"}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getEditor();
			}
		};

		resolvableProperties.put("editor", editor);
		resolvableProperties.put("bearbeiter", editor);
		Property creationtimestamp = new Property(new String[] { "cdate"}) {
			public Object get(AbstractFolder folder, String key) {
				return folder.getCDate().getTimestamp();
			}
		};

		resolvableProperties.put("creationtimestamp", creationtimestamp);
		resolvableProperties.put("erstellungstimestamp", creationtimestamp);
		Property edittimestamp = new Property(new String[] { "edate"}) {
			public Object get(AbstractFolder folder, String key) {
				return folder.getEDate().getTimestamp();
			}
		};

		resolvableProperties.put("editdate", new Property(new String[] { "edate"}) {
			public Object get(AbstractFolder folder, String key) {
				return folder.getEDate();
			}
		});
		resolvableProperties.put("creationdate", new Property(new String[] { "cdate"}) {
			public Object get(AbstractFolder folder, String key) {
				return folder.getCDate();
			}
		});
		resolvableProperties.put("edittimestamp", edittimestamp);
		resolvableProperties.put("bearbeitungstimestamp", edittimestamp);
		resolvableProperties.put("object", new Property(new String[] {}) {
			public Object get(AbstractFolder folder, String key) {
				return new ObjectTagResolvable(folder);
			}
		});
		resolvableProperties.put(FOLDERS_PROPERTY, new Property(new String[] { FOLDERS_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getChildFolders();
			}
		});
		resolvableProperties.put(TEMPLATES_PROPERTY, new Property(new String[] { TEMPLATES_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getTemplates();
			}
		});
		resolvableProperties.put(PAGES_PROPERTY, new Property(new String[] { PAGES_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getPages(null);
			}
		});
		resolvableProperties.put(FILES_PROPERTY, new Property(new String[] { FILES_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getFiles();
			}
		});
		resolvableProperties.put(IMAGES_PROPERTY, new Property(new String[] { IMAGES_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getImages();
			}
		});
		resolvableProperties.put(FILESANDIMAGES_PROPERTY, new Property(new String[] {
			FILES_PROPERTY, IMAGES_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.getFilesAndImages(null);
			}
		});
		resolvableProperties.put("children", new Property(new String[] { FILES_PROPERTY, IMAGES_PROPERTY, PAGES_PROPERTY, FOLDERS_PROPERTY}) {
			public Object get(AbstractFolder folder, String key) throws NodeException {
				List<NodeObject> children = new ArrayList<NodeObject>();

				children.addAll(folder.getPages(null));
				children.addAll(folder.getFilesAndImages(null));
				children.addAll(folder.getChildFolders());
				return children;
			}
		});
		resolvableProperties.put("ismaster", new Property(null) {
			@Override
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.isMaster();
			}
		});
		resolvableProperties.put("inherited", new Property(null) {
			@Override
			public Object get(AbstractFolder folder, String key) throws NodeException {
				return folder.isInherited();
			}
		});
	}

	protected AbstractFolder(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getQualifiedName()
	 */
	public String getQualifiedName() throws NodeException {
		ArrayList<NodeObject> ancestors = new ArrayList<NodeObject>();
		NodeObject parent = this;

		while (null != parent) {
			ancestors.add(parent);
			parent = parent.getParentObject();
		}
		Collections.reverse(ancestors);
		StringBuffer qname = new StringBuffer();

		for (Iterator<NodeObject> i = ancestors.iterator(); i.hasNext();) {
			if (qname.length() > 0) {
				qname.append("/");
			}
			NodeObject ancestor = i.next();

			if (ancestor instanceof Folder) {
				qname.append(((Folder) ancestor).getName());
			}
		}
		return qname.toString();
	}

	@Override
	public void unlinkTemplate(Integer templateId) throws InsufficientPrivilegesException, NodeException {
		// unlinking is done for the master folder
		if (!isMaster()) {
			getMaster().unlinkTemplate(templateId);
			return;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER_INTEGER, this.getId(), PermHandler.PERM_TEMPLATE_DELETE)) {
			throw new InsufficientPrivilegesException("You are not allowed to unlink templates from the folder " + this, "no_perm_del_template", this.getName(),
					this, PermType.deletetemplates);
		}
		Template template = (Template) t.getObject(Template.class, templateId);

		// avoid nasty NPE
		if (template == null) {
			return;
		}
		// replace the template id with the id of the master
		template = template.getMaster();
		templateId = template.getId();
		Collection<Template> templatesToDelete = getTemplatesToDelete(Arrays.asList(new Template[] { template }), false);

		for (Template toDelete : templatesToDelete) {
			toDelete.delete();
		}

		if (template.isUnlinkable(this)) {
			performUnlinkTemplate(template.getId());
		} else {
			RenderResult renderResult = t.getRenderResult();
			CNI18nString message = new CNI18nString("notification.templatedelete.islinked");

			message.setParameter("1", template.getName());
			renderResult.addMessage(new DefaultNodeMessage(Level.INFO, Template.class, message.toString()));
		}
	}

	/**
	 * Performs the unlink process of a template. But instead of directly unlinking the template the action is cached.
	 * When committing the Transaction the action is performed.
	 * @param templateId The id of the template to delete
	 * @throws NodeException
	 */
	protected abstract void performUnlinkTemplate(Integer templateId) throws NodeException;


	/**
	 * Deletes the folder recursively.<br />
	 * If the transaction doesn't have the right to delete any of the objects
	 * inside the folder, the complete folder stays untouched and a
	 * {@link InsufficientPrivilegesException} is thrown.
	 * @param force true to really delete the folder, even if wastebin was activated
	 * @throws InsufficientPrivilegesException If the transaction doesn't have
	 *         the right to delete the folder or any subobjects of the folder.
	 */
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		Set<Template> templatesToCheck = internalDelete(force);

		for (Template template : templatesToCheck) {
			if (template.getPages().isEmpty() && template.getFolders().isEmpty()) {
				template.delete();
			}
		}
	}

	/**
	 * Internal delete method. This method will return the templates, that were linked to the folder and need to be checked whether they can be deleted.
	 * @param force true to really delete the folder, even if wastebin was activated
	 * @return set of templates to check
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	protected Set<Template> internalDelete(boolean force) throws InsufficientPrivilegesException, NodeException {
		Set<Template> templatesToCheck = new HashSet<Template>();

		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.isInterrupted()) {
			return templatesToCheck;
		}

		PermHandler permHandler = t.getPermHandler();

		if (!permHandler.canDelete(this)) {
			throw new InsufficientPrivilegesException("You are not allowed to delete the folder " + this, "no_perm_del_folder", this.getName(), this,
					PermType.delete);
		}

		boolean rootFolder = isRoot();
		boolean wastebin = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN, getOwningNode());


		if (isMaster()) {
			// collect all subfolders
			Set<Folder> subfolders = new HashSet<Folder>();

			try (ChannelTrx trx = new ChannelTrx(getChannel())) {
				subfolders.addAll(getChildFolders());
			}

			// also check for channel local subfolders in subchannels
			Collection<Node> allChannels = getNode().getAllChannels();
			for (Node channel : allChannels) {
				try (ChannelTrx trx = new ChannelTrx(channel)) {
					List<Folder> channelChildren = getChildFolders();

					for (Folder channelChild : channelChildren) {
						Node folderChannel = channelChild.getChannel();

						if (folderChannel != null && channelChild.isMaster()
								&& ObjectTransformer.getInt(folderChannel.getId(), -1) == ObjectTransformer.getInt(channel.getId(), -1)) {
							subfolders.add(channelChild);
						}
					}
				}
			}

			for (Folder subfolder : subfolders) {
				templatesToCheck.addAll(((AbstractFolder)subfolder).internalDelete(force));
			}
		}

		// collect all nodes, for which objects must be deleted
		List<Node> nodes = new ArrayList<Node>();
		Node folderNode = hasChannel() ? getChannel() : getNode();
		nodes.add(folderNode);
		nodes.addAll(folderNode.getAllChannels());

		// Delete Pages - has to be deleted before templates otherwise some templates wouldn't be removed in certain cases
		if (isMaster()) {
			PageSearch pageSearch = new PageSearch();
			pageSearch.setInherited(false);

			List<Page> pages = new ArrayList<Page>();

			for (Node node : nodes) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					pages.addAll(getPages(pageSearch));
				}
			}
			for (Page page : pages) {
				page.delete(force);
			}
		}

		// Delete Templates, but just the ones that should be really deleted
		if (isMaster() && (!wastebin || (wastebin && force))) {
			TemplateSearch templateSearch = new TemplateSearch();
			templateSearch.setInherited(false);

			List<Template> allTemplates = new ArrayList<Template>();

			for (Node node : nodes) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					allTemplates.addAll(getTemplates(templateSearch));
				}
			}

			// remove the templates, that are no master objects
			for (Iterator<Template> i = allTemplates.iterator(); i.hasNext();) {
				Template template = i.next();
				if (!template.isMaster()) {
					i.remove();
				}
			}

			if (!allTemplates.isEmpty()) {
				// Check permissions for template delete
				if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER_INTEGER, this.getId(), PermHandler.PERM_TEMPLATE_DELETE)) {
					throw new InsufficientPrivilegesException("You don't have the permission to delete templates from the folder " + this, 
							"no_perm_del_template", this.getName(), this, PermType.deletetemplates);
				}

				Collection<Template> templatesToDelete = getTemplatesToDelete(allTemplates);

				for (Template template : templatesToDelete) {
					template.delete(force);
				}

				allTemplates.removeAll(templatesToDelete);
				templatesToCheck.addAll(allTemplates);
			}
		}

		// delete files
		if (isMaster()) {
			FileSearch fileSearch = new FileSearch();
			fileSearch.setInherited(false);
			Collection<File> files = new ArrayList<File>();
			Collection<ImageFile> images = new ArrayList<ImageFile>();

			for (Node node : nodes) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					files.addAll(getFiles(fileSearch));
					images.addAll(getImages(fileSearch));
				}
			}
			// Delete Files
			for (File file : files) {
				file.delete(force);
			}
			// Delete Images
			for (ImageFile image : images) {
				image.delete(force);
			}
		}

		// delete forms
		if (isMaster()) {
			List<Form> forms = getForms(new FormSearch().setWastebin(t.getWastebinFilter()));
			for (Form form : forms) {
				form.delete(force);
			}
		}

		// put folder into wastebin
		if (!force && wastebin) {
			putIntoWastebin();
			templatesToCheck.clear();

			onDelete(this, true, t.getUserId());

			return templatesToCheck;
		}

		// Delete the ObjectTags
		Collection<ObjectTag> objTags = getObjectTags().values();

		// deleting folders should never cause deletion of objtags in other folders due to synchronization
		try (NoObjectTagSync noSync = new NoObjectTagSync()) {
			for (ObjectTag objTag : objTags) {
				objTag.delete();
			}
		}

		performDelete();

		onDelete(this, false, t.getUserId());

		if (t.isInterrupted()) {
			return templatesToCheck;
		}

		// commit the transaction after deleting every folder, so that the delete list does not get too long
		// we do not do this, if the feature del_single_transaction is set, or if the root folder is deleted
		// commiting the transaction after the root folder could cause inconsistent data, because the node entry referencing the deleted
		// former root folder could remain.
		if (!rootFolder && !getFeature("del_single_transaction")) {
			t.commit(false);
		}

		return templatesToCheck;
	}

	/**
	 * Performs the delete of the folder
	 */
	protected abstract void performDelete() throws NodeException;

	/**
	 * Put the folder into the wastebin
	 * @throws NodeException
	 */
	protected abstract void putIntoWastebin() throws NodeException;

	/**
	 * Returns true if the specified feature is enabled, otherwise false.
	 * @param feature The key of the feature to get.
	 * @return True, if the feature is enabled, otherwise false
	 */
	protected abstract boolean getFeature(String feature) throws NodeException;
    
	/**
	 * Gets the Templates which have no reference to another folder and can safely be removed
	 * @param templates A collection of templates suggested to delete
	 */
	protected abstract Collection<Template> getTemplatesToDelete(Collection<Template> templates) throws NodeException;
    
	/**
	 * Gets the Templates which have no reference to another folder and can safely be removed
	 * @param templates A collection of templates suggested to delete
	 * @param addFolderToDeleteList Specifies if the current folder should also be added to the list of folders marked to delete.
	 */
	protected abstract Collection<Template> getTemplatesToDelete(Collection<Template> templates, boolean addFolderToDeleteList) throws NodeException;


	/**
	 * get an objecttag of this folder by name without fallback.
	 * @param name the name of the object tag, without 'object.' prefix.
	 * @return the objecttag, or null if not found.
	 */
	public ObjectTag getObjectTag(String name) throws NodeException {
		return (ObjectTag) getObjectTags().get(name);
	}

	/**
	 * get an objecttag of this folder by name and do fallback if requested.
	 * @param name the name of the objecttag, without 'object.' prefix.
	 * @param fallback true, if fallback to parent objects should be done.
	 * @return the objecttag, or null if not found.
	 */
	public ObjectTag getObjectTag(String name, boolean fallback) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		ObjectTag tag = (ObjectTag) getObjectTags().get(name);

		if (renderType.doHandleDependencies()) {
			// add dependency on the - not existing - objecttag (such that the
			// folder will be dirted when the objecttag is created)
			if (tag == null) {
				renderType.addDependency(new DependencyObject(this, (NodeObject) null), "object." + name);
			} else {
				renderType.addDependency(new DependencyObject(this, tag), null);
			}
		}

		if ((tag == null || !tag.isEnabled()) && fallback) {// TODO: do fallback
		}

		return tag;
	}

	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = null;
			try {
				value = prop.get(this, key);
			} catch (NodeException e) {
				logger.error(String.format("unable to get property %s of folder %s", key, this), e);
				return null;
			}

			if (MASTER_PROPERTIES.contains(key)) {
				try {
					getMaster().addDependency(key, value);
				} catch (NodeException e) {
					logger.warn("Error while adding dependency on the master folder of " + this + ". Adding dependency on " + this + " instead", e);
					addDependency(key, value);
				}
			} else {
				addDependency(key, value);
			}
			return value;
		} else {
			return super.get(key);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackKeywords()
	 */
	public String[] getStackKeywords() {
		return RENDER_KEYS;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getShortcutResolvable()
	 */
	public Resolvable getShortcutResolvable() throws NodeException {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackHashKey()
	 */
	public String getStackHashKey() {
		return "folder:" + getHashKey();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.gentics.lib.base.StackResolvable#getKeywordResolvable(java.lang.String
	 * )
	 */
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode
	 * .events.DependencyObject, java.lang.String[], int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask,
			int depth, int channelId) throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);

		// this folder was moved
		if (Events.isEvent(eventMask, Events.MOVE) && !Events.isEvent(eventMask, Events.CHILD) && !Events.isEvent(eventMask, Events.PARENT)) {
			// trigger update event on the property "mother"
			triggerEvent(object, getModifiedPropertiesArray(new String[] { "mother", "node_id"}), Events.UPDATE, depth + 1, channelId);
		}

		// a child was moved (to or from this folder)
		if (Events.isEvent(eventMask, Events.MOVE) && Events.isEvent(eventMask, Events.CHILD)) {
			// trigger update event on the property "folders"
			triggerEvent(object, getModifiedPropertiesArray(new String[] { Folder.FOLDERS_PROPERTY}), Events.UPDATE, depth + 1, channelId);
		}

		// this folder or a parent was moved to another node
		if (Events.isEvent(eventMask, Events.MOVE) && Events.isEvent(eventMask, Events.PARENT) && !ObjectTransformer.isEmpty(property)) {
			// trigger update event on the properties (will most likely be
			// "node_id")
			triggerEvent(object, getModifiedPropertiesArray(property), Events.UPDATE, depth + 1, channelId);

			// dirt all pages in this folder
			List<Page> pages = getPages();

			for (Page page : pages) {
				page.dirtPage(channelId);
			}
		}

		if (Events.isEvent(eventMask, Events.UPDATE) && Events.isEvent(eventMask, Events.CHILD)) {
			// a child folder was created or removed
			// trigger update events on the folders property
			triggerEvent(object, getModifiedPropertiesArray(new String[] { Folder.FOLDERS_PROPERTY}), Events.UPDATE, depth + 1, channelId);
		}

		if (Events.isEvent(eventMask, Events.UPDATE) && !Events.isEvent(eventMask, Events.CHILD) && object.getElementClass() == null) {
			// trigger the event on the mother (for overviews)
			Folder mother = getMother();

			if (mother != null) {
				if (property == null || property.length != 2 || !PAGES_PROPERTY.equals(property[0]) || !"children".equals(property[1])) {
					// this event made big problems when triggered with the
					// 'pages' event
					// so i included this hackish check to not trigger it
					// explicitly for this one case...
					// TODO check if this is actually required
					mother.triggerEvent(new DependencyObject(mother, Folder.class), property != null && property.length == 0 ? null : property, eventMask,
							depth + 1, channelId);
				}
			}

			if (object.getElementClass() == null && property != null && property.length == 0) {
				// update event triggered on the whole folder (without
				// properties)
				// -> dirt the folder
				dirtFolder(channelId);
			}
		}

		// folder was deleted (in a channel) or revealed in a channel
		if ((Events.isEvent(eventMask, Events.DELETE) || Events.isEvent(eventMask, Events.HIDE) || Events.isEvent(eventMask, Events.REVEAL)
				|| Events.isEvent(eventMask, Events.CREATE)) && !Events.isEvent(eventMask, Events.CHILD)) {
			// dirt the "folder" property of all pages/files
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
				String[] folderProp = { "folder" };
				try (ChannelTrx cTrx = new ChannelTrx(channelId)) {
					for (Page p : getPages()) {
						p.triggerEvent(new DependencyObject(p), folderProp, Events.UPDATE, depth + 1, channelId);
					}
					for (File f : getFilesAndImages()) {
						f.triggerEvent(new DependencyObject(f), folderProp, Events.UPDATE, depth + 1, channelId);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.gentics.contentnode.object.AbstractContentObject#getModifiedProperties
	 * (java.lang.String[])
	 */
	protected List<String> getModifiedProperties(String[] modifiedDataProperties) {
		List<String> modifiedProperties = super.getModifiedProperties(modifiedDataProperties);

		return getModifiedProperties(resolvableProperties, modifiedDataProperties, modifiedProperties);
	}

	@Override
	public void dirtCache() throws NodeException {
		super.dirtCache();
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)
				&& !ObjectTransformer.getBoolean(t.getAttributes().get(DIRTING_CHANNELSET), false)) {
			try {
				t.getAttributes().put(DIRTING_CHANNELSET, true);
				Map<Integer, Integer> channelSet = getChannelSet();

				for (Map.Entry<Integer, Integer> entry : channelSet.entrySet()) {
					Integer folderId = entry.getValue();

					if (ObjectTransformer.getInt(folderId, -1) != ObjectTransformer.getInt(getId(), -1)) {
						t.dirtObjectCache(Folder.class, folderId, false);
					}
				}
			} finally {
				t.getAttributes().remove(DIRTING_CHANNELSET);
			}
		}
	}

	@Override
	public NodeObject getParentObject() throws NodeException {
		// parent of a folder is the mother
		return getMother();
	}

	@Override
	public int getEffectiveUdate() throws NodeException {
		// get the folder's udate
		int udate = getUdate();
		// check objtags
		Map<String, ObjectTag> tags = getObjectTags();

		for (Tag tag : tags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		return udate;
	}

	@Override
	public boolean isRecyclable() {
		return true;
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public String getPublishPath() throws NodeException {
		if (getNode().isPubDirSegment()) {
			Folder mother = getMother();
			if (mother == null) {
				return FilePublisher.getPath(true, true, ObjectTransformer.getString(get("pub_dir"), null));
			} else {
				String motherPath = ObjectTransformer.getString(mother.getPublishPath(), null);
				return FilePublisher.getPath(true, true, motherPath, ObjectTransformer.getString(get("pub_dir"), null));
			}
		} else {
			return ObjectTransformer.getString(get("pub_dir"), null);
		}
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 * @throws NodeException
		 */
		public abstract Object get(AbstractFolder object, String key) throws NodeException;
	}
}
