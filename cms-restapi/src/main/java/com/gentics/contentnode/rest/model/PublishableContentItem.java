package com.gentics.contentnode.rest.model;

import java.io.Serializable;

/**
 * Base class for publishable objects
 */
public abstract class PublishableContentItem extends ContentNodeItem implements Serializable {

	private static final long serialVersionUID = 57907188582401112L;


	/**
	 * Last publisher of this page
	 */
	private User publisher;

	/**
	 * Future (i.e. timemanagment) publisher of this page
	 */
	private User futurePublisher;


	/**
	 * Date when this page was published the last time
	 */
	private int pdate;

	/**
	 * The date when a page was unpublished
	 */
	private Integer unpublishedDate;


	/**
	 * The user that took the content item offline
	 */
	private User unpublisher;

		/**y
	 * The user that planned the takeOffline action
	 */
	private User futureUnpublisher;


	public PublishableContentItem() {
	}


	public PublishableContentItem(ItemType itemType) {
		setType(itemType);
	}

	/**
	 * Publisher
	 *
	 * @return the publisher
	 */
	public User getPublisher() {
		return this.publisher;
	}

	/**
	 * @param publisher the publisher to set
	 */
	public void setPublisher(User publisher) {
		this.publisher = publisher;
	}

	/**
	 * Future Publisher
	 *
	 * @return the future publisher
	 */
	public User getFuturePublisher() {
		return this.futurePublisher;
	}

	/**
	 * @param futurePublisher the future publisher to set
	 */
	public void setFuturePublisher(User futurePublisher) {
		this.futurePublisher = futurePublisher;
	}


	/**
	 * Publish Date
	 *
	 * @return the pdate
	 */
	public int getPdate() {
		return this.pdate;
	}


	/**
	 * @param pdate the pdate to set
	 */
	public void setPdate(int pdate) {
		this.pdate = pdate;
	}


	/**
	 * Gets the unpublished date.
	 *
	 * @return the unpublished date as an Integer
	 */
	public Integer getUnpublishedDate() {
		return unpublishedDate;
	}

	/**
	 * Sets the unpublished date.
	 *
	 * @param unpublishedDate the unpublished date to set
	 */
	public void setUnpublishedDate(Integer unpublishedDate) {
		this.unpublishedDate = unpublishedDate;
	}

	/**
	 * Gets the unpublisher.
	 *
	 * @return the unpublisher
	 */
	public User getUnpublisher() {
		return unpublisher;
	}

	/**
	 * Sets the unpublisher.
	 *
	 * @param unpublisher the unpublisher to set
	 */
	public void setUnpublisher(User unpublisher) {
		this.unpublisher = unpublisher;
	}

	/**
	 * Gets the user that set the unpublish action.
	 *
	 * @return the future unpublisher
	 */
	public User getFutureUnpublisher() {
		return futureUnpublisher;
	}

	/**
	 * Set the future unpublisher
	 */
	public void setFutureUnpublisher(User futureUnpublisher) {
		this.futureUnpublisher = futureUnpublisher;
	}

}
