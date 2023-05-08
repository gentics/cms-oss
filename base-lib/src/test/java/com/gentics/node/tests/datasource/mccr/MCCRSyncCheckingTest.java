package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.node.testutils.QueryCounter;
import com.gentics.node.tests.utils.TimingUtils;
import org.junit.experimental.categories.Category;


/**
 * Test cases for syncchecking for a MCCRDatasource
 */
@Category(BaseLibTest.class)
public class MCCRSyncCheckingTest extends AbstractMCCRCacheTest {

	@Override
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		super.addCachingDatasourceParameters(dsProperties);
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING, "true");
		dsProperties.put(MCCRDatasource.CACHE_SYNCCHECKING_DIFFERENTIAL, "true");
		dsProperties.put("cache.syncchecking.interval", "1");
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// create some test data
		// the values in the master will be 1, 2, ... 10
		// the values in the channel will be 2, 4, ... 20
		for (int num = 1; num <= 10; num++) {
			createObject(num, new int[] { num, num * 2}, true);
		}

		TimingUtils.waitForNextSecond();
		TimingUtils.waitForBackgroundSyncChecker(ds);
	}

	/**
	 * Test accessing an object, that was cached and later modified (in background)
	 */
	@Test
	public void testFilterModifiedObject() throws Exception {
		// first do some queries for the objects
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.pauseScheduler();

		// now change an object "in background"
		updateObject(5, new int[] { 1000, 1000}, false);

		// do the queries again (immediately, before the syncchecking job had the time to do its job)
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 9, result.size());
		}
	}

	/**
	 * Test filtering for an object, that was added (in background)
	 */
	@Test
	public void testFilterAddedObject() throws Exception {
		// first do some queries for the objects
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.pauseScheduler();

		// now create an object "in background"
		createObject(11, new int[] { 1, 1}, false);

		// do the queries again (immediately, before the syncchecking job had the time to do its job)
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 11, result.size());
		}
	}

	/**
	 * Test filtering for an object, that was removed (in background)
	 */
	@Test
	public void testFilterRemovedObject() throws Exception {
		// first do some queries for the objects
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.pauseScheduler();

		// now delete an object "in background"
		deleteObject(5, false);

		// do the queries again (immediately, before the syncchecking job had the time to do its job)
		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 10, result.size());
		}

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		for (int channelId : CHANNEL_IDS) {
			Collection<Resolvable> result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.int <= 20")), null);

			assertEquals("Check # of filtered objects for channel " + channelId, 9, result.size());
		}
	}

	/**
	 * Test accessing an object that was modified (in background)
	 * @throws Exception
	 */
	@Test
	public void testAccessModifiedObject() throws Exception {
		// access the object
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000.5", ds);

			assertNotNull("Object must not be null", object);
			assertEquals("Check attribute value", 5 * channelId, ObjectTransformer.getInt(object.get("int"), 0));
		}

		TimingUtils.pauseScheduler();

		// now change an object "in background"
		updateObject(5, new int[] { 1000, 1000}, false);

		// access the object again (immediately, before the syncchecking job had the time to do its job)
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000.5", ds);

			assertNotNull("Object must not be null", object);
			assertEquals("Check attribute value", 5 * channelId, ObjectTransformer.getInt(object.get("int"), 0));
		}

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000.5", ds);

			assertNotNull("Object must not be null", object);
			assertEquals("Check attribute value", 1000, ObjectTransformer.getInt(object.get("int"), 0));
		}
	}

	/**
	 * Test accessing an object that was not modified (in background)
	 * @throws Exception
	 */
	@Test
	public void testAccessUnmodifiedObject() throws Exception {
		// access the object
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000.5", ds);

			assertNotNull("Object must not be null", object);
			assertEquals("Check attribute value", 5 * channelId, ObjectTransformer.getInt(object.get("int"), 0));
		}

		TimingUtils.pauseScheduler();

		// now change another object "in background"
		updateObject(6, new int[] { 1000, 1000}, false);

		TimingUtils.resumeScheduler();
		TimingUtils.waitForBackgroundSyncChecker(ds);

		// access the object again, check that no DB query was done
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			QueryCounter counter = new QueryCounter(false, true);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000.5", ds);

			assertNotNull("Object must not be null", object);
			assertEquals("Check attribute value", 5 * channelId, ObjectTransformer.getInt(object.get("int"), 0));
			assertEquals("Check # of DB statements", 0, counter.getCount());
		}
	}
}
