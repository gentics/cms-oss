package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DBHandle;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync3Test extends AbstractCRSyncTest {

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
	public CRSync3Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * Check if NPE will occur while syncing attributes with only NULL fields
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAttrTypeBlobNullAttribute() throws Exception {

		sync.setUseLobStreams(true);

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();

		byte[] data = createRandomBinaryData(4);

		// This value will be set to null
		attrs.put("binary", data);

		attrs.put("contentid", "10007." + 278);
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "13498");
		attrs.put("folder_id", "10002.107");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

		c = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, c);

		// Set to null
		String sql = "update contentattribute set value_blob = NULL where contentid = '10007.278'";

		sourceUtils.executeQueryManipulation(sql);

		doIt();

	}

	/**
	 * This test uses the rule 'object.node_id CONTAINSONEOF [24,25]' which does not work properly with oracle db. In this case the rule will match to both objects
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRuleCONTAINSONEOF() throws Exception {

		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable co = null;

		attrs.clear();
		attrs.put("contentid", "10007.1");
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "25");
		attrs.put("folder_id", "10002.107");
		attrs.put("editor", "rb");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		attrs.clear();
		attrs.put("contentid", "10007.2");
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "24");
		attrs.put("folder_id", "10002.107");
		attrs.put("editor", "rb");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		String rule = "object.node_id CONTAINSONEOF [24,25]";

		// String rule = "";

		sync = new CRSync(sourceDS, targetDS, rule, false, false, true, true, 100);
		sync.doSync();

		DatasourceInfo dsInfo = sourceDS.delete(sourceDS.createDatasourceFilter(ExpressionParser.getInstance().parse("!(" + rule + ")")));

		assertTrue("asserting that source really had objects which did not meet the rule. {" + dsInfo.getAffectedRecordCount() + "}",
				dsInfo.getAffectedRecordCount() > 0);

		assertCompare();

	}

	/**
	 * Test snycing attributes that contain dots (".") in their name
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testDottedAttributes() throws Exception {

		final String ATTRIBUTE_NAME = "stupid.attribute";
		final String ATTRIBUTE_VALUE = "fourtytwo";

		final String LOB_ATTRIBUTE_NAME = "stupid.lob.attribute";
		final String LOB_ATTRIBUTE_VALUE = "veeeeeeeery long text";

		final String MULTI_ATTRIBUTE_NAME = "stupid.multivalue.attribute";
		final List<Long> MULTI_ATTRIBUTE_VALUES = Arrays.asList(new Long[] { new Long(4711), new Long(815) });

		// create a non-lob attribute with "." in the name
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(ATTRIBUTE_NAME, GenticsContentAttribute.ATTR_TYPE_TEXT, false,
				null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		// create a lob attribute with "." in the name
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(LOB_ATTRIBUTE_NAME, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG,
				false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		// create a multivalue attribute with "." in the name
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(MULTI_ATTRIBUTE_NAME, GenticsContentAttribute.ATTR_TYPE_LONG,
				false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
		attrs.put(LOB_ATTRIBUTE_NAME, LOB_ATTRIBUTE_VALUE);
		attrs.put(MULTI_ATTRIBUTE_NAME, MULTI_ATTRIBUTE_VALUES);
		Changeable c = createPage(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		Resolvable contentObject = PortalConnectorFactory.getContentObject(ObjectTransformer.getString(c.get("contentid"), null), sourceDS);

		assertEquals("Check whether dotted attribute was writted correctly", ATTRIBUTE_VALUE, contentObject.getProperty(ATTRIBUTE_NAME));
		assertEquals("Check whether dotted attribute was writted correctly", LOB_ATTRIBUTE_VALUE, contentObject.getProperty(LOB_ATTRIBUTE_NAME));
		assertEquals("Check whether dotted attribute was writted correctly", convertToStringList(MULTI_ATTRIBUTE_VALUES),
				convertToStringList(contentObject.getProperty(MULTI_ATTRIBUTE_NAME)));

		doIt();
	}

	/**
	 * Test CRSync with the -ignoreoptimized flag set, when a new (optimized) contentattribute is found in the source
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithNewAttribute() throws Exception {

		// set the ignore optimized flag
		sync.setIgnoreOptimized(true);
		final String ATTRIBUTE_NAME = "optimizedattribute";

		// create a new optimized attribute in the source ds
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(ATTRIBUTE_NAME, GenticsContentAttribute.ATTR_TYPE_TEXT, true,
				ObjectAttributeBean.constructQuickColumnName(ATTRIBUTE_NAME), false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false,
				false), true);

		// now do the sync
		sync.doSync();

		// get the attributetype in the target ds and check that it is NOT
		// optimized
		ObjectAttributeBean targetAttribute = getAttributeType(GenticsContentFactory.getHandle(targetDS), ATTRIBUTE_NAME);

		if (targetAttribute == null) {
			fail("attributetype '" + ATTRIBUTE_NAME + "' was not found in target db");
		}

		// check that the attributetype is really not optimized
		assertFalse("Target attributetype '" + ATTRIBUTE_NAME + "' must not be optimized", targetAttribute.getOptimized());
	}

	/**
	 * Test doing a full sync (by setting the lastupdate timestamp in the target cr to 0)
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testFullSync() throws Exception {

		Map<String, Object> attrs = new HashMap<String, Object>();

		// remove an object from the source ds
		attrs.clear();
		attrs.put("contentid", "10007.227");
		sourceDS.delete(Arrays.asList(new Changeable[] { sourceDS.create(attrs, -1, false) }));

		// remove an object from the target ds
		attrs.clear();
		attrs.put("contentid", "10002.666");
		sourceDS.delete(Arrays.asList(new Changeable[] { sourceDS.create(attrs, -1, false) }));

		// modify an object in the source ds
		Changeable c = getContentObject(sourceDS, "10007.277", attrs);

		c.setProperty("name", "Modified Name");
		storeSingleChangeable(sourceDS, c);

		// set the lastupdate timestamp from the target ds to 0 (should do a
		// full sync)
		touchAllObjects(target, 1);
		touchRepository(targetDS, 0, false);

		touchAllObjects(source, 1);
		touchRepository(sourceDS, 1, false);

		logger.info("DONE TEST SETUP ---- RUNING SYNC -----");
		sync.doSync();
		assertCompare();
	}

	/**
	 * Test synchronization of modified motherid (when no optimized columns exist)
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedMotherId() throws Exception {
		// make all attributes not-optimized
		Collection<ObjectAttributeBean> attributes = ObjectManagementManager.loadAttributeTypes(sourceDS.getHandle().getDBHandle());

		for (ObjectAttributeBean attr : attributes) {
			attr.setOptimized(false);
			ObjectManagementManager.saveAttributeType(sourceDS, attr, true);
		}
		doIt();

		// directly update the motherid of an object in the source db (not using
		// the PortalConnector)
		source.updateSQL("update " + sourceDS.getHandle().getDBHandle().getContentMapName()
				+ " set mother_obj_type = 10002, mother_obj_id = 1, motherid = '10002.1' where contentid = '10007.777'");

		doIt();
	}

	/**
	 * Test synchronizing multivalue link attributes
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testMultivalueLinkAttribute() throws Exception {
		DBHandle dbh = GenticsContentFactory.getHandle(sourceDS);

		// create a new multivalue link attribute
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("linkedpages", GenticsContentAttribute.ATTR_TYPE_OBJ, false,
				null, true, GenticsContentObject.OBJ_TYPE_FOLDER, GenticsContentObject.OBJ_TYPE_PAGE, null, null, null, false, false), true);

		// fill the attribute
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject("10002.107", sourceDS);
		List<String> values = new Vector<String>();

		values.add("10007.227");
		values.add("10007.277");
		// also add an invalid contentid
		values.add("10007.99999");
		values.add("10007.666");
		changeable.setProperty("linkedpages", values);
		storeSingleChangeable(sourceDS, changeable);

		values.clear();
		values.add("10007.277");
		values.add("10007.666");
		values.add("10007.227");
		changeable = PortalConnectorFactory.getChangeableContentObject("10002.666", sourceDS);
		changeable.setProperty("linkedpages", values);
		storeSingleChangeable(sourceDS, changeable);

		// now sync
		doIt();
	}
}
