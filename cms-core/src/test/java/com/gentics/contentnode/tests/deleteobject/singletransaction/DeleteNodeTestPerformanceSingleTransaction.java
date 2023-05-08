/*
 * @author floriangutmann
 * @date Nov 30, 2009
 * @version $Id: DeleteNodeTestPerformanceSingleTransaction.java,v 1.3 2010-08-26 12:49:14 johannes2 Exp $
 */
package com.gentics.contentnode.tests.deleteobject.singletransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.deleteobject.DeleteAbstractTest;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.utils.SQLDumpUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Tests the performance when deleting a Node. The test has to be started with a
 * maximum memory of 64m otherwise it will fail.
 * 
 * @author floriangutmann
 */
public class DeleteNodeTestPerformanceSingleTransaction {

	/**
	 * Name of the properties file containing the database connection properties
	 */
	public final static String NODEDB_PROPERTIES_FILENAME = "node4_objectdelete_nodedb.properties";

	public static Properties dbProperties;

	public static Properties contextProperties;

	private SQLUtils dbUtils;

	private ContentNodeTestContext context;

	static {
		try {
			contextProperties = new Properties();
			contextProperties.put("contentnode.db.settings.login", "root");
			String prefix = "random_" + TestEnvironment.getRandomHash(5);

			contextProperties.setProperty(ConfigurationValue.DBFILES_PATH.getSystemPropertyName(), "/tmp/" + prefix + "MultichannellingTest/content/dbfiles");
			contextProperties.setProperty("filepath", "/tmp/" + prefix + "MultichannellingTest");
			contextProperties.setProperty("contentnode.nodepath", "/tmp/" + prefix + "MultichannellingTest");
			contextProperties.put("contentnode.feature.symlink_files", "False");
			contextProperties.put("contentnode.feature.persistentscheduler", "True");
			contextProperties.put("contentnode.feature.del_single_transaction", "true");
			contextProperties.put("contentnode.feature.inbox_to_email_optional", "false");
			contextProperties.put("contentnode.feature.inbox_to_email", "false");
			
			GenericTestUtils.initConfigPathForCache();
			dbProperties = new Properties();
			dbProperties.load(DeleteAbstractTest.class.getResourceAsStream(NODEDB_PROPERTIES_FILENAME));
		} catch (IOException e) {
			fail("Error while loading db settings - properties:" + e.getCause());
		}
	}

	@Before
	public void setUp() throws Exception {
		// Check if the HeapSize is correctly set to 64m
		if (Runtime.getRuntime().maxMemory() < 66000000 && Runtime.getRuntime().maxMemory() > 60000000) {
			throw new Exception("Test has to be started with 64m maximum heapsize (-Xmx64m)");
		}
		dbUtils = SQLUtilsFactory.getSQLUtils(dbProperties);
		dbUtils.connectDatabase();

		SQLDumpUtils dumpUtils = new SQLDumpUtils(dbUtils);
		
		dbUtils.executeQueryManipulation("DROP DATABASE IF EXISTS " + dbUtils.getTestDatabase().getDBName());
		dbUtils.executeQueryManipulation("CREATE DATABASE " + dbUtils.getTestDatabase().getDBName());
		dbUtils.executeQueryManipulation("USE " + dbUtils.getTestDatabase().getDBName());
		dumpUtils.executeSQLFileFromStream(DeleteAbstractTest.class.getResourceAsStream("node_performancetest_structure.sql"));
		dumpUtils.executeSQLFileFromStream(DeleteAbstractTest.class.getResourceAsStream("node_performancetest_data.sql"));
		dumpUtils.evaluateNestedSQLReader(new InputStreamReader(SQLUtils.class.getResourceAsStream("dumps/cn41_basic_procedures_20090402.sql"), "UTF-8"));
		dumpUtils.evaluateNestedSQLReader(new InputStreamReader(SQLUtils.class.getResourceAsStream("dumps/cn41_basic_trigger_20090402.sql"), "UTF-8"));
		dbUtils.applyChangeLog(new Date(1259691948000l), new String[] {
			"contentmap", "portal" });

		context = new ContentNodeTestContext();
	}

	/**
	 * Delete the BUA Node wher the transaction is committed for each folder
	 */
	@Test
	public void testPerformance1() throws Exception {

		Transaction t = TransactionManager.getCurrentTransaction();

		// get the BUA Node
		Node buaNode = (Node) t.getObject(Node.class, new Integer(13));

		assertNotNull("Check whether we found the Node BUA", buaNode);
		assertEquals("Check the Name 'BUA - Business Unit Architecture'", "BUA - Business Unit Architecture", buaNode.getFolder().getName());

		// delete the Node
		long startTime = System.currentTimeMillis();

		buaNode.delete();
		t.commit(true);

		long running = System.currentTimeMillis() - startTime;

		System.out.println(
				"Deletion of Node BUA took " + ((int) Math.floor(running / 60 / 1000)) + ":" + ((int) (Math.floor(running / 1000) % 60)) + " (" + (running) + " ms).");

		assertTrue("Check if delete was finished under 10 minutes", running < (10 * 60 * 1000));

	}

	@After
	public void tearDown() throws Exception {

		dbUtils.removeDatabase();
		dbUtils.disconnectDatabase();
	}
}
