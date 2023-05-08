package com.gentics.node.tests.crsync;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync7Test extends AbstractCRSyncTest {

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
	public CRSync7Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * test multivaluetype -> optimized singlevaluetype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedMultivaluetoOptimizedSinglevalueType() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_perm", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_perm", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false), true);

		// now manually remove all additional data for the former multivalue
		// attribute
		// TODO: remove this, when the data is removed in the
		// ObjectManagementManager
		// TODO: the crsync should also automatically correct the sortorder,
		// when the attribute type is now singlevalue
		DB.update(sourceDS.getHandle().getDBHandle(), "DELETE FROM " + sourceDS.getHandle().getDBHandle().getContentAttributeName()
				+ " WHERE name = ? AND sortorder IS NOT NULL AND sortorder > ?", new Object[] { "permissions", new Integer(1) });
		DB.update(sourceDS.getHandle().getDBHandle(), "UPDATE " + sourceDS.getHandle().getDBHandle().getContentAttributeName()
				+ " SET sortorder = NULL WHERE name = ? AND contentid != ?", new Object[] { "permissions", "10002.1" });

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test remove optimized single value type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedOptimizedSinglevalueAttributeType() throws Exception {

		ObjectManagementManager.deleteAttributeType(GenticsContentFactory.getHandle(sourceDS), new ObjectAttributeBean("node_id",
				GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false), true);

		ObjectManagementManager.deleteAttributeType(GenticsContentFactory.getHandle(sourceDS), new ObjectAttributeBean("node_id",
				GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null,
				false, false), true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("obj_type", "10007");
		attrs.put("name", "Priority");
		Changeable c = sourceDS.create(attrs, -1, false);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test remove single value type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedSinglevalueAttributeType() throws Exception {

		ObjectManagementManager.deleteAttributeType(GenticsContentFactory.getHandle(sourceDS), new ObjectAttributeBean("description",
				GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false),
				true);

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test remove multivalue type
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedMultivalueAttributeType() throws Exception {

		ObjectManagementManager.deleteAttributeType(GenticsContentFactory.getHandle(sourceDS), new ObjectAttributeBean("permissions",
				GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false),
				true);

		Changeable c = createPage(sourceDS, null);

		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test new objecttype (like folder or page)
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewObjectType() throws Exception {

		// set up contentobject, contentattributetypes (folders)
		ObjectManagementManager.createNewObject(GenticsContentFactory.getHandle(sourceDS), "13", "raoul");

		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("ddatum", GenticsContentAttribute.ATTR_TYPE_DATE, false, null,
				false, 13, 0, null, null, null, false, false), true);

		// insert data
		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable co = null;

		// 13.13
		attrs.clear();
		attrs.put("contentid", "13.13");
		attrs.put("ddatum", "1983-06-13");
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		// 13.xx
		attrs.clear();
		attrs.put("obj_type", "13");
		attrs.put("ddatum", "2007-06-13");
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		doIt();
	}

	/**
	 * test removed objecttype
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedObjectType() throws Exception {

		Collection<ObjectTypeBean> ot = ObjectManagementManager.loadObjectTypes(GenticsContentFactory.getHandle(sourceDS));
		Collection<ObjectAttributeBean> oat = ObjectManagementManager.loadAttributeTypes(GenticsContentFactory.getHandle(sourceDS));

		ObjectManagementManager.setReferences(ot, oat);

		ObjectTypeBean otb = null;

		for (Iterator<ObjectTypeBean> iter = ot.iterator(); iter.hasNext();) {
			otb = iter.next();

			if (otb.getType().intValue() == GenticsContentObject.OBJ_TYPE_PAGE) {
				break;
			}

		}

		ObjectManagementManager.deleteAttributeTypes(GenticsContentFactory.getHandle(sourceDS), otb.getAttributeTypes(), true);
		ObjectManagementManager.deleteObjectType(GenticsContentFactory.getHandle(sourceDS), otb, false, true);

		doIt();
	}

}
