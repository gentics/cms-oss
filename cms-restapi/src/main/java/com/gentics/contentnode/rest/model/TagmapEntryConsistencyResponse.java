package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of inconsistencies
 */
@XmlRootElement
public class TagmapEntryConsistencyResponse extends AbstractListResponse<TagmapEntryInconsistencyModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3839873698125930213L;
}
