package com.gentics.contentnode.servlets;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBtableType;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.PrefixService;
import com.gentics.contentnode.etc.RandomPrefixService;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.genericexceptions.UnavailableException;
import com.gentics.lib.log.NodeLogger;

/**
* this is a tool class which executes all checks necessary for the
* udate table, and creates the according table/stored procedure/trigger
*/
public class UdateChecker {

	private final static ServiceLoaderUtil<PrefixService> prefixServiceLoader = ServiceLoaderUtil
			.load(PrefixService.class);
	private static NodeLogger logger = NodeLogger.getNodeLogger(UdateChecker.class);
	private UdateChecker() {// do not instantiate me ..
	}

	/**
	 * basic check routine which will execute all needed check routines
	 */
	public static void check() {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		DBHandle handle = config.getNodeConfig().getSQLHandle(true).getDBHandle();
		DB.clearTableFieldCache();

		// The trigger version is stored in nodesetup - if it is incremented all triggers and stored procedures
		// are forced to be renewed
		int triggerversion = 17;

		SimpleResultProcessor proc = new SimpleResultProcessor();

		try {
			DB.query(handle, "SELECT intvalue FROM nodesetup WHERE name = 'triggerversion'", proc);
			int curversion = -1;

			if (proc.size() > 0) {
				curversion = proc.getRow(1).getInt("intvalue");
			}
			boolean forcetriggers = triggerversion != curversion;
			boolean triggerupdate = proc.size() > 0;

			if (forcetriggers) {
				NodeConfigRuntimeConfiguration.runtimeLog.info(
						"Forcing all triggers/stored procedures to be renewed - curversion (in local db) {" + curversion + "} triggerversion {" + triggerversion
						+ "}");
			}

			Tables structTables = null;
			try (InputStream in = NodeConfigRuntimeConfiguration.class.getResourceAsStream("copy_configuration.xml")) {
				structTables = StructureCopy.readConfiguration(in);
			}

			checkStoredProcedures(handle, forcetriggers);
			checkTriggers(handle, forcetriggers, structTables);

			if (forcetriggers) {
				if (triggerupdate) {
					DB.update(handle, "UPDATE nodesetup SET intvalue = ? WHERE name = 'triggerversion'", new Object[] { new Integer(triggerversion) });
				} else {
					DB.update(handle, "INSERT INTO nodesetup (name, intvalue) VALUES (?, ?)", new Object[] { "triggerversion", new Integer(triggerversion)});
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error while checking stored procedures, tables and mappedglobal ids.", e);
		}
	}

	/**
	 * check if stored procedure for triggers exists and create it if it's missing
	 * @param handle
	 * @param forcetriggers
	 */
	protected static void checkStoredProcedures(DBHandle handle, boolean forcetriggers) {
		SimpleResultProcessor proc = new SimpleResultProcessor();

		NodeConfig config = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = config.getConnection(true);
			stmt = conn.createStatement();

			// check for updateUdate procedure
			DB.query(handle, "SHOW PROCEDURE STATUS WHERE Name = 'updateUdate' AND Db = '" + conn.getCatalog() + "'", proc);
			if (proc.size() == 1) {
			logger.info("remove stored procedure updateUdate");
				stmt.addBatch("DROP PROCEDURE " + conn.getCatalog() + ".updateUdate");
				stmt.executeBatch();
				stmt.clearBatch();
			}

			stmt.clearBatch();

			// check for deleteUdate procedure
			DB.query(handle, "SHOW PROCEDURE STATUS WHERE Name = 'deleteUdate' AND Db = '" + conn.getCatalog() + "'", proc);
			if (proc.size() == 1) {
				logger.info("remove stored procedure deleteUdate");
				stmt.addBatch("DROP PROCEDURE " + conn.getCatalog() + ".deleteUdate");
				stmt.executeBatch();
				stmt.clearBatch();
			}

			stmt.clearBatch();

			// check for insertGlobalId procedure
			DB.query(handle, "SHOW PROCEDURE STATUS WHERE Name = 'insertGlobalId' AND Db = '" + conn.getCatalog() + "'", proc);
			if (proc.size() == 1) {
				logger.info("remove stored procedure insertGlobalId");
				stmt.addBatch("DROP PROCEDURE " + conn.getCatalog() + ".insertGlobalId");
				stmt.executeBatch();
				stmt.clearBatch();
			}

			// check for generateMappedGlobalId procedure
			DB.query(handle, "SHOW FUNCTION STATUS WHERE Name = 'generateMappedGlobalId' AND Db = '" + conn.getCatalog() + "'", proc);
			if (proc.size() == 1) {
				logger.debug("remove function generateMappedGlobalId");
				stmt.addBatch("DROP FUNCTION " + conn.getCatalog() + ".generateMappedGlobalId");
				stmt.executeBatch();
				stmt.clearBatch();
			}
		} catch (Exception e) {
			logger.error("error while checking or creating stored procedures and functions", e);
		} finally {
			if (stmt != null) {
				DB.close(stmt);
			}
			if (conn != null) {
				config.returnConnection(conn);
			}
		}
	}

	/**
	 * Check whether triggers for all tables that are exportable exist.
	 * Whether or not a table is exportable is decided with the exportable=true attribute.
	 * (copy_configuration.xml).
	 * @param forcetriggers
	 * @throws NodeException
	 */
	protected static void checkTriggers(DBHandle handle, boolean forcetriggers, Tables structTables) throws NodeException {
		try {
			// get the current user
			String user = null;
			try {
				SimpleResultProcessor userProc = new SimpleResultProcessor();
				DB.query(handle, "SELECT CURRENT_USER() user", userProc);
				if (userProc.size() >= 1) {
					user = extractUsername(userProc.getRow(1).getString("user"));
				}
			} catch (SQLException e) {
				logger.warn("Current user could not be determined, so the triggers probably will not be checked for incorrect definer", e);
			}

			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(handle, "SHOW TRIGGERS", proc);
			List<String> triggers = new ArrayList<String>(proc.size());
			Set<String> triggersWithIncorrectDefiner = new HashSet<>();

			// build list of existing triggers
			if (proc.size() > 0) {
				for (SimpleResultRow row : proc) {
					triggers.add(row.getString("Trigger"));
					String definer = extractUsername(row.getString("Definer"));
					if (!StringUtils.isEmpty(user) && !StringUtils.isEqual(user, definer)) {
						triggersWithIncorrectDefiner.add(row.getString("Trigger"));
					}
				}
			}

			List<String> oldTriggerPrefixes = Arrays.asList("udate_update", "udate_insert", "udate_delete");
			List<String> newTriggerPrefixes = Arrays.asList("uuid_insert", "uuid_update");
			Set<String> handledTriggers = new HashSet<String>();
			JAXBtableType[] tables = structTables.getTable();

			for (int i = 0; i < tables.length; i++) {
				Table table = (Table) tables[i];
				String tableName = table.getName();

				if (handledTriggers.contains(tableName)) {
					continue;
				}
				handledTriggers.add(tableName);

				dropSyncTriggers(handle, oldTriggerPrefixes, tableName, triggers);

				// only tables with export objects need uuid and udate triggers.
				// if any other tables must be skipped, they must be handled here as well.
				if (!table.isExportable()) {
					// list all possible trigger prefixes that may be created by this method.
					// (will be concatenated with the table name to form trigger names).
					dropSyncTriggers(handle, newTriggerPrefixes, tableName, triggers);
				} else {
					if (!DB.fieldExists(handle, tableName, "uuid")) {
						logger.info("Column uuid does not exist in table " + tableName + ". Creation of trigger postponed.");
						continue;
					}

					String createTriggerName = getInsertTriggerName(tableName);
					if (!forcetriggers && triggers.contains(createTriggerName) && !triggersWithIncorrectDefiner.contains(createTriggerName)) {
						logger.debug("found trigger {" + createTriggerName + "}");
					} else {
						if (triggers.contains(createTriggerName)) {
							logger.info(String.format("recreating incorrect trigger {%s}", createTriggerName));
						} else {
							logger.info("adding missing trigger {" + createTriggerName + "}");
						}
						addCreateTrigger(handle, tableName, createTriggerName, table.isCrossTable(), triggers.contains(createTriggerName));
					}

					if (!table.isCrossTable()) {
						String updateTriggerName = getUpdateTriggerName(tableName);
						if (!forcetriggers && triggers.contains(updateTriggerName) && !triggersWithIncorrectDefiner.contains(updateTriggerName)) {
							logger.debug("found trigger {" + updateTriggerName + "}");
						} else {
							if (triggers.contains(updateTriggerName)) {
								logger.info(String.format("recreating incorrect trigger {%s}", updateTriggerName));
							} else {
								logger.info("adding missing trigger {"+updateTriggerName+"}");
							}
							List<String> nonDataColumns = Collections.emptyList();
							String nonDataColumnsString = table.getProperty("nondatacolumns");
							if (!ObjectTransformer.isEmpty(nonDataColumnsString)) {
								nonDataColumns = Arrays.asList(nonDataColumnsString.split(","));
							}
							addUpdateTrigger(handle, tableName, updateTriggerName, nonDataColumns, triggers.contains(updateTriggerName));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Could not check for triggers", e);
			throw new NodeException("Could not check for triggers", e);
		}
	}

	/**
	 * Drops triggers.
	 * @param handle database handle to perform the drop with.
	 * @param triggerPrefixes trigger names are a combination of a prefix and a table name.
	 * This is a list of all prefixes to combine with the given table - all resulting trigger
	 * names will be dropped if they exist.
	 * @param tableName will be combined with the triggerPrefixes to form the trigger name.
	 * @param existingTriggers names of all triggers which currently exist in the database.
	 */
	protected static void dropSyncTriggers(DBHandle handle, Collection<String> triggerPrefixes, String tableName, Collection<String> existingTriggers) throws SQLException {
		for (String triggerPrefix : triggerPrefixes) {
			String triggerName = triggerPrefix + "_" + tableName;

			if (existingTriggers.contains(triggerName)) {
				logger.info("dropping obsolete trigger " + triggerName);
				DB.update(handle, "DROP TRIGGER " + triggerName);
			}
		}
	}

	/**
	 * Create the create trigger on the given table
	 * @param handle db handle
	 * @param table table name
	 * @param triggerName trigger name
	 * @param crossTable true if it is a crosstable
	 * @param dropFirst true to drop the trigger first
	 * @throws NodeException
	 */
	protected static void addCreateTrigger(DBHandle handle, String table, String triggerName, boolean crossTable, boolean dropFirst) throws NodeException {
		try {
			if (dropFirst) {
				DB.update(handle, "DROP TRIGGER " + triggerName);
			}

			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TRIGGER ").append(triggerName).append(" BEFORE INSERT ON `").append(table).append("` FOR EACH ROW\n");
			sql.append("BEGIN\n");
			sql.append(" IF LENGTH(NEW.uuid) = 0 THEN\n");
			sql.append("  SET NEW.uuid = CONCAT((SELECT globalprefix FROM globalprefix), '.', UUID());\n");
			sql.append(" END IF;");
			if (!crossTable) {
				sql.append(" SET NEW.udate = UNIX_TIMESTAMP();");
			}
			sql.append("END");

			DB.update(handle, sql.toString());
		} catch (SQLException e) {
			throw new NodeException("Error while creating trigger " + triggerName, e);
		}
	}

	/**
	 * Recreate the update trigger for the given table
	 * @param tableName table name
	 * @throws NodeException
	 */
	public static void recreateUpdateTrigger(String tableName) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Tables structTables = null;
		try (InputStream in = NodeConfigRuntimeConfiguration.class.getResourceAsStream("copy_configuration.xml")) {
			structTables = StructureCopy.readConfiguration(in);
		} catch (Exception e) {
			throw new NodeException(e);
		}

		Table pageTable = Stream.of(structTables.getTable()).map(jaxbTable -> (Table) jaxbTable).filter(table -> tableName.equals(table.getName())).findFirst()
				.orElseThrow(() -> new NodeException("No definition found for table '" + tableName + "'"));

		if (pageTable.isCrossTable()) {
			throw new NodeException("Cannot recreate update trigger for table '" + tableName + "', because it is a cross-table");
		}

		List<String> nonDataColumns = Collections.emptyList();
		String nonDataColumnsString = pageTable.getProperty("nondatacolumns");
		if (!ObjectTransformer.isEmpty(nonDataColumnsString)) {
			nonDataColumns = Arrays.asList(nonDataColumnsString.split(","));
		}

		addUpdateTrigger(t.getDBHandle(), tableName, UdateChecker.getUpdateTriggerName(tableName), nonDataColumns, true);
	}

	/**
	 * Create the update trigger for the given table
	 * @param handle db handle
	 * @param table table name
	 * @param triggerName trigger name
	 * @param nonDataColumns list of non-data columns
	 * @param dropFirst true to drop the trigger first
	 * @throws NodeException
	 */
	protected static void addUpdateTrigger(DBHandle handle, String table, String triggerName, List<String> nonDataColumns, boolean dropFirst) throws NodeException {
		try {
			if (dropFirst) {
				DB.update(handle, "DROP TRIGGER " + triggerName);
			}

			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TRIGGER ").append(triggerName).append(" BEFORE UPDATE ON `").append(table).append("` FOR EACH ROW\n");
			sql.append("BEGIN\n");

			List<String> allColumns = new ArrayList<String>(DB.getTableColumns(handle, table));

			// remove the non data columns
			allColumns.removeAll(nonDataColumns);
			// now generate the IF statement (which compares all data columns)
			sql.append(" IF (");
			boolean first = true;

			for (String column : allColumns) {
				if (first) {
					first = false;
				} else {
					sql.append(" OR ");
				}
				sql.append("OLD.").append(column).append(" != NEW.").append(column);
			}
			sql.append(") THEN \n");

			sql.append("  SET NEW.udate = UNIX_TIMESTAMP();\n");
			sql.append(" END IF;\n");
			sql.append("END");

			DB.update(handle, sql.toString());
		} catch (SQLException e) {
			throw new NodeException("Error while creating trigger " + triggerName, e);
		}
	}

	/**
	 * Ensure that the correct globalprefix is stored in the database.
	 * @throws NodeException reading the configuration went wrong
	 * @throws UnavailableException if the globalprefix in the Node DB is empty and a new one could not be determined from the license
	 * @throws SQLException	error while accessing database
	 */
	public static void ensureGlobalPrefix() throws UnavailableException, SQLException, NodeException {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		DBHandle handle = config.getNodeConfig().getSQLHandle(true).getDBHandle();

		String licenseGlobalPrefix = getGlobalPrefix();
		String storedGlobalPrefix = getStoredGlobalPrefix();

		if (StringUtils.isEmpty(licenseGlobalPrefix) && StringUtils.isEmpty(storedGlobalPrefix)) {
			throw new UnavailableException("No globalprefix set in Node DB and no license key is available");
		}

		if (storedGlobalPrefix != null && licenseGlobalPrefix != null && !storedGlobalPrefix.equals(licenseGlobalPrefix)) {
			logger.warn("Changing globalprefix in node DB from {" + storedGlobalPrefix + "} to {" + licenseGlobalPrefix + "}");
			DB.update(handle, "UPDATE globalprefix SET globalprefix = ?", new Object[] { licenseGlobalPrefix });
		} else if (storedGlobalPrefix == null) {
			DB.update(handle, "INSERT INTO globalprefix (globalprefix) VALUES (?)", new Object[] { licenseGlobalPrefix });
			recreateTriggers();
		}
	}

	/**
	 * Finds a service implementation that is able to get or generate a global prefix
	 */
	private static String getGlobalPrefix() {
		for (PrefixService service : prefixServiceLoader) {
			String prefix = service.getGlobalPrefix();
			if (!StringUtils.isEmpty(prefix)) {
				return prefix;
			}
		}

		return new RandomPrefixService().getGlobalPrefix();
	}

	/**
	 * Recreate triggers and stored procedures
	 * @throws NodeException if Tables configuration couldn't be read
	 */
	private static void recreateTriggers() throws NodeException {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		DBHandle handle = config.getNodeConfig().getSQLHandle(true).getDBHandle();

		NodeConfigRuntimeConfiguration.runtimeLog.info("Forcing recreation of triggers.");

		Tables structTables = null;
		try (InputStream in = NodeConfigRuntimeConfiguration.class.getResourceAsStream("copy_configuration.xml")) {
			structTables = StructureCopy.readConfiguration(in);
		} catch (Exception e) {
			throw new NodeException("Couldn't read Tables object from configuration", e);
		}

		checkStoredProcedures(handle, true);
		checkTriggers(handle, true, structTables);
	}

	/**
	 * Extract the username from 'name@host' (which is returned from the DB)
	 * @param nameAndHost
	 * @return
	 */
	private static String extractUsername(String nameAndHost) {
		int atPos = nameAndHost.indexOf('@');
		if (atPos >= 0) {
			nameAndHost = nameAndHost.substring(0, atPos);
		}
		return nameAndHost;
	}

	/**
	 * Get the globalprefix as currently stored in the database
	 * @return the globalprefix stored in the database or null if there is no globalprefix in it
	 * @throws SQLException
	 */
	public static String getStoredGlobalPrefix() throws SQLException {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		DBHandle handle = config.getNodeConfig().getSQLHandle(true).getDBHandle();

		SimpleResultProcessor proc = new SimpleResultProcessor();
		DB.query(handle, "SELECT globalprefix FROM globalprefix", proc);
		String storedGlobalPrefix = null;

		if (proc.size() == 0) {
			logger.info("no globalprefix found in database, we need to generate all globalids.");
		} else {
			storedGlobalPrefix = proc.getRow(1).getString("globalprefix");
		}
		return storedGlobalPrefix;
	}

	/**
	 * Get name of the insert trigger for the given table
	 * @param tableName table name
	 * @return name of the insert trigger
	 */
	protected static String getInsertTriggerName(String tableName) {
		return "uuid_insert_" + tableName;
	}

	/**
	 * Get name of the update trigger for the given table
	 * @param tableName table name
	 * @return name of the update trigger
	 */
	protected static String getUpdateTriggerName(String tableName) {
		return "uuid_update_" + tableName;
	}
}
