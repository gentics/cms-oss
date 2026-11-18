package com.gentics.contentnode.rest.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of files of a Portal Implementation
 */
@XmlRootElement
public class PortalImplementationFileListResponse extends AbstractListResponse<PortalImplementationFileModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2287914415734101238L;

	/**
	 * Files in the list
	 * @return list of files
	 */
	@Override
	public List<PortalImplementationFileModel> getItems() {
		return super.getItems();
	}

	/**
	 * Set the files
	 * @param items list of files
	 */
	@Override
	public void setItems(List<PortalImplementationFileModel> items) {
		super.setItems(items);
	}
}
