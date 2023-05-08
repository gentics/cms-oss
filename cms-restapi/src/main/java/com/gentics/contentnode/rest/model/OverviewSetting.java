package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.SelectType;

/**
 * Model for overview settings
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class OverviewSetting implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -620832794822693636L;

	private List<ListType> listTypes;

	private List<SelectType> selectTypes;

	private boolean hideSortOptions;

	private boolean stickyChannel;

	/**
	 * Create empty instance
	 */
	public OverviewSetting() {
	}

	/**
	 * Allowed types of listed objects
	 * @return list types
	 */
	public List<ListType> getListTypes() {
		return listTypes;
	}

	/**
	 * Set list types
	 * @param listTypes list types
	 */
	public void setListTypes(List<ListType> listTypes) {
		this.listTypes = listTypes;
	}

	/**
	 * Allowed ways to select objects
	 * @return select types
	 */
	public List<SelectType> getSelectTypes() {
		return selectTypes;
	}

	/**
	 * Set select types
	 * @param selectTypes select types
	 */
	public void setSelectTypes(List<SelectType> selectTypes) {
		this.selectTypes = selectTypes;
	}

	/**
	 * Flag to determine, whether sorting options shall be hidden
	 * @return flag
	 */
	public boolean isHideSortOptions() {
		return hideSortOptions;
	}

	/**
	 * Set hideSortOptions flag
	 * @param hideSortOptions flag
	 */
	public void setHideSortOptions(boolean hideSortOptions) {
		this.hideSortOptions = hideSortOptions;
	}

	/**
	 * Flag to determine, whether source channel shall be stored for every selected object
	 * @return flag
	 */
	public boolean isStickyChannel() {
		return stickyChannel;
	}

	/**
	 * Set stickyChannel flag
	 * @param stickyChannel flag
	 */
	public void setStickyChannel(boolean stickyChannel) {
		this.stickyChannel = stickyChannel;
	}
}
