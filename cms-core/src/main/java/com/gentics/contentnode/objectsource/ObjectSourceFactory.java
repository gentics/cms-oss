/*
 * @author Stefan Hepp
 * @date 30.12.2005
 * @version $Id: ObjectSourceFactory.java,v 1.12.20.1 2011-01-18 13:21:55 norbert Exp $
 */
package com.gentics.contentnode.objectsource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.lib.log.NodeLogger;

/**
 * The ObjectSourceFactory can be used to get a configured objectsource by key or an overview by value.
 */
public class ObjectSourceFactory {

	private ObjectSourceFactory() {}

	/**
	 * get an objectsource by key, like a staticobjectsource
	 * @param key the type of the objectsource.
	 * @param value the value which contains the selected values or where the selection should be stored.
	 * @return the objectsource, or null if the key is not known.
	 */
	public static ObjectSource getObjectSource(String key, Value value) {
		return null;
	}

	/**
	 * Get an overview (id) from a value.
	 *
	 * @param value the value which contains the reference to the overview.
	 * @return the overview's id, or null if no overview is linked to this value.
	 */
	public static Integer getOverviewId(Value value) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (value == null) {
			return null;
		}

		ValueContainer container = value.getContainer();

		if (container == null) {
			NodeLogger.getLogger(ObjectSourceFactory.class).warn("Value has no container.");
			return null;
		}

		String type = container.getTypeKeyword();

		// no overview for constructs
		if ("construct".equals(type)) {
			return null;
		}

		Integer typeId = container.getId();

		// if the container is new, so is the datasource
		if (typeId == null) {
			return null;
		}

		Integer dsId = null;

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			// TODO this should be done using datasources, or (better) the dsid should be stored in the value.
			// TODO seems that ds can be stored for the template_id, when not changeable: implement fallback!

			if (value.getObjectInfo().isCurrentVersion() || !"contenttag".equals(type)) {
				stmt = t.prepareStatement("SELECT id FROM ds WHERE " + type + "_id = ?");
				stmt.setInt(1, typeId);
			} else {
				int versionTimestamp = value.getObjectInfo().getVersionTimestamp();

				stmt = t.prepareStatement(
						"SELECT id FROM ds_nodeversion WHERE " + type
						+ "_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? GROUP BY id");
				stmt.setInt(1, typeId);
				stmt.setInt(2, versionTimestamp);
				stmt.setInt(3, versionTimestamp);
			}

			rs = stmt.executeQuery();

			if (rs.next()) {
				dsId = new Integer(rs.getInt("id"));
			}

		} catch (SQLException e) {
			throw new NodeException("Could not load datasource.", e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(stmt);
		}

		return dsId;
	}
}
