package com.gentics.contentnode.object;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.ExtensibleObject;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Editor;
import com.gentics.contentnode.rest.model.PageLanguageCode;

public abstract class AbstractNode extends AbstractContentObject implements Node, ExtensibleObject<Node> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5405836491576211827L;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	/**
	 * True if the node is a channel, false if not, null if not yet determined
	 */
	protected Boolean channel;

	static {
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("https", new Property(new String[] { "https" }) {
			@Override
			public Object get(AbstractNode node, String key) {
				return node.isHttps();
			}
		});
		resolvableProperties.put("host", new Property(new String[] { "host" }) {
			public Object get(AbstractNode node, String key) {
				return node.getHostname();
			}
		});
		Property path = new Property(new String[] { "pub_dir" }) {
			public Object get(AbstractNode node, String key) {
				return node.getPublishDir();
			}
		};

		resolvableProperties.put("pub_dir", path);
		resolvableProperties.put("path", path);
		resolvableProperties.put("pub_dir_bin", new Property(new String[] { "pub_dir_bin" }) {
			@Override
			public Object get(AbstractNode node, String key) {
				return node.getBinaryPublishDir();
			}
		});
		resolvableProperties.put("master", new Property(new String[] { "master" }) {
			public Object get(AbstractNode node, String key) {
				try {
					Node master = node.getMaster();

					// getMaster returns itself when it has no master
					if (node != master) {
						return master;
					}
				} catch (NodeException e) {
					node.logger.error("Could not get the master of" + node, e);
				}

				return null;
			}
		});
		resolvableProperties.put("utf8", new Property(new String[] { "utf8" }) {
			public Object get(AbstractNode node, String key) {
				return node.isUtf8();
			}
		});
		resolvableProperties.put("alohaeditor", new Property(new String[] { "alohaeditor" }) {
			public Object get(AbstractNode node, String key) {
				// Unfortunately, getEditorversion returns an int
				return node.getEditorversion() == Editor.AlohaEditor.ordinal();
			}
		});
		resolvableProperties.put("folder", new Property(new String[] { "folder_id" }) {
			public Object get(AbstractNode node, String key) {
				try {
					return node.getFolder();
				} catch (NodeException e) {
					node.logger.error("could not get root folder", e);
				}
				return null;
			}
		});
		resolvableProperties.put("languages", new Property(null) {
			public Object get(AbstractNode node, String key) {
				try {
					return node.getLanguages();
				} catch (NodeException e) {
					node.logger.error("could not get node languages", e);
					return null;
				}
			}
		});
		resolvableProperties.put("meshProject", new Property(null) {
			@Override
			public Object get(AbstractNode node, String key) {
				try {
					return node.getMeshProject();
				} catch (NodeException e) {
					node.logger.error("could not get mesh project", e);
					return null;
				}
			}
		});
	}

	protected AbstractNode(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public void setFolder(Folder folder) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setPublishDir(String publishDir) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setBinaryPublishDir(String publishDir) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPubDirSegment(boolean pubDirSegment) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setHttps(boolean https) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishImageVariants(boolean publishImageVariants) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setHostname(String hostname) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setHostnameProperty(String hostProperty) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void resolveHostnameProperty() throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFtpHostname(String ftpHostname) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFtpLogin(String ftpLogin) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFtpPassword(String ftpPassword) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFtpWwwRoot(String ftpWwwRoot) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFtpSync(boolean ftpSync) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishFilesystem(boolean publishFilesystem) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishFilesystemPages(boolean publish) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishFilesystemFiles(boolean publish) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishContentmap(boolean publishContentmap) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishContentMapPages(boolean publish) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishContentMapFiles(boolean publish) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishContentMapFolders(boolean publish) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setContentmapKeyword(String contentmapKeyword) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPublishDisabled(boolean publishDisabled) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		boolean isChannel = isChannel();

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			// when this node is a channel, we also need to delete localized and local objects
			if (isChannel) {
				// first all channel specific pages
				Collection<Page> localPages = getLocalChannelPages();

				for (Page page : localPages) {
					page.delete(true);
				}

				// then all channel specific files
				Collection<File> localFiles = getLocalChannelFiles();

				for (File file : localFiles) {
					file.delete(true);
				}

				// then all channel specific templates
				Collection<Template> localTemplates = getLocalChannelTemplates();

				for (Template template : localTemplates) {
					template.delete(true);
				}

				// then all channel specific folders (but not the root folder)
				Collection<Folder> localFolders = getLocalChannelFolders();
				Folder rootFolder = getFolder();

				for (Folder folder : localFolders) {
					if (folder != rootFolder) {
						folder.delete(true);
					}
				}
			}
			// delete the root folder
			// Wastebin: Since it is a node based feature,
			// we always force deletion when deleting nodes
			this.getFolder().delete(true);

			performDelete();

			onDelete(this, false, TransactionManager.getCurrentTransaction().getUserId());
		}
	}

	/**
	 * Performs the delete of the Node
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;

	@Override
	public void setUtf8(boolean utf8) throws ReadOnlyException {
		failReadOnly();
	}

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

	@Override
	public void setContentrepositoryId(Integer contentRepositoryId) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public ContentMap getContentMap() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// when the node does not publish into a contentmap, we return null
		if (!doPublishContentmap()) {
			return null;
		}

		// get the contentrepository
		Integer crId = ObjectTransformer.getInteger(getContentrepositoryId(), null);

		if (crId == null || crId.intValue() <= 0) {
			// when multichannelling is activated and this is a channel and the master node has a multichannelling aware contentrepository assigned, we get the contentmap
			// of the master
			if (isChannel()) {
				List<Node> masterNodes = getMasterNodes();

				if (!masterNodes.isEmpty()) {
					Node master = masterNodes.get(masterNodes.size() - 1);
					ContentMap masterContentMap = master.getContentMap();

					if (masterContentMap != null && masterContentMap.isMultichannelling()) {
						masterContentMap.addNode(this);
						return masterContentMap;
					}
				}
			}
			return null;
		}

		// get the contentmap instance
		ContentMap contentMap = null;
		Collection<ContentMap> contentMaps = t.getNodeConfig().getContentMaps();

		for (ContentMap temp : contentMaps) {
			if (temp.getId().equals(crId)) {
				contentMap = temp;
				break;
			}
		}

		if (contentMap != null) {
			contentMap.addNode(this);
		}
		return contentMap;
	}

	@Override
	public boolean isChannel() throws NodeException {
		if (channel == null) {
			channel = Boolean.valueOf((getFolder().getChannelMaster() != null));
		}
		return channel;
	}

	@Override
	public void setEditorversion(int editorversion) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setDefaultFileFolder(Folder folder) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setDefaultImageFolder(Folder folder) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setUrlRenderWayPages(int value) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setUrlRenderWayFiles(int value) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setOmitPageExtension(boolean omitPageExtension) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setPageLanguageCode(PageLanguageCode pageLanguageCode) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	@Override
	public void setMeshPreviewUrl(String url) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setMeshPreviewUrlProperty(String urlProperty) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void resolveMeshPreviewUrlProperty() throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setInsecurePreviewUrl(boolean insecurePreviewUrl) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public Node getConflictingNode() throws NodeException {
		// get all other nodes
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Node> otherNodes = t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS)).stream().filter(n -> !n.equals(this))
				.collect(Collectors.toList());

		List<String> thisData = FolderFactory.getHostnameAndBasePath(this);

		for (Node other : otherNodes) {
			List<String> otherData = FolderFactory.getHostnameAndBasePath(other);

			if (!Collections.disjoint(thisData, otherData)) {
				return other;
			}
		}

		return null;
	}

	@Override
	public String getName() {
		try {
			return getFolder().getName();
		} catch (NodeException e) {
			return getHostname();
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
		 */
		public abstract Object get(AbstractNode object, String key);
	}
}
