/*
 * @author raoul
 * @date 17.03.2006
 * @version $Id: CRSyncTest.java,v 1.4 2010-09-28 17:08:09 norbert Exp $
 */
package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalconnector.tests.ExtendedPortalConnectorFactory;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.junit.ParallelizedParameterized;

/**
 * requires 2 defined contentrepositories - repositories will be created, see ./*.properties
 *
 * @author raoul, laurin, johannes2
 */
@RunWith(value = ParallelizedParameterized.class)
abstract public class AbstractCRSyncTest {
	/**
	 * Timeout for each test case
	 */
	public final static int TEST_TIMEOUT_MS = 15 * 60 * 1000;

	// source and target jdbc connections
	DBCon source = null;

	DBCon source2 = null;

	String sourceDbType = "";

	DBCon target = null;

	DBCon target2 = null;

	String targetDbType = "";

	// source and target datasources
	CNWriteableDatasource sourceDS = null;

	CNWriteableDatasource targetDS = null;

	SQLUtils sourceUtils = null;

	SQLUtils targetUtils = null;

	static final int SLEEPMS = 1005;

	static NodeLogger logger = NodeLogger.getNodeLogger(AbstractCRSyncTest.class);

	/**
	 * 15 kB of binary length per object
	 */
	public final static int BINLENGTH = 15 * 1024;

	/**
	 * 10 kB of text data per object
	 */
	public final static int TEXTLENGTH = 10 * 1024;

	/**
	 * number of objects synced
	 */
	public final static int OBJECTS = 1000;

	protected Properties srcDSConf;

	protected Properties tgtDSConf;

	protected TestDatabase sourceDatabase;
	protected TestDatabase targetDatabase;

	@Rule
	public TestName name = new TestName();

	// sync class
	CRSync sync = null;

	/**
	 * Create an instance with the given test parameters
	 * @param source source database
	 * @param target target database
	 */
	public AbstractCRSyncTest(TestDatabase source, TestDatabase target) {
		sourceDatabase = source;
		targetDatabase = target;
	}

	@BeforeClass
	public static void setupOnce() throws IOException, JDBCMalformedURLException, SQLUtilException {
		System.setProperty("com.gentics.portalnode.portalcache", "false");
	}

	/**
	 * setup method - prepare databases
	 */
	@Before
	synchronized public void setUp() throws Exception {
		sourceDatabase = new TestDatabase(sourceDatabase.getIdentifier(), sourceDatabase.getSettings());
		targetDatabase = new TestDatabase(targetDatabase.getIdentifier(), targetDatabase.getSettings());

		sourceUtils = SQLUtilsFactory.getSQLUtils(sourceDatabase);
		sourceUtils.connectDatabase();
		targetUtils = SQLUtilsFactory.getSQLUtils(targetDatabase);
		targetUtils.connectDatabase();

		sourceDatabase.setRandomDatabasename(getClass().getSimpleName() + "_source_");
		targetDatabase.setRandomDatabasename(getClass().getSimpleName() + "_target_");

		// load source ds properties and init datasource
		srcDSConf = sourceDatabase.getSettings();

		// load source ds properties and init writabledatasource
		tgtDSConf = targetDatabase.getSettings();

		createDatabases();
		setupDatasources();

		sync = new CRSync(sourceDS, targetDS, "", false, false, true, false, 100);
		sync.setUseLobStreams(true);

		// setup target first to get a correct LastUpdateTS
		prepareContentRepository(target, targetDS);
		fillContentRepository(target, targetDS);

		// add todelete item to target
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("name", "zu viel");
		attrs.put("contentid", GenticsContentObject.STR_OBJ_TYPE_FOLDER + ".99");
		attrs.put("node_id", "3");
		Changeable c = createFolder(targetDS, attrs);

		storeSingleChangeable(targetDS, c);

		prepareContentRepository(source, sourceDS);
		fillContentRepository(source, sourceDS);

	}

	/**
	 * Drop/purge and create the databases used for this test.
	 *
	 * @throws Exception
	 */
	private void createDatabases() throws Exception {
		logger.info("Creating databases via SQLUtils");
		sourceUtils.createCRDatabase(getClass());
		targetUtils.createCRDatabase(getClass());
	}

	/**
	 * This method will remove all created databases.
	 *
	 * @throws Exception
	 */
	private void removeDatabasesSQLUtils() throws Exception {
		logger.info("Removing databases via SQLUtils");
		if (sourceUtils != null) {
			sourceUtils.removeDatabase();
		}
		if (targetUtils != null) {
			targetUtils.removeDatabase();
		}
	}

	private void setupDatasources() throws Exception {

		logger.info("Setting up datasources");
		Map<String, String> dsProps = new HashMap<String, String>();

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

	private void disconnectDatabaseSQLUtils() throws Exception {
		if (sourceUtils != null) {
			sourceUtils.disconnectDatabase();
		}
		if (targetUtils != null) {
			targetUtils.disconnectDatabase();
		}
	}

	private void disconnectDatasources() {
		if (target != null) {
			target.closeCON();
		}
		if (target2 != null) {
			target2.closeCON();
		}
		if (source != null) {
			source.closeCON();
		}
		if (source2 != null) {
			source2.closeCON();
		}

		if (sourceDS != null) {
			sourceDS.getHandlePool().close();
			sourceDS = null;
		}

		if (targetDS != null) {
			targetDS.getHandlePool().close();
			targetDS = null;
		}
	}

	/**
	 * emtpy database
	 *
	 * @param db
	 *            database connection
	 * @throws Exception
	 */
	protected void emtpyTables(DBCon db) throws Exception {
		for (int i = 0; i < CRTableCompare.CRTABLES.length; i++) {
			try {
				db.updateSQL("DELETE FROM " + CRTableCompare.CRTABLES[i]);
			} catch (Exception e) {// ignore exceptions since we will be creating them anyway
			}
		}
		db.updateSQL("DELETE FROM contentstatus");

		// drop quick columns after delete
		// dropQuickColumns(db, "contentmap");
		setUpTables(db);
	}

	/**
	 * setup tables (drop and recreate contentmap)
	 *
	 * @param db
	 *            db to work on
	 * @throws NodeException
	 * @throws Exception
	 */
	protected void setUpTables(DBCon db) throws NodeException {
		recreateTable(db, "contentmap");
		recreateTable(db, "contentmap_nodeversion");
	}

	/**
	 * Recreate table (drop and recreate)
	 *
	 * @param db
	 *            database to work on
	 * @param table
	 *            name of the table to recreate
	 * @throws NodeException
	 */
	protected void recreateTable(DBCon db, String table) throws NodeException {
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
	 *
	 * @param statements
	 *            the string containing possibly many SQL statements
	 * @param database
	 *            the name of the database
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
	 * setup and prefill database
	 *
	 * @param db
	 *            db to setup (identical to ds)
	 * @param ds
	 *            datasource to setup (identical to db)
	 * @throws Exception
	 */
	protected void prepareContentRepository(DBCon db, CNWriteableDatasource ds) throws Exception {

		// emtpyTables(db);

		DBHandle dbh = GenticsContentFactory.getHandle(ds);

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
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("optimizedclob", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, true, "quick_optimizedclob", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false),
				true);
		ObjectManagementManager.saveAttributeType(ds,
				new ObjectAttributeBean("multivalueclob", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, true, GenticsContentObject.OBJ_TYPE_PAGE, 0, null,
				null, null, false, false),
				true);
	}

	/**
	 * add initial sample data, required for test
	 *
	 * @param db
	 * @param ds
	 * @throws DatasourceException
	 */
	protected void fillContentRepository(DBCon db, CNWriteableDatasource ds) throws DatasourceException {
		// insert data
		Map<String, Object> attrs = new HashMap<String, Object>();
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
		attrs.put("optimizedclob", "muhahahahahahahha");
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

		// 10007.777
		attrs.clear();
		attrs.put("contentid", "10007.777");
		attrs.put("name", "Seite");
		attrs.put("node_id", "1");
		attrs.put("multivalueclob", Arrays.asList(new Object[] { "dumdidumdidum", "muhahahahaha" }));
		co = ds.create(attrs, -1, false);
		storeSingleChangeable(ds, co);

	}

	/**
	 * update LastUpdateTimestamp in Datasource ds. <br>
	 * <br>
	 * if timestamp == -1 use current timestamp, otherwise use given timestamp.<br>
	 * if sleepbeforeupdate is set, sleep a given time before updating to be sure that lastUpdateTimestamp is > contentmap.updatetimestamp
	 * (and not >= lastupdatetimestamp)
	 *
	 * @param ds
	 *            datasource to set timestamp in
	 * @param timestamp
	 *            timestamp to use. -1 to use current timestamp
	 * @param sleepBeforeUpdate
	 *            sleep a given time bevore the update
	 * @throws DatasourceException
	 */
	protected void touchRepository(CNWriteableDatasource ds, long timestamp, boolean sleepBeforeUpdate) throws DatasourceException {
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
	 * update Timestamp of all objects in Datasource ds. <br>
	 * <br>
	 * if timestamp == -1 use current timestamp, otherwise use given timestamp.<br>
	 *
	 * @param db
	 *            datasource to set timestamp in
	 * @param timestamp
	 *            timestamp to use. -1 to use current timestamp
	 */
	public void touchAllObjects(DBCon db, long timestamp) {
		if (timestamp == -1) {
			timestamp = System.currentTimeMillis() / 1000;
		}
		db.updateSQL("update contentmap set updatetimestamp = '" + timestamp + "'");
	}

	/**
	 * sleep ms milliseconds
	 *
	 * @param ms
	 *            milliseconds to sleep
	 */
	private void sleepMs(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * teardown method
	 */
	@After
	public void tearDown() throws Exception {
		disconnectDatasources();
		removeDatabasesSQLUtils();
		disconnectDatabaseSQLUtils();
	}

	protected long getUsedMemory() {
		Runtime r = Runtime.getRuntime();

		return r.totalMemory() - r.freeMemory();
	}

	protected void saveData(int batchSize, int binLength, int textLength, int count) throws Exception {

		Map<String, Object> attrs = new HashMap<String, Object>();
		Changeable co = null;

		byte[] data = createRandomBinaryData(binLength); // 15 KB Binary Data
		String text = createRandomString(textLength); // 10 KB Text

		System.out.println("BatchSize[" + batchSize + "] - creating: " + count + " objects.");
		System.out.println("BatchSize[" + batchSize + "] - summarized size: " + ((count * ((binLength / 1024) + (textLength / 1024)) / 1024)) + " MB.");
		long startTime = System.currentTimeMillis();

		int i = 1;

		while (i <= (count)) {

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

			i++;
		}
		data = null;
		text = null;

		long stopTime = System.currentTimeMillis();

		System.out.println("BatchSize[" + batchSize + "] - time to create data: " + ((stopTime - startTime)) + " ms.");
	}

	/**
	 * Convert the given list to a list of Strings (for better comparing)
	 *
	 * @param list
	 *            list to convert
	 * @return list of strings
	 */
	protected List<String> convertToStringList(Object o) {
		Collection<?> list = ObjectTransformer.getCollection(o, Collections.emptyList());
		List<String> stringList = new Vector<String>();

		for (Iterator<?> iterator = list.iterator(); iterator.hasNext();) {
			stringList.add(iterator.next().toString());
		}
		return stringList;
	}

	/**
	 * create random string of specific length
	 *
	 * @param len
	 * @return String
	 */
	protected String createRandomString(int len) {
		StringBuffer str = new StringBuffer();
		int i = 0;

		while (i < len) {
			char c = (char) ((int) (Math.random() * 60) + 60);

			str.append(c);
			i++;
		}
		return str.toString();
	}

	/**
	 * create a binary array with random data
	 *
	 * @param len
	 * @return byte[]
	 */
	protected byte[] createRandomBinaryData(int len) {
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
	 * shortcut to compareRepositories with assert on errors
	 */
	protected int compareRepositories() throws Exception {
		return compareRepositories(true);
	}

	/**
	 * compare repositories and assert errors
	 *
	 * @throws Exception
	 */
	protected int compareRepositories(boolean assertOnError) throws Exception {

		// IpaxCompare icomp = new IpaxCompare(source, target, source);
		CRTableCompare icomp = new CRTableCompare(source, target, source2, target2);

		// remove emtpy lines
		cleanDbBeforeCompare(source);
		cleanDbBeforeCompare(target);

		int count = 0;

		for (int i = 0; i < CRTableCompare.CRTABLES.length; i++) {
			try {
				// icomp.compareTable(tables[i], "significant");
				icomp.compareTable(CRTableCompare.CRTABLES[i]);
				count++;
			} catch (SQLException e) {
				e.printStackTrace();
				if (assertOnError) {
					assertTrue(e.toString(), false);
				}
			} catch (CompareException ce) {
				// ce.printStackTrace();
				count = -1000;
				if (assertOnError) {
					assertTrue("table structure different for table " + CRTableCompare.CRTABLES[i] + ": " + ce.getMessage(), false);
				}
			} catch (CompareDataException cde) {
				cde.printStackTrace();
				count = -1000;
				if (assertOnError) {
					assertTrue("table data different for table " + CRTableCompare.CRTABLES[i] + ": " + cde.getMessage(), false);
				}
			} catch (TableDoesNotExistException tdnee) {
				// tdnee.printStackTrace();
				count = -1000;
				if (assertOnError) {
					assertTrue("table does not exist " + CRTableCompare.CRTABLES[i] + ": " + tdnee.getMessage(), false);
				}
			} catch (Exception e) {
				e.printStackTrace();
				count = -1000;
				if (assertOnError) {
					assertTrue("Exception: " + e.getMessage(), false);
				}
			}
		}

		return count;
	}

	/**
	 * do some db cleanup before comparing the database
	 *
	 * @param db
	 */
	protected void cleanDbBeforeCompare(DBCon db) {
		// remove empty lines
		db.updateSQL(
				"DELETE FROM contentattribute WHERE value_text IS NULL AND value_bin IS NULL AND value_int IS NULL AND value_blob IS NULL AND value_long IS NULL AND value_double IS NULL AND value_date IS NULL AND value_clob IS NULL");

		// remove updatetimestamp
		// db.updateSQL("UPDATE contentmap SET updatetimestamp = 0");

		// remove motherid labled 0.0
		// db.updateSQL("UPDATE contentmap SET motherid = '' WHERE motherid =
		// '0.0'");

		// remove sortoder
		// db.updateSQL("UPDATE contentattribute SET sortorder=0 ");

		// remove id_counter
		// db.updateSQL("UPDATE contentobject SET id_counter = 0");
	}

	/**
	 * sync and compare in a single call
	 *
	 * @throws Exception
	 */
	protected void doIt() throws Exception {

		// we want to make sure that target is older than source
		touchAllObjects(target, 1);
		touchRepository(targetDS, 1, false);

		touchAllObjects(source, 2);
		touchRepository(sourceDS, 2, false);

		logger.info("DONE TEST SETUP ---- RUNING SYNC -----");
		sync.doSync();
		assertCompare();
	}

	/**
	 * assert compare (check if compared table count matches given table count
	 *
	 * @throws Exception
	 */
	protected void assertCompare() throws Exception {
		int t = compareRepositories();

		assertTrue("checked " + t + " tables", t == CRTableCompare.CRTABLES.length);
	}

	/**
	 * create new page and add additional attributes
	 *
	 * @param ds
	 *            datasource to create page in
	 * @param additionalAttributes
	 *            additional attributes to add to the page
	 * @return Changeable new page
	 * @throws DatasourceException
	 */
	protected Changeable createPage(CNWriteableDatasource ds, Map<String, Object> additionalAttributes) throws DatasourceException {
		Map<String, Object> attrs = new HashMap<String, Object>();

		if (attrs.get("contentid") == null) {
			attrs.put("contentid", GenticsContentObject.STR_OBJ_TYPE_PAGE + ".13");
		}

		attrs.put("name", "Priority");
		attrs.put("editor", "rb");
		attrs.put("description", "Priority Websites");
		attrs.put("node_id", "1");
		attrs.put("folder_id", "10002.107");
		attrs.put("datum", "1990-01-01");
		attrs.put("binary", new byte[] { 65, 65, 65 });
		attrs.put("content",
				"Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aliquam tellus elit, placerat sit amet, semper nec, fermentum et, mi. Nullam mi. Pellentesque dui. Phasellus libero dui, venenatis vel, convallis nec, dignissim eget, nulla. Praesent ante urna, aliquam vitae, congue vel, posuere ac, massa. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Proin rutrum risus in orci. Curabitur lacinia, diam ac scelerisque posuere, ligula lacus laoreet sem, ac venenatis odio nisl sed lectus. Donec pretium, magna sed lacinia dignissim, mauris ligula auctor ante, in ultrices purus nunc id lorem. Morbi mattis nulla aliquam enim varius ornare. Morbi eu quam. Proin in leo. Suspendisse id felis eget eros molestie placerat. Fusce congue, dui quis fermentum viverra, erat risus volutpat odio, quis tristique purus purus in massa. Phasellus fringilla ullamcorper massa. In arcu. Nunc leo ligula, aliquet feugiat, tempor vel, tristique sed, sem. Maecenas auctor, dui eget vestibulum aliquam, dolor turpis gravida tortor, quis convallis arcu risus eu mauris. Morbi nisl.");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));
		if (additionalAttributes != null) {
			attrs.putAll(additionalAttributes);
		}

		return ds.create(attrs, -1, false);
	}

	/**
	 * use this to get a new contentobject with contentid and attributes attrs for datasource ds
	 *
	 * @param ds
	 *            datasource to create contentobject for
	 * @param contentId
	 *            use this contentid
	 * @param additionalAttributes
	 *            additional attributes to add
	 * @return
	 * @throws DatasourceException
	 */
	protected Changeable getContentObject(CNWriteableDatasource ds, String contentId, Map<String, Object> additionalAttributes) throws DatasourceException {
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put("contentid", contentId);

		// add additionalAttributes
		if (additionalAttributes != null) {
			attrs.putAll(additionalAttributes);
		}

		// create ContentObject but do not check for existnace
		// (so its possible to create new ContentObjects)
		return ds.create(attrs, -1, false);
	}

	/**
	 * create new folder and add additional attributes
	 *
	 * @param ds
	 *            datasource to create page in
	 * @param additionalAttributes
	 *            additional attributes to add to the folder
	 * @return Changeable new folder
	 * @throws DatasourceException
	 */
	protected Changeable createFolder(CNWriteableDatasource ds, Map<String, Object> additionalAttributes) throws DatasourceException {
		Map<String, Object> attrs = new HashMap<String, Object>();

		if (attrs.get("contentid") == null) {
			attrs.put("contentid", GenticsContentObject.STR_OBJ_TYPE_FOLDER + ".13");
		}
		attrs.put("name", "Priority");
		attrs.put("editor", "rb");
		attrs.put("description", "Priority Websites");
		if (attrs.get("node_id") == null) {
			attrs.put("node_id", "1");
		}
		attrs.put("datum", "2007-07-07");
		attrs.put("euro", "1,2");
		attrs.put("permissions", Arrays.asList(new String[] { "1", "2" }));

		if (additionalAttributes != null) {
			attrs.putAll(additionalAttributes);
		}

		return ds.create(attrs, -1, false);

	}

	/**
	 * store singe changeable (e.g. contentobject) c in datasource ds
	 *
	 * @param ds
	 *            datasource to store c in
	 * @param c
	 *            changeable (e.g. contentobject)
	 * @throws DatasourceException
	 */
	protected void storeSingleChangeable(CNWriteableDatasource ds, Changeable c) throws DatasourceException {

		ds.store(Collections.singleton(c));
	}

	public boolean isRemoteOracle() throws Exception {
		Properties srcDSConf = new Properties();

		srcDSConf.load(AbstractCRSyncTest.class.getResourceAsStream("source.properties"));
		if ("oracle.jdbc.driver.OracleDriver".equalsIgnoreCase(srcDSConf.getProperty("driverClass"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Helper method to get the attributetype with given name from the datasource with given handle
	 *
	 * @param handle
	 *            handle of the datasource
	 * @param name
	 *            attribute name
	 * @return attribute instance or null if not found
	 */
	protected ObjectAttributeBean getAttributeType(DBHandle handle, String name) {
		if (name == null || handle == null) {
			return null;
		}
		Collection<ObjectAttributeBean> targetAttributeTypes = ObjectManagementManager.loadAttributeTypes(handle);

		for (ObjectAttributeBean targetAttribute : targetAttributeTypes) {
			if (name.equals(targetAttribute.getName())) {
				return targetAttribute;
			}
		}

		return null;
	}
}
