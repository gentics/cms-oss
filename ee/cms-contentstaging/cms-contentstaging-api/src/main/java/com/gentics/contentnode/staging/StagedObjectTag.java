package com.gentics.contentnode.staging;

/**
 * Model of a staged object tag
 */
public class StagedObjectTag extends StagedTag {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3398514890932417873L;

	/**
	 * Definition reference
	 */
	private StagedReference definition;

	/**
	 * Get the reference to the object property definition
	 * @return reference
	 */
	public StagedReference getDefinition() {
		return definition;
	}

	/**
	 * Set the reference to the object property definition
	 * @param definition
	 */
	public void setDefinition(StagedReference definition) {
		this.definition = definition;
	}
}
