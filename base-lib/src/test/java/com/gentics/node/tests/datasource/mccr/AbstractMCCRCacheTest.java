package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for caching in MCCR Datasources
 */
public abstract class AbstractMCCRCacheTest {
	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(MCCRAttributeCacheTest.class);

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FILESYSTEM_BASEPATH = "./target/";

	/**
	 * Map of attribute data
	 */
	public final static Map<String, Object> ATTRIBUTE_DATA = new HashMap<String, Object>();

	/**
	 * Map of modified attribute data
	 */
	public final static Map<String, Object> MODIFIED_ATTRIBUTE_DATA = new HashMap<String, Object>();

	/**
	 * Assertion appender
	 */
	protected AssertionAppender appender;

	/**
	 * Datasource instance, that has caches active
	 */
	protected WritableMCCRDatasource ds;

	/**
	 * Datasource instance, with cache inactive
	 */
	protected WritableMCCRDatasource uncachedDS;

	/**
	 * Directory, in which the fs attributes are stored
	 */
	protected File attributeDirectory;

	/**
	 * ID of the master
	 */
	protected final static int MASTER_ID = 1;

	/**
	 * ID of the channel
	 */
	protected final static int CHANNEL_ID = 2;

	/**
	 * Array of channel ids
	 */
	protected final static int[] CHANNEL_IDS = { MASTER_ID, CHANNEL_ID };

	static {
		try {
			ATTRIBUTE_DATA.put("text", "The quick brown fox");
			ATTRIBUTE_DATA.put("link", "1000.2");
			ATTRIBUTE_DATA.put("int", 42);
			ATTRIBUTE_DATA.put("longtext", "The quick brown fox jumps over the lazy dog");
			ATTRIBUTE_DATA.put("blob", "The quick brown fox".getBytes("UTF-8"));
			ATTRIBUTE_DATA.put("long", 16777216L);
			ATTRIBUTE_DATA.put("double", 3.1415926);
			ATTRIBUTE_DATA.put("date", MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 0));

			ATTRIBUTE_DATA.put("text_opt", "The quick brown fox");
			ATTRIBUTE_DATA.put("link_opt", "1000.2");
			ATTRIBUTE_DATA.put("int_opt", 42);
			ATTRIBUTE_DATA.put("long_opt", 16777216L);
			ATTRIBUTE_DATA.put("double_opt", 3.1415926);
			ATTRIBUTE_DATA.put("date_opt", MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 0));

			ATTRIBUTE_DATA.put("text_multi", Arrays.asList("The quick brown fox - 1", "The quick brown fox - 2"));
			ATTRIBUTE_DATA.put("link_multi", Arrays.asList("1000.2", "1000.3"));
			ATTRIBUTE_DATA.put("int_multi", Arrays.asList(421, 422));
			ATTRIBUTE_DATA.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1", "The quick brown fox jumps over the lazy dog - 2"));
			ATTRIBUTE_DATA.put("blob_multi", Arrays.asList("The quick brown fox - 1".getBytes("UTf-8"), "The quick brown fox - 2".getBytes("UTf-8")));
			ATTRIBUTE_DATA.put("long_multi", Arrays.asList(167772161L, 167772162L));
			ATTRIBUTE_DATA.put("double_multi", Arrays.asList(3.14159261, 3.14159262));
			ATTRIBUTE_DATA.put("date_multi",
					Arrays.asList(MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1), MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 2)));

			ATTRIBUTE_DATA.put("longtext_fs", ATTRIBUTE_DATA.get("longtext"));
			ATTRIBUTE_DATA.put("blob_fs", ATTRIBUTE_DATA.get("blob"));

			ATTRIBUTE_DATA.put("longtext_fs_multi", ATTRIBUTE_DATA.get("longtext_multi"));
			ATTRIBUTE_DATA.put("blob_fs_multi", ATTRIBUTE_DATA.get("blob_multi"));

			MODIFIED_ATTRIBUTE_DATA.put("text", "The quick brown fox - 2");
			MODIFIED_ATTRIBUTE_DATA.put("link", "1000.3");
			MODIFIED_ATTRIBUTE_DATA.put("int", 43);
			MODIFIED_ATTRIBUTE_DATA.put("longtext", "The quick brown fox jumps over the lazy dog - 2");
			MODIFIED_ATTRIBUTE_DATA.put("blob", "The quick brown fox - 2".getBytes("UTF-8"));
			MODIFIED_ATTRIBUTE_DATA.put("long", 16777217L);
			MODIFIED_ATTRIBUTE_DATA.put("double", 3.1415927);
			MODIFIED_ATTRIBUTE_DATA.put("date", MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1));

			MODIFIED_ATTRIBUTE_DATA.put("text_opt", "The quick brown fox - 2");
			MODIFIED_ATTRIBUTE_DATA.put("link_opt", "1000.3");
			MODIFIED_ATTRIBUTE_DATA.put("int_opt", 43);
			MODIFIED_ATTRIBUTE_DATA.put("long_opt", 16777217L);
			MODIFIED_ATTRIBUTE_DATA.put("double_opt", 3.1415927);
			MODIFIED_ATTRIBUTE_DATA.put("date_opt", MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1));

			MODIFIED_ATTRIBUTE_DATA.put("text_multi", Arrays.asList("The quick brown fox - 1.2", "The quick brown fox - 2.2"));
			MODIFIED_ATTRIBUTE_DATA.put("link_multi", Arrays.asList("1000.2", "1000.4"));
			MODIFIED_ATTRIBUTE_DATA.put("int_multi", Arrays.asList(521, 522));
			MODIFIED_ATTRIBUTE_DATA.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1.2", "The quick brown fox jumps over the lazy dog - 2.2"));
			MODIFIED_ATTRIBUTE_DATA.put("blob_multi",
					Arrays.asList("The quick brown fox - 1.2".getBytes("UTf-8"), "The quick brown fox - 2.2".getBytes("UTf-8")));
			MODIFIED_ATTRIBUTE_DATA.put("long_multi", Arrays.asList(167772162L, 167772163L));
			MODIFIED_ATTRIBUTE_DATA.put("double_multi", Arrays.asList(3.14159262, 3.14159263));
			MODIFIED_ATTRIBUTE_DATA.put("date_multi",
					Arrays.asList(MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 2), MCCRTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 3)));

			MODIFIED_ATTRIBUTE_DATA.put("longtext_fs", MODIFIED_ATTRIBUTE_DATA.get("longtext"));
			MODIFIED_ATTRIBUTE_DATA.put("blob_fs", MODIFIED_ATTRIBUTE_DATA.get("blob"));

			MODIFIED_ATTRIBUTE_DATA.put("longtext_fs_multi", MODIFIED_ATTRIBUTE_DATA.get("longtext_multi"));
			MODIFIED_ATTRIBUTE_DATA.put("blob_fs_multi", MODIFIED_ATTRIBUTE_DATA.get("blob_multi"));

		} catch (Exception e) {
			logger.error("Error while initializing data", e);
		}
	}

	/**
	 * Setup:
	 * <ol>
	 *   <li>Create a hsql in memory db</li>
	 *   <li>Create a mccr datasources</li>
	 *   <li>Import test structure</li>
	 *   <li>Clear the caches</li>
	 * </ol>
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		attributeDirectory = new File(FILESYSTEM_BASEPATH, "mccrfstests_" + TestEnvironment.getRandomHash(8));
		attributeDirectory.mkdirs();
		assertTrue("Check existence of " + attributeDirectory.getAbsolutePath(), attributeDirectory.exists());
		FileUtil.cleanDirectory(attributeDirectory);

		appender = new AssertionAppender();
		NodeLogger.getRootLogger().removeAllAppenders();
		NodeLogger.getRootLogger().addAppender(appender);

		Map<String, String> handleProperties = new HashMap<String, String>();

		handleProperties.put("type", "jdbc");
		handleProperties.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProperties.put("url", "jdbc:hsqldb:mem:" + getClass().getSimpleName());
		handleProperties.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProperties = new HashMap<String, String>();

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, attributeDirectory.getAbsolutePath());
		addCachingDatasourceParameters(dsProperties);

		ds = (WritableMCCRDatasource) PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);
		assertNotNull("Datasource creation failed: " + appender.getErrors(), ds);

		// import test structure
		MCCRTestDataHelper.importTypes(ds);

		// create and save the channel structure
		ChannelTree tree = new ChannelTree();
		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(MASTER_ID, "Master"));

		tree.getChildren().add(master);
		ChannelTreeNode channel = new ChannelTreeNode(new DatasourceChannel(CHANNEL_ID, "Channel"));

		master.getChildren().add(channel);
		ds.saveChannelStructure(tree);

		// clear the caches
		ds.clearCaches();

		dsProperties = new HashMap<String, String>();
		dsProperties.put("cache", "false");
		dsProperties.put("autorepair2", "false");
		dsProperties.put("sanitycheck2", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck", "false");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, FILESYSTEM_BASEPATH);
		uncachedDS = (WritableMCCRDatasource) PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);

		TimingUtils.registerJobMonitor();
	}

	/**
	 * Teardown
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		TimingUtils.unregisterJobMonitor();
		PortalConnectorFactory.destroy();
		FileUtil.deleteDirectory(attributeDirectory);
	}

	/**
	 * Set the datasource parameters for the caching datasource. The default implementation will just set "cache" to "true"
	 * @param dsProperties datasource parameter map
	 */
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		dsProperties.put("cache", "true");
	}

	/**
	 * Create an object in the master and channel
	 * @param channelSetId channelset id
	 * @param channelValues values of the attribute "int" for the channels
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void createObject(int channelSetId, int[] channelValues, boolean cachingDS) throws Exception {
		Integer[] iChannelValues = new Integer[channelValues.length];
		for (int i = 0; i < channelValues.length; i++) {
			iChannelValues[i] = channelValues[i];
		}
		createObject(channelSetId, cachingDS, new Attribute<Integer>("int", iChannelValues));
	}

	/**
	 * Create an object in the master and channels
	 * @param channelSetId channelset id
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @param attrs optional list of attributes to set (channel specific)
	 * @throws Exception
	 */
	protected void createObject(int channelSetId, boolean cachingDS, Attribute<?>...attrs) throws Exception {
		WritableMCCRDatasource ds = cachingDS ? this.ds : this.uncachedDS;
		Map<String, Object> dataMap = new HashMap<String, Object>();

		dataMap.put("contentid", "1000." + channelSetId);
		dataMap.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, channelSetId);

		for (int i = 0; i < CHANNEL_IDS.length; i++) {
			int channelId = CHANNEL_IDS[i];
	
			ds.setChannel(channelId);
			for (Attribute<?> attr : attrs) {
				dataMap.put(attr.name, attr.values[i]);
			}
			dataMap.put(WritableMultichannellingDatasource.MCCR_CHANNEL_ID, channelId);
			Changeable masterObject = ds.create(dataMap);

			ds.store(Collections.singleton(masterObject));
		}
	}

	/**
	 * Update the object with given channelset id
	 * @param channelSetId channelset id
	 * @param channelValues channel values
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void updateObject(int channelSetId, int[] channelValues, boolean cachingDS) throws Exception {
		WritableMCCRDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		for (int i = 0; i < CHANNEL_IDS.length; i++) {
			int channelId = CHANNEL_IDS[i];

			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000." + channelSetId, ds);

			assertNotNull("Check that object was found", object);
			object.setProperty("int", channelValues[i]);
			ds.store(Collections.singleton(object));
		}
	}

	/**
	 * Delete the object with given channelset id
	 * @param channelSetId channelset id
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void deleteObject(int channelSetId, boolean cachingDS) throws Exception {
		WritableMCCRDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		for (int i = 0; i < CHANNEL_IDS.length; i++) {
			int channelId = CHANNEL_IDS[i];

			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000." + channelSetId, ds);

			assertNotNull("Check that object was found", object);
			ds.delete(Collections.singleton(object));
		}
	}

	/**
	 * Get the expected attribute data (channel specific)
	 * @param channelId channel id
	 * @param attribute attribute name
	 * @return attribute data
	 */
	protected Object getAttributeData(int channelId, String attribute) {
		return getAttributeData(channelId, attribute, false);
	}

	/**
	 * Get the expected attribute data (channel specific)
	 * @param channelId channel id
	 * @param attribute attribute name
	 * @param modified true if the modified data shall be fetched
	 * @return attribute data
	 */
	protected Object getAttributeData(int channelId, String attribute, boolean modified) {
		// get the map holding the data
		Map<String, Object> map = modified ? MODIFIED_ATTRIBUTE_DATA : ATTRIBUTE_DATA;

		// get the channel unspecific data
		Object data = map.get(attribute);

		// make it channel specific and return
		if (!attribute.startsWith("link")) {
			data = makeChannelSpecific(channelId, data);
		}
		return data;
	}

	/**
	 * Make the given piece of data channel specific
	 * @param channelId channel id
	 * @param data data value
	 * @return channel specific data value
	 */
	protected Object makeChannelSpecific(int channelId, Object data) {
		if (data == null || channelId == 0) {
			return data;
		} else if (data instanceof Collection<?>) {
			Collection<Object> col = (Collection<Object>)data;
			Collection<Object> newCol = new ArrayList<Object>(col);
			for (Object o : col) {
				newCol.add(makeChannelSpecific(channelId, o));
			}
			return newCol;
		} else if (data instanceof String) {
			return data + " for Channel #" + channelId;
		} else if (data instanceof Integer) {
			return ObjectTransformer.getInt(data, 0) + channelId;
		} else {
			return data;
		}
	}

	/**
	 * Helper class for attribute
	 * @param <T> Class of values stored in the attribute
	 */
	protected static class Attribute<T> {
		/**
		 * Attribute name
		 */
		protected String name;

		/**
		 * Channel specific attribute values
		 */
		protected T[] values;

		/**
		 * Create an instance
		 * @param name attribute name
		 * @param values channel specific attribute values
		 */
		public Attribute(String name, @SuppressWarnings("unchecked") T...values) {
			this.name = name;
			this.values = values;
		}
	}
}
