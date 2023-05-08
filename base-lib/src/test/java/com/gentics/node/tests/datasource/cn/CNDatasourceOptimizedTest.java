/*
 * @author herbert
 * @date 27.03.2007
 * @version $Id: CNDatasourceOptimizedTest.java,v 1.2 2010-06-13 19:08:35 johannes2 Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.utils.SQLDumpUtils;
import org.junit.experimental.categories.Category;

/**
 * Very simple test to verify that Insert/Update statements work for optimized attributes. This test is basically important for SQL Errors..
 * which were thrown .. when inserting/updating null values into optimized integer attributes. but also tries to validate the correctness of
 * the written data
 *
 *
 * @author herbert
 *
 */
@Ignore("Disabled this test because it fails while dropping the database (Connection was already closed)")
@Category(BaseLibTest.class)
public class CNDatasourceOptimizedTest extends AbstractVersioningTest {

	String[] allKeys = new String[] { "mystring", "myint", "mylongtext", "mybinary", "mylongint", "mydouble", "mydate", "mylink" };

	private WriteableDatasource ds;

	private Changeable obj1;
	private Changeable obj2;

	private String obj1contentid;
	private String obj2contentid;

	private Map obj1map;
	private Map obj2map;

	@Before
	public void setUp() throws Exception {
		InputStream ins = CNDatasourceOptimizedTest.class.getResourceAsStream("/com/gentics/node/tests/datasource/cn/sql/CNDatasourceOptimizedTest-TestData.sql");

		assertNotNull("Could not find test data dump.", ins);
		new SQLDumpUtils(sqlUtils).evaluateSQLReader(new InputStreamReader(ins));

		ds = PortalConnectorFactory.createWriteableDatasource(handleProperties);

	}

	@After
	public void tearDown() throws Exception {
		sqlUtils.removeDatabase();
		sqlUtils.disconnectDatabase();
	}

	/**
	 * Compares a given map with a loaded resolvable ..
	 *
	 * @param expected
	 * @param actual
	 */
	private void compareMapWithResolvable(Map expected, Resolvable actual) {
		for (int i = 0; i < allKeys.length; i++) {
			String key = allKeys[i];
			Object expectedValue = expected.get(key);

			if ("myint".equals(key) && expectedValue != null) {// integers are still... strings ...
				// expectedValue = expectedValue.toString();
			} else if ("obj_type".equals(key)) {
				continue;
			} else if ("mybinary".equals(key) && expectedValue != null) {
				// just make sure that we have a binary array
				byte[] validate = (byte[]) actual.get(key);

				assertEquals("Asserting bary array is the same length for object {" + actual.get("contentid") + "}", ((byte[]) expectedValue).length,
						validate.length);
				continue;
			} else if ("mydate".equals(key) && expectedValue != null) {
				// database only stores full seconds ...
				expectedValue = new Timestamp(((Date) expectedValue).getTime() / 1000 * 1000);
			} else if (expectedValue == null && "".equals(actual.get(key))) {
				continue;
			}

			assertEquals("Asserting content object {" + actual.get("contentid") + "} - attr {" + key + "}", expectedValue, actual.get(key));
		}
	}

	/**
	 * Creates example objects ..
	 *
	 * @throws Exception
	 */
	private void createObjects() throws Exception {
		// get some arbitrary binary data... again
		InputStream stream = CNDatasourceOptimizedTest.class.getResourceAsStream("CNDatasourceOptimizedTest.class");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int b;

		while ((b = stream.read()) != -1) {
			out.write(b);
		}

		// build the attributes for some content object
		Map map = new HashMap();

		map.put("obj_type", "101");
		map.put("mystring", "some cool string");
		map.put("myint", new Integer(Integer.MAX_VALUE));
		map.put("mylongtext", "this is a very long text.... kind of .. well .. it's too boring and useless to really try it out .. so i just stop .. here.");
		map.put("mybinary", out.toByteArray());
		map.put("mylongint", new Long(Long.MAX_VALUE));
		map.put("mydouble", new Double(3e100 / 7));
		map.put("mydate", new Date());
		map.put("mylink", null);
		obj1map = map;

		// create the content object
		obj1 = ds.create(map);
		ds.insert(Collections.singleton(obj1));
		obj1contentid = (String) obj1.get("contentid");

		// Create a second object ...
		map = new HashMap();
		map.put("obj_type", "101");
		map.put("mylink", obj1);

		obj2 = ds.create(map);
		obj2map = map;
		ds.insert(Collections.singleton(obj2));
		obj2contentid = (String) obj2.get("contentid");

	}

	/**
	 * Changes all values ..
	 *
	 * @throws Exception
	 */
	private void updateObjects() throws Exception {
		Changeable ch = PortalConnectorFactory.getChangeableContentObject(obj1contentid, ds);

		obj1map.put("mystring", "another cewl string");
		ch.setProperty("mystring", "another cewl string");
		obj1map.put("myint", new Integer(32487823));
		ch.setProperty("myint", new Integer(32487823));
		obj1map.put("mylongtext",
				"waaaaaaaaaaaaaaaaaaaaaa this is a very long text.... kind of .. well .. it's too boring and useless to really try it out .. so i just stop .. here.");
		ch.setProperty("mylongtext",
				"waaaaaaaaaaaaaaaaaaaaaa this is a very long text.... kind of .. well .. it's too boring and useless to really try it out .. so i just stop .. here.");
		obj1map.put("mylongint", new Long(234723487234872343L));
		ch.setProperty("mylongint", new Long(234723487234872343L));
		obj1map.put("mydouble", new Double(-9.489383874));
		ch.setProperty("mydouble", new Double(-9.489383874));
		obj1map.put("mydate", new Date(500000000));
		ch.setProperty("mydate", new Date(500000000));
		obj1map.put("mylink", obj2);
		ch.setProperty("mylink", obj2);

		ds.store(Collections.singleton(ch));
	}

	private void setNullValues() throws Exception {
		Changeable ch = PortalConnectorFactory.getChangeableContentObject(obj1contentid, ds);

		for (int i = 0; i < allKeys.length; i++) {
			String key = allKeys[i];

			obj1map.remove(key);
			ch.setProperty(key, null);
		}
		ds.store(Collections.singleton(ch));
	}

	private void verifyObjects() throws Exception {
		compareMapWithResolvable(obj1map, PortalConnectorFactory.getContentObject(obj1contentid, ds));
		// compareMapWithResolvable(obj2map,
		// PortalConnectorFactory.getContentObject(obj2contentid, ds));
	}

	/*
	 * The only test method in here..
	 *
	 * creates the objects, updates their values and afterwards sets all attributes to null ..
	 */
	@Test
	public void testOptimizedObjectHandling() throws Exception {
		createObjects();
		verifyObjects();
		updateObjects();
		verifyObjects();
		setNullValues();
		verifyObjects();
	}

	@Test
	@Ignore("The testOptimizedObjectHandling is failing with mysql 5.1 and mysql 5.0. Add tests and fix the issue.")
	public void testOptimitzedObjectHandlingReminder() {// TODO Reminder test
	}

}
