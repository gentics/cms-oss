/*
 * @author floriangutmann
 * @date Dec 16, 2009
 * @version $Id: DeleteTest.java,v 1.3 2010-08-26 12:49:13 johannes2 Exp $
 */
package com.gentics.contentnode.tests.deleteobject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.utils.SQLDumpUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Superclass that sets up the test environment for the delete tests.
 * 
 * @author floriangutmann
 */
public class DeleteAbstractTest extends TestCase {

	/**
	 * Name of the properties file containing the database connection properties
	 */
	public final static String NODEDB_PROPERTIES_FILENAME = "node4_objectdelete_nodedb.properties";

	public final static String CRDB_PROPERTIES_FILENAME = "node4_objectdelete_cr.properties";

	/**
	 * Name of the SQL File containing the testdata
	 */
	public final static String NODEDB_TESTDATA_FILENAME = "node4_objectdelete_nodedb.sql";

	/**
	 * timeout in (ms) for waiting on the dirtqueue worker
	 */
	public final static int DIRTQUEUEWORKER_WAITTIMEOUT = 60000;

	/**
	 * Database properties
	 */
	public static Properties dbProperties;

	public static Properties crDBProperties;

	public static Properties contextProperties;

	public SQLUtils dbUtils;

	public SQLUtils crDBUtils;

	public ContentNodeTestContext context;

	public java.io.File pubDir;

	static {
		try {
			GenericTestUtils.initConfigPathForCache();
			contextProperties = new Properties();
			
			String prefix = "random_" + TestEnvironment.getRandomHash(5);

			contextProperties.setProperty(ConfigurationValue.DBFILES_PATH.getSystemPropertyName(), "/tmp/" + prefix + "MultichannellingTest/content/dbfiles");
			contextProperties.setProperty("filepath", "/tmp/" + prefix + "MultichannellingTest");
			contextProperties.setProperty("contentnode.nodepath", "/tmp/" + prefix + "MultichannellingTest");
			
			contextProperties.put("contentnode.db.settings.login", "node");
			contextProperties.put("contentnode.feature.symlink_files", "False");
			contextProperties.put("contentnode.feature.persistentscheduler", "True");
			contextProperties.put("mailhost", "mail.gentics.com");
			contextProperties.put("contentnode.feature.inbox_to_email_optional", "false");
			contextProperties.put("contentnode.feature.inbox_to_email", "false");
			
			dbProperties = new Properties();
			dbProperties.load(DeleteAbstractTest.class.getResourceAsStream(NODEDB_PROPERTIES_FILENAME));
			crDBProperties = new Properties();
			crDBProperties.load(DeleteAbstractTest.class.getResourceAsStream(CRDB_PROPERTIES_FILENAME));
		} catch (IOException e) {
			fail("Error while loading db settings - properties:" + e.getCause());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		dbUtils = SQLUtilsFactory.getSQLUtils(dbProperties);
		dbUtils.connectDatabase();
		// dbUtils.createNodeDatabaseStructureOnly();
		Date buildDate = dbUtils.createNodeDatabase();

		dbUtils.applyChangeLog(buildDate, new String[] { "contentmap", "portal" });

		SQLDumpUtils dumpUtils = new SQLDumpUtils(dbUtils);

		// insert test data
		dumpUtils.evaluateSQLReader(new InputStreamReader(getClass().getResourceAsStream(NODEDB_TESTDATA_FILENAME), "UTF-8"));

		// init the contentrepository
		crDBUtils = SQLUtilsFactory.getSQLUtils(crDBProperties);
		crDBUtils.connectDatabase();
		crDBUtils.createCRDatabase(getClass());

		// make sure that the configured contentrepository uses the correct connection data
		dbUtils.executeQueryManipulation(
				"UPDATE contentrepository SET username = '" + crDBProperties.getProperty("username") + "', password = '" + crDBProperties.getProperty("passwd")
				+ "', url = '" + crDBProperties.getProperty("url") + "' WHERE id = 1");

		// create the test context
		context = new ContentNodeTestContext();

		// set the current language
		ContentNodeHelper.setLanguageId(1);

		// add the dbfiles
		NodePreferences prefs = context.getTransaction().getNodeConfig().getDefaultPreferences();
		java.io.File dbFilesDir = new java.io.File(ConfigurationValue.DBFILES_PATH.get());

		dbFilesDir.mkdirs();
		ResultSet contentFiles = dbUtils.executeQuery("SELECT id, filesize FROM contentfile");

		while (contentFiles.next()) {
			java.io.File file = new java.io.File(dbFilesDir, contentFiles.getInt("id") + ".bin");
			int filesize = contentFiles.getInt("filesize");
			byte[] data = new byte[filesize];

			for (int i = 0; i < data.length; i++) {
				data[i] = '\1';
			}

			FileOutputStream fileStream = new FileOutputStream(file);

			fileStream.write(data);
			fileStream.close();
		}

		// get the pubdir
		pubDir = new java.io.File(
				prefs.getProperty("filepath") + java.io.File.separator + "content" + java.io.File.separator + "publish" + java.io.File.separator + "pub"
				+ java.io.File.separator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		dbUtils.removeDatabase();
		dbUtils.disconnectDatabase();
		crDBUtils.removeDatabase();
		crDBUtils.disconnectDatabase();
	}
}
