package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for updating attributes of objects
 */
@Category(BaseLibTest.class)
public class MCCRUpdateAttributeTest extends AbstractSingleVariationDatabaseTest {
	/**
	 * Number of objects (must not exceed 10)
	 */
	public final static int NUM_OBJECTS = 10;

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(AbstractMCCRDatasourceWriteTest.class);

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FS_BASEPATH = "/tmp/MCCRUpdateAttributeTest_" + TestEnvironment.getRandomHash(8);

	/**
	 * Assertion appender
	 */
	private static AssertionAppender appender;

	/**
	 * Currently used datasource instance
	 */
	private WritableMCCRDatasource ds;

	/**
	 * Flag for whether the test shall prepare the update
	 */
	private boolean prepareUpdate = false;

	/**
	 * Flag for whether the test shall change attribute values before updating
	 */
	private boolean changeAttribute = false;

	/**
	 * Flag for whether updates shall be done in batches
	 */
	private boolean batchUpdate = false;

	/**
	 * Text values for the created objects
	 */
	private String[] textValues = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

	/**
	 * Multivalue Text values for the objects
	 */
	private List<List<String>> multiTextValues = Arrays.asList(
		Arrays.asList("one", "two", "three"),
		Arrays.asList("two", "three", "four"),
		Arrays.asList("three", "four", "five"),
		Arrays.asList("four", "five", "six"),
		Arrays.asList("five", "six", "seven"),
		Arrays.asList("six", "seven", "eight"),
		Arrays.asList("seven", "eight", "nine"),
		Arrays.asList("eight", "nine", "ten"),
		Arrays.asList("nine", "ten", "eleven"),
		Arrays.asList("ten", "eleven", "twelve")
	);

	/**
	 * Get the test parameters
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: updateTest: {0}, prepare: {1}, change: {2}, batch: {3}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			for (boolean prepareUpdate : Arrays.asList(true, false)) {
				for (boolean changeAttribute : Arrays.asList(true, false)) {
					for (boolean batchUpdate : Arrays.asList(true, false)) {
						data.add(new Object[] { testDB, prepareUpdate, changeAttribute, batchUpdate });
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create an instance of the test class
	 * @param testDatabase test database
	 * @param prepareUpdate true when the test shall prepare the update
	 * @param changeAttribute true when the test shall change attribute values
	 * @param batchUpdate true for doing updates in batches, false for single updates
	 */
	public MCCRUpdateAttributeTest(TestDatabase testDatabase, boolean prepareUpdate, boolean changeAttribute, boolean batchUpdate) {
		super(testDatabase);
		this.prepareUpdate = prepareUpdate;
		this.changeAttribute = changeAttribute;
		this.batchUpdate = batchUpdate;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		appender = new AssertionAppender();
		NodeLogger.getRootLogger().addAppender(appender);
	}

	/**
	 * Setup the test database if not done before
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
			ObjectManagementManager.importTypes(ds, getClass().getResourceAsStream(definitionFileName));

			createChannelStructure();

			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = createWritableDataSource();

			// clean the data in the cr
			DBHandle dbHandle = ds.getHandle();

			// thanks to referential integrity in the DB, it is sufficient to delete the objects from contentmap
			DB.update(dbHandle, "DELETE FROM " + dbHandle.getContentMapName());
			ds.clearCaches();
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
	 * Test updating attribute values
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		ds.setCache(false);
		// create the objects
		for (int i = 0; i < NUM_OBJECTS; i++) {
			int changed = storeObject(1, i + 1, textValues[i], i + 1, i + 10001, multiTextValues.get(i), Arrays.asList(i + 1, i + 2, i + 3),
					Arrays.asList(i + 10001, i + 10002, i + 10003));
			assertTrue("Object was not saved", changed == 1);
		}

		QueryCounter counter = getQueryCounter(false);
		ds.setCache(true);
		if (prepareUpdate) {
			counter.counter = 0;
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), null);
			assertEquals("Check # of records", NUM_OBJECTS, result.size());
			MCCRHelper.prepareForUpdate(result);
			// we expect 3 SQL Statements for preparing data:
			// 1 for checking existence of the object
			// 1 for getting the existing non optimized attribute data
			// 1 for getting the existing optimized attribute data
			assertEquals("Check # of SQL Statements for preparing data for update", 3, counter.counter);
		}
		int expectedSQLStatements = 0;
		counter.counter = 0;

		Collection<Changeable> toStore = new ArrayList<Changeable>();

		for (int i = 0; i < NUM_OBJECTS; i++) {
			if (changeAttribute) {
				if (batchUpdate) {
					toStore.add(getObjectToStore(1, i + 1, textValues[i] + " modified", i + 2, i + 10002,
							Arrays.asList(multiTextValues.get(i).get(2), multiTextValues.get(i).get(1), multiTextValues.get(i).get(0)),
							Arrays.asList(i + 3, i + 2, i + 1), Arrays.asList(i + 10003, i + 10002, i + 10001)));
				} else {
					int changed = storeObject(1, i + 1, textValues[i] + " modified", i + 2, i + 10002,
							Arrays.asList(multiTextValues.get(i).get(2), multiTextValues.get(i).get(1), multiTextValues.get(i).get(0)),
							Arrays.asList(i + 3, i + 2, i + 1), Arrays.asList(i + 10003, i + 10002, i + 10001));

					assertTrue("Object was not saved", changed == 1);
				}
				// we expect 11 Statements:
				// 3 for updating the non optimized singlevalue attribute values
				// 6 for updating the multivalue attribute values (3 attributes, 2 values change for each)
				// 1 for updating the object itself
				// 1 for updating the change timestamp of the channel (only if not done in a batch)
				expectedSQLStatements += batchUpdate ? 10 : 11;
			} else {
				if (batchUpdate) {
					toStore.add(getObjectToStore(1, i + 1, textValues[i], i + 1, i + 10001, multiTextValues.get(i), Arrays.asList(i + 1, i + 2, i + 3),
							Arrays.asList(i + 10001, i + 10002, i + 10003)));
				} else {
					int changed = storeObject(1, i + 1, textValues[i], i + 1, i + 10001, multiTextValues.get(i), Arrays.asList(i + 1, i + 2, i + 3),
							Arrays.asList(i + 10001, i + 10002, i + 10003));
					assertTrue("Object was modified although no attributes were changed", changed == 0);
				}
			}
			if (!prepareUpdate) {
				// when we do not prepare the objects for update, we expect 3 (additional) SQL Statements:
				// 1 for checking existence of the object
				// 1 for getting the existing non optimized attribute data
				// 1 for getting the existing optimized attribute data
				expectedSQLStatements += 3;
			}
		}

		if (batchUpdate) {
			if (changeAttribute) {
				expectedSQLStatements++; // we expect one single statement for updating the timestamp of the channel
			}
			int changed = ds.store(toStore).getAffectedRecordCount();
			assertEquals("Check # of changed Objects", changeAttribute ? NUM_OBJECTS : 0, changed);
		}

		assertEquals("Check # of DB statements", expectedSQLStatements, counter.counter);

		if (prepareUpdate) {
			MCCRHelper.resetPreparedForUpdate();
		}
		ds.setCache(false);
	}

	/**
	 * Create writable multichannelling datasource
	 * @return datasource
	 */
	private WritableMCCRDatasource createWritableDataSource() {
		return (WritableMCCRDatasource) createDataSource(true);
	}

	/**
	 * Create a multichannelling datasource
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

	/**
	 * Create channel structure for the tests: master -> channel1 -> channel2
	 *
	 * @throws Exception
	 */
	private void createChannelStructure() throws Exception {
		ChannelTree tree = new ChannelTree();

		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(1, "Master"));

		tree.getChildren().add(master);

		ChannelTreeNode channel1 = new ChannelTreeNode(new DatasourceChannel(2, "Channel 1"));

		master.getChildren().add(channel1);

		ChannelTreeNode channel2 = new ChannelTreeNode(new DatasourceChannel(3, "Channel 2"));

		channel1.getChildren().add(channel2);

		ds.saveChannelStructure(tree);
	}

	/**
	 * Store an object
	 * @param channelId channel id
	 * @param channelSetId channelset id
	 * @param textAttribute value of the text attribute
	 * @param intAttribute value of the int attribute
	 * @param longAttribute value of the long attribute
	 * @param multiTextAttribute value of the multivalue text attribute
	 * @param multiIntAttribute value of the multivalue int attribute
	 * @param multiLongAttribute value of the multivalue long attribute
	 * @return number of changed objects
	 * @throws Exception
	 */
	private int storeObject(int channelId, int channelSetId, String textAttribute, int intAttribute, long longAttribute, List<String> multiTextAttribute,
			List<Integer> multiIntAttribute, List<Integer> multiLongAttribute) throws Exception {
		return ds.store(
				Collections.singleton(getObjectToStore(channelId, channelSetId, textAttribute, intAttribute, longAttribute, multiTextAttribute,
						multiIntAttribute, multiLongAttribute))).getAffectedRecordCount();
	}

	/**
	 * Get the object to store
	 * @param channelId channel id
	 * @param channelSetId channelset id
	 * @param textAttribute value of the text attribute
	 * @param intAttribute value of the int attribute
	 * @param longAttribute value of the long attribute
	 * @param multiTextAttribute value of the multivalue text attribute
	 * @param multiIntAttribute value of the multivalue int attribute
	 * @param multiLongAttribute value of the multivalue long attribute
	 * @return object to store
	 * @throws Exception
	 */
	private Changeable getObjectToStore(int channelId, int channelSetId, String textAttribute, int intAttribute, long longAttribute, List<String> multiTextAttribute,
			List<Integer> multiIntAttribute, List<Integer> multiLongAttribute) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(WritableMCCRDatasource.MCCR_CHANNEL_ID, channelId);
		data.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, channelSetId);
		data.put("contentid", "1000." + channelSetId);
		data.put("text", textAttribute);
		data.put("int", intAttribute);
		data.put("long", longAttribute);
		data.put("text_opt", textAttribute);
		data.put("int_opt", intAttribute);
		data.put("long_opt", longAttribute);
		data.put("text_multi", multiTextAttribute);
		data.put("int_multi", multiIntAttribute);
		data.put("long_multi", multiLongAttribute);
		return ds.create(data);
	}

	/**
	 * Get a query counter for the DB queries issued from now on
	 * @param logStatements true if statements should be logged as well
	 * @return query counter
	 * @throws DatasourceException
	 */
	protected QueryCounter getQueryCounter(boolean logStatements) throws DatasourceException {
		// use the logging mechanism to count the number of statements
		NodeLogger l = NodeLogger.getNodeLogger(DB.class);

		l.setLevel(Level.DEBUG);
		QueryCounter counter = new QueryCounter(logStatements, ds.getHandle().getDummyStatement());

		l.removeAllAppenders();
		l.addAppender(counter);

		return counter;
	}

	/**
	 * Internal helper class that can be used as appender to the DB logger to count the number of issued SQL queries
	 */
	protected class QueryCounter extends AbstractAppender {

		/**
		 * counter for SQL queries
		 */
		protected int counter = 0;

		/**
		 * Statements
		 */
		protected StringBuffer statements = null;

		/**
		 * Dummy Statement, just used to check whether a db handle is still alive
		 * Will not be counted
		 */
		protected String dummyStatement = null;

		/**
		 * Create an instance
		 * @param logStatements true if statements should be logged as well
		 * @param dummyStatement dummy statement (will not be counted)
		 */
		public QueryCounter(boolean logStatements, String dummyStatement) {
			super(UUID.randomUUID().toString(), null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
			if (logStatements) {
				statements = new StringBuffer();
			}
			this.dummyStatement = dummyStatement;
		}

		@Override
		public void append(LogEvent event) {
			if (isStarted()) {
				String message = event.getMessage().getFormattedMessage();

				if (message.startsWith("sql")) {
					// omit the dummy statement
					if (dummyStatement != null && message.endsWith(dummyStatement)) {
						return;
					}
					counter++;
					if (statements != null) {
						statements.append(message).append("\n");
					}
				}
			}
		}
	}
}
