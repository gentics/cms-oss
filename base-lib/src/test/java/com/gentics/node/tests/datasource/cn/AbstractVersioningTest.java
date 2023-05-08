package com.gentics.node.tests.datasource.cn;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.database.utils.SQLDumpUtils;

public class AbstractVersioningTest {

	public static Properties handleProperties;

	public static TestDatabase testDatabase;
	public final static String DATABASE_DUMP_FILENAME = "dist_tests_versioning.sql";
	public static SQLUtils sqlUtils;

	@BeforeClass
	public static void setupOnce() throws SQLUtilException, JDBCMalformedURLException {
		testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		testDatabase.setRandomDatabasename("VersioningTest" + System.currentTimeMillis());
		handleProperties = testDatabase.getSettings();
		sqlUtils = SQLUtilsFactory.getSQLUtils(handleProperties);
		sqlUtils.connectDatabase();
		sqlUtils.createDatabase();
		sqlUtils.selectDatabase();
		new SQLDumpUtils(sqlUtils).evaluateSQLStream(CNDatasourceFilterTest.class.getResourceAsStream(DATABASE_DUMP_FILENAME), false, true);
	}

	@AfterClass
	public static void tearDownOnce() throws SQLUtilException {
		sqlUtils.removeDatabase();
	}

}
