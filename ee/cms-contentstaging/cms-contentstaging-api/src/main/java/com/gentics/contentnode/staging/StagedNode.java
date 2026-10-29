package com.gentics.contentnode.staging;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.PageLanguageCode;

/**
 * REST entity for the node, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedNode extends StagingBase {

	public static final String TYPE_NAME = "node";

	private static final long serialVersionUID = 4249916187600453578L;

	private String publishDir;
	private String binaryPublishDir;
	private boolean pubDirSegment;
	private boolean https;
	private boolean publishImageVariants;
	private String hostname;
	private String hostnameProperty;
	private boolean publishFs;
	private boolean publishFsPages;
	private boolean publishFsFiles;
	private String contentRepositoryId;
	private boolean publishContentMap;
	private boolean publishContentMapPages;
	private boolean publishContentMapFiles;
	private boolean publishContentMapFolders;
	private List<String> languages;

	private String pageUrls;
	private String fileUrls;

	private boolean omitPageExtension;
	private PageLanguageCode pageLanguageCode;

	private String rootFolderId;

	private Set<String> features = new TreeSet<>();

	private Set<String> formTypes = new TreeSet<>();

	private Set<String> constructIds = new TreeSet<>();

	private Set<String> objectPropertyIds = new TreeSet<>();

	private Set<String> packages = new TreeSet<>();

	private String meshProjectName;

	private String meshPreviewUrlProperty;

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
	 * Get the publish directory for binaries
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
	public boolean isPubDirSegment() {
		return pubDirSegment;
	}

	/**
	 * Set flag for publish directory segments
	 * @param pubDirSegment flag
	 */
	public void setPubDirSegment(boolean pubDirSegment) {
		this.pubDirSegment = pubDirSegment;
	}

	/**
	 * True if image variants should be created on page/object property publish
	 * @return true 
	 */
	public boolean isPublishImageVariants() {
		return publishImageVariants;
	}

	/**
	 * Set whether image variants should be created on page/object property publish
	 * @param publishImageVariants
	 */
	public void setPublishImageVariants(boolean publishImageVariants) {
		this.publishImageVariants = publishImageVariants;
	}

	/**
	 * True if secure https is enabled for this node
	 * @return true for secure https
	 */
	public boolean isHttps() {
		return https;
	}

	/**
	 * Set whether secure https is enabled for the node
	 * @param https
	 */
	public void setHttps(boolean https) {
		this.https = https;
	}

	/**
	 * Hostname of the node
	 * @return hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Set the hostname
	 * @param hostname hostname
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Hostname property of the node
	 * @return hostname property
	 */
	public String getHostnameProperty() {
		return hostnameProperty;
	}

	/**
	 * Set the hostname property
	 * @param hostnameProperty hostname property
	 */
	public void setHostnameProperty(String hostnameProperty) {
		this.hostnameProperty = hostnameProperty;
	}

	/**
	 * True if the node shall publish into the filesystem
	 * @return true for publishing into filesystem
	 */
	public boolean isPublishFs() {
		return publishFs;
	}

	/**
	 * Set true for publishing into the filesystem
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files).
	 * @param publishFs
	 */
	public void setPublishFs(boolean publishFs) {
		this.publishFs = publishFs;
	}

	/**
	 * True if the node shall publish pages into the file system
	 * @return true for publishing pages into file system
	 */
	public boolean isPublishFsPages() {
		return publishFsPages;
	}

	/**
	 * Set true for publishing pages into the content repository
	 * @param publishFsPages Set to true for publishing pages into the content repository
	 */
	public void setPublishFsPages(boolean publishFsPages) {
		this.publishFsPages = publishFsPages;
	}

	/**
	 * True if the node shall publish files into the file system
	 * @return true for publishing files into file system
	 */
	public boolean isPublishFsFiles() {
		return publishFsFiles;
	}

	/**
	 * Set true for publishing files into the content repository
	 * @param publishFsFiles
	 */
	public void setPublishFsFiles(boolean publishFsFiles) {
		this.publishFsFiles = publishFsFiles;
	}

	/**
	 * ID of the assigned content repository
	 * @return content repository ID
	 */
	public String getContentRepositoryId() {
		return contentRepositoryId;
	}

	/**
	 * Set the content repository ID
	 * @param contentRepositoryId ID
	 */
	public void setContentRepositoryId(String contentRepositoryId) {
		this.contentRepositoryId = contentRepositoryId;
	}

	/**
	 * True if the node shall publish into a contentmap (if a contentrepository is assigned)
	 * @return true for publishing into content repository
	 */
	public boolean isPublishContentMap() {
		return publishContentMap;
	}

	/**
	 * Set true for publishing into contentmap
	 * Calling this also automatically enables/disables publishing for the
	 * individual object types (pages, files).
	 * @param publishContentMap
	 */
	public void setPublishContentMap(boolean publishContentMap) {
		this.publishContentMap = publishContentMap;
	}

	/**
	 * True if to publish pages to the content repository
	 * @return True for publishing pages
	 */
	public boolean isPublishContentMapPages() {
		return publishContentMapPages;
	}

	/**
	 * Set whether to publish pages to the content repository
	 * @param publishContentMapPages
	 */
	public void setPublishContentMapPages(boolean publishContentMapPages) {
		this.publishContentMapPages = publishContentMapPages;
	}

	/**
	 * True if to publish files to the content repository
	 * @return
	 */
	public boolean isPublishContentMapFiles() {
		return publishContentMapFiles;
	}

	/**
	 * Set whether to publish files to the content repository
	 * @param publishContentMapFiles
	 */
	public void setPublishContentMapFiles(boolean publishContentMapFiles) {
		this.publishContentMapFiles = publishContentMapFiles;
	}

	/**
	 * True if to publish folders to the content repository
	 * @return
	 */
	public boolean isPublishContentMapFolders() {
		return publishContentMapFolders;
	}

	/**
	 * Set whether to publish folders to the content repository
	 * @param publishContentMapFolders
	 */
	public void setPublishContentMapFolders(boolean publishContentMapFolders) {
		this.publishContentMapFolders = publishContentMapFolders;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	/**
	 * Should the page extensions be omitted?
	 * 
	 * @return
	 */
	public boolean isOmitPageExtension() {
		return omitPageExtension;
	}

	/**
	 * Set whether the page extensions be omitted.
	 * 
	 * @param omitPageExtension
	 */
	public void setOmitPageExtension(boolean omitPageExtension) {
		this.omitPageExtension = omitPageExtension;
	}

	/**
	 * Get page language code
	 * 
	 * @return
	 */
	public PageLanguageCode getPageLanguageCode() {
		return pageLanguageCode;
	}

	/**
	 * Set page language code
	 * 
	 * @param pageLanguageCode
	 */
	public void setPageLanguageCode(PageLanguageCode pageLanguageCode) {
		this.pageLanguageCode = pageLanguageCode;
	}

	/**
	 * ID of the root folder
	 * @return root folder ID
	 */
	public String getRootFolderId() {
		return rootFolderId;
	}

	/**
	 * Set the ID of the root folder
	 * @param rootFolderId root folder ID
	 */
	public void setRootFolderId(String rootFolderId) {
		this.rootFolderId = rootFolderId;
	}

	/**
	 * Get the features
	 * @return features set
	 */
	public Set<String> getFeatures() {
		return features;
	}

	/**
	 * Set the features
	 * @param features features set
	 */
	public void setFeatures(Set<String> features) {
		this.features.clear();
		this.features.addAll(features);
	}

	/**
	 * Get the form types
	 * @return form types
	 */
	public Set<String> getFormTypes() {
		return formTypes;
	}

	/**
	 * Set the form types
	 * @param formTypes form types to set
	 */
	public void setFormTypes(Set<String> formTypes) {
		this.formTypes.clear();
		this.formTypes.addAll(formTypes);
	}

	/**
	 * IDs of the assigned constructs
	 * @return construct IDs
	 */
	public Set<String> getConstructIds() {
		return constructIds;
	}

	/**
	 * Set the IDs of assigned constructs
	 * @param constructIds construct IDs
	 */
	public void setConstructIds(Set<String> constructIds) {
		this.constructIds.clear();
		this.constructIds.addAll(constructIds);
	}

	/**
	 * IDs of the assigned object properties
	 * @return object property IDs
	 */
	public Set<String> getObjectPropertyIds() {
		return objectPropertyIds;
	}

	/**
	 * Set the IDs of assigned object properties
	 * @param objectPropertyIds object property IDs
	 */
	public void setObjectPropertyIds(Set<String> objectPropertyIds) {
		this.objectPropertyIds.clear();
		this.objectPropertyIds.addAll(objectPropertyIds);
	}

	/**
	 * Devtool packages, to which the node is assigned
	 * @return package names
	 */
	public Set<String> getPackages() {
		return packages;
	}

	/**
	 * Set the assigned devtool packages
	 * @param packages packages
	 */
	public void setPackages(Set<String> packages) {
		this.packages = packages;
	}

	public String getPageUrls() {
		return pageUrls;
	}

	public void setPageUrls(String pageUrls) {
		this.pageUrls = pageUrls;
	}

	public String getFileUrls() {
		return fileUrls;
	}

	public void setFileUrls(String fileUrls) {
		this.fileUrls = fileUrls;
	}

	public String getMeshPreviewUrlProperty() {
		return meshPreviewUrlProperty;
	}

	public void setMeshPreviewUrlProperty(String meshPreviewUrlProperty) {
		this.meshPreviewUrlProperty = meshPreviewUrlProperty;
	}

	public String getMeshProjectName() {
		return meshProjectName;
	}

	public void setMeshProjectName(String meshProjectName) {
		this.meshProjectName = meshProjectName;
	}

	@XmlTransient
	@JsonIgnore
	public void clone(StagedNode origin) {
		super.clone(origin);
		this.publishDir = origin.publishDir;
		this.binaryPublishDir = origin.binaryPublishDir;
		this.pubDirSegment = origin.pubDirSegment;
		this.https = origin.https;
		this.publishFs = origin.publishFs;
		this.publishFsPages = origin.publishFsPages;
		this.publishFsFiles = origin.publishFsFiles;
		this.contentRepositoryId = origin.contentRepositoryId;
		this.publishContentMap = origin.publishContentMap;
		this.publishContentMapPages = origin.publishContentMapPages;
		this.publishContentMapFiles = origin.publishContentMapFiles;
		this.publishContentMapFolders = origin.publishContentMapFolders;
		this.languages = origin.languages;
		this.omitPageExtension = origin.omitPageExtension;
		this.pageLanguageCode = origin.pageLanguageCode;
		this.rootFolderId = origin.rootFolderId;
		this.constructIds = origin.constructIds;
		this.objectPropertyIds = origin.objectPropertyIds;
		this.packages = origin.packages;
		this.hostname = origin.hostname;
		this.hostnameProperty = origin.hostnameProperty;
		this.meshPreviewUrlProperty = origin.meshPreviewUrlProperty;
		this.fileUrls = origin.fileUrls;
		this.pageUrls = origin.pageUrls;
		this.meshProjectName = origin.meshProjectName;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return TYPE_NAME;
	}
}
