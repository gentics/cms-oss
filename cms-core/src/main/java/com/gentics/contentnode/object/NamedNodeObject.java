package com.gentics.contentnode.object;

/**
 * Interface for NodeObjects that have a name
 */
public interface NamedNodeObject extends NodeObject {
	/**
	 * Get the name of the object
	 * @return object name
	 */
	String getName();
}
