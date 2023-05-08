package com.gentics.contentnode.tests.publish.cr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.junit.After;
import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest.Type;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCFolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;

/**
 * Sandbox helper to encapsulate setup and teardown of a test sandbox
 */
public class AbstractLocalizedObjectsPublishingTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();
	
	public final static String CR_DIRNAME = "CR";

	/**
	 * CN datasources for the tests (keys are the node ids)
	 */
	protected Map<Integer, CNDatasource> datasources = new HashMap<Integer, CNDatasource>();

	/**
	 * The master node for testing
	 */
	protected Node master;

	/**
	 * The sub-channel for testing
	 */
	protected Node channel;

	protected Map<Integer, TestDatabase> testDatabases = new HashMap<Integer, TestDatabase>();

	/**
	 * Test user
	 */
	protected SystemUser user;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	public static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (ObjectType objectType : ObjectType.values()) {
			for (boolean instant : Arrays.asList(true, false)) {
				testData.add(new Object[] { objectType, instant });
			}
		}
		return testData;
	}

	/**
	 * Remove test databases
	 */
	@After
	public void tearDown() throws Exception {
		for (TestDatabase testDatabase : testDatabases.values()) {
			SQLUtils testDbUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
			testDbUtils.connectWithoutSelectingDatabase();
			testDbUtils.removeDatabase();
			testDbUtils.disconnectDatabase();
		}
	}

	/**
	 * Get the test database for the given node
	 * 
	 * @param nodeId
	 *            node id
	 * @return test database
	 * @throws Exception
	 */
	public TestDatabase getTestDatabase(Integer nodeId) throws Exception {
		TestDatabase testDatabase = testDatabases.get(nodeId);
		if (testDatabase == null) {
			testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();

			SQLUtils testDbUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
			testDbUtils.connectDatabase();
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			testDbUtils.createDatabase();
			testDbUtils.selectDatabase(testDatabase.getDBName());
			testDatabases.put(nodeId, testDatabase);
		}
		return testDatabase;
	}

	/**
	 * Setup the test data
	 * 
	 * @param runPublishProcess
	 *            true to run the publish process
	 * @param instantPublishing
	 *            true to use instant publishing
	 * @throws Exception
	 */
	public void setUpTestData(boolean runPublishProcess, boolean instantPublishing) throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.CR_FILESYSTEM_ATTRIBUTES.name().toLowerCase(), true);

		testContext.startTransactionWithPermissions(false);

		user = Trx.supply(t -> t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS));

		// create master node
		master = createNode("masterNode", "masterNode", instantPublishing);

		// create channel node
		channel = createChannel(master, "channelNode", "channelNode", instantPublishing);

		if (runPublishProcess) {
			runPublishProcess();
		}
	}

	public void runPublishProcess() throws Exception {
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());
	}

	/**
	 * Create the template
	 * 
	 * @param folder
	 *            folder where to create the template
	 * @return template
	 * @throws Exception
	 */
	public Template createTemplate(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a template
		Template template = t.createObject(Template.class);

		template.setName("Template using file");
		template.setMlId(1);
		template.setSource("<node page.name>");
		template.setFolderId(folder.getId());
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a new Page
	 * 
	 * @return
	 * @throws Exception
	 */
	public Page createPage() throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a page in the master, using this file
		Page page = t.createObject(Page.class);

		page.setTemplateId(createTemplate(master.getFolder()).getId(), true);
		page.setName("page_test1");
		page.setFolderId(master.getFolder().getId());
		page.getChannelSetId();
		page.save();
		t.commit(false);

		page.publish();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return page;
	}

	/**
	 * Localize page
	 * 
	 * @param channel
	 * @param page
	 * @return
	 * @throws Exception
	 */
	public Page localizePage(Node channel, Page page) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page localizedPage = (Page) page.copy();

		localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
		localizedPage.save();
		t.commit(false);

		localizedPage.publish();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedPage;
	}

	/**
	 * Create new file
	 * 
	 * @return
	 * @throws Exception
	 */
	public File createFile() throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a page in the master, using this file
		File file = t.createObject(File.class);

		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("textfile_1.txt");
		file.setFolderId(master.getFolder().getId());
		file.getChannelSetId();
		file.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return file;
	}

	/**
	 * Localize file
	 * 
	 * @param channel
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public File localizeFile(Node channel, File file) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		File localizedFile = (File) file.copy();

		localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
		localizedFile.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedFile;
	}

	/**
	 * Create new image
	 * 
	 * @param name
	 *            name
	 * @return {@link ImageFile} instance
	 * @throws Exception
	 */
	public ImageFile createImage() throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a page in the master, using this file
		ImageFile file = t.createObject(ImageFile.class);

		file.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		file.setName("blume_1.jpg");
		file.setFolderId(master.getFolder().getId());
		file.getChannelSetId();
		file.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return file;
	}

	/**
	 * Localize image
	 * 
	 * @param channel
	 * @param image
	 * @return
	 * @throws Exception
	 */
	public ImageFile localizeImage(Node channel, ImageFile image) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile localizedImage = (ImageFile) image.copy();

		localizedImage.setChannelInfo(channel.getId(), image.getChannelSetId());
		localizedImage.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedImage;
	}

	/**
	 * Create new folder
	 * 
	 * @param template
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public Folder createFolder() throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a page in the master, using this file
		Folder folder = t.createObject(Folder.class);

		folder.setName("folder_1");
		folder.setMotherId(master.getFolder().getId());
		folder.getChannelSetId();
		folder.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return folder;
	}

	public Folder localizeFolder(Node channel, Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder localizedFolder = (Folder) folder.copy();

		localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return localizedFolder;
	}

	public void createTagmapEntries(ContentRepository cr) throws NodeException {
		ContentNodeTestDataUtils.addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"page.ismaster", "ismaster", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(cr, ContentFile.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"file.ismaster", "ismaster", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(cr, Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"folder.ismaster", "ismaster", null, false, false, true, -1, null, null);

		ContentNodeTestDataUtils.addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"page.inherited", "inherited", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(cr, ContentFile.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"file.inherited", "inherited", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(cr, Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_INTEGER,
				"folder.inherited", "inherited", null, false, false, true, -1, null, null);
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param instantPublishing
	 *            true to use instant publishing into the contentrepository
	 * @return node
	 * @throws Exception
	 */
	public Node createNode(String hostName, String name, boolean instantPublishing) throws Exception {
		Node node = ContentNodeTestDataUtils.createNode(hostName, name, PublishTarget.BOTH, false, instantPublishing);
		ContentRepository cr = node.getContentRepository();

		createTagmapEntries(cr);

		datasources.put(node.getId(), node.getContentMap().getDatasource());

		return node;
	}

	/**
	 * Unlocalize specified page.
	 * 
	 * @param localizedPage
	 * @throws Exception
	 */
	public void unlocalizePage(Page localizedPage) throws Exception {
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.page));
		unlocalizeRequest.setChannelId(ObjectTransformer.getInt(channel.getId(), 0));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCPageResourceImpl().unlocalize(ObjectTransformer.getInt(localizedPage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}
	}

	/**
	 * Unlocalize specified image.
	 * 
	 * @param localizedImage
	 * @throws Exception
	 */
	public void unlocalizeImage(ImageFile localizedImage) throws Exception {
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.image));
		unlocalizeRequest.setChannelId(ObjectTransformer.getInt(channel.getId(), 0));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCImageResourceImpl().unlocalize(ObjectTransformer.getInt(localizedImage.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}
	}

	/**
	 * Unlocalize specified file.
	 * 
	 * @param localizedFile
	 * @throws Exception
	 */
	public void unlocalizeFile(File localizedFile) throws Exception {
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.file));
		unlocalizeRequest.setChannelId(ObjectTransformer.getInt(channel.getId(), 0));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFileResourceImpl().unlocalize(ObjectTransformer.getInt(localizedFile.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}
	}

	/**
	 * Unlocalize specified folder.
	 * 
	 * @param localizedFolder
	 * @throws Exception
	 */
	public void unlocalizeFolder(Folder localizedFolder) throws Exception {
		UnlocalizeRequest unlocalizeRequest = new UnlocalizeRequest();

		unlocalizeRequest.setForegroundTime(Integer.MAX_VALUE);
		unlocalizeRequest.setRecursive(false);
		unlocalizeRequest.setTypes(Arrays.asList(Type.folder));
		unlocalizeRequest.setChannelId(ObjectTransformer.getInt(channel.getId(), 0));

		try (Trx trx = new Trx(user)) {
			GenericResponse response = new MCFolderResourceImpl().unlocalize(ObjectTransformer.getInt(localizedFolder.getId(), -1), unlocalizeRequest);
			assertEquals("Check if the response is OK.", ResponseCode.OK, response.getResponseInfo().getResponseCode());
			trx.success();
		}
	}

	/**
	 * Create a new Channel
	 * 
	 * @param master
	 *            master node
	 * @param hostName
	 *            hostname
	 * @param name
	 *            name
	 * @param instantPublishing
	 *            true to use instant publishing into the contentrepository
	 * @return channel
	 * @throws Exception
	 */
	public Node createChannel(Node master, String hostName, String name, boolean instantPublishing) throws Exception {
		Node node = ContentNodeTestDataUtils.createChannel(master, name, hostName, "/", PublishTarget.BOTH, instantPublishing);
		ContentRepository cr = node.getContentRepository();

		createTagmapEntries(cr);

		datasources.put(node.getId(), node.getContentMap().getDatasource());

		return node;
	}

	/**
	 * Get the base path for the content repository (create the directories if they are missing).
	 * 
	 * @return CR base path
	 */
	public String getBasePath() {
		java.io.File baseDir = new java.io.File(System.getProperty("java.io.tmpdir"));

		java.io.File diskBaseDir = new java.io.File(baseDir, CR_DIRNAME);

		if (!diskBaseDir.exists()) {
			assertTrue("Could not create directories for CR base directory", diskBaseDir.mkdirs());
		}

		return diskBaseDir.getAbsolutePath();
	}

	/**
	 * Assert existence/nonexistence of the published file in the contentrepository of the given node.
	 * 
	 * @param node
	 *            node
	 * @param file
	 *            file
	 * @param expected
	 *            true if the file is expected to exist
	 * @param attributes optional attributes
	 * @throws Exception
	 */
	public void assertFileInContentrepository(Node node, File file, boolean expected, CRAttribute...attributes) throws Exception {
		CNDatasource ds = datasources.get(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", ds);

		Resolvable crFile = PortalConnectorFactory.getContentObject("10008." + file.getId(), ds);

		if (expected) {
			assertNotNull("File " + file + " must exist in cr of " + node, crFile);
			for (CRAttribute attr : attributes) {
				attr.assertValue(crFile);
			}
		} else {
			assertNull("File " + file + " must not exist in cr of " + node, crFile);
		}
	}

	/**
	 * Assert existence/nonexistence of the published page in the contentrepository of the given node.
	 * 
	 * @param node
	 *            node
	 * @param page
	 *            page
	 * @param expected
	 *            true if the file is expected to exist
	 * @param attributes optional attributes
	 * @throws Exception
	 */
	public Resolvable assertPageInContentrepository(Node node, Page page, boolean expected, CRAttribute...attributes) throws Exception {
		CNDatasource ds = datasources.get(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", ds);

		Resolvable crPage = PortalConnectorFactory.getContentObject("10007." + page.getId(), ds);

		if (expected) {
			assertNotNull("Page " + page + " must exist in cr of " + node, crPage);
			for (CRAttribute attr : attributes) {
				attr.assertValue(crPage);
			}
		} else {
			assertNull("Page " + page + " must not exist in cr of " + node, crPage);
		}

		return crPage;
	}

	/**
	 * Assert existence/nonexistence of the published folder in the contentrepository of the given node.
	 * 
	 * @param node
	 *            node
	 * @param folder
	 *            folder
	 * @param expected
	 *            true if the file is expected to exist
	 * @param attributes optional attributes
	 * @throws Exception
	 */
	public void assertFolderInContentrepository(Node node, Folder folder, boolean expected, CRAttribute...attributes) throws Exception {
		CNDatasource ds = datasources.get(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", ds);

		Resolvable crFolder = PortalConnectorFactory.getContentObject("10002." + folder.getId(), ds);

		if (expected) {
			assertNotNull("Folder " + folder + " must exist in cr of " + node, crFolder);
			for (CRAttribute attr : attributes) {
				attr.assertValue(crFolder);
			}
		} else {
			assertNull("Folder " + folder + " must not exist in cr of " + node, crFolder);
		}
	}

	public Node getChannel() {
		return channel;
	}

	public Node getMaster() {
		return master;
	}

	/**
	 * Set instant publishing
	 * 
	 * @param instant
	 *            true for instant publishing, false for not
	 * @throws Exception
	 */
	public void setInstant(boolean instant) throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(false);

		// set the instant publishing setting
		ContentRepository cr = t.getObject(ContentRepository.class, master.getContentrepositoryId(), true);
		cr.setInstantPublishing(instant);
		cr.save();
		cr = t.getObject(ContentRepository.class, channel.getContentrepositoryId(), true);
		cr.setInstantPublishing(instant);
		cr.save();
		t.commit(false);
	}

	/**
	 * Enumeration of possible object type combinations
	 */
	public enum ObjectType {
		PAGE("page"), IMAGE("image"), FILE("file"), FOLDER("folder");

		private String shortName;

		private ObjectType(String shortName) {
			this.shortName = shortName;
		}

		public String getShortName() {
			return shortName;
		}
	}

	/**
	 * ContentRepository attribute
	 */
	public static class CRAttribute {
		/**
		 * Name of the CR attribute
		 */
		protected String name;

		/**
		 * Value of the CR attribute
		 */
		protected Object value;

		/**
		 * Create an instance
		 * @param name name
		 * @param value value
		 */
		protected CRAttribute(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		/**
		 * Get a new instance
		 * @param name name
		 * @param value value
		 * @return new instance
		 */
		public static CRAttribute get(String name, Object value) {
			return new CRAttribute(name, value);
		}

		/**
		 * Assert that the object has the attribute set to the value
		 * @param obj object
		 * @throws Exception
		 */
		public void assertValue(Resolvable obj) throws Exception {
			if (obj == null) {
				fail("Cannot assert a value for null object");
			}
			assertEquals("Check attribute " + name + " for " + obj, value, obj.get(name));
		}
	}
}
