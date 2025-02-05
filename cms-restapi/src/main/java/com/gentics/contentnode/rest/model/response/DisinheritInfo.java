package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DisinheritInfo {
	/**
	 * True if the object is be excluded from multichannelling, false if not
	 */
	private Boolean exclude;

	/**
	 * Node IDs for disinheriting status
	 */
	private List<Integer> disinherit;

	/**
	 * IDs of nodes, where this object can be inherited
	 */
	private List<Integer> inheritable;

	/**
	 * Create an empty instance
	 */
	public DisinheritInfo() {
	}

	/**
	 * Create an instance with response info and data
	 * @param exclude exclusion status flag
	 * @param disinherit disinherited node IDs
	 * @param inheritable inheritable node IDs
	 */
	public DisinheritInfo(Boolean exclude, List<Integer> disinherit, List<Integer> inheritable) {
		setExclude(exclude);
		setDisinherit(disinherit);
		setInheritable(inheritable);
	}

	/**
	 * True if the object is be excluded from multichannelling, false if not
	 * @return true for exclusion
	 */
	public Boolean isExclude() {
		return exclude;
	}

	/**
	 * Set true to exclude object from multichannelling
	 * @param exclude true to exclude
	 */
	public void setExclude(Boolean exclude) {
		this.exclude = exclude;
	}

	/**
	 * IDs of nodes/channels, in which the object will not be inherited. This will be ignored, if the object is excluded from multichannelling
	 * @return set of node IDs
	 */
	public List<Integer> getDisinherit() {
		return disinherit;
	}

	/**
	 * Set node IDs to disinherit object
	 * @param disinherit set of node IDs
	 */
	public void setDisinherit(List<Integer> disinherit) {
		this.disinherit = disinherit;
	}

	/**
	 * IDs of nodes/channels, where this object (actually its master) can be inherited
	 * @return list of node IDs
	 */
	public List<Integer> getInheritable() {
		return inheritable;
	}

	/**
	 * Set the node IDs where this object can be inherited
	 * @param inheritable list of node IDs
	 */
	public void setInheritable(List<Integer> inheritable) {
		this.inheritable = inheritable;
	}
}
