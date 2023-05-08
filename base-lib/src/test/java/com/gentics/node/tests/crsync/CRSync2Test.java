package com.gentics.node.tests.crsync;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync2Test extends AbstractCRSyncTest {

	/**
	 * Get the test parameters
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
				data.add(new Object[] { source, target});
			}
		}

		return data;
	}

	/**
	 * Create instance with test parameters
	 * @param source source database
	 * @param target target database
	 */
	public CRSync2Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * Test singlevaluetype -> multivaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedSinglevalueToMultivalueAttributeType() throws Exception {

		// create priority non-optimized
		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("content", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);

		// create a new page with a multivalue attribute set
		Map<String, Object> attrs = new HashMap<String, Object>();
		List<String> data = new Vector<String>();

		data.add("rb");
		data.add("lh");
		data.add("nop");
		attrs.put("content", data);

		Changeable c = createPage(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		// modify an existing object
		c = getContentObject(sourceDS, "10007.227", null);
		c.setProperty("content", "modified!");
		storeSingleChangeable(sourceDS, c);

		// and another one (just add a second value)
		c = getContentObject(sourceDS, "10007.277", null);
		Collection content = ObjectTransformer.getCollection(c.getProperty("content"), null);

		content.add("added content");
		c.setProperty("content", content);
		storeSingleChangeable(sourceDS, c);

		sync.setUseLobStreams(false);
		doIt();
	}

	/**
	 * test singlevaluetype -> optimized singlevaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedSinglevalueToOptimizedSinglevalueAttributeType() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test optimized singlevaluetype -> multivaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedOptimizedSinglevalueToMultivalueAttributeType() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("node_id", Arrays.asList(new String[] { "70", "80", "90" }));
		Changeable c = createPage(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		doIt();

	}

	/**
	 * test optimized singlevaluetype -> singlevaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedOptimizedSinglevalueToSinglevalueAttributeType() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test multivaluetype -> singlevaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedMultivaluetoSinglevalueType() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);

		ObjectManagementManager.saveAttributeType(sourceDS,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);

		// now manually remove all additional data for the former multivalue
		// attribute
		// TODO: remove this, when the data is removed in the
		// ObjectManagementManager
		// TODO: the crsync should also automatically correct the sortorder,
		// when the attribute type is now singlevalue
		DB.update(sourceDS.getHandle().getDBHandle(),
				"DELETE FROM " + sourceDS.getHandle().getDBHandle().getContentAttributeName() + " WHERE name = ? AND sortorder IS NOT NULL AND sortorder > ?",
				new Object[] { "permissions", new Integer(1) });
		DB.update(sourceDS.getHandle().getDBHandle(),
				"UPDATE " + sourceDS.getHandle().getDBHandle().getContentAttributeName() + " SET sortorder = NULL WHERE name = ? AND contentid != ?",
				new Object[] { "permissions", "10002.1" });

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();

	}

}
