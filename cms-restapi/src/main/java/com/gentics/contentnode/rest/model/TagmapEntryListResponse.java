package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

import java.util.List;

/**
 * Response containing a list of ContentRepository Entries
 */
@XmlRootElement
public class TagmapEntryListResponse extends AbstractListResponse<TagmapEntryModel> {

    /**
     * Tagmap entries in the list
     * @return List of tagmap entries
     */
    @Override
    public List<TagmapEntryModel> getItems() {
        return super.getItems();
    }

    /**
     * Set the entries
     * @param items list of tagmap entries
     */
    @Override
    public void setItems(List<TagmapEntryModel> items) {
        super.setItems(items);
    }
}
