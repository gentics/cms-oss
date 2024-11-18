/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ValueList.java,v 1.6 2007-01-03 12:20:14 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collection;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.resolving.ResolvableMapWrappable;

/**
 * The valuelist is a special list which holds values. The values can be retrieved by
 * keyname, partorder or partid.
 */
public interface ValueList extends Resolvable, Collection<Value>, ResolvableMapWrappable {

	/**
	 * get the value by partid.
	 * @param partId the partid of the value to get.
	 * @return the value, or null if not found.
	 */
	Value getByPartId(Object partId);

	/**
	 * get the value by part keyname.
	 * @param keyname the keyname of the part of the value to get.
	 * @return the value, or null if not found.
	 */
	Value getByKeyname(String keyname);

	/**
	 * Get the value by ID
	 * @param id value ID
	 * @return value or null
	 */
	Value getById(int id);

	/**
	 * Check whether this valuelist has the same values as the other
	 * @param other other
	 * @return true, iff the valuelists have the same content
	 */
	boolean hasSameValues(ValueList other) throws NodeException;
}
