package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model of a tagmap inconsistency
 */
@XmlRootElement
public class TagmapEntryInconsistencyModel implements Serializable {
	/**
	 * Serial Version
	 */
	private static final long serialVersionUID = 2795737349818777410L;

	private String description;

	private List<String> entries = new ArrayList<>();

	/**
	 * Human readable description of the inconcistency
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set description
	 * @param description description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * List of entry UUIDs of tagmap entries forming the inconcistency
	 * @return list of entry UUIDs
	 */
	public List<String> getEntries() {
		return entries;
	}

	/**
	 * Set entry UUIDs
	 * @param entries entries
	 */
	public void setEntries(List<String> entries) {
		this.entries = entries;
	}
}
