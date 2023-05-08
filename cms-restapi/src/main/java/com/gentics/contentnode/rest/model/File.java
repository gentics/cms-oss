/*
 * @author norbert
 * @date 26.04.2010
 * @version $Id: File.java,v 1.3.4.3 2011-02-08 14:14:39 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * File object, represents a File in GCN
 * @author norbert
 */
@XmlRootElement
public class File extends ContentNodeItem implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 516738470722803579L;

	/**
	 * Type ID
	 * @return the typeId
	 */
	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {}

	/**
	 * Ttype of file
	 */
	private final Integer typeId = 10008;

	/**
	 * Mimetype of the File
	 */
	protected String fileType;

	/**
	 * Description of the file
	 */
	protected String description;

	/**
	 * Id of the folder of the file
	 */
	protected Integer folderId;

	/**
	 * the folder name
	 */
	protected String folderName;
    
	/**
	 * Filesize of the file
	 */
	protected Integer fileSize;

	protected Object channelId;

	/**
	 * true when the file is inherited from a master channel, false if not
	 */
	protected boolean inherited;

	/**
	 * Tags in the file (objecttags)
	 */
	protected Map<String, Tag> tags;
    
	/**
	 * URL to the file.
	 */
	protected String url;

	/**
	 * The page's live URL.
	 * This is the assumed URL the page will
	 * have, when being published on the webserver.
	 */
	private String liveUrl;

	/**
	 * The page's publish path
	 */
	private String publishPath;

	/**
	 * Name of the node the object was inherited from
	 */
	private String inheritedFrom;

	/**
	 * Id of the node the object was inherited from
	 */
	private Integer inheritedFromId;

	/**
	 * name of the node, the master object belongs to
	 */
	private String masterNode;

	/**
	 * Id of the node, the master object belongs to
	 */
	private Integer masterNodeId;

	/**
	 * Path to the file, separated by '/', starting and ending with '/'
	 */
	private String path;

	/**
	 * True if the file shall be force to go online, even if nothing depends on it
	 * (may be null if status is undetermined).
	 */
	protected Boolean forceOnline;

	/**
	 * True if the file is online (for the node it was fetched for)
	 */
	protected boolean online;

	/**
	 * True if the file is broken
	 */
	protected boolean broken;

	/**
	 * Disinherited channels
	 */
	private Set<Node> disinheritedChannels;

	/**
	 * Whether this page is excluded from multichannelling.
	 */
	private Boolean excluded;

	/**
	 * Whether this folder is disinherited by default in new channels.
	 */
	private Boolean disinheritDefault;

	/**
	 * Whether this file is disinherited in some channels
	 */
	private Boolean disinherited;

	/**
	 * Folder of the file
	 */
	private Folder folder;

	/**
	 * Nice URL of the file
	 */
	private String niceUrl;

	/**
	 * Alternate URLs
	 */
	private SortedSet<String> alternateUrls;

	/**
	 * Name of the node, this file was inherited from
	 * @return
	 */
	public String getInheritedFrom() {
		return inheritedFrom;
	}

	/**
	 * Sets the name of the node the file was inherited from.
	 * @param inheritedFrom
	 */
	public void setInheritedFrom(String inheritedFrom) {
		this.inheritedFrom = inheritedFrom;
	}

	/**
	 * Return id of the node, this file was inherited from.
	 * @return
	 */
	public Integer getInheritedFromId() {
		return inheritedFromId;
	}

	/**
	 * Set the id of the node this file was inherited from.
	 * @param inheritedFromId
	 */
	public void setInheritedFromId(Integer inheritedFromId) {
		this.inheritedFromId = inheritedFromId;
	}

	/**
	 * Name of the node, the master object belongs to
	 * @return node name
	 */
	public String getMasterNode() {
		return masterNode;
	}

	/**
	 * Set the name of the node, the master object belongs to
	 * @param masterNode node name
	 */
	public void setMasterNode(String masterNode) {
		this.masterNode = masterNode;
	}

	/**
	 * Id of the node, the master object belongs to.
	 * @return
	 */
	public Integer getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the id of the node, the master object belongs to. 
	 * @param masterNodeId
	 */
	public void setMasterNodeId(Integer masterNodeId) {
		this.masterNodeId = masterNodeId;
	}

	/**
	 * Name of the folder of this file
	 * @return
	 */
	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	/**
	 * Channel ID
	 * @return
	 */
	public Object getChannelId() {
		return channelId;
	}
    
	public void setChannelId(Object channelId) {
		this.channelId = channelId;
	}

	/**
	 * Constructor used by JAXB
	 */
	public File() {
		super(ItemType.file);
	}

	/**
	 * Name of the file
	 * @See {@link #getName()}
	 * @return
	 */
	public String getText() {
		return getName();
	}

	public void setText(String text) {
		setName(text);
	}
    
	/**
	 * This is a file so leaf is true
	 * @return
	 */
	public boolean getLeaf() {
		return true;
	}

	public void setLeaf(boolean leaf) {}

	/**
	 * This is a file cls 
	 */
	public String getCls() {
		return "file";
	}

	public void setCls(String cls) {}
    
	/**
	 * Define attribute to select the appropriate class
	 * @return
	 */
	public String getIconCls() {
		return "gtx_file";
	}

	public void setIconCls(String iconCls) {}

	/**
	 * File type
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Folder ID
	 * @return the folderId
	 */
	public Integer getFolderId() {
		return folderId;
	}

	/**
	 * File size
	 * @return the fileSize
	 */
	public Integer getFileSize() {
		return fileSize;
	}

	/**
	 * Map of object tags of this file
	 * @return
	 */
	public Map<String, Tag> getTags() {
		return tags;
	}
    
	/**
	 * URL to the file
	 * @return the url to the file
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the live URL of the page
	 * @return
	 */
	public void setLiveUrl(String liveUrl) {
		this.liveUrl = liveUrl;
	}

	/**
	 * Live URL to the page
	 * @return
	 */
	public String getLiveUrl() {
		return liveUrl;
	}

	/**
	 * Set the publish path
	 * @param publishPath publish path
	 */
	public void setPublishPath(String publishPath) {
		this.publishPath = publishPath;
	}

	/**
	 * Publish path
	 * @return publish path
	 */
	public String getPublishPath() {
		return publishPath;
	}

	/**
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param folderId the folderId to set
	 */
	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(Integer fileSize) {
		this.fileSize = fileSize;
	}

	public void setTags(Map<String, Tag> tags) {
		this.tags = tags;
	}
    
	/**
	 * @param url the url to the file.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isInherited() {
		return inherited;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	/**
	 * Folder path of this file
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path
	 * @param path the path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * True if the file shall be force to go online, even if nothing depends on it
	 * (may be null if status is undetermined).
	 * @return true to force the file online
	 */
	public Boolean isForceOnline() {
		return forceOnline;
	}

	/**
	 * Set the force online status
	 * @param forceOnline new force online status
	 */
	public void setForceOnline(boolean forceOnline) {
		this.forceOnline = forceOnline;
	}

	/**
	 * True if the file is online, false if it is offline
	 * @return online status
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Set the online status
	 * @param online online status
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * True for broken files
	 * @return true for broken
	 */
	public boolean isBroken() {
		return broken;
	}

	/**
	 * Set the broken status
	 * @param broken true for broken
	 */
	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	/**
	 * the set of disinherited channels for this object
	 *
	 * @return the set of disinherited channels
	 */
	public Set<Node> getDisinheritedChannels() {
		return disinheritedChannels;
	}

	/**
	 * Set the disinherited channels for this object
	 *
	 * @param disinheritedChannels
	 *            the set of disinherited channnels
	 */
	public void setDisinheritedChannels(Set<Node> disinheritedChannels) {
		this.disinheritedChannels = disinheritedChannels;
	}

	/**
	 * Whether this page is excluded from multichannelling.
	 *
	 * @return true iff the file should be excluded from multichannelling
	 */
	public Boolean isExcluded() {
		return excluded;
	}

	/**
	 * Set wether the file is excluded from multichannelling
	 *
	 * @param excluded
	 *            if true, the file will be excluded from multichannelling
	 */
	public void setExcluded(Boolean excluded) {
		this.excluded = excluded;
	}

	/**
	 * Whether this folder is disinherited by default in new channels.
	 *
	 * @return <code>true</code> if the folder is disinherited in new channels,
	 *		<code>false</code> otherwise.
	 */
	public Boolean isDisinheritDefault() {
		return disinheritDefault;
	}

	/**
	 * Set whether this folder should be disinherited by default in new channels.
	 *
	 * @param disinheritDefault If set to <code>true</code> this folder will be
	 *		disinherited by default in new channels.
	 */
	public void setDisinheritDefault(Boolean disinheritDefault) {
		this.disinheritDefault = disinheritDefault;
	}

	/**
	 * True if the file is disinherited in some channels
	 * @return true iff the file is disinherited
	 */
	public Boolean isDisinherited() {
		return disinherited;
	}

	/**
	 * Set whether the file is disinherited
	 * @param disinherited true if disinherited
	 */
	public void setDisinherited(Boolean disinherited) {
		this.disinherited = disinherited;
	}

	/**
	 * Folder of the file
	 * @return folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * Set the folder
	 * @param folder folder
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	/**
	 * Nice URL
	 * @return nice URL
	 */
	public String getNiceUrl() {
		return niceUrl;
	}

	/**
	 * Set the nice URL
	 * @param niceUrl nice URL
	 */
	public void setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
	}

	/**
	 * Alternate URLs (in alphabetical order)
	 * @return sorted alternate URLs
	 */
	public SortedSet<String> getAlternateUrls() {
		return alternateUrls;
	}

	/**
	 * Set the alternate URLs
	 * @param alternateUrls alternate URLs
	 */
	public void setAlternateUrls(SortedSet<String> alternateUrls) {
		this.alternateUrls = alternateUrls;
	}
}
