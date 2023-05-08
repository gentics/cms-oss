/*
 * @author norbert
 * @date 12.10.2006
 * @version $Id: DeactivatePublishForNode.java,v 1.4 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.gentics.contentnode.dbcopy.DBCopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.ObjectModificator;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;

/**
 * Modificator that deactivates publishing for copied nodes
 */
public class DeactivatePublishForNode implements ObjectModificator {

	/* (non-Javadoc)
	 * @see com.gentics.ObjectModificator#modifyObject(com.gentics.StructureCopy, java.sql.Connection, com.gentics.DBObject)
	 */
	public void modifyObject(StructureCopy copy, Connection conn, DBObject object, int actionTaken) throws StructureCopyException {
		// ok, when this is an import or export, we do NOT disable publishing for the node :-)
		if (!(copy.getCopyController() instanceof DBCopyController)) {
			return;
		}
		PreparedStatement st = null;

		try {
			st = conn.prepareStatement("UPDATE node SET disable_publish = ? WHERE id = ?");
			st.setBoolean(1, true);
			st.setObject(2, object.getId());
			st.executeUpdate();
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (SQLException ignored) {}
			}
		}
	}
}
