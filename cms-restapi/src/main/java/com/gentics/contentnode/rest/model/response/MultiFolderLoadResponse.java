package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * Response containing information about multiple folders.
 */
@XmlRootElement
public class MultiFolderLoadResponse extends AbstractStagingResponse<String> {

	private static final long serialVersionUID = -7521219058301429875L;

	/**
	 * The list of found folders.
	 */
	private List<Folder> folders;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiFolderLoadResponse() {
	}

	/**
	 * Convenience constructor.
	 *
	 * Automatically adds a response info with a response code
	 * of <code>OK</code>.
	 *
	 * @param folders The folders the send with the response.
	 */
	public MultiFolderLoadResponse(List<Folder> folders) {
		super(
			null,
			new ResponseInfo(
				ResponseCode.OK,
				(folders == null ? 0 : folders.size()) + " folder(s) loaded"));

		this.folders = folders;
	}

	/**
	 * Response with an empty folder list.
	 *
	 * @param message The message that should be displayed to the user.
	 * @param response ResponseInfo with the status of the response.
	 */
	public MultiFolderLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);

		this.folders = new ArrayList<>();
	}

	/**
	 * The list of found folders.
	 *
	 * @return The list of found folders.
	 */
	public List<Folder> getFolders() {
		return folders;
	}

	/**
	 * Set the list of folders to send with the response.
	 *
	 * @param pages The list of folders to send with the response.
	 */
	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}
}
