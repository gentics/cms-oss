package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Test cases for TableVersion
 */
@Category(BaseLibTest.class)
public class TableVersionTest extends AbstractSingleVariationDatabaseTest {
	@Parameters(name = "{index}: db: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS));
	}

	/**
	 * Create test instance
	 * @param testDatabase test database
	 */
	public TableVersionTest(TestDatabase testDatabase) {
		super(testDatabase);
	}

	/**
	 * SQL Handle
	 */
	protected SQLHandle sqlHandle;

	@Before
	public void setup() throws NodeException, SQLUtilException, JDBCMalformedURLException {
		sqlHandle = new SQLHandle("testhandle");
		testDatabase.setRandomDatabasename(TableVersionTest.class.getSimpleName());
		sqlHandle.init(testDatabase.getSettingsMap());

		SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		sqlUtils.connectDatabase();
		sqlUtils.createDatabase();
		sqlUtils.disconnectDatabase();
	}

	@After
	public void tearDown() throws SQLUtilException, JDBCMalformedURLException {
		if (sqlHandle != null) {
			sqlHandle.close();
			sqlHandle = null;

			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
			sqlUtils.connectDatabase();
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Test creating versions for lots of records
	 * @throws SQLException
	 * @throws NodeException
	 */
	@Test
	public void testCreateVersion() throws SQLException, NodeException {
		int RECORDS = 950;
		int VERSIONS = 10;

		DB.update(sqlHandle.getDBHandle(), "DROP TABLE IF EXISTS testtable");
		DB.update(sqlHandle.getDBHandle(), "DROP TABLE IF EXISTS testtable_nodeversion");
		DB.update(sqlHandle.getDBHandle(), "CREATE TABLE testtable (id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, foreignid INTEGER NOT NULL, data VARCHAR(255) DEFAULT '' NOT NULL, nv_data VARCHAR(255) DEFAULT '' NOT NULL)");
		DB.update(sqlHandle.getDBHandle(), "CREATE TABLE testtable_nodeversion (auto_id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT, id INTEGER NOT NULL, foreignid INTEGER NOT NULL, data VARCHAR(255) DEFAULT '' NOT NULL, nodeversiontimestamp INTEGER DEFAULT 0 NOT NULL, nodeversion_user INTEGER DEFAULT 0 NOT NULL, nodeversionlatest INTEGER DEFAULT 0 NOT NULL, nodeversionremoved INTEGER DEFAULT 0 NOT NULL)");

		TableVersion tv = new TableVersion(false);
		tv.setAutoIncrement(true);
		tv.setHandle(sqlHandle.getDBHandle());
		tv.setTable("testtable");
		tv.setWherePart("gentics_main.foreignid = ?");

		for (int i = 0; i < RECORDS; i++) {
			DB.update(sqlHandle.getDBHandle(), "INSERT INTO testtable (foreignid, data, nv_data) VALUES (?, ?, ?)",
					new Object[] { 1, "data-" + i + "-", "data-" + i + "-" });
		}

		for (int version = 1; version <= VERSIONS; version++) {
			DB.update(sqlHandle.getDBHandle(), "UPDATE testtable SET data = CONCAT(data, ?), nv_data = CONCAT(nv_data, ?)", new Object[] { version, version });
			tv.createVersion2(1L, version, "1");
		}

		SimpleResultProcessor proc = tv.getVersionData(new Object[] {1}, 3);
		assertEquals("Versioned records", RECORDS, proc.size());

		Set<String> expectedVersioned = new HashSet<>();
		Set<String> expectedNotVersioned = new HashSet<>();
		for (int i = 0; i < RECORDS; i++) {
			expectedVersioned.add("data-" + i + "-123");
			expectedNotVersioned.add("data-" + i + "-12345678910");
		}
		Set<String> actualVersioned = new HashSet<>();
		Set<String> actualNotVersioned = new HashSet<>();
		for (SimpleResultRow row : proc) {
			actualVersioned.add(row.getString("data"));
			actualNotVersioned.add(row.getString("nv_data"));
		}

		assertEquals("Versioned data", expectedVersioned, actualVersioned);
 		assertEquals("Not versioned data", expectedNotVersioned, actualNotVersioned);
	}
}
