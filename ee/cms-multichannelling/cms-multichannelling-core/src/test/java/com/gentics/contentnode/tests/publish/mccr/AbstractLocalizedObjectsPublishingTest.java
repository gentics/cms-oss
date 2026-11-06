package com.gentics.contentnode.tests.publish.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.After;
import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.TagmapEntry;
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
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
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
	 * Multichannelling datasources for the tests
	 */
	protected MCCRDatasource datasource;

	/**
	 * The master node for testing
	 */
	protected Node master;

	protected TestDatabase testDatabase;

	protected SQLUtils testDbUtils;

	/**
	 * The sub-channel for testing
	 */
	protected Node channel;

	/**
	 * Test user
	 */
	protected SystemUser user;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (ObjectType objectType : ObjectType.values()) {
			testData.add(new Object[] { objectType });
		}
		return testData;
	}

	@After
	public void tearDown() throws Exception {
		if (testDatabase != null) {
			SQLUtils testDbUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
			testDbUtils.connectWithoutSelectingDatabase();
			testDbUtils.removeDatabase();
			testDbUtils.disconnectDatabase();
		}
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

		testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		testDbUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		testDbUtils.connectDatabase();
		testDatabase.setRandomDatabasename(getClass().getSimpleName());
		testDbUtils.createDatabase();
		testDbUtils.selectDatabase(testDatabase.getDBName());

		user = Trx.supply(t -> t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS));

		testContext.startTransactionWithPermissions(false);

		master = createNode("masterNode", "masterNode", instantPublishing);
		channel = createChannel(master, "channelNode", "channelNode", instantPublishing);

		// run the publish process to create nodes and channel structure before inserting the test data
		runPublishProcess();

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

	/**
	 * Create a datasource for the given node
	 * 
	 * @return handle properties
	 * @throws Exception
	 */
	private Map<String, String> createDatasource() throws Exception {
		Map<String, String> handleProperties = testDatabase.getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("multichannelling", "true");
		dsProperties.put("attribute.path", getBasePath());

		// Get normal or writable datasource
		datasource = (MCCRDatasource) PortalConnectorFactory.createMultichannellingDatasource(handleProperties, dsProperties);
		assertNotNull("Check whether datasource was created", datasource);

		return dsProperties;
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
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		node.save();
		t.commit(false);

		TagmapEntry contentEntry = t.createObject(TagmapEntry.class);

		contentEntry.setMapname("content");
		contentEntry.setObject(10007);
		contentEntry.setFilesystem(true);
		contentEntry.setAttributeTypeId(5);

		TagmapEntry binaryContentEntry = t.createObject(TagmapEntry.class);

		binaryContentEntry.setMapname("binarycontent");
		binaryContentEntry.setObject(10008);
		binaryContentEntry.setFilesystem(true);
		binaryContentEntry.setAttributeTypeId(6);

		TagmapEntry pageNameEntry = t.createObject(TagmapEntry.class);

		pageNameEntry.setMapname("name");
		pageNameEntry.setTagname("page.name");
		pageNameEntry.setObject(10007);
		pageNameEntry.setOptimized(true);
		pageNameEntry.setAttributeTypeId(1);

		TagmapEntry fileNameEntry = t.createObject(TagmapEntry.class);

		fileNameEntry.setMapname("name");
		fileNameEntry.setTagname("file.name");
		fileNameEntry.setObject(10008);
		fileNameEntry.setOptimized(true);
		fileNameEntry.setAttributeTypeId(1);

		TagmapEntry folderNameEntry = t.createObject(TagmapEntry.class);

		folderNameEntry.setMapname("name");
		folderNameEntry.setTagname("folder.name");
		folderNameEntry.setObject(10002);
		folderNameEntry.setOptimized(true);
		folderNameEntry.setAttributeTypeId(1);

		// create a datasource
		Map<String, String> handleProperties = createDatasource();
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);
		cr.setInstantPublishing(instantPublishing);
		cr.setMultichannelling(true);
		cr.setBasepath(getBasePath());

		List<TagmapEntry> entries = cr.getEntries();

		entries.add(contentEntry);
		entries.add(binaryContentEntry);
		entries.add(pageNameEntry);
		entries.add(fileNameEntry);
		entries.add(folderNameEntry);

		cr.save();

		// activate publishing into the contentrepository
		node.setPublishContentmap(true);
		node.setContentrepositoryId(cr.getId());
		node.save();
		t.commit(false);

		// update the cr to point to the hsql database
		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?", new Object[] { "mysql",
				handleProperties.get("url"), handleProperties.get("username"), handleProperties.get("passwd"), cr.getId() });

		return t.getObject(Node.class, node.getId());
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
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		node.setPublishContentmap(true);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		Folder masterRootFolder = master.getFolder();

		rootFolder.setChannelMaster(masterRootFolder);

		node.save();
		t.commit(false);

		return t.getObject(Node.class, node.getId());
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
	 * @throws Exception
	 */
	public void assertFileInContentrepository(Node node, File file, boolean expected) throws Exception {
		assertNotNull(node + " has no datasource", datasource);
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));

		Resolvable crFile = PortalConnectorFactory.getContentObject("10008." + file.getId(), datasource);

		if (expected) {
			assertNotNull("File " + file + " must exist in cr of " + node, crFile);
		} else {
			assertNull("File " + file + " must not exist in cr of " + node, crFile);
		}
	}

	/**
	 * Assert existence/nonexistence of the published page in the contentrepository of the given node.
	 * 
	 * @param node
	 *            node
	 * @param file
	 *            file
	 * @param expected
	 *            true if the file is expected to exist
	 * @throws Exception
	 */
	public Resolvable assertPageInContentrepository(Node node, Page page, boolean expected) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", datasource);

		Resolvable crPage = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		if (expected) {
			assertNotNull("Page " + page + " must exist in cr of " + node, crPage);
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
	 * @param file
	 *            file
	 * @param expected
	 *            true if the file is expected to exist
	 * @throws Exception
	 */
	public void assertFolderInContentrepository(Node node, Folder folder, boolean expected) throws Exception {
		assertNotNull(node + " has no datasource", datasource);
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));

		Resolvable crFolder = PortalConnectorFactory.getContentObject("10002." + folder.getId(), datasource);

		if (expected) {
			assertNotNull("Folder " + folder + " must exist in cr of " + node, crFolder);
		} else {
			assertNull("Folder " + folder + " must not exist in cr of " + node, crFolder);
		}
	}

	/**
	 * Assert equality of page content ID and attributes based on the specified parameters.
	 * 
	 * @param page
	 *            page
	 * @param shouldContentIdsBeDifferent
	 *            true if the content ID of the master and channel object should be different, false - otherwise.
	 * @param shouldAttributesBeDifferent
	 *            true if the attributes of the master and channel object should be different, false - otherwise.
	 * @throws Exception
	 */
	public void assertPageEquality(Page page, boolean shouldContentIdsBeDifferent, boolean shouldAttributesBeDifferent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(master.getId(), 0));
		Resolvable resolvable = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		datasource.setChannel(ObjectTransformer.getInt(channel.getId(), 0));
		Resolvable localizedResolvable = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		assertStringAttrEquality("contentid", resolvable, localizedResolvable, shouldContentIdsBeDifferent, 0, "");
		assertStringAttrEquality("name", resolvable, localizedResolvable, shouldAttributesBeDifferent, 0, "");
		assertStringAttrEquality("content", resolvable, localizedResolvable, shouldAttributesBeDifferent, ObjectTransformer.getInt(page.getChannelSetId(), 0),
				"10007");
	}

	/**
	 * Assert equality of file content ID and attributes based on the specified parameters.
	 * 
	 * @param page
	 *            page
	 * @param shouldContentIdsBeDifferent
	 *            true if the content ID of the master and channel object should be different, false - otherwise.
	 * @param shouldAttributesBeDifferent
	 *            true if the attributes of the master and channel object should be different, false - otherwise.
	 * @throws Exception
	 */
	public void assertFileEquality(File file, boolean shouldContentIdsBeDifferent, boolean shouldAttributesBeDifferent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(master.getId(), 0));
		Resolvable resolvable = PortalConnectorFactory.getContentObject("10008." + file.getId(), datasource);

		datasource.setChannel(ObjectTransformer.getInt(channel.getId(), 0));
		Resolvable localizedResolvable = PortalConnectorFactory.getContentObject("10008." + file.getId(), datasource);

		assertStringAttrEquality("contentid", resolvable, localizedResolvable, shouldContentIdsBeDifferent, 0, "");
		assertStringAttrEquality("name", resolvable, localizedResolvable, shouldAttributesBeDifferent, 0, "");
		assertBinaryAttrEquality("binarycontent", resolvable, localizedResolvable, shouldAttributesBeDifferent, ObjectTransformer.getInt(file.getChannelSetId(), 0),
				"10008");
	}

	/**
	 * Assert equality of folder content ID and attributes based on the specified parameters.
	 * 
	 * @param page
	 *            page
	 * @param shouldContentIdsBeDifferent
	 *            true if the content ID of the master and channel object should be different, false - otherwise.
	 * @param shouldAttributesBeDifferent
	 *            true if the attributes of the master and channel object should be different, false - otherwise.
	 * @throws Exception
	 */
	public void assertFolderEquality(Folder folder, boolean shouldContentIdsBeDifferent, boolean shouldAttributesBeDifferent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(master.getId(), 0));
		Resolvable resolvable = PortalConnectorFactory.getContentObject("10002." + folder.getId(), datasource);

		datasource.setChannel(ObjectTransformer.getInt(channel.getId(), 0));
		Resolvable localizedResolvable = PortalConnectorFactory.getContentObject("10002." + folder.getId(), datasource);

		assertStringAttrEquality("contentid", resolvable, localizedResolvable, shouldContentIdsBeDifferent, 0, "");
		assertStringAttrEquality("name", resolvable, localizedResolvable, shouldAttributesBeDifferent, 0, "");
	}

	/**
	 * Assert that a specified String attribute in two {@link Resolvable} instances is the same or different based on the specified parameters.
	 * 
	 * @param attrName
	 *            The name of the attribute.
	 * @param first
	 *            The first {@link Resolvable} instance.
	 * @param second
	 *            The second {@link Resolvable} instance.
	 * @param shouldBeDifferent
	 *            true if the attribute should be different, false - otherwise.
	 */
	private void assertStringAttrEquality(String attrName, Resolvable first, Resolvable second, boolean shouldBeDifferent, int channelSetId,
			String objectType) throws Exception {
		String resolvableAttr = ObjectTransformer.getString(first.get(attrName), null);
		String localizedResolvableAttr = ObjectTransformer.getString(second.get(attrName), null);

		assertNotNull("Attribute " + attrName + " of master object should not be null.", resolvableAttr);
		assertNotNull("Attribute " + attrName + " of channel object should not be null.", localizedResolvableAttr);

		if (shouldBeDifferent) {
			assertFalse(attrName + " attribute should be different", resolvableAttr.equals(localizedResolvableAttr));
		} else {
			assertTrue(attrName + " attribute should be different", resolvableAttr.equals(localizedResolvableAttr));
		}
	}

	/**
	 * Assert that a specified binary attribute in two {@link Resolvable} instances is the same or different based on the specified parameters.
	 * 
	 * @param attrName
	 *            The name of the attribute.
	 * @param first
	 *            The first {@link Resolvable} instance.
	 * @param second
	 *            The second {@link Resolvable} instance.
	 * @param shouldBeDifferent
	 *            true if the attribute should be different, false - otherwise.
	 */
	private void assertBinaryAttrEquality(String attrName, Resolvable first, Resolvable second, boolean shouldBeDifferent, int channelSetId,
			String objectType) throws Exception {
		byte[] resolvableBinContent = ObjectTransformer.getBinary(first.get(attrName), null);
		byte[] localizedResolvableBinContent = ObjectTransformer.getBinary(second.get(attrName), null);

		assertNotNull("Attribute " + attrName + " of master object should not be null.", resolvableBinContent);
		assertNotNull("Attribute " + attrName + " of channel object should not be null.", resolvableBinContent);

		if (shouldBeDifferent) {
			assertFalse("Binary content attribute should be different", Arrays.equals(resolvableBinContent, localizedResolvableBinContent));
		} else {
			assertTrue("Binary content attribute should be different", Arrays.equals(resolvableBinContent, localizedResolvableBinContent));
		}
	}

	public Node getChannel() {
		return channel;
	}

	public Node getMaster() {
		return master;
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

}
