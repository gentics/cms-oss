package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of ContentRepository Entries
 */
@XmlRootElement
public class TagmapEntryListResponse extends AbstractListResponse<TagmapEntryModel> {
}
