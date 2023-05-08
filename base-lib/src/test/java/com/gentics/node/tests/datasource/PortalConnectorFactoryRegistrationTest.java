package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.DuplicateIdException;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for registering handles and datasources using the {@link #PortalConnectorFactory}
 */
@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class PortalConnectorFactoryRegistrationTest {
	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: valid datasource {0}, registered handle {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean validDatasource : Arrays.asList(true, false)) {
			for (boolean registeredHandle : Arrays.asList(true, false)) {
				data.add(new Object[] { validDatasource, registeredHandle });
			}
		}
		return data;
	}

	/**
	 * Valid datasource
	 */
	protected boolean validDatasource;

	/**
	 * Handle already registered
	 */
	protected boolean registeredHandle;

	/**
	 * Create a test instance
	 * @param validDatasource true for valid datasource, false for invalie
	 * @param registeredHandle true for already registered handle, false if not
	 */
	public PortalConnectorFactoryRegistrationTest(boolean validDatasource, boolean registeredHandle) {
		this.validDatasource = validDatasource;
		this.registeredHandle = registeredHandle;
	}

	/**
	 * Test creating a datasource
	 * @throws NodeException
	 */
	@Test
	public void testCreatedDatasource() throws NodeException {
		Map<String, String> handleProps = new HashMap<String, String>();
		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbc.JDBCDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:test");
		handleProps.put("username", "sa");
		handleProps.put("shutDownCommand", "SHUTDOWN");

		String handleId = StringUtils.md5(handleProps.toString());

		if (registeredHandle) {
			PortalConnectorFactory.registerHandle(handleId, HandleType.sql, handleProps);
		}

		Map<String, String> dsProps = new HashMap<String, String>();
		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", validDatasource ? "true" : "false");

		Datasource datasource = PortalConnectorFactory.createDatasource(handleProps, dsProps);

		if (validDatasource) {
			assertNotNull("Datasource creation must succeed", datasource);
		} else {
			assertNull("Datasource creation must not succeed", datasource);
		}

		// try to register a handle with the same id
		try {
			PortalConnectorFactory.registerHandle(handleId, HandleType.sql, handleProps, false);
			if (validDatasource || registeredHandle) {
				fail("Handle should already have been registered");
			}
		} catch (DuplicateIdException e) {
			if (!validDatasource && !registeredHandle) {
				fail("Handle should not have been registered for invalid datasource");
			}
		}
	}

	/**
	 * Destroy all datasources and handles
	 */
	@After
	public void tearDown() {
		PortalConnectorFactory.destroy();
	}
}
