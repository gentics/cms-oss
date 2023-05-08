package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.TagmapEntryModel;

/**
 * Response containing a tagmap entry
 */
@XmlRootElement
public class TagmapEntryResponse extends GenericResponse {
	private TagmapEntryModel entry;

	/**
	 * Create empty instance
	 */
	public TagmapEntryResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public TagmapEntryResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and entry
	 * @param entry entry
	 * @param responseInfo response info
	 */
	public TagmapEntryResponse(TagmapEntryModel entry, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setEntry(entry);
	}

	/**
	 * Tagmap entry
	 * @return entry
	 */
	public TagmapEntryModel getEntry() {
		return entry;
	}

	/**
	 * Set the entry
	 * @param entry entry
	 */
	public void setEntry(TagmapEntryModel entry) {
		this.entry = entry;
	}
}
