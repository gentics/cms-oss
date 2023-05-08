package com.gentics.node.tests.datasource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.DatasourceType;
import com.gentics.api.portalnode.connector.HandleType;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import org.junit.experimental.categories.Category;

/**
 * Test cases for sanitycheck
 */
@Category(BaseLibTest.class)
public class PortalConnectorSanitycheckTest {
	private static final String DS2_ID = "mccr2";
	private static final String DS1_ID = "mccr";
	private static final String HANDLE_ID = "handle";

	/**
	 * Test using test old sanitycheck for mccr datasources (must not cause errors)
	 * @throws NodeException
	 */
	@Test
	public void testSanitycheckMCCR() throws NodeException {
		// create a datasource handle
		Map<String, String> handleProps = new HashMap<String, String>();
		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbc.JDBCDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:test");
		handleProps.put("username", "sa");
		handleProps.put("shutDownCommand", "SHUTDOWN");
		PortalConnectorFactory.registerHandle(HANDLE_ID, HandleType.sql, handleProps);

		// register a mccr datasource with autorepair2, which will create the schema for mccr datasources
		Map<String, String> dsProps = new HashMap<String, String>();
		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", "true");
		PortalConnectorFactory.registerDatasource(DS1_ID, DatasourceType.mccr, dsProps, Arrays.asList(HANDLE_ID));

		// register another mccr datasource with sanitycheck (but not autorepair). This should cause no errors, because sanitycheck will not be done for mccr datasources
		dsProps.clear();
		dsProps.put("sanitycheck", "true");
		dsProps.put("autorepair", "false");
		PortalConnectorFactory.registerDatasource(DS2_ID, DatasourceType.mccr, dsProps, Arrays.asList(HANDLE_ID));
	}

	@After
	public void tearDown() {
		PortalConnectorFactory.destroy();
	}
}
