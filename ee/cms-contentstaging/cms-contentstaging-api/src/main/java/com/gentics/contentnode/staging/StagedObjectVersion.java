package com.gentics.contentnode.staging;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A version for the version-enabled entity, for the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedObjectVersion implements Serializable {

	private static final long serialVersionUID = 7091408498561010481L;

	private String versionNumber;
	private StagedObjectVersionType type;
	private int timestamp;
	private int publishAt;
	private Boolean online;
	private Integer timeOff;

	/**
	 * Clone the origin into this version.
	 * 
	 * @param origin
	 */
	public void clone(StagedObjectVersion origin) {
		this.versionNumber = origin.versionNumber;
		this.type = origin.type;
		this.timestamp = origin.timestamp;
		this.publishAt = origin.publishAt;
		this.online = origin.online;
		this.timeOff = origin.timeOff;
	}

	/**
	 * Get version timestamp in seconds.
	 * 
	 * @return
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Set version timestamp in seconds.
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get version number.
	 * 
	 * @return
	 */
	public String getVersionNumber() {
		return versionNumber;
	}

	/**
	 * Set version number.
	 * 
	 * @param versionNumber
	 */
	public void setVersionNumber(String versionNumber) {
		this.versionNumber = versionNumber;
	}

	public StagedObjectVersionType getType() {
		return type;
	}

	public void setType(StagedObjectVersionType type) {
		this.type = type;
	}

	public int getPublishAt() {
		return publishAt;
	}

	public void setPublishAt(int publishAt) {
		this.publishAt = publishAt;
	}

	public Boolean getOnline() {
		return online;
	}

	public void setOnline(Boolean online) {
		this.online = online;
	}

	public Integer getTimeOff() {
		return timeOff;
	}

	public void setTimeOff(Integer timeOff) {
		this.timeOff = timeOff;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().isInstance(obj)) {
			return false;
		}
		return getClass().cast(obj).getTimestamp() == timestamp;
	}

	@Override
	public int hashCode() {
		return timestamp;
	}

	/**
	 * Is this version major?
	 * 
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
    public boolean isMajor() {
		return this.versionNumber != null && this.versionNumber.endsWith(".0");
	}
}
