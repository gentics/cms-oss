package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model for the localization information of a specific type.
 */
@XmlRootElement
public class LocalizationInfoItem implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7273257220306690040L;

	/**
	 * Number of inherited objects
	 */
	private int inherited = 0;

	/**
	 * Number of localized objects
	 */
	private int localized = 0;

	/**
	 * Number of local objects
	 */
	private int local = 0;

	/**
	 * Empty constructor
	 */
	public LocalizationInfoItem() {}

	/**
	 * Copy constructor
	 * @param item localization info
	 */
	public LocalizationInfoItem(LocalizationInfoItem item) {
		this.inherited = item.inherited;
		this.localized = item.localized;
		this.local = item.local;
	}

	/**
	 * @return the inherited
	 */
	public int getInherited() {
		return inherited;
	}

	/**
	 * @param inherited the inherited to set
	 */
	public void setInherited(int inherited) {
		this.inherited = inherited;
	}

	/**
	 * @return the localized
	 */
	public int getLocalized() {
		return localized;
	}

	/**
	 * @param localized the localized to set
	 */
	public void setLocalized(int localized) {
		this.localized = localized;
	}

	/**
	 * @return the local
	 */
	public int getLocal() {
		return local;
	}

	/**
	 * @param local the local to set
	 */
	public void setLocal(int local) {
		this.local = local;
	}

	/**
	 * Merge this item with the given item
	 * @param item item
	 */
	public void mergeWith(LocalizationInfoItem item) {
		this.inherited += item.inherited;
		this.localized += item.localized;
		this.local += item.local;
	}
}
