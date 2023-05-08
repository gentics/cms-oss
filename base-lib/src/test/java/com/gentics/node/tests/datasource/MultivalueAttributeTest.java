/*
 * @author norbert
 * @date 03.04.2007
 * @version $Id: MultivalueAttributeTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.content.GenticsContentObjectImpl;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.portalconnector.tests.AbstractLegacyNavigationDumpTest;
import org.junit.experimental.categories.Category;

/**
 * Test case for correct storing and reading of multivalue attributes (sortorder)
 *
 * TODO: This test currently only works with MySQL and it is using the legacy_dist_tests_navigation dump with testdata
 */
@Category(BaseLibTest.class)
public class MultivalueAttributeTest extends AbstractLegacyNavigationDumpTest {
	public final static String TESTCONTENTID = "10007.994299";
	public final static String ATTRIBUTE = "testmultivaluedouble";

	private CNWriteableDatasource ds;
	private DBHandle dbHandle;

	private List valueList = new Vector();

	@Before
	public void setUp() throws Exception {

		prepareTestDatabase(getClass().getSimpleName());
		insertDumpIntoDatabase();

		Properties handleProperties = getTestDatabase().getSettings();

		// turn on the cache
		Map dsProps = new HashMap(handleProperties);

		dsProps.put("cache", Boolean.toString(false));

		SQLHandle handle;

		// lookup existing handles in acvtivehandles map
		handle = null;
		// not found, create new handle
		if (handle == null) {
			handle = new SQLHandle("mumu");
			handle.init(handleProperties);
		}
		ds = new CNWriteableDatasource(null, new SimpleHandlePool(handle), dsProps);

		dbHandle = GenticsContentFactory.getHandle(ds);
		// prepare the multivalues (in a special sortorder)
		valueList.clear();
		valueList.add(new Double(4711));
		valueList.add(new Double(99));
		valueList.add(new Double(815));
		valueList.add(new Double(42));
	}

	@After
	public void tearDown() throws Exception {
		removeTestDatabase();
	}

	/**
	 * Test inserting a new object with data for a multivalue attribute and check for correct sortorder
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueInsert() throws Exception {
		// store the object
		Map dataMap = new HashMap();

		dataMap.put("contentid", TESTCONTENTID);
		dataMap.put(ATTRIBUTE, valueList);
		Changeable object = ds.create(dataMap, -1, false);

		ds.store(Collections.singleton(object));

		checkDataSortorder(valueList);
	}

	/**
	 * Test updating the multivalue attribute of an object (change the sortorder)
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueUpdate() throws Exception {
		// do the insert first
		testMultivalueInsert();

		// now update the value (sort the list)
		Collections.sort(valueList);
		Changeable object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);

		object.setProperty(ATTRIBUTE, valueList);

		// store the object
		ds.store(Collections.singleton(object));

		checkDataSortorder(valueList);
	}

	/**
	 * Test updating the multivalue attribute of an object (add value and change sortorder)
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivaluePlus() throws Exception {
		// do the insert first
		testMultivalueInsert();

		// add another value (and sort)
		valueList.add(new Double(125));
		Collections.sort(valueList);
		Changeable object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);

		object.setProperty(ATTRIBUTE, valueList);

		// store the object
		ds.store(Collections.singleton(object));

		checkDataSortorder(valueList);
	}

	/**
	 * Test updating the multivalue attribute of an object (remove a value and change sortorder)
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueMinus() throws Exception {
		// do the insert first
		testMultivalueInsert();

		// remove a value (and sort)
		valueList.remove(new Double(99));
		Collections.sort(valueList);
		Changeable object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);

		object.setProperty(ATTRIBUTE, valueList);

		// store the object
		ds.store(Collections.singleton(object));

		checkDataSortorder(valueList);
	}

	/**
	 * Test fetching of multivalue attributes in the correct order
	 *
	 * @throws Exception
	 */
	@Test
	public void testSelectMultivalue() throws Exception {
		// do the insert first
		testMultivalueInsert();

		// get the object
		Changeable object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);
		List readValues = (List) object.get(ATTRIBUTE);

		checkDataNumbers(valueList, readValues);
	}

	/**
	 * Test fetching of multivalue attributes by prefetch in the correct order
	 *
	 * @throws Exception
	 */
	@Test
	public void testSelectMultivaluePrefetch() throws Exception {
		// do the insert first
		testMultivalueInsert();

		// get the object
		Changeable object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);

		// prefetch the attributes
		GenticsContentObjectImpl.prefillContentObjects(ds, new GenticsContentObject[] { (GenticsContentObject) object }, new String[] { ATTRIBUTE }, -1, true);
		List readValues = (List) object.get(ATTRIBUTE);

		checkDataNumbers(valueList, readValues);
	}

	/**
	 * Internal method to check two lists of numbers for identity
	 *
	 * @param expected
	 *            expected numbers
	 * @param received
	 *            received numbers
	 * @throws Exception
	 */
	protected void checkDataNumbers(List expected, List received) throws Exception {
		assertEquals("Check current number of received values", expected.size(), received.size());
		int size = expected.size();

		for (int i = 0; i < size; ++i) {
			Number expectedNumber = (Number) expected.get(i);
			Number receivedNumber = (Number) received.get(i);

			if (Math.abs(expectedNumber.doubleValue() - receivedNumber.doubleValue()) > 0.001) {
				fail("Value # " + i + " does not match: expected " + expectedNumber + ", was " + receivedNumber);
			}
		}
	}

	/**
	 * Check sortorder of values in database
	 *
	 * @param expectedOrder
	 *            expected list of values
	 * @throws Exception
	 */
	protected void checkDataSortorder(List expectedOrder) throws Exception {
		// get the data from the db
		SimpleResultProcessor rs = new SimpleResultProcessor();

		DB.query(dbHandle, "SELECT value_double, sortorder FROM " + dbHandle.getContentAttributeName() + " WHERE contentid = ? AND name = ? ORDER BY sortorder",
				new Object[] { TESTCONTENTID, ATTRIBUTE }, rs);

		// check correct number of values
		assertEquals("Check correct number of values", expectedOrder.size(), rs.size());

		for (Iterator iter = rs.iterator(); iter.hasNext();) {
			SimpleResultRow row = (SimpleResultRow) iter.next();
			int sortorder = row.getInt("sortorder");
			double value = row.getDouble("value_double");

			assertTrue("Check sortorder {" + sortorder + "}", sortorder <= expectedOrder.size());
			assertEquals("Check value for sortorder {" + sortorder + "}", expectedOrder.get(sortorder - 1), new Double(value));
		}
	}
}
