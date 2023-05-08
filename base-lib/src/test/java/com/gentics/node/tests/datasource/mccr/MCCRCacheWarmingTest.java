package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;


/**
 * Test cases for cache warming in MCCRDatasources
 */
@Category(BaseLibTest.class)
public class MCCRCacheWarmingTest extends AbstractMCCRCacheTest {
	/**
	 * Timestamp of the last update
	 */
	protected long lastUpdate;

	@Override
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		super.addCachingDatasourceParameters(dsProperties);
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING, "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING_DIFFERENTIAL, "true");
		dsProperties.put("cache.syncchecking.interval", "1");
		dsProperties.put(MCCRDatasource.CACHE_WARMING_ATTRIBUTES, "int,text,int_multi,text_multi");
		dsProperties.put("cache.attribute.int_multi.region", "gentics-content-contentrepository-atts-warmed");
		dsProperties.put("cache.attribute.text_multi.region", "gentics-content-contentrepository-atts-warmed");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setUp() throws Exception {
		super.setUp();

		// test data must be created in a single transaction. Otherwise it could happen, that the background sync checker
		// might see only part of the created data (although all data is created with the same updatetimestamp)
		DB.startTransaction(ds.getHandle());

		// create some test data
		// the values in the master will be 1, 2, ... 10
		// the values in the channel will be 2, 4, ... 20
		for (int num = 1; num <= 10; num++) {
			createObject(num, true,
					new Attribute<Integer>("int", num, num*2),
					new Attribute<List<Integer>>("int_multi", Arrays.asList(num, num + 1), Arrays.asList(num * 2, num * 2 + 1)));
		}
		DB.commitTransaction(ds.getHandle());

		TimingUtils.waitForNextSecond();
		TimingUtils.waitForBackgroundSyncChecker(ds);
	}

	/**
	 * Test whether the cache is warmed
	 * @throws Exception
	 */
	@Test
	public void testWarming() throws Exception {
		// get the objects
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());

			// get a query counter
			QueryCounter counter = new QueryCounter(true, true);

			// access the warmed attributes
			for (Resolvable res : result) {
				// access a filled attribute
				res.get("int");
				// access a not filled attribute
				res.get("text");
			}

			assertEquals(
					"Check # of DB statements after warmed attributes were accessed. Logged Statements: "
							+ counter.getLoggedStatements(), 0,
					counter.getCount());

			// access some not warmed attributes
			for (Resolvable res : result) {
				res.get("long");
			}

			assertEquals(
					"Check # of DB statements after not warmed attributes were accessed. Logged Statements: "
							+ counter.getLoggedStatements(), 10,
					counter.getCount());
		}
	}

	/**
	 * Test whether the cache is warmed for multivalue attributes
	 * @throws Exception
	 */
	@Test
	public void testWarmingMulti() throws Exception {
		// get the objects
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());

			// get a query counter
			QueryCounter counter = new QueryCounter(true, true);

			// access the warmed attributes
			for (Resolvable res : result) {
				// access a filled attribute
				res.get("int_multi");
				// access a not filled attribute
				res.get("text_multi");
			}

			assertEquals(
					"Check # of DB statements after warmed attributes were accessed. Logged Statements: "
							+ counter.getLoggedStatements(), 0,
					counter.getCount());

			// access some not warmed attributes
			for (Resolvable res : result) {
				res.get("long_multi");
			}

			assertEquals(
					"Check # of DB statements after not warmed attributes were accessed. Logged Statements: "
							+ counter.getLoggedStatements(), 10,
					counter.getCount());
		}
	}

	/**
	 * Test that modified objects are correctly warmed
	 * @throws Exception
	 */
	@Test
	public void testWarmingAfterModification() throws Exception {
		TimingUtils.pauseScheduler();

		// modify an object
		updateObject(5, new int[] { 1000, 2000}, false);

		// access the object immediately (before the syncchecker could refresh the caches)
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);

			QueryCounter counter = new QueryCounter(true, true);
			Resolvable object = PortalConnectorFactory.getContentObject("1000.5", ds);

			assertNotNull("Check that object was found", object);

			// attribute value must be unchanged
			assertEquals("Check attribute value before cache was refreshed", 5 * channelId, object.get("int"));

			// everything must have been cached
			assertEquals("Check # of DB statements. Logged Statements: "
					+ counter.getLoggedStatements(), 0, counter.getCount());
		}

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		// access the object again
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);

			QueryCounter counter = new QueryCounter(false, true);
			Resolvable object = PortalConnectorFactory.getContentObject("1000.5", ds);

			assertNotNull("Check that object was found", object);

			// attribute value must be unchanged
			assertEquals("Check attribute value after cache was refreshed", 1000 * channelId, object.get("int"));

			// everything must have been cached
			assertEquals("Check # of DB statements", 0, counter.getCount());
		}
	}

	/**
	 * Test that created objects are correctly warmed
	 * @throws Exception
	 */
	@Test
	public void testWarmingAfterCreation() throws Exception {
		// modify an object
		createObject(11, new int[] { 11, 22}, false);

		TimingUtils.waitForBackgroundSyncChecker(ds);

		// access the object
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);

			QueryCounter counter = new QueryCounter(true, true);
			Resolvable object = PortalConnectorFactory.getContentObject("1000.11", ds);

			assertNotNull("Check that object was found", object);

			// attribute value must be unchanged
			assertEquals("Check attribute value after cache was refreshed", 11 * channelId, object.get("int"));

			// everything must have been cached
			assertEquals("Check # of DB statements. Logged Statements: "
					+ counter.getLoggedStatements(), 0, counter.getCount());
		}
	}
}
