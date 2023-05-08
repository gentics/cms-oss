/*
 * @author norbert
 * @date 02.04.2009
 * @version $Id: ExcludedObject.java,v 1.3 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

/**
 * Helper class to mark mandatory objects, that are excluded from an export
 */
public class ExcludedObject extends DBObject {

	/**
	 * Create instance of the excluded object
	 * @param sourceTable source table
	 */
	public ExcludedObject(Table sourceTable, String name) {
		super(sourceTable, name);
	}
}
