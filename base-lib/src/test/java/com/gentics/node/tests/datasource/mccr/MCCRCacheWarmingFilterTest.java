package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for cache warming with a filter
 */
@Category(BaseLibTest.class)
public class MCCRCacheWarmingFilterTest extends AbstractMCCRCacheTest {
	@Override
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		super.addCachingDatasourceParameters(dsProperties);
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING, "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING_DIFFERENTIAL, "true");
		dsProperties.put("cache.syncchecking.interval", "1");
		dsProperties.put(MCCRDatasource.CACHE_WARMING_ATTRIBUTES, "int,text");
		dsProperties.put(MCCRDatasource.CACHE_WARMING_FILTER, "object.int <= 5");
	}

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
			createObject(num, new int[] { num, num * 2}, true);
		}
		DB.commitTransaction(ds.getHandle());

		TimingUtils.waitForNextSecond();
		TimingUtils.waitForBackgroundSyncChecker(ds);
	}

	/**
	 * Test whether filtered objects are warmed
	 * @throws Exception
	 */
	@Test
	public void testWarmingFilteredObjects() throws Exception {
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 5")), null);

			// get a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// access the warmed attributes
			for (Resolvable res : result) {
				// access a filled attribute
				res.get("int");
				// access a not filled attribute
				res.get("text");
			}

			assertEquals("Check # of DB statements after warmed attributes were accessed", 0, counter.getCount());
		}
	}

	/**
	 * Test whether not filtered objects are not warmed
	 * @throws Exception
	 */
	@Test
	public void testNotWarmingUnfilteredObjects() throws Exception {
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int > 5")), null);

			// get a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// access the warmed attributes
			for (Resolvable res : result) {
				// access a filled attribute
				res.get("int");
				// access a not filled attribute
				res.get("text");
			}

			assertEquals("Check # of DB statements after warmed attributes were accessed", channelId == 1 ? 5 * 2 : 8 * 2, counter.getCount());
		}
	}
}
