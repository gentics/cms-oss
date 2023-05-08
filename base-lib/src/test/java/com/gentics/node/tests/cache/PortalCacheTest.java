package com.gentics.node.tests.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheAttributes;
import com.gentics.lib.cache.JCSPortalCache;
import com.gentics.lib.etc.PortalConfigurationHelper;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the PortalCache
 */
@Category(BaseLibTest.class)
public class PortalCacheTest {
	/**
	 * Name of the dummy region (not configured in cache.ccf)
	 */
	public final static String DUMMY_REGION = "dummyregion";

	/**
	 * Name of the region that has element attributes configured in cache.ccf
	 */
	public final static String REGION_WITH_ELEMENT_ATTRIBUTES = "with-element-attributes";

	/**
	 * Name of the region that is configured, but without element attributes
	 */
	public final static String REGION_WITHOUT_ELEMENT_ATTRIBUTES = "without-element-attributes";

	@BeforeClass
	public static void setup() throws Exception {
		// set the environment variable for the path where cache.ccf can be found
		URL cacheCcfUrl = PortalCacheTest.class.getResource("cache.ccf");
		assertNotNull("Did not find cache.ccf", cacheCcfUrl);
		File cacheCcfFile = new File(cacheCcfUrl.toURI());
		System.setProperty(PortalConfigurationHelper.PROPERTY_CONFIGPATH, cacheCcfFile.getParent());

		// initialize the cache by getting a region
		assertNotNull("Could not get dummyregion", PortalCache.getCache(DUMMY_REGION));
	}

	@Test
	public void testImplementationClass() throws Exception {
		assertEquals("Check cache implementation class", JCSPortalCache.class, PortalCache.getCache(DUMMY_REGION).getClass());
	}

	/**
	 * Test getting the default element attributes
	 * @throws Exception
	 */
	@Test
	public void testDefaultElementAttributes() throws Exception {
		PortalCache dummyRegionCache = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", dummyRegionCache);
		String key = "objectKey";
		String object = "storedObject";
		dummyRegionCache.put(key, object);

		PortalCacheAttributes defaultAttributes = dummyRegionCache.getCacheAttributes(key);
		assertNotNull("Element attributes must exist", defaultAttributes);
		assertEquals("Check maxAge", 1800, defaultAttributes.getMaxAge());
	}

	/**
	 * Test getting element attribute configured for a region
	 * @throws Exception
	 */
	@Test
	public void testRegionElementAttributes() throws Exception {
		PortalCache region = PortalCache.getCache(REGION_WITH_ELEMENT_ATTRIBUTES);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		region.put(key, object);

		PortalCacheAttributes cacheAttributes = region.getCacheAttributes(key);
		assertNotNull("Element attributes must exist", cacheAttributes);
		assertEquals("Check maxAge", 999, cacheAttributes.getMaxAge());
	}

	/**
	 * Test getting custom element attributes (which were put into the cache for an object)
	 * Using a custom Implementation Class
	 * @throws Exception
	 */
	@Test
	public void testCustomElementAttributes() throws Exception {
		PortalCache region = PortalCache.getCache(REGION_WITH_ELEMENT_ATTRIBUTES);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		TestedPortalCacheAttributes attributes = new TestedPortalCacheAttributes();
		attributes.setMaxAge(777);
		region.put(key, object, attributes);

		PortalCacheAttributes cacheAttributes = region.getCacheAttributes(key);
		assertNotNull("Element attributes must exist", cacheAttributes);
		assertEquals("Check maxAge", attributes.getMaxAge(), cacheAttributes.getMaxAge());
	}

	/**
	 * Test getting custom element attributes (which were put into the cache for an object)
	 * using the default attributes
	 * @throws Exception
	 */
	@Test
	public void testCustomElementAttributes2() throws Exception {
		PortalCache region = PortalCache.getCache(REGION_WITH_ELEMENT_ATTRIBUTES);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		PortalCacheAttributes attributes = region.getDefaultCacheAttributes();
		attributes.setMaxAge(777);
		region.put(key, object, attributes);

		PortalCacheAttributes cacheAttributes = region.getCacheAttributes(key);
		assertNotNull("Element attributes must exist", cacheAttributes);
		assertEquals("Check maxAge", attributes.getMaxAge(), cacheAttributes.getMaxAge());
	}

	/**
	 * Test getting default element attributes for a region that was configured
	 * but without element attributes
	 *
	 * @throws Exception
	 */
	@Test
	public void testConfiguredWithoutElementAttributes() throws Exception {
		PortalCache region = PortalCache.getCache(REGION_WITHOUT_ELEMENT_ATTRIBUTES);
		assertNotNull("Region must exist", region);
		PortalCacheAttributes defaultAttributes = region.getDefaultCacheAttributes();
		assertNotNull("Default element attributes must exist", defaultAttributes);
		assertEquals("Check maxAge", 1800, defaultAttributes.getMaxAge());
	}

	/**
	 * Test cache attribute "isEternal"
	 * @throws Exception
	 */
	@Test
	public void testIsEternal() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		region.put(key, object);

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check default for isEternal", false, attributes.getIsEternal());

		attributes.setIsEternal(true);
		region.put(key, object, attributes);

		attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check changed value for isEternal", true, attributes.getIsEternal());
	}

	/**
	 * Test cache attribute "createDate"
	 * @throws Exception
	 */
	@Test
	public void testCreateDate() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		long timeBeforePut = System.currentTimeMillis();
		region.put(key, object);
		long timeAfterPut = System.currentTimeMillis();

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		long createDate = attributes.getCreateDate();
		if (timeBeforePut == timeAfterPut) {
			assertEquals("Check create date", timeBeforePut, createDate);
		} else {
			assertTrue("Create date [" + createDate + "] must be between [" + timeBeforePut + "] and [" + timeAfterPut + "]", timeBeforePut <= createDate
					&& createDate <= timeAfterPut);
		}
	}

	/**
	 * Test cache attribute "lastAccessDate"
	 * @throws Exception
	 */
	@Test
	public void testLastAccessDate() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		long timeBeforePut = System.currentTimeMillis();
		region.put(key, object);
		long timeAfterPut = System.currentTimeMillis();

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		long lastAccessDate = attributes.getLastAccessDate();
		if (timeBeforePut == timeAfterPut) {
			assertEquals("Check last access date", timeBeforePut, lastAccessDate);
		} else {
			assertTrue("Last access date [" + lastAccessDate + "] must be between [" + timeBeforePut + "] and [" + timeAfterPut + "]",
					timeBeforePut <= lastAccessDate && lastAccessDate <= timeAfterPut);
		}

		// wait a bit, access element and check last access date
		Thread.sleep(100);
		long timeBeforeAccess = System.currentTimeMillis();
		region.get(key);
		long timeAfterAccess = System.currentTimeMillis();
		attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		lastAccessDate = attributes.getLastAccessDate();

		if (timeBeforeAccess == timeAfterAccess) {
			assertEquals("Check last access date", timeBeforeAccess, lastAccessDate);
		} else {
			assertTrue("Last access date [" + lastAccessDate + "] must be between [" + timeBeforeAccess + "] and [" + timeAfterAccess + "]",
					timeBeforeAccess <= lastAccessDate && lastAccessDate <= timeAfterAccess);
		}
	}

	/**
	 * Test cache attribute "maxAge"
	 * @throws Exception
	 */
	@Test
	public void testMaxAge() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		region.put(key, object);

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check default for maxAge", 1800, attributes.getMaxAge());

		attributes.setMaxAge(888);
		region.put(key, object, attributes);

		attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check changed value for maxAge", 888, attributes.getMaxAge());
	}

	/**
	 * Test cache attribute "maxIdleTime"
	 * @throws Exception
	 */
	@Test
	public void testMaxIdleTime() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		region.put(key, object);

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check default for idleTime", 1000, attributes.getMaxIdleTime());

		attributes.setMaxIdleTime(888);
		region.put(key, object, attributes);

		attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check changed value for idleTime", 888, attributes.getMaxIdleTime());
	}

	/**
	 * Test cache attribute "size"
	 * @throws Exception
	 */
	@Test
	public void testSize() throws Exception {
		PortalCache region = PortalCache.getCache(DUMMY_REGION);
		assertNotNull("Region must exist", region);
		String key = "objectKey";
		String object = "storedObject";
		region.put(key, object);

		PortalCacheAttributes attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check default for size", 0, attributes.getSize());

		attributes.setSize(object.getBytes().length);
		region.put(key, object, attributes);

		attributes = region.getCacheAttributes(key);
		assertNotNull("Cache attribute must exist", attributes);
		assertEquals("Check changed value for size", object.getBytes().length, attributes.getSize());
	}
}
