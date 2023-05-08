package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Construct;

/**
 * Response containing a list of constructs
 */
@XmlRootElement
public class ConstructListResponse extends GenericResponse {

	private static final long serialVersionUID = 4631758890136807432L;

	/**
	 * List of Constructs
	 */
	private List<Construct> constructs;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Create an empty instance
	 */
	public ConstructListResponse() {}

	/**
	 * Create instance with message and responseinfo
	 * @param message message
	 * @param responseInfo responseinfo
	 */
	public ConstructListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the constructs
	 * @return constructs
	 */
	public List<Construct> getConstructs() {
		return constructs;
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
	 * Set the constructs
	 * @param constructs cons
	 */
	public void setConstructs(List<Construct> constructs) {
		this.constructs = constructs;
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
