package com.gentics.contentnode.init;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DatabaseMetaDataHandler;
import com.gentics.lib.log.NodeLogger;

/**
 * Initialization Job that migrates the global IDs
 */
public class MigrateGlobalIds extends InitJob {
	/**
	 * Batch size for migrating globalids
	 */
	private final static int BATCH_SIZE = 10000;

	/**
	 * Name of the temporary table used to do the migration
	 */
	private final static String TMP_TABLE_NAME = "gtx_mgi_migration";

	/**
	 * List of tables, that must have the columns uuid and udate
	 */
	private static List<String> tables = Arrays.asList("construct", "construct_category", "content", "contentfile", "contentgroup", "contentrepository",
			"contentset", "contenttag", "datasource", "datasource_value", "dicuser", "ds", "ds_obj", "folder", "node", "objprop", "objprop_category", "objtag",
			"outputuser", "page", "part", "tagmap", "template", "templategroup", "templatetag", "value");

	/**
	 * List of tables, that are versioned ([tablename]_nodeversion exists). Column uuid will be versioned too.
	 */
	private static List<String> versioned = Arrays.asList("contenttag", "datasource", "datasource_value", "ds", "ds_obj", "page", "value");

	/**
	 * Map containing the crosstables. Keys are the table names, values are the id columns
	 */
	private static Map<String, List<String>> crossTables = new HashMap<String, List<String>>();

	/**
	 * List of tables for import
	 */
	private static List<String> importTables = Arrays.asList("bundle", "bundleimportobject", "missingreference");

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(MigrateGlobalIds.class);

	static {
		crossTables.put("construct_node", Arrays.asList("node_id", "construct_id"));
		crossTables.put("node_contentgroup", Arrays.asList("node_id", "contentgroup_id"));
		crossTables.put("objprop_node", Arrays.asList("objprop_id", "node_id"));
		crossTables.put("template_folder", Arrays.asList("template_id", "folder_id"));
	}

	@Override
	public void execute() throws NodeException {
		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();

			if (!DB.tableExists(t.getDBHandle(), "mappedglobalid")) {
				logger.info("Table mappedglobalid does not exist any more. Migration already done.");
				return;
			}

			for (String table : tables) {
				logger.info("Migrating table " + table);
				boolean createUuid = !DB.fieldExists(t.getDBHandle(), table, "uuid");
				boolean createUdate = !DB.fieldExists(t.getDBHandle(), table, "udate");

				logger.info("Dropping old triggers for " + table);
				DBUtils.executeUpdate("DROP TRIGGER IF EXISTS udate_insert_" + table, null);
				DBUtils.executeUpdate("DROP TRIGGER IF EXISTS udate_update_" + table, null);
				DBUtils.executeUpdate("DROP TRIGGER IF EXISTS udate_delete_" + table, null);

				if (createUuid) {
					logger.info("Creating column uuid in " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD uuid VARCHAR(41) NOT NULL DEFAULT ''", null);

					logger.info("Inserting existing uuids into " + table);
					int inserted = 0;
					int batchInserted = 0;
					do {
						// drop the temporary table (if it exists)
						DBUtils.executeUpdate("DROP TEMPORARY TABLE IF EXISTS " + TMP_TABLE_NAME, null);

						// create a temporary table and fill with some data to
						// migrate
						batchInserted = DBUtils.executeUpdate("CREATE TEMPORARY TABLE IF NOT EXISTS " + TMP_TABLE_NAME + " AS "
								+ "(SELECT t.id id, CONCAT(SUBSTRING(mgi.globalprefix, 1, 4), '.', mgi.globalid) uuid FROM " + table
								+ " t JOIN mappedglobalid mgi ON " + "(t.id = mgi.localid AND mgi.tablename = '" + table + "') WHERE t.uuid = '' LIMIT "
								+ BATCH_SIZE + ")", null);

						// migrate the data
						if (batchInserted > 0) {
							DBUtils.executeUpdate("UPDATE " + table + " t JOIN " + TMP_TABLE_NAME + " tmp ON (t.id = tmp.id) SET t.uuid = tmp.uuid", null);
							inserted += batchInserted;
						}
					} while(batchInserted > 0);
					logger.info("Inserted " + inserted + " existing uuids into " + table);

					// generate missing uuid's. We do this in a loop, because if we did this in a single statement, all records would get the same uuid
					String sql = "UPDATE " + table + " SET uuid = CONCAT((SELECT globalprefix FROM globalprefix), '.', UUID()) WHERE uuid = '' LIMIT 1";
					int updateCount = 0;
					int totalCreate = 0;
					logger.info("Creating missing uuids in " + table);
					do {
						updateCount = DBUtils.executeUpdate(sql, null);
						totalCreate += updateCount;
					} while(updateCount > 0);
					logger.info("Created " + totalCreate + " missing uuids in " + table);

					logger.info("Creating unique key for " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD UNIQUE KEY (uuid)", null);
				}
				if (createUdate) {
					logger.info("Creating column udate in " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD udate INT DEFAULT 0", null);

					logger.info("Inserting existing udates into " + table);
					int inserted = DBUtils.executeUpdate("UPDATE " + table + " t JOIN udate ON (t.id = udate.o_id AND udate.tablename = '" + table
							+ "') SET t.udate = udate.udate", null);
					logger.info("Inserted " + inserted + " existing udates into " + table);
				}

				logger.info("Table " + table + " done.");
			}

			for (String table : versioned) {
				logger.info("Migrating table " + table + "_nodeversion");
				boolean createUuid = !DB.fieldExists(t.getDBHandle(), table + "_nodeversion", "uuid");

				if (createUuid) {
					logger.info("Creating column uuid in " + table + "_nodeversion");
					DBUtils.executeUpdate("ALTER TABLE " + table + "_nodeversion ADD uuid varchar(41) NOT NULL", null);
					logger.info("Inserting existing uuids into " + table + "_nodeversion");
					DBUtils.executeUpdate("UPDATE " + table + "_nodeversion n JOIN " + table + " t ON (n.id = t.id) SET n.uuid = t.uuid", null);
				}

				logger.info("Table " + table + "_nodeversion done.");
			}

			for (Map.Entry<String, List<String>> entry : crossTables.entrySet()) {
				String table = entry.getKey();
				logger.info("Migrating table " + table);
				List<String> ids = entry.getValue();

				boolean createUuid = !DB.fieldExists(t.getDBHandle(), table, "uuid");

				if (createUuid) {
					logger.info("Creating column uuid in " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD uuid VARCHAR(41) NOT NULL DEFAULT ''", null);
					logger.info("Inserting existing uuids into " + table);
					int inserted = DBUtils.executeUpdate("UPDATE " + table + " t JOIN mappedglobalid mgi ON (t." + ids.get(0) + " = mgi.localid AND t." + ids.get(1)
							+ " = mgi.localid2 AND tablename = '" + table + "') SET t.uuid = CONCAT(SUBSTRING(mgi.globalprefix, 1, 4), '.', mgi.globalid)", null);
					logger.info("Inserted " + inserted + " existing uuids into " + table);

					// generate missing uuid's. We do this in a loop, because if we did this in a single statement, all records would get the same uuid
					String sql = "UPDATE " + table + " SET uuid = CONCAT((SELECT globalprefix FROM globalprefix), '.', UUID()) WHERE uuid = '' LIMIT 1";
					int updateCount = 0;
					int totalCreate = 0;
					logger.info("Creating missing uuids in " + table);
					do {
						updateCount = DBUtils.executeUpdate(sql, null);
						totalCreate += updateCount;
					} while(updateCount > 0);
					logger.info("Created " + totalCreate + " missing uuids in " + table);

					logger.info("Creating unique key for " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD UNIQUE KEY (uuid)", null);
				}
				logger.info("Table " + table + " done.");
			}

			for (String table : importTables) {
				logger.info("Migrating table " + table);
				String prefix = "missingreference".equals(table) ? "target_" : "";

				boolean createUuid = !DB.fieldExists(t.getDBHandle(), table, prefix + "uuid");

				if (createUuid) {
					logger.info("Creating column uuid in " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + prefix + "uuid VARCHAR(41) NOT NULL DEFAULT '' AFTER " + prefix + "globalid", null);
					logger.info("Migrating existing uuids for " + table);
					DBUtils.executeUpdate("UPDATE " + table + " SET " + prefix + "uuid = CONCAT(SUBSTRING(" + prefix + "globalprefix, 1, 4), '.', " + prefix + "globalid)", null);
					logger.info("Dropping old columns from " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " DROP COLUMN " + prefix + "globalprefix, DROP COLUMN " + prefix + "globalid", null);
					logger.info("Creating key for uuid in " + table);
					DBUtils.executeUpdate("ALTER TABLE " + table + " ADD KEY (" + prefix + "uuid)", null);
				}
				logger.info("Table " + table + " done.");
			}

			// do a final check for all tables, whether all columns exist and are filled properly
			logger.info("Checking migration");
			MigrationChecker checker = new MigrationChecker();
			DB.handleDatabaseMetaData(t.getDBHandle(), checker);
			checker.assertSuccess();

			// drop the temporary table (if it exists)
			DBUtils.executeUpdate("DROP TEMPORARY TABLE IF EXISTS " + TMP_TABLE_NAME, null);

			logger.info("Dropping old tables mappedglobalidsequence, mappedglobalid and udate");
			DBUtils.executeUpdate("DROP TABLE IF EXISTS mappedglobalidsequence, mappedglobalid, udate", null);

			// Clear the node object cache
			t.clearNodeObjectCache();

			t.commit(false);
			logger.info("Migration done");

			// check the triggers again
			UdateChecker.check();
		} catch (Exception e) {
			throw new NodeException("Error while migrating global IDs", e);
		}
	}

	/**
	 * Class for checking the migration success
	 */
	protected static class MigrationChecker implements DatabaseMetaDataHandler {
		/**
		 * Map holding all tables that need to have the column 'uuid', values are true iff column exists
		 */
		protected Map<String, Boolean> uuidColumn = new HashMap<String, Boolean>();

		/**
		 * Map holding all tables that need to have a unique key on column 'uuid', values are true iff index exists
		 */
		protected Map<String, Boolean> uuidIndex = new HashMap<String, Boolean>();

		/**
		 * Map holding all tables that need to have the column 'udate', values are true iff column exists
		 */
		protected Map<String, Boolean> udateColumn = new HashMap<String, Boolean>();

		/**
		 * Create an instance, initialize all checks
		 */
		public MigrationChecker() {
			for (String table : tables) {
				uuidColumn.put(table, false);
				uuidIndex.put(table, false);
				udateColumn.put(table, false);
			}
			for (String table : versioned) {
				uuidColumn.put(table, false);
			}
			for (String table : crossTables.keySet()) {
				uuidColumn.put(table, false);
				uuidIndex.put(table, false);
			}
			for (String table : importTables) {
				uuidColumn.put(table, false);
			}
		}

		@Override
		public void handleMetaData(DatabaseMetaData metaData) throws SQLException {
			for (Map.Entry<String, Boolean> entry : uuidColumn.entrySet()) {
				String uuidColumn = getUuidColumn(entry.getKey());
				try (ResultSet columns = metaData.getColumns(null, null, entry.getKey(), uuidColumn)) {
					if (columns.next()) {
						entry.setValue(true);
					}
				}
			}

			for (Map.Entry<String, Boolean> entry : uuidIndex.entrySet()) {
				try (ResultSet indices = metaData.getIndexInfo(null, null, entry.getKey(), true, false)) {
					while (indices.next()) {
						if ("uuid".equals(indices.getString("COLUMN_NAME")) && !indices.getBoolean("NON_UNIQUE")) {
							entry.setValue(true);
							break;
						}
					}
				}
			}

			for (Map.Entry<String, Boolean> entry : udateColumn.entrySet()) {
				try (ResultSet columns = metaData.getColumns(null, null, entry.getKey(), "udate")) {
					if (columns.next()) {
						entry.setValue(true);
					}
				}
			}
		}

		/**
		 * Get the name of the uuid column in the given table
		 * @param table table
		 * @return name of the uuid column
		 */
		protected String getUuidColumn(String table) {
			if ("missingreference".equals(table)) {
				return "target_uuid";
			} else {
				return "uuid";
			}
		}

		/**
		 * Check whether everything was migrated
		 * @throws NodeException
		 */
		protected void assertSuccess() throws NodeException {
			boolean success = true;

			for (Map.Entry<String, Boolean> entry : uuidColumn.entrySet()) {
				String table = entry.getKey();
				boolean done = entry.getValue();
				String uuidColumn = getUuidColumn(table);
				if (done) {
					logger.info("Column '" + uuidColumn + "' was created for table '" + table + "'");
				} else {
					logger.error("Table '" + table + "' is missing column '" + uuidColumn + "'");
					success = false;
				}
			}

			for (Map.Entry<String, Boolean> entry : uuidIndex.entrySet()) {
				String table = entry.getKey();
				boolean done = entry.getValue();
				if (done) {
					logger.info("Unique Key was created for column '" + table + ".uuid'");
				} else {
					logger.error("Missing or incorrect unique key on column '" + table + ".uuid'");
					success = false;
				}
			}

			for (Map.Entry<String, Boolean> entry : udateColumn.entrySet()) {
				String table = entry.getKey();
				boolean done = entry.getValue();
				if (done) {
					logger.info("Column udate was created for table '" + table + "'");
				} else {
					logger.error("Table '" + table + "' is missing column udate");
					success = false;
				}
			}

			if (!success) {
				throw new NodeException("Some tables were not migrated successfully");
			}
		}
	}
}
