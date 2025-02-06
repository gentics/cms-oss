package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to localize an object
 */
@XmlRootElement
public class LocalizeRequest implements Serializable {
	
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2067356379395533407L;

	/**
	 * Id of the channel
	 */
	private int channelId;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * Create an empty instance
	 */
	public LocalizeRequest() {}

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public Integer getForegroundTime() {
		return foregroundTime;
	}

	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}

	/**
	 * Enumeration for unlocalized subtypes
	 */
	// @XmlType(name = "UnlocalizeType")
	// public static enum Type {
	// page, template, image, file, folder
	// }
}
