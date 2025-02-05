package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;

/**
 * Response containing a ContentRepository Fragment Entry
 */
@XmlRootElement
public class ContentRepositoryFragmentEntryResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4289919103148025877L;

	private ContentRepositoryFragmentEntryModel entry;

	/**
	 * Create empty instance
	 */
	public ContentRepositoryFragmentEntryResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public ContentRepositoryFragmentEntryResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and entry
	 * @param entry entry
	 * @param responseInfo response info
	 */
	public ContentRepositoryFragmentEntryResponse(ContentRepositoryFragmentEntryModel entry, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setEntry(entry);
	}

	/**
	 * ContentRepository Fragment Entry
	 * @return entry
	 */
	public ContentRepositoryFragmentEntryModel getEntry() {
		return entry;
	}

	/**
	 * Set the entry
	 * @param entry entry
	 */
	public void setEntry(ContentRepositoryFragmentEntryModel entry) {
		this.entry = entry;
	}
}
