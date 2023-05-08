package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.ContentId;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.node.testutils.QueryCounter;

/**
 * Test cases for caching of attributes in MCCR datasources
 */
@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class MCCRAttributeCacheTest extends AbstractMCCRCacheTest {

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Channel objects
	 */
	protected Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

	@Parameters(name = "{index}: cacheTest: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new Vector<Object[]>();

		for (String name : ATTRIBUTE_DATA.keySet()) {
			data.add(new Object[] { name});
		}
		return data;
	}

	/**
	 * Create an instance for testing the given attribute
	 * @param attribute attribute
	 */
	public MCCRAttributeCacheTest(String attribute) {
		this.attribute = attribute;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// these are the ids of the link targets
		List<Integer> linkTargetIds = Arrays.asList(2, 3, 4);

		Map<Integer, MCCRObject> channelObjects = new HashMap<Integer, MCCRObject>();

		// create and save an object in master and channel
		for (int channelId : CHANNEL_IDS) {

			// create the link target objects
			for (Integer id : linkTargetIds) {
				MCCRTestDataHelper.createObject(ds, channelId, id, "1000." + id, null);
			}

			Map<String, Object> dataMap = new HashMap<String, Object>();

			dataMap.put(attribute, getAttributeData(channelId, attribute));
			MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, 4711, "1000." + (channelId + 100), dataMap);

			channelObjects.put(channelId, object);
		}

		// clear the caches
		ds.clearCaches();
	}

	/**
	 * Test whether attributes are cached after access
	 * @throws Exception
	 */
	@Test
	public void testCacheAfterAccess() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		QueryCounter counter = new QueryCounter(false, true);

		// generally, we expect no SQL statements
		int expectedStatements = 0;

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		// no DB statements should have been necessary
		assertEquals("Check # of DB statements", expectedStatements, counter.getCount());
	}

	/**
	 * Test whether attributes that are modified are correctly removed from cache
	 * @throws Exception
	 */
	@Test
	public void testCacheAfterModification() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		// modify the attribute and store it back
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000." + (channelId + 100), ds);

			object.setProperty(attribute, getAttributeData(channelId, attribute, true));
			ds.store(Collections.singleton(object));
		}

		// access the attribute again (must be changed)
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute, true), readValue);
		}
	}

	/**
	 * Test whether the cached object is not affected by attribute modification that is not stored back
	 * @throws Exception
	 */
	@Test
	public void testCacheIntegrity() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		// modify the attribute, but don't store it back
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000." + (channelId + 100), ds);

			object.setProperty(attribute, getAttributeData(channelId, attribute, true));
		}

		// access the attribute again (must not be changed)
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}
	}

	/**
	 * Test whether a cache clear affects the attribute caches
	 * @throws Exception
	 */
	@Test
	public void testCacheClear() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		// clear all caches
		ds.clearCaches();
		MCCRHelper.getObjectTypes(ds, false);
		ObjectAttributeBean attr = MCCRHelper.getAttributeType(ds, 1000, attribute);
		int expectedStatements = 0;

		QueryCounter counter = new QueryCounter(false, true);

		// read all objects and access the attribute again, this must access the DB now
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			// reading the object needs a statement
			expectedStatements++;
			Object readValue = object.get(attribute);
			// reading the value needs a statement, if the attribute is not optimized
			if (!attr.getOptimized()) {
				expectedStatements++;
			}
			// if the data contains any MCCRObjects, we expect one SQL statement for each
			expectedStatements += countContentIds(readValue);

			MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
		}

		assertEquals("Check # of DB statements", expectedStatements, counter.getCount());
	}

	/**
	 * Test caching empty attribute values
	 * @throws Exception
	 */
	@Test
	public void testCacheEmpty() throws Exception {
		Object emptyValue = null;
		if (attribute.endsWith("_multi")) {
			emptyValue = Collections.emptyList();
		}

		// set all attributes to empty
		for (int channelId: CHANNEL_IDS) {
			ds.setChannel(channelId);
			Changeable object = PortalConnectorFactory.getChangeableContentObject("1000." + (channelId + 100), ds);
			object.setProperty(attribute, emptyValue);
			ds.store(Collections.singleton(object));
		}

		// read all objects and access the attribute, this should put the attribute into the cache
		for (int channelId : CHANNEL_IDS) {
			ds.setChannel(channelId);
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", emptyValue, readValue);
		}

		QueryCounter counter = new QueryCounter(false, true);

		// generally, we expect no SQL statements
		int expectedStatements = 0;

		// access the attribute again
		for (int channelId : CHANNEL_IDS) {
			counter.stop();
			ds.setChannel(channelId);
			counter.start();
			Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
			Object readValue = object.get(attribute);

			MCCRTestDataHelper.assertData("Check data", emptyValue, readValue);
		}

		// no DB statements should have been necessary
		assertEquals("Check # of DB statements", expectedStatements, counter.getCount());
	}

	/**
	 * Test accessing of prepared data
	 * @throws Exception
	 */
	@Test
	public void testAccessPreparedData() throws Exception {
		// generally, we expect no SQL statements
		int expectedStatements = 0;

		QueryCounter counter = new QueryCounter(true, true);

		try {
			// access the attribute
			for (int channelId : CHANNEL_IDS) {
				counter.stop();
				ds.setChannel(channelId);
				// prepare data for objects
				Resolvable object = PortalConnectorFactory.getContentObject("1000." + (channelId + 100), ds);
				MCCRHelper.prepareForUpdate(Arrays.asList(object));
				counter.start();
				Object readValue = object.get(attribute);

				// for every MCCRObject contained in the value, we expect a statement (to get the object)
				if (readValue instanceof MCCRObject) {
					expectedStatements++;
				} else if (readValue instanceof Collection<?>) {
					@SuppressWarnings("unchecked")
					Collection<Object> col = (Collection<Object>)readValue;
					for (Object o : col) {
						if (o instanceof MCCRObject) {
							expectedStatements++;
						}
					}
				}

				MCCRTestDataHelper.assertData("Check data", getAttributeData(channelId, attribute), readValue);
			}

			// no DB statements should have been necessary
			assertEquals("Check # of DB statements: " + counter.getLoggedStatements(), expectedStatements, counter.getCount());
		} finally {
			MCCRHelper.resetPreparedForUpdate();
		}
	}

	/**
	 * Count the number of new contentids in the given value
	 * @param value value
	 * @return number of MCCRObject instances
	 */
	protected int countContentIds(Object value) {
		if (value instanceof String) {
			try {
				new ContentId((String) value);
				return 1;
			} catch (DatasourceException e) {
				return 0;
			}
		} else if (value instanceof MCCRObject) {
			MCCRObject obj = (MCCRObject)value;
			return countContentIds(obj.get("contentid"));
		} else if (value instanceof Collection) {
			int sum = 0;

			for (Object v : (Collection<?>) value) {
				sum += countContentIds(v);
			}
			return sum;
		} else {
			return 0;
		}
	}
}
