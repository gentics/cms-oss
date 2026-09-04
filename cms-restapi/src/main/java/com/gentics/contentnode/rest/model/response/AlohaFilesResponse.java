package com.gentics.contentnode.rest.model.response;

import java.util.List;

/**
 * Response containing the files, which must be loaded for aloha editor
 */
public class AlohaFilesResponse extends GenericResponse {
	private static final long serialVersionUID = -8898697188212397841L;

	protected List<String> jsFiles;

	protected List<String> cssFiles;

	/**
	 * Create empty instance
	 */
	public AlohaFilesResponse() {
		super();
	}

	/**
	 * Create instance with message and response info
	 * @param message response message
	 * @param responseInfo response info
	 */
	public AlohaFilesResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * List of JS files for Aloha Editor
	 * @return list of files
	 */
	public List<String> getJsFiles() {
		return jsFiles;
	}

	/**
	 * Set the list of JS Files
	 * @param jsFiles JS Files
	 * @return fluent API
	 */
	public AlohaFilesResponse setJsFiles(List<String> jsFiles) {
		this.jsFiles = jsFiles;
		return this;
	}

	/**
	 * List of CSS Files for Aloha Editor
	 * @return list of files
	 */
	public List<String> getCssFiles() {
		return cssFiles;
	}

	/**
	 * Set the list of CSS files
	 * @param cssFiles CSS Files
	 * @return fluent API
	 */
	public AlohaFilesResponse setCssFiles(List<String> cssFiles) {
		this.cssFiles = cssFiles;
		return this;
	}
}
