package com.gentics.node.tests.crsync;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync9Test extends AbstractCRSyncTest {

	/**
	 * Get the test parameters
	 *
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: multiDBTest: {0} -> {1}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> sourceVariations = AbstractDatabaseVariationTest.getVariations(TestDatabaseVariationConfigurations.BASIC);
		Map<String, TestDatabase> targetVariations = AbstractDatabaseVariationTest.getVariations(TestDatabaseVariationConfigurations.BASIC);
		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase source : sourceVariations.values()) {
			for (TestDatabase target : targetVariations.values()) {
				data.add(new Object[] { source, target });
			}
		}

		return data;
	}

	/**
	 * Create instance with test parameters
	 *
	 * @param source
	 *            source database
	 * @param target
	 *            target database
	 */
	public CRSync9Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testLobAttributeAdded() throws Exception {

		// remove the attribute content from the target object (meaning that it
		// is new in the source object)
		Changeable c = getContentObject(targetDS, "10007.227", null);

		c.setProperty("content", null);
		storeSingleChangeable(targetDS, c);

		doIt();

	}

	/**
	 * test removing an optimized single value attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedOptimizedSingleValueAttribute() throws Exception {

		// delete from contentattribute where name = node_id and contentid = 1
		// update contentmap set quick_Node_id = null where contentid = 1

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("name", null); // ATTR_TYPE_TEXT_LONG
		Changeable c = getContentObject(sourceDS, "10002.107", attrs);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test removing a multivalue attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedMultiValueAttribute() throws Exception {

		// delete from contentattribute where name = node_id and contentid = 1
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("permissions", null); // ATTR_TYPE_LONG
		c = getContentObject(sourceDS, "10002.107", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test adding a new optimized single value attribute type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewOptimizedSinglevalueAttributeType() throws Exception {

		// add priority, optimized, and create column quick_priority
		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable co = null;

		// create priority optimized
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("priority", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_priority", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("priority", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_priority", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false), true);

		attrs.clear();
		attrs.put("contentid", "10007.13");
		attrs.put("name", "Priority");
		attrs.put("editor", "rb");
		attrs.put("description", "Priority Websites");
		attrs.put("node_id", "1");
		attrs.put("priority", "99");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		doIt();
	}

	/**
	 * test adding a new single value attribute type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewSinglevalueAttributeType() throws Exception {

		// create new attribute priority, not optimized

		// create priority non-optimized
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("priority", GenticsContentAttribute.ATTR_TYPE_LONG, false, null,
				false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("priority", "99");
		Changeable c = createPage(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test adding a new multivalue attribute type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewMultivalueAttributeType() throws Exception {

		// create priority non-optimized
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("mv", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, true,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("mv", Arrays.asList(new String[] { "1", "2", "3" }));
		Changeable c = createPage(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

}
