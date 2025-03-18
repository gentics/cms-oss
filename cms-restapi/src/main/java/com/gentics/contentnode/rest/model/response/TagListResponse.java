package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Tag;

/**
 * Response with a list of tags
 */
@XmlRootElement
public class TagListResponse extends GenericResponse {

	/**
	 * List of tags
	 */
	private List<Tag> tags;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor
	 */
	public TagListResponse() {}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public TagListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get tags list
	 * @return tags
	 */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * True if more items are available (paging)
	 * @return true if more items are present
	 */
	public Boolean isHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Get total number of items present
	 * @return total number of items present
	 */
	public Integer getNumItems() {
		return numItems;
	}

	/**
	 * Set tags list
	 * @param tags tags
	 */
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * Set true when more items are available
	 * @param hasMoreItems true if more items are available
	 */
	public void setHasMoreItems(Boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Set the total number of items present
	 * @param numItems total number of items present
	 */
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}
}
