/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Node.java,v 1.23.4.4 2011-02-11 14:38:19 norbert Exp $
 */
package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.substituteSingleProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.resolving.ResolvableMapWrappable;

/**
 * The object for a Node or Domain in content.node.
 */
@TType(Node.TYPE_NODE)
public interface Node extends StageableNodeObject, Resolvable, NamedNodeObject, ResolvableMapWrappable {

	/**
	 * The ttype for the node object.
	 */
	public static final int TYPE_NODE = 10001;

	public final static Integer TYPE_NODE_INTEGER = new Integer(TYPE_NODE);

	/**
	 * The ttype for the node object.
	 */
	public static final int TYPE_CHANNEL = 10033;

	public final static Integer TYPE_CHANNEL_INTEGER = new Integer(TYPE_CHANNEL);

	/**
	 * Use LiveEditor.
	 * @deprecated Only {@link #EDITOR_VERSION_ALOHA_EDITOR} is supported.
	 */
	int EDITOR_VERSION_LIVE_EDITOR = 0;
	/** Use Aloha editor. */
	int EDITOR_VERSION_ALOHA_EDITOR = 1;

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<Node, com.gentics.contentnode.rest.model.Node> TRANSFORM2REST = node -> {
		// beware of NPE
		if (node == null) {
			return null;
		}
		return ModelBuilder.getNode(node);
	};

	/**
	 * Predicate for validation of previewurl properties
	 */
	public final static Predicate<String> NODE_PREVIEWURL_FILTER = value -> StringUtils.startsWith(value, "NODE_PREVIEWURL_");

	/**
	 * Predicate for validation of host properties
	 */
	public final static Predicate<String> NODE_HOST_FILTER = value -> StringUtils.startsWith(value, "NODE_HOST_");

	/**
	 * How URLs will be rendered in publish mode
	 */
	public enum UrlRenderWay {
		AUTO                  (0, 0),
		PORTAL                (1, RenderUrl.LINK_PORTAL),
		STATIC_DYNAMIC        (2, RenderUrl.LINK_AUTO),
		STATIC_WITH_DOMAIN    (3, RenderUrl.LINK_HOST),
		STATIC_WITHOUT_DOMAIN (4, RenderUrl.LINK_REL);

		private int value;
		private int linkWay;

		UrlRenderWay(int value, int linkWay) {
			this.value   = value;
			this.linkWay = linkWay;
		}

		/**
		 * Get the value
		 * @return value
		 */
		public int getValue() {
			return value;
		}

		/**
		 * Get the associated linkway
		 * @return linkway
		 */
		public int getLinKway() {
			return this.linkWay;
		}

		/**
		 * Get the instance with the given value or {@link UrlRenderWay#AUTO} if not found
		 * @param value value
		 * @return instance
		 */
		public static UrlRenderWay fromValue(int value) {
			for (UrlRenderWay urlRenderWayValue : UrlRenderWay.values()) {
				if (urlRenderWayValue.getValue() == value) {
					return urlRenderWayValue;
				}
			}

			return AUTO;
		}
	}

	/**
	 * get the root folder of this node.
	 * @return the root folder for this node.
	 * @throws NodeException
	 */
	Folder getFolder() throws NodeException;

	/**
	 * Set the root folder of this node
	 * @param folder root folder of this node
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setFolder(Folder folder) throws ReadOnlyException, NodeException;

	@Override
	default void setFolder(Node node, Folder parent) throws NodeException {
		setFolder(parent);
	}

	/**
	 * If this node is a channel, get all localized and local folders
	 * @return list of localized and local folders in this channel
	 * @throws NodeException
	 */
	Collection<Folder> getLocalChannelFolders() throws NodeException;

	/**
	 * If this node is a channel, get all localized and local pages
	 * @return list of localized and local pages in this channel
	 * @throws NodeException
	 */
	Collection<Page> getLocalChannelPages() throws NodeException;

	/**
	 * If this node is a channel, get all localized and local templates
	 * @return list of localized and local templates in this channel
	 * @throws NodeException
	 */
	Collection<Template> getLocalChannelTemplates() throws NodeException;

	/**
	 * If this node is a channel, get all localized and local files/images
	 * @return list of localized and local files/images in this channel
	 * @throws NodeException
	 */
	Collection<File> getLocalChannelFiles() throws NodeException;

	/**
	 * get the flag of creating image variants for the binaries used by the page/object property.
	 * @return the base publish directory path.
	 */
	@FieldGetter("pub_img_variants")
	boolean isPublishImageVariants();

	/**
	 * Set the flag of creating image variants for the binaries used by the page/object property.
	 * @param publishImageVariants flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("pub_img_variants")
	void setPublishImageVariants(boolean publishImageVariants) throws ReadOnlyException;

	/**
	 * get the base publish directory path for this node.
	 * @return the base publish directory path.
	 */
	@FieldGetter("pub_dir")
	String getPublishDir();

	/**
	 * Set the base publish directory path for this node.
	 * @param publishDir publish dir
	 * @throws ReadOnlyException
	 */
	@FieldSetter("pub_dir")
	void setPublishDir(String publishDir) throws ReadOnlyException;

	/**
	 * get the base publish directory path for binaries for this node.
	 * @return the base publish directory path for binaries
	 */
	@FieldGetter("pub_dir_bin")
	String getBinaryPublishDir();

	/**
	 * Set the base publish directory path for binaries for this node
	 * @param publishDir publish dir
	 * @throws ReadOnlyException
	 */
	@FieldSetter("pub_dir_bin")
	void setBinaryPublishDir(String publishDir) throws ReadOnlyException;

	/**
	 * Check whether the publish directories are created from segments defined for each folder
	 * @return true, when using pub_dir segments
	 */
	@FieldGetter("pub_dir_segment")
	boolean isPubDirSegment();

	/**
	 * Set flag for creating publish directories from segments
	 * @param pubDirSegment flag for pub_dir_segment
	 * @throws ReadOnlyException
	 */
	@FieldSetter("pub_dir_segment")
	void setPubDirSegment(boolean pubDirSegment) throws ReadOnlyException;

	/**
	 * Check, whether the node uses the secure https protocol.
	 * @return true, if https is enabled for the node.
	 */
	@FieldGetter("https")
	boolean isHttps();

	/**
	 * Set whether the node uses the secure https protocol.
	 * @param https
	 * @throws ReadOnlyException
	 */
	@FieldSetter("https")
	void setHttps(boolean https) throws ReadOnlyException;

	/**
	 * get the hostname of this node.
	 * @return the hostname of the node.
	 */
	@FieldGetter("host")
	String getHostname();

	/**
	 * Set the hostname of this node
	 * @param hostname hostname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("host")
	void setHostname(String hostname) throws ReadOnlyException;

	/**
	 * get the hostname property of this node.
	 * @return the hostname property of the node.
	 */
	@FieldGetter("host_property")
	String getHostnameProperty();

	/**
	 * Set the hostname property of this node
	 * @param hostnameProperty hostname property
	 * @throws ReadOnlyException
	 */
	@FieldSetter("host_property")
	void setHostnameProperty(String hostnameProperty) throws ReadOnlyException;

	/**
	 * If a hostname property is set, resolve it now and set it as hostname.
	 * If the hostname changes, the internal "modified" flag will be set
	 * @throws ReadOnlyException
	 */
	void resolveHostnameProperty() throws ReadOnlyException;

	/**
	 * Get the effective hostname. This will do property substitution.
	 * @return effective hostname
	 */
	default String getEffectiveHostname() {
		String hostnameProperty = getHostnameProperty();
		if (!StringUtils.isBlank(hostnameProperty)) {
			return substituteSingleProperty(hostnameProperty, NODE_HOST_FILTER);
		} else {
			return getHostname();
		}
	}

	/**
	 * get the ftp remote hostname of this node, used for syncing.
	 * @return the hostname for ftp sync.
	 */
	@FieldGetter("ftphost")
	String getFtpHostname();

	/**
	 * Set the FTP Remote Hostname
	 * @param ftpHostname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ftphost")
	void setFtpHostname(String ftpHostname) throws ReadOnlyException;

	/**
	 * get the login name of the ftp user for ftp syncing.
	 * @return the ftp user login name.
	 */
	@FieldGetter("ftplogin")
	String getFtpLogin();

	/**
	 * Set the login name for the FTP User
	 * @param ftpLogin
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ftplogin")
	void setFtpLogin(String ftpLogin) throws ReadOnlyException;

	/**
	 * get the password of the ftp user for ftp syncing.
	 * @return the ftp user password.
	 */
	@FieldGetter("ftppassword")
	String getFtpPassword();

	/**
	 * Set the password for the FTP User
	 * @param ftpPassword
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ftppassword")
	void setFtpPassword(String ftpPassword) throws ReadOnlyException;

	/**
	 * get the root directory where the content should be synced on the remote machine.
	 * @return the root directory on the remote machine.
	 */
	@FieldGetter("ftpwwwroot")
	String getFtpWwwRoot();

	/**
	 * Set the root directory where the conten should be synced to
	 * @param ftpWwwRoot
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ftpwwwroot")
	void setFtpWwwRoot(String ftpWwwRoot) throws ReadOnlyException;

	/**
	 * check, if this node should be synced using ftp after publishing.
	 * @return true, if ftpsync is requested.
	 */
	@FieldGetter("ftpsync")
	boolean doFtpSync();

	/**
	 * Set whether this node should be synced using ftp
	 * @param ftpSync
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ftpsync")
	void setFtpSync(boolean ftpSync) throws ReadOnlyException;

	/**
	 * check if this node should be written to the filesystem. this is mandatory for ftpsync or rsync.
	 * @return true, if the node should be written to the filesystem.
	 */
	@FieldGetter("publish_fs")
	boolean doPublishFilesystem();

	/**
	 * Set whether this node should be written to the filesystem.
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files).
	 * @param publishFilesystem
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_fs")
	void setPublishFilesystem(boolean publishFilesystem) throws ReadOnlyException;

	/**
	 * Check if pages in this node should be written to the filesystem
	 * @return true, if pages the node should be written to the filesystem
	 */
	@FieldGetter("publish_fs_pages")
	boolean doPublishFilesystemPages();

	/**
	 * Set whether pages of this node should be written to the filesystem (publish_fs must be on also)
	 * @param publish
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_fs_pages")
	void setPublishFilesystemPages(boolean publish) throws ReadOnlyException;

	/**
	 * Check if files in this node should be written to the filesystem
	 * @return true, if files in the node should be written to the filesystem
	 */
	@FieldGetter("publish_fs_files")
	boolean doPublishFilesystemFiles();

	/**
	 * Set whether files in this node should be written to the filesystem.
	 * @param publish
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_fs_files")
	void setPublishFilesystemFiles(boolean publish) throws ReadOnlyException;

	/**
	 * check, if this node should be written to a contentmap.
	 * @return true, if this node should be written to a contentmap.
	 */
	@FieldGetter("publish_contentmap")
	boolean doPublishContentmap();

	/**
	 * Set whether this node should be written to a contentmap
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files, folders).
	 * @param publishContentmap
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_contentmap")
	void setPublishContentmap(boolean publishContentmap) throws ReadOnlyException;

	/**
	 * check, if pages in this node should be written to a contentmap.
	 * @return true, if pages in this node should be written to a contentmap.
	 */
	@FieldGetter("publish_contentmap_pages")
	boolean doPublishContentMapPages();

	/**
	 * Set whether pages in this node should be written to a contentmap
	 * @param publish
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_contentmap_pages")
	void setPublishContentMapPages(boolean publish) throws ReadOnlyException;

	/**
	 * check, if files in this node should be written to a contentmap.
	 * @return true, if this node should be written to a contentmap.
	 */
	@FieldGetter("publish_contentmap_files")
	boolean doPublishContentMapFiles();

	/**
	 * Set whether files in this node should be written to a contentmap
	 * @param publish
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_contentmap_files")
	void setPublishContentMapFiles(boolean publish) throws ReadOnlyException;

	/**
	 * Check, if folders in this node should be written to a contentmap.
	 * @return true, if this node should be written to a contentmap.
	 */
	@FieldGetter("publish_contentmap_folders")
	boolean doPublishContentMapFolders();

	/**
	 * Set whether folders in this node should be written to a contentmap
	 * @param publish
	 * @throws ReadOnlyException
	 */
	@FieldSetter("publish_contentmap_folders")
	void setPublishContentMapFolders(boolean publish) throws ReadOnlyException;

	/**
	 * get the keyname of the configured contentmap where the published data should be written to.
	 * @return the keyname of the contentmap to publish to, or null or an empty string, if the default contentmap should be used.
	 */
	@FieldGetter("contentmap_handle")
	String getContentmapKeyword();

	/**
	 * Set the keyname of the configured contentmap
	 * @param contentmapKeyword
	 * @throws ReadOnlyException
	 */
	@FieldSetter("contentmap_handle")
	void setContentmapKeyword(String contentmapKeyword) throws ReadOnlyException;

	/**
	 * get hasmap with language ids, ordered by sortorder
	 * @return hashmap keyed by sortorder containing contentgroup_id as values
	 * @throws NodeException
	 */
	HashMap<Integer, Integer> getOrderedNodeLanguageIds() throws NodeException;

	/**
	 * Check, whether publishing is disabled
	 * @return true when publishing is disabled, false if not
	 */
	@FieldGetter("disable_publish")
	boolean isPublishDisabled();

	/**
	 * Enable/disable publishing
	 * @param publishDisabled
	 * @throws ReadOnlyException
	 */
	@FieldSetter("disable_publish")
	void setPublishDisabled(boolean publishDisabled) throws ReadOnlyException;

	/**
	 * Get all online files in this node. If the feature {@link Feature#CONTENTFILE_AUTO_OFFLINE} is on for this node, this method will check the {@link FileOnlineStatus}
	 * for the files.
	 *
	 * @return collection of all files which shall be published into this node
	 * @throws NodeException
	 */
	Collection<File> getOnlineFiles() throws NodeException;

	/**
	 * check, if the content of this node should be published as utf8 content.
	 * @return true, if the content should be written in utf8.
	 */
	@FieldGetter("utf8")
	boolean isUtf8();

	/**
	 * Set whether the node should publish as utf8 content
	 * @param utf8
	 * @throws ReadOnlyException
	 */
	@FieldSetter("utf8")
	void setUtf8(boolean utf8) throws ReadOnlyException;

	/**
	 * Get the timestamp of the last publish process for this node
	 * @return timestamp of last publish process or -1 if the node was not yet published
	 * @throws NodeException
	 */
	int getLastPublishTimestamp() throws NodeException;

	/**
	 * Set the timestamp of the last publish process for this node to the transaction timestamp
	 * @throws NodeException
	 */
	void setLastPublishTimestamp() throws NodeException;

	/**
	 * Get sorted list of languages of this node
	 * @return sorted list of languages
	 * @throws NodeException
	 */
	List<ContentLanguage> getLanguages() throws NodeException;

	/**
	 * Get a md5 sum over the list of languages for the node. This string can be used as
	 * cache-key for caching page languages, that depend on the node settings.
	 * @return md5 sum over all languages assigned to the node
	 * @throws NodeException
	 */
	String getLanguagesMD5() throws NodeException;

	/**
	 * Get the id of the contentrepository into which the node publishes
	 * @return id of the contentrepository
	 */
	Integer getContentrepositoryId();

	/**
	 * Set the contentrepository id
	 * @param contentRepositoryId contentrepository id
	 * @throws ReadOnlyException
	 */
	void setContentrepositoryId(Integer contentRepositoryId) throws ReadOnlyException;

	/**
	 * Get the contentmap into which this node shall be published
	 * This method uses at least one additional sql statement, so it should not be used too often.
	 * Inside the publish process, better use the method {@link CnMapPublisher#getContentMap(Node, boolean)}.
	 * @return contentmap or null if the node is not published into a contentrepository
	 * @throws NodeException
	 */
	ContentMap getContentMap() throws NodeException;

	/**
	 * Check whether this node is a channel of the given node
	 * @param node possible master node
	 * @return true if this node is a channel of the given node, false if not
	 * @throws NodeException
	 */
	public default boolean isChannelOf(Node node) throws NodeException {
		return getMasterNodes().contains(node);
	}

	/**
	 * Determine whether this node is a channel.
	 * @return true for channels, false for normal nodes
	 * @throws NodeException
	 */
	boolean isChannel() throws NodeException;

	/**
	 * Get the list of master nodes of this node, if this node is a channel. The
	 * first element of this list will be the direct master, the second the
	 * master of this, and so on. If this node is no channel, an empty list will
	 * be returned.
	 * @return list of master nodes for multichannelling support
	 * @throws NodeException
	 */
	List<Node> getMasterNodes() throws NodeException;

	/**
	 * Get the (top) master node of this node. If this node is no channel, it will return itself
	 * @return (top) master node, never null
	 * @throws NodeException
	 */
	public default Node getMaster() throws NodeException {
		List<Node> masters = getMasterNodes();

		if (ObjectTransformer.isEmpty(masters)) {
			return this;
		} else {
			return masters.get(masters.size() - 1);
		}
	}

	/**
	 * Get the collection of channels, which use this node (directly) as master.
	 * If this node is not master to a channel, an empty list will be returned.
	 * @return direct subchannels of this node
	 * @throws NodeException
	 */
	Collection<Node> getChannels() throws NodeException;

	/**
	 * Get all subchannels of this node.
	 * If this node is not master to a channel, an empty list will be returned
	 * @return all subchannels of this node
	 * @throws NodeException
	 */
	Collection<Node> getAllChannels() throws NodeException;

	/**
	 * Get the list of constructs assigned to this node
	 * @return list of constructs assigned to this node
	 * @throws NodeException
	 */
	List<Construct> getConstructs() throws NodeException;

	/**
	 * Get the list of object tag definitions assigned to this node
	 * @return list of object tag definitions assigned to this node
	 * @throws NodeException
	 */
	List<ObjectTagDefinition> getObjectTagDefinitions() throws NodeException;

	/**
	 * Get the contentrepository for the node
	 * @return contentrepository for the node
	 * @throws NodeException
	 */
	ContentRepository getContentRepository() throws NodeException;

	/**
	 * Get the editor version
	 * @return editor version
	 */
	@FieldGetter("editorversion")
	int getEditorversion();

	/**
	 * Set the editor version
	 * @param editorversion
	 * @throws ReadOnlyException
	 */
	@FieldSetter("editorversion")
	void setEditorversion(int editorversion) throws ReadOnlyException;

	/**
	 * return the creation date of the node as a unix timestamp
	 * @return creation date as a unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * return the edit date of the node as a unix timestamp
	 * @return edit date as a unix timestamp
	 */
	ContentNodeDate getEDate();

	/**
	 * retrieve node creator
	 * @return creator of the node
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * retrieve node editor
	 * @return last editor of the node
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * Get the default file upload folder
	 * @return default file upload folder or null if non set
	 * @throws NodeException
	 */
	Folder getDefaultFileFolder() throws NodeException;

	/**
	 * Set the default file folder (or null to unset)
	 * @param folder default file folder or null
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setDefaultFileFolder(Folder folder) throws ReadOnlyException, NodeException;

	/**
	 * Get the default image upload folder
	 * @return default image upload folder or null if non set
	 * @throws NodeException
	 */
	Folder getDefaultImageFolder() throws NodeException;

	/**
	 * Set the default image folder (or null to unset)
	 * @param folder default image folder or null
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setDefaultImageFolder(Folder folder) throws ReadOnlyException, NodeException;

	/**
	 * Get the RenderUrl link way for pages
	 * @return RenderUrl constant
	 */
	public default int getLinkwayPages() {
		return UrlRenderWay.fromValue(this.getUrlRenderWayPages()).getLinKway();
	}

	/**
	 * Get the RenderUrl link way for files
	 * @return RenderUrl constant
	 */
	public default int getLinkwayFiles() {
		return UrlRenderWay.fromValue(this.getUrlRenderWayFiles()).getLinKway();
	}

	/**
	 * Get the url render way of pages
	 * @return renderway
	 */
	@FieldGetter("urlrenderway_pages")
	int getUrlRenderWayPages();

	/**
	 * Set the url render way of pages
	 * @param value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("urlrenderway_pages")
	void setUrlRenderWayPages(int value) throws ReadOnlyException;

	/**
	 * Get the url render way of files
	 * @return renderway
	 */
	@FieldGetter("urlrenderway_files")
	int getUrlRenderWayFiles();

	/**
	 * Set the url render way of files
	 * @param value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("urlrenderway_files")
	void setUrlRenderWayFiles(int value) throws ReadOnlyException;

	/**
	 * Get the flag to omit page extensions
	 * @return flag
	 * @throws NodeException
	 */
	@FieldGetter("omit_page_extension")
	boolean isOmitPageExtension() throws NodeException;

	/**
	 * Set the flag to omit page extensions
	 * @param omitPageExtension flag
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("omit_page_extension")
	void setOmitPageExtension(boolean omitPageExtension) throws ReadOnlyException, NodeException;

	/**
	 * Get the setting for where to add the page language code
	 * @return setting
	 * @throws NodeException
	 */
	@FieldGetter("page_language_code")
	PageLanguageCode getPageLanguageCode() throws NodeException;

	/**
	 * Set the setting for where to add the page language code
	 * @param pageLanguageCode setting
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("page_language_code")
	void setPageLanguageCode(PageLanguageCode pageLanguageCode) throws ReadOnlyException, NodeException;

	/**
	 * Get the mesh preview URL
	 * @return mesh preview URL
	 */
	@FieldGetter("mesh_preview_url")
	String getMeshPreviewUrl();

	/**
	 * Set the mesh preview URL
	 * @param url mesh preview URL
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mesh_preview_url")
	void setMeshPreviewUrl(String url) throws ReadOnlyException;

	/**
	 * Get the mesh preview URL property
	 * @return mesh preview URL property
	 */
	@FieldGetter("mesh_preview_url_property")
	String getMeshPreviewUrlProperty();

	/**
	 * Set the mesh preview URL property
	 * @param urlProperty mesh preview URL property
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mesh_preview_url_property")
	void setMeshPreviewUrlProperty(String urlProperty) throws ReadOnlyException;

	/**
	 * If a mesh preview URL property is set, resolve it now and set it as mesh preview URL.
	 * If the mesh preview URL changes, the internal "modified" flag will be set
	 * @throws ReadOnlyException
	 */
	void resolveMeshPreviewUrlProperty() throws ReadOnlyException;

	/**
	 * Get the effective mesh preview URL. This will do property substitution.
	 * @return effective mesh preview URL
	 */
	default String getEffectiveMeshPreviewUrl() {
		String meshPreviewUrlProperty = getMeshPreviewUrlProperty();
		if (!StringUtils.isBlank(meshPreviewUrlProperty)) {
			return substituteSingleProperty(meshPreviewUrlProperty, NODE_PREVIEWURL_FILTER);
		} else {
			return getMeshPreviewUrl();
		}
	}

	/**
	 * Check whether insecure connections to the preview URL are allowed.
	 * @return Whether insecure connections to the preview URL are allowed
	 */
	@FieldGetter("insecure_preview_url")
	boolean isInsecurePreviewUrl();

	/**
	 * Set whether insecure connections to the preview URL are allowed.
	 * @param insecurePreviewUrl Whether insecure connections to the preview URL are allowed
	 * @throws ReadOnlyException
	 */
	@FieldSetter("insecure_preview_url")
	void setInsecurePreviewUrl(boolean insecurePreviewUrl) throws ReadOnlyException;

	/**
	 * Get the mesh project name
	 * @return mesh project name
	 */
	@FieldGetter("mesh_project_name")
	String getMeshProjectName();

	/**
	 * Set the mesh project name
	 * @param meshProjectName mesh project name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mesh_project_name")
	void setMeshProjectName(String meshProjectName) throws ReadOnlyException;

	/**
	 * Get the list of features that are activated for the node.
	 * This will not return features, that cannot be activated per node or are generally deactivated.
	 * For channels, this will include features that are set on the master node and are inheritable
	 * @return list of activated features
	 * @throws NodeException
	 */
	List<Feature> getFeatures() throws NodeException;

	/**
	 * Activate the given feature for the node
	 * @param feature feature to activate
	 * @throws NodeException
	 */
	void activateFeature(Feature feature) throws NodeException;

	/**
	 * Deactivate the given feature for the node
	 * @param feature feature to deactivate
	 * @throws NodeException
	 */
	void deactivateFeature(Feature feature) throws NodeException;

	/**
	 * Purge the wastebin of this node
	 * @return map holding the numbers of purged objects per type
	 * @throws NodeException
	 */
	Map<Integer, Integer> purgeWastebin() throws NodeException;

	/**
	 * Get the templates assigned to the node
	 * @return templates of the node
	 * @throws NodeException
	 */
	Set<Template> getTemplates() throws NodeException;
	/**
	 * Assign the template to the node
	 * @param template template
	 * @throws NodeException
	 */
	void addTemplate(Template template) throws NodeException;

	/**
	 * Remove the template from the node.
	 * This will also remove the template from all folders of the node
	 * @param template template
	 * @throws NodeException
	 */
	void removeTemplate(Template template) throws NodeException;

	/**
	 * Assign the construct to the node
	 * @param construct construct
	 * @throws NodeException
	 */
	void addConstruct(Construct construct) throws NodeException;

	/**
	 * Remove the construct from the node
	 * @param construct construct
	 * @throws NodeException
	 */
	void removeConstruct(Construct construct) throws NodeException;

	/**
	 * Assign the object property definition to the node
	 * @param def object property definition
	 * @throws NodeException
	 */
	void addObjectTagDefinition(ObjectTagDefinition def) throws NodeException;

	/**
	 * Remove the object property definition from the node
	 * @param def object property definition
	 * @throws NodeException
	 */
	void removeObjectTagDefinition(ObjectTagDefinition def) throws NodeException;

	/**
	 * Get the mesh project, this node publishes into
	 * @return mesh project
	 * @throws NodeException
	 */
	default String getMeshProject() throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
			return MeshPublisher.getMeshProjectName(this);
		} else {
			return null;
		}
	}

	/**
	 * Get node, which conflicts with the hostname/pub_dir
	 * @return conflicting node
	 * @throws NodeException
	 */
	Node getConflictingNode() throws NodeException;

	@Override
	default String getSuffix() {
		try {
			return isChannel() ? ".channel" : ".node";
		} catch (NodeException e) {
			throw new IllegalStateException(e);
		}
	}
}
