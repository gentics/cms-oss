package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for caching of types for MCCR Datasources
 */
@Category(BaseLibTest.class)
public class MCCRTypeCacheTest extends AbstractMCCRCacheTest {

	/**
	 * Test types cache
	 * @throws Exception
	 */
	@Test
	public void testTypesCache() throws Exception {
		// read the types (this should fill the cache)
		Map<Integer, ObjectTypeBean> objectTypes = MCCRHelper.getObjectTypes(ds, false);

		assertFalse("Object types must not be null or empty", ObjectTransformer.isEmpty(objectTypes));
		assertNotNull("Object type 1000 is expected to exist", objectTypes.get(1000));

		// setup counting DB statements
		QueryCounter counter = new QueryCounter(false, true);

		// read the types again
		objectTypes = MCCRHelper.getObjectTypes(ds, false);
		assertFalse("Object types must not be null or empty", ObjectTransformer.isEmpty(objectTypes));
		assertNotNull("Object type 1000 is expected to exist", objectTypes.get(1000));

		// check whether types were all read from cache the second time
		assertEquals("Check # of issued SQL statements", 0, counter.getCount());
	}

	/**
	 * Test whether modifying - but not saving - an object type fetched from the cache does not change the cache itself
	 * @throws Exception
	 */
	@Test
	public void testObjectTypeCacheIntegrity() throws Exception {
		Map<Integer, ObjectTypeBean> objectTypes = MCCRHelper.getObjectTypes(ds, false);
		ObjectTypeBean typeBean = objectTypes.get(1000);

		// change a type
		String originalName = typeBean.getName();

		typeBean.setName("New name");

		// read the type again
		objectTypes = MCCRHelper.getObjectTypes(ds, false);
		typeBean = objectTypes.get(1000);

		assertEquals("Check name from cache", originalName, typeBean.getName());
	}

	/**
	 * Test whether modifying - but not saving - an attribute type from the cache does not change the cache itself
	 * @throws Exception
	 */
	@Test
	public void testAttributeTypeCacheIntegrity() throws Exception {
		Map<Integer, ObjectTypeBean> objectTypes = MCCRHelper.getObjectTypes(ds, false);
		ObjectTypeBean typeBean = objectTypes.get(1000);
		ObjectAttributeBean attrType = typeBean.getAttributeTypesMap().get("text");

		// change a attr type
		boolean originalMultivalue = attrType.getMultivalue();

		attrType.setMultivalue(!originalMultivalue);

		// read the type again
		objectTypes = MCCRHelper.getObjectTypes(ds, false);
		typeBean = objectTypes.get(1000);
		attrType = typeBean.getAttributeTypesMap().get("text");

		assertEquals("Check multivalue flag from cache", originalMultivalue, attrType.getMultivalue());
	}

	/**
	 * Test clearing the types cache (together with all caches)
	 * @throws Exception
	 */
	@Test
	public void testClearTypesCache() throws Exception {
		// read the types (this should fill the cache)
		MCCRHelper.getObjectTypes(ds, false);

		// setup counting DB statements
		QueryCounter counter = new QueryCounter(false, true);

		// check cache
		MCCRHelper.getObjectTypes(ds, false);
		assertEquals("Check # of issued SQL statements", 0, counter.getCount());

		// clear the caches
		counter.stop();
		ds.clearCaches();
		counter.start();

		// check whether types are all read from DB the second time
		MCCRHelper.getObjectTypes(ds, false);
		assertEquals("Check # of issued SQL statements", 1, counter.getCount());
	}

	/**
	 * Test whether cache is cleared when a type is changed
	 * @throws Exception
	 */
	@Test
	public void testChangeType() throws Exception {
		// read the types (this should fill the cache)
		Map<Integer, ObjectTypeBean> types = MCCRHelper.getObjectTypes(ds, false);

		// change a type
		ObjectTypeBean type = types.get(1000);

		type.setName("New name");
		ObjectManagementManager.save(ds, type, false, false, false);

		// read the types again
		types = MCCRHelper.getObjectTypes(ds, false);
		type = types.get(1000);
		assertEquals("Check the name after modification", "New name", type.getName());
	}

	/**
	 * Test whether cache is cleared when an attribute type is changed
	 * @throws Exception
	 */
	@Test
	public void testChangeAttributeType() throws Exception {
		// read the types (this should fill the cache)
		Map<Integer, ObjectTypeBean> types = MCCRHelper.getObjectTypes(ds, false);

		// change an attribute type
		ObjectTypeBean type = types.get(1000);
		ObjectAttributeBean attrType = type.getAttributeTypesMap().get("text");

		assertNotNull("Attribute type must exist", attrType);
		boolean multivalue = attrType.getMultivalue();

		multivalue = !multivalue;
		attrType.setMultivalue(multivalue);
		ObjectManagementManager.save(ds, type, true, false, false);

		// read the types again
		types = MCCRHelper.getObjectTypes(ds, false);
		type = types.get(1000);
		attrType = type.getAttributeTypesMap().get("text");
		assertEquals("Check multivalue property", multivalue, attrType.getMultivalue());
	}
}
