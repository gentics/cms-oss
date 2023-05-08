/*
 * @author Christopher
 * Copied from the original PageListResponse by Clemens
 * @date Oct 6, 2010
 * @version $Id: PageListResponse.java,v 1.2 2010-10-12 09:53:20 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Response for a file list request 
 * @author Christopher
 */
@XmlRootElement
public class FoundFilesListResponse extends GenericResponse {
	private List<File> files;
    
	/**
	 * Empty constructor needed by JAXB
	 */
	public FoundFilesListResponse() {}
    
	public List<File> getFiles() {
		return files;
	}

	/**
	 * NOTE: files won't be listed correctly until a setter is defined
	 * @param files
	 */
	public void setFiles(List<File> files) {
		this.files = files;
	}
}
