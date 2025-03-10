/*
 * @author johannes2
 * @date Sep 8, 2010
 * @version $Id: FileUploadResponse.java,v 1.1.6.1 2011-03-23 14:06:23 johannes2 Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

@XmlRootElement
public class FileUploadResponse extends GenericResponse {

	/**
	 * Used for fileuploader.js response identification
	 */
	private boolean success;
    
	/**
	 * File contained in the response
	 */
	private File file;

	/**
	 * Empty Constructor
	 */
	public FileUploadResponse() {}
    
	/**
	 * Constructor with message, response info and success 
	 * @param message message
	 * @param responseInfo response info
	 * @param file file meta
	 */
	public FileUploadResponse(Message message, ResponseInfo responseInfo, boolean success, File file) {
		super(message, responseInfo);
		this.success = success;
		this.file = file;
	}
    
	/**
	 * Constructor with message, response info and success 
	 * @param message message
	 * @param responseInfo response info
	 */
	public FileUploadResponse(Message message, ResponseInfo responseInfo, boolean success) {
		super(message, responseInfo);
		this.success = success;

	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;   
	}
}
