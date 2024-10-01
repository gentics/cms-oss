/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ContentFile.java,v 1.23.4.2.2.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.NoObjectTagSync;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.ExtensibleObject;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * A contentFile is a basic implementation of a {@link File} and
 * {@link ImageFile}.
 */
public abstract class ContentFile extends AbstractContentObject implements ImageFile,
		StackResolvable, ObjectTagContainer, LocalizableNodeObject<ContentFile>, Disinheritable<ContentFile>, ExtensibleObject<File> {
    
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -6883724473951837799L;

	/**
	 * render keys for files (non images)
	 */
	public static final String[] RENDER_KEYS = new String[] { "datei", "file", "object"};

	/**
	 * render keys for image files
	 */
	public static final String[] RENDER_KEYS_IMAGE = new String[] { "datei", "file", "bild", "image", "object"};

	/**
	 * application/octet-stream
	 */
	public final static String DEFAULT_FILETYPE = "application/octet-stream";
    
	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	static {
		resolvableProperties = new HashMap<String, Property>();
		Property file = new Property(null) {
			public Object get(ContentFile file, String key) {
				if ("bild".equals(key) || "image".equals(key)) {
					return file.isImage() ? file : null;
				} else {
					return file;
				}
			}
		};

		resolvableProperties.put("datei", file);
		resolvableProperties.put("file", file);
		resolvableProperties.put("bild", file);
		resolvableProperties.put("image", file);
		resolvableProperties.put("ttype", new Property(null) {
			public Object get(ContentFile object, String key) {
				return object.isImage() ? ImageFile.TYPE_IMAGE_INTEGER : File.TYPE_FILE_INTEGER;
			}
		});
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			public Object get(ContentFile file, String key) {
				return file.getName();
			}
		});
		resolvableProperties.put("description", new Property(new String[] { "description"}) {
			public Object get(ContentFile file, String key) {
				return file.getDescription();
			}
		});
		Property size = new Property(new String[] { "filesize"}) {
			public Object get(ContentFile file, String key) {
				return new Integer(file.getFilesize());
			}
		};

		resolvableProperties.put("size", size);
		resolvableProperties.put("sizeb", size);

		// add a human readable file format which either prints MB or KB.
		Property readablesize = new Property(new String[] { "filesize"}) {
			public Object get(ContentFile file, String key) {
				// yep, it's stupid .. but it's the same way it was implemented in PHP
				if (file.getSizeMB() > 1) {
					return nf.format(file.getSizeMB()) + " MB";
				} else {
					return nf.format(file.getSizeKB()) + " KB";
				}
			}
		};

		resolvableProperties.put("readablesize", readablesize);
		Property sizekb = new Property(new String[] { "filesize"}) {
			public Object get(ContentFile file, String key) {
				synchronized (this) {
					return nf.format(file.getSizeKB());
				}
			}
		};

		resolvableProperties.put("sizekb", sizekb);
		resolvableProperties.put("sizeKB", sizekb);
		Property sizemb = new Property(new String[] { "filesize"}) {
			public Object get(ContentFile file, String key) {
				synchronized (this) {
					return nf.format(file.getSizeMB());
				}
			}
		};

		resolvableProperties.put("sizemb", sizemb);
		resolvableProperties.put("sizeMB", sizemb);
		resolvableProperties.put("folder_id", new Property(new String[] { "folder_id"}) {
			public Object get(ContentFile file, String key) {
				try {
					return file.getFolder().getId();
				} catch (NodeException e) {
					file.logger.error("unable to retrieve folder id of file {" + file.getId() + "}", e);
					return null;
				}
			}
		});
		Property folderProperty = new Property(new String[] { "folder_id"}) {
			public Object get(ContentFile file, String key) {
				try {
					return file.getFolder();
				} catch (NodeException e) {
					file.logger.error("unable to retrieve folder for {" + file + "}", e);
					return null;
				}
			}
		};

		resolvableProperties.put("folder", folderProperty);
		resolvableProperties.put("ordner", folderProperty);
		resolvableProperties.put("node", new Property(new String[] { "folder_id"}) {
			public Object get(ContentFile file, String key) {
				try {
					// add additional dependencies here
					RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

					if (renderType.doHandleDependencies()) {
						renderType.addDependency(file.getFolder(), "node");
					}

					return file.getFolder().getNode();
				} catch (NodeException e) {
					file.logger.error("unable to retrieve node for {" + file + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("extension", new Property(new String[] { "name"}) {
			public Object get(ContentFile file, String key) {
				return file.getExtension();
			}
		});
		Property creator = new Property(new String[] { "creator"}) {
			public Object get(ContentFile file, String key) {
				try {
					return file.getCreator();
				} catch (NodeException e) {
					file.logger.error("could not retrieve file creator", e);
					return null;
				}
			}
		};

		resolvableProperties.put("creator", creator);
		resolvableProperties.put("ersteller", creator);
		Property editor = new Property(new String[] { "editor"}) {
			public Object get(ContentFile file, String key) {
				try {
					return file.getEditor();
				} catch (NodeException e) {
					file.logger.error("could not retrieve file editor", e);
					return null;
				}
			}
		};

		resolvableProperties.put("editor", editor);
		resolvableProperties.put("bearbeiter", editor);
		Property createtimestamp = new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(ContentFile file, String key) {
				return file.getCustomOrDefaultCDate().getTimestamp();
			}
		};

		resolvableProperties.put("createtimestamp", createtimestamp);
		resolvableProperties.put("creationtimestamp", createtimestamp);
		// this typo (timstamp) is kept for backwards compatibility or maybe
		// just because it looks so neat?
		resolvableProperties.put("createtimstamp", createtimestamp);
		resolvableProperties.put("createdate", new Property(new String[] { "cdate", "custom_cdate" }) {
			public Object get(ContentFile file, String key) {
				return file.getCustomOrDefaultCDate();
			}
		});
		resolvableProperties.put("edittimestamp", new Property(new String[] { "edate", "custom_edate" }) {
			public Object get(ContentFile file, String key) {
				return file.getCustomOrDefaultEDate().getTimestamp();
			}
		});
		resolvableProperties.put("editdate", new Property(new String[] { "edate", "custom_edate" }) {
			public Object get(ContentFile file, String key) {
				return file.getCustomOrDefaultEDate();
			}
		});
		resolvableProperties.put("type", new Property(new String[] { "filetype"}) {
			public Object get(ContentFile file, String key) {
				return file.getFiletype();
			}
		});
		resolvableProperties.put("object", new Property(new String[] {}) {
			public Object get(ContentFile file, String key) {
				return new ObjectTagResolvable(file);
			}
		});
        
		resolvableProperties.put("url", new Property(new String[] { null}) {
			public Object get(ContentFile file, String key) {
				try {
					RenderUrl renderUrl;

					renderUrl = TransactionManager.getCurrentTransaction().getRenderType().getRenderUrl(File.class, file.getId());
					renderUrl.setMode(RenderUrl.MODE_LINK);
					return renderUrl.toString();
				} catch (NodeException e) {
					file.logger.error("could not generate file url", e);
					return null;
				}
			}
		});
		// TODO all props below this line are image props, which should only be
		// added if file.isImage() ist true. Maybe there is a better solution
		// than the if inside each prop.
		Property widthProperty = new Property(new String[] { "sizex"}) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Integer(file.getSizeX());
				} else {
					return null;
				}
			}
		};

		resolvableProperties.put("width", widthProperty);
		resolvableProperties.put("sizex", widthProperty);

		Property heightProperty = new Property(new String[] { "sizey"}) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Integer(file.getSizeY());
				} else {
					return null;
				}
			}
		};

		resolvableProperties.put("height", heightProperty);
		resolvableProperties.put("sizey", heightProperty);

		resolvableProperties.put("dpix", new Property(new String[] { "dpix"}) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Integer(file.getDpiX());
				} else {
					return null;
				}
			}
		});
		resolvableProperties.put("dpiy", new Property(new String[] { "dpiy"}) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Integer(file.getDpiY());
				} else {
					return null;
				}
			}
		});
		resolvableProperties.put("dpi", new Property(new String[] { "dpix", "dpiy"}) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					if (file.getDpiX() == file.getDpiY()) {
						return new Integer(file.getDpiX());
					} else {
						return file.getDpiX() + "x" + file.getDpiY();
					}
				} else {
					return null;
				}
			}
		});

		// Focal Point
		resolvableProperties.put("fpx", new Property(new String[] { "fpx" }) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Float(file.getFpX());
				} else {
					return null;
				}
			}
		});
		resolvableProperties.put("fpy", new Property(new String[] { "fpy" }) {
			public Object get(ContentFile file, String key) {
				if (file.isImage()) {
					return new Float(file.getFpY());
				} else {
					return null;
				}
			}
		});
		resolvableProperties.put("gis_resizable", new Property(new String[] { "gis_resizable" }) {
			public Object get(ContentFile file, String key) {
				return file.isGisResizable();
			}
		});
		resolvableProperties.put("isimage", new Property(new String[] { null}) {
			public Object get(ContentFile file, String key) {
				return file.isImage();
			}
		});
		resolvableProperties.put("isfile", new Property(new String[] { null}) {
			public Object get(ContentFile file, String key) {
				return file.isFile();
			}
		});
		resolvableProperties.put("ismaster", new Property(null) {
			@Override
			public Object get(ContentFile file, String key) {
				try {
					return file.isMaster();
				} catch (NodeException e) {
					file.logger.error("Error while checking property ismaster of file {" + file.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("inherited", new Property(null) {
			@Override
			public Object get(ContentFile file, String key) {
				try {
					return file.isInherited();
				} catch (NodeException e) {
					file.logger.error("Error while checking property inherited of file {" + file.getId() + "}", e);
					return null;
				}
			}
		});
		resolvableProperties.put("nice_url", new Property(new String[] { "nice_url"}) {
			public Object get(ContentFile file, String key) {
				return file.getNiceUrl();
			}
		});
		resolvableProperties.put("alternate_urls", new Property(new String[] {"alternate_urls"}) {
			public Object get(ContentFile file, String key) {
				try {
					return file.getAlternateUrls();
				} catch (NodeException e) {
					file.logger.error("Error while retrieving alternate URLs", e);
					return null;
				}
			}
		});

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected ContentFile(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * shared number formatter
	 */
	protected static NumberFormat nf = new DecimalFormat("#,##0.0", new DecimalFormatSymbols(Locale.US));
    
	/**
	 * get the filename of the file.
	 * @return the filename.
	 */
	public abstract String getName();

	@Override
	public String getFilename() {
		return getName();
	}

	/**
	 * Set the name of the file
	 * @param name new name
	 * @return previous name
	 * @throws NodeException when the file was not fetched for updating
	 */
	public abstract String setName(String name) throws NodeException;

	/**
	 * get the filetype as application-mime-type.
	 * @return the mime-type of the file.
	 */
	public abstract String getFiletype();

	/**
	 * Set the filetype of the file.
	 * Note: when using this method to set the filetype, no auto-detection of the
	 * filetype will be done while saving the binary content.
	 * @param filetype new filetype
	 * @return old filetype
	 */
	public abstract String setFiletype(String filetype) throws ReadOnlyException;
    
	/**
	 * get the folder of this file.
	 * @return the folder of this file.
	 */
	public abstract Folder getFolder() throws NodeException;

	/**
	 * Set the folder id of the file
	 * @param folderId new folder id
	 * @return old folder id
	 * @throws ReadOnlyException when the object was not fetched for update
	 * @throws NodeException 
	 */
	public Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
		failReadOnly();
		return null;
	}

	@Override
	public void setCustomCDate(int timestamp) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setCustomEDate(int timestamp) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the size of this file in bytes.
	 * @return the filesize in bytes.
	 */
	public abstract int getFilesize();
    
	/**
	 * Set the filesize of the file
	 * @param filesize
	 * @return old filesize of the file
	 */
	public abstract int setFilesize(int filesize) throws ReadOnlyException;
    
	/**
	 * get a description of this file.
	 * @return the description of this file.
	 */
	public abstract String getDescription();
    
	/**
	 * Set the description of the file
	 * @param description new description
	 * @return old description
	 * @throws ReadOnlyException when the file was not fetched for updating
	 */
	public abstract String setDescription(String description) throws ReadOnlyException;

	/**
	 * check, if this file is an image.
	 * 
	 * @return true, if the file is an image.
	 */
	public boolean isImage() {
		String fileType = getFiletype();
		if (fileType == null) {
			return false;
		} else {
			return fileType.startsWith("image");
		}
	}

	/**
	 * check, if this file is a file... omg... is used for {@link #resolvableProperties}
	 * @return true, if the file is a file. glorious
	 */
	public boolean isFile() {
		return !isImage();
	}
    
	/**
	 * get the extension of the file or an empty String if the file has no extension
	 * @return the extension of the filename or ""
	 */
	public String getExtension() {
		return getName().contains(".") ? getName().replaceAll(".*\\.(.*)$", "$1").toLowerCase() : "";
	}

	/**
	 * retrieve file creator
	 * @return creator of the file
	 * @throws NodeException
	 */
	public abstract SystemUser getCreator() throws NodeException;

	/**
	 * retrieve file editor
	 * @return last editor of the file
	 * @throws NodeException
	 */
	public abstract SystemUser getEditor() throws NodeException;
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#getFileStream()
	 */
	public abstract InputStream getFileStream() throws NodeException;
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#storeFileStream(java.io.InputStream)
	 */
	public abstract void setFileStream(InputStream stream) throws NodeException, ReadOnlyException;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.AbstractContentObject#delete(boolean)
	 */
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		// Permission check
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		if (!permHandler.canDelete(this)) {
			throw new InsufficientPrivilegesException("You don't have the privileges to delete the ContentFile with id { " + this.getId() + " }",
					"no_perm_del_files_images", this.getFolder().getName(), this, PermType.delete);
		}

		// put file into wastebin
		if (!force && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN, getOwningNode())) {
			putIntoWastebin();

			onDelete(this, true, t.getUserId());

			return;
		}

		Collection<ObjectTag> objectTags = getObjectTags().values();

		// deleting files should never cause deletion of objtags in other files due to synchronization
		try (NoObjectTagSync noSync = new NoObjectTagSync()) {
			for (ObjectTag objTag : objectTags) {
				objTag.delete();
			}
		}

		performDelete();

		onDelete(this, false, t.getUserId());
	}

	/**
	 * Performs the delete of the Contentfile
	 */
	protected abstract void performDelete() throws NodeException;

	/**
	 * Put the file into the wastebin
	 * @throws NodeException
	 */
	protected abstract void putIntoWastebin() throws NodeException;

	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getKeywordResolvable(java.lang.String)
	 */
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return this;
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
		return (isImage() ? "image:" : "file:") + getHashKey();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackKeywords()
	 */
	public String[] getStackKeywords() {
		return isImage() ? RENDER_KEYS_IMAGE : RENDER_KEYS;
	}

	public ObjectTag getObjectTag(String name, boolean fallback) throws NodeException {
		ObjectTag tag = getObjectTag(name);
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (renderType.doHandleDependencies()) {
			if (tag == null) {
				renderType.addDependency(new DependencyObject(this, (NodeObject) null), "object." + name);
			} else {
				renderType.addDependency(new DependencyObject(this, tag), null);
			}
		}

		if ((tag == null || !tag.isEnabled()) && fallback) {

			// get the folder's objecttag
			tag = getFolder().getObjectTag(name, true);
			if (renderType.doHandleDependencies()) {
				if (tag == null) {
					renderType.addDependency(new DependencyObject(getFolder(), (NodeObject) null), "object." + name);
				} else {
					renderType.addDependency(new DependencyObject(getFolder(), tag), null);
				}
			}
		}
		return tag;
	}

	public ObjectTag getObjectTag(String name) throws NodeException {
		return (ObjectTag) getObjectTags().get(name);
	}

	@Override
	public Set<String> getObjectTagNames(boolean fallback) throws NodeException {
		Set<String> names = new HashSet<>();
		names.addAll(getObjectTags().keySet());
		if (fallback) {
			names.addAll(getFolder().getObjectTags().keySet());
		}
		return names;
	}

	/**
	 * check whether the file has been changed since the given timestamp
	 * @param timestamp timestamp
	 * @return true when the file has been changed, false if not
	 * @throws NodeException
	 */
	public abstract boolean isChangedSince(int timestamp) throws NodeException;

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode.events.DependencyObject, java.lang.String[], int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);

		if (Events.isEvent(eventMask, Events.UPDATE)) {
			// trigger the folder (for overviews)
			getFolder().triggerEvent(new DependencyObject(getFolder(), File.class), property != null && property.length == 0 ? null : property, eventMask,
					depth + 1, 0);

			// update event triggered on the whole file (without properties)
			// -> dirt the file
			if (object.getElementClass() == null && property != null && property.length == 0) {
				dirtFile(channelId);
			}
		}

		// the file was moved
		if (Events.isEvent(eventMask, Events.MOVE)) {
			// the property "folder_id" changed
			List modProps = getModifiedProperties(new String[] { "folder_id"});

			triggerEvent(object, (String[]) modProps.toArray(new String[modProps.size()]), Events.UPDATE, depth + 1, channelId);
		}
	}

	/**
	 * Dirt the file
	 * @param channelId channe lid
	 * @throws NodeException
	 */
	public void dirtFile(int channelId) throws NodeException {
		PublishQueue.dirtObject(this, Action.DEPENDENCY, channelId);
		// TODO logging
		ActionLogger.logCmd(ActionLogger.MODIFY, TransactionManager.getCurrentTransaction().getTType(File.class), getId(), new Integer(0), "file dirted by JP");
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
		 */
		public abstract Object get(ContentFile object, String key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.AbstractContentObject#getModifiedProperties(java.lang.String[])
	 */
	protected List getModifiedProperties(String[] modifiedDataProperties) {
		List modifiedProperties = super.getModifiedProperties(modifiedDataProperties);

		return getModifiedProperties(resolvableProperties, modifiedDataProperties, modifiedProperties);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getParentObject()
	 */
	public NodeObject getParentObject() throws NodeException {
		// parent of a file is the folder
		return getFolder();
	}

	/**
	 * Get the filesize in KB
	 * @return filesize in KB
	 */
	public double getSizeKB() {
		return Math.ceil(getFilesize() * 10 / 1024.0) / 10;
	}

	/**
	 * Get the filesize in MB
	 * @return filesize in MB
	 */
	public double getSizeMB() {
		return Math.ceil(getFilesize() * 10 / 1048576.0) / 10;
	}

	/**
	 * Get the formatted size (in KB or MB) of the file
	 * @return formatted size
	 */
	public String getFormattedSize() {
		synchronized (this) {
			double sizeMB = getSizeMB();

			if (sizeMB > 1.) {
				return nf.format(sizeMB) + " MB";
			} else {
				return nf.format(getSizeKB()) + " KB";
			}
		}        
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getObject()
	 */
	public ContentFile getObject() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#setChannelInfo(java.lang.Integer, java.lang.Integer)
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {
		setChannelInfo(channelId, channelSetId, false);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#setChannelInfo(java.lang.Integer, java.lang.Integer, boolean)
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
	 */
	public void modifyChannelId(Integer channelId) throws ReadOnlyException,
				NodeException {
		failReadOnly();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#getMaster()
	 */
	public ContentFile getMaster() throws NodeException {
		return MultichannellingFactory.getMaster(this);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#getNextHigherObject()
	 */
	public ContentFile getNextHigherObject() throws NodeException {
		return MultichannellingFactory.getNextHigherObject(this);
	}

	/**
	 * Push this file into the given master
	 * @param master master node to push this file to
	 * @return target file
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public ContentFile pushToMaster(Node master) throws ReadOnlyException, NodeException {
		return MultichannellingFactory.pushToMaster(this, master).getObject();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.File#getChannelVariant(com.gentics.contentnode.object.Node)
	 */
	public File getChannelVariant(Node channel) throws NodeException {
		return MultichannellingFactory.getChannelVariant(this, channel);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the file's udate
		int udate = getUdate();
		// check effective udates from objtags
		Map<String, ObjectTag> tags = getObjectTags();

		for (Tag tag : tags.values()) {
			udate = Math.max(udate, tag.getEffectiveUdate());
		}
		return udate;
	}

	@Override
	public Node getNode() throws NodeException {
		Folder folder;
		try (NoMcTrx noMc = new NoMcTrx()) {
			folder = getFolder();
		}
		return folder.getNode();
	}

	@Override
	public boolean isRecyclable() {
		return true;
	}
}
