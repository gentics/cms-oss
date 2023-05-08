package com.gentics.contentnode.etc;

/**
 * Simple value pair class that contain the two possible values for a nodesetup entry
 * 
 * @author johannes2
 * 
 */
public class NodeSetupValuePair {
	String textValue;
	int intValue;

	/**
	 * 
	 * @param intValue
	 * @param textValue
	 */
	public NodeSetupValuePair(int intValue, String textValue) {
		this.textValue = textValue;
		this.intValue = intValue;
	}

	/**
	 * Returns the text value for this nodesetup value pair
	 * 
	 * @return
	 */
	public String getTextValue() {
		return textValue;
	}

	/**
	 * Sets the text value for this nodesetup value pair
	 * 
	 * @param textValue
	 */
	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

	/**
	 * Returns the int value for this nodesetup value pair
	 * 
	 * @return
	 */
	public int getIntValue() {
		return intValue;
	}

	/**
	 * Sets the int value for this nodesetup value pair
	 * 
	 * @param intValue
	 */
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

}
