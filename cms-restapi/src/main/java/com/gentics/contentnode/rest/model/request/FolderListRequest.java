package com.gentics.contentnode.rest.model.request;

import java.util.List;

import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request object for a request to list folders
 * @author najor
 */

@XmlRootElement
public class FolderListRequest {

	private String id;
	private Integer nodeId;
	private int skipCount = 0;
	private int maxItems = -1;
	private boolean recursive = false;
	private String sortBy = "name";
	private String sortOrder = "asc";
	private Boolean inherited;
	private String search;
	private String editor;
	private String creator;
	private int editedBefore = 0;
	private int editedSince = 0;
	private int createdBefore = 0;
	private int createdSince = 0;
	private boolean tree = false;
	private List<String> recursiveIds;
	private boolean addPrivileges = false;
	private boolean privilegeMap = false;
	private WastebinSearch wastebin = WastebinSearch.exclude;
	@QueryParam("package")
	private String stagingPackageName;

	/**
	 * @param id
	 * @param addPrivileges
	 */
	public FolderListRequest() {
	}

	/**
	 * Get id
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * Set id
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * Get nodeId
	 * @return the nodeId
	 */
	public Integer getNodeId() {
		return nodeId;
	}
	/**
	 * Set nodeId
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}
	/**
	 * Get skipCount
	 * @return the skipCount
	 */
	public int getSkipCount() {
		return skipCount;
	}
	/**
	 * Set skipCount
	 * @param skipCount the skipCount to set
	 */
	public void setSkipCount(int skipCount) {
		this.skipCount = skipCount;
	}
	/**
	 * Get maxItems
	 * @return the maxItems
	 */
	public int getMaxItems() {
		return maxItems;
	}
	/**
	 * Set maxItems
	 * @param maxItems the maxItems to set
	 */
	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
	/**
	 * Get recursive
	 * @return the recursive
	 */
	public boolean isRecursive() {
		return recursive;
	}
	/**
	 * Set recursive
	 * @param recursive the recursive to set
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	/**
	 * Get sortBy
	 * @return the sortBy
	 */
	public String getSortBy() {
		return sortBy;
	}
	/**
	 * Set sortBy
	 * @param sortBy the sortBy to set
	 */
	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}
	/**
	 * Get sortOrder
	 * @return the sortOrder
	 */
	public String getSortOrder() {
		return sortOrder;
	}
	/**
	 * Set sortOrder
	 * @param sortOrder the sortOrder to set
	 */
	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
	/**
	 * Get inherited
	 * @return the inherited
	 */
	public Boolean getInherited() {
		return inherited;
	}
	/**
	 * Set inherited
	 * @param inherited the inherited to set
	 */
	public void setInherited(Boolean inherited) {
		this.inherited = inherited;
	}
	/**
	 * Get search
	 * @return the search
	 */
	public String getSearch() {
		return search;
	}
	/**
	 * Set search
	 * @param search the search to set
	 */
	public void setSearch(String search) {
		this.search = search;
	}
	/**
	 * Get editor
	 * @return the editor
	 */
	public String getEditor() {
		return editor;
	}
	/**
	 * Set editor
	 * @param editor the editor to set
	 */
	public void setEditor(String editor) {
		this.editor = editor;
	}
	/**
	 * Get creator
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}
	/**
	 * Set creator
	 * @param creator the creator to set
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}
	/**
	 * Get editedBefore
	 * @return the editedBefore
	 */
	public int getEditedBefore() {
		return editedBefore;
	}

	/**
	 * Set editedBefore
	 * @param editedBefore the editedBefore to set
	 */
	public void setEditedBefore(int editedBefore) {
		this.editedBefore = editedBefore;
	}

	/**
	 * Get editedSince
	 * @return the editedSince
	 */
	public int getEditedSince() {
		return editedSince;
	}
	/**
	 * Set editedSince
	 * @param editedSince the editedSince to set
	 */
	public void setEditedSince(int editedSince) {
		this.editedSince = editedSince;
	}

	/**
	 * Get createdBefore
	 * @return the createdBefore
	 */
	public int getCreatedBefore() {
		return createdBefore;
	}

	/**
	 * Set createdBefore
	 * @param createdBefore the createdBefore to set
	 */
	public void setCreatedBefore(int createdBefore) {
		this.createdBefore = createdBefore;
	}

	/**
	 * Get createdSince
	 * @return the createdSince
	 */
	public int getCreatedSince() {
		return createdSince;
	}
	/**
	 * Set createdSince
	 * @param createdSince the createdSince to set
	 */
	public void setCreatedSince(int createdSince) {
		this.createdSince = createdSince;
	}
	/**
	 * Get tree
	 * @return the tree
	 */
	public boolean isTree() {
		return tree;
	}
	/**
	 * Set tree
	 * @param tree the tree to set
	 */
	public void setTree(boolean tree) {
		this.tree = tree;
	}
	/**
	 * Get recursiveIds
	 * @return the recursiveIds
	 */
	public List<String> getRecursiveIds() {
		return recursiveIds;
	}
	/**
	 * Set recursiveIds
	 * @param recursiveIds the recursiveIds to set
	 */
	public void setRecursiveIds(List<String> recursiveIds) {
		this.recursiveIds = recursiveIds;
	}
	/**
	 * Get addPrivileges
	 * @return the addPrivileges
	 */
	public boolean isAddPrivileges() {
		return addPrivileges;
	}
	/**
	 * Set addPrivileges
	 * @param addPrivileges the addPrivileges to set
	 */
	public void setAddPrivileges(boolean addPrivileges) {
		this.addPrivileges = addPrivileges;
	}

	/**
	 * True for adding privilege maps to the folders
	 * @return true to add privilege maps
	 */
	public boolean isPrivilegeMap() {
		return privilegeMap;
	}

	/**
	 * Set true to add privilege maps
	 * @param privilegeMap
	 */
	public void setPrivilegeMap(boolean privilegeMap) {
		this.privilegeMap = privilegeMap;
	}

	/**
	 * exclude (default) to exclude deleted objects, include to include deleted objects, only to return only deleted objects
	 * @return wastebin search option
	 */
	public WastebinSearch getWastebin() {
		return wastebin;
	}

	/**
	 * Set the wastebin search option
	 * @param wastebin wastebin search option
	 */
	public void setWastebin(WastebinSearch wastebin) {
		this.wastebin = wastebin;
	}

	/**
	 * Get staging package name to check the folder staging status upon
	 * @return
	 */
	public String getStagingPackageName() {
		return stagingPackageName;
	}

	/**
	 * Set staging package name to check the folder staging status upon
	 * @param stagingPackageName
	 */
	public void setStagingPackageName(String stagingPackageName) {
		this.stagingPackageName = stagingPackageName;
	}
}
