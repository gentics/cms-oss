package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for cache warming in CNDatasources
 */
@Category(BaseLibTest.class)
public class CNCacheWarmingTest extends AbstractCNCacheTest {
	@Override
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		super.addCachingDatasourceParameters(dsProperties);
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING, "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING_DIFFERENTIAL, "true");
		dsProperties.put("cache.syncchecking.interval", "1");
		dsProperties.put(MCCRDatasource.CACHE_WARMING_ATTRIBUTES, "int,text");
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// test data must be created in a single transaction. Otherwise it could happen, that the background sync checker
		// might see only part of the created data (although all data is created with the same updatetimestamp)
		DB.startTransaction(ds.getHandle().getDBHandle());

		// create some test data
		// the values will be 1, 2, ... 10
		for (int num = 1; num <= 10; num++) {
			createObject("1000." + num, num, true);
		}
		DB.commitTransaction(ds.getHandle().getDBHandle());

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
		Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 10")), null);

		assertEquals("Check # of filtered objects", 10, result.size());

		// get a query counter
		QueryCounter counter = new QueryCounter(true, true);

		// access the warmed attributes
		for (Resolvable res : result) {
			// access a filled attribute
			assertTrue("Attribute 'int' must be in allowed range", ObjectTransformer.getInt(res.get("int"), -1) > 0);
			// access a not filled attribute
			assertNull("Attribute 'text' must be null", res.get("text"));
		}

		assertEquals("Check # of DB statements after warmed attributes were accessed:\n" + counter.getLoggedStatements(), 0, counter.getCount());

		// access some not warmed attributes
		for (Resolvable res : result) {
			res.get("long");
		}

		assertEquals("Check # of DB statements after not warmed attributes were accessed", 10, counter.getCount());
	}

	/**
	 * Test that modified objects are correctly warmed
	 * @throws Exception
	 */
	@Test
	public void testWarmingAfterModification() throws Exception {
		TimingUtils.pauseScheduler();

		// modify an object
		updateObject("1000.5", 1000, false);

		// access the object immediately (before the syncchecker could refresh the caches)
		QueryCounter counter = new QueryCounter(false, true);
		Resolvable object = PortalConnectorFactory.getContentObject("1000.5", ds);

		assertNotNull("Check that object was found", object);

		// attribute value must be unchanged
		assertEquals("Check attribute value before cache was refreshed", 5, object.get("int"));

		// everything must have been cached
		assertEquals("Check # of DB statements", 0, counter.getCount());

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		// access the object again
		counter = new QueryCounter(false, true);
		object = PortalConnectorFactory.getContentObject("1000.5", ds);

		assertNotNull("Check that object was found", object);

		// attribute value must be unchanged
		assertEquals("Check attribute value after cache was refreshed", 1000, object.get("int"));

		// everything must have been cached
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test that created objects are correctly warmed
	 * @throws Exception
	 */
	@Test
	public void testWarmingAfterCreation() throws Exception {
		// modify an object
		createObject("1000.11", 11, false);

		TimingUtils.waitForBackgroundSyncChecker(ds);

		// access the object
		QueryCounter counter = new QueryCounter(false, true);
		Resolvable object = PortalConnectorFactory.getContentObject("1000.11", ds);

		assertNotNull("Check that object was found", object);

		// attribute value must be unchanged
		assertEquals("Check attribute value after cache was refreshed", 11, object.get("int"));

		// everything must have been cached
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}
}
