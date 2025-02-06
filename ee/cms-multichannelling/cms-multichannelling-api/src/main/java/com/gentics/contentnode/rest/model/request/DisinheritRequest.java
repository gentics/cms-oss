package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set the multichannelling exclusion and disinheriting status of an object
 */
@XmlRootElement
public class DisinheritRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4686650982391108021L;

	/**
	 * True if the object shall be excluded from multichannelling, false if not
	 */
	private Boolean exclude;

	/**
	 * Flag for disinheriting default
	 */
	private Boolean disinheritDefault = false;

	/**
	 * IDs of nodes, that shall disinherit the objects
	 */
	private List<Integer> disinherit;

	/**
	 * IDs of nodes, that shall inherit the objects
	 */
	private List<Integer> reinherit;

	/**
	 * True if exclusion and disinheriting shall be set recursively (if set on folders), false if not
	 */
	private Boolean recursive;

	/**
	 * Empty constructor
	 */
	public DisinheritRequest() {
	}

	/**
	 * True if the object shall be excluded from multichannelling, false if not
	 * @return true for exclusion
	 */
	public Boolean getExclude() {
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
	 * IDs of nodes/channels, in which the object will be inherited. This will be ignored, if the object is excluded from multichannelling
	 * @return set of node IDs
	 */
	public List<Integer> getReinherit() {
		return reinherit;
	}

	/**
	 * Set node IDs to reinherit object
	 * @param reinherit set of node IDs
	 */
	public void setReinherit(List<Integer> reinherit) {
		this.reinherit = reinherit;
	}

	/**
	 * True if exclusion and disinheriting shall be set recursively (if set on folders), false if not
	 * @return true for recursive
	 */
	public Boolean getRecursive() {
		return recursive;
	}

	/**
	 * Set true for recursive setting of exclusion/disinheriting
	 * @param recursive true for recursive
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * True if the object should be disinherited by default in new channels (default: <code>false</code>).
	 *
	 * @return <code>true</code> if disinheriting is the default for new channels,
	 *		<code>false</code> otherwise.
	 */
	public Boolean getDisinheritDefault() {
		return disinheritDefault;
	}

	/**
	 * Set the default disinheriting behavior.
	 *
	 * @param disinheritDefault Set to <code>true</code> if disinheriting should be the
	 *		default behavior for new channels.
	 */
	public void setDisinheritDefault(Boolean disinheritDefault) {
		this.disinheritDefault = disinheritDefault;
	}
}
