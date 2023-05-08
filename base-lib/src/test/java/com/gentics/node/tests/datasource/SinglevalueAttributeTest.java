/*
 * @author herbert
 * @date Jul 9, 2008
 * @version $Id: SinglevalueAttributeTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.node.testutils.NodeTestUtils;
import org.junit.experimental.categories.Category;

@Ignore("This test fails - I think it should be converted to a test that does its own data setup")
@Category(BaseLibTest.class)
public class SinglevalueAttributeTest {
	public final static String TESTCONTENTID = "10007.964299";
	public final static String ATTRIBUTE = "filename";
	public final static String VALUE = "muh";

	private CNWriteableDatasource ds;
	private DBHandle dbHandle;

	@Before
	public void setUp() throws Exception {
		ds = (CNWriteableDatasource) NodeTestUtils.createWriteableDatasource(false);
		dbHandle = GenticsContentFactory.getHandle(ds);
		cleanup();
	}

	@After
	public void tearDown() throws Exception {
		cleanup();
	}

	protected void cleanup() throws Exception {
		// remove the object and all its attributes
		DB.update(dbHandle, "delete from " + dbHandle.getContentMapName() + " where contentid = ?", new String[] { TESTCONTENTID });
		DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeName() + " where contentid = ?", new String[] { TESTCONTENTID });
	}

	@Test
	public void testRemoveSingleValue() throws Exception {
		// store the object
		Map dataMap = new HashMap();

		dataMap.put("contentid", TESTCONTENTID);
		dataMap.put(ATTRIBUTE, VALUE);
		Changeable object = ds.create(dataMap, -1, false);

		ds.store(Collections.singleton(object));
		object = PortalConnectorFactory.getChangeableContentObject(TESTCONTENTID, ds);

		checkExistingAttribute();

		DB.update(dbHandle, "INSERT INTO contentattribute (contentid, name, value_text) VALUES (?,?,?)", new Object[] { TESTCONTENTID, ATTRIBUTE, VALUE });

		try {
			checkExistingAttribute();
			fail("check was not correct !!!");
		} catch (AssertionFailedError e) {// yeah .. it found our wrong attribute ;)
		}

		object.setProperty(ATTRIBUTE, VALUE);

		ds.store(Collections.singleton(object));

		checkExistingAttribute();

	}

	private void checkExistingAttribute() throws Exception {
		SimpleResultProcessor proc = new SimpleResultProcessor();

		DB.query(dbHandle, "SELECT contentid, name FROM contentattribute where contentid = ?", TESTCONTENTID, proc);
		assertEquals("we should only have one attribute ...", 1, proc.size());
		// SimpleResultRow row = proc.getRow(1);
	}

}
