package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * Parameter bean for filtering publishable objects
 */
public class PublishableParameterBean extends EditableParameterBean {
	/**
	 * (optional) true when only the objects which were last published by the user
	 * shall be returned
	 */
	@QueryParam("ispublisher")
	@DefaultValue("false")
	public boolean isPublisher = false;

	/**
	 * Pattern for restricting objects by publisher
	 */
	@QueryParam("publisher")
	public String publisher;

	/**
	 * IDs for restricting objects by publisher
	 */
	@QueryParam("publisherId")
	public List<Integer> publisherIds;

	/**
	 * Timestamp to search objects, which were published before a given time (0 for all objects)
	 */
	@QueryParam("publishedbefore")
	@DefaultValue("0")
	public int publishedBefore = 0;

	/**
	 * Timestamp to search objects, which were published since a given time (0 for all objects)
	 */
	@QueryParam("publishedsince")
	@DefaultValue("0")
	public int publishedSince = 0;

	/**
	 * (optional) true to restrict to modified objects, false to restrict to unmodified objects
	 */
	@QueryParam("modified")
	public Boolean modified;

	/**
	 * (optional) true to restrict to online objects, false to restrict to offline objects
	 */
	@QueryParam("online")
	public Boolean online;


	public PublishableParameterBean setPublisher(boolean publisher) {
		isPublisher = publisher;
		return this;
	}

	public PublishableParameterBean setPublisher(String publisher) {
		this.publisher = publisher;
		return this;
	}

	public PublishableParameterBean setPublisherIds(List<Integer> publisherIds) {
		this.publisherIds = publisherIds;
		return this;
	}

	public PublishableParameterBean setPublishedBefore(int publishedBefore) {
		this.publishedBefore = publishedBefore;
		return this;
	}

	public PublishableParameterBean setPublishedSince(int publishedSince) {
		this.publishedSince = publishedSince;
		return this;
	}

	public PublishableParameterBean setModified(Boolean modified) {
		this.modified = modified;
		return this;
	}

	public PublishableParameterBean setOnline(Boolean online) {
		this.online = online;
		return this;
	}

}
