package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a sanitized folder publish directory
 */
@XmlRootElement
public class FolderPublishDirSanitizeResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 904149088614424172L;

	private String publishDir;

	/**
	 * Create empty instance
	 */
	public FolderPublishDirSanitizeResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public FolderPublishDirSanitizeResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Sanitized publish directory
	 * @return publish directory
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * Set publish directory
	 * @param publishDir publish directory
	 * @return fluent API
	 */
	public FolderPublishDirSanitizeResponse setPublishDir(String publishDir) {
		this.publishDir = publishDir;
		return this;
	}
}
