package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for caching in MCCR Datasources
 */
public abstract class AbstractCNCacheTest {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(AbstractCNCacheTest.class);

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
	private AssertionAppender appender;

	/**
	 * Datasource instance, that has caches active
	 */
	protected CNWriteableDatasource ds;

	/**
	 * Datasource instance, with cache inactive
	 */
	protected CNWriteableDatasource uncachedDS;

	/**
	 * Directory, in which the fs attributes are stored
	 */
	protected File attributeDirectory;

	static {
		try {
			ATTRIBUTE_DATA.put("text", "The quick brown fox");
			ATTRIBUTE_DATA.put("link", "1000.2");
			ATTRIBUTE_DATA.put("int", 42);
			ATTRIBUTE_DATA.put("longtext", "The quick brown fox jumps over the lazy dog");
			ATTRIBUTE_DATA.put("blob", "The quick brown fox".getBytes("UTF-8"));
			ATTRIBUTE_DATA.put("long", 16777216L);
			ATTRIBUTE_DATA.put("double", 3.1415926);
			ATTRIBUTE_DATA.put("date", CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 0));

			ATTRIBUTE_DATA.put("text_opt", "The quick brown fox");
			ATTRIBUTE_DATA.put("link_opt", "1000.2");
			ATTRIBUTE_DATA.put("int_opt", 42);
			ATTRIBUTE_DATA.put("long_opt", 16777216L);
			ATTRIBUTE_DATA.put("double_opt", 3.1415926);
			ATTRIBUTE_DATA.put("date_opt", CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 0));

			ATTRIBUTE_DATA.put("text_multi", Arrays.asList("The quick brown fox - 1", "The quick brown fox - 2"));
			ATTRIBUTE_DATA.put("link_multi", Arrays.asList("1000.2", "1000.3"));
			ATTRIBUTE_DATA.put("int_multi", Arrays.asList(421, 422));
			ATTRIBUTE_DATA.put("longtext_multi",
					Arrays.asList("The quick brown fox jumps over the lazy dog - 1", "The quick brown fox jumps over the lazy dog - 2"));
			ATTRIBUTE_DATA.put("blob_multi", Arrays.asList("The quick brown fox - 1".getBytes("UTf-8"), "The quick brown fox - 2".getBytes("UTf-8")));
			ATTRIBUTE_DATA.put("long_multi", Arrays.asList(167772161L, 167772162L));
			ATTRIBUTE_DATA.put("double_multi", Arrays.asList(3.14159261, 3.14159262));
			ATTRIBUTE_DATA.put("date_multi",
					Arrays.asList(CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1), CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 2)));

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
			MODIFIED_ATTRIBUTE_DATA.put("date", CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1));

			MODIFIED_ATTRIBUTE_DATA.put("text_opt", "The quick brown fox - 2");
			MODIFIED_ATTRIBUTE_DATA.put("link_opt", "1000.3");
			MODIFIED_ATTRIBUTE_DATA.put("int_opt", 43);
			MODIFIED_ATTRIBUTE_DATA.put("long_opt", 16777217L);
			MODIFIED_ATTRIBUTE_DATA.put("double_opt", 3.1415927);
			MODIFIED_ATTRIBUTE_DATA.put("date_opt", CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 1));

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
					Arrays.asList(CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 2), CNTestDataHelper.getTimestamp(1972, 9, 25, 10, 35, 3)));

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
		attributeDirectory = new File(FILESYSTEM_BASEPATH, "cndsfstests_" + TestEnvironment.getRandomHash(8));
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
		dsProperties.put("setUpdatetimestampOnWrite", "true");
		dsProperties.put(CNDatasource.ATTRIBUTE_PATH, attributeDirectory.getAbsolutePath());
		addCachingDatasourceParameters(dsProperties);

		ds = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(handleProperties, dsProperties);
		assertNotNull("Datasource creation failed: " + appender.getErrors(), ds);

		// import test structure
		CNTestDataHelper.importTypes(ds);

		// clear the caches
		ds.clearCaches();

		dsProperties = new HashMap<String, String>();
		dsProperties.put("cache", "false");
		dsProperties.put("autorepair2", "false");
		dsProperties.put("sanitycheck2", "false");
		dsProperties.put("autorepair", "false");
		dsProperties.put("sanitycheck", "false");
		dsProperties.put("setUpdatetimestampOnWrite", "true");
		dsProperties.put(CNDatasource.ATTRIBUTE_PATH, FILESYSTEM_BASEPATH);
		uncachedDS = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(handleProperties, dsProperties);

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
	 * Create an object with a value
	 * @param contentId content id
	 * @param value value of the attribute named "int"
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void createObject(String contentId, int value, boolean cachingDS) throws Exception {
		CNWriteableDatasource ds = cachingDS ? this.ds : this.uncachedDS;
		Map<String, Object> dataMap = new HashMap<String, Object>();

		dataMap.put("contentid", contentId);
		dataMap.put("int", value);
		Changeable object = ds.create(dataMap, -1, false);

		ds.store(Collections.singleton(object));
	}

	/**
	 * Update the object with given contentId
	 * @param contentId contentid
	 * @param value value of the attribute named "int"
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void updateObject(String contentId, int value, boolean cachingDS) throws Exception {
		CNWriteableDatasource ds = cachingDS ? this.ds : this.uncachedDS;
		Changeable object = PortalConnectorFactory.getChangeableContentObject(contentId, ds);

		assertNotNull("Check that object was found", object);
		object.setProperty("int", value);
		ds.store(Collections.singleton(object));
	}

	/**
	 * Delete the object with given contentId
	 * @param contentId contentid
	 * @param cachingDS true to use the caching ds, false to use the other one
	 * @throws Exception
	 */
	protected void deleteObject(String contentId, boolean cachingDS) throws Exception {
		CNWriteableDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		Changeable object = PortalConnectorFactory.getChangeableContentObject(contentId, ds);

		assertNotNull("Check that object was found", object);
		ds.delete(Collections.singleton(object));
	}
}
