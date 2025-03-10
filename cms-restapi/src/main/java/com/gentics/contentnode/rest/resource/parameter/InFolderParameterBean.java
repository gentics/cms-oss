package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for items "in folders"
 */
public class InFolderParameterBean {
	/**
	 * Folder ID
	 */
	@QueryParam("folderId")
	public String folderId;

	/**
	 * (optional) true when the items shall be fetched recursively, false if
	 * not. Defaults to false
	 */
	@QueryParam("recursive")
	@DefaultValue("false")
	public boolean recursive = false;

	@QueryParam("package")
	public String stagingPackageName;

	public InFolderParameterBean setStagingPackageName(String stagingPackageName) {
		this.stagingPackageName = stagingPackageName;
		return this;
	}

	public InFolderParameterBean setFolderId(String folderId) {
		this.folderId = folderId;
		return this;
	}

	public InFolderParameterBean setRecursive(boolean recursive) {
		this.recursive = recursive;
		return this;
	}

	/**
	 * Create a clone of this parameter bean.
	 * @return A clone of this parameter bean
	 */
	public InFolderParameterBean clone() {
		return new InFolderParameterBean()
			.setFolderId(folderId)
			.setRecursive(recursive);
	}
}
