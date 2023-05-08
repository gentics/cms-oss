package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
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
 * Test cases for changing attribute type changes
 */
@Category(BaseLibTest.class)
public class MCCRAttributeTypeChangeTest extends AbstractSingleVariationDatabaseTest {
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
	public final static String FS_BASEPATH = "/tmp/MCCROptimizedAttributeTest_" + TestEnvironment.getRandomHash(8);

	/**
	 * Assertion appender
	 */
	private static AssertionAppender appender;

	/**
	 * Currently used datasource instance
	 */
	private WritableMCCRDatasource ds;

	/**
	 * Text values for the created objects
	 */
	private String[] textValues = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

	/**
	 * Multivalue Text values for the objects
	 */
	@SuppressWarnings("unchecked")
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
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			data.add(new Object[] { testDB });
		}
		return data;
	}

	/**
	 * Create an instance of the test class
	 * @param testDatabase test database
	 */
	public MCCRAttributeTypeChangeTest(TestDatabase testDatabase) {
		super(testDatabase);
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
	 * Do the migration test:
	 * <ol>
	 * <li>Create objects</li>
	 * <li>Check whether the given attribute has the expected value for 'optimized'</li>
	 * <li>Change the value for 'optimized'</li>
	 * <li>Read the objects, check whether attribute values still can be read</li>
	 * <li>Check in the database whether contentattribute entries are present/not present</li>
	 * </ol>
	 * @param attribute name of the attribute
	 * @param optimized true if the attribute is expected to be optimized at the start of the test
	 * @throws Exception
	 */
	protected void doMigrationTest(String attribute, boolean optimized) throws Exception {
		// create the objects
		for (int i = 0; i < NUM_OBJECTS; i++) {
			int changed = storeObject(1, i + 1, textValues[i], i + 1, i + 10001, multiTextValues.get(i), Arrays.asList(i + 1, i + 2, i + 3),
					Arrays.asList(i + 10001, i + 10002, i + 10003));
			assertTrue("Object was not saved", changed == 1);
		}

		Collection<ObjectTypeBean> types = ObjectManagementManager.loadObjectTypes(ds, true);
		for (ObjectTypeBean type : types) {
			Map<String, ObjectAttributeBean> attrTypes = type.getAttributeTypesMap();
			ObjectAttributeBean attrTypeToChange = attrTypes.get(attribute);
			assertNotNull("Attribute type to change was not found", attrTypeToChange);
			assertEquals("Check 'optimized' flag for attribute " + attribute, optimized, attrTypeToChange.getOptimized());
			attrTypeToChange.setOptimized(!optimized);
			assertTrue("Saving the changed attribute type failed", ObjectManagementManager.save(ds, type, true, true, false));
		}

		// read the objects
		for (int i = 0; i < NUM_OBJECTS; i++) {
			String contentId = "1000." + (i + 1);
			Changeable changeable = PortalConnectorFactory.getChangeableContentObject(contentId, ds);
			assertNotNull("Failed to read object " + contentId, changeable);
			assertEquals("Check value of attribute " + attribute, textValues[i], changeable.get(attribute));
		}

		// Check the number of entries in table contentattribute
		DBHandle handle = ds.getHandle();
		SimpleResultProcessor proc = new SimpleResultProcessor();
		DB.query(handle, "SELECT count(*) c FROM " + handle.getContentAttributeName() + " WHERE name = ?", new Object[] {attribute}, proc);
		assertTrue("Failed to count entries in contentattribute", proc.size() == 1);
		// if the attribute was optimized before, it is not now, so we expect entries in table contentattribute (one for each object)
		// if the attribute was not optimized before, it is now, so we expect no entries in table contentattribute
		assertEquals("Check # of entries in contentattribute", optimized ? NUM_OBJECTS : 0, proc.getRow(1).getInt("c"));
	}

	/**
	 * Test changing an optimized to a non optimized attribute
	 * @throws Exception
	 */
	@Test
	public void testOptimizedToNonOptimized() throws Exception {
		doMigrationTest("text_opt", true);
	}

	/**
	 * Test changing a non optimized to an optimized attribute
	 * @throws Exception
	 */
	@Test
	public void testNonOptimizedToOptimized() throws Exception {
		doMigrationTest("text", false);
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
		Changeable object = ds.create(data);
		return ds.store(Collections.singleton(object)).getAffectedRecordCount();
	}
}
