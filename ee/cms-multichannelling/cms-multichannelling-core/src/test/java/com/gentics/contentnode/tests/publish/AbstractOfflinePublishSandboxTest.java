package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.ClassRule;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
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
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.testutils.GenericTestUtils;

/**
 * Sandbox helper to encapsulate setup and teardown of a test sandbox
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class AbstractOfflinePublishSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Array containing false and true
	 */
	public final static boolean[] FALSE_TRUE = { false, true };

	/**
	 * ID of the fileurl construct
	 */
	public final static int FILEURL_CONSTRUCT_ID = 11;

	/**
	 * Map of channels
	 */
	protected static Map<Boolean, Map<Boolean, Node>> channels = new HashMap<Boolean, Map<Boolean, Node>>();

	/**
	 * Foreign master
	 */
	protected static Node foreignMaster;

	/**
	 * Foreign channel
	 */
	protected static Node foreignChannel;

	/**
	 * Map of datasources per node/channel
	 */
	protected static Map<Integer, Datasource> datasources = new HashMap<Integer, Datasource>();

	/**
	 * Maps keys to file ids
	 */
	protected static Map<String, Integer> fileIds = new HashMap<String, Integer>();

	/**
	 * List of all page ids
	 */
	protected static List<Integer> pageIds = new Vector<Integer>();

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	public static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (boolean image : FALSE_TRUE) {
			for (Inheritance inheritance : Inheritance.values()) {
				for (MCSetting feature : MCSetting.values()) {
					for (MCSetting force : MCSetting.values()) {
						for (MCSetting dependency : MCSetting.values()) {
							if (inheritance.allows(feature, force, dependency)) {
								testData.add(new Object[] { image, inheritance, feature, force, dependency });
							}
						}
					}
				}
			}
		}
		return testData;
	}

	/**
	 * Set some custom settings and invoke the sandbox setup.
	 * 
	 * @throws Exception
	 */
	public static void prepareSandbox() throws Exception {
		// remove all dependencies
		testContext.getDBSQLUtils().executeQueryManipulation("DELETE FROM dependencymap2");

	}

	/**
	 * Get the key for the given test parameters
	 * @param image true for images
	 * @param inheritance inheritance setting
	 * @param feature feature setting
	 * @param force force online setting
	 * @param dependency dependency setting
	 * @param foreign whether the dependency is in a foreign channel structure
	 * @return key
	 */
	public static String getKey(boolean image, Inheritance inheritance, MCSetting feature, MCSetting force, MCSetting dependency, boolean foreign) {
		StringBuffer key = new StringBuffer();

		key
			.append(image ? "t" : "f")
			.append(inheritance.getShortName())
			.append(feature.getShortName())
			.append(force.getShortName())
			.append(dependency.getShortName())
			.append(foreign ? "f" : "s");

		return key.toString();
	}

	/**
	 * Setup the test data
	 *
	 * Convenience method which calls {@link #setUpTestData(boolean, boolean, boolean)}
	 * with <code>foreign = false</code>.
	 * 
	 * @param runPublishProcess true to run the publish process
	 * @param instantPublishing true to use instant publishing
	 * @throws Exception
	 */
	public static void setUpTestData(boolean runPublishProcess, boolean instantPublishing) throws Exception {
		setUpTestData(runPublishProcess, instantPublishing, false);
	}

	/**
	 * Setup the test data
	 *
	 * @param runPublishProcess true to run the publish process
	 * @param instantPublishing true to use instant publishing
	 * @param foreign true to create the dependency in a different channel structure
	 * @throws Exception
	 */
	public static void setUpTestData(boolean runPublishProcess, boolean instantPublishing, boolean foreign) throws Exception {
		// generally activate the feature
		TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().setFeature(Feature.CONTENTFILE_AUTO_OFFLINE.toString().toLowerCase(),
				true);

		// first disable publishing for all nodes
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE node SET disable_publish = 1, publish_fs = 0, publish_contentmap = 0");

		if (foreign) {
			foreignMaster = createNode("ForeignMaster", "ForeignMaster", true, false);
			foreignChannel = createChannel(foreignMaster, "ForeignChannel", "ForeignChannel", true, false);
		}

		// create node with feature
		Node nodeOn = createNode("NodeOn", "NodeOn", true, instantPublishing);

		// create channels
		Node channelOnOn = createChannel(nodeOn, "ChannelOnOn", "ChannelOnOn", true, instantPublishing);
		Node channelOnOff = createChannel(nodeOn, "ChannelOnOff", "ChannelOnOff", false, instantPublishing);

		Map<Boolean, Node> nodeMap = new HashMap<Boolean, Node>();

		nodeMap.put(true, channelOnOn);
		nodeMap.put(false, channelOnOff);
		channels.put(true, nodeMap);

		// create node without feature
		Node nodeOff = createNode("NodeWithoutFeature", "NodeWithoutFeature", false, instantPublishing);

		// create channels
		Node channelOffOn = createChannel(nodeOff, "ChannelOffOn", "ChannelOffOn", true, instantPublishing);
		Node channelOffOff = createChannel(nodeOff, "ChannelOffOff", "ChannelOffOff", false, instantPublishing);

		nodeMap = new HashMap<Boolean, Node>();
		nodeMap.put(true, channelOffOn);
		nodeMap.put(false, channelOffOff);
		channels.put(false, nodeMap);

		Template template = createTemplate(nodeOn.getFolder());

		// generate test data
		Collection<Object[]> testParameters = getTestParameters();

		for (Object[] params : testParameters) {
			Boolean image = (Boolean) params[0];
			Inheritance inheritance = (Inheritance) params[1];
			MCSetting feature = (MCSetting) params[2];
			MCSetting force = (MCSetting) params[3];
			MCSetting dependency = (MCSetting) params[4];
			String key = getKey(image, inheritance, feature, force, dependency, foreign);

			fileIds.put(key, ObjectTransformer.getInteger(createFile(template, image, inheritance, feature, force, dependency, foreign).getId(), null));
		}

		if (runPublishProcess) {
			// run the publish process
			testContext.getContext().startTransaction();
			assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
		}
	}

	/**
	 * Publish all pages and run the publish process
	 * @throws Exception
	 */
	public static void addAllDependencies() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (Integer pageId : pageIds) {
			Page page = t.getObject(Page.class, pageId, true);

			page.publish();
		}
		t.commit(false);
		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
	}

	/**
	 * Take all pages offline and run the publish process
	 * @throws Exception
	 */
	public static void removeAllDependencies() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (Integer pageId : pageIds) {
			Page page = t.getObject(Page.class, pageId, true);
			page.takeOffline();
		}
		t.commit(false);
		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
	}

	/**
	 * Get the test file for the given setting
	 * @param image true for image
	 * @param inheritance inheritance setting
	 * @param feature feature setting
	 * @param force force setting
	 * @param dependency dependency setting
	 * @return test file
	 * @throws Exception
	 */
	public File getTestFile(boolean image, Inheritance inheritance, MCSetting feature, MCSetting force, MCSetting dependency) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer fileId = fileIds.get(getKey(image, inheritance, feature, force, dependency, false));

		return t.getObject(File.class, fileId);
	}

	/**
	 * Get the test file for the given setting
	 * @param image true for image
	 * @param inheritance inheritance setting
	 * @param feature feature setting
	 * @param force force setting
	 * @param dependency dependency setting
	 * @param foreign whether the potential dependency is in a foreign channel structure
	 * @return test file
	 * @throws Exception
	 */
	public File getTestFile(boolean image, Inheritance inheritance, MCSetting feature, MCSetting force, MCSetting dependency, boolean foreign) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer fileId = fileIds.get(getKey(image, inheritance, feature, force, dependency, foreign));

		return t.getObject(File.class, fileId);
	}

	/**
	 * Create the template
	 * 
	 * @param folder
	 *            folder where to create the template
	 * @return template
	 * @throws Exception
	 */
	public static Template createTemplate(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a template
		Template template = t.createObject(Template.class);

		template.setName("Template using file");
		template.setMlId(1);
		template.setSource("<node file>");
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setConstructId(FILEURL_CONSTRUCT_ID);
		tag.setEnabled(true);
		tag.setPublic(true);
		tag.setName("file");
		template.getTemplateTags().put("file", tag);

		template.setFolderId(folder.getId());
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a new File
	 * 
	 * @param template
	 * @param image
	 * @param inheritance
	 * @param feature
	 * @param force
	 * @param dependency
	 * @return
	 * @throws Exception
	 */
	public static File createFile(Template template, boolean image, Inheritance inheritance, MCSetting feature, MCSetting force, MCSetting dependency, boolean foreign) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		String nameAppendix = getKey(image, inheritance, feature, force, dependency, foreign);

		// get the master node, channel
		Node channel = getChannel(feature);
		Node masterNode = channel.getMasterNodes().get(0);
		Folder masterRootFolder = masterNode.getFolder();

		File file = null;

		if (image) {
			file = t.createObject(ImageFile.class);
			file.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			file.setName("blume_" + nameAppendix.toString() + ".jpg");
		} else {
			file = t.createObject(File.class);
			file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
			file.setName("textfile_" + nameAppendix.toString() + ".txt");
		}
		file.setFolderId(masterRootFolder.getId());

		if (inheritance == Inheritance.LOCAL) {
			// create a local file
			file.setChannelInfo(channel.getId(), null);
		}

		switch (force) {
		case MASTER:
		case BOTH:
			file.setForceOnline(true);
			break;

		case CHANNEL:
			if (inheritance == Inheritance.LOCAL) {
				file.setForceOnline(true);
			}
			break;
		}

		file.save();
		t.commit(false);
		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		if (inheritance == Inheritance.LOCALIZED) {
			// localize the file
			File localizedFile = (File) file.copy();

			localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
			switch (force) {
			case CHANNEL:
			case BOTH:
				localizedFile.setForceOnline(true);
				break;

			default:
				localizedFile.setForceOnline(false);
			}
			localizedFile.save();
			t.commit(false);
			// it is necessary to reset the published objects attribute in the transaction,
			// otherwise the file would not be handled again during the test
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);
		}

		if (foreign) {
			channel = foreignChannel;
			masterNode = channel.getMasterNodes().get(0);
			masterRootFolder = masterNode.getFolder();
		}

		// create a page in the master, using this file
		Page masterPage = t.createObject(Page.class);

		masterPage.setFolderId(masterRootFolder.getId());
		masterPage.setTemplateId(template.getId(), true);
		masterPage.setName("Page " + nameAppendix);
		ContentTag tag = masterPage.getContentTag("file");

		Value value = tag.getValues().iterator().next();
		PartType partType = value.getPartType();

		if (partType instanceof FileURLPartType) {
			((FileURLPartType) partType).setTargetFile(file);
		} else {
			fail("parttype should be a FileURLPartType");
		}

		masterPage.save();
		t.commit(false);
		pageIds.add(masterPage.getId());

		// localize the page
		Page localizedPage = (Page) masterPage.copy();

		localizedPage.setChannelInfo(channel.getId(), masterPage.getChannelSetId());
		localizedPage.save();
		t.commit(false);
		pageIds.add(localizedPage.getId());

		// publish or take offline pages
		switch (dependency) {
		case MASTER:
			masterPage.publish();
			break;

		case CHANNEL:
			localizedPage.publish();
			break;

		case BOTH:
			masterPage.publish();
			localizedPage.publish();
			break;

		case NONE:
			break;
		}
		t.commit(false);
		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		return file;
	}

	/**
	 * Get the channel for the given feature setting
	 * 
	 * @param feature
	 *            feature setting
	 * @return channel
	 */
	public static Node getChannel(MCSetting feature) {
		switch (feature) {
		case MASTER:
			return channels.get(true).get(false);

		case CHANNEL:
			return channels.get(false).get(true);

		case BOTH:
			return channels.get(true).get(true);

		case NONE:
			return channels.get(false).get(false);

		default:
			fail("Cannot get channel setting for " + feature);
			return null;
		}
	}

	/**
	 * Create a datasource for the given node
	 * @param node node
	 * @return handle properties
	 * @throws Exception
	 */
	public static Map<String, String> createDatasource(Node node) throws Exception {
		Map<String, String> handleProps = new HashMap<String, String>();

		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:" + node.getHostname());
		handleProps.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProps = new HashMap<String, String>();

		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", "true");

		Datasource datasource = PortalConnectorFactory.createDatasource(handleProps, dsProps);

		assertNotNull("Check whether datasource was created", datasource);

		datasources.put(ObjectTransformer.getInteger(node.getId(), null), datasource);

		return handleProps;
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param feature
	 *            true if feature is on
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(String hostName, String name, boolean feature, boolean instantPublishing) throws Exception {
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

		// create a datasource
		Map<String, String> handleProperties = createDatasource(node);
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);
		cr.setInstantPublishing(instantPublishing);
		cr.save();

		// activate publishing into the contentrepository
		node.setPublishContentmap(true);
		node.setContentrepositoryId(cr.getId());
		node.save();
		t.commit(false);

		// update the cr to point to the hsql database
		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?",
				new Object[] { "hsql", handleProperties.get("url"), "sa", "", cr.getId()});

		if (!instantPublishing) {
			ContentNodeTestDataUtils.addTagmapEntryAllTypes(cr, GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id",
					null, false, false, true, -1, null, null);
		}

		if (feature) {
			node.activateFeature(Feature.CONTENTFILE_AUTO_OFFLINE);
		}

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
	 * @param feature
	 *            true if feature is on
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @return channel
	 * @throws Exception
	 */
	public static Node createChannel(Node master, String hostName, String name, boolean feature, boolean instantPublishing) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		Folder masterRootFolder = master.getFolder();

		rootFolder.setChannelMaster(masterRootFolder);

		node.save();
		t.commit(false);

		// create a datasource
		Map<String, String> handleProperties = createDatasource(node);
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);
		cr.setInstantPublishing(instantPublishing);
		cr.save();

		// activate publishing into the contentrepository
		node.setPublishContentmap(true);
		node.setContentrepositoryId(cr.getId());
		node.save();
		t.commit(false);

		// update the cr to point to the hsql database
		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?",
				new Object[] { "hsql", handleProperties.get("url"), "sa", "", cr.getId()});

		if (!instantPublishing) {
			ContentNodeTestDataUtils.addTagmapEntryAllTypes(cr, GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id",
					null, false, false, true, -1, null, null);
		}

		if (feature) {
			node.activateFeature(Feature.CONTENTFILE_AUTO_OFFLINE);
		}

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Assert existence/nonexistence of the published file for the given node
	 * @param node node
	 * @param file file
	 * @param expected true if the file is expected to exist, false if it is expected to not exist
	 * @throws Exception
	 */
	public void assertFileInFilesystem(Node node, File file, boolean expected) throws Exception {
		Folder folder = file.getFolder();
		java.io.File nodeBaseDir = new java.io.File(testContext.getPubDir(), node.getHostname());
		java.io.File nodePubDir = new java.io.File(nodeBaseDir, node.getPublishDir());
		java.io.File folderPubDir = new java.io.File(nodePubDir, folder.getPublishDir());
		java.io.File publishedFile = new java.io.File(folderPubDir, file.getName());

		if (expected) {
			assertTrue("File " + publishedFile + " must exist", publishedFile.exists());
		} else {
			assertFalse("File " + publishedFile + " must not exist", publishedFile.exists());
		}
	}

	/**
	 * Assert existence/nonexistence of the published file in the contentrepository of the given node
	 * @param node node
	 * @param file file
	 * @param expected true if the file is expected to exist
	 * @throws Exception
	 */
	public void assertFileInContentrepository(Node node, File file, boolean expected) throws Exception {
		Datasource datasource = datasources.get(node.getId());

		assertNotNull(node + " has no datasource", datasource);
		File channelVariant = file.getChannelVariant(node);
		if (channelVariant == null) {
			channelVariant = file;
		}
		Resolvable crFile = PortalConnectorFactory.getContentObject("10008." + channelVariant.getId(), datasource);

		if (expected) {
			assertNotNull("File " + channelVariant + " must exist in cr of " + node, crFile);
		} else {
			assertNull("File " + channelVariant + " must not exist in cr of " + node, crFile);
		}

		// if the channel variant is different than the given file, we don't want the original file in the contentrepository
		if (!channelVariant.equals(file)) {
			crFile = PortalConnectorFactory.getContentObject("10008." + file.getId(), datasource);
			assertNull("File " + file + " must not exist in cr of " + node + ", because it is different than the channelset variant", crFile);
		}
	}

	/**
	 * Enumeration of possible inheritance situations for the file
	 */
	public enum Inheritance {

		/**
		 * The file is created in the Master and inherited in the Channel
		 */
		INHERITED("i"), /**
		 * The file is created in the Master and localized in the Channel
		 */ LOCALIZED("l"), /**
		 * The file is created in the Channel
		 */ LOCAL("c");

		/**
		 * Short Name
		 */
		protected String shortName;

		/**
		 * Create an instance
		 * 
		 * @param shortName
		 */
		Inheritance(String shortName) {
			this.shortName = shortName;
		}

		/**
		 * Get the short name
		 * 
		 * @return short name
		 */
		public String getShortName() {
			return shortName;
		}

		/**
		 * Check whether this inheritance setting allows the MCSettings
		 * @param feature feature setting
		 * @param force force online setting
		 * @param dependency dependency setting 
		 * @return true if the combination is allowed, false if not
		 */
		public boolean allows(MCSetting feature, MCSetting force, MCSetting dependency) {
			switch (this) {
			case INHERITED:
				// inherited objects have their setting in the master only
				if (!(force == MCSetting.MASTER || force == MCSetting.NONE)) {
					return false;
				}
				break;

			case LOCALIZED:
				// when the object is localized, all combinations are possible
				break;

			case LOCAL:
				// local objects only exist in the channel (master is not interesting)
				if (!(feature == MCSetting.CHANNEL || feature == MCSetting.NONE)) {
					return false;
				}
				if (!(force == MCSetting.CHANNEL || force == MCSetting.NONE)) {
					return false;
				}
				if (!(dependency == MCSetting.CHANNEL || dependency == MCSetting.NONE)) {
					return false;
				}
				break;

			default:
				return false;
			}

			return true;
		}
	}

	/**
	 * Enumeration of possible combinations for some settings
	 */
	public enum MCSetting {

		/**
		 * Only set in the master
		 */
		MASTER("m"), /**
		 * Only set in the channel
		 */ CHANNEL("c"), /**
		 * Set in the master and channel
		 */ BOTH("b"), /**
		 * Not set at all
		 */ NONE("n");

		/**
		 * Short Name
		 */
		protected String shortName;

		/**
		 * Create an instance
		 * 
		 * @param shortName
		 */
		MCSetting(String shortName) {
			this.shortName = shortName;
		}

		/**
		 * Check whether the setting includes the master
		 * @return true when the master is included, false if not
		 */
		public boolean inMaster() {
			return this == MASTER || this == BOTH;
		}

		/**
		 * Check whether the setting includes the channel
		 * @return true when the channel is included, false if not
		 */
		public boolean inChannel() {
			return this == CHANNEL || this == BOTH;
		}

		/**
		 * Get the short name
		 * 
		 * @return short name
		 */
		public String getShortName() {
			return shortName;
		}
	}
}