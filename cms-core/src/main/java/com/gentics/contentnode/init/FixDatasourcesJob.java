package com.gentics.contentnode.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Datasource;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.contentnode.factory.object.TableVersion;

/**
 * Background job that fixes the DatasourcePartType instances.
 * <ol>
 * <li>Separate instance of datasource (and datasource_values) will be created for each value</li>
 * <li>Versions of datasource (and datasource_value) will be created for each value of a contenttag</li>
 * </ol>
 */
public class FixDatasourcesJob extends InitJob {

	@Override
	public void execute() throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting job " + getClass().getName());
		}
		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();

			TableVersion contentTagTableVersion = getTableVersion(t.getDBHandle(), "contenttag", "id = ?");
			TableVersion datasourceTableVersion = getTableVersion(t.getDBHandle(), "datasource", "id = ?");
			TableVersion datasourceValueTableVersion = getTableVersion(t.getDBHandle(), "datasource_value", "datasource_id = ?");

			// 1. get all values for a part with type_id = 32 (DAtasource)

			if (logger.isInfoEnabled()) {
				logger.info("Get values of DatasourcePartType");
			}
			// map of value.id -> datasource.id
			final Map<Integer, Integer> referencedDatasource = new HashMap<Integer, Integer>();
			// map of value.id -> contenttag.id
			final Map<Integer, Integer> contentTagIds = new HashMap<Integer, Integer>();
			DBUtils.executeStatement("SELECT value.id, value.value_ref, value.contenttag_id FROM value, part WHERE value.part_id = part.id AND part.type_id = 32", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int valueId = rs.getInt("id");
						int datasourceId = rs.getInt("value_ref");
						int contentTagId = rs.getInt("contenttag_id");
						if (datasourceId > 0) {
							referencedDatasource.put(valueId, datasourceId);
							if (contentTagId > 0) {
								contentTagIds.put(valueId, contentTagId);
							}
						}
					}
				}
			});

			// 2. iterate through all values and create datasource copies if necessary
			if (logger.isInfoEnabled()) {
				logger.info("Creating copies of datasources that are illegally shared between values");
			}
			Set<Integer> foundDatasources = new HashSet<Integer>();
			for (Map.Entry<Integer, Integer> entry : referencedDatasource.entrySet()) {
				int valueId = entry.getKey();
				int datasourceId = entry.getValue();

				if (logger.isDebugEnabled()) {
					logger.debug("Found value " + valueId + " referencing datasource " + datasourceId);
				}

				Datasource datasource = t.getObject(Datasource.class, datasourceId);

				// create a copy of the datasource if necessary
				if (datasource != null && foundDatasources.contains(datasourceId)) {
					if (logger.isDebugEnabled()) {
						logger.debug("datasource " + datasourceId + " is referenced multiple times and needs to be copied.");
					}

					Datasource copy = (Datasource)datasource.copy();
					copy.save();
					t.commit(false);
					datasourceId = ObjectTransformer.getInt(copy.getId(), 0);

					if (logger.isDebugEnabled()) {
						logger.debug("Created datasource " + datasourceId + " as copy of datasource " + datasource.getId());
					}

					// 3. update the values to reference the correct datasource, update value_nodeversion also
					DBUtils.executeUpdate("UPDATE value SET value_ref = ? WHERE id = ?", new Object[] {datasourceId, valueId});
					DBUtils.executeUpdate("UPDATE value_nodeversion SET value_ref = ? WHERE id = ?", new Object[] {datasourceId, valueId});

					if (logger.isDebugEnabled()) {
						logger.debug("Updated value " + valueId + " to reference datasource " + datasourceId);
					}

					entry.setValue(datasourceId);
				}
				foundDatasources.add(datasourceId);
			}

			// 4. for datasources referenced from contenttags, create a version (with the oldest timestamp of the value)
			if (logger.isInfoEnabled()) {
				logger.info("Creating versions for datasources used in contenttags");
			}

			for (Map.Entry<Integer, Integer> entry : contentTagIds.entrySet()) {
				int valueId = entry.getKey();
				long contentTagId = entry.getValue().longValue();
				long datasourceId = referencedDatasource.get(valueId).longValue();

				if (logger.isDebugEnabled()) {
					logger.debug("Datasource " + datasourceId + " is referenced from value " + valueId + " of contenttag " + contentTagId);
				}

				// check whether the datasource already has versions
				if (ObjectTransformer.isEmpty(datasourceTableVersion.getVersions(datasourceId))) {
					if (logger.isDebugEnabled()) {
						logger.debug("Datasource " + datasourceId + " does not have any versions, creating one.");
					}
					Version[] contentTagVersions = contentTagTableVersion.getVersions(contentTagId);
					if (!ObjectTransformer.isEmpty(contentTagVersions)) {
						Version version = contentTagVersions[0];

						datasourceTableVersion.createVersion2(datasourceId, version.getTimestamp(), version.getUser());
						datasourceValueTableVersion.createVersion2(datasourceId, version.getTimestamp(), version.getUser());
						t.commit(false);
						if (logger.isDebugEnabled()) {
							logger.debug("Created version @" + version.getTimestamp() + " for datasource " + datasourceId);
						}
					} else {
						logger.warn("Did not find any versions of contenttag " + contentTagId + ": No version of datasource " + datasourceId
								+ " will be created");
					}
				}
			}
		} catch (NodeException e) {
			throw new NodeException("Error while fixing datasource tags", e);
		} finally {
			if (t != null) {
				try {
					t.commit(false);
				} catch (TransactionException ignored) {
				}
			}
		}
	}

	/**
	 * Get TableVersion instance for a specific table
	 * @param handle db handle
	 * @param table table
	 * @param wherePart where part
	 * @return TableVersion instance
	 * @throws NodeException
	 */
	protected TableVersion getTableVersion(DBHandle handle, String table, String wherePart) throws NodeException {
		TableVersion tableVersion = new TableVersion();
		tableVersion.setAutoIncrement(true);
		tableVersion.setTable(table);
		tableVersion.setWherePart(wherePart);
		return tableVersion;
	}
}
