/*
 * @author norbert
 * @date 29.02.2008
 * @version $Id: DeletedDBObject.java,v 1.2 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Table;

/**
 * This is the special class for deleted DBOBjects
 */
public class DeletedDBObject extends DBObject {

	/**
	 * Create an instance of the deleted object
	 * @param sourceTable source table
	 */
	public DeletedDBObject(Table sourceTable) {
		super(sourceTable, null);
	}
}
