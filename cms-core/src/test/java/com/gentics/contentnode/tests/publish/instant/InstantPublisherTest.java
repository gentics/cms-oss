/**
 * 
 */
package com.gentics.contentnode.tests.publish.instant;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Ignore;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.utils.SQLDumpUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;
import com.gentics.testutils.sandbox.GCNSandboxHelper;

/**
 * @author norbert
 * 
 */
@Ignore("This test fails due to an data inconsistency exception")
public class InstantPublisherTest extends TestCase {
	public final static String NODEDB_TESTDATA_FILENAME = "node4_instantpublisher_nodedb.sql";

	/**
	 * Name of the contentrepository
	 */
	public final static String CR_NAME = "Migrated from Configuration file";

	/**
	 * Context properties
	 */
	public static Properties contextProperties;

	/**
	 * Static initialization
	 */
	static {

		contextProperties = new Properties();
		String prefix = "random_" + TestEnvironment.getRandomHash(5);

		contextProperties.setProperty(ConfigurationValue.DBFILES_PATH.getSystemPropertyName(), "/tmp/" + prefix + "MultichannellingTest/content/dbfiles");
		contextProperties.setProperty("filepath", "/tmp/" + prefix + "MultichannellingTest");
		contextProperties.setProperty("contentnode.nodepath", "/tmp/" + prefix + "MultichannellingTest");
		contextProperties.put("contentnode.db.settings.login", "root");
		contextProperties.put("contentnode.feature.symlink_files", "False");
		contextProperties.put("contentnode.feature.instant_cr_publishing", "True");
			
		GenericTestUtils.initConfigPathForCache();
	}

	protected ContentNodeTestContext context;
	private SQLUtils nodeDBUtils;

	private Map<String, SQLUtils> crSQLUtils;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		// create the context
		context = new ContentNodeTestContext(false, false);

		// create sqlUtils for the node db
		nodeDBUtils = context.createNodeDBSQLUtils();

		// establish connection
		nodeDBUtils.connectDatabase();

		// create node db (latest version)
		Date buildDate = nodeDBUtils.createNodeDatabase();

		nodeDBUtils.applyChangeLog(buildDate, new String[] { "contentmap", "portal" });

		SQLDumpUtils dumpUtils = new SQLDumpUtils(nodeDBUtils);

		// insert test data
		dumpUtils.evaluateSQLReader(new InputStreamReader(getClass().getResourceAsStream(NODEDB_TESTDATA_FILENAME), "UTF-8"));

		// now start a transaction
		context.startTransaction();

		// create db files
		context = new ContentNodeTestContext(true, false);
		NodePreferences prefs = context.getTransaction().getNodeConfig().getDefaultPreferences();
		java.io.File dbFilesDir = new java.io.File(ConfigurationValue.DBFILES_PATH.get());
		dbFilesDir.mkdirs();
		GCNSandboxHelper.downloadDBFiles(dbFilesDir);

		// create databases for all contentrepositories
		crSQLUtils = context.createCRDBSQLUtils();
		for (Iterator<SQLUtils> i = crSQLUtils.values().iterator(); i.hasNext();) {
			SQLUtils crSQLUtil = i.next();

			crSQLUtil.connectDatabase();
			crSQLUtil.createCRDatabase(getClass());
		}

		// publish everything
		publishAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		nodeDBUtils.removeDatabase();
		nodeDBUtils.disconnectDatabase();
		nodeDBUtils = null;

		if (crSQLUtils != null) {
			for (Iterator<SQLUtils> i = crSQLUtils.values().iterator(); i.hasNext();) {
				SQLUtils crSQLUtil = i.next();

				crSQLUtil.removeDatabase();
				crSQLUtil.disconnectDatabase();
			}
		}

		crSQLUtils = null;

		// clear the nodeobject cache
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();
	}

	/**
	 * Convenience method to perform a full publish process, check whether the publish process succeeds and return the duration in ms
	 * 
	 * @param multithreaded
	 *            true when the publish process shall be done multithreaded, false for singlethreaded
	 * @return duration in ms
	 * @throws Exception
	 */
	protected long publishAll() throws Exception {
		long start = System.currentTimeMillis();
		PublishInfo info = context.publish(true);
		long end = System.currentTimeMillis();

		assertEquals("Check status of publish process: ", PublishInfo.RETURN_CODE_SUCCESS, info.getReturnCode());
		return (end - start);
	}

	/**
	 * Test creating a new folder
	 * 
	 * @throws Exception
	 */
	public void testCreateFolder() throws Exception {// TODO
	}

	/**
	 * Test modification of a folder metadata
	 * 
	 * @throws Exception
	 */
	public void testModifyFolderMetadata() throws Exception {// TODO
	}

	/**
	 * Test moving a folder
	 * 
	 * @throws Exception
	 */
	public void testMoveFolder() throws Exception {// TODO
	}

	/**
	 * Test modification of a folder's object property
	 * 
	 * @throws Exception
	 */
	public void testModifyFolderObjectProperty() throws Exception {// TODO
	}

	/**
	 * Test deleting a folder
	 * 
	 * @throws Exception
	 */
	public void testDeleteFolder() throws Exception {
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;
		int folderId = 12;

		// check whether the folder exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10002." + folderId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting folders in the contentrepository before removing", 1, res.getInt("c"));

		// remove the folder
		Transaction t = context.getTransaction();

		Folder folder = (Folder) t.getObject(Folder.class, folderId);

		assertNotNull("Check whether the folder was found in the backend", folder);
		folder.delete();

		t.commit();

		// check whether the folder no longer exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10002." + folderId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting folders in the contentrepository after removing", 0, res.getInt("c"));
	}

	/**
	 * Test creating a file
	 * 
	 * @throws Exception
	 */
	public void testCreateFile() throws Exception {
		Transaction t = context.getTransaction();
		int folderId = 12;
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;

		// check initial number of files in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE obj_type = ?");
		pst.setInt(1, 10008);
		res = pst.executeQuery();
		res.next();
		assertEquals("Check initial number of files", 7, res.getInt("c"));

		// create a new file
		File file = (File) t.createObject(File.class);

		file.setName("testfile.txt");
		file.setFileStream(getClass().getResourceAsStream("testfile.txt"));
		file.setDescription("This is the instantly published testfile");
		file.setFolderId(folderId);
		file.setFiletype("text/plain");

		// save the file
		file.save();
		t.commit();

		// now check whether the file exists and has the correct data
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10008." + file.getId());
		res = pst.executeQuery();
		res.next();
		assertEquals("Check the new created file", 1, res.getInt("c"));

		// check number of files in the contentrepository after new file was added
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE obj_type = ?");
		pst.setInt(1, 10008);
		res = pst.executeQuery();
		res.next();
		assertEquals("Check number of files after file creation", 8, res.getInt("c"));
	}

	/**
	 * Test modifying a file's metadata
	 * 
	 * @throws Exception
	 */
	public void testModifyFileMetadata() throws Exception {
		Transaction t = context.getTransaction();
		int fileId = 3;
		String originalName = "testfile1.1.txt";
		String newName = "newname.txt";

		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;

		// Check the original filename
		pst = c.prepareStatement("SELECT quick_name FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10008." + fileId);
		res = pst.executeQuery();
		res.next();
		assertEquals("Check original filename", originalName, res.getString("quick_name"));

		// modify the filename
		File file = (File) t.getObject(File.class, fileId, true);

		assertNotNull("Check whether file was found in backend", file);
		file.setName(newName);
		file.save();
		t.commit();

		// Check the new filename
		res = pst.executeQuery();
		res.next();
		assertEquals("Check filename after modifying", newName, res.getString("quick_name"));
	}

	/**
	 * Test modifying a file's content
	 * 
	 * @throws Exception
	 */
	public void testModifyFileContents() throws Exception {
		Transaction t = context.getTransaction();
		int fileId = 3;
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;

		// modify the file contents
		File file = (File) t.getObject(File.class, fileId, true);

		assertNotNull("Check whether file was found in backend", file);
		file.setFileStream(getClass().getResourceAsStream("testfile.txt"));
		file.save();
		t.commit();

		// Check the new file contents
		pst = c.prepareStatement("SELECT value_blob FROM contentattribute WHERE contentid = ? AND name = ?");
		pst.setString(1, "10008." + fileId);
		pst.setString(2, "binarycontent");
		res = pst.executeQuery();
		res.next();
		String storedContents = FileUtil.stream2String(res.getBinaryStream("value_blob"), "UTF-8");
		String fileContents = FileUtil.stream2String(getClass().getResourceAsStream("testfile.txt"), "UTF-8");

		assertEquals("Check file contents after modifying in backend", fileContents, storedContents);
	}

	/**
	 * Test moving a file
	 * 
	 * @throws Exception
	 */
	public void testMoveFile() throws Exception {
		Transaction t = context.getTransaction();
		int fileId = 3;
		int oldFolderId = 7;
		int newFolderId = 8;
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;

		// check folder id before moving
		pst = c.prepareStatement("SELECT quick_folder_id FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10008." + fileId);
		res = pst.executeQuery();
		res.next();
		assertEquals("Check folder id before moving", "10002." + oldFolderId, res.getString("quick_folder_id"));

		// move the file
		File file = (File) t.getObject(File.class, fileId, true);

		file.setFolderId(newFolderId);
		file.save();
		t.commit();

		// check folder id after moving
		res = pst.executeQuery();
		res.next();
		assertEquals("Check folder id after moving", "10002." + newFolderId, res.getString("quick_folder_id"));
	}

	/**
	 * Test modifying a file's object property
	 * 
	 * @throws Exception
	 */
	public void testModifyFileObjectProperty() throws Exception {// TODO
	}

	/**
	 * Test removing a file
	 * 
	 * @throws Exception
	 */
	public void testDeleteFile() throws Exception {
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;
		int fileId = 7;

		// check whether the file exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10008." + fileId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting files in the contentrepository before removing", 1, res.getInt("c"));

		// remove the folder
		Transaction t = context.getTransaction();

		File file = (File) t.getObject(File.class, fileId);

		assertNotNull("Check whether the file was found in the backend", file);
		file.delete();

		t.commit();

		// check whether the file no longer exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10008." + fileId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting files in the contentrepository after removing", 0, res.getInt("c"));
	}

	/**
	 * Test creating (first publishing) a page
	 * 
	 * @throws Exception
	 */
	public void testCreatePage() throws Exception {// TODO
	}

	/**
	 * Test modifying a page's metadata (and republish)
	 * 
	 * @throws Exception
	 */
	public void testModifyPageMetadata() throws Exception {// TODO
	}

	/**
	 * Test modifying a page's content (and republish)
	 * 
	 * @throws Exception
	 */
	public void testModifyPageContent() throws Exception {// TODO
	}

	/**
	 * Test modifying a page's object property (and republish)
	 * 
	 * @throws Exception
	 */
	public void testModifyPageObjectProperty() throws Exception {// TODO
	}

	/**
	 * Test moving a page
	 * 
	 * @throws Exception
	 */
	public void testMovePage() throws Exception {// TODO
	}

	/**
	 * Test taking a page offline
	 * 
	 * @throws Exception
	 */
	public void testTakeOfflinePage() throws Exception {// TODO
	}

	/**
	 * Test removing a page
	 * 
	 * @throws Exception
	 */
	public void testDeletePage() throws Exception {
		SQLUtils crSQLUtil = (SQLUtils) (crSQLUtils.get(CR_NAME));
		Connection c = crSQLUtil.getConnection();
		PreparedStatement pst;
		ResultSet res;
		int pageId = 7;

		// check whether the page exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10007." + pageId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting pages in the contentrepository before removing", 1, res.getInt("c"));

		// remove the folder
		Transaction t = context.getTransaction();

		Page page = (Page) t.getObject(Page.class, pageId);

		assertNotNull("Check whether the page was found in the backend", page);
		page.delete();

		t.commit();

		// check whether the page no longer exists in the contentrepository
		pst = c.prepareStatement("SELECT count(*) c FROM contentmap WHERE contentid = ?");
		pst.setString(1, "10007." + pageId);

		res = pst.executeQuery();
		res.next();

		assertEquals("Check number of interesting pages in the contentrepository after removing", 0, res.getInt("c"));
	}
}
