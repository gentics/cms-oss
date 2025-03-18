package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of ContentRepository Fragment Entries
 */
@XmlRootElement
public class ContentRepositoryFragmentEntryListResponse extends AbstractListResponse<ContentRepositoryFragmentEntryModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8772960415320623731L;
}
