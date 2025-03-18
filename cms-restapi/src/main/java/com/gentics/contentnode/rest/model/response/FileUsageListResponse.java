package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Response for a file/image usage request.
 */
@XmlRootElement
public class FileUsageListResponse extends LegacyFileListResponse {

	/**
	 * Number of using files, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of using files
	 */
	private int total = 0;

	/**
	 * Empty constructor
	 */
	public FileUsageListResponse() {}

	/**
	 * Create a new instance
	 * @param message message
	 * @param responseInfo response info
	 * @param files files
	 * @param total total number of files
	 * @param withoutPermission number of files without permission
	 */
	public FileUsageListResponse(Message message, ResponseInfo responseInfo, List<File> files, int total, int withoutPermission) {
		super(message, responseInfo);
		setFiles(files);
		setTotal(total);
		setWithoutPermission(withoutPermission);
	}

	/**
	 * Get the number of files without permission
	 * @return number of files without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Get the total number of files
	 * @return total number of files
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the number of files without permission
	 * @param withoutPermission number of files without permission
	 */
	public void setWithoutPermission(int withoutPermission) {
		this.withoutPermission = withoutPermission;
	}

	/**
	 * Set the total number of files
	 * @param total total number of files
	 */
	public void setTotal(int total) {
		this.total = total;
	}
}
