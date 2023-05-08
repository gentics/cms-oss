/*
 * @author herbert
 * @date 03.08.2006
 * @version $Id: WriteablePortalConnectorTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.DatasourceModificationException;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class WriteablePortalConnectorTest extends AbstractLegacyNavigationDumpTest {
	private final static String TEST_CONTENTID = "10007.7865";

	private final static String TEST_CONTENTID_NAME = "Portal.Server.Production";

	private WriteableDatasource ds;

	@Before
	public void setup() throws Exception {
		prepareTestDatabase(this.getClass().getSimpleName());
		insertDumpIntoDatabase();
		ds = getWriteableDatasource();
	}

	@After
	public void tearDown() throws SQLException, IOException, SQLUtilException, JDBCMalformedURLException {
		PortalConnectorFactory.destroy();
		removeTestDatabase();
	}

	/**
	 * Returns a writable datasource by using the testdatabase connections
	 *
	 * @return
	 * @throws IOException
	 */
	private WriteableDatasource getWriteableDatasource() throws IOException {
		Properties handleProperties = getTestDatabase().getSettings();
		Properties dsProperties = new Properties();

		dsProperties.setProperty("cache", "true");
		WriteableDatasource ds = PortalConnectorFactory.createWriteableDatasource(handleProperties, dsProperties);

		return ds;
	}

	@Test
	public void testWriteablePortalConnector() throws IOException, DatasourceNotAvailableException, InsufficientPrivilegesException,
				DatasourceException {
		String newNameValue = "New Name Value";

		Changeable changeable = PortalConnectorFactory.getChangeableContentObject(TEST_CONTENTID, ds);
		String name = (String) changeable.get("name");

		System.out.println("name: " + name);
		assertEquals("Asserting that we got the correct object.", TEST_CONTENTID_NAME, name);

		changeable.setProperty("name", newNameValue);
		ds.store(Collections.singleton(changeable));

		// Make sure it was saved correctly.
		changeable = PortalConnectorFactory.getChangeableContentObject(TEST_CONTENTID, ds);
		assertEquals("Asserting that change was saved.", newNameValue, changeable.get("name"));

		changeable.setProperty("name", TEST_CONTENTID_NAME);
		ds.store(Collections.singleton(changeable));

		// assert that it was successfully changed back.
		Resolvable resolvable = PortalConnectorFactory.getContentObject(TEST_CONTENTID, ds);

		assertEquals("Asserting name was saved successfully", TEST_CONTENTID_NAME, resolvable.get("name"));
	}

	@Test
	public void testInsertFailure() throws IOException, DatasourceNotAvailableException, InsufficientPrivilegesException, DatasourceException {
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject(TEST_CONTENTID, ds);

		assertEquals("Asserting that we got the correct object.", TEST_CONTENTID_NAME, changeable.get("name"));
		changeable.setProperty("name", "harhar");
		try {
			ds.insert(Collections.singleton(changeable));
			fail("We have not received an exception.");
		} catch (DatasourceModificationException e) {// As expected..
		}

	}

	@Test
	public void testUpdateFailure() throws IOException, DatasourceException {
		Map objmap = new HashMap();

		objmap.put("obj_type", "10007");
		objmap.put("name", "test");
		Changeable changeable = ds.create(objmap);

		try {
			DatasourceInfo info = ds.update(Collections.singleton(changeable));

			fail("We have not received an exception.");
		} catch (DatasourceModificationException e) {// As expected..
		}

	}

	@Test
	public void testMultiValue() throws IOException, DatasourceNotAvailableException {
		Changeable changeable = PortalConnectorFactory.getChangeableContentObject(TEST_CONTENTID, ds);
		Object obj = changeable.get("testmultivalue");
		Object obj2 = changeable.get("testmultivaluedouble");
		Object obj3 = changeable.get("testmultivaluedate");

		System.out.println("test");
	}
}
