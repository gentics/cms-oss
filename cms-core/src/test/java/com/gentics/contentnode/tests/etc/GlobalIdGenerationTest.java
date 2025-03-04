package com.gentics.contentnode.tests.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBTableType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for generation of global IDs
 */
@RunWith(value = Parameterized.class)
public class GlobalIdGenerationTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Tested table
	 */
	protected String table;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 * @throws Exception
	 */
	@Parameters(name = "{index}: table {0}")
	public static Collection<Object[]> data() throws Exception {
		Collection<Object[]> data = new ArrayList<Object[]>();

		// get all table names of tables, that are exportable, but are no cross tables
		Tables structTables = null;
		try (InputStream in = NodeConfigRuntimeConfiguration.class.getResourceAsStream("copy_configuration.xml")) {
			structTables = StructureCopy.readConfiguration(in);
		}
		JAXBTableType[] tables = structTables.getTable();

		Set<String> tableNames = new HashSet<String>();
		for (int i = 0; i < tables.length; i++) {
			Table table = (Table) tables[i];
			String tableName = table.getName();
			if (table.isExportable() && !table.isCrossTable()) {
				tableNames.add(tableName);
			}
		}

		for (String tableName : tableNames) {
			data.add(new Object[] {tableName});
		}

		return data;
	}

	/**
	 * Create a test instance for the given table
	 * @param table tested table
	 */
	public GlobalIdGenerationTest(String table) {
		this.table = table;
	}

	/**
	 * Test generation of a global id
	 * @throws Exception
	 */
	@Test
	public void testGeneration() throws Exception {
		// insert an entry in the table
		int insertId = insertRecord(table);

		// get the global id
		GlobalId globalId = GlobalId.getGlobalId(table, insertId);
		assertNotNull("Global ID was not generated", globalId);
		assertTrue("Generated Global ID {" + globalId + "} was invalid", GlobalId.isGlobalId(globalId.toString()));

		assertEquals("Check lookup of globalid", insertId, globalId.getLocalId(table).intValue());

		// now delete the entry
		DBUtils.executeStatement("DELETE FROM " + table + " WHERE id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, insertId);
			}
		}, Transaction.DELETE_STATEMENT);

		// check whether the global ID was removed
		assertEquals("Check lookup of globalid after deletion", null, globalId.getLocalId(table));
	}

	/**
	 * Insert a record into the table.
	 * If the table contains foreign keys, insert records into the tables containing the primary key first
	 * @param table table
	 * @return insert ID
	 * @throws Exception
	 */
	protected int insertRecord(String table) throws Exception {
		DBHandle dbHandle = TransactionManager.getCurrentTransaction().getDBHandle();
		Map<String, String> foreignKeyTables = new HashMap<>();
		Map<String, Integer> foreignKeys = new HashMap<>();
		Map<String, String> defaultValues = new HashMap<>();

		DB.handleDatabaseMetaData(dbHandle, metaData -> {
			try (ResultSet rs = metaData.getImportedKeys(null, null, table)) {
				while (rs.next()) {
					foreignKeyTables.put(rs.getString("FKCOLUMN_NAME"), rs.getString("PKTABLE_NAME"));
				}
			}

			try (ResultSet rs = metaData.getColumns(null, null, table, null)) {
				while (rs.next()) {
					String columnName = rs.getString("COLUMN_NAME");
					String defaultValue = rs.getString("COLUMN_DEF");
					if (defaultValue != null) {
						// string default values are enclosed by single quotes
						if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
							defaultValues.put(columnName, defaultValue.substring(1, defaultValue.length() - 1));
						}
					}
				}
			}
		});

		for (Map.Entry<String, String> entry : foreignKeyTables.entrySet()) {
			String column = entry.getKey();
			String primaryTable = entry.getValue();
			foreignKeys.put(column, insertRecord(primaryTable));
		}

		final List<String> columns = DB.getTableColumns(dbHandle, table);
		columns.remove("uuid");

		AtomicInteger insertId = new AtomicInteger();
		DBUtils.executeStatement("INSERT INTO `" + table + "` (" + StringUtils.merge((Object[]) columns.toArray(new Object[columns.size()]), ",") + ") VALUES ("
				+ StringUtils.repeat("?", columns.size(), ",") + ")", Transaction.INSERT_STATEMENT, stmt -> {
					for (int i = 0; i < columns.size(); i++) {
						String columnName = columns.get(i);
						if (defaultValues.containsKey(columnName)) {
							stmt.setString(i + 1, defaultValues.get(columnName));
						} else {
							// set all columns to 0 ('0' for string based)
							stmt.setInt(i + 1, foreignKeys.getOrDefault(columnName, 0));
						}
					}
				}, null, stmt -> {
					ResultSet keys = stmt.getGeneratedKeys();
					if (keys.next()) {
						insertId.set(keys.getInt(1));
					}
				});

		// check whether the insert id was not 0
		assertTrue("ID must be generated", insertId.get() != 0);

		return insertId.get();
	}
}
