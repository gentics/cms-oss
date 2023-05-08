package com.gentics.contentnode.rest.model;

/**
 * Compatibility level for used CMP components in a node.
 */
public enum CmpCompatibility {
	/**
	 * Both the Mesh and Portal versions could be retrieved, and match the CMP
	 * version of the CMS.
	 */
	SUPPORTED,
	/**
	 * Both the Mesh and Portal versions could be retrieved, but at least one
	 * of them does not match the CMP version of the CMS.
	 */
	NOT_SUPPORTED,
	/**
	 * Either the Mesh or Portal version could not be retrieved, or the CMS
	 * could not determine the requirements for its CMP version.
	 */
	UNKNOWN
}
