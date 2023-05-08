package com.gentics.node.tests.datasource.cn;

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
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the caching of queries for CNDatasources
 */
@Category(BaseLibTest.class)
public class CNFilterCacheTest extends AbstractCNCacheTest {

	/**
	 * Filter expression to test
	 */
	public final static String FILTER_EXPRESSION = "object.int >= data.min && object.int <= data.max";

	// expected numbers for the first filter
	protected Collection<Integer> expectFirst = null;

	// expected numbers for the second filter
	protected Collection<Integer> expectSecond = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// create some test data
		// the values will be 1, 2, ... 10
		for (int num = 1; num <= 10; num++) {
			createObject("1000." + num, num, true);
		}

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 6, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 6, 7, 8, 9, 10);
	}

	/**
	 * Test whether queries are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheFilter() throws Exception {
		// do the queries to cache the results

		// get the result for one query
		getResult(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), null, true);

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// do the queries again

		// get the result for one query
		getResult(3, 7, expectFirst.size(), counter, true);
		// check DB counter
		assertEquals("Check # of DB statements for first query", 0, counter.getCount());

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), counter, true);
		// check DB counter
		assertEquals("Check # of DB statements for second query", 0, counter.getCount());
	}

	/**
	 * Test whether getting counts is cached
	 * @throws Exception
	 */
	@Test
	public void testCacheCount() throws Exception {
		// get the counts to cache the results

		// get the count for one query
		getCount(3, 7, expectFirst.size(), null, true);

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), null, true);

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the counts again

		// get the count for one query
		getCount(3, 7, expectFirst.size(), counter, true);
		// check DB counter
		assertEquals("Check # of DB statements for first query", 0, counter.getCount());

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), counter, true);
		// check DB counter
		assertEquals("Check # of DB statements for second query", 0, counter.getCount());
	}

	/**
	 * Test whether query caches are cleared when an object is inserted
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnInsert() throws Exception {
		// do the queries to cache the results

		// get the result for one query
		getResult(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), null, true);

		// create an object, which will add a 6
		createObject("1000.11", 6, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 6, 6, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 6, 6, 7, 8, 9, 10);

		// do the queries again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the result for one query
		getResult(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test whether count caches are cleared when an object is created
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnInsert() throws Exception {
		// get the counts to cache the results

		// get the count for one query
		getCount(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), null, true);

		// create an object, which will add a 6
		createObject("1000.11", 6, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 6, 6, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 6, 6, 7, 8, 9, 10);

		// get the counts again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the count for one query
		getCount(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test whether query caches are cleared when an object is changed
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnUpdate() throws Exception {
		// do the queries to cache the results

		// get the result for one query
		getResult(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), null, true);

		// update an object
		updateObject("1000.6", 1000, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 7, 8, 9, 10);

		// do the queries again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the result for one query
		getResult(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test whether count caches are cleared when an object is changed
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnUpdate() throws Exception {
		// get the counts to cache the results

		// get the count for one query
		getCount(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), null, true);

		// update an object
		updateObject("1000.6", 1000, true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 7, 8, 9, 10);

		// get the counts again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the count for one query
		getCount(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test whether query caches are cleared when an object is deleted
	 * @throws Exception
	 */
	@Test
	public void testClearFilterCacheOnDelete() throws Exception {
		// do the queries to cache the results

		// get the result for one query
		getResult(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), null, true);

		// delete an object
		deleteObject("1000.6", true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 7, 8, 9, 10);

		// do the queries again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the result for one query
		getResult(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test whether count caches are cleared when an object is deleted
	 * @throws Exception
	 */
	@Test
	public void testClearCountCacheOnDelete() throws Exception {
		// get the counts to cache the results

		// get the count for one query
		getCount(3, 7, expectFirst.size(), null, true);

		// get the result for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), null, true);

		// delete an object
		deleteObject("1000.6", true);

		// the first query will select objects with numbers between 3 and 7
		expectFirst = Arrays.asList(3, 4, 5, 7);

		// the second query will select objects with numbers between 5 and 18
		expectSecond = Arrays.asList(5, 7, 8, 9, 10);

		// get the counts again

		// create a query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the count for one query
		getCount(3, 7, expectFirst.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), counter, true);

		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test filtering from a datasource that does not cache
	 * @throws Exception
	 */
	@Test
	public void testFilterWithoutCache() throws Exception {
		// do the queries to cache the results

		// get the result for one query
		getResult(3, 7, expectFirst.size(), null, false);

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), null, false);

		// do the queries again

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the result for one query
		getResult(3, 7, expectFirst.size(), counter, false);
		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the result for another query (same filter, but different data)
		getResult(5, 18, expectSecond.size(), counter, false);
		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Test count from a datasource that does not cache
	 * @throws Exception
	 */
	@Test
	public void testCountWithoutCache() throws Exception {
		// get the counts to cache the results

		// get the count for one query
		getCount(3, 7, expectFirst.size(), null, false);

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), null, false);

		// get the counts again

		// create a counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the count for one query
		getCount(3, 7, expectFirst.size(), counter, false);
		// check DB counter
		assertEquals("Check # of DB statements for first query", 1, counter.getCount());

		// get the count for another query (same filter, but different data)
		getCount(5, 18, expectSecond.size(), counter, false);
		// check DB counter
		assertEquals("Check # of DB statements for second query", 2, counter.getCount());
	}

	/**
	 * Get the result with the filter in the given channel
	 * @param minValue min value (used in the filter)
	 * @param maxValue max value (used in the filter)
	 * @param expected expected number of objects
	 * @param counter counter (may be null)
	 * @param cachingDS true to use the caching ds, false to use the non-caching
	 * @throws Exception
	 */
	protected void getResult(int minValue, int maxValue, int expected, QueryCounter counter, boolean cachingDS) throws Exception {
		CNWriteableDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(FILTER_EXPRESSION));
		Map<String, Integer> valueMap = new HashMap<String, Integer>();

		valueMap.put("min", minValue);
		valueMap.put("max", maxValue);
		filter.addBaseResolvable("data", new MapResolver(valueMap));

		Collection<Resolvable> result = ds.getResult(filter, null);

		assertEquals("Check # of filtered objects", expected, result.size());
	}

	/**
	 * Count the objects with the filter in the given channel
	 * @param minValue min value (used in the filter)
	 * @param maxValue max value (used in the filter)
	 * @param expected expected count
	 * @param counter counter (may be null)
	 * @param cachingDS true to use the caching ds, false to use the non-caching
	 * @throws Exception
	 */
	protected void getCount(int minValue, int maxValue, int expected, QueryCounter counter, boolean cachingDS) throws Exception {
		CNWriteableDatasource ds = cachingDS ? this.ds : this.uncachedDS;

		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(FILTER_EXPRESSION));
		Map<String, Integer> valueMap = new HashMap<String, Integer>();

		valueMap.put("min", minValue);
		valueMap.put("max", maxValue);
		filter.addBaseResolvable("data", new MapResolver(valueMap));

		assertEquals("Check # of counted objects", expected, ds.getCount(filter));
	}
}
