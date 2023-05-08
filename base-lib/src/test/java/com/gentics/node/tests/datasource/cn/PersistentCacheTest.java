package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.DatasourceType;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.node.tests.utils.TimingUtils;
import org.junit.experimental.categories.Category;

/**
 * Test cases with "persistent" caches
 */
@Category(BaseLibTest.class)
public class PersistentCacheTest {
	protected final static String HANDLE_ID = "handle";
	protected final static String DS_ID = "ds";

	@BeforeClass
	public static void setupOnce() throws NodeException {
		TimingUtils.registerJobMonitor();
	}

	/**
	 * Test background sync with persistent caches
	 * @throws Exception
	 */
	@Test
	public void testBackgroundSync() throws Exception {
		String contentId = "1000.1";

		// create datasource
		registerHandle();
		registerDatasource();
		CNWriteableDatasource ds = PortalConnectorFactory.createDatasource(CNWriteableDatasource.class, DS_ID);
		CNTestDataHelper.importTypes(ds);

		// create object and read it (this will populate the cache)
		Map<String, Object> data = new HashMap<>();
		data.put("text", "cached value");
		CNTestDataHelper.createObject(ds, contentId, data);
		assertEquals("Check attribute value", "cached value", PortalConnectorFactory.getContentObject(contentId, ds).get("text"));

		TimingUtils.waitForNextSecond();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		// destroy datasource and handle, cache is still populated
		PortalConnectorFactory.destroy();

		// recreate handle and datasource, background sync checker will start to run with an already populated cache
		registerHandle();
		registerDatasource();
		ds = PortalConnectorFactory.createDatasource(CNWriteableDatasource.class, DS_ID);
		CNTestDataHelper.importTypes(ds);

		// wait for the background sync checker, first run should clear cache
		TimingUtils.waitForBackgroundSyncChecker(ds);
		assertNull("Object must not be cached any more", PortalConnectorFactory.getContentObject(contentId, ds));
	}

	/**
	 * Register datasource handle
	 * @throws NodeException
	 */
	protected void registerHandle() throws NodeException {
		Map<String, String> handleProperties = new HashMap<String, String>();
		handleProperties.put("type", "jdbc");
		handleProperties.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProperties.put("url", "jdbc:hsqldb:mem:" + PersistentCacheTest.class.getSimpleName());
		handleProperties.put("shutDownCommand", "SHUTDOWN");
		PortalConnectorFactory.registerHandle(HANDLE_ID, HandleType.sql, handleProperties);
	}

	/**
	 * Register datasource
	 * @throws NodeException
	 */
	protected void registerDatasource() throws NodeException {
		Map<String, String> dsProperties = new HashMap<String, String>();
		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("setUpdatetimestampOnWrite", "true");
		dsProperties.put("cache", "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING, "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING_DIFFERENTIAL, "true");
		dsProperties.put("cache.syncchecking.interval", "1");
		PortalConnectorFactory.registerDatasource(DS_ID, DatasourceType.contentrepository, dsProperties, Arrays.asList(HANDLE_ID));
	}
}
