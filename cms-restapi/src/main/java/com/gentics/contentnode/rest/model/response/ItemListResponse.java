package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.gentics.contentnode.rest.model.ContentNodeItem;

/**
 * Response for a item list request 
 * @author Clemens
 */
@XmlRootElement
public class ItemListResponse extends GenericResponse {
	private List<? extends ContentNodeItem> items;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor needed by JAXB
	 */
	public ItemListResponse() {}

	public ItemListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	@JsonTypeInfo(use=Id.CLASS)
	public List<? extends ContentNodeItem> getItems() {
		return items;
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
	 * NOTE: items won't be listed correctly until a setter is defined
	 * @param items
	 */
	@JsonTypeInfo(use=Id.CLASS)
	public void setItems(List<? extends ContentNodeItem> items) {
		this.items = items;
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
