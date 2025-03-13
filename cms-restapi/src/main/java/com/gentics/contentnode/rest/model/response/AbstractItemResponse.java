package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Abstract item response
 *
 * @param <T> item type
 */
@XmlRootElement
public class AbstractItemResponse <T extends Object> extends GenericResponse {
	/**
	 * Serial Version UId
	 */
	private static final long serialVersionUID = -1415634035461255976L;

	/**
	 * Item
	 */
	private T item;

	/**
	 * Create empty instance
	 */
	public AbstractItemResponse() {
	}

	/**
	 * Create instance with response info and item
	 * @param item item
	 * @param responseInfo response info
	 */
	public AbstractItemResponse(T item, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setItem(item);
	}

	/**
	 * Item contained in the response
	 * @return item
	 */
	public T getItem() {
		return item;
	}

	/**
	 * Set the item
	 * @param item item
	 */
	public void setItem(T item) {
		this.item = item;
	}
}
