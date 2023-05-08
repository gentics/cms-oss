package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the caching of queries for MCCR Datasources
 */
@Category(BaseLibTest.class)
public class MCCRFilterCacheTest extends AbstractMCCRCacheTest {

	/**
	 * Filter expression to test
	 */
	public final static String FILTER_EXPRESSION = "object.int >= data.min && object.int <= data.max";

	// expected numbers for the first filter (per channel)
	protected Map<Integer, Collection<Integer>> expectFirst = new HashMap<Integer, Collection<Integer>>();

	// expected numbers for the second filter (per channel)
	protected Map<Integer, Collection<Integer>> expectSecond = new HashMap<Integer, Collection<Integer>>();

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// create some test data
		// the values in the master will be 1, 2, ... 10
		// the values in the channel will be 2, 4, ... 20
		for (int num = 1; num <= 10; num++) {
			createObject(num, new int[] { num, num * 2}, true);
		}

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 6, 7));
		expectFirst.put(2, Arrays.asList(4, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 6, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 8, 10, 12, 14, 16, 18));
	}

	/**
	 * Test whether queries are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheFilter() throws Exception {
		// do the queries to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// do the queries again
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);
			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 0, counter.getCount());

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);
			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 0, counter.getCount());
		}
	}

	/**
	 * Test whether getting counts is cached
	 * @throws Exception
	 */
	@Test
	public void testCacheCount() throws Exception {
		// get the counts to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the counts again
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);
			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 0, counter.getCount());

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);
			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 0, counter.getCount());
		}
	}

	/**
	 * Test whether query caches are cleared when an object is inserted
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnInsert() throws Exception {
		// do the queries to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// create an object, which will add a 6 for the master and for the channel
		createObject(11, new int[] { 6, 6}, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 6, 6, 7));
		expectFirst.put(2, Arrays.asList(4, 6, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 6, 6, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 6, 8, 10, 12, 14, 16, 18));

		// do the queries again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test whether count caches are cleared when an object is created
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnInsert() throws Exception {
		// get the counts to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// create an object, which will add a 6 for the master and for the channel
		createObject(11, new int[] { 6, 6}, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 6, 6, 7));
		expectFirst.put(2, Arrays.asList(4, 6, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 6, 6, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 6, 8, 10, 12, 14, 16, 18));

		// get the counts again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test whether query caches are cleared when an object is changed
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnUpdate() throws Exception {
		// do the queries to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// update an object
		updateObject(6, new int[] { 1000, 6}, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 7));
		expectFirst.put(2, Arrays.asList(4, 6, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 6, 8, 10, 14, 16, 18));

		// do the queries again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test whether count caches are cleared when an object is changed
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnUpdate() throws Exception {
		// get the counts to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// update an object
		updateObject(6, new int[] { 1000, 6}, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 7));
		expectFirst.put(2, Arrays.asList(4, 6, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 6, 8, 10, 14, 16, 18));

		// get the counts again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test whether query caches are cleared when an object is deleted
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnDelete() throws Exception {
		// do the queries to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// delete an object
		deleteObject(6, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 7));
		expectFirst.put(2, Arrays.asList(4, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 8, 10, 14, 16, 18));

		// do the queries again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test whether count caches are cleared when an object is deleted
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnDelete() throws Exception {
		// get the counts to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), null, true);

			// get the result for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), null, true);
		}

		// delete an object
		deleteObject(6, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst.put(1, Arrays.asList(3, 4, 5, 7));
		expectFirst.put(2, Arrays.asList(4, 6));

		// the second query will select objects with numbers between 5 and 18
		expectSecond.put(1, Arrays.asList(5, 7, 8, 9, 10));
		expectSecond.put(2, Arrays.asList(6, 8, 10, 14, 16, 18));

		// get the counts again
		for (int channelId : CHANNEL_IDS) {
			// create a query counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), counter, true);

			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test filtering from a datasource that does not cache
	 * @throws Exception
	 */
	@Test
	public void testFilterWithoutCache() throws Exception {
		// do the queries to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), null, false);

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), null, false);
		}

		// do the queries again
		for (int channelId : CHANNEL_IDS) {
			// create a counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the result for one query
			getResult(channelId, 3, 7, expectFirst.get(channelId).size(), counter, false);
			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the result for another query (same filter, but different data)
			getResult(channelId, 5, 18, expectSecond.get(channelId).size(), counter, false);
			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Test count from a datasource that does not cache
	 * @throws Exception
	 */
	@Test
	public void testCountWithoutCache() throws Exception {
		// get the counts to cache the results
		for (int channelId : CHANNEL_IDS) {
			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), null, false);

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), null, false);
		}

		// get the counts again
		for (int channelId : CHANNEL_IDS) {
			// create a counter
			QueryCounter counter = new QueryCounter(false, true);

			// get the count for one query
			getCount(channelId, 3, 7, expectFirst.get(channelId).size(), counter, false);
			// check DB counter
			assertEquals("Check # of DB statements for first query in channel " + channelId, 1, counter.getCount());

			// get the count for another query (same filter, but different data)
			getCount(channelId, 5, 18, expectSecond.get(channelId).size(), counter, false);
			// check DB counter
			assertEquals("Check # of DB statements for second query in channel " + channelId, 2, counter.getCount());
		}
	}

	/**
	 * Get the result with the filter in the given channel
	 * @param channelId channel id
	 * @param minValue min value (used in the filter)
	 * @param maxValue max value (used in the filter)
	 * @param expected expected number of objects
	 * @param counter counter (may be null)
	 * @param cachingDS true to use the caching ds, false to use the non-caching
	 * @throws Exception
	 */
	protected void getResult(int channelId, int minValue, int maxValue, int expected, QueryCounter counter, boolean cachingDS) throws Exception {
		WritableMCCRDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		if (counter != null) {
			counter.stop();
		}
		ds.setChannel(channelId);
		if (counter != null) {
			counter.start();
		}

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(FILTER_EXPRESSION));
		Map<String, Integer> valueMap = new HashMap<String, Integer>();

		valueMap.put("min", minValue);
		valueMap.put("max", maxValue);
		filter.addBaseResolvable("data", new MapResolver(valueMap));

		Collection<Resolvable> result = ds.getResult(filter, null);

		assertEquals("Check # of filtered objects in channel " + channelId, expected, result.size());
	}

	/**
	 * Count the objects with the filter in the given channel
	 * @param channelId channel id
	 * @param minValue min value (used in the filter)
	 * @param maxValue max value (used in the filter)
	 * @param expected expected count
	 * @param counter counter (may be null)
	 * @param cachingDS true to use the caching ds, false to use the non-caching
	 * @throws Exception
	 */
	protected void getCount(int channelId, int minValue, int maxValue, int expected, QueryCounter counter, boolean cachingDS) throws Exception {
		WritableMCCRDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		if (counter != null) {
			counter.stop();
		}
		ds.setChannel(channelId);
		if (counter != null) {
			counter.start();
		}

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(FILTER_EXPRESSION));
		Map<String, Integer> valueMap = new HashMap<String, Integer>();

		valueMap.put("min", minValue);
		valueMap.put("max", maxValue);
		filter.addBaseResolvable("data", new MapResolver(valueMap));

		assertEquals("Check # of counted objects in channel " + channelId, expected, ds.getCount(filter));
	}
}
