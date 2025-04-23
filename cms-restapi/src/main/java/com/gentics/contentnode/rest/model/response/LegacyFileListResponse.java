/*
 * @author Clemens
 * @date Oct 6, 2010
 * @version $Id: FileListResponse.java,v 1.1.2.2.2.1 2011-03-15 14:02:03 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.gentics.contentnode.rest.model.File;

/**
 * Response for a page list request
 * @author Clemens
 */
@XmlRootElement
public class LegacyFileListResponse extends AbstractStagingResponse<String> {

	private static final long serialVersionUID = -7014356589262104642L;

	/**
	 * Files in the filelist response
	 */
	private List<File> files;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor needed by JAXB
	 */
	public LegacyFileListResponse() {}

	/**
	 * Create an instance with given message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public LegacyFileListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the files
	 * @return files
	 */
	@JsonTypeInfo(use=Id.CLASS)
	public List<File> getFiles() {
		return files;
	}

	/**
	 * True if more items are available (paging)
	 * @return true if more items are present
	 */
	public Boolean isHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Get total number of items present
	 * @return total number of items present
	 */
	public Integer getNumItems() {
		return numItems;
	}

	/**
	 * Set the files
	 * @param files
	 */
	@JsonTypeInfo(use=Id.CLASS)
	public void setFiles(List<File> files) {
		this.files = files;
	}

	/**
	 * Set true when more items are available
	 * @param hasMoreItems true if more items are available
	 */
	public void setHasMoreItems(Boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Set the total number of items present
	 * @param numItems total number of items present
	 */
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}
}
