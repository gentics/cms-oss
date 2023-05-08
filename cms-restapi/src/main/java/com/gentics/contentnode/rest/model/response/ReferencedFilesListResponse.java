package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Response for a list of referenced files or images
 */
@XmlRootElement
public class ReferencedFilesListResponse extends LegacyFileListResponse {
	/**
	 * Number of referenced files, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of referenced files
	 */
	private int total = 0;

	/**
	 * Create an instance
	 * @param message
	 * @param responseInfo
	 * @param files
	 * @param total
	 * @param withoutPermission
	 */
	public ReferencedFilesListResponse(Message message, ResponseInfo responseInfo, List<File> files, int total, int withoutPermission) {
		super(message, responseInfo);
		setFiles(files);
		setTotal(total);
		setWithoutPermission(withoutPermission);

	}

	/**
	 * Number of files without permission
	 * @return number of files without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Total number of files
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
