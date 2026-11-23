package com.gentics.contentnode.rest.model;

/**
 * Type of the portal, which renders the content of a node.
 */
public enum PortalType {
	/** Gentics Portal | java */
	Java,

	/** Gentics Portal | php */
	PHP;

	/**
	 * Get the portal type for the given name, ignoring case. Values from properties are entered by hand, so "java" must be accepted as well.
	 * @param name name, may be null
	 * @return portal type or null, if the name does not match any portal type
	 */
	public static PortalType parse(String name) {
		if (name == null) {
			return null;
		}
		String trimmed = name.trim();
		for (PortalType type : values()) {
			if (type.name().equalsIgnoreCase(trimmed)) {
				return type;
			}
		}
		return null;
	}
}
