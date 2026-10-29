package com.gentics.contentnode.tests.publish.mccr;

import static org.junit.Assert.assertEquals;
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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.testutils.ContentRepositoryResource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.testutils.GenericTestUtils;
/**
 * Test cases for direct object dirting.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR})
public class DirectObjectDirtingSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@ClassRule
	public static ContentRepositoryResource contentRepository = new ContentRepositoryResource();

	public final static String CR_DIRNAME = "CR";

	/**
	 * Multichannelling datasources for the tests
	 */
	protected static MCCRDatasource datasource;

	/**
	 * The master node for testing
	 */
	protected static Node master;

	/**
	 * The channel for testing
	 */
	protected static Node channel;

	/**
	 * ID of the htmllong construct
	 */
	public final static int HTMLLONG_CONSTRUCT = 1;

	private final WorkingNodeSettings workingNodeSettings;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: workingNodeSettings {0}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		setUpTestData(false, false);
	}

	/**
	 * Create a parameterized instance of the test case
	 * 
	 * @param workingNodeSettings
	 */
	public DirectObjectDirtingSandboxTest(WorkingNodeSettings workingNodeSettings) {
		this.workingNodeSettings = workingNodeSettings;
	}

	/**
	 * Test direct object dirting during object creation.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testObjectCreation() throws Exception {
		int creationTime = 1;
		int publishRun = 2;
		int checkAfterPublish = 3;

		// Pick the creation channel from the current test variation.
		Node node = null;
		boolean createInMaster = false;

		switch (workingNodeSettings) {
		case MASTER:
			node = getMaster();
			createInMaster = true;
			break;

		case CHANNEL:
			node = getChannel();
			break;
		}

		startTransaction(creationTime);

		// Create the objects we'll test with
		Page page = createPage(node, "DirectDirting");
		File file = createFile(node, "DirectDirting");
		ImageFile image = createImage(node, "DirectDirting");
		Folder folder = createFolder(node, "DirectDirting");

		publish(publishRun);

		startTransaction(checkAfterPublish);

		if (createInMaster) {
			// Verify the objects are published in CR for master node
			assertPageInContentrepository(getMaster(), page, true);
			assertFileInContentrepository(getMaster(), file, true);
			assertFileInContentrepository(getMaster(), image, true);
			assertFolderInContentrepository(getMaster(), folder, true);

			// Verify the objects are published in CR for channel node
			assertPageInContentrepository(getChannel(), page, true);
			assertFileInContentrepository(getChannel(), file, true);
			assertFileInContentrepository(getChannel(), image, true);
			assertFolderInContentrepository(getChannel(), folder, true);
		} else {
			// Verify the objects are NOT published in CR for master node
			assertPageInContentrepository(getMaster(), page, false);
			assertFileInContentrepository(getMaster(), file, false);
			assertFileInContentrepository(getMaster(), image, false);
			assertFolderInContentrepository(getMaster(), folder, false);

			// Verify the objects are published in CR for channel node
			assertPageInContentrepository(getChannel(), page, true);
			assertFileInContentrepository(getChannel(), file, true);
			assertFileInContentrepository(getChannel(), image, true);
			assertFolderInContentrepository(getChannel(), folder, true);
		}
	}

	/**
	 * Test direct object dirting during object removal.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testObjectRemoval() throws Exception {
		int creationTime = 1;
		int firstPublishRun = 2;
		int removeTime = 3;
		int secondPublishRun = 4;
		int checkAfterPublish = 5;

		// Pick the creation channel from the current test variation.
		Node node = null;

		switch (workingNodeSettings) {
		case MASTER:
			node = getMaster();
			break;

		case CHANNEL:
			node = getChannel();
			break;
		}

		// Create and publish the object we'll test with
		startTransaction(creationTime);
		Page page = createPage(node, "DirectDirting");
		Page pageForOffline = createPage(node, "DirectDirting-forOffline");
		File file = createFile(node, "DirectDirting");
		ImageFile image = createImage(node, "DirectDirting");
		Folder folder = createFolder(node, "DirectDirting");

		publish(firstPublishRun);

		// Remove/takeOffline the objects
		startTransaction(removeTime);
		page.delete();
		file.delete();
		image.delete();
		folder.delete();
		pageForOffline.takeOffline();
		TransactionManager.getCurrentTransaction().commit(false);

		publish(secondPublishRun);

		startTransaction(checkAfterPublish);
		// Verify the objects are NOT published in CR for master node
		assertPageInContentrepository(getMaster(), page, false);
		assertFileInContentrepository(getMaster(), file, false);
		assertFileInContentrepository(getMaster(), image, false);
		assertFolderInContentrepository(getMaster(), folder, false);
		assertPageInContentrepository(getMaster(), pageForOffline, false);

		// Verify the objects are NOT published in CR for channel node
		assertPageInContentrepository(getChannel(), page, false);
		assertFileInContentrepository(getChannel(), file, false);
		assertFileInContentrepository(getChannel(), image, false);
		assertFolderInContentrepository(getChannel(), folder, false);
		assertPageInContentrepository(getChannel(), pageForOffline, false);
	}

	/**
	 * Test direct object dirting during object content/binarycontent update.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testContentUpdate() throws Exception {
		int creationTime = 1;
		int firstPublishRun = 2;
		int updateTime = 3;
		int secondPublishRun = 4;
		int checkAfterPublish = 5;

		// Pick the creation channel from the current test variation.
		Node node = null;
		boolean createInMaster = false;

		switch (workingNodeSettings) {
		case MASTER:
			node = getMaster();
			createInMaster = true;
			break;

		case CHANNEL:
			node = getChannel();
			break;
		}

		// Create and publish the object we'll test with
		startTransaction(creationTime);
		Page page = createPage(node, "DirectDirting");
		File file = createFile(node, "DirectDirting");
		ImageFile image = createImage(node, "DirectDirting");

		publish(firstPublishRun);

		// Update content/binarycontent of the objects
		Transaction t = startTransaction(updateTime);
		page = t.getObject(Page.class, page.getId(), true);
		page.getContentTag("content").getValues().iterator().next().setValueText("This is the modified page content");
		page.save();
		page.publish();
		t.commit(false);

		file = t.getObject(File.class, file.getId(), true);
		file.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
		file.save();
		t.commit(false);

		image = t.getObject(ImageFile.class, image.getId(), true);
		image.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }));
		image.save();
		t.commit(false);

		publish(secondPublishRun);

		startTransaction(checkAfterPublish);
		// Verify the objects are available in CR
		if (createInMaster) {
			// For master
			assertPageInContentrepository(getMaster(), page, true);
			assertFileInContentrepository(getMaster(), file, true);
			assertFileInContentrepository(getMaster(), image, true);

			// For channel
			assertPageInContentrepository(getChannel(), page, true);
			assertFileInContentrepository(getChannel(), file, true);
			assertFileInContentrepository(getChannel(), image, true);
		} else {
			// For master
			assertPageInContentrepository(getMaster(), page, false);
			assertFileInContentrepository(getMaster(), file, false);
			assertFileInContentrepository(getMaster(), image, false);

			// For channel
			assertPageInContentrepository(getChannel(), page, true);
			assertFileInContentrepository(getChannel(), file, true);
			assertFileInContentrepository(getChannel(), image, true);
		}

		// Verify the objects' content is updated in CR
		if (createInMaster) {
			assertPageContentInCR(getMaster(), page, "Test template for dependency page: This is the modified page content");
			assertFileBinaryContentInCR(getMaster(), file, new byte[] { 1, 2, 3 });
			assertFileBinaryContentInCR(getMaster(), image, new byte[] { 1, 2, 3, 4 });
		}

		assertPageContentInCR(getChannel(), page, "Test template for dependency page: This is the modified page content");
		assertFileBinaryContentInCR(getChannel(), file, new byte[] { 1, 2, 3 });
		assertFileBinaryContentInCR(getChannel(), image, new byte[] { 1, 2, 3, 4 });
	}

	/**
	 * Create folder dependency and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private Folder createFolder(Node node, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder folder = t.createObject(Folder.class);

		folder.setName("TestFolder-master-" + uniqueNamePostfix);
		folder.setChannelInfo(node.getId(), folder.getChannelSetId());
		folder.setMotherId(node.getFolder().getId());
		folder.save();
		t.commit(false);

		return folder;
	}

	/**
	 * Create image dependency and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private ImageFile createImage(Node node, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile image = t.createObject(ImageFile.class);

		image.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		image.setName("TestImageFile-master-" + uniqueNamePostfix + ".jpg");
		if (node.isChannel()) {
			image.setChannelInfo(node.getId(), image.getChannelSetId());
		}
		image.setFolderId(node.getFolder().getId());
		image.save();
		t.commit(false);

		return image;
	}

	/**
	 * Create fil dependencye and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private File createFile(Node node, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		File file = t.createObject(File.class);

		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("TestFile-master-" + uniqueNamePostfix + ".txt");
		if (node.isChannel()) {
			file.setChannelInfo(node.getId(), file.getChannelSetId());
		}
		file.setFolderId(node.getFolder().getId());
		file.save();
		t.commit(false);

		return file;
	}

	/**
	 * Create page dependency and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param templateId
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private Page createPage(Node node, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create the template with two template tags - htmllong and overview
		Template template = t.createObject(Template.class);

		template.setFolderId(getMaster().getFolder().getId());
		template.setName("TestTemplate-master-" + uniqueNamePostfix);
		template.setSource("Test template for dependency page: <node content>");

		Map<String, TemplateTag> templateTags = template.getTags();
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setConstructId(HTMLLONG_CONSTRUCT);
		tag.setEnabled(true);
		tag.setName("content");
		tag.setPublic(true);
		templateTags.put(tag.getName(), tag);

		template.save();
		t.commit(false);

		Page page = t.createObject(Page.class);

		page.setName("TestPage-master-" + uniqueNamePostfix);
		page.setTemplateId(template.getId(), true);
		if (node.isChannel()) {
			page.setChannelInfo(node.getId(), page.getChannelSetId());
		}
		page.setFolderId(node.getFolder().getId());
		ContentTag contentTag = page.getContentTag("content");
		PartType partType = contentTag.getValues().iterator().next().getPartType();

		partType.getValueObject().setValueText("This is the basic page content");
		page.save();
		page.publish();
		t.commit(false);

		return page;
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (WorkingNodeSettings workingNodeSettings : WorkingNodeSettings.values()) {
			testData.add(new Object[] { workingNodeSettings });
		}

		return testData;
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
	public static void setUpTestData(boolean runPublishProcess, boolean instantPublishing) throws Exception {

		// remove all logcmd entries
		testContext.getDBSQLUtils().executeQueryManipulation("DELETE FROM logcmd");

		// create master node
		master = createNode("masterNode", "masterNode", instantPublishing, false);

		// create channel node
		channel = createChannel(master, "channelNode", "channelNode", instantPublishing, false);
	}

	/**
	 * Start a new transaction with the given timestamp. Any currently open transaction will be committed and closed first.
	 * The new transaction will be set as the current transaction.
	 * @param timestamp transaction timestamp (in s)
	 * @return transaction
	 * @throws NodeException
	 */
	protected Transaction startTransaction(int timestamp) throws NodeException {
		testContext.getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();
		t.setTimestamp(timestamp * 1000);
		return t;
	}

	public void runPublishProcess() throws Exception {
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
	}

	/**
	 * Run a publish process with the given timestamp (in s)
	 * @param timestamp timestamp in s
	 * @throws Exception
	 */
	protected void publish(int timestamp) throws Exception {
		// run a publish process
		PublishInfo info = testContext.getContext().publish(false, true, timestamp * 1000L);
		assertTrue("Publish process failed", info.getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);
	}

	/**
	 * Create a datasource for the given node
	 * 
	 * @return handle properties
	 * @throws Exception
	 */
	private static Map<String, String> createDatasource() throws Exception {
		Map<String, String> handleProperties = contentRepository.getCRTestUtils().getTestDatabase().getSettingsMap();

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
	public static Node createNode(String hostName, String name, boolean instantPublishing, boolean publishDisabled) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		node.setPublishDisabled(publishDisabled);
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
	public static Node createChannel(Node master, String hostName, String name, boolean instantPublishing, boolean publishDisabled) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		node.setPublishContentmap(true);
		node.setPublishDisabled(publishDisabled);
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
	 * Assert that the content of a page in CR is as expected.
	 * 
	 * @param node
	 * @param page
	 * @param expectedContent
	 * @throws Exception
	 */
	public void assertPageContentInCR(Node node, Page page, String expectedContent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", datasource);

		Resolvable crPage = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		assertEquals("Page content is not as expected.", expectedContent, crPage.get("content"));
		;
	}

	/**
	 * Assert that the binarycontent of a file/image in CR is as expected.
	 * 
	 * @param node
	 * @param file
	 * @param expectedContent
	 * @throws Exception
	 */
	public void assertFileBinaryContentInCR(Node node, File file, byte[] expectedContent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", datasource);

		Resolvable crPage = PortalConnectorFactory.getContentObject("10008." + file.getId(), datasource);

		assertTrue("File binarycontent is not as expected.",
				Arrays.equals(expectedContent, ObjectTransformer.getBinary(crPage.get("binarycontent"), null)));
	}

	/**
	 * Get the base path for the content repository (create the directories if they are missing).
	 * 
	 * @return CR base path
	 */
	public static String getBasePath() {
		java.io.File baseDir = new java.io.File(System.getProperty("java.io.tmpdir"));

		java.io.File diskBaseDir = new java.io.File(baseDir, CR_DIRNAME);

		if (!diskBaseDir.exists()) {
			assertTrue("Could not create directories for CR base directory", diskBaseDir.mkdirs());
		}

		return diskBaseDir.getAbsolutePath();
	}

	public static Node getChannel() {
		return channel;
	}

	public static Node getMaster() {
		return master;
	}

	/**
	 * Settings of which node we're currently working with
	 */
	public enum WorkingNodeSettings {
		MASTER, CHANNEL;
	}

}
