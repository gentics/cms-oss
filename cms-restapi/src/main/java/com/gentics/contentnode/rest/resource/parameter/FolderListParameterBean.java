package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameter bean for getting subfolders of a folder.
 */
public class FolderListParameterBean {

	/**
	 * Node ID of the channel when used in multichannelling.
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * true to only return inherited pages in the given node, false to only get
	 * local/localized pages, null to get local and inherited pages.
	 */
	@QueryParam("inherited")
	public Boolean inherited;

	/**
	 * Whether folders shall be returned as tree(s). Subfolders will be attached
	 * to their mothers. This only makes sense, when recursive is true.
	 */
	@QueryParam("tree")
	@DefaultValue("false")
	public boolean tree = false;

	/**
	 * Optional list of folder ids, for which the children shall be fetched
	 * (ignored if recursive is false). The ids might be composed as
	 * nodeId/folderId to get children for folders in specific channels only.
	 */
	@QueryParam("recursiveIds")
	public List<String> recursiveIds;

	/**
	 * Whether the privileges shall be added to the folders.
	 */
	@QueryParam("addPrivileges")
	@DefaultValue("false")
	public boolean addPrivileges = false;

	/**
	 * Whether the privileges shall be added to the folders as map.
	 */
	@QueryParam("privilegeMap")
	@DefaultValue("false")
	public boolean privilegeMap = false;

	public FolderListParameterBean setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public FolderListParameterBean setInherited(Boolean inherited) {
		this.inherited = inherited;
		return this;
	}

	public FolderListParameterBean setTree(boolean tree) {
		this.tree = tree;
		return this;
	}

	public FolderListParameterBean setRecursiveIds(List<String> recursiveIds) {
		this.recursiveIds = recursiveIds;
		return this;
	}

	public FolderListParameterBean setAddPrivileges(boolean addPrivileges) {
		this.addPrivileges = addPrivileges;
		return this;
	}

	public FolderListParameterBean setPrivilegeMap(boolean privilegeMap) {
		this.privilegeMap = privilegeMap;
		return this;
	}

	/**
	 * Create a clone of this parameter bean.
	 * @return Create a clone of this parameter bean
	 */
	public FolderListParameterBean clone() {
		return new FolderListParameterBean()
			.setNodeId(nodeId)
			.setInherited(inherited)
			.setTree(tree)
			.setRecursiveIds(recursiveIds == null ? null : new ArrayList<>(recursiveIds))
			.setAddPrivileges(addPrivileges)
			.setPrivilegeMap(privilegeMap);
	}
}
