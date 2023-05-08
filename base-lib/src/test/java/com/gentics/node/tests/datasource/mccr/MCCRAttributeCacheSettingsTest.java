package com.gentics.node.tests.datasource.mccr;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.tests.category.BaseLibTest;
import com.gentics.lib.cache.JCSPortalCache;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.node.testutils.QueryCounter;
import org.apache.jcs.engine.stats.behavior.ICacheStats;
import org.apache.jcs.engine.stats.behavior.IStatElement;
import org.apache.jcs.engine.stats.behavior.IStats;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Attribute cache test for various attribute cache settings (caching disabled, own cache region)
 */
@Category(BaseLibTest.class)
public class MCCRAttributeCacheSettingsTest extends AbstractMCCRCacheTest {

	/**
	 * Name of the custom cache region
	 */
	protected final static String CUSTOM_CACHE_REGION = "custom-cache-region";

	/**
	 * Name of the attribute that has caching disabled
	 */
	protected final static String uncachedAttribute = "text";

	/**
	 * Name of the attribute that uses a custom cache region
	 */
	protected final static String ownCacheRegionAttribute = "int";

	/**
	 * Name of the attribute that uses default cache settings
	 */
	protected final static String defaultCachedAttribute = "double";

	@Override
	public void setUp() throws Exception {
		super.setUp();

		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {
			Map<String, Object> dataMap = new HashMap<String, Object>();

			dataMap.put(uncachedAttribute, getTextValue(channelId));
			dataMap.put(ownCacheRegionAttribute, getIntValue(channelId));
			dataMap.put(defaultCachedAttribute, getDoubleValue(channelId));
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, 4711, "1000." + (channelId + 100), dataMap);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();
	}

	@Override
	protected void addCachingDatasourceParameters(Map<String, String> dsProperties) {
		dsProperties.put("cache", "true");
		dsProperties.put("cache.attribute." + uncachedAttribute, "false");
		dsProperties.put("cache.attribute." + ownCacheRegionAttribute + ".region", CUSTOM_CACHE_REGION);
	}

	/**
	 * Test accessing the uncached attribute
	 * @throws Exception
	 */
	@Test
	public void testUncachedAttribute() throws Exception {
		// read all objects and access the attribute, this would put the attribute into the cache (if the attribute was cached)
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(uncachedAttribute);

			MCCRTestDataHelper.assertData("Check data", getTextValue(channelId), readValue);
		}

		QueryCounter counter = new QueryCounter(false, true);

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(uncachedAttribute);

			MCCRTestDataHelper.assertData("Check data", getTextValue(channelId), readValue);
		}

		// we expect one DB access per attribute
		assertEquals("Check # of DB statements", CHANNEL_IDS.length, counter.getCount());
	}

	/**
	 * Test accessing the default cached attribute
	 * @throws Exception
	 */
	@Test
	public void testDefaultCachedAttribute() throws Exception {
		// read all objects and access the attribute, this will put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(defaultCachedAttribute);

			MCCRTestDataHelper.assertData("Check data", getDoubleValue(channelId), readValue);
		}

		QueryCounter counter = new QueryCounter(false, true);

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(defaultCachedAttribute);

			MCCRTestDataHelper.assertData("Check data", getDoubleValue(channelId), readValue);
		}

		// we expect no DB access
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test accessing the attribute that uses its own cache region
	 * @throws Exception
	 */
	@Test
	public void testOwnCacheRegionAttribute() throws Exception {
		// read all objects and access the attribute, this will put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(ownCacheRegionAttribute);

			MCCRTestDataHelper.assertData("Check data", getIntValue(channelId), readValue);
		}

		// get the start hit counts
		int customHitCount = getHitCount(CUSTOM_CACHE_REGION);
		int defaultHitCount = getHitCount(MCCRCacheHelper.ATTRIBUTESCACHEREGION);

		QueryCounter counter = new QueryCounter(false, true);

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(ownCacheRegionAttribute);

			MCCRTestDataHelper.assertData("Check data", getIntValue(channelId), readValue);
		}

		// we expect no DB access
		assertEquals("Check # of DB statements", 0, counter.getCount());

		// custom cache region should have been hit twice
		assertEquals("Check hit count for cache region " + CUSTOM_CACHE_REGION, customHitCount + 2, getHitCount(CUSTOM_CACHE_REGION));
		assertEquals("Check hit count for cache region " + MCCRCacheHelper.ATTRIBUTESCACHEREGION, defaultHitCount,
				getHitCount(MCCRCacheHelper.ATTRIBUTESCACHEREGION));
	}

	/**
	 * Test whether clearing all datasource caches will also clear the custom cache regions
	 * @throws Exception
	 */
	@Test
	public void testClearCustomCacheRegion() throws Exception {
		// read all objects and access the attribute, this will put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(ownCacheRegionAttribute);

			MCCRTestDataHelper.assertData("Check data", getIntValue(channelId), readValue);
		}

		QueryCounter counter = new QueryCounter(false, true);

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(ownCacheRegionAttribute);

			MCCRTestDataHelper.assertData("Check data", getIntValue(channelId), readValue);
		}

		// we expect no DB access
		assertEquals("Check # of DB statements", 0, counter.getCount());
		// all attributes for an object (all channels) are grouped together in a single cache entry
		assertEquals("Check cache size", 1, getCacheSize(CUSTOM_CACHE_REGION));

		// now clear the datasource caches
		ds.clearCaches();
		assertEquals("Check cache size", 0, getCacheSize(CUSTOM_CACHE_REGION));
	}

	/**
	 * Get the text value for the channel
	 * @param channelId channel id
	 * @return text value
	 */
	protected String getTextValue(int channelId) {
		return "Text value for channel " + channelId;
	}

	/**
	 * Get the int value for the channel
	 * @param channelId channel id
	 * @return int value
	 */
	protected int getIntValue(int channelId) {
		return channelId + 100;
	}

	/**
	 * Get the double value for the channel
	 * @param channelId channel id
	 * @return double value
	 */
	protected double getDoubleValue(int channelId) {
		return channelId + 0.4711;
	}

	/**
	 * Get the cache size of the given region
	 * @param region region
	 * @return cache size
	 * @throws PortalCacheException
	 */
	protected int getCacheSize(String region) throws PortalCacheException {
		int mapSize = 0;
		PortalCache cache = PortalCache.getCache(region);
		if (!(cache instanceof JCSPortalCache)) {
			throw new PortalCacheException("Cannot get CacheSize for non JCSPortalCaches");
		}
		ICacheStats statistics = ((JCSPortalCache) cache).getJcsCache().getStatistics();
		IStats[] auxCacheStats = statistics.getAuxiliaryCacheStats();

		for (IStats stat : auxCacheStats) {
			IStatElement[] statElements = stat.getStatElements();

			for (IStatElement iStatElement : statElements) {
				if ("Map Size".equals(iStatElement.getName())) {
					mapSize += ObjectTransformer.getInt(iStatElement.getData(), 0);
				}
			}
		}

		return mapSize;
	}

	/**
	 * Get the hit count for the given cache region
	 * @param region cache region
	 * @return hit count
	 * @throws PortalCacheException
	 */
	protected int getHitCount(String region) throws PortalCacheException {
		int hitCount = 0;
		PortalCache cache = PortalCache.getCache(region);
		if (!(cache instanceof JCSPortalCache)) {
			throw new PortalCacheException("Cannot get HitCount for non JCSPortalCaches");
		}
		ICacheStats statistics = ((JCSPortalCache) cache).getJcsCache().getStatistics();
		IStatElement[] statElements = statistics.getStatElements();

		for (IStatElement iStatElement : statElements) {
			if ("HitCountRam".equals(iStatElement.getName())) {
				hitCount = ObjectTransformer.getInt(iStatElement.getData(), hitCount);
			}
		}

		return hitCount;
	}
}
