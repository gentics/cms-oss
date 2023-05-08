package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentObjectImpl;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.etc.StringUtils;
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
 * Tests for reading attributes from CNDatasources. This parameterized test sets up its own contentrepositories. Since the test cases only read data, every
 * contentrepository is only setup once for the lifetime of the test.
 */
@Category(BaseLibTest.class)
public class CNDatasourceReadTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(CNDatasourceReadTest.class);

	/**
	 * Currently used datasource instance
	 */
	private Datasource ds;

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Map of expected data
	 */
	public final static Map<String, Object> EXPECTEDDATA = new HashMap<String, Object>();

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Map of db identifiers to dump filenames
	 */
	public static Map<String, String> dumpFileNames = new HashMap<String, String>();

	static {
		dumpFileNames.put("generic", "read_test_data.sql");
		dumpFileNames.put("oracle11g", "read_test_data_oracle.sql");
		dumpFileNames.put("oracle10g", "read_test_data_oracle.sql");
		dumpFileNames.put("oracle12.2", "read_test_data_oracle.sql");
	}

	static {
		// prepare expected test data
		try {
			EXPECTEDDATA.put("text", "The quick brown fox");
			EXPECTEDDATA.put("link", new GenticsContentObjectImpl("1000.2", null));
			EXPECTEDDATA.put("int", 42);
			EXPECTEDDATA.put("bin", "The quick brown fox");
			EXPECTEDDATA.put("longtext", "The quick brown fox jumps over the lazy dog");
			EXPECTEDDATA.put("blob", "The quick brown fox".getBytes("UTf-8"));
			EXPECTEDDATA.put("foreignlink", Arrays.asList(new GenticsContentObjectImpl("1000.2", null), new GenticsContentObjectImpl("1000.3", null)));
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
	public CNDatasourceReadTest(TestDatabase testDatabase, String attribute) throws SQLUtilException, JDBCMalformedURLException {
		super(testDatabase);
		this.attribute = attribute;
	}

	/**
	 * Get the dump filename for the test database
	 * @return dump filename
	 */
	public String getDumpFileName() {
		TestDatabase testDatabase = getTestDatabase();

		if (dumpFileNames.containsKey(testDatabase.getIdentifier())) {
			return dumpFileNames.get(testDatabase.getIdentifier());
		} else {
			return dumpFileNames.get("generic");
		}
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
			logger.debug("Creating test database.");
			final SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			sqlUtils.connectDatabase();
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			logger.debug("Created database with name: {" + sqlUtils.getTestDatabase().getDBName() + "}");

			String definitionFileName = "read_test_definition.xml";
			String dataFileName = getDumpFileName();

			// create the basic CR
			sqlUtils.createCRDatabase(getClass());
			sqlUtils.selectDatabase();
			ds = getDatasource();
			ObjectManagementManager.importTypes((CNDatasource) ds, getClass().getResourceAsStream(definitionFileName));

			BufferedReader dataReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(dataFileName), "UTF-8"));
			String statement = null;

			while ((statement = dataReader.readLine()) != null) {
				statement = statement.trim();
				if (!StringUtils.isEmpty(statement)) {
					sqlUtils.executeQueryManipulation(statement);
				}
			}

			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = getDatasource();
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
				data.add(new Object[] { testDB, attribute});
			}
		}
		return data;
	}

	/**
	 * Test reading single value non optimized string data
	 * @throws Exception
	 */
	@Test
	public void testReadData() throws Exception {
		Resolvable obj = PortalConnectorFactory.getContentObject("1000.1", ds);

		assertData("Check data", EXPECTEDDATA.get(attribute), obj.get(attribute), !"foreignlink".equals(attribute));
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 * @param message message
	 * @param expected expected data
	 * @param actual actual data
	 * @param listOrderRelevant true if the list order is relevant (for multivalue)
	 */
	protected void assertData(String message, Object expected, Object actual, boolean listOrderRelevant) {
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

			if (listOrderRelevant) {
				// when the list order is relevant, we compare the items per index
				for (int i = 0; i < expectedColl.size(); i++) {
					assertData(message + " data[" + i + "]", expectedColl.get(i), actualColl.get(i), listOrderRelevant);
				}
			} else {
				// when the list order is not relevant, we check whether each list contains all elements of the other
				for (Object expItem : expectedColl) {
					assertTrue(message + " expected item " + expItem, actualColl.contains(expItem));
				}
				for (Object actItem : actualColl) {
					assertTrue(message + " actual item " + actItem, expectedColl.contains(actItem));
				}
			}
		} else if (expected instanceof Number && actual instanceof Number) {
			assertEquals(message, ((Number) expected).doubleValue(), ((Number) actual).doubleValue(), 0.0000001);
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
	protected Datasource getDatasource() throws Exception {
		TestDatabase testDatabase = getTestDatabase();
		Properties handleProperties = testDatabase.getSettings();
		Map dsProperties = new HashMap(handleProperties);

		dsProperties.put("sanitycheck", "false");
		return PortalConnectorFactory.createDatasource(handleProperties, dsProperties);
	}
}
