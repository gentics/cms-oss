/*
 * @author alexander
 * @date 08.10.2007
 * @version $Id: ContentRepositoryStructureCheckTest.java,v 1.2.2.1 2011-04-07 10:09:29 norbert Exp $
 */
package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.AbstractContentRepositoryStructure;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.ColumnDefinition;
import com.gentics.lib.datasource.ContentRepositoryStructureDefinition.TableDefinition;
import com.gentics.lib.datasource.MssqlContentRepositoryStructure;
import com.gentics.lib.datasource.OracleContentRepositoryStructure;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Check the structure consistency check
 *
 * @author alexander
 */
@Category(BaseLibTest.class)
public class ContentRepositoryStructureCheckTest extends AbstractSingleVariationDatabaseTest {

	public final static NodeLogger logger = NodeLogger.getNodeLogger(ContentRepositoryStructureCheckTest.class);

	protected boolean reportFailure = false;

	private AssertionAppender appender;

	private SQLUtils sqlUtils;

	private Datasource ds;

	private AbstractContentRepositoryStructure cr;

	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(
				getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS, TestDatabaseVariationConfigurations.MSSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.ORACLE_VARIATIONS));
	}

	/**
	 * Use own appender for errors to report in assertion text
	 */
	public ContentRepositoryStructureCheckTest(TestDatabase testDatabase) {
		super(testDatabase);
		appender = new AssertionAppender();
		NodeLogger.getRootLogger().addAppender(appender);
	}

	/**
	 * Create a new datasource. Read jdbc-handle.properties from file system. Disable sanitycheck and autorepair.
	 *
	 * @param versioning
	 *            true if versioning datasource should be created, false if not
	 * @return A versioning datasource created from the jdbc-handles.properties file.
	 * @throws IOException
	 *             If the properties file could not be found.
	 * @throws JDBCMalformedURLException
	 * @throws SQLUtilException
	 */
	protected VersioningDatasource createVersioningDataSource(boolean versioning, boolean autorepair, boolean autorepair2, boolean sanitycheck2) throws IOException, JDBCMalformedURLException, SQLUtilException {

		VersioningDatasource dsv = (VersioningDatasource) createDataSource(versioning, autorepair, autorepair2, sanitycheck2);

		return dsv;
	}

	/**
	 *
	 * @param versioning
	 * @param autorepair
	 * @param autorepair2
	 * @param sanitycheck2
	 * @return
	 */
	protected Datasource createDataSource(boolean versioning, boolean autorepair, boolean autorepair2, boolean sanitycheck2) {
		Properties handleProperties = testDatabase.getSettings();

		handleProperties.setProperty("type", "jdbc");
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("versioning", versioning ? "true" : "false");
		dsProperties.put("autorepair", autorepair ? "true" : "false");
		dsProperties.put("autorepair2", autorepair2 ? "true" : "false");
		dsProperties.put("sanitycheck2", autorepair ? "true" : "false");
		Datasource ds = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		return ds;
	}

	/**
	 * Create a new datasource. Read jdbc-handle.properties from file system. Disable sanitycheck and autorepair.
	 *
	 * @return A versioning datasource created from the jdbc-handles.properties file.
	 * @throws IOException
	 *             If the properties file could not be found.
	 * @throws JDBCMalformedURLException
	 * @throws SQLUtilException
	 */
	protected VersioningDatasource createDataSource() throws IOException, JDBCMalformedURLException, SQLUtilException {
		return (VersioningDatasource) createDataSource(false, false, false, false);
	}

	/**
	 * Delete all tables from the database that belong to the datasource.
	 *
	 * @param ds
	 *            The datasource to test.
	 * @param structure
	 *            The reference structure.
	 * @throws SQLException
	 * @throws NodeException
	 *             If unable to get DBHandle.
	 */
	protected void clearDatabase(Datasource ds, AbstractContentRepositoryStructure structure) throws SQLException, NodeException {
		for (Iterator iterator = structure.getReferenceTables().keySet().iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();

			try {
				runUpdate(ds, "DROP TABLE " + name, null);
				if (structure instanceof OracleContentRepositoryStructure) {
					runUpdate(ds, "DROP SEQUENCE " + name + "_SEQUENCE", null);
				}
			} catch (SQLException sqle) {
				logger.error(sqle);
			}
		}
	}

	/**
	 * Run an update statement.
	 *
	 * @param ds
	 *            The datasource to run the statement on.
	 * @param statement
	 *            The SQL statement to execute.
	 * @param params
	 *            Optional parameters for a prepared statement.
	 * @throws SQLException
	 * @throws NodeException
	 *             If unable to get valid DBHandle for datasource.
	 */
	protected void runUpdate(Datasource ds, String statement, Object[] params) throws SQLException, NodeException {
		DatasourceHandle datasourceHandle = ds.getHandlePool().getHandle();

		if (!(datasourceHandle instanceof SQLHandle)) {
			throw new NodeException("Unable to get a valid SQLHandle from datasource.");
		}

		DBHandle dbHandle = ((SQLHandle) datasourceHandle).getDBHandle();

		if (dbHandle == null) {
			throw new NodeException("Unable to get valid DBHandle from datasource.");
		}

		if (params == null) {
			DB.update(dbHandle, statement, null, null, false);
		} else {
			DB.update(dbHandle, statement, params, null, true);
		}
	}

	/**
	 * Test check and autorepair of a missing table
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingTable() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		runUpdate(ds, "DROP TABLE " + contentmap.getTableName(), null);
		if (cr instanceof OracleContentRepositoryStructure) {
			try {
				runUpdate(ds, "DROP SEQUENCE " + contentmap.getTableName() + "_SEQUENCE", null);
			} catch (SQLException sqle) {// ignore
			}
		}
		boolean testMissingTable = cr.checkStructureConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingTable);

		boolean testMissingTableAutorepair = cr.checkStructureConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingTableAutorepair);
	}

	/**
	 * Test check and autorepair of a missing column.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingColumn() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		TableDefinition contentstatus = cr.getReferenceTable("contentstatus");
		ColumnDefinition stringvalue = contentstatus.getColumn("stringvalue");

		if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds,
					"ALTER TABLE " + contentstatus.getTableName() + " DROP CONSTRAINT DF_" + stringvalue.getTableName() + "_" + stringvalue.getColumnName(), null);
		}
		runUpdate(ds, "ALTER TABLE " + contentstatus.getTableName() + " DROP COLUMN " + stringvalue.getColumnName(), null);

		boolean testMissingColumn = cr.checkStructureConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingColumn);

		boolean testMissingColumnAutorepair = cr.checkStructureConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingColumnAutorepair);
	}

	/**
	 * Test check and autorepair of an incorrect column type.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testIncorrectColumn() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		TableDefinition contentmap = cr.getReferenceTable("contentmap");
		ColumnDefinition updatetimestamp = contentmap.getColumn("updatetimestamp");

		if (cr instanceof MssqlContentRepositoryStructure) {
			try {
				runUpdate(ds,
						"ALTER TABLE " + contentmap.getTableName() + " DROP CONSTRAINT DF_" + contentmap.getTableName() + "_" + updatetimestamp.getColumnName(), null);
			} catch (SQLException sqle) {// do nothing
			}
		}
		if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ALTER COLUMN " + updatetimestamp.getColumnName() + " varchar(100)", null);
		} else if (cr instanceof OracleContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE \"" + contentmap.getTableName() + "\" MODIFY ( " + updatetimestamp.getColumnName() + " varchar(100))", null);
		} else {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " MODIFY COLUMN " + updatetimestamp.getColumnName() + " varchar(100)", null);
		}

		boolean testIncorrectColumn = cr.checkStructureConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testIncorrectColumn);

		boolean testIncorrectColumnAutorepair = cr.checkStructureConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testIncorrectColumnAutorepair);
	}

	/**
	 * Test check and autorepair of an incorrect column type.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testIncorrectColumnWithIndex() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		TableDefinition contentattribute = cr.getReferenceTable("contentattribute");
		ColumnDefinition contentid = contentattribute.getColumn("contentid");

		if (cr instanceof MssqlContentRepositoryStructure) {
			try {
				runUpdate(ds,
						"ALTER TABLE " + contentattribute.getTableName() + " DROP CONSTRAINT DF_" + contentattribute.getTableName() + "_" + contentid.getColumnName(),
						null);
				runUpdate(ds, "DROP INDEX " + contentattribute.getTableName() + ".contentattribute_idx2", null);
				runUpdate(ds, "DROP INDEX " + contentattribute.getTableName() + ".contentattribute_idx4", null);
			} catch (SQLException sqle) {// do nothing
			}
		}
		if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentattribute.getTableName() + " ALTER COLUMN " + contentid.getColumnName() + " integer", null);
			runUpdate(ds, "CREATE INDEX [contentattribute_idx2] ON [" + contentattribute.getTableName() + "] ([contentid])", null);
			runUpdate(ds, "CREATE INDEX [contentattribute_idx4] ON [" + contentattribute.getTableName() + "] ([contentid], [name])", null);
		} else if (cr instanceof OracleContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE \"" + contentattribute.getTableName() + "\" MODIFY ( " + contentid.getColumnName() + " integer)", null);
		} else {
			runUpdate(ds, "ALTER TABLE " + contentattribute.getTableName() + " MODIFY COLUMN " + contentid.getColumnName() + " integer", null);
		}

		boolean testIncorrectColumn = cr.checkStructureConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testIncorrectColumn);

		boolean testIncorrectColumnAutorepair = cr.checkStructureConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testIncorrectColumnAutorepair);
	}

	/**
	 * Test check and autorepair a missing index.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingIndex() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds, "DROP INDEX " + contentmap.getTableName() + ".contentmap_idx5", null);
		} else if (cr instanceof OracleContentRepositoryStructure) {
			runUpdate(ds, "DROP INDEX CONTENTMAP_IDX5", null);
		} else {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " DROP INDEX contentmap_idx5", null);
		}

		boolean testMissingIndex = cr.checkStructureConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingIndex);

		boolean testMissingIndexAutorepair = cr.checkStructureConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingIndexAutorepair);
	}

	/**
	 * Test check and autorepair of a missing quick column of the type text
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingTextQuickColumn() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, objecttype) VALUES (?, ?, ?, ?, ?)",
				new Object[] { "test", "1", "1", "quick_testtext", "10002" });

		boolean testMissingQuickColumn = cr.checkDataConsistency(false);
		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingQuickColumn);

		boolean testMissingQuickColumnAutorepair = cr.checkDataConsistency(true);
		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingQuickColumnAutorepair);
	}

	/**
	 * Test check and autorepair of an incorrect quick column of the type text with an incorrect default value.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testIncorrectTextQuickColumnDefaultValue() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// Add the quick column type definition
		runUpdate(ds, "INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, objecttype) VALUES (?, ?, ?, ?, ?)", new Object[] { "test", "1", "1",
				"quick_testtext", "10002" });

		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		// Create the incorrect quick column
		if (cr instanceof OracleContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD \"QUICK_TESTTEXT\" VARCHAR2(255) DEFAULT ''", null);
			runUpdate(ds, "CREATE INDEX \"IDX_CONTENTMAP_QUICK_TESTTEXT\" ON " + contentmap.getTableName() + " (\"QUICK_TESTTEXT\")", null);
		} else if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName()
					+ " ADD [quick_testtext]  [nvarchar](255) CONSTRAINT DF_contentmap_quick_testtext DEFAULT ''", null);
			runUpdate(ds, "CREATE INDEX [idx_contentmap_quick_testtext] ON [" + contentmap.getTableName() + "] ([quick_testtext])", null);
		} else {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD quick_testtext VARCHAR(255) DEFAULT ''", null);
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD KEY `idx_contentmap_quick_testtext` (`quick_testtext`)", null);
		}

		boolean isStructureValid = cr.checkStructureConsistency(false);
		assertTrue("The structure shoule be valid otherwise the checkDataConsistency will not be invoked.", isStructureValid);

		boolean isValid = cr.checkDataConsistency(false);
		assertFalse("The structure should not be valid: " + appender.getErrors(), isValid);

		boolean testMissingQuickColumnAutorepair = cr.checkDataConsistency(true);
		assertTrue("The repairing process of the cr structure was not successful: " + appender.getErrors(), testMissingQuickColumnAutorepair);
	}

	/**
	 * Test check and autorepair of an correct quick column
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testCorrectTextQuickColumnDefaultValue() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// Add the quick column type definition
		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, objecttype) VALUES (?, ?, ?, ?, ?)",
				new Object[] { "test", "1", "1", "quick_testtext", "10002" });

		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		// Create the correct quick column
		if (cr instanceof OracleContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD \"QUICK_TESTTEXT\" VARCHAR2(255) DEFAULT NULL", null);
			runUpdate(ds, "CREATE INDEX \"IDX_CONTENTMAP_QUICK_TESTTEXT\" ON " + contentmap.getTableName() + " (\"QUICK_TESTTEXT\")", null);
		} else if (cr instanceof MssqlContentRepositoryStructure) {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName()
					+ " ADD [quick_testtext]  [nvarchar](255) CONSTRAINT DF_contentmap_quick_testtext DEFAULT NULL", null);
			runUpdate(ds, "CREATE INDEX [idx_contentmap_quick_testtext] ON [" + contentmap.getTableName() + "] ([quick_testtext])", null);
		} else {
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD quick_testtext VARCHAR(255) DEFAULT NULL", null);
			runUpdate(ds, "ALTER TABLE " + contentmap.getTableName() + " ADD KEY `idx_contentmap_quick_testtext` (`quick_testtext`)", null);
		}

		boolean isStructureValid = cr.checkStructureConsistency(false);
		assertTrue("The structure shoule be valid.", isStructureValid);

		boolean isStructureWithAutorepairEnabledValid = cr.checkStructureConsistency(true);
		assertTrue("The structure shoule be valid.", isStructureWithAutorepairEnabledValid);

		boolean isValid = cr.checkDataConsistency(false);
		assertTrue("The structure should be valid: " + appender.getErrors(), isValid);

		boolean testMissingQuickColumnAutorepair = cr.checkDataConsistency(true);
		assertTrue("The repairing process of the cr structure was not successful: " + appender.getErrors(), testMissingQuickColumnAutorepair);
	}

	/**
	 * Test check and autorepair of a missing quick column.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingQuickColumn() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, objecttype) VALUES (?, ?, ?, ?, ?)",
				new Object[] { "test", "3", "1", "quick_test", "10002" });

		boolean testMissingQuickColumn = cr.checkDataConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingQuickColumn);

		boolean testMissingQuickColumnAutorepair = cr.checkDataConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingQuickColumnAutorepair);
	}

	/**
	 * Test check and autorepair of a missing quick column index. Create a new contentattributetype of type 5 (longtext) to force an index
	 * that needs a keylength.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testMissingQuickColumnWithIndex() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, objecttype) VALUES (?, ?, ?, ?, ?)",
				new Object[] { "test", "5", "1", "quick_test", "10002" });

		boolean testMissingQuickColumn = cr.checkDataConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testMissingQuickColumn);

		boolean testMissingQuickColumnAutorepair = cr.checkDataConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testMissingQuickColumnAutorepair);
	}

	/**
	 * Test check and autorepair an incorrect ic_counter column in contentobject.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testIncorrectIdCounter() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds, "INSERT INTO " + cr.getReferenceTable("contentmap").getTableName() + " (contentid, obj_id, obj_type)  VALUES (?, ?, ?)",
				new Object[] { "10042.132", "132", "10042" });

		runUpdate(ds, "INSERT INTO " + cr.getReferenceTable("contentobject").getTableName() + " (name, type, id_counter)  VALUES (?, ?, ?)",
				new Object[] { "test", "10042", "100" });

		boolean testIncorrectIdCounter = cr.checkDataConsistency(false);

		assertFalse("Error while testing structure: " + appender.getErrors(), testIncorrectIdCounter);

		boolean testIncorrectIdCounterAutorepair = cr.checkDataConsistency(true);

		assertTrue("Error while repairing structure: " + appender.getErrors(), testIncorrectIdCounterAutorepair);
	}

	/**
	 * Test check for conflicting definition of attributetypes (attributetype with same name but different definition).
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testConflictingAttributeTypes() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "test", "1", "0", null, "0", "10002", "0", "", "", "0" });

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "test", "3", "0", null, "0", "10008", "0", "", "", "0" });

		boolean testConflictingAttributeTypes = cr.checkDataConsistency(false);

		assertFalse("Conflicting attributes not detected: " + appender.getErrors(), testConflictingAttributeTypes);
	}

	/**
	 * Test valid definition of linked attributetypes.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testAttributeTypeLinks() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "testlink", "2", "0", null, "0", "10002", "10008", "", "", "0" });

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "testback", "7", "0", null, "0", "10008", "10002", "testlink", "", "0" });

		boolean testAttributeTypes = cr.checkDataConsistency(false);

		assertTrue("Error testing attribute type links: " + appender.getErrors(), testAttributeTypes);
	}

	/**
	 * Test conflicting definition of linked attributetypes.
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testConflictingAttributeTypeLinks() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "testlink", "2", "0", null, "0", "10002", "10009", "", "", "0" });

		runUpdate(ds,
				"INSERT INTO " + cr.getReferenceTable("contentattributetype").getTableName()
				+ " (name, attributetype, optimized, quickname, multivalue, objecttype, linkedobjecttype, foreignlinkattribute, foreignlinkattributerule, exclude_versioning)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { "testback", "7", "0", null, "0", "10008", "10002", "testlink", "", "0" });

		boolean testAttributeTypes = cr.checkDataConsistency(false);

		assertFalse("Error testing conflicting attribute type links: " + appender.getErrors(), testAttributeTypes);
	}

	/**
	 * Test portal connector with invalid repository and sanitycheck2=true and autorepair2=false
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testPortalConnectorInvalidRepository() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// destroy structure
		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		runUpdate(ds, "DROP TABLE " + contentmap.getTableName(), null);
		if (cr instanceof OracleContentRepositoryStructure) {
			try {
				runUpdate(ds, "DROP SEQUENCE " + contentmap.getTableName() + "_SEQUENCE", null);
			} catch (SQLException sqle) {// ignore
			}
		}

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck2", "true");
		Datasource dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNull("Datasource created on invalid repository: " + appender.getErrors(), dsTest);

		dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNull("Datasource created on invalid repository: " + appender.getErrors(), dsTest);
	}

	/**
	 * Test portal connector with invalid repository and sanitycheck2=true and autorepair2=true
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testPortalConnectorInvalidRepositoryRepair() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// destroy structure
		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		runUpdate(ds, "DROP TABLE " + contentmap.getTableName(), null);
		if (cr instanceof OracleContentRepositoryStructure) {
			try {
				runUpdate(ds, "DROP SEQUENCE " + contentmap.getTableName() + "_SEQUENCE", null);
			} catch (SQLException sqle) {// ignore
			}
		}

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("autorepair2", "true");
		Datasource dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNotNull("Autorepair of datasource failed: " + appender.getErrors(), dsTest);

		dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNotNull("Autorepair of datasource failed: " + appender.getErrors(), dsTest);
	}

	/**
	 * Test portal connector with valid repository and sanitycheck2=false and autorepair2=false
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testPortalConnectorValidRepository() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);
		Datasource dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNotNull("Datasource on valid repository not created: " + appender.getErrors(), dsTest);

		dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

		assertNotNull("Datasource on valid repository not created: " + appender.getErrors(), dsTest);
	}

	/**
	 * Do multithreaded test, invalid repository, sanitycheck2=true, autorepair2=false
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	@Ignore("Ignored, because this test sometimes fails (probably a test error)")
	public void testPortalConnectorInvalidRepositoryMultithreaded() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// destroy structure
		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		runUpdate(ds, "DROP TABLE " + contentmap.getTableName(), null);
		if (cr instanceof OracleContentRepositoryStructure) {
			try {
				runUpdate(ds, "DROP SEQUENCE " + contentmap.getTableName() + "_SEQUENCE", null);
			} catch (SQLException sqle) {// ignore
			}
		}

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck2", "true");

		logger.info("Starting threads");
		Collection threads = new Vector();

		reportFailure = false;
		for (int i = 0; i < 100; i++) {
			Thread t = new MultithreadedTestThread(handleProperties, dsProperties, true);

			threads.add(t);
			t.start();
		}

		for (Iterator iterator = threads.iterator(); iterator.hasNext();) {
			Thread t = (Thread) iterator.next();

			try {
				t.join(100000);
			} catch (InterruptedException ie) {//
			}
		}
		logger.info("Main thread done.");
		assertFalse("Not all threads reported invalid repository: " + appender.getErrors(), reportFailure);
	}

	/**
	 * Do multithreaded test, invalid repository, sanitycheck2=true, autorepair2=true
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testPortalConnectorInvalidRepositoryMultithreadedRepair() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		// destroy structure
		TableDefinition contentmap = cr.getReferenceTable("contentmap");

		runUpdate(ds, "DROP TABLE " + contentmap.getTableName(), null);
		if (cr instanceof OracleContentRepositoryStructure) {
			try {
				runUpdate(ds, "DROP SEQUENCE " + contentmap.getTableName() + "_SEQUENCE", null);
			} catch (SQLException sqle) {// ignore
			}
		}

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("autorepair2", "true");

		logger.info("Starting threads");
		Collection threads = new Vector();

		reportFailure = false;
		for (int i = 0; i < 100; i++) {
			Thread t = new MultithreadedTestThread(handleProperties, dsProperties, false);

			threads.add(t);
			t.start();
		}

		for (Iterator iterator = threads.iterator(); iterator.hasNext();) {
			Thread t = (Thread) iterator.next();

			try {
				t.join(100000);
			} catch (InterruptedException ie) {//
			}
		}
		logger.info("Main thread done.");
		assertFalse("Not all threads got valid datasource: " + appender.getErrors(), reportFailure);
	}

	/**
	 * Do multithreaded test, valid repository, sanitycheck2=true, autorepair2=false
	 *
	 * @throws IOException
	 * @throws NodeException
	 * @throws SQLException
	 */
	@Test
	public void testPortalConnectorValidRepositoryMultithreaded() throws IOException, NodeException, SQLException {

		boolean testCreate = cr.checkStructureConsistency(true);

		assertTrue("Error while re-creating tables: " + appender.getErrors(), testCreate);

		appender.reset();

		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("autorepair2", "false");

		logger.info("Starting threads");
		Collection threads = new Vector();

		reportFailure = false;
		for (int i = 0; i < 100; i++) {
			Thread t = new MultithreadedTestThread(handleProperties, dsProperties, false);

			threads.add(t);
			t.start();
		}

		for (Iterator iterator = threads.iterator(); iterator.hasNext();) {
			Thread t = (Thread) iterator.next();

			try {
				t.join(100000);
			} catch (InterruptedException ie) {//
			}
		}
		logger.info("Main thread done.");
		assertFalse("Not all threads got valid datasource: " + appender.getErrors(), reportFailure);
	}

	/**
	 * called by subthreads to report error
	 */
	protected synchronized void reportError() {
		reportFailure = true;
	}

	/**
	 * Subthread trying to create a datasource and reporting error
	 *
	 * @author alexander
	 */
	private class MultithreadedTestThread extends Thread {
		private Properties handleProperties;

		private Map dsProperties;

		private boolean assertNull;

		public MultithreadedTestThread(Properties handleProperties, Map dsProperties, boolean assertNull) {
			this.handleProperties = handleProperties;
			this.dsProperties = dsProperties;
			this.assertNull = assertNull;
		}

		public void run() {
			logger.info("Thread {" + this.getName() + "} starting.");
			try {
				Datasource dsTest = PortalConnectorFactory.createDatasource(handleProperties, dsProperties);

				if (assertNull) {
					if (dsTest != null) {
						reportError();
					}
				} else {
					if (dsTest == null) {
						reportError();
					}
				}
			} catch (Exception e) {
				logger.error("Thread {" + this.getName() + "} threw exception.", e);
				reportError();
			}
			logger.info("Thread {" + this.getName() + "} finished.");
		}
	}

	@Before
	public void setUp() throws Exception {
		appender.reset();

		TestDatabase testDatabase = getTestDatabase();

		sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		sqlUtils.connectDatabase();
		testDatabase.setRandomDatabasename(getClass().getSimpleName());
		sqlUtils.createCRDatabase(getClass());

		ds = createDataSource();
		cr = AbstractContentRepositoryStructure.getStructure(ds, "handleid");
		clearDatabase(ds, cr);
	}

	@After
	public void tearDown() throws SQLUtilException {
		PortalConnectorFactory.destroy();
		sqlUtils.removeDatabase();
		sqlUtils.disconnectDatabase();
	}
}
