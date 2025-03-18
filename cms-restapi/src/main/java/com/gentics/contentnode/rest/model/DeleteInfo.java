package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeleteInfo implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2572889667023162760L;

	/**
	 * Date when the item was deleted
	 */
	private int at;

	/**
	 * User who deleted the object
	 */
	private User by;

	/**
	 * Empty constructor
	 */
	public DeleteInfo() {
	}

	/**
	 * Create an instance
	 * @param at deleted at
	 * @param by deleted by
	 */
	public DeleteInfo(int at, User by) {
		this.at = at;
		this.by = by;
	}

	/**
	 * Date when the object was deleted
	 * @return date of deletion
	 */
	public int getAt() {
		return at;
	}

	/**
	 * Set deletion date
	 * @param at deletion date
	 */
	public void setAt(int at) {
		this.at = at;
	}

	/**
	 * User who deleted the object
	 * @return user
	 */
	public User getBy() {
		return by;
	}

	/**
	 * Set user who deleted the object
	 * @param by user
	 */
	public void setBy(User by) {
		this.by = by;
	}
}
