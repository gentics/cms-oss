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
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSyncTest extends AbstractCRSyncTest {

	/**
	 * Create instance with given test parameters
	 * @param source source database
	 * @param target target database
	 */
	public CRSyncTest(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

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

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testObjWithMissingAttribute() throws Exception {

		sync.setUseLobStreams(true);

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();

		int i = 0;

		while (i < 180) {
			// This value will be set to null
			attrs.put("description", "irgendwas"); // ATTR_TYPE_TEXT_LONG

			attrs.put("contentid", "10007." + (278 + i));
			attrs.put("name", "Data" + System.currentTimeMillis());
			attrs.put("node_id", "13498");

			// setting folder_id to
			int u = 0;

			while (u < 20) {
				attrs.put("folder_id", "10002." + (1047 + i + u));
				u++;
			}

			attrs.put("datum", "1990-01-01");
			attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

			c = sourceDS.create(attrs, -1, false);
			storeSingleChangeable(sourceDS, c);
			i++;
		}

		touchAllObjects(target, 1);
		touchRepository(targetDS, 1, false);

		touchAllObjects(source, 2);
		touchRepository(sourceDS, 2, false);

		logger.info("DONE TEST SETUP ---- RUNING SYNC -----");
		sync.doSync();

		// doIt();
	}

	/**
	 * Test whether new content objects are synced correctly DONE
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewContentObject() throws Exception {

		// additionally prepare database
		// add 10007.11
		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable co = null;

		attrs.clear();
		attrs.put("contentid", "10007.11");
		attrs.put("name", "Mehrsprachige Websites");
		attrs.put("editor", "rb");
		attrs.put("description", "Mehrsprachige Websites");
		attrs.put("node_id", "1");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		// add 10007.237
		attrs.clear();
		attrs.put("contentid", "10007.237");
		attrs.put("name", "Neue Seite");
		attrs.put("editor", "nop");
		attrs.put("description", "Neue Seite");
		attrs.put("node_id", "3");
		attrs.put("permissions", Arrays.asList(new String[] { "123", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		// add 10007.???
		attrs.clear();
		attrs.put("obj_type", "10007");
		attrs.put("name", "increase my id_counter");

		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

		doIt();

	}

	/**
	 * Test if contentobjects are correctly removed
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedContentObject() throws Exception {

		// DONE
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("contentid", "10007.227");

		sourceDS.delete(Arrays.asList(new Changeable[] { sourceDS.create(attrs, -1, false) }));

		doIt();
	}

	/**
	 * Test adding a new singleavalueattribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewSingleValueAttribute() throws Exception {

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("editor", "newest editor"); // ATTR_TYPE_TEXT
		// attrs.put("datum", new Date()); // ATTR_TYPE_DATE
		attrs.put("datum", "123123"); // ATTR_TYPE_DATE
		attrs.put("description", "ich bin eine beschreubung lalalalalal :)"); // ATTR_TYPE_TEXT_LONG
		attrs.put("folder_id", "10002.1"); // ATTR_TYPE_FOREIGNOBJ
		c = getContentObject(sourceDS, "10007.666", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("binary", new byte[] { 67, 66, 65 }); // ATTR_TYPE_BLOB
		c = getContentObject(sourceDS, "10007.777", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("euro", new Double(13.76093)); // ATTR_TYPE_DOUBLE
		c = getContentObject(sourceDS, "10002.777", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * Check whether the crsync syncs illegal object links
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testIllegalObjLinkAttribute() throws Exception {

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("folder_id", "10002.0"); // ATTR_TYPE_FOREIGNOBJ
		c = getContentObject(sourceDS, "10007.666", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test adding a new optimized single value attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewOptimizedSingleValueAttribute() throws Exception {

		// DONE
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("node_id", "666"); // ATTR_TYPE_INTEGER
		c = getContentObject(sourceDS, "10007.666", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("node_id", "777"); // ATTR_TYPE_INTEGER
		c = getContentObject(sourceDS, "10007.777", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test adding a new multivalue attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testNewMultivalueAttribute() throws Exception {

		// DONE
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("permissions", Arrays.asList(new String[] { "33" })); // ATTR_TYPE_LONG
		c = getContentObject(sourceDS, "10007.666", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("permissions", Arrays.asList(new String[] { "33", "77", "13" })); // ATTR_TYPE_LONG
		c = getContentObject(sourceDS, "10007.777", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test modifying (update) a single vallue attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedSingleValueAttribute() throws Exception {

		// UPDATE contentattribute set node_id = 2 where contentid = 1 and name
		// = node_id;
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("editor", "newest editor"); // ATTR_TYPE_TEXT
		attrs.put("description", "ich bin eine beschreubung lalalalalal :)"); // ATTR_TYPE_TEXT_LONG
		attrs.put("folder_id", "10002.1"); // ATTR_TYPE_FOREIGNOBJ
		attrs.put("binary", new byte[] { 67, 66, 65 }); // ATTR_TYPE_BLOB
		c = getContentObject(sourceDS, "10007.277", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("node_id", "13498"); // ATTR_TYPE_INTEGER
		attrs.put("euro", "13.76093"); // ATTR_TYPE_DOUBLE
		c = getContentObject(sourceDS, "10002.107", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test modifying (update) an optimized single value attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedOptimizedSingleValueAttribute() throws Exception {

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("name", "newest editor"); // ATTR_TYPE_TEXT
		c = getContentObject(sourceDS, "10002.107", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

}
