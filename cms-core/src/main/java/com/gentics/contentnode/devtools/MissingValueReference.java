package com.gentics.contentnode.devtools;

/**
 * Reference to a missing value.
 */
public class MissingValueReference {

	/** Global ID of the missing object. */
	private final String targetGlobalId;
	/** Name for the reference. */
	private final String name;

	public MissingValueReference(String targetGlobalId, String name) {
		this.targetGlobalId = targetGlobalId;
		this.name = name;
	}

	public String getTargetGlobalId() {
		return targetGlobalId;
	}

	public String getName() {
		return name;
	}
}
