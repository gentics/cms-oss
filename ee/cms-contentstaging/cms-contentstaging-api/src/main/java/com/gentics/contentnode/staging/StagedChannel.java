package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST entity for channel info for the Content Staging package object.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedChannel extends StagedNode {

	public static final String TYPE_NAME = "channel";

	private static final long serialVersionUID = -5069880653613236301L;

	private String masterNodeId;

	/**
	 * Get the ID of the (direct) master node
	 * @return master node ID
	 */
	public String getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the ID of the direct master node
	 * @param masterNodeId master node ID
	 */
	public void setMasterNodeId(String masterNodeId) {
		this.masterNodeId = masterNodeId;
	}

	/**
	 * Clone the origin into this entity.
	 * 
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
    public void clone(StagedChannel origin) {
		super.clone(origin);
		this.masterNodeId = origin.masterNodeId;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return TYPE_NAME;
	}
}
