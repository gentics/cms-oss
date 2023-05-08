/*
 * @author johannes2
 * @date Sep 24, 2010
 * @version $Id: FolderNavigationObjectRequest.java,v 1.1 2010-09-27 08:22:19 johannes2 Exp $
 */
package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class will be used to map FolderNavigationObject requests to java
 * @author johannes2
 *
 */
@XmlRootElement
public class FolderNavigationObjectRequest {

	/**
	 * Requested folderId
	 */
	private Integer folderId;
    
	/**
	 * Constructor used by JAXB
	 */
	public FolderNavigationObjectRequest() {}

	public Integer getFolderId() {
		return folderId;
	}

	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}
    
}
