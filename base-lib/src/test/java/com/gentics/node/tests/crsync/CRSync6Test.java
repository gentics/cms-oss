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

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CRSync6Test extends AbstractCRSyncTest {

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
	public CRSync6Test(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * test modifying (update) a mulivalue attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testModifiedMultiValueAttribute() throws Exception {

		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("permissions", Arrays.asList(new String[] { "33" })); // ATTR_TYPE_LONG
		c = getContentObject(sourceDS, "10002.107", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	/**
	 * test removing a single value attribute
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testRemovedSingleValueAttribute() throws Exception {

		// OK
		// delete from contentattribute where name = node_id and contentid = 1
		Changeable c;
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.clear();
		attrs.put("editor", null); // ATTR_TYPE_TEXT
		attrs.put("datum", null); // ATTR_TYPE_DATE
		c = getContentObject(sourceDS, "10007.107", attrs);
		storeSingleChangeable(sourceDS, c);

		attrs.clear();
		attrs.put("euro", null); // ATTR_TYPE_DOUBLE
		c = getContentObject(sourceDS, "10002.277", attrs);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testLobAttributeRemoved() throws Exception {

		Changeable c;

		c = getContentObject(sourceDS, "10007.227", null);
		c.setProperty("description", null); // ATTR_TYPE_TEXT_LONG
		storeSingleChangeable(sourceDS, c);

		c = getContentObject(sourceDS, "10007.277", null);
		c.setProperty("binary", null); // ATTR_TYPE_BLOG
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testLobAttributeUpdateOptimized() throws Exception {

		Changeable c;

		c = getContentObject(sourceDS, "10007.666", null);
		c.setProperty("optimizedclob", "i have changed");
		storeSingleChangeable(sourceDS, c);

		doIt();
	}

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testLobAttributeMultiValueRemove() throws Exception {

		Changeable c;

		c = getContentObject(sourceDS, "10007.777", null);
		List<?> multivalueclob = (List<?>) c.getProperty("multivalueclob");

		multivalueclob.remove(0);
		c.setProperty("multivalueclob", multivalueclob);
		storeSingleChangeable(sourceDS, c);

		doIt();
	}
}
