package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Synchronization info
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjTagSyncStatus extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 196949816409311638L;

	private int inSync;

	private int outOfSync;

	private int unchecked;

	/**
	 * Create empty instance
	 */
	public ObjTagSyncStatus() {
	}

	/**
	 * Create instance with response info
	 * @param info info
	 */
	public ObjTagSyncStatus(ResponseInfo info) {
		super(null, info);
	}

	/**
	 * Number of object tags in sync
	 * @return number in sync
	 */
	public int getInSync() {
		return inSync;
	}

	/**
	 * Set number of object tags in sync
	 * @param inSync number
	 * @return fluent API
	 */
	public ObjTagSyncStatus setInSync(int inSync) {
		this.inSync = inSync;
		return this;
	}

	/**
	 * Number of object tags out of sync
	 * @return number out of sync
	 */
	public int getOutOfSync() {
		return outOfSync;
	}

	/**
	 * Set number of object tags out of sync
	 * @param outOfSync number
	 * @return fluent API
	 */
	public ObjTagSyncStatus setOutOfSync(int outOfSync) {
		this.outOfSync = outOfSync;
		return this;
	}

	/**
	 * Get number of unchecked object tags
	 * @return number unchecked
	 */
	public int getUnchecked() {
		return unchecked;
	}

	/**
	 * Set number of unchecked object tags
	 * @param unchecked number
	 * @return fluent API
	 */
	public ObjTagSyncStatus setUnchecked(int unchecked) {
		this.unchecked = unchecked;
		return this;
	}
}
