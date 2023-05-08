package com.gentics.node.tests.datasource.cn;

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
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.mccr.ContentId;
import com.gentics.node.testutils.QueryCounter;

/**
 * Test cases for caching of attributes in CN datasources
 */
@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class CNAttributeCacheTest extends AbstractCNCacheTest {

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Tested object
	 */
	protected GenticsContentObject object = null;

	@Parameters(name = "{index}: cacheTest: {0}")
	public static Collection<Object[]> data() {
//		org.hsqldb.util.DatabaseManagerSwing.main(new String[] {});
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
	public CNAttributeCacheTest(String attribute) {
		this.attribute = attribute;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// these are the ids of the link targets
		List<String> linkTargetIds = Arrays.asList("1000.2", "1000.3", "1000.4");

		// create the link target objects
		for (String id : linkTargetIds) {
			CNTestDataHelper.createObject(ds, id, null);
		}

		// create and save the object
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put(attribute, ATTRIBUTE_DATA.get(attribute));
		object = CNTestDataHelper.createObject(ds, "1000.100", dataMap);

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
		Resolvable object = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object readValue = object.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);

		QueryCounter counter = new QueryCounter(false, true);

		// generally, we expect no SQL statements
		int expectedStatements = 0;

		// access the attribute again
		Resolvable cachedObject = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object cachedReadValue = cachedObject.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), cachedReadValue);

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
		Resolvable object = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object readValue = object.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);

		// modify the attribute and store it back
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject("1000.100", ds);

		changeable.setProperty(attribute, MODIFIED_ATTRIBUTE_DATA.get(attribute));
		ds.store(Collections.singleton(changeable));

		// access the attribute again (must be changed)
		Resolvable resolvable = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object cachedReadValue = resolvable.get(attribute);

		CNTestDataHelper.assertData("Check data", MODIFIED_ATTRIBUTE_DATA.get(attribute), cachedReadValue);
	}

	/**
	 * Test whether the cached object is not affected by attribute modification that is not stored back
	 * @throws Exception
	 */
	@Test
	public void testCacheIntegrity() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		Resolvable object = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object readValue = object.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);

		// modify the attribute, but don't store it back
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject("1000.100", ds);
		changeable.setProperty(attribute, MODIFIED_ATTRIBUTE_DATA.get(attribute));

		// access the attribute again (must not be changed)
		Resolvable resolvable = PortalConnectorFactory.getContentObject("1000.100", ds);
		readValue = resolvable.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);
	}

	/**
	 * Test whether a cache clear affects the attribute caches
	 * @throws Exception
	 */
	@Test
	public void testCacheClear() throws Exception {
		// read all objects and access the attribute, this should put the attribute into the cache
		Resolvable object = PortalConnectorFactory.getContentObject("1000.100", ds);
		Object readValue = object.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);

		// clear all caches
		ds.clearCaches();

		// we expect 2 sql statements (one for the object, one for the
		// attribute)
		int expectedStatements = 2;

		QueryCounter counter = new QueryCounter(false, true);

		// read all objects and access the attribute again, this must access the DB now
		Resolvable resolvable = PortalConnectorFactory.getContentObject("1000.100", ds);
		readValue = resolvable.get(attribute);

		CNTestDataHelper.assertData("Check data", ATTRIBUTE_DATA.get(attribute), readValue);

		// if the data contains any GenticsContentObjects, we expect one SQL statement for each
		expectedStatements += countContentIds(readValue);

		assertEquals("Check # of DB statements", expectedStatements, counter.getCount());
	}

	/**
	 * Count the number of contentids in the given value
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
		} else if (value instanceof GenticsContentObject) {
			return 1;
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
