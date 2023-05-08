package com.gentics.contentnode.rest.model.request;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to move multiple folders
 */
@XmlRootElement
public class MultiFolderMoveRequest extends FolderMoveRequest {
	/**
	 * Folder IDs to move
	 */
	protected Set<String> ids;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiFolderMoveRequest() {
	}

	/**
	 * Folder IDs to move. The IDs may be internal or global IDs
	 * @return folder IDs
	 */
	public Set<String> getIds() {
		return ids;
	}

	/**
	 * Set the folder IDs
	 * @param ids
	 */
	public void setIds(Set<String> ids) {
		this.ids = ids;
	}
}
