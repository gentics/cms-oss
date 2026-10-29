package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Request to unlocalize an object
 */
@XmlRootElement
public class UnlocalizeRequest implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6272344062821689222L;

	/**
	 * Id of the channel
	 */
	private int channelId;

	/**
	 * True of unlocalizing shall be done recursively, false if not
	 */
	private Boolean recursive;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * List of types to be unlocalized (if the unlocalized object is a folder). If not given, no subobjects will be unlocalized
	 */
	private List<Type> types;

	/**
	 * Create an empty instance
	 */
	public UnlocalizeRequest() {}

	/**
	 * Copy constructor.
	 *
	 * @param other The request to copy.
	 */
	public UnlocalizeRequest(UnlocalizeRequest other) {
		channelId = other.channelId;
		recursive = other.recursive;
		foregroundTime = other.foregroundTime;
		if (other.types != null) {
			types = new ArrayList<Type>(other.types);
		} else {
			types = Collections.emptyList();
		}
	}

	public int getChannelId() {
		return channelId;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
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
	 * Enumeration for unlocalized subtypes
	 */
	@XmlType(name = "UnlocalizeType")
	public static enum Type {
		page, template, image, file, folder
	}
}
