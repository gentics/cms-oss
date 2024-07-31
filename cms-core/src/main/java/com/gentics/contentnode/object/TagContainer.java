/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: TagContainer.java,v 1.6.26.1 2011-03-07 16:53:53 norbert Exp $
 */
package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.resolving.StackResolvable;

import java.util.Map;
import java.util.Set;

/**
 * The tagcontainer is an interface for all objects which contain tags.
 */
public interface TagContainer extends StackResolvable {

	/**
	 * Get a tag by name.
	 * @param name the name of the tag to get.
	 * @return the tag, or null if not found.
	 * @throws NodeException
	 */
	Tag getTag(String name) throws NodeException;

	/**
	 * get a list of all known tags of this container with their keynames.
	 * @return a map of all tags with their keyname as String->Tag.
	 * @throws NodeException
	 */
	Map<String, ? extends Tag> getTags() throws NodeException;

	/**
	 * Get the set of available tag names
	 * @return set of available tag names
	 * @throws NodeException
	 */
	default Set<String> getTagNames() throws NodeException {
		return getTags().keySet();
	}
}
