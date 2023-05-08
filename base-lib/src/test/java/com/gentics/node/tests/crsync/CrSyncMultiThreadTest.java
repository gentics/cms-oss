/*
 * @author johannes2
 * @date 18.04.2008
 * @version $Id: CrSyncMultiThreadTest.java,v 1.4 2010-09-28 17:08:09 norbert Exp $
 */
package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.JDBCSettings;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.database.utils.SQLDumpUtils;
import com.gentics.testutils.infrastructure.EnvironmentException;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class CrSyncMultiThreadTest {

	static NodeLogger logger;

	// test settings
	public static final int OBJECTSIZE = 40;

	public static final int TESTSIZE = 20;

	// source and target jdbc connections
	static DBCon source = null;
	static DBCon source2 = null;

	private static TestDatabase sourceDatabase;
	private static TestDatabase targetDatabase;

	private static SQLUtils targetSQLUtils;
	private static SQLUtils sourceSQLUtils;

	static String sourceDbType = "";

	static DBCon target = null;
	static DBCon target2 = null;

	static final int SLEEPMS = 1005;

	static String targetDbType = "";

	// source and target datasources
	static CNWriteableDatasource sourceDS = null;

	static CNWriteableDatasource targetDS = null;

	public CRSQLUtils crsqlutilsTarget;
	public CRSQLUtils crsqlutilsSource;

	private static void setupLogger() {
		// TestUtils.setupNodeLogger();
		// TestUtils.initLog4j();
		logger = NodeLogger.getNodeLogger(CrSyncMultiThreadTest.class);
	}

	/**
	 * Prepare all datasources
	 *
	 * @throws Exception
	 */
	private static void setupDatasource() throws Exception {

		// initialize datasource
		Properties srcDSConf = sourceDatabase.getSettings();

		sourceDS = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(srcDSConf);

		source = new DBCon(srcDSConf.getProperty("driverClass"), srcDSConf.getProperty("url"), srcDSConf.getProperty("username"),
				srcDSConf.getProperty("passwd"));
		source2 = new DBCon(srcDSConf.getProperty("driverClass"), srcDSConf.getProperty("url"), srcDSConf.getProperty("username"),
				srcDSConf.getProperty("passwd"));

		Properties tgtDSConf = targetDatabase.getSettings();

		// initialize writabledatasource
		targetDS = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(tgtDSConf);

		target = new DBCon(tgtDSConf.getProperty("driverClass"), tgtDSConf.getProperty("url"), tgtDSConf.getProperty("username"),
				tgtDSConf.getProperty("passwd"));
		target2 = new DBCon(tgtDSConf.getProperty("driverClass"), tgtDSConf.getProperty("url"), tgtDSConf.getProperty("username"),
				tgtDSConf.getProperty("passwd"));

	}

	/**
	 * update LastUpdateTimestamp in Datasource ds. if timestamp == -1 use
	 * current timestamp, otherwise use given timestamp. if sleepbeforeupdate is
	 * set, sleep a given time before updating to be sure that
	 * lastUpdateTimestamp is > contentmap.updatetimestamp (and not >=
	 * lastupdatetimestamp)
	 * @param ds datasource to set timestamp in
	 * @param timestamp timestamp to use. -1 to use current timestamp
	 * @param sleepBeforeUpdate sleep a given time bevore the update
	 * @throws DatasourceException
	 */
	private void touchRepository(CNWriteableDatasource ds, long timestamp, boolean sleepBeforeUpdate) throws DatasourceException {
		if (sleepBeforeUpdate) {
			sleepMs(SLEEPMS);
		}

		if (timestamp == -1) {
			ds.setLastUpdate();
		} else {
			ds.setLastUpdate(timestamp);
		}
	}

	/**
	 * sleep ms milliseconds
	 * @param ms milliseconds to sleep
	 */
	private void sleepMs(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@BeforeClass
	public static void setUpOnce() throws IOException, SQLUtilException, JDBCMalformedURLException {
		setupLogger();

		sourceDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		targetDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();

		sourceDatabase.setRandomDatabasename(CrSyncMultiThreadTest.class.getSimpleName());
		targetDatabase.setRandomDatabasename(CrSyncMultiThreadTest.class.getSimpleName());

		targetSQLUtils = SQLUtilsFactory.getSQLUtils(targetDatabase);
		sourceSQLUtils = SQLUtilsFactory.getSQLUtils(sourceDatabase);
	}

	/**
	 * empty source database tables and prepareCR.
	 */
	@Before
	public void setUp() throws Exception {

		crsqlutilsTarget = new CRSQLUtils(targetSQLUtils);
		targetSQLUtils.connectDatabase();
		targetSQLUtils.cleanDatabase();
		targetSQLUtils.createCRDatabase(getClass());

		crsqlutilsSource = new CRSQLUtils(sourceSQLUtils);
		sourceSQLUtils.connectDatabase();
		sourceSQLUtils.cleanDatabase();
		sourceSQLUtils.createCRDatabase(getClass());

		setupDatasource();
		emtpyTables(source);

		prepareContentRepository(source, sourceDS);
		fillContentRepository(source, sourceDS);
		touchRepository(sourceDS, -1, false);

	}

	@After
	public void tearDown() throws Exception {

		crsqlutilsTarget.dropTargetDatabases(8);
		emtpyTables(source);
		source.closeCON();
		target.closeCON();

		targetSQLUtils.removeDatabase();
		targetSQLUtils.disconnectDatabase();

		sourceSQLUtils.removeDatabase();
		sourceSQLUtils.disconnectDatabase();

	}

	/**
	 * add initial sample data, required for test
	 *
	 * @param db
	 * @param ds
	 * @throws DatasourceException
	 */
	private void fillContentRepository(DBCon db, CNWriteableDatasource ds) throws DatasourceException {
		// insert data
		Map attrs = new HashMap();
		Changeable co = null;

		// 10002.107
		attrs.clear();
		attrs.put("contentid", "10002.107");
		attrs.put("name", "Ordner");
		attrs.put("description", "Ordner");
		attrs.put("editor", "rb");
		attrs.put("node_id", "1");
		attrs.put("permissions", Arrays.asList(new String[] { "11", "22" }));
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10002.666
		attrs.clear();
		attrs.put("contentid", "10002.666");
		attrs.put("name", "Ordner");
		attrs.put("node_id", "1");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10002.777
		attrs.clear();
		attrs.put("contentid", "10002.777");
		attrs.put("name", "Ordner");
		attrs.put("node_id", "1");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10002.xxx
		attrs.clear();
		attrs.put("obj_type", "10002");
		attrs.put("name", "IPAX Ordner");
		attrs.put("description", "IPAX Ordner");
		attrs.put("editor", "rb");
		attrs.put("node_id", "2");
		attrs.put("permissions", Arrays.asList(new String[] { "11" }));
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10007.227
		attrs.clear();
		attrs.put("contentid", "10007.227");
		attrs.put("name", "Projektübersicht");
		attrs.put("description", "Übersicht über alle Projekte");
		attrs.put("editor", "lh");
		attrs.put("node_id", "1");
		attrs.put("folder_id", "10002.107");
		attrs.put("content", "yeah!");
		attrs.put("permissions", Arrays.asList(new String[] { "11", "22", "33" }));
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10007.277
		attrs.clear();
		attrs.put("contentid", "10007.277");
		attrs.put("name", "Seite1");
		attrs.put("description", "Erste Seite");
		attrs.put("editor", "lh");
		attrs.put("node_id", "1");
		attrs.put("folder_id", "10002.107");
		attrs.put("content", "yeah!");
		attrs.put("binary", new byte[] { 65, 66, 67 }); // ATTR_TYPE_BLOB
		attrs.put("permissions", Arrays.asList(new String[] { "11", "22", "33" }));
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10007.666
		attrs.clear();
		attrs.put("contentid", "10007.666");
		attrs.put("name", "Seite");
		attrs.put("node_id", "1");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10007.777
		attrs.clear();
		attrs.put("contentid", "10007.777");
		attrs.put("name", "Seite");
		attrs.put("node_id", "1");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

	}

	/**
	 * empty database
	 * @param db database connection
	 * @throws Exception
	 */
	private void emtpyTables(DBCon db) throws Exception {
		for (int i = 0; i < CRTableCompare.CRTABLES.length; i++) {
			db.updateSQL("DELETE FROM " + CRTableCompare.CRTABLES[i]);
		}
		db.updateSQL("DELETE FROM contentstatus");

		// drop quick columns after delete
		// dropQuickColumns(db, "contentmap");
		setUpTables(db);
	}

	/**
	 * setup tables (drop and recreate contentmap)
	 * @param db db to work on
	 * @throws NodeException
	 * @throws Exception
	 */
	private void setUpTables(DBCon db) throws NodeException {

		recreateTable(db, "contentmap");
		recreateTable(db, "contentmap_nodeversion");
	}

	/**
	 * Recreate table (drop and recreate)
	 * @param db database to work on
	 * @param table name of the table to recreate
	 * @throws NodeException
	 */
	private void recreateTable(DBCon db, String table) throws NodeException {
		String sql = null;

		// drop table contentmap
		sql = "DROP TABLE " + table;

		// ignore "table not found exception"
		db.updateSQL(sql, false);

		String type = db.getType();
		if (type.equals("mariadb")) {
			type = "mysql";
		}

		// if oracle, also drop sequence
		// (since crsync also checks internal ids, we have to reset those)
		if ("oracle".equals(type)) {
			sql = "DROP SEQUENCE " + table + "_sequence";
			db.updateSQL(sql, false);
		}

		// recreate table contentmap
		sql = CRSyncObjectManagementManagerTest.getCreateTableForCRTable(table, type);
		String[] sqlStatements = splitStatements(sql, type);

		for (int i = 0; i < sqlStatements.length; i++) {
			db.updateSQL(sqlStatements[i], false);
		}
	}

	/**
	 * Split multiple SQL statements
	 * @param statements the string containing possibly many SQL statements
	 * @param database the name of the database
	 * @return an array with one SQL statement per entry
	 */
	private String[] splitStatements(String statements, String database) {
		if ("mysql".equals(database)) {
			return statements.split(";");
		} else if ("mssql".equals(database)) {
			return statements.split("GO");
		} else if ("oracle".equals(database)) {
			return statements.split("/");
		} else {
			return new String[] { statements };
		}
	}

	/**
	 * create random string of specific length
	 * @param len length in chars/bytes
	 * @return String
	 */
	private String createRandomString(int len) {
		String str = "";
		int i = 0;

		while (i < len) {
			char c = (char) ((int) (Math.random() * 60) + 60);

			str += c;
			i++;
		}
		return str;
	}

	/**
	 * create a binary array with random data
	 * @param len length in bytes
	 * @return byte[]
	 */
	private byte[] createRandomBinaryData(int len) {
		byte[] data = new byte[len];
		int i = 0;

		while (i < len) {
			int value = (int) (Math.random() * 128);

			data[i] = (byte) value;
			i++;
		}
		return data;
	}

	/**
	 * This method is used to create CR data in the source database. It may
	 * divides the data into segments. Each segment will be used for one crsync
	 * thread.
	 * @param nSegments count of segements that will be created.
	 * @param objectSize
	 * @param wholeSize whole
	 * @return string of rules
	 * @throws DatasourceException
	 */
	public String[] createData(int nSegments, int objectSize, int wholeSize) throws DatasourceException {
		String[] rules = new String[nSegments];

		int binLenght = objectSize / 2;
		int textLenght = objectSize / 2;

		logger.info("binLenght: " + binLenght);
		logger.info("textLenght: " + textLenght);

		// create data
		byte[] data = createRandomBinaryData(binLenght);
		String text = createRandomString(textLenght);

		// calculate number of objects
		int nObjects = ((wholeSize * 1024) / ((binLenght) + (textLenght)));

		logger.info("Objects to create: " + nObjects);

		// calculate nObjects for each segement
		int nObjectsPerSegement = nObjects / nSegments;

		// store nObjectsPerSegement for each of nSegments and save specific rule
		int idvalue = 0;
		int r = 1;

		while (idvalue < nSegments) {
			logger.info("Creating Segement " + idvalue);
			int k = 0;

			while (k < nObjectsPerSegement) {
				storeObject(r, idvalue, data, text);
				k++;
				r++;
			}
			rules[idvalue] = "object.node_id == " + idvalue;
			idvalue++;
		}

		return rules;
	}

	/**
	 * store specific object in source database.
	 *
	 * @param i
	 * @param idvalue
	 * @param data
	 * @param text
	 * @throws DatasourceException
	 */
	private void storeObject(int i, int idvalue, byte[] data, String text) throws DatasourceException {
		Map attrs = new HashMap();
		Changeable co = null;

		attrs.clear();
		attrs.put("contentid", "10007." + i);
		attrs.put("name", "Data" + System.currentTimeMillis());
		attrs.put("binary", data);
		attrs.put("description", text);
		attrs.put("node_id", "13498");
		attrs.put("folder_id", "10002.107");
		attrs.put("editor", "rb");
		attrs.put("datum", "1990-01-01");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		co = sourceDS.create(attrs, -1, false);
		storeSingleChangeable(sourceDS, co);

	}

	/**
	 * setup and prefill database
	 * @param db db to setup (identical to ds)
	 * @param ds datasource to setup (identical to db)
	 * @throws Exception
	 */
	private void prepareContentRepository(DBCon db, CNWriteableDatasource ds) throws Exception {
		// drop/create/alter tables syntax
		// setUpTables(db);
		emtpyTables(db);

		DBHandle dbh = GenticsContentFactory.getHandle(ds);

		/**
		 * ************************************ setup db *************************************
		 */

		// set up contentobject, contentattributetypes (folders)
		ObjectManagementManager.createNewObject(dbh, "" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder");

		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("contentid", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("description", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null,
				null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("editor", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0,
				null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("datum", GenticsContentAttribute.ATTR_TYPE_DATE, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("euro", GenticsContentAttribute.ATTR_TYPE_DOUBLE, false, null, true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null,
				false, false),
				true);

		// set up contentattributetypes (pages)
		ObjectManagementManager.createNewObject(dbh, "" + GenticsContentObject.OBJ_TYPE_PAGE, "page");

		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("contentid", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("description", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null,
				null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("editor", GenticsContentAttribute.ATTR_TYPE_TEXT, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("node_id", GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_node_id", false, GenticsContentObject.OBJ_TYPE_PAGE, 0,
				null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("permissions", GenticsContentAttribute.ATTR_TYPE_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("folder_id", GenticsContentAttribute.ATTR_TYPE_OBJ, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE,
				GenticsContentObject.OBJ_TYPE_FOLDER, null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("content", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null,
				null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("datum", GenticsContentAttribute.ATTR_TYPE_DATE, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("binary", GenticsContentAttribute.ATTR_TYPE_BLOB, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null,
				false, false),
				true);
	}

	/**
	 * store single changeable (e.g. contentobject) c in datasource ds
	 * @param ds datasource to store c in
	 * @param c changeable (e.g. contentobject)
	 * @throws DatasourceException
	 */
	private void storeSingleChangeable(CNWriteableDatasource ds, Changeable c) throws DatasourceException {

		ds.store(Collections.singleton(c));
	}

	/**
	 * This method creates multiple cr databases.
	 *
	 * @param count
	 */
	public void prepareTargetDatabases(int count, String dbtyp) {
		CrSyncMultiThreadTest.class.getResourceAsStream("source.properties");
	}

	/**
	 * @param threads count of threads
	 * @param batchsize 1,10,100,1000
	 * @param objectSize Size in KB
	 * @param testSize Size of all Data in MB
	 * @return
	 */
	public void syncTest(int threads, int batchSize, int objectSize, int testSize) {
		String[] rules;

		// Filling sourcedatabase with data which will be synced
		try {

			// create data for crsync threads
			rules = createData(threads, objectSize, testSize);

			// creating target databases

			crsqlutilsTarget.createTargetDatabases(threads);
			Properties srcDSConf = sourceDatabase.getSettings();
			Properties tgtDSConf = targetDatabase.getSettings();

			// create properties
			Properties targetArgs = createArgs(true, tgtDSConf);
			Properties sourceArgs = createArgs(false, srcDSConf);
			Properties extraArgs = new Properties();

			extraArgs.setProperty(" -batchsize ", "" + batchSize);

			// create and run crsyncs
			assertTrue(createThreadsNwait(threads, batchSize, rules, targetArgs, sourceArgs, extraArgs));

		} catch (DatasourceException e) {
			logger.info("Error while creating data in sourcedatabase", e);
			assertTrue(false);
		}

	}

	/**
	 * CrSync test with one thread
	 *
	 * @throws IOException
	 */
	@Test
	public void testSync1Threads() throws IOException {
		syncTest(1, 100, OBJECTSIZE, TESTSIZE);

	}

	/**
	 * CrSync test with two threads
	 *
	 * @throws IOException
	 */
	@Test
	public void testSync2Threads() throws IOException {
		syncTest(2, 100, OBJECTSIZE, TESTSIZE);
	}

	/**
	 * CrSync test with four threads
	 *
	 * @throws IOException
	 */
	@Test
	public void testSync4Threads() throws IOException {
		syncTest(4, 100, OBJECTSIZE, TESTSIZE);
	}

	/**
	 * CrSync test with eight threads
	 *
	 * @throws IOException
	 */
	@Test
	public void testSync8Threads() throws IOException {
		syncTest(8, 100, OBJECTSIZE, TESTSIZE);

	}

	/**
	 * Create a crSync number of crsync threads.
	 * @param nThreads int number of threads to create
	 * @param batchSize int batchsize batchsize for each crsync thread
	 * @param rules String[] rules Array with rules. Each element of array will
	 *        be assigned to one thread.
	 * @param targetArgs Properties targetDB Properties
	 * @param sourceArgs Properties sourceDB Properties
	 * @param extraProps Properties extra Properties such as
	 * @return
	 */
	public boolean createThreadsNwait(int nThreads, int batchSize, String[] rules, Properties targetArgs, Properties sourceArgs, Properties extraProps) {
		ArrayList threads = new ArrayList();

		// create threads
		int i = 0;

		while (i < nThreads) {
			Properties targetArgsI = (Properties) targetArgs.clone();
			Properties sourceArgsI = (Properties) sourceArgs.clone();
			Properties extraPropsI = (Properties) extraProps.clone();
			CRSyncThread currentThread = new CRSyncThread(i, rules[i], targetArgsI, sourceArgsI, extraPropsI);

			threads.add(currentThread);
			i++;
		}

		// start threads
		i = 0;
		while (i < nThreads) {
			try {
				Thread.sleep(150);
			} catch (Exception e) {
				e.printStackTrace();
			}
			((CRSyncThread) threads.get(i)).start();
			i++;
		}

		// wait for termination of all threads
		i = 0;
		while (i < nThreads) {
			try {
				((CRSyncThread) (threads.get(i))).join();
			} catch (Exception e) {
				logger.info("RSyncThread was interrupted: ", e);
			}
			i++;
		}

		// check result of each thread
		i = 0;
		while (i < nThreads) {
			if (!((CRSyncThread) (threads.get(i))).status.getStatus()) {
				logger.info("CRSync failure. It took: " + ((CRSyncThread) (threads.get(i))).status.getCrSyncTime());
				return false;
			}
			i++;
		}

		// print results
		i = 0;
		while (i < nThreads) {

			System.out.println("- - - - - - -");
			ArrayList segementTimes = ((CRSyncThread) (threads.get(i))).status.getSegementTimes();
			int r = 0;

			while (r < segementTimes.size()) {
				System.out.println("Thread[" + i + "] - Segment[" + r + "] took " + segementTimes.get(r) + " ms.");
				r++;
			}

			System.out.println("Thread[" + i + "] - Successful run. CRSync took: " + ((CRSyncThread) (threads.get(i))).status.getCrSyncTime() + " ms.");

			i++;
		}
		System.out.println("- - - - - - -");
		return true;

	}

	/**
	 * @param istarget
	 * @param props
	 * @return
	 */
	public Properties createArgs(boolean istarget, Properties props) {
		return createArgs(istarget, props.getProperty("url"), props.getProperty("driverClass"), props.getProperty("username"), props.getProperty("passwd"));
	}

	/**
	 * Creating cmd line args from given parameters
	 *
	 * @param istarget
	 * @param url
	 * @param driver
	 * @param user
	 * @param pass
	 * @return
	 */
	private Properties createArgs(boolean istarget, String url, String driver, String user, String pass) {
		String destination;

		if (istarget) {
			destination = "target";
		} else {
			destination = "source";
		}
		Properties args = new Properties();

		args.setProperty(" -" + destination + "_url ", ObjectTransformer.getString(url, ""));
		args.setProperty(" -" + destination + "_driverClass ", ObjectTransformer.getString(driver, ""));
		args.setProperty(" -" + destination + "_username ", ObjectTransformer.getString(user, ""));
		args.setProperty(" -" + destination + "_passwd ", ObjectTransformer.getString(pass, ""));
		return args;
	}

	/**
	 * This class contains database utils for creating target databases.
	 *
	 * @author johannes2
	 */
	class CRSQLUtils {
		SQLUtils sqlUtils;

		public CRSQLUtils(SQLUtils sqlUtils) throws Exception {
			this.sqlUtils = sqlUtils;
		}

		/**
		 * method used to create number of target databases
		 *
		 * @param count
		 * @return boolean
		 */
		public boolean createTargetDatabases(int count) {

			boolean status = true;
			int i = 0;

			while (i < count) {
				if (!createTargetDatabase("crsync_target_", i)) {
					status = false;
				}
				i++;
			}
			return status;

		}

		public boolean dropTargetDatabases(int count) {
			boolean status = true;
			int i = 0;

			while (i < count) {
				if (!dropDatabase("crsync_target_", i)) {
					status = false;
				}
				i++;
			}
			return status;
		}

		private boolean dropDatabase(String prefix, int number) {

			// drop database
			String sql = "DROP DATABASE " + prefix + number;

			try {
				sqlUtils.executeQueryManipulation(sql);
			} catch (SQLUtilException e) {
				if (e.getErrorCode() == 1008) {
					logger.info("Database not exisiting.. continuing anyway");
				} else {
					logger.info("Error while dropping database '" + prefix + number + "'", e);
					return false;
				}

			}
			return true;
		}

		/**
		 * create one specific target database with specific name. eg.
		 * 'PREFIX_NUMBER'
		 * @param prefix String prefix name of database (such as
		 *        'crsync_target')
		 * @param number int specific number of database
		 * @return
		 */
		private boolean createTargetDatabase(String prefix, int number) {

			dropDatabase(prefix, number);

			// create database
			String sql = "CREATE DATABASE " + prefix + number;

			try {
				sqlUtils.executeQueryManipulation(sql);
			} catch (SQLUtilException e) {
				logger.info("Error while creating database '" + prefix + number + "'", e);
				return false;
			}

			// fetching the proper sqlstructure file
			String crStructureFilename = "";

			if (sqlUtils.getTestDatabase().getType() == TestDatabase.MYSQL || sqlUtils.getTestDatabase().getType() == TestDatabase.MARIADB) {
				crStructureFilename = "cr_structure_mysql.sql";
			} else if (sqlUtils.getTestDatabase().getType() == JDBCSettings.MSSQL) {
				crStructureFilename = "cr_structure_mssql.sql";
			} else if (sqlUtils.getTestDatabase().getType() == JDBCSettings.ORACLE) {
				crStructureFilename = "cr_structure_oracle.sql";
			}

			InputStream crStructureStream = GenericTestUtils.getCRStructureDump("mysql");

			// using current database
			sql = "USE " + prefix + number;
			try {
				sqlUtils.executeQueryManipulation(sql);
			} catch (SQLUtilException e) {
				logger.info("Error while seleting database '" + prefix + number + "'", e);
				return false;
			}

			// evaluating the file - create database structure
			try {
				new SQLDumpUtils(sqlUtils).evaluateSQLReader(new InputStreamReader(crStructureStream));
			} catch (Exception e) {
				logger.info("Exception while evaluating SQL file: ", e);
				return false;
			}

			return true;
		}

	}

	/**
	 * This class contains a crsync thread based on Runtime.getRuntime().exec(). Each CrSync process will run in its own JVM.
	 *
	 * @author johannes2
	 */
	class CRSyncThread extends Thread {

		public ThreadStatus status;

		public int threadNo;

		Properties targetArgs;

		Properties sourceArgs;

		Properties extraProps;

		String rule;

		boolean constructorDone = false;

		/**
		 * @param threadNo
		 * @param rule
		 * @param targetArgs
		 * @param sourceArgs
		 * @param extraProps
		 */
		public CRSyncThread(int threadNo, String rule, Properties targetArgs, Properties sourceArgs, Properties extraProps) {
			this.status = new ThreadStatus();
			this.threadNo = threadNo;

			this.targetArgs = targetArgs;
			this.sourceArgs = sourceArgs;
			this.extraProps = extraProps;
			this.rule = rule;
			constructorDone = true;
		}

		public void run() {

			// wait until constructor finished his work
			if (constructorDone) {
				try {
					status.setStatus(startCrSyncProcess(threadNo, rule, targetArgs, sourceArgs, extraProps));
				} catch (EnvironmentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					logger.info("error", e);
				}
			}
		}

		/**
		 * convert properties to string that can be used to create arguments
		 * @param prop Properties source properties
		 * @return String
		 */
		private String prop2string(Properties prop) {

			int max = prop.size() - 1;
			StringBuffer buf = new StringBuffer();
			Iterator it = prop.entrySet().iterator();

			for (int i = 0; i <= max; i++) {
				Map.Entry e = (Map.Entry) (it.next());
				Object key = e.getKey();
				Object value = e.getValue();

				buf.append((key == this ? "(this Map)" : key) + "" + (value == this ? "(this Map)" : value));

			}

			return buf.toString();

		}

		/**
		 * This method starts the crsync with the given parameters.
		 *
		 * @param threadNo
		 *            int Thread Number. Used to identify each threads output.
		 * @param rule
		 *            String rule to be used for the crsync
		 * @param targetArgs
		 *            String target database arguments (such as url, pw, username..)
		 * @param sourceArgs
		 *            String source database arguments (such as url, pw, username..)
		 * @param extraArgs
		 *            String extra arguments.
		 * @return
		 */
		private boolean startCrSyncProcess(int threadNo, String rule, Properties targetArgs, Properties sourceArgs, Properties extraArgs) throws EnvironmentException {

			// remove -target_passwd option if passwd is not set or ""
			if (targetArgs.getProperty(" -target_passwd ") != null) {
				if (!(targetArgs.getProperty(" -target_passwd ").length() > 0)) {
					targetArgs.remove(" -target_passwd ");
				}

			}
			// remove -source_passwd option if passwd is not set or ""
			if (sourceArgs.getProperty(" -source_passwd ") != null) {
				if (!(sourceArgs.getProperty(" -source_passwd ").length() > 0)) {
					sourceArgs.remove(" -source_passwd ");
				}
			}

			targetArgs.setProperty(" -target_url ", targetArgs.getProperty(" -target_url ") + "_" + threadNo);

			// adding rule and altertable option to extra arguments
			extraArgs.setProperty(" -rule ", rule);
			extraArgs.setProperty(" -allowaltertable ", "");

			// creating argument string from properties
			String args = "";

			args += prop2string(targetArgs);
			args += prop2string(sourceArgs);
			args += prop2string(extraArgs);

			File mavendependenciesDirectory = new File("target/mavendependencies");
			StringBuffer cmdBuffer = new StringBuffer();

			cmdBuffer.append(System.getProperty("java.home") + "/bin/java -classpath ");
			for (File currentFile : mavendependenciesDirectory.listFiles()) {
				if (FilenameUtils.isExtension(currentFile.getName(), "jar")) {
					cmdBuffer.append(currentFile.getAbsolutePath());
					cmdBuffer.append(":");
				}

			}
			cmdBuffer.append(new File("target/classes").getAbsolutePath());
			cmdBuffer.append(" com.gentics.api.portalnode.connector.CRSync" + " " + args);

			System.out.println(cmdBuffer.toString());
			// logger.info("CMD: " + cmd);
			try {
				long startTime = System.currentTimeMillis();
				Process p1 = Runtime.getRuntime().exec(cmdBuffer.toString());

				BufferedReader in = new BufferedReader(new InputStreamReader(p1.getInputStream()));
				long lastSegment = startTime;
				long allSegments = 0;

				// read process output
				for (String s; (s = in.readLine()) != null;) {

					if (s.indexOf("Start syncing") != -1) {
						lastSegment = System.currentTimeMillis();
					}

					if (s.indexOf("CRSync finished in") != -1) {

						int idx = s.indexOf("CRSync finished in ");
						int idx2 = s.indexOf(" ms.");

						if (idx != -1 && idx2 != -1) {

							String timestr = s.substring(idx + 19, idx2);
							long time = Long.parseLong(timestr);

							status.setCrSyncTime(time);
							// logger.info("Thread["+threadNo+"] - CrSync took: " + time);
						} else {
							logger.info("Thread[" + threadNo + "] - Time was not parseable");
						}

					} else if (s.indexOf("Synced") != -1) {

						long segmentTime = System.currentTimeMillis();

						// logger.info("Thread["+threadNo+"] - Segment took: " + (segmentTime - lastSegment));
						status.addSegementTime((segmentTime - lastSegment));
						allSegments += (segmentTime - lastSegment);
						lastSegment = segmentTime;
						// logger.info("AAAOutput[" + threadNo + "]: " +
						// System.currentTimeMillis() + " " + s);
					}

					status.addOutput(s);
				}

				// long endTime = System.currentTimeMillis();
				// status.setTime(endTime - startTime);
				logger.info("Thread[" + threadNo + "] - All segments took: " + allSegments);
			} catch (IOException e) {
				e.printStackTrace();
				return false;

			}
			return true;
		}

		/**
		 * this class contains a data structure for the crsync results
		 *
		 * @author johannes2
		 */
		class ThreadStatus {
			private String output;

			private long crsynctime;

			private boolean status;

			private ArrayList segmentTimes = new ArrayList();

			public String getOutput() {
				return this.output;
			}

			public long getCrSyncTime() {
				return this.crsynctime;
			}

			public boolean getStatus() {
				return this.status;
			}

			public ArrayList getSegementTimes() {
				return this.segmentTimes;
			}

			public void addSegementTime(long time) {
				this.segmentTimes.add("" + time);
			}

			public void setOutput(String output) {
				this.output = output;
			}

			public void addOutput(String line) {
				this.output += line + "\n";
			}

			public void setCrSyncTime(long crsynctime) {
				this.crsynctime = crsynctime;
			}

			public void setStatus(boolean status) {
				this.status = status;
			}

		}

	}

}
