package com.gentics.portalconnector.tests;

import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.database.utils.SQLDumpUtils;

public class AbstractLegacyNavigationDumpTest {

	private TestDatabase testDatabase;

	public TestDatabase getTestDatabase() {
		return testDatabase;
	}

	/**
	 * Creates an empty database with the given prefix
	 * @param databasePrefix
	 * @throws Exception
	 */
	public void prepareTestDatabase(String databasePrefix) throws Exception {
		testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		testDatabase.setRandomDatabasename(databasePrefix);
		SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		sqlUtils.connectDatabase();
		sqlUtils.createDatabase();
		sqlUtils.disconnectDatabase();
	}

	public void removeTestDatabase() throws SQLUtilException, JDBCMalformedURLException {
		SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		sqlUtils.connectDatabase();
		sqlUtils.removeDatabase();
		sqlUtils.disconnectDatabase();
	}

	/**
	 * Inserts the legacy "legacy_dist_tests_navigation.sql" dump into the current database
	 * 
	 * @throws SQLUtilException
	 * @throws JDBCMalformedURLException
	 */
	@Deprecated
	public void insertDumpIntoDatabase() throws SQLUtilException, JDBCMalformedURLException {
		SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		sqlUtils.connectDatabase();
		sqlUtils.selectDatabase();
		SQLDumpUtils dumpUtils = new SQLDumpUtils(sqlUtils);

		dumpUtils.evaluateSQLStream(CNDatasourceReadTest.class.getResourceAsStream("legacy_dist_tests_navigation.sql"), false, false);
		sqlUtils.disconnectDatabase();
	}

}
