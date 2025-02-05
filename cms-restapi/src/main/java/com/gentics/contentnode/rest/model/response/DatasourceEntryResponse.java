package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.DatasourceEntryModel;

/**
 * Response containing a datasource entry
 */
@XmlRootElement
public class DatasourceEntryResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6607080159935751601L;

	private DatasourceEntryModel entry;

	/**
	 * Create an empty instance
	 */
	public DatasourceEntryResponse() {
	}

	/**
	 * Create an instance with response info and entry
	 * @param info response info
	 * @param entry entry
	 */
	public DatasourceEntryResponse(ResponseInfo info, DatasourceEntryModel entry) {
		super(null, info);
		setEntry(entry);
	}

	/**
	 * Datasource entry
	 * @return entry
	 */
	public DatasourceEntryModel getEntry() {
		return entry;
	}

	/**
	 * Set the datasource entry
	 * @param entry entry
	 */
	public void setEntry(DatasourceEntryModel entry) {
		this.entry = entry;
	}
}
