package com.gentics.contentnode.staging;

import java.util.Set;
import java.util.TreeSet;

import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract base class for staged localizable objects
 */
public abstract class StagedLocalizableObject extends StagingBase {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1888573801735002390L;

	/**
	 * ID of the master object, if object is localized
	 */
	private String masterId;

	/**
	 * Whether this object is excluded from multichannelling.
	 */
	private Boolean excluded;

	/**
	 * Whether this object is disinherited by default in new channels.
	 */
	private Boolean disinheritDefault;

	/**
	 * IDs of the channels, from which the object is disinherited
	 */
	private Set<String> disinheritedChannelIds;

	/**
	 * Whether this object is excluded from multichannelling.
	 *
	 * @return true iff the object should be excluded from multichannelling
	 */
	public Boolean getExcluded() {
		return excluded;
	}

	/**
	 * Set whether the object is excluded from multichannelling
	 *
	 * @param excluded
	 *            if true, the object will be excluded from multichannelling
	 */
	public void setExcluded(Boolean excluded) {
		this.excluded = excluded;
	}

	/**
	 * Whether this object is disinherited by default in new channels.
	 *
	 * @return <code>true</code> if the object is disinherited in new channels,
	 *		<code>false</code> otherwise.
	 */
	public Boolean getDisinheritDefault() {
		return disinheritDefault;
	}

	/**
	 * Set whether this object should be disinherited by default in new channels.
	 *
	 * @param disinheritDefault If set to <code>true</code> this object will be
	 *		disinherited by default in new channels.
	 */
	public void setDisinheritDefault(Boolean disinheritDefault) {
		this.disinheritDefault = disinheritDefault;
	}

	/**
	 * Get the channel IDs, from which the object is disinherited
	 * @return set of channel IDs
	 */
	public Set<String> getDisinheritedChannelIds() {
		return disinheritedChannelIds;
	}

	/**
	 * Set the disinherited channel IDs
	 * @param disinheritedChannelIds channel IDs
	 */
	public void setDisinheritedChannelIds(Set<String> disinheritedChannelIds) {
		if (disinheritedChannelIds != null) {
			// set as treeset to preserve natural order
			this.disinheritedChannelIds = new TreeSet<>(disinheritedChannelIds);
		} else {
			this.disinheritedChannelIds = null;
		}
	}

	/**
	 * ID of the master object
	 * @return master ID
	 */
	public String getMasterId() {
		return masterId;
	}

	/**
	 * Set the ID of the master object
	 * @param masterId ID
	 */
	public void setMasterId(String masterId) {
		this.masterId = masterId;
	}

	/**
	 * Clone the origin
	 * @param origin to clone
	 */
	@XmlTransient
	@JsonIgnore
	public void clone(StagedLocalizableObject origin) {
		this.excluded = origin.excluded;
		this.disinheritDefault = origin.disinheritDefault;
		this.disinheritedChannelIds = origin.disinheritedChannelIds;
		this.masterId = origin.masterId;
	}
}
