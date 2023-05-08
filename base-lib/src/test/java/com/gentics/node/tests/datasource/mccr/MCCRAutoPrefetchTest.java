package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.node.testutils.QueryCounter;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Tests for auto prefetch of optimized attributes feature
 */
@Category(BaseLibTest.class)
public class MCCRAutoPrefetchTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(MCCRAutoPrefetchTest.class);

	/**
	 * Assertion appender
	 */
	private static AssertionAppender appender;

	/**
	 * Currently used datasource instance
	 */
	private WritableMultichannellingDatasource ds;

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Map of test optimized attributes data
	 */
	public final static Map<String, Object> ATTRIBUTES_DATA = new HashMap<String, Object>();

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FS_BASEPATH = "/tmp/MCCRAutoRefreshTest";

	static {
		try {
			ATTRIBUTES_DATA.put("text_opt", "The quick brown fox");
			ATTRIBUTES_DATA.put("int_opt", 42);
			ATTRIBUTES_DATA.put("long_opt", 16777216L);
			ATTRIBUTES_DATA.put("double_opt", 3.1415926);
			ATTRIBUTES_DATA.put("date_opt", getTimestamp(1972, 9, 25, 10, 35, 0));

			appender = new AssertionAppender();
			NodeLogger.getRootLogger().addAppender(appender);
		} catch (Exception e) {
			logger.error("Error while initializing data", e);
		}
	}

	/**
	 * Get a sql timestamp object for the given date
	 *
	 * @param year
	 *            year
	 * @param month
	 *            month (1 is january)
	 * @param day
	 *            day in the month
	 * @param hour
	 *            hour
	 * @param minute
	 *            minute
	 * @param second
	 *            second
	 * @return timestamp object
	 */
	protected static Timestamp getTimestamp(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();

		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * Create a test instance
	 * @param testDatabase test database
	 * @param attribute tested attribute
	 */
	public MCCRAutoPrefetchTest(TestDatabase testDatabase, String attribute) {
		super(testDatabase);
		this.attribute = attribute;
	}

	/**
	 * Setup the test database if not done before
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		appender.reset();
		TestDatabase testDatabase = getTestDatabase();

		if (!databases.containsKey(testDatabase.getIdentifier())) {
			databases.put(testDatabase.getIdentifier(), testDatabase);
			logger.debug("Creating test database.");
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			logger.debug("Created database with name: {" + sqlUtils.getTestDatabase().getDBName() + "}");

			String definitionFileName = "mccr_tests_structure.xml";

			sqlUtils.connectDatabase();
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			sqlUtils.createDatabase();

			ds = createWritableDataSource();
			ObjectManagementManager.importTypes((MCCRDatasource) ds, getClass().getResourceAsStream(definitionFileName));

			createChannelStructure();

			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = createWritableDataSource();

			// clean the data in the cr
			DBHandle dbHandle = ((MCCRDatasource) ds).getHandle();

			// thanks to referential integrity in the DB, it is sufficient to delete the objects from contentmap
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName());
			((MCCRDatasource) ds).clearCaches();
		}

		// make sure that the filesystem basepath exists and is empty
		File basePath = new File(FS_BASEPATH);

		basePath.mkdirs();
		FileUtil.cleanDirectory(basePath);
	}

	/**
	 * Clean the databases when finished with all tests
	 *
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
	 * Get the test parameters. Every item of the collection will contain the TestDatabase, attribute name, flag that indicates if the autoPrefetch feature will be used.
	 *
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: testDatabase: {0}, attribute: {1}, useAutoPrefetch {2}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = AbstractDatabaseVariationTest.getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDatabase : variations.values()) {
			for (String attribute : ATTRIBUTES_DATA.keySet()) {
				data.add(new Object[] { testDatabase, attribute });
			}
		}

		return data;
	}

	/**
	 * Create channel structure for the tests: master -> channel1 -> channel2
	 *
	 * @throws Exception
	 */
	public void createChannelStructure() throws Exception {
		ChannelTree tree = new ChannelTree();

		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(1, "Master"));

		tree.getChildren().add(master);

		ds.saveChannelStructure(tree);
	}

	/**
	 * Test autoPrefetch feature.
	 *
	 * @throws Exception
	 */
	@Test
	public void testAccessingAutoPrefetchedAttribute() throws Exception {
		doAutoPrefetchTest(ATTRIBUTES_DATA.get(attribute));
	}

	/**
	 * Test autoPrefetch with empty attribute values
	 * @throws Exception
	 */
	@Test
	public void testAccessingEmptyAutoPrefetchedAttribute() throws Exception {
		Object emptyValue = null;
		if (attribute.endsWith("_multi")) {
			emptyValue = Collections.emptyList();
		}
		doAutoPrefetchTest(emptyValue);
	}

	/**
	 * Do the autoprefetch test with the given attribute value
	 * @param value attribute value
	 * @throws Exception
	 */
	protected void doAutoPrefetchTest(Object value) throws Exception {
		String contentId = "1000.1";

		// Initialize parameters for storing the object - we store contentId, channelSetId and the current attribute
		Map<String, Object> params = new HashMap<String, Object>();

		params.put("contentid", contentId);
		params.put(WritableMultichannellingDatasource.MCCR_CHANNELSET_ID, 99);
		params.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, 1);
		params.put(attribute, value);

		// Set the current channel ID
		ds.setChannel(1);

		// Create the object we want to store
		Changeable object = ds.create(params);

		assertNotNull("Check object", object);

		// Do the actual storing
		ds.store(Collections.singleton(object));

		// read the object (should autoprefetch the optimized attribute)
		Resolvable retrievedObject = PortalConnectorFactory.getContentObject(contentId, ds);

		// access the attribute and count DB statements
		QueryCounter counter = new QueryCounter(false, true);
		Object readValue = retrievedObject.get(attribute);

		assertData("Check data", value, readValue);

		// we expect no DB statements (optimized attributes will always be prefetched)
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 *
	 * @param message
	 *            message
	 * @param expected
	 *            expected data
	 * @param data
	 *            actual data
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
		} else if (expected instanceof Resolvable || actual instanceof Resolvable) {
			assertEquals(message, expected.toString(), actual.toString());
		} else {
			assertEquals(message, expected, actual);
		}
	}

	/**
	 * Create writable multichannelling datasource
	 *
	 * @return datasource
	 */
	private WritableMultichannellingDatasource createWritableDataSource() {
		return (WritableMultichannellingDatasource) createDataSource(true);
	}

	/**
	 * Create a multichannelling datasource
	 *
	 * @return datasource
	 */
	private MultichannellingDatasource createDataSource(boolean requestWritableDatasource) {
		Map<String, String> handleProperties = testDatabase.getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, FS_BASEPATH);

		MultichannellingDatasource ds = null;

		// Get normal or writable datasource
		if (requestWritableDatasource) {
			ds = PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);
		} else {
			ds = PortalConnectorFactory.createMultichannellingDatasource(handleProperties, dsProperties);
		}

		if (ds == null) {
			fail("Creation of the datasource failed: " + appender.getErrors());
			return null;
		} else {
			return ds;
		}
	}
}
