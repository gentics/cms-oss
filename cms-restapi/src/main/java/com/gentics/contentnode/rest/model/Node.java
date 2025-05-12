package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.webcohesion.enunciate.metadata.ReadOnly;

/**
 * REST Model for a Node
 */
@XmlRootElement
public class Node extends ContentNodeItem implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 597351838739931498L;

	private Integer folderId;
	private Integer masterId;

	/**
	 * Name of the master node
	 */
	private String masterName;

	/**
	 * Id of node or channel which this node inherits from.
	 */
	private Integer inheritedFromId;

	/**
	 * Name of the node or channel which this node inherits from
	 */
	private String inheritedFromName;

	/**
	 * Id of the master node.
	 */
	private Integer masterNodeId;
	private String publishDir;
	private String binaryPublishDir;
	private Boolean pubDirSegment;
	private Boolean https;
	private Boolean publishImageVariants;
	private String host;
	private String hostProperty;
	private Boolean utf8 = true;
	private Boolean publishFs;
	private Boolean publishFsPages;
	private Boolean publishFsFiles;
	private Boolean publishContentMap;
	private Boolean publishContentMapPages;
	private Boolean publishContentMapFiles;
	private Boolean publishContentMapFolders;
	private Integer contentRepositoryId;

	/**
	 * Name of the content repository
	 */
	private String contentRepositoryName;
	private Boolean disablePublish;
	private Integer defaultFileFolderId;
	private Integer defaultImageFolderId;
	private List<Integer> languagesId;
	private Integer editorVersion = 1;
	private Integer urlRenderWayPages;
	private Integer urlRenderWayFiles;
	private String meshPreviewUrl;
	private String meshPreviewUrlProperty;
	private Boolean insecurePreviewUrl;
	private String meshProject;
	private Boolean omitPageExtension;
	private PageLanguageCode pageLanguageCode;

	/**
	 * Create empty instance
	 */
	public Node() {
	}

	/**
	 * ID of the root folder
	 * @return root folder ID
	 */
	@ReadOnly
	public Integer getFolderId() {
		return folderId;
	}

	/**
	 * Set the root folder id
	 * @param folderId ID of the root folder
	 */
	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}

	/**
	 * Publish directory
	 * @return publish directory
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * Set the publish directory
	 * @param publishDir publish directory
	 */
	public void setPublishDir(String publishDir) {
		this.publishDir = publishDir;
	}

	/**
	 * Publish directory for binaries
	 * @return publish directory for binaries
	 */
	public String getBinaryPublishDir() {
		return binaryPublishDir;
	}

	/**
	 * Set the publish directory for binaries
	 * @param binaryPublishDir publish directory for binaries
	 */
	public void setBinaryPublishDir(String binaryPublishDir) {
		this.binaryPublishDir = binaryPublishDir;
	}

	/**
	 * True if the publish directories are created from segments defined for the folders. False if every folder has its own publish directory.
	 * @return true for publish directory segments
	 */
	public Boolean isPubDirSegment() {
		return pubDirSegment;
	}

	/**
	 * Set flag for publish directory segments
	 * @param pubDirSegment flag
	 */
	public void setPubDirSegment(Boolean pubDirSegment) {
		this.pubDirSegment = pubDirSegment;
	}

	/**
	 * True if secure https is enabled for this node
	 * @return true for secure https
	 */
	public Boolean isHttps() {
		return https;
	}

	/**
	 * Set image variants should be created on page/object property publish
	 * @param publishImageVariants
	 */
	public void setPublishImageVariants(Boolean publishImageVariants) {
		this.publishImageVariants = publishImageVariants;
	}

	/**
	 * True if image variants should be created on page/object property publish
	 * @return 
	 */
	public Boolean isPublishImageVariants() {
		return publishImageVariants;
	}

	/**
	 * Set whether secure https is enabled for the node
	 * @param https
	 */
	public void setHttps(Boolean https) {
		this.https = https;
	}

	/**
	 * Hostname of the node
	 * @return hostname
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the hostname
	 * @param host hostname
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Property containing the hostname. This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return hostname property
	 */
	public String getHostProperty() {
		return hostProperty;
	}

	/**
	 * Set the hostname property
	 * @param hostProperty hostname property
	 */
	public void setHostProperty(String hostProperty) {
		this.hostProperty = hostProperty;
	}

	/**
	 * True if the node content should be encoded in UTF8
	 * @deprecated No longer used since Aloha editor requires UTF-8.
	 * @return true for UTF8
	 */
	@Deprecated
	public Boolean isUtf8() {
		return utf8;
	}

	/**
	 * Set whether the node should be encoded in UTF8
	 * @param utf8
	 */
	@Deprecated
	public void setUtf8(Boolean utf8) {
		this.utf8 = utf8;
	}

	/**
	 * True if the node shall publish into the filesystem
	 * @return true for publishing into filesystem
	 */
	public Boolean isPublishFs() {
		return publishFs;
	}

	/**
	 * Set true for publishing into the filesystem
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files).
	 * @param publishFs
	 */
	public void setPublishFs(Boolean publishFs) {
		this.publishFs = publishFs;
	}

	/**
	 * True if the node shall publish pages into the file system
	 * @return true for publishing pages into file system
	 */
	public Boolean getPublishFsPages() {
		return publishFsPages;
	}

	/**
	 * Set true for publishing pages into the content repository
	 * @param publishFsPages Set to true for publishing pages into the content repository
	 */
	public void setPublishFsPages(Boolean publishFsPages) {
		this.publishFsPages = publishFsPages;
	}

	/**
	 * True if the node shall publish files into the file system
	 * @return true for publishing files into file system
	 */
	public Boolean getPublishFsFiles() {
		return publishFsFiles;
	}

	/**
	 * Set true for publishing files into the content repository
	 * @param publishFsFiles
	 */
	public void setPublishFsFiles(Boolean publishFsFiles) {
		this.publishFsFiles = publishFsFiles;
	}

	/**
	 * True if the node shall publish into a contentmap (if a contentrepository is assigned)
	 * @return true for publishing into content repository
	 */
	public Boolean isPublishContentMap() {
		return publishContentMap;
	}

	/**
	 * Set true for publishing into contentmap
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files).
	 * @param publishContentMap
	 */
	public void setPublishContentMap(Boolean publishContentMap) {
		this.publishContentMap = publishContentMap;
	}

	/**
	 * True if to publish pages to the content repository
	 * @return True for publishing pages
	 */
	public Boolean getPublishContentMapPages() {
		return publishContentMapPages;
	}

	/**
	 * Set whether to publish pages to the content repository
	 * @param publishContentMapPages
	 */
	public void setPublishContentMapPages(Boolean publishContentMapPages) {
		this.publishContentMapPages = publishContentMapPages;
	}

	/**
	 * True if to publish files to the content repository
	 * @return
	 */
	public Boolean getPublishContentMapFiles() {
		return publishContentMapFiles;
	}

	/**
	 * Set whether to publish files to the content repository
	 * @param publishContentMapFiles
	 */
	public void setPublishContentMapFiles(Boolean publishContentMapFiles) {
		this.publishContentMapFiles = publishContentMapFiles;
	}

	/**
	 * True if to publish folders to the content repository
	 * @return
	 */
	public Boolean getPublishContentMapFolders() {
		return publishContentMapFolders;
	}

	/**
	 * Set whether to publish folders to the content repository
	 * @param publishContentMapFolders
	 */
	public void setPublishContentMapFolders(Boolean publishContentMapFolders) {
		this.publishContentMapFolders = publishContentMapFolders;
	}

	/**
	 * ID of the assigned contentrepository
	 * @return contentrepository ID
	 */
	public Integer getContentRepositoryId() {
		return contentRepositoryId;
	}

	/**
	 * Set the contentrepository id
	 * @param contentRepositoryId
	 */
	public void setContentRepositoryId(Integer contentRepositoryId) {
		this.contentRepositoryId = contentRepositoryId;
	}

	/**
	 * True if publishing content modifications is disabled
	 * @return true for disabling publish
	 */
	public Boolean isDisablePublish() {
		return disablePublish;
	}

	/**
	 * Set true for disabling publishing content modifications
	 * @param disablePublish
	 */
	public void setDisablePublish(Boolean disablePublish) {
		this.disablePublish = disablePublish;
	}

	/**
	 * Editor to be used in the node
	 *
	 * Possible values are 0 for LiveEditor and 1 for Aloha editor.
	 *
	 * @return editor
	 */
	public Integer getEditorVersion() {
		return editorVersion;
	}

	/**
	 * Set the editor
	 *
	 * Possible values are 0 for LiveEditor and 1 for Aloha editor.
	 *
	 * @param editorVersion
	 */
	public void setEditorVersion(Integer editorVersion) {
		this.editorVersion = editorVersion;
	}

	/**
	 * Editor to be used in the node.
	 *
	 * <strong>Note:</strong> This field is here for backward compatibility
	 * reasons, the preferred way to get the editor is {@link #getEditorVersion}.
	 *
	 * @see #getEditorVersion
	 * @return Editor to be used in the node
	 */
	public Editor getContentEditor() {
		return editorVersion != null ? Editor.getByCode(editorVersion) : null;
	}

	/**
	 * Set the editor.
	 *
	 * <strong>Note:</strong> This field is here for backward compatibility
	 * reasons, the preferred way to set the editor is {@link #setEditorVersion}.
	 *
	 * @see #setEditorVersion(Integer)
	 * @param contentEditor The editor to be used.
	 */
	public void setContentEditor(Editor contentEditor) {
		editorVersion = contentEditor != null ? contentEditor.getCode() : null;
	}

	/**
	 * Default File Upload Folder ID
	 * @return default file folder ID
	 */
	public Integer getDefaultFileFolderId() {
		return defaultFileFolderId;
	}

	/**
	 * Set the default file folder id
	 * @param defaultFileFolderId
	 */
	public void setDefaultFileFolderId(Integer defaultFileFolderId) {
		this.defaultFileFolderId = defaultFileFolderId;
	}

	/**
	 * Default Image Upload Folder ID
	 * @return default image folder ID
	 */
	public Integer getDefaultImageFolderId() {
		return defaultImageFolderId;
	}

	/**
	 * Set the default image folder ID
	 * @param defaultImageFolderId
	 */
	public void setDefaultImageFolderId(Integer defaultImageFolderId) {
		this.defaultImageFolderId = defaultImageFolderId;
	}

	/**
	 * Ordered list of IDs of the languages assigned to the node
	 * @return list of language IDs
	 */
	public List<Integer> getLanguagesId() {
		return languagesId;
	}

	/**
	 * Set the languages IDs
	 * @param languagesId list of IDs
	 */
	public void setLanguagesId(List<Integer> languagesId) {
		this.languagesId = languagesId;
	}

	/**
	 * The id of the master node if this is a channel.
	 *
	 * @return The id of the master node if this is a channel,
	 *		<code>null</code> otherwise.
	 */
	public Integer getMasterId() {
		return masterId;
	}

	/**
	 * Set the id of the master channel.
	 *
	 * @param masterId Set to the id of the master channel if this
	 * is a channel, or to <code>null</code> if this is a normal node.
	 */
	public void setMasterId(Integer masterId) {
		this.masterId = masterId;
	}

	/**
	 * ID of the node or channel from which this node inherits from directly (immediate master node)
	 * @return ID
	 */
	@ReadOnly
	public Integer getInheritedFromId() {
		return inheritedFromId;
	}

	/**
	 * Set the id of the node or channel which inherits the node.
	 * @param inheritedFromId
	 */
	public void setInheritedFromId(Integer inheritedFromId) {
		this.inheritedFromId = inheritedFromId;
	}

	/**
	 * Name of the node or channel this channel inherits from directly (immediate master node)
	 * @return name
	 */
	@ReadOnly
	public String getInheritedFromName() {
		return inheritedFromName;
	}

	/**
	 * Set the name of the node or channel this node inherits from
	 * @param inheritedFromName name
	 */
	public void setInheritedFromName(String inheritedFromName) {
		this.inheritedFromName = inheritedFromName;
	}

	/**
	 * ID of the top master node of the node. The id will point to the node itself if there is no specific master.
	 * @return
	 */
	@ReadOnly
	public Integer getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the id of the master node of the node.
	 * @param masterNodeId
	 */
	public void setMasterNodeId(Integer masterNodeId) {
		this.masterNodeId = masterNodeId;
	}

	/**
	 * How URLs are rendered for pages in this node
	 * @return A value of UrlRenderWay
	 */
	public Integer getUrlRenderWayPages() {
		return urlRenderWayPages;
	}

	/**
	 * Set how URLs are rendered for pages in this node.
	 * @param value A value of UrlRenderWay
	 */
	public void setUrlRenderWayPages(Integer value) {
		this.urlRenderWayPages = value;
	}

	/**
	 * How URLs are rendered for files in this node
	 * @return A value of UrlRenderWay
	 */
	public Integer getUrlRenderWayFiles() {
		return urlRenderWayFiles;
	}

	/**
	 * Set how URLs are rendered for pages in this node.
	 * @param value A value of UrlRenderWay
	 */
	public void setUrlRenderWayFiles(Integer value) {
		this.urlRenderWayFiles = value;
	}

	/**
	 * Preview URL of Mesh Portal.
	 * @return URL
	 */
	public String getMeshPreviewUrl() {
		return meshPreviewUrl;
	}

	/**
	 * Set the Mesh Preview URL
	 * @param meshPreviewUrl URL
	 */
	public void setMeshPreviewUrl(String meshPreviewUrl) {
		this.meshPreviewUrl = meshPreviewUrl;
	}

	/**
	 * Property containing the preview URL. This can be set to a system property or environment variable in the format ${sys:property} or ${env:variable}.
	 * @return URL
	 */
	public String getMeshPreviewUrlProperty() {
		return meshPreviewUrlProperty;
	}

	/**
	 * Set the mesh preview URL property
	 * @param meshPreviewUrlProperty
	 */
	public void setMeshPreviewUrlProperty(String meshPreviewUrlProperty) {
		this.meshPreviewUrlProperty = meshPreviewUrlProperty;
	}

	/**
	 * Whether insecure connections to the preview URL are allowed.
	 * @return {@code true} when insecure connections to the preview URL are allowed
	 */
	public Boolean getInsecurePreviewUrl() {
		return insecurePreviewUrl;
	}

	/**
	 * Set whether insecure connections to the preview URL are allowed.
	 * @param insecurePreviewUrl Whether insecure connections to the preview URL are allowed
	 */
	public void setInsecurePreviewUrl(Boolean insecurePreviewUrl) {
		this.insecurePreviewUrl = insecurePreviewUrl;
	}

	/**
	 * Mesh Project, this node publishes into
	 * @return mesh project
	 */
	@ReadOnly
	public String getMeshProject() {
		return meshProject;
	}

	/**
	 * Set the mesh project
	 * @param meshProject mesh project
	 */
	public void setMeshProject(String meshProject) {
		this.meshProject = meshProject;
	}

	/**
	 * True to omit page extensions when generating filenames of pages
	 * @return flag
	 */
	public Boolean getOmitPageExtension() {
		return omitPageExtension;
	}

	/**
	 * Set the flag to omit page extensions
	 * @param omitPageExtension flag
	 */
	public void setOmitPageExtension(Boolean omitPageExtension) {
		this.omitPageExtension = omitPageExtension;
	}

	/**
	 * Setting for placement of language codes for pages
	 * @return language code placement
	 */
	public PageLanguageCode getPageLanguageCode() {
		return pageLanguageCode;
	}

	/**
	 * Set the language code placenment
	 * @param pageLanguageCode language code placement
	 */
	public void setPageLanguageCode(PageLanguageCode pageLanguageCode) {
		this.pageLanguageCode = pageLanguageCode;
	}

	/**
	 * Name of the master node, if this node is a channel
	 * @return master node name
	 */
	@ReadOnly
	public String getMasterName() {
		return masterName;
	}

	/**
	 * Set the name of the master node
	 * @param masterName name of the master node
	 */
	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	/**
	 * Name of the content repository assigned to the node
	 * @return content repository name
	 */
	@ReadOnly
	public String getContentRepositoryName() {
		return contentRepositoryName;
	}

	/**
	 * Set the name of the content repository
	 * @param contentRepositoryName content repository name
	 */
	public void setContentRepositoryName(String contentRepositoryName) {
		this.contentRepositoryName = contentRepositoryName;
	}

	@Override
	public String toString() {
		return "Node %s".formatted(getName());
	}
}
