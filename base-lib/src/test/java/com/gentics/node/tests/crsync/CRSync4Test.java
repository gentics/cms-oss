package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync4Test extends AbstractCRSyncTest {

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
	public CRSync4Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * Test total repository sync
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testTotalSync() throws Exception {

		emtpyTables(target);
		targetDS.setLastUpdate(0);

		doIt();
	}

	/**
	 * test allowAlterTable flag
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAllowAlterTableFlag() throws Exception {

		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("test", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_test", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);
		ObjectManagementManager.saveAttributeType(sourceDS, new ObjectAttributeBean("test", GenticsContentAttribute.ATTR_TYPE_LONG, true,
				"quick_test", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false), true);

		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("contentid", "10007.666");
		attrs.put("test", "333");
		Changeable co = sourceDS.create(attrs, -1, false);

		storeSingleChangeable(sourceDS, co);

		sync = new CRSync(sourceDS, targetDS, "", false, false, false, false, 100);

		try {
			sync.doSync();
			assertTrue(false);
		} catch (NodeException e) {
			assertTrue(true);
		} catch (Exception e) {
			assertTrue(false);
		}

		sync = new CRSync(sourceDS, targetDS, "", false, false, true, false, 100);
		doIt();
	}

	/**
	 * test delete flag
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testDeleteFlag() throws Exception {

		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable c = null;

		attrs.clear();
		attrs.put("node_id", "666"); // ATTR_TYPE_INTEGER
		c = createFolder(targetDS, attrs);
		storeSingleChangeable(targetDS, c);
		String contentId = ObjectTransformer.getString(c.get("contentid"), "");

		// negating the rule does not include NULL values
		sync = new CRSync(sourceDS, targetDS, "(object.node_id == 1) || (object.node_id == 2)", false, false, false, true, 100);

		// folder c exists in targetDS with node_id == 666,
		// which does not match the rule and should be deleted
		assertNotNull(contentId + " does not exist in targetDS", PortalConnectorFactory.getChangeableContentObject(contentId, targetDS));

		sync.doSync();

		// folder c does not exist in sourceDS and does not match the rule
		// test if this contentobject has been removed in targetDS
		assertNull(contentId + " still exists in targetDS", PortalConnectorFactory.getChangeableContentObject(contentId, targetDS));
		assertCompare();
	}

	/**
	 * Test sync when delete flag is not set. Add a new object to the target repository and sync from source repository. Assert that object still exists in target
	 * repository.
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testDeleteFlagNotSet() throws Exception {

		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable c = null;

		attrs.clear();
		attrs.put("node_id", "666"); // ATTR_TYPE_INTEGER
		c = createFolder(targetDS, attrs);
		storeSingleChangeable(targetDS, c);
		String contentId = ObjectTransformer.getString(c.get("contentid"), "");

		// negating the rule does not include NULL values
		sync = new CRSync(sourceDS, targetDS, "(object.node_id == 1) || (object.node_id == 2)", false, false, false, false, 100);

		// folder c exists in targetDS with node_id == 666,
		// which does not match the rule and should be deleted, but
		// only if delete flag is set
		assertNotNull(contentId + " does not exist in targetDS", PortalConnectorFactory.getChangeableContentObject(contentId, targetDS));

		sync.doSync();

		// folder c does not exist in sourceDS
		// test if this contentobject is still in targetDS, since
		// delete flag is not set
		assertNotNull(contentId + " deleted from targetDS", PortalConnectorFactory.getChangeableContentObject(contentId, targetDS));
	}

	/**
	 * test allowEmpty flag
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAllowEmptyFlag() throws Exception {

		emtpyTables(source);

		try {
			sync = new CRSync(sourceDS, targetDS, null, false, false, false, false, 100);
			sync.doSync();
			assertTrue("CRSync synced from empty source to a given target", false);
		} catch (NodeException e) {
			assertTrue(true);
		}

		sync = new CRSync(sourceDS, targetDS, null, false, true, false, false, 100);
		sync.doSync();
		assertCompare();
	}
}
