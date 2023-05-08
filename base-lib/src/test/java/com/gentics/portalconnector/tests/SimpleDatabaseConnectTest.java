/*
 * @author herbert
 * @date 28.03.2006
 * @version $Id: SimpleTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.portalconnector.tests;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class SimpleDatabaseConnectTest extends AbstractSingleVariationDatabaseTest {

	public SimpleDatabaseConnectTest(TestDatabase testDatabase) {
		super(testDatabase);
	}

	@Test
	public void testSystemProperties() {
		System.out.println("Outputting all system properties (to verify java version & co)");
		Properties props = System.getProperties();
		Enumeration enumeration = props.keys();

		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			Object value = props.get(key);

			System.out.println("{" + key + "}: {" + value + "}");
		}
		System.out.println("-------------------------------------------");
	}

	@Test
	public void testDatabaseDriverVersion() throws SQLException, ClassNotFoundException, IOException, SQLUtilException, JDBCMalformedURLException {
		TestDatabase testDatabase = getTestDatabase();

		Properties props = new Properties();
		SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		sqlUtils.connectDatabase();
		Connection conn = sqlUtils.getConnection();

		DatabaseMetaData metaData = conn.getMetaData();

		System.out.println("databaseNameVersion: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
		System.out.println("databaseMajorMinorVersion: " + metaData.getDatabaseMajorVersion() + "." + metaData.getDatabaseMinorVersion());
		System.out.println("driverName: " + metaData.getDriverName());
		System.out.println("driverVersion: " + metaData.getDriverVersion());
		System.out.println("driverMajorMinorVersion: " + metaData.getDriverMajorVersion() + "." + metaData.getDriverMinorVersion());
	}
}
