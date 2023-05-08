package com.gentics.node.tests.datasource;

import java.util.HashMap;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.portalconnector.tests.ExtendedPortalConnectorFactory;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import org.junit.experimental.categories.Category;

/**
 * This class will test the datasource connection pool behavior.
 *
 * @author johannes2
 *
 */
@Ignore("This test is working correctly. It can reproduce the Too many connections issue but the issue was not resolved yet since it is not that critical.")
@Category(BaseLibTest.class)
public class DatasourceConnectionTest {

	static CNWriteableDatasource targetDS = null;

	static SQLUtils targetUtils = null;
	private static TestDatabase testDatabase;

	@BeforeClass
	public static void setupOnce() throws SQLUtilException, JDBCMalformedURLException {
		testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		testDatabase.setRandomDatabasename(DatasourceConnectionTest.class.getSimpleName());
	}

	private void executeQueries(CNWriteableDatasource ds) throws Exception {

		DBHandle dbHandle = ds.getHandle().getDBHandle();
		SimpleResultProcessor rs = new SimpleResultProcessor();

		int currentRow = 0;
		int nStatements = 500;

		// do some select statements
		for (int i = 0; i < nStatements; i++) {

			DB.query(dbHandle, "show full processlist", rs);

			if (currentRow != rs.size()) {
				currentRow = rs.size();
				System.out.println("Open connections: " + currentRow);
			}

		}
		System.out.println("Executed all " + nStatements + " Statements.");
	}

	@Before
	public void setup() throws Exception {
		targetUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		targetUtils.connectDatabase();
		targetUtils.cleanDatabase();
		targetUtils.createCRDatabase(getClass());
	}

	@Test
	public void testConnectionPooling() throws Exception {

		for (int i = 0; i < 500; i++) {

			ExtendedPortalConnectorFactory.clearHandles();
			ExtendedPortalConnectorFactory.clearDatasourceFactories();

			if (targetDS != null) {
				System.out.println("Closing database handle");
				targetDS.getHandle().close();
				targetDS.getHandlePool().close();
			}

			targetDS = (CNWriteableDatasource) ExtendedPortalConnectorFactory.createWriteableDatasource(testDatabase.getSettings(), new HashMap());
			if (i % 5 == 0) {
				System.out.println("Created datasource nr " + i);
			}

			// Execute some queries
			executeQueries(targetDS);
		}

	}

	@After
	public void tearDown() throws Exception {
		targetUtils.connectDatabase();
		targetUtils.removeDatabase();
		targetUtils.disconnectDatabase();
	}
}
