package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Response containing information about multiple files.
 */
@XmlRootElement
public class MultiFileLoadResponse extends StagingResponse<String> {

	private static final long serialVersionUID = 7307279374399639967L;

	/**
	 * The list of found files.
	 */
	private List<File> files;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiFileLoadResponse() {
	}

	/**
	 * Convenience constructor.
	 *
	 * Automatically adds a response info with a response code
	 * of <code>OK</code>.
	 *
	 * @param files The files the send with the response.
	 */
	public MultiFileLoadResponse(List<File> files) {
		super(
			null,
			new ResponseInfo(
				ResponseCode.OK,
				(files == null ? 0 : files.size()) + " file(s) loaded"));

		this.files = files;
	}

	/**
	 * Response with an empty file list.
	 *
	 * @param message The message that should be displayed to the user.
	 * @param response ResponseInfo with the status of the response.
	 */
	public MultiFileLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);

		this.files = new ArrayList<>();
	}

	/**
	 * The list of found files.
	 *
	 * @return The list of found files.
	 */
	public List<File> getFiles() {
		return files;
	}

	/**
	 * Set the list of files to send with the response.
	 *
	 * @param pages The list of files to send with the response.
	 */
	public void setFiles(List<File> files) {
		this.files = files;
	}
}
