package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;
import com.gentics.testutils.infrastructure.TestEnvironment;

@Category(BaseLibTest.class)
public class MCCRBatchInsertTest extends AbstractSingleVariationDatabaseTest {
	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FS_BASEPATH = "/tmp/MCCRBatchInsertTest_" + TestEnvironment.getRandomHash(8);

	/**
	 * Number of objects to insert per channel
	 */
	protected int numObjects;

	/**
	 * True if objects shall be inserted in a batch, false if not
	 */
	protected boolean batch;

	/**
	 * Currently used datasource instance
	 */
	private WritableMCCRDatasource ds;

	/**
	 * Get the test parameters
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: insertTest: {0}, objects {1}, batch {2}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			for (int objects : Arrays.asList(10)) {
				for (boolean batch : Arrays.asList(false, true)) {
					data.add(new Object[] { testDB, objects, batch });
				}
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param testDB test database
	 * @param numObjects number of objects
	 * @param batch true to insert in a batch
	 */
	public MCCRBatchInsertTest(TestDatabase testDB, int numObjects, boolean batch) {
		super(testDB);
		this.numObjects = numObjects;
		this.batch = batch;
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
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

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
		PortalConnectorFactory.destroy();
		for (TestDatabase testDatabase : databases.values()) {
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			sqlUtils.connectDatabase();
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Test inserting objects in batches
	 * @throws Exception
	 */
	@Test
	public void testInsert() throws Exception {
		Collection<Changeable> objects = new ArrayList<Changeable>();
		for (int i = 0; i < numObjects; i++) {
			for (int channelId = 1; channelId <= 3; channelId++) {
				Changeable changeable = createObject(channelId, i + 1, "blablubb", i + 1, i + 10001, Arrays.asList("one", "two", "three"),
						Arrays.asList(i + 1, i + 2, i + 3), Arrays.asList(i + 10001l, i + 10002l, i + 10003l));
				if (!batch) {
					ds.insert(Collections.singleton(changeable));
				}
				objects.add(changeable);
			}
		}
		if (batch) {
			ds.insert(objects);
		}

		ds.clearCaches();

		// check whether the objects were correctly inserted
		for (int channelId = 1; channelId <= 3; channelId++) {
			ds.setChannel(channelId);
			for (int i = 0; i < numObjects; i++) {
				int channelSetId = i + 1;
				String contentId = "1000." + channelSetId;
				assertObject(PortalConnectorFactory.getContentObject(contentId, ds), channelId, channelSetId, "blablubb", i + 1, i + 10001,
						Arrays.asList("one", "two", "three"), Arrays.asList(i + 1, i + 2, i + 3), Arrays.asList(i + 10001l, i + 10002l, i + 10003l));
			}
		}
	}

	@Test
	public void testUpdate() throws Exception {
		// insert first
		Collection<Changeable> objects = new ArrayList<Changeable>();
		for (int i = 0; i < numObjects; i++) {
			for (int channelId = 1; channelId <= 3; channelId++) {
				Changeable changeable = createObject(channelId, i + 1, null, 0, 0, null, null, null);
				if (!batch) {
					ds.insert(Collections.singleton(changeable));
				}
				objects.add(changeable);
			}
		}
		if (batch) {
			ds.insert(objects);
		}
		ds.clearCaches();

		// do an update
		int i = 0;
		for (Changeable changeable : objects) {
			updateObject(changeable, "blablubb", i + 1, i + 10001, Arrays.asList("one", "two", "three"), Arrays.asList(i + 1, i + 2, i + 3),
					Arrays.asList(i + 10001l, i + 10002l, i + 10003l));
			i++;
			if (!batch) {
				ds.update(Collections.singleton(changeable));
			}
		}

		if (batch) {
			ds.update(objects);
		}
		ds.clearCaches();

		// do another update
		for (Changeable changeable : objects) {
			updateObject(changeable,  null, 0, 0, null, null, null);
			if (!batch) {
				ds.update(Collections.singleton(changeable));
			}
		}

		if (batch) {
			ds.update(objects);
		}
	}

	/**
	 * Create an object
	 * @param channelId channel id
	 * @param channelSetId channelset id
	 * @param textAttribute value of the text attribute
	 * @param intAttribute value of the int attribute
	 * @param longAttribute value of the long attribute
	 * @param multiTextAttribute value of the multivalue text attribute
	 * @param multiIntAttribute value of the multivalue int attribute
	 * @param multiLongAttribute value of the multivalue long attribute
	 * @return created object
	 * @throws Exception
	 */
	private Changeable createObject(int channelId, int channelSetId, String textAttribute, int intAttribute, long longAttribute, List<String> multiTextAttribute,
			List<Integer> multiIntAttribute, List<Long> multiLongAttribute) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(WritableMCCRDatasource.MCCR_CHANNEL_ID, channelId);
		data.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, channelSetId);
		data.put("contentid", "1000." + channelSetId);
		data.put("text", textAttribute);
		data.put("longtext", textAttribute);
		data.put("longtext_fs", textAttribute);
		data.put("int", intAttribute);
		data.put("long", longAttribute);
		data.put("double", ObjectTransformer.getDouble(longAttribute, 0));
		data.put("blob", textAttribute != null ? textAttribute.getBytes() : null);
		data.put("blob_fs", textAttribute != null ? textAttribute.getBytes() : null);

		data.put("text_opt", textAttribute);
		data.put("int_opt", intAttribute);
		data.put("long_opt", longAttribute);
		data.put("double_opt", ObjectTransformer.getDouble(longAttribute, 0));

		data.put("text_multi", multiTextAttribute);
		data.put("longtext_multi", multiTextAttribute);
		data.put("longtext_fs_multi", multiTextAttribute);
		data.put("int_multi", multiIntAttribute);
		data.put("long_multi", multiLongAttribute);
		data.put("blob_multi", convertToMultiBlob(multiTextAttribute));
		data.put("blob_fs_multi", convertToMultiBlob(multiTextAttribute));

		Changeable object = ds.create(data);
		return object;
	}

	/**
	 * Update the object
	 * @param object object
	 * @param textAttribute value of the text attribute
	 * @param intAttribute value of the int attribute
	 * @param longAttribute value of the long attribute
	 * @param multiTextAttribute value of the multivalue text attribute
	 * @param multiIntAttribute value of the multivalue int attribute
	 * @param multiLongAttribute value of the multivalue long attribute
	 * @throws Exception
	 */
	private void updateObject(Changeable object, String textAttribute, int intAttribute, long longAttribute, List<String> multiTextAttribute,
			List<Integer> multiIntAttribute, List<Long> multiLongAttribute) throws Exception {
		object.setProperty("text", textAttribute);
		object.setProperty("longtext", textAttribute);
		object.setProperty("longtext_fs", textAttribute);
		object.setProperty("int", intAttribute);
		object.setProperty("long", longAttribute);
		object.setProperty("double", ObjectTransformer.getDouble(longAttribute, 0));
		object.setProperty("blob", textAttribute != null ? textAttribute.getBytes() : null);
		object.setProperty("blob_fs", textAttribute != null ? textAttribute.getBytes() : null);

		object.setProperty("text_opt", textAttribute);
		object.setProperty("int_opt", intAttribute);
		object.setProperty("long_opt", longAttribute);
		object.setProperty("double_opt", ObjectTransformer.getDouble(longAttribute, 0));

		object.setProperty("text_multi", multiTextAttribute);
		object.setProperty("longtext_multi", multiTextAttribute);
		object.setProperty("longtext_fs_multi", multiTextAttribute);
		object.setProperty("int_multi", multiIntAttribute);
		object.setProperty("long_multi", multiLongAttribute);
		object.setProperty("blob_multi", convertToMultiBlob(multiTextAttribute));
		object.setProperty("blob_fs_multi", convertToMultiBlob(multiTextAttribute));
	}

	/**
	 * Assert validity of the stored data
	 * @param resolvable resolvable
	 * @param channelId channel id
	 * @param channelSetId channelset id
	 * @param textAttribute expected text attribute value
	 * @param intAttribute expected int attribute value
	 * @param longAttribute expected long attribute value
	 * @param multiTextAttribute expected multi text attribute value
	 * @param multiIntAttribute expected multi int attribute value
	 * @param multiLongAttribute expected multi long attribute value
	 * @throws Exception
	 */
	private void assertObject(Resolvable resolvable, int channelId, int channelSetId, String textAttribute, int intAttribute, long longAttribute,
			List<String> multiTextAttribute, List<Integer> multiIntAttribute, List<Long> multiLongAttribute) throws Exception {
		String contentId = "1000." + channelSetId;
		assertNotNull("Object " + contentId + " does not exist in channel " + channelId, resolvable);
		assertEquals("Check contentId", contentId, resolvable.get("contentid"));
		assertEquals("Check channel_id", channelId, resolvable.get("channel_id"));
		assertEquals("Check channelset_id", channelSetId, resolvable.get("channelset_id"));

		MCCRTestDataHelper.assertData("Check text attribute", textAttribute, resolvable.get("text"));
		MCCRTestDataHelper.assertData("Check longtext attribute", textAttribute, resolvable.get("longtext"));
		MCCRTestDataHelper.assertData("Check fs longtext attribute", textAttribute, resolvable.get("longtext_fs"));
		MCCRTestDataHelper.assertData("Check int attribute", intAttribute, resolvable.get("int"));
		MCCRTestDataHelper.assertData("Check long attribute", longAttribute, resolvable.get("long"));
		MCCRTestDataHelper.assertData("Check double attribute", ObjectTransformer.getDouble(longAttribute, 0), resolvable.get("double"));
		MCCRTestDataHelper.assertData("Check blob attribute", textAttribute.getBytes(), resolvable.get("blob"));
		MCCRTestDataHelper.assertData("Check fs blob attribute", textAttribute.getBytes(), resolvable.get("blob_fs"));

		MCCRTestDataHelper.assertData("Check text_opt attribute", textAttribute, resolvable.get("text_opt"));
		MCCRTestDataHelper.assertData("Check int_opt attribute", intAttribute, resolvable.get("int_opt"));
		MCCRTestDataHelper.assertData("Check long_opt attribute", longAttribute, resolvable.get("long_opt"));
		MCCRTestDataHelper.assertData("Check double_opt attribute", ObjectTransformer.getDouble(longAttribute, 0), resolvable.get("double_opt"));

		MCCRTestDataHelper.assertData("Check text_multi attribute", multiTextAttribute, resolvable.get("text_multi"));
		MCCRTestDataHelper.assertData("Check longtext_multi attribute", multiTextAttribute, resolvable.get("longtext_multi"));
		MCCRTestDataHelper.assertData("Check fs longtext_multi attribute", multiTextAttribute, resolvable.get("longtext_fs_multi"));
		MCCRTestDataHelper.assertData("Check int_multi attribute", multiIntAttribute, resolvable.get("int_multi"));
		MCCRTestDataHelper.assertData("Check long_multi attribute", multiLongAttribute, resolvable.get("long_multi"));
		MCCRTestDataHelper.assertData("Check blob_multi attribute", convertToMultiBlob(multiTextAttribute), resolvable.get("blob_multi"));
		MCCRTestDataHelper.assertData("Check fs blob_multi attribute", convertToMultiBlob(multiTextAttribute), resolvable.get("blob_fs_multi"));
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
			fail("Creation of the datasource failed");
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
	 * Convert a multitext attribute value to a multiblob attribute value
	 * @param multiText multitext
	 * @return multiblob
	 */
	private List<byte[]> convertToMultiBlob(List<String> multiText) {
		if (multiText == null) {
			return null;
		}
		List<byte[]> multiBlob = new ArrayList<>(multiText.size());
		for (String text : multiText) {
			multiBlob.add(text.getBytes());
		}
		return multiBlob;
	}
}
