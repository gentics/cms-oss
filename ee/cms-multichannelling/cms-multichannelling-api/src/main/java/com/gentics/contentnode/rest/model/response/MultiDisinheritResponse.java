package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing disinherit information for multiple objects
 */
@XmlRootElement
public class MultiDisinheritResponse extends GenericResponse {
	/**
	 * Flag for exclusion status of all objects.
	 * True if the object is be excluded from multichannelling, false if not,
	 * null if the exclusion status of the objects is not equal.
	 */
	private Boolean exclude;

	/**
	 * IDs of nodes, that disinherit all objects
	 */
	private List<Integer> disinherit;

	/**
	 * IDs of nodes, that disinherit some objects
	 */
	private List<Integer> partialDisinherit;

	/**
	 * IDs of nodes, where all objects can be inherited
	 */
	private List<Integer> inheritable;

	/**
	 * Create an empty instance
	 */
	public MultiDisinheritResponse() {
	}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public MultiDisinheritResponse(List<Message> messages, ResponseInfo responseInfo) {
		super();
		setMessages(messages);
		setResponseInfo(responseInfo);
	}

	/**
	 * Create an instance
	 * @param responseInfo response info
	 * @param exclude exclusion flag (may be null)
	 * @param disinherit list if disinherited node IDs (for all objects)
	 * @param partialDisinherit list of partially disinherited node IDs (some objects are disinherited there)
	 * @param inheritable list of inheritable node IDs
	 */
	public MultiDisinheritResponse(ResponseInfo responseInfo, Boolean exclude, List<Integer> disinherit, List<Integer> partialDisinherit,
			List<Integer> inheritable) {
		super(null, responseInfo);
		setExclude(exclude);
		setDisinherit(disinherit);
		setPartialDisinherit(partialDisinherit);
		setInheritable(inheritable);
	}

	/**
	 * Flag for exclusion status of all objects.
	 * True if the object is be excluded from multichannelling, false if not,
	 * null if the exclusion status of the objects is not equal.
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
	 * IDs of nodes/channels, in which some objects will not be inherited. This will be ignored, if the object is excluded from multichannelling
	 * @return set of node IDs
	 */
	public List<Integer> getPartialDisinherit() {
		return partialDisinherit;
	}

	/**
	 * Set node IDs for partially disinherited object
	 * @param partialDisinherit set of node IDs
	 */
	public void setPartialDisinherit(List<Integer> partialDisinherit) {
		this.partialDisinherit = partialDisinherit;
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
