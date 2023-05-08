package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * Response for a folder usage request.
 */
@XmlRootElement
public class FolderUsageListResponse extends LegacyFolderListResponse {

	/**
	 * Number of using folders, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of using folders
	 */
	private int total = 0;

	/**
	 * Empty constructor
	 */
	public FolderUsageListResponse() {}

	/**
	 * Create a new instance
	 * @param message message
	 * @param responseInfo response info
	 * @param folders folders
	 * @param total total number of folders
	 * @param withoutPermission number of folders without permission
	 */
	public FolderUsageListResponse(Message message, ResponseInfo responseInfo, List<Folder> folders, int total, int withoutPermission) {
		super(message, responseInfo);
		setFolders(folders);
		setTotal(total);
		setWithoutPermission(withoutPermission);
	}

	/**
	 * Get the number of folders without permission
	 * @return number of folders without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Get the total number of folders
	 * @return total number of folders
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the number of folders without permission
	 * @param withoutPermission number of folders without permission
	 */
	public void setWithoutPermission(int withoutPermission) {
		this.withoutPermission = withoutPermission;
	}

	/**
	 * Set the total number of folders
	 * @param total total number of folders
	 */
	public void setTotal(int total) {
		this.total = total;
	}

}
