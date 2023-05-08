package com.gentics.node.tests.datasource.cn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.PoolConnection;
import com.gentics.node.tests.crsync.AbstractCRSyncTest;
import com.gentics.node.tests.crsync.DBCon;
import com.gentics.portalconnector.tests.ExtendedPortalConnectorFactory;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import org.junit.experimental.categories.Category;

/**
 * A simple datasource pool handle test that checks whether all handles are
 * correctly returned once they have been used
 *
 * @author johannes2
 *
 */
@Ignore("This test is working correctly. It can reproduce the Too many connections issue but the issue was not resolved yet since it is not that critical.")
@Category(BaseLibTest.class)
public class CNWritableDatasourcePoolTest {

	private static Properties srcDSConf;
	private static Properties tgtDSConf;
	private static CNWriteableDatasource sourceDS = null;
	private static CNWriteableDatasource targetDS = null;

	// source and target jdbc connections
	static DBCon source = null;

	static DBCon source2 = null;

	static String sourceDbType = "";

	static DBCon target = null;

	static DBCon target2 = null;

	static String targetDbType = "";

	@BeforeClass
	public static void setUpOnce() throws IOException, JDBCMalformedURLException, SQLUtilException {

		// load source ds properties and init datasource
		srcDSConf = new Properties();
		srcDSConf.load(AbstractCRSyncTest.class.getResourceAsStream("source.properties"));
		SQLUtils srcUtils = SQLUtilsFactory.getSQLUtils(srcDSConf);

		srcUtils.connectDatabase();
		srcUtils.createCRDatabase(CNWritableDatasourcePoolTest.class);

		// load source ds properties and init writabledatasource
		tgtDSConf = new Properties();
		tgtDSConf.load(AbstractCRSyncTest.class.getResourceAsStream("target.properties"));
		SQLUtils tgtUtils = SQLUtilsFactory.getSQLUtils(tgtDSConf);

		tgtUtils.connectDatabase();
		tgtUtils.createCRDatabase(CNWritableDatasourcePoolTest.class);
	}

	/**
	 * This test will test if the fetched poolhandles are correctly returned
	 * into the pool
	 *
	 * @throws Exception
	 */
	@Test
	public void testPool() throws Exception {

		for (int i = 0; i < 500; i++) {
			if (i % 10 == 0) {
				System.out.println(i);
			}
			setupDatasources();
			DBHandle handle = GenticsContentFactory.getHandle(sourceDS);

			DB.getOpenConnection(handle);
			PoolConnection c = DB.getPoolConnection(handle);

			DB.safeRelease(handle, c);
			disconnectDatasources();
		}

	}

	private void disconnectDatasources() {
		target.closeCON();
		target2.closeCON();
		source.closeCON();
		source2.closeCON();
	}

	private void setupDatasources() throws Exception {

		Map dsProps = new HashMap();

		dsProps.put(CNDatasource.ILLEGALLINKSNOTNULL, "true");

		ExtendedPortalConnectorFactory.clearHandles();
		ExtendedPortalConnectorFactory.clearDatasourceFactories();

		if (sourceDS != null) {
			sourceDS.getHandle().close();
			sourceDS.getHandlePool().close();
		}

		if (targetDS != null) {
			targetDS.getHandle().close();
			targetDS.getHandlePool().close();
		}
		sourceDS = (CNWriteableDatasource) ExtendedPortalConnectorFactory.createWriteableDatasource(srcDSConf, dsProps);
		if (sourceDS == null) {
			throw new Exception("Could not create source connection with settings: {" + srcDSConf.toString() + "}");
		}
		sourceDS.setRepairIDCounterOnInsert(false);

		source = new DBCon(srcDSConf.getProperty("driverClass"), srcDSConf.getProperty("url"), srcDSConf.getProperty("username"),
				srcDSConf.getProperty("passwd"));
		source2 = new DBCon(srcDSConf.getProperty("driverClass"), srcDSConf.getProperty("url"), srcDSConf.getProperty("username"),
				srcDSConf.getProperty("passwd"));

		targetDS = (CNWriteableDatasource) ExtendedPortalConnectorFactory.createWriteableDatasource(tgtDSConf, dsProps);
		targetDS.setRepairIDCounterOnInsert(false);

		target = new DBCon(tgtDSConf.getProperty("driverClass"), tgtDSConf.getProperty("url"), tgtDSConf.getProperty("username"),
				tgtDSConf.getProperty("passwd"));
		target2 = new DBCon(tgtDSConf.getProperty("driverClass"), tgtDSConf.getProperty("url"), tgtDSConf.getProperty("username"),
				tgtDSConf.getProperty("passwd"));

	}

}
