/*
 * @author norbert
 * @date 11.10.2006
 * @version $Id: NodeNameModificator.java,v 1.4 2008-05-26 15:05:55 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import com.gentics.contentnode.dbcopy.CopyController;
import com.gentics.contentnode.dbcopy.DBCopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.ObjectModificator;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;

/**
 * Object modifier that makes node names unique
 */
public class NodeNameModificator implements ObjectModificator {

	/* (non-Javadoc)
	 * @see com.gentics.ObjectModificator#modifyObject(com.gentics.StructureCopy, java.sql.Connection, com.gentics.DBObject)
	 */
	public void modifyObject(StructureCopy copy, Connection conn, DBObject object, int actionTaken) throws StructureCopyException {
		// do nothing when not copying in the db or the object was ignored
		if (!(copy.getCopyController() instanceof DBCopyController) || actionTaken == CopyController.OBJECT_IGNORED) {
			return;
		}

		// make the name of the new node unique

		// only do this for the "main" folders
		Object motherId = object.getColValue("mother");

		PreparedStatement st = null;
		String currentName = null;
		List<String> conflictingNames = new Vector<String>();

		try {
			if (motherId == null || motherId.equals(new Integer(0))) {
				// get the name of the object
				st = conn.prepareStatement("SELECT name FROM folder WHERE id = ?");
				st.setObject(1, object.getId());
				ResultSet res = st.executeQuery();

				if (res.next()) {
					currentName = res.getString("name");
				} else {
					throw new SQLException("Could not find name of the node");
				}
				res.close();
				st.close();
				st = null;

				// get all conflicting names
				st = conn.prepareStatement("SELECT name FROM folder WHERE mother = 0 AND id != ?");
				st.setObject(1, object.getId());

				res = st.executeQuery();
				while (res.next()) {
					conflictingNames.add(res.getString("name"));
				}
				res.close();
				st.close();
				st = null;

				// find a new name
				int currentCounter = 1;
				boolean foundNewName = false;
				String newName = null;

				while (!foundNewName) {
					newName = "Kopie " + currentCounter + " von " + currentName;
					if (!conflictingNames.contains(newName)) {
						foundNewName = true;
					}
					currentCounter++;
				}

				// now update the name
				st = conn.prepareStatement("UPDATE folder SET name = ? WHERE id = ?");
				st.setString(1, newName);
				st.setObject(2, object.getId());
				st.executeUpdate();
				st.close();
				st = null;
			}
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
