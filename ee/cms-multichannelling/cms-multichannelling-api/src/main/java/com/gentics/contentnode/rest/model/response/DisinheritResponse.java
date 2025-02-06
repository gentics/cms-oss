package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing disinherit information
 */
@XmlRootElement
public class DisinheritResponse extends GenericResponse {
	/**
	 * True if the object is be excluded from multichannelling, false if not
	 */
	private Boolean exclude;

	/**
	 * Indicates that the object should be disinherited by default in new channels.
	 */
	private Boolean disinheritDefault;

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
	public DisinheritResponse() {
	}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public DisinheritResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create an instance with response info and data
	 * @param responseInfo response info
	 * @param exclude exclusion status flag
	 * @param disinherit disinherited node IDs
	 * @param inheritable inheritable node IDs
	 */
	public DisinheritResponse(ResponseInfo responseInfo, Boolean exclude, Boolean disinheritDefault, List<Integer> disinherit, List<Integer> inheritable) {
		super(null, responseInfo);
		setExclude(exclude);
		setDisinheritDefault(disinheritDefault);
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
	 * Indicates whether the object should be disinherited by default in new channels.
	 *
	 * @return <code>true</code> if the object should be disinherited in new channels,
	 *		<code>false</code> otherwise.
	 */
	public Boolean isDisinheritDefault() {
		return disinheritDefault;
	}

	/**
	 * Set whether the object should be disinherited by default in new channels.
	 *
	 * @param disinheritDefault Set to <code>true</code> if the object should be
	 *		disinherited in new channels.
	 */
	public void setDisinheritDefault(Boolean disinheritDefault) {
		this.disinheritDefault = disinheritDefault;
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
