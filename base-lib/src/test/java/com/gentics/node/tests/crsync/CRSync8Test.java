package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
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
import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync8Test extends AbstractCRSyncTest {

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
	public CRSync8Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * test "test" flag
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testTestFlag() throws Exception {

		// store new object
		Map<String, Object> attrs = new HashMap<String, Object>();

		String contentid1 = "10002.112233";
		String contentid2 = GenticsContentObject.STR_OBJ_TYPE_FOLDER + ".99";

		attrs.put("contentid", contentid1);
		Changeable c = createFolder(sourceDS, attrs);

		storeSingleChangeable(sourceDS, c);

		// we want to make sure that target is older than source
		touchAllObjects(target, 1);
		touchRepository(targetDS, 1, false);

		touchAllObjects(source, 2);
		touchRepository(sourceDS, 2, false);

		sync = new CRSync(sourceDS, targetDS, null, true, false, false, false, 100);
		sync.doSync();

		assertNotNull("test if " + contentid1 + " exists in sourceDS", PortalConnectorFactory.getChangeableContentObject(contentid1, sourceDS));
		assertNull("test if " + contentid1 + " exists in targetDS", PortalConnectorFactory.getChangeableContentObject(contentid1, targetDS));
		assertNotNull("test if " + contentid2 + " exists in targetDS", PortalConnectorFactory.getChangeableContentObject(contentid2, targetDS));

		sync = new CRSync(sourceDS, targetDS, null, false, false, false, false, 100);
		sync.doSync();

		assertNotNull("test if " + contentid1 + " still exists in sourceDS", PortalConnectorFactory.getChangeableContentObject(contentid1, sourceDS));
		assertNotNull("test if " + contentid1 + " now exists in targetDS", PortalConnectorFactory.getChangeableContentObject(contentid1, targetDS));
		assertNull("test if " + contentid2 + " still exists in targetDS", PortalConnectorFactory.getChangeableContentObject(contentid2, targetDS));

	}

	/**
	 * Check if NPE will occur while syncing attributes with only NULL fields
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAttrTypeTextLongNullAttribute() throws Exception {

		sync.setUseLobStreams(true);
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();

		// This value will be set to null
		attrs.put("description", "irgendwas"); // ATTR_TYPE_TEXT_LONG

		attrs.put("contentid", "10007." + 278);
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "13498");
		attrs.put("folder_id", "10002.107");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

		c = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, c);

		// Set to null
		String sql = "update contentattribute set value_clob = NULL where contentid = '10007.278'";

		sourceUtils.executeQueryManipulation(sql);

		doIt();
	}

	/**
	 * Check if NPE will occur while syncing attributes with only NULL fields
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAttrTypeForeignObjAttribute() throws Exception {

		sync.setUseLobStreams(true);
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();

		// This value will be set to null
		attrs.put("folder_id", "10002.1"); // ATTR_TYPE_FOREIGNOBJ

		attrs.put("contentid", "10007." + 278);
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "13498");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

		c = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, c);

		// Set to null
		String sql = "update contentattribute set value_text = NULL where contentid = '10007.278' and name ='folder_id' ";

		sourceUtils.executeQueryManipulation(sql);

		sql = "select * from contentattribute where contentid= '10007.278' and name = 'folder_id' ";
		ResultSet rs = sourceUtils.executeQuery(sql);

		if (!rs.next()) {
			fail("Query was empty this shouldn't happen. folder_id attribute is missing in source.");
		}

		doIt();
	}

	/**
	 * Check if NPE will occur while syncing attributes with only NULL fields
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testAttrTypeTextNullAttribute() throws Exception {

		sync.setUseLobStreams(true);
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		// This value will be set to null
		attrs.put("editor", "waha"); // ATTR_TYPE_TEXT

		attrs.put("contentid", "10007." + 278);
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("node_id", "13498");
		attrs.put("folder_id", "10002.107");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

		c = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, c);

		// Set to null
		String sql = "update contentattribute set value_text = NULL where contentid = '10007.278' and name = 'editor' ";

		sourceUtils.executeQueryManipulation(sql);

		doIt();
	}

	/**
	 * target has existing data, rule syncs only parts from source to target. delete flag is not set.
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testSyncWithRule() throws Exception {

		// test sync for one node_id

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("node_id", "1"); // ATTR_TYPE_INTEGER
		c = getContentObject(sourceDS, "10002.666", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("node_id", "666"); // ATTR_TYPE_INTEGER
		c = createFolder(sourceDS, attrs);
		storeSingleChangeable(sourceDS, c);

		sync = new CRSync(sourceDS, targetDS, "(object.node_id == 1) || (object.node_id == 2)");
		sync.doSync();

		c = PortalConnectorFactory.getChangeableContentObject("10002.13", targetDS);

		assertNull("Check for non-existance of non-synced object 10002.13", c);

		// increase sourceTS by 1
		targetDS.setLastUpdate(sourceDS.getLastUpdate() - 1);
		sync = new CRSync(sourceDS, targetDS, "");
		sync.doSync();

		c = PortalConnectorFactory.getChangeableContentObject("10002.13", targetDS);
		assertNotNull("Check for existance of synced object 10002.13 (null)", c);

		// now the repositories should match
	}
}
