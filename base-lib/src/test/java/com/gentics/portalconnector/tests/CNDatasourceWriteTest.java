package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentObjectImpl;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Tests for writing data into contentrepositories
 */
@Category(BaseLibTest.class)
public class CNDatasourceWriteTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(CNDatasourceReadTest.class);

	/**
	 * Currently used datasource instance
	 */
	private WriteableDatasource ds;

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Map of expected data
	 */
	public final static Map<String, Object> EXPECTEDDATA = new HashMap<String, Object>();

	/**
	 * Map of expected modified data
	 */
	public final static Map<String, Object> EXPECTEDMODDATA = new HashMap<String, Object>();

	/**
	 * Map of excluded test cases (keys are the testdb identifiers, values are the attribute names)
	 */
	public final static Map<String, List<String>> EXCLUDEDTESTS = new HashMap<String, List<String>>();

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	static {
		// prepare expected test data
		try {
			EXPECTEDDATA.put("text", "The quick brown fox");
			EXPECTEDDATA.put("link", new GenticsContentObjectImpl("1000.2", null));
			EXPECTEDDATA.put("int", 42);
			EXPECTEDDATA.put("bin", "The quick brown fox");
			EXPECTEDDATA.put("longtext", "The quick brown fox jumps over the lazy dog");
			EXPECTEDDATA.put("blob", "The quick brown fox".getBytes("UTF-8"));
			EXPECTEDDATA.put("long", 16777216L);
			EXPECTEDDATA.put("double", 3.1415926);
			EXPECTEDDATA.put("date", getTimestamp(1972, 9, 25, 10, 35, 0));

			EXPECTEDDATA.put("text_opt", "The quick brown fox");
			EXPECTEDDATA.put("link_opt", new GenticsContentObjectImpl("1000.2", null));
			EXPECTEDDATA.put("int_opt", 42);
			EXPECTEDDATA.put("long_opt", 16777216L);
			EXPECTEDDATA.put("double_opt", 3.1415926);
			EXPECTEDDATA.put("date_opt", getTimestamp(1972, 9, 25, 10, 35, 0));

			EXPECTEDDATA.put("text_multi", Arrays.asList("The quick brown fox - 1", "The quick brown fox - 2"));
			EXPECTEDDATA.put("link_multi", Arrays.asList(new GenticsContentObjectImpl("1000.2", null), new GenticsContentObjectImpl("1000.3", null)));
			EXPECTEDDATA.put("int_multi", Arrays.asList(421, 422));
			EXPECTEDDATA.put("bin_multi", Arrays.asList("The quick brown fox - 1", "The quick brown fox - 2"));
			EXPECTEDDATA.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1", "The quick brown fox jumps over the lazy dog - 2"));
			EXPECTEDDATA.put("blob_multi", Arrays.asList("The quick brown fox - 1".getBytes("UTf-8"), "The quick brown fox - 2".getBytes("UTf-8")));
			EXPECTEDDATA.put("long_multi", Arrays.asList(167772161L, 167772162L));
			EXPECTEDDATA.put("double_multi", Arrays.asList(3.14159261, 3.14159262));
			EXPECTEDDATA.put("date_multi", Arrays.asList(getTimestamp(1972, 9, 25, 10, 35, 1), getTimestamp(1972, 9, 25, 10, 35, 2)));

			EXPECTEDMODDATA.put("text", "The quick green fox");
			EXPECTEDMODDATA.put("link", new GenticsContentObjectImpl("1000.3", null));
			EXPECTEDMODDATA.put("int", 99);
			EXPECTEDMODDATA.put("bin", "The quick green fox");
			EXPECTEDMODDATA.put("longtext", "The quick green fox jumps over the lazy dog");
			EXPECTEDMODDATA.put("blob", "The quick green fox".getBytes("UTf-8"));
			EXPECTEDMODDATA.put("long", 16777217L);
			EXPECTEDMODDATA.put("double", 2.71828);
			EXPECTEDMODDATA.put("date", getTimestamp(1972, 11, 26, 10, 35, 0));

			EXPECTEDMODDATA.put("text_opt", "The quick green fox");
			EXPECTEDMODDATA.put("link_opt", new GenticsContentObjectImpl("1000.3", null));
			EXPECTEDMODDATA.put("int_opt", 99);
			EXPECTEDMODDATA.put("long_opt", 16777217L);
			EXPECTEDMODDATA.put("double_opt", 2.71828);
			EXPECTEDMODDATA.put("date_opt", getTimestamp(1972, 11, 26, 10, 35, 0));

			EXPECTEDMODDATA.put("text_multi", Arrays.asList("The quick green fox - 1", "The quick green fox - 2"));
			EXPECTEDMODDATA.put("link_multi", Arrays.asList(new GenticsContentObjectImpl("1000.3", null), new GenticsContentObjectImpl("1000.4", null)));
			EXPECTEDMODDATA.put("int_multi", Arrays.asList(991, 992));
			EXPECTEDMODDATA.put("bin_multi", Arrays.asList("The quick green fox - 1", "The quick green fox - 2"));
			EXPECTEDMODDATA.put("longtext_multi",
					Arrays.asList("The quick green fox jumps over the lazy dog - 1", "The quick green fox jumps over the lazy dog - 2"));
			EXPECTEDMODDATA.put("blob_multi", Arrays.asList("The quick green fox - 1".getBytes("UTf-8"), "The quick green fox - 2".getBytes("UTf-8")));
			EXPECTEDMODDATA.put("long_multi", Arrays.asList(167772171L, 167772172L));
			EXPECTEDMODDATA.put("double_multi", Arrays.asList(2.718281, 2.718282));
			EXPECTEDMODDATA.put("date_multi", Arrays.asList(getTimestamp(1972, 11, 26, 10, 35, 1), getTimestamp(1972, 11, 26, 10, 35, 2)));

			// exclude the (deprecated) bin attributes for mssql, since they won't work
			// the reason is, that the bin attributes internally treat the values as strings, which causes errors due to unallowed implicit data conversions on MSSQL
			EXCLUDEDTESTS.put("mssql05", Arrays.asList("bin", "bin_multi"));
			EXCLUDEDTESTS.put("mssql08", Arrays.asList("bin", "bin_multi"));
			// also exclude deprecated bin attributes for oracle, they also don't work
			EXCLUDEDTESTS.put("oracle10g", Arrays.asList("bin", "bin_multi"));
			EXCLUDEDTESTS.put("oracle11g", Arrays.asList("bin", "bin_multi"));
			EXCLUDEDTESTS.put("oracle12.2", Arrays.asList("bin", "bin_multi"));
		} catch (Exception e) {
			logger.error("Error while initializing data", e);
		}
	}

	/**
	 * Get a sql timestamp object for the given date
	 * @param year year
	 * @param month month (1 is january)
	 * @param day day in the month
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 * @return timestamp object
	 */
	protected static Timestamp getTimestamp(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();

		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * Default constructor used for parameterized junit tests
	 * @param testDatabase test database
	 * @param attribute name of the tested attribute
	 * @throws SQLUtilException
	 * @throws JDBCMalformedURLException
	 */
	public CNDatasourceWriteTest(TestDatabase testDatabase, String attribute) throws SQLUtilException, JDBCMalformedURLException {
		super(testDatabase);
		this.attribute = attribute;
	}

	/**
	 * Setup the test database if not done before
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		TestDatabase testDatabase = getTestDatabase();

		if (!databases.containsKey(testDatabase.getIdentifier())) {
			databases.put(testDatabase.getIdentifier(), testDatabase);
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			logger.debug("Creating test database.");
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			logger.debug("Created database with name: {" + sqlUtils.getTestDatabase().getDBName() + "}");

			String definitionFileName = "read_test_definition.xml";

			// create the basic CR
			sqlUtils.connectDatabase();
			sqlUtils.createCRDatabase(getClass());
			sqlUtils.selectDatabase();
			ds = getDatasource();
			ObjectManagementManager.importTypes((CNDatasource) ds, getClass().getResourceAsStream(definitionFileName));

			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = getDatasource();

			// clean the data in the cr
			DBHandle dbHandle = ((CNDatasource) ds).getHandle().getDBHandle();

			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName());
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName() + "_nodeversion");
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName());
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentAttributeName() + "_nodeversion");
			((CNDatasource) ds).clearCaches();
		}

		// create possible target objects
		Map<String, Object> data = new HashMap<String, Object>();

		for (String contentId : Arrays.asList("1000.2", "1000.3", "1000.4")) {
			data.put("contentid", contentId);
			Changeable obj = ((CNWriteableDatasource) ds).create(data, -1, false);
			assertNotNull("Link Target object", obj);
			ds.store(Collections.singleton(obj));
		}
	}

	/**
	 * Clean the databases when finished with all tests
	 * @throws Exception
	 */
	@AfterClass
	public static void tearDownOnce() throws Exception {
		logger.debug("Cleanup of test databases.");
		PortalConnectorFactory.destroy();
		for (TestDatabase testDatabase : databases.values()) {
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			sqlUtils.connectDatabase();
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Get the test parameters. Every item of the collection will contain the TestDatabase and attribute name
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}, {1}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = AbstractDatabaseVariationTest.getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			for (String attribute : EXPECTEDDATA.keySet()) {
				if (!isExcluded(testDB, attribute)) {
					data.add(new Object[] { testDB, attribute});
				}
			}
		}
		return data;
	}

	/**
	 * Check whether a specific testDB/attribute conbination is excluded
	 * @param testDB test database
	 * @param attribute attribute name
	 * @return true if the test is excluded, false if not
	 */
	public static boolean isExcluded(TestDatabase testDB, String attribute) {
		if (!EXCLUDEDTESTS.containsKey(testDB.getIdentifier())) {
			return false;
		} else {
			return EXCLUDEDTESTS.get(testDB.getIdentifier()).contains(attribute);
		}
	}

	/**
	 * Test reading single value non optimized string data
	 * @throws Exception
	 */
	@Test
	public void testWriteData() throws Exception {
		// create the object
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("obj_type", 1000);
		data.put(attribute, EXPECTEDDATA.get(attribute));
		Changeable obj = ds.create(data);

		ds.store(Collections.singleton(obj));

		// clear caches, and read the object again
		((CNDatasource) ds).clearCaches();
		Resolvable result = PortalConnectorFactory.getContentObject(ObjectTransformer.getString(obj.get("contentid"), null), ds);

		// read the written data
		assertData("Check data after insert", EXPECTEDDATA.get(attribute), result.get(attribute));

		// update data
		obj.setProperty(attribute, EXPECTEDMODDATA.get(attribute));
		ds.store(Collections.singleton(obj));

		// clear caches, and read the object again
		((CNDatasource) ds).clearCaches();
		result = PortalConnectorFactory.getContentObject(ObjectTransformer.getString(obj.get("contentid"), null), ds);

		// read the written data
		assertData("Check data after update", EXPECTEDMODDATA.get(attribute), result.get(attribute));

		// update data to null
		obj.setProperty(attribute, null);
		ds.store(Collections.singleton(obj));

		// clear caches, and read the object again
		((CNDatasource) ds).clearCaches();
		result = PortalConnectorFactory.getContentObject(ObjectTransformer.getString(obj.get("contentid"), null), ds);

		// read the written data
		Object expectedNullValue = null;

		if (attribute.endsWith("_multi")) {
			expectedNullValue = Collections.emptyList();
		}
		assertData("Check data after setting null", expectedNullValue, result.get(attribute));
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 * @param message message
	 * @param expected expected data
	 * @param data actual data
	 */
	protected void assertData(String message, Object expected, Object actual) {
		if (expected instanceof byte[] && actual instanceof byte[]) {
			byte[] expectedBytes = (byte[]) expected;
			byte[] actualBytes = (byte[]) actual;

			assertEquals(message + " length", expectedBytes.length, actualBytes.length);
			for (int i = 0; i < expectedBytes.length; i++) {
				assertEquals(message + " data[" + i + "]", expectedBytes[i], actualBytes[i]);
			}
		} else if (expected instanceof List && actual instanceof List) {
			List<?> expectedColl = (List<?>) expected;
			List<?> actualColl = (List<?>) actual;

			assertEquals(message + " length", expectedColl.size(), actualColl.size());
			for (int i = 0; i < expectedColl.size(); i++) {
				assertData(message + " data[" + i + "]", expectedColl.get(i), actualColl.get(i));
			}
		} else if (expected instanceof BigDecimal || actual instanceof BigDecimal) {
			// if at least one value is a BigDecimal, we transform both into BigDecimals and compare then (Oracle returns all numbers as BigDecimal)
			assertEquals(message, ObjectTransformer.getDouble(expected, null), ObjectTransformer.getDouble(actual, null));
		} else {
			assertEquals(message, expected, actual);
		}
	}

	/**
	 * Create the datasource
	 *
	 * @return datasource
	 * @throws Exception
	 */
	protected WriteableDatasource getDatasource() throws Exception {
		TestDatabase testDatabase = getTestDatabase();
		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		return PortalConnectorFactory.createWriteableDatasource(handleProperties, dsProperties);
	}
}
