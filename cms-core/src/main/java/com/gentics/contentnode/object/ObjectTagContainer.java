/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ObjectTagContainer.java,v 1.4 2010-10-19 11:23:41 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * This is an interface for all objects which may hold objecttags.
 * It defines methods to retrieve the objecttags of this object by name.
 */
public interface ObjectTagContainer extends NodeObject, StackResolvable {

	/**
	 * get an objecttag of this object by name without fallback.
	 *
	 * @param name the name of the object tag, without 'object.' prefix.
	 * @return the objecttag, or null if not found.
	 * @throws NodeException
	 */
	ObjectTag getObjectTag(String name) throws NodeException;

	/**
	 * get an objecttag of this object by name and do fallback to parent objects if requested.
	 *
	 * @param name the name of the objecttag, without 'object.' prefix.
	 * @param fallback true, if fallback to parent objects should be done.
	 * @return the objecttag, or null if not found.
	 * @throws NodeException
	 */
	ObjectTag getObjectTag(String name, boolean fallback) throws NodeException;

	/**
	 * get the list of all objects directly linked to this object hashed by name, without the
	 * 'object.' prefix.
	 *
	 * @return a map of objecttags as String->ObjectTag. 
	 * @throws NodeException 
	 */
	Map<String, ObjectTag> getObjectTags() throws NodeException;
}
