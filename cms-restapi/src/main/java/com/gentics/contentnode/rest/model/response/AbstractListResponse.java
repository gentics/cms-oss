package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.request.Permission;

/**
 * Abstract list response
 *
 * @param <T> type of the objects contained in the list
 */
@XmlRootElement
public abstract class AbstractListResponse <T extends Object> extends StagingResponse<String> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Items
	 */
	private List<T> items;

	/**
	 * True if more items are available (paging)
	 */
	private boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private int numItems;

	/**
	 * Permissions map
	 */
	private Map<Integer, Set<Permission>> perms;

	/**
	 * Empty constructor needed by JAXB
	 */
	public AbstractListResponse() {}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public AbstractListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Items in the list
	 * @return list of items
	 */
	public List<T> getItems() {
		return items;
	}

	/**
	 * Set the list of items
	 * @param items list of items
	 */
	public void setItems(List<T> items) {
		this.items = items;
	}

	/**
	 * True if more items are available to get (if paging was used)
	 * @return true for more items
	 */
	public boolean getHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Set whether more items are available
	 * @param hasMoreItems true for more items
	 */
	public void setHasMoreItems(boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Get total number of items available
	 * @return total number of items available
	 */
	public int getNumItems() {
		return numItems;
	}

	/**
	 * Set total number of items available
	 * @param numItems total number of items
	 */
	public void setNumItems(int numItems) {
		this.numItems = numItems;
	}

	/**
	 * User permissions on the returned items, if applicable and requested
	 * @return map of permissions
	 */
	public Map<Integer, Set<Permission>> getPerms() {
		return perms;
	}

	/**
	 * Set user permissions
	 * @param perms permissions
	 */
	public void setPerms(Map<Integer, Set<Permission>> perms) {
		this.perms = perms;
	}
}
