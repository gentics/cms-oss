package com.gentics.contentnode.rest.model.devtools;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Item that is returned to autocomplete calls
 */
@XmlRootElement
public class AutocompleteItem {
	private String label;

	private String value;

	/**
	 * Create an empty instance
	 */
	public AutocompleteItem() {
	}

	/**
	 * Create an instance with label and value
	 * @param label label
	 * @param value value
	 */
	public AutocompleteItem(String label, String value) {
		this.label = label;
		this.value = value;
	}

	/**
	 * Label of the autocomplete item
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set the label
	 * @param label label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Value of the autocomplete item
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value
	 * @param value value
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
