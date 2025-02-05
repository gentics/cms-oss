/*
 * @author norbert
 * @date 26.04.2010
 * @version $Id: Folder.java,v 1.5.6.5 2011-03-18 11:56:56 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Folder object, representing a Folder in GCN
 * @author norbert
 */
@XmlRootElement
public class Folder extends ContentNodeItem implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4836885052432014192L;

	/**
	 * Identifier of the mother folder TODO link folder?
	 */
	private Integer motherId;

	/**
	 * publish directory of the Folder
	 */
	private String publishDir;

	/**
	 * Description of the folder
	 */
	private String description;

	/**
	 * id of the folder's startpage
	 * null if there is none
	 */
	private Object startPageId = null;

	/**
	 * Tags in the folder (objecttags)
	 */
	private Map<String, Tag> tags;

	/**
	 * List of subfolders
	 */
	private List<Folder> subfolders;

	/**
	 * True if the folder has subfolders, false if not
	 */
	private boolean hasSubfolders;

	/**
	 * Id of the folders node
	 */
	private Integer nodeId;

	/**
	 * true when the folder is inherited from a master channel, false if not
	 */
	private boolean inherited;

	/**
	 * Folder privileges
	 */
	private List<Privilege> privileges;

	/**
	 * Privileges as bits
	 */
	private String privilegeBits;

	/**
	 * Privilege map
	 */
	private PrivilegeMap privilegeMap;

	/**
	 * Position of the folder in the folder tree.
	 */
	private String atposidx;
    
	/**
	 * name of the node the object was inherited from
	 */
	private String inheritedFrom;

	/**
	 * id of the node the object was inherited from
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
	 * Path to the folder, separated by '/', starting and ending with '/'
	 */
	private String path;

	/**
	 * ID of the master node (read only)
	 */
	private Integer masterId;

	/**
	 * The channelset_id of the folder (read only)
	 */
	private Integer channelsetId;

	/**
	 * The channel_id of the folder (read only)
	 */
	private Integer channelId;

	/**
	 * is_master of the folder (read only)
	 */
	private Boolean isMaster;

	/**
	 * Disinherited channels
	 */
	private Set<Node> disinheritedChannels;

	/**
	 * Whether this folder is excluded from multichannelling.
	 */
	private Boolean excluded;

	/**
	 * Whether this folder is disinherited by default in new channels.
	 */
	private Boolean disinheritDefault;

	/**
	 * Whether this folder is disinherited in some channels
	 */
	private Boolean disinherited;

	/**
	 * Mesh Project, the node publishes into (only set for Node folders)
	 */
	private String meshProject;

	/**
	 * Breadcrumbs of the object
	 */
	private List<BreadcrumbItem> breadcrumbs;

	/**
	 * Translated names
	 */
	private Map<String, String> nameI18n;

	/**
	 * Translated descriptions
	 */
	private Map<String, String> descriptionI18n;

	/**
	 * Translated publish directories
	 */
	private Map<String, String> publishDirI18n;

	/**
	 * Constructor used by JAXB
	 */
	public Folder() {
		super(ItemType.folder);
	}

	/**
	 * Name of the node this folder is inherited from
	 * @return
	 */
	public String getInheritedFrom() {
		return inheritedFrom;
	}

	/**
	 * sets inherited from
	 * @param inheritedFrom
	 */
	public void setInheritedFrom(String inheritedFrom) {
		this.inheritedFrom = inheritedFrom;
	}

	/**
	 * Id of the node this folder is inherited from.
	 * @return
	 */
	public Integer getInheritedFromId() {
		return inheritedFromId;
	}

	/**
	 * Sets the id of the node from which the folder was inherited from.
	 * 
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
	 * Return the id of the node, the master object belongs to.
	 * @return
	 */
	public Integer getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the id of the node the master object belongs to.
	 * @param masterNodeId
	 */
	public void setMasterNodeId(Integer masterNodeId) {
		this.masterNodeId = masterNodeId;
	}

	/**
	 * Mother id of the folder
	 * @return the motherId
	 */
	public Integer getMotherId() {
		return motherId;
	}

	/**
	 * Publish directory of the folder
	 * @return the publishDir
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Node id
	 * @return node id
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * True if the folder is inherited, false if not
	 * @return true if the folder is inherited, false if not
	 */
	public boolean isInherited() {
		return inherited;
	}

	/**
	 * Position of the folder in the folder tree
	 * @return position of the folder in the folder tree
	 */
	public String getAtposidx() {
		return atposidx;
	}

	/**
	 * @param motherId the motherId to set
	 */
	public void setMotherId(Integer motherId) {
		this.motherId = motherId;
	}

	/**
	 * @param publishDir the pubDir to set
	 */
	public void setPublishDir(String publishDir) {
		this.publishDir = publishDir;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * ID of the startpage
	 * @return startpage id
	 */
	public Object getStartPageId() {
		return startPageId;
	}

	/**
	 * set startpage id
	 * @param startPageId
	 */
	public void setStartPageId(Object startPageId) {
		this.startPageId = startPageId;
	}

	/**
	 * Map of object tags of the folder
	 * @return the tags
	 */
	public Map<String, Tag> getTags() {
		return tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(Map<String, Tag> tags) {
		this.tags = tags;
	}

	/**
	 * List of subfolders
	 * @return the subfolders
	 */
	public List<Folder> getSubfolders() {
		return subfolders;
	}

	/**
	 * True if the folder has subfolders (regardless of whether they have been fetched), false if not
	 * @return true for folders having subfolders
	 */
	public boolean isHasSubfolders() {
		return hasSubfolders;
	}

	/**
	 * @param subfolders the subfolders to set
	 */
	public void setSubfolders(List<Folder> subfolders) {
		this.subfolders = subfolders;
	}

	/**
	 * Set whether the folder has subfolders
	 * @param hasSubfolders true if the folder has subfolders, false if not
	 */
	public void setHasSubfolders(boolean hasSubfolders) {
		this.hasSubfolders = hasSubfolders;
	}

	/**
	 * Set the node id
	 * @param nodeId node id
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Set whether the folder is inherited
	 * @param inherited true if the folder is inherited, false if not
	 */
	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}
    
	/**
	 * Folder privileges
	 * @return the folder privileges
	 */
	public List<Privilege> getPrivileges() {
		return this.privileges;
	}
    
	/**
	 * Set the folder privileges
	 * 
	 * @param privileges Folder privileges
	 */
	public void setPrivileges(List<Privilege> privileges) {
		this.privileges = privileges;
	}

	/**
	 * Privilege bits
	 * @return privilege bits
	 */
	public String getPrivilegeBits() {
		return privilegeBits;
	}

	/**
	 * Set the privilege bits
	 * @param privilegeBits
	 */
	public void setPrivilegeBits(String privilegeBits) {
		this.privilegeBits = privilegeBits;
	}

	/**
	 * Map representation of all privileges
	 * @return privilege map
	 */
	public PrivilegeMap getPrivilegeMap() {
		return privilegeMap;
	}

	/**
	 * Set the privilege map
	 * @param privilegeMap
	 */
	public void setPrivilegeMap(PrivilegeMap privilegeMap) {
		this.privilegeMap = privilegeMap;
	}

	/**
	 * Set the position of the folder in the folder tree
	 * @param atposidx position of the folder in the folder tree
	 */
	public void setAtposidx(String atposidx) {
		this.atposidx = atposidx;
	}

	/**
	 * Folder path of this folder
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
	 * Master ID. The master is the next folder up in the channel hierarchy with the same Channelset ID.
	 * The Master ID is 0 if there is no master. Read only.
	 *
	 * @return Master ID
	 */
	public Integer getMasterId() {
		return masterId;
	}

	/**
	 * Sets the Master ID
	 *
	 * @param masterId
	 *            Master ID
	 */
	public void setMasterId(Integer masterId) {
		this.masterId = masterId;
	}

	/**
	 * Channelset ID. All copies of the same folder in different channels share the same Channelset ID. Read only.
	 *
	 * @return Channelset ID
	 */
	public Integer getChannelsetId() {
		return channelsetId;
	}

	/**
	 * Sets the Chnanelset ID
	 *
	 * @param channelsetId
	 *            Channelset ID
	 */
	public void setChannelsetId(Integer channelsetId) {
		this.channelsetId = channelsetId;
	}

	/**
	 * Channel ID. It identifies different versions of the same folder in different channels. Equals to
	 * the node id for which the folder is defined, or to 0 if it is defined in the topost node of the channel hierarchy. Read only.
	 *
	 * @return Channel ID
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * Sets the Channel ID
	 *
	 * @param channelId
	 *            Channel ID
	 */
	public void setChannelId(Integer channelId) {
		this.channelId = channelId;
	}

	/**
	 * True if the folder is a master, false otherwise. A folder is a master if it isn't a localized copy of another folder. Read only.
	 *
	 * @return is_master
	 */
	public Boolean getIsMaster() {
		return isMaster;
	}

	/**
	 * Sets is_master
	 *
	 * @param isMaster
	 *            is_master
	 */
	public void setIsMaster(Boolean isMaster) {
		this.isMaster = isMaster;
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
	 * Whether this folder is excluded from multichannelling.
	 *
	 * @return true iff the folder should be excluded from multichannelling
	 */
	public Boolean isExcluded() {
		return excluded;
	}

	/**
	 * Set wether the folder is excluded from multichannelling
	 *
	 * @param excluded
	 *            if true, the folder will be excluded from multichannelling
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
	 * True if the folder is disinherited in some channels
	 * @return true iff the folder is disinherited
	 */
	public Boolean isDisinherited() {
		return disinherited;
	}

	/**
	 * Set whether the folder is disinherited
	 * @param disinherited true if disinherited
	 */
	public void setDisinherited(Boolean disinherited) {
		this.disinherited = disinherited;
	}

	/**
	 * Mesh Project, this node publishes into. This will only be set for root folders
	 * @return mesh project
	 */
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
	 * Breadcrums of the folder. The first item is the root folder and the last item the folder itself
	 * @return list of breadcrumb items
	 */
	public List<BreadcrumbItem> getBreadcrumbs() {
		return breadcrumbs;
	}

	/**
	 * Set the breadcrumbs
	 * @param breadcrumbs
	 */
	public void setBreadcrumbs(List<BreadcrumbItem> breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}

	/**
	 * Map of translated names (keys are the language codes)
	 * @return name map
	 */
	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	/**
	 * Set translated names
	 * @param nameI18n map of translations
	 */
	public void setNameI18n(Map<String, String> nameI18n) {
		this.nameI18n = nameI18n;
	}

	/**
	 * Map of translated descriptions (keys are the language codes)
	 * @return description map
	 */
	public Map<String, String> getDescriptionI18n() {
		return descriptionI18n;
	}

	/**
	 * Set translated descriptions
	 * @param descriptionI18n map of translations
	 */
	public void setDescriptionI18n(Map<String, String> descriptionI18n) {
		this.descriptionI18n = descriptionI18n;
	}

	/**
	 * Map of translated publish directories (keys are the language codes)
	 * @return publish directory map
	 */
	public Map<String, String> getPublishDirI18n() {
		return publishDirI18n;
	}

	/**
	 * Set translated publish directories
	 * @param publishDirI18n map of translations
	 */
	public void setPublishDirI18n(Map<String, String> publishDirI18n) {
		this.publishDirI18n = publishDirI18n;
	}
}
