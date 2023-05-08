package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for object caches for CNDatasources
 */
@Category(BaseLibTest.class)
public class CNObjectCacheTest extends AbstractCNCacheTest {
	/**
	 * Test whether objects fetched by contentid are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheByContentId() throws Exception {
		CNTestDataHelper.createObject(ds, "1000.100", null);

		// clear the caches
		ds.clearCaches();

		// get the object (which should put it into the cache)
		PortalConnectorFactory.getContentObject("1000.100", ds);

		// setup query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get the object again
		Resolvable cachedObject = PortalConnectorFactory.getContentObject("1000.100", ds);

		assertNotNull("Cached object must exist", cachedObject);
		assertEquals("Check contentId", "1000.100", cachedObject.get("contentid"));

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test whether clearing caches works
	 * @throws Exception
	 */
	@Test
	public void testClearCache() throws Exception {
		CNTestDataHelper.createObject(ds, "1000.100", null);

		// clear the caches
		ds.clearCaches();

		// get the object (which should put it into the cache)
		PortalConnectorFactory.getContentObject("1000.100", ds);

		// clear the caches
		ds.clearCaches();

		QueryCounter counter = new QueryCounter(false, true);

		// get the objects again
		PortalConnectorFactory.getContentObject("1000.100", ds);
		assertEquals("Check # of DB statements", 1, counter.getCount());
	}
}
