package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Request to push a specified object from a channel into a master
 */
@XmlRootElement
public class PushToMasterRequest implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2168950478739376485L;

	/**
	 * Id of the master
	 */
	private int masterId;

	/**
	 * Id of the channel
	 */
	private int channelId;

	/**
	 * True if pushing shall be done recursively, false if not
	 */
	private Boolean recursive;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * List of types to be pushed (if the pushed object is a folder). If not given, no subobjects will be pushed
	 */
	private List<Type> types;

	/**
	 * Empty constructor
	 */
	public PushToMasterRequest() {}

	public int getMasterId() {
		return masterId;
	}

	public void setMasterId(int masterId) {
		this.masterId = masterId;
	}

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	public Integer getForegroundTime() {
		return foregroundTime;
	}

	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}

	public List<Type> getTypes() {
		return types;
	}

	public void setTypes(List<Type> types) {
		this.types = types;
	}

	/**
	 * Enumeration for pushed subtypes
	 */
	@XmlType(name = "PushToMasterType")
	public static enum Type {
		page, template, image, file, folder
	}
}
