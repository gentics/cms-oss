package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to load multiple folders.
 */
@XmlRootElement
public class MultiFolderLoadRequest extends MultiObjectLoadRequest {

	/**
	 * Whether information about privileges should be included in
	 * the response.
	 */
	private Boolean addPrivileges = false;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiFolderLoadRequest() {
	}

	/**
	 * Indicates whether information about privileges should be included in
	 * the response.
	 *
	 * @return <code>true</code> if information about privileges should be
	 *		included in the response, <code>false</code> otherwise.
	 */
	public Boolean isAddPrivileges() {
		return addPrivileges;
	}

	/**
	 * Set whether information about privileges should be included in
	 * the response.
	 *
	 * @param addPrivileges Set to <code>true</code> if information about
	 *		privileges should be included in the response.
	 */
	public void setAddPrivileges(Boolean addPrivileges) {
		this.addPrivileges = addPrivileges;
	}
}
