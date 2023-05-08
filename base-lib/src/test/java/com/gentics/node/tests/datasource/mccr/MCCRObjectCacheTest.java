package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for object caches for MCCR Datasources
 */
@Category(BaseLibTest.class)
public class MCCRObjectCacheTest extends AbstractMCCRCacheTest {

	/**
	 * Test whether objects fetched by id are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheById() throws Exception {
		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, 4711, "1000." + (channelId + 100), null);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();

		for (int channelId : CHANNEL_IDS) {
			// get the object (which should put it into the cache)
			ds.setChannel(channelId);
			ds.getObjectById(channelObjects.get(channelId).getId());
		}

		// setup query counter
		QueryCounter counter = new QueryCounter(false, true);

		for (int channelId : CHANNEL_IDS) {
			// change the channel. This will issue sql statements, so we stop the counter first
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			// get the object again
			MCCRObject cachedObject = ds.getObjectById(channelObjects.get(channelId).getId());

			assertTrue("Cached object must exist", cachedObject.exists());
			assertEquals("Check contentId", "1000." + (channelId + 100), cachedObject.get("contentid"));
		}

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test whether objects fetched by channelset id are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheByChannelsetId() throws Exception {
		int channelSetId = 4711;

		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, channelSetId, "1000." + (channelId + 100), null);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();

		for (int channelId : CHANNEL_IDS) {
			// get the object (which should put it into the cache)
			ds.setChannel(channelId);
			ds.getObjectByChannelsetId(channelSetId);
		}

		// setup query counter
		QueryCounter counter = new QueryCounter(false, true);

		for (int channelId : CHANNEL_IDS) {
			// change the channel. This will issue sql statements, so we stop the counter first
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			// get the object again
			MCCRObject cachedObject = ds.getObjectByChannelsetId(channelSetId);

			assertTrue("Cached object must exist", cachedObject.exists());
			assertEquals("Check contentId", "1000." + (channelId + 100), cachedObject.get("contentid"));
		}

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test whether objects fetched by contentid are cached
	 * @throws Exception
	 */
	@Test
	public void testCacheByContentId() throws Exception {
		int channelSetId = 4711;

		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, channelSetId, "1000." + (channelId + 100), null);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();

		for (int channelId : CHANNEL_IDS) {
			// get the object (which should put it into the cache)
			ds.setChannel(channelId);
			PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			// also get the object with the master's contentid
			PortalConnectorFactory.getContentObject("1000.101", ds);
		}

		// setup query counter
		QueryCounter counter = new QueryCounter(false, true);

		for (int channelId : CHANNEL_IDS) {
			// change the channel. This will issue sql statements, so we stop the counter first
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			// get the object again
			Resolvable cachedObject = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);

			assertNotNull("Cached object must exist", cachedObject);
			assertEquals("Check contentId", "1000." + (channelId + 100), cachedObject.get("contentid"));

			// now get the object by the master's contentid
			cachedObject = PortalConnectorFactory.getContentObject("1000.101", ds);
			assertNotNull("Cached object must exist", cachedObject);
			assertEquals("Check contentId", "1000." + (channelId + 100), cachedObject.get("contentid"));
		}

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test whether changing the contentid of an object in a channel is correctly cached
	 * @throws Exception
	 */
	@Test
	public void testCacheIntegrity() throws Exception {
		int channelSetId = 4711;

		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, channelSetId, "1000.101", null);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();

		for (int channelId : CHANNEL_IDS) {
			// get the object (which should put it into the cache)
			ds.setChannel(channelId);
			PortalConnectorFactory.getContentObject("1000.101", ds);
		}

		// now change the objects so that they have different contentids
		for (int channelId : CHANNEL_IDS) {
			MCCRObject modifiedObject = MCCRTestDataHelper.createObject(ds, channelId, channelSetId, "1000." + (100 + channelId), null);

			assertEquals("Check object id", channelObjects.get(channelId).getId(), modifiedObject.getId());
		}

		// fetch the objects again and check whether they were returned correctly
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable modifiedObject = PortalConnectorFactory.getContentObject("1000." + (100 + channelId), ds);

			assertNotNull("Modified Object must exist", modifiedObject);
			assertEquals("Check contentid", "1000." + (100 + channelId), modifiedObject.get("contentid"));

			// also get the object by the master's contentid
			modifiedObject = PortalConnectorFactory.getContentObject("1000.101", ds);
			assertNotNull("Modified Object must exist", modifiedObject);
			assertEquals("Check contentid", "1000." + (100 + channelId), modifiedObject.get("contentid"));
		}

		// now change the objects so that they have identical contentids
		for (int channelId : CHANNEL_IDS) {
			MCCRObject modifiedObject = MCCRTestDataHelper.createObject(ds, channelId, channelSetId, "1000.101", null);

			assertEquals("Check object id", channelObjects.get(channelId).getId(), modifiedObject.getId());
		}

		// fetch the objects again and check whether they were returned correctly
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable modifiedObject = PortalConnectorFactory.getContentObject("1000.101", ds);

			assertNotNull("Modified Object must exist", modifiedObject);
			assertEquals("Check contentid", "1000.101", modifiedObject.get("contentid"));
		}
	}

	/**
	 * Test whether clearing caches works
	 * @throws Exception
	 */
	@Test
	public void testClearCache() throws Exception {
		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, 4711, "1000." + (channelId + 100), null);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();

		for (int channelId : CHANNEL_IDS) {
			// get the object (which should put it into the cache)
			ds.setChannel(channelId);
			PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
		}

		// clear the caches
		ds.clearCaches();
		MCCRHelper.getObjectTypes(ds, false);

		QueryCounter counter = new QueryCounter(false, true);

		for (int channelId : CHANNEL_IDS) {
			// get the objects again
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
		}

		assertEquals("Check # of DB statements", 2, counter.getCount());
	}
}
