package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.CopyController;
import com.gentics.contentnode.dbcopy.DBCopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.ObjectModificator;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.factory.object.AbstractFactory;
import com.gentics.lib.db.DB;

/**
 * Object Modifier that generates channelset_id's for new created objects
 */
public class ChannelsetIdGenerator implements ObjectModificator {

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ObjectModificator#modifyObject(com.gentics.contentnode.dbcopy.StructureCopy, java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject, int)
	 */
	public void modifyObject(StructureCopy copy, Connection conn, DBObject object, int actionTaken) throws StructureCopyException {
		CopyController cc = copy.getCopyController();

		if (cc instanceof DBCopyController) {// for node copy, we always generated the channelset_id
		} else {
			// for other controllers (e.g. export), we never do something
			return;
		}
		if (actionTaken == CopyController.OBJECT_CREATED) {
			PreparedStatement st = null;

			try {
				int channelsetId = ObjectTransformer.getInt(AbstractFactory.createChannelsetId(), 0);
				StringBuffer sql = new StringBuffer();
				String tableName = object.getSourceTable().getName();

				sql.append("UPDATE `").append(tableName).append("` SET channelset_id = ? WHERE id = ?");
				st = conn.prepareStatement(sql.toString());
				st.setInt(1, channelsetId);
				st.setInt(2, ObjectTransformer.getInt(object.getId(), 0));
				st.executeUpdate();
			} catch (Exception e) {
				throw new StructureCopyException("Error while generating channelset_id for " + object, e);
			} finally {
				DB.close(st);
			}
		}
	}
}
