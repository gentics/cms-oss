package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync5Test extends AbstractCRSyncTest {

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
	public CRSync5Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * Test CRSync with the -ignoreoptimized flag set, when a lob attributetype is optimized in the target, but not in the source
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithOptimizedLOBInTarget() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, true, null, null);
	}

	/**
	 * Test CRSync with -ignoreoptimized flag set, when an optimized attributetype has different quicknames in source and target
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithDifferentQuickname() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT, true, true, "quick_insource", "quick_intarget");
	}

	/**
	 * Test CRSync with -ignoreoptimized flag set, when an optimized attributetype has different quicknames in source and target
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithDifferentLOBQuickname() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, true, true, "quick_insource", "quick_intarget");
	}

	/**
	 * Test CRSync with the -ignoreoptimized flag set, when an attributetype is optimized in the source, but not in the target
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithOptimizedInSource() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT, true, false, null, null);
	}

	/**
	 * Test CRSync with the -ignoreoptimized flag set, when an attributetype is optimized in the target, but not in the source
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithOptimizedInTarget() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT, false, true, null, null);
	}

	/**
	 * Test CRSync with the -ignoreoptimized flag set, when a lob attributetype is optimized in the source, but not in the target
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIgnoreOptimizedWithOptimizedLOBInSource() throws Exception {

		doIgnoreOptimizedTest(GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, true, false, null, null);
	}

	/**
	 * Do the test for ignored attributes
	 *
	 * @param attributeType
	 *            type of the tested attribute
	 * @param optimizedInSource
	 *            true when the attribute shall be optimized in the source, false if not
	 * @param optimizedInTarget
	 *            true when the attribute shall be optimized in the target, false if not
	 * @param sourceQuickName
	 *            quickname of the optimized attribute in the source, null for default
	 * @param targetQuickName
	 *            quickname of the optimized attribute in the target, null for default
	 * @throws Exception
	 */
	protected void doIgnoreOptimizedTest(int attributeType, boolean optimizedInSource, boolean optimizedInTarget, String sourceQuickName,
			String targetQuickName) throws Exception {
		final String ATTRIBUTE_NAME = "optimizedattribute";
		String quickName = ObjectAttributeBean.constructQuickColumnName(ATTRIBUTE_NAME);
		final String ATTRIBUTE_VALUE = "The value of the optimizedattribute";
		final String OBJECT_CONTENTID = "10007.227";

		// set the ignore optimized flag
		sync.setIgnoreOptimized(true);

		if (optimizedInSource) {
			// create a new optimized attribute in the source ds
			ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(ATTRIBUTE_NAME, attributeType, true,
					sourceQuickName != null ? sourceQuickName : quickName, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false,
					false), true);
		} else {
			// create a new not optimized attribute in the source ds
			ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean(ATTRIBUTE_NAME, attributeType, false, null, false,
					GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);
		}

		if (optimizedInTarget) {
			// create a new optimized attribute in the target ds
			ObjectManagementManager.saveAttributeType(targetDS, new ObjectAttributeBean(ATTRIBUTE_NAME, attributeType, true,
					targetQuickName != null ? targetQuickName : quickName, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false,
					false), true);
		} else {
			// create a new not optimized attribute in the target ds
			ObjectManagementManager.saveAttributeType(targetDS, new ObjectAttributeBean(ATTRIBUTE_NAME, attributeType, false, null, false,
					GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);
		}

		// get a changeable and set the attribute for it
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject(OBJECT_CONTENTID, sourceDS);

		changeable.setProperty(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
		storeSingleChangeable(sourceDS, changeable);

		// we want to make sure that target is older than source
		touchAllObjects(target, 1);
		touchRepository(targetDS, 1, false);

		touchAllObjects(source, 2);
		touchRepository(sourceDS, 2, false);

		// now do the sync
		sync.doSync();

		// get the target attribute and check whether it is still not optimized
		ObjectAttributeBean targetAttribute = getAttributeType(GenticsContentFactory.getHandle(targetDS), ATTRIBUTE_NAME);

		if (targetAttribute == null) {
			fail("attributetype '" + ATTRIBUTE_NAME + "' was not found in target db");
		}

		if (optimizedInTarget) {
			// check that the attributetype is really optimized
			assertTrue("Target attributetype '" + ATTRIBUTE_NAME + "' must be optimized in target", targetAttribute.getOptimized());
			assertEquals("Check for current quickname of attributetype '" + ATTRIBUTE_NAME + "' in target", targetQuickName != null ? targetQuickName
					: quickName, targetAttribute.getQuickname());
		} else {
			// check that the attributetype is really not optimized
			assertFalse("Target attributetype '" + ATTRIBUTE_NAME + "' must not be optimized in target", targetAttribute.getOptimized());
		}

		// get the object via a rule from the target ds
		DatasourceFilter filter = targetDS.createDatasourceFilter(ExpressionParser.getInstance().parse(
				"object." + ATTRIBUTE_NAME + " == '" + ATTRIBUTE_VALUE + "'"));
		Collection<Resolvable> result = targetDS.getResult(filter, null);

		assertEquals("Check whether the expected number of objects were found", 1, result.size());

		// check the result object
		Resolvable targetObject = (Resolvable) result.iterator().next();

		assertEquals("Check whether the correct object was found", OBJECT_CONTENTID, targetObject.get("contentid"));
		assertEquals("Check for correct attribute value", ATTRIBUTE_VALUE, targetObject.get(ATTRIBUTE_NAME));

		// now clear the attribute value in the source
		changeable = PortalConnectorFactory.getChangeableContentObject(OBJECT_CONTENTID, sourceDS);
		changeable.setProperty(ATTRIBUTE_NAME, null);
		storeSingleChangeable(sourceDS, changeable);
		touchRepository(sourceDS, 3, false);

		// do the sync again
		sync.doSync();

		// now check whether the data has been cleared in source and target
		Collection<Resolvable> sourceObjects = sourceDS.getResult(
				sourceDS.createDatasourceFilter(ExpressionParser.getInstance().parse("isempty(object." + ATTRIBUTE_NAME + ")")), null);
		Collection<Resolvable> targetObjects = targetDS.getResult(
				targetDS.createDatasourceFilter(ExpressionParser.getInstance().parse("isempty(object." + ATTRIBUTE_NAME + ")")), null);

		assertEquals("Check whether the same number of objects were found in source and target", sourceObjects.size(), targetObjects.size());

		// check whether our object was among the found objects
		assertTrue("Check whether test object was found in source", sourceObjects.contains(changeable));
		assertTrue("Check whether test object was found in target", targetObjects.contains(changeable));
	}

}
