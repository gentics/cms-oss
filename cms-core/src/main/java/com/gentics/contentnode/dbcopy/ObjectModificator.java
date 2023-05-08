/*
 * @author norbert
 * @date 11.10.2006
 * @version $Id: ObjectModificator.java,v 1.3 2008-02-25 12:48:58 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;

/**
 * Interface for object modification classes
 */
public interface ObjectModificator {

	/**
	 * Modify the object in the database (after it has been successfully
	 * copied).
	 * @param copy copy configuration
	 * @param conn database connection
	 * @param object object to modify
	 * @param actionTaken the action taken during the copying of the object, one
	 *        if ({@link CopyController#OBJECT_CREATED},
	 *        {@link CopyController#OBJECT_UPDATED},
	 *        {@link CopyController#OBJECT_REMOVED},
	 *        {@link CopyController#OBJECT_IGNORED}).
	 * @throws StructureCopyException
	 */
	void modifyObject(StructureCopy copy, Connection conn, DBObject object, int actionTaken) throws StructureCopyException;
}
