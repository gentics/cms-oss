package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.Folder;

import java.util.List;

/**
 * Response for a folder list request.
 */
public class FolderListResponse extends AbstractListResponse<Folder> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7299936179419707056L;

	/**
	 * List of folder ids (or [nodeId/folderId]), which do not exist on the
	 * backend (at least not visibly for the user), but were requested to be
	 * "opened"
	 */
	private List<String> deleted;

	public FolderListResponse() {
	}

	public FolderListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * List of folderIds (or [nodeId/folderId]s), which were requested to be
	 * opened (when getting folder structures), but do not exist in the backend
	 * (at least not visible for the user)
	 * @return list of folder ids
	 */
	public List<String> getDeleted() {
		return deleted;
	}

	/**
	 * Set the list of folder ids, that do not exist in the backend
	 * @param deleted list of folder ids
	 */
	public FolderListResponse setDeleted(List<String> deleted) {
		this.deleted = deleted;
		return this;
	}
}
