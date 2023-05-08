/*
 * @author norbert
 * @date 31.07.2006
 * @version $Id: SimpleObject.java,v 1.1 2006-09-07 11:33:15 laurin Exp $
 */
package com.gentics.lib.datasource.simple;

import com.gentics.api.lib.resolving.Resolvable;

/*
 * Implementation notice: keep this class synchronized with the class SimpleWSObject
 */

/**
 * Interface for objects that were fetched from a datasource
 */
public interface SimpleObject extends Resolvable {

	/**
	 * Get the objects id
	 * @return objects id
	 */
	String getId();

	/**
	 * Get the attributes of the object
	 * @return array of attributes
	 */
	SimpleAttribute[] getAttributes();
}
