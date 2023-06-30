package com.gentics.contentnode.tests.publish.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.testutils.ContentRepositoryResource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.testutils.GenericTestUtils;

@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR })
public class AbstractMCCRPublishingSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Rule
	public ContentRepositoryResource contentRepository = new ContentRepositoryResource();

	/**
	 * Multichannelling datasources for the tests
	 */
	protected MCCRDatasource datasource;

	/**
	 * Maps keys to page ids
	 */
	protected Map<String, Integer> pageIds = new HashMap<String, Integer>();

	/**
	 * Maps keys to file ids
	 */
	protected Map<String, Integer> fileIds = new HashMap<String, Integer>();

	/**
	 * Maps keys to image ids
	 */
	protected Map<String, Integer> imageIds = new HashMap<String, Integer>();

	/**
	 * Maps keys to folder ids
	 */
	protected Map<String, Integer> folderIds = new HashMap<String, Integer>();

	/**
	 * The master node for testing
	 */
	protected Node master;

	/**
	 * The sub-channel for testing
	 */
	protected Node channel;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	public static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (Inheritance inheritance : Inheritance.values()) {
			for (ObjectType objectType : ObjectType.values()) {
				if (objectType == ObjectType.PAGE) {
					// Pre-calculate what online statuses we can use for different inheritance use-cases.
					MCSetting[] onlineStatusVariants = null;

					if (inheritance == Inheritance.LOCAL) {
						// When inheritance is LOCAL - the page is not in master so we cannot use MASTER or BOTH
						onlineStatusVariants = new MCSetting[] { MCSetting.CHANNEL, MCSetting.NONE };
					} else if (inheritance == Inheritance.INHERITED) {
						// When inheritance is INHERITED - the page is not in master so we cannot use CHANNEL or BOTH
						onlineStatusVariants = new MCSetting[] { MCSetting.MASTER, MCSetting.NONE };
					} else {
						// When inheritance is LOCALIZED we can use all four online statuses because we have a page in master and in channel
						onlineStatusVariants = MCSetting.values();
					}

					for (MCSetting onlineStatus : onlineStatusVariants) {
						testData.add(new Object[] { onlineStatus, inheritance, objectType });
					}
				} else {
					switch (inheritance) {
					case INHERITED:
						testData.add(new Object[] { MCSetting.MASTER, inheritance, objectType });
						break;

					case LOCALIZED:
						testData.add(new Object[] { MCSetting.BOTH, inheritance, objectType });
						break;

					case LOCAL:
						testData.add(new Object[] { MCSetting.CHANNEL, inheritance, objectType });
						break;
					}
				}
			}
		}
		return testData;
	}

	/**
	 * Get the key for the given test parameters
	 * 
	 * @param onlineStatus
	 *            indicates where the page should be published
	 * @param inheritance
	 *            inheritance setting
	 * @return key
	 */
	public static String getKey(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) {
		StringBuffer key = new StringBuffer();

		key.append(onlineStatus.getShortName()).append(inheritance.getShortName()).append(objectType.getShortName());
		return key.toString();
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

		// create master node
		master = createNode("masterNode", "masterNode", instantPublishing);

		// create channel node
		channel = createChannel(master, "channelNode", "channelNode", instantPublishing);

		// run the publish process to create nodes and channel structure before inserting the test data
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());

		Template template = createTemplate(master.getFolder());

		// generate test data
		Collection<Object[]> testParameters = getTestParameters();

		for (Object[] params : testParameters) {
			MCSetting onlineStatus = (MCSetting) params[0];
			Inheritance inheritance = (Inheritance) params[1];
			ObjectType objectType = (ObjectType) params[2];

			String key = getKey(onlineStatus, inheritance, objectType);

			// Create the correct object by the specified objectType
			switch (objectType) {
			case PAGE:
				pageIds.put(key, ObjectTransformer.getInteger(createPage(template, onlineStatus, inheritance, objectType).getId(), null));
				break;

			case FILE:
				fileIds.put(key, ObjectTransformer.getInteger(createFile(template, onlineStatus, inheritance, objectType).getId(), null));
				break;

			case IMAGE:
				imageIds.put(key, ObjectTransformer.getInteger(createImage(template, onlineStatus, inheritance, objectType).getId(), null));
				break;

			case FOLDER:
				folderIds.put(key, ObjectTransformer.getInteger(createFolder(template, onlineStatus, inheritance, objectType).getId(), null));
				break;

			default:
				break;
			}

		}

		if (runPublishProcess) {
			// run the publish process
			testContext.getContext().startTransaction();
			assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(true).getReturnCode());
		}
	}

	/**
	 * Get the test page for the given setting
	 * 
	 * @param onlineStatus
	 *            indicates where the page should be published
	 * @param inheritance
	 *            inheritance setting
	 * @return test file
	 * @throws Exception
	 */
	public Page getTestPage(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer pageId = pageIds.get(getKey(onlineStatus, inheritance, objectType));

		return t.getObject(Page.class, pageId);
	}

	/**
	 * Get the test file for the given setting
	 * 
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public File getTestFile(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer fileId = fileIds.get(getKey(onlineStatus, inheritance, objectType));

		return t.getObject(ImageFile.class, fileId);
	}

	/**
	 * Get the test image for the given setting
	 * 
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public ImageFile getTestImage(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer imageId = imageIds.get(getKey(onlineStatus, inheritance, objectType));

		return t.getObject(ImageFile.class, imageId);
	}

	/**
	 * Get the test folder for the given setting
	 * 
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public Folder getTestFolder(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer folderId = folderIds.get(getKey(onlineStatus, inheritance, objectType));

		return t.getObject(Folder.class, folderId);
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
		template.setSource("<node file>");
		template.setFolderId(folder.getId());
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a new Page
	 * 
	 * @param template
	 * @param image
	 * @param inheritance
	 * @return
	 * @throws Exception
	 */
	public Page createPage(Template template, MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		String nameAppendix = getKey(onlineStatus, inheritance, objectType);

		// create a page in the master, using this file
		Page page = t.createObject(Page.class);

		page.setTemplateId(template.getId(), true);
		page.setName("Page " + nameAppendix);

		if (inheritance == Inheritance.LOCAL) {
			// create a local file
			page.setFolderId(channel.getFolder().getId());
			page.setChannelInfo(channel.getId(), null);
		} else {
			page.setFolderId(master.getFolder().getId());
		}

		page.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		Page localizedPage = null;

		if (inheritance == Inheritance.LOCALIZED) {
			// localize the file
			localizedPage = (Page) page.copy();
			localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
			localizedPage.save();
			t.commit(false);

			// it is necessary to reset the published objects attribute in the transaction,
			// otherwise the file would not be handled again during the test
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);
		}

		// publish or take offline pages
		switch (onlineStatus) {
		case MASTER:
			page.publish();
			if (localizedPage != null) {
				localizedPage.takeOffline();
			}
			break;

		case CHANNEL:
			if (inheritance == Inheritance.LOCAL) {
				page.publish();
			} else {
				page.takeOffline();
			}
			if (localizedPage != null) {
				localizedPage.publish();
			}
			break;

		case BOTH:
			page.publish();
			if (localizedPage != null) {
				localizedPage.publish();
			}
			break;

		case NONE:
			page.takeOffline();
			if (localizedPage != null) {
				localizedPage.takeOffline();
			}
			break;
		}
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
	 * @param template
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public File createFile(Template template, MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		String nameAppendix = getKey(onlineStatus, inheritance, objectType);

		// create a page in the master, using this file
		File file = t.createObject(File.class);

		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("textfile_" + nameAppendix.toString() + ".txt");

		if (inheritance == Inheritance.LOCAL) {
			// create a local file
			file.setFolderId(channel.getFolder().getId());
			file.setChannelInfo(channel.getId(), null);
		} else {
			file.setFolderId(master.getFolder().getId());
		}

		file.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		File localizedFile = null;

		if (inheritance == Inheritance.LOCALIZED) {
			// localize the file
			localizedFile = (File) file.copy();
			localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);

			// it is necessary to reset the published objects attribute in the transaction,
			// otherwise the file would not be handled again during the test
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);
		}

		return file;
	}

	/**
	 * Create new image
	 * 
	 * @param template
	 * @param onlineStatus
	 * @param inheritance
	 * @param objectType
	 * @return
	 * @throws Exception
	 */
	public ImageFile createImage(Template template, MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		String nameAppendix = getKey(onlineStatus, inheritance, objectType);

		// create a page in the master, using this file
		ImageFile file = t.createObject(ImageFile.class);

		file.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		file.setName("blume_" + nameAppendix.toString() + ".jpg");

		if (inheritance == Inheritance.LOCAL) {
			// create a local file
			file.setFolderId(channel.getFolder().getId());
			file.setChannelInfo(channel.getId(), null);
		} else {
			file.setFolderId(master.getFolder().getId());
		}

		file.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		ImageFile localizedFile = null;

		if (inheritance == Inheritance.LOCALIZED) {
			// localize the file
			localizedFile = (ImageFile) file.copy();
			localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);

			// it is necessary to reset the published objects attribute in the transaction,
			// otherwise the file would not be handled again during the test
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);
		}

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
	public Folder createFolder(Template template, MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		String nameAppendix = getKey(onlineStatus, inheritance, objectType);

		// create a page in the master, using this file
		Folder folder = t.createObject(Folder.class);

		folder.setName("folder_" + nameAppendix.toString() + ".jpg");

		if (inheritance == Inheritance.LOCAL) {
			// create a local file
			folder.setMotherId(channel.getFolder().getId());
			folder.setChannelInfo(channel.getId(), null);
		} else {
			folder.setMotherId(master.getFolder().getId());
		}

		folder.save();
		t.commit(false);

		// it is necessary to reset the published objects attribute in the transaction,
		// otherwise the file would not be handled again during the test
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
		t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);

		Folder localizedFolder = null;

		if (inheritance == Inheritance.LOCALIZED) {
			// localize the file
			localizedFolder = (Folder) folder.copy();
			localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
			localizedFolder.save();
			t.commit(false);

			// it is necessary to reset the published objects attribute in the transaction,
			// otherwise the file would not be handled again during the test
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED, null);
			t.getAttributes().put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, null);
		}

		return folder;
	}

	/**
	 * Create a datasource for the given node
	 * 
	 * @return handle properties
	 * @throws Exception
	 */
	private Map<String, String> createDatasource() throws Exception {
		Map<String, String> handleProperties = contentRepository.getCRTestUtils().getTestDatabase().getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("multichannelling", "true");

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

		// create a datasource
		Map<String, String> handleProperties = createDatasource();
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);
		cr.setInstantPublishing(instantPublishing);
		cr.setMultichannelling(true);
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
	 * Assert existence/nonexistence of the published file in the contentrepository of the given node
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
	 * Assert existence/nonexistence of the published page in the contentrepository of the given node
	 * 
	 * @param node
	 * @param page
	 * @param expected
	 * @throws Exception
	 */
	public void assertPageInContentrepository(Node node, Page page, boolean expected) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		assertNotNull(node + " has no datasource", datasource);

		Resolvable crPage = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		if (expected) {
			assertNotNull("Page " + page + " must exist in cr of " + node, crPage);
		} else {
			assertNull("Page " + page + " must not exist in cr of " + node, crPage);
		}
	}

	/**
	 * Assert existence/nonexistence of the published folder in the contentrepository of the given node
	 * 
	 * @param node
	 * @param folder
	 * @param expected
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

	/**
	 * Enumeration of possible inheritance situations for the file
	 */
	public enum Inheritance {

		/**
		 * The file is created in the Master and inherited in the Channel
		 */
		INHERITED("i"), /**
		 * The file is created in the Master and localized in the Channel
		 */
		LOCALIZED("l"), /**
		 * The file is created in the Channel
		 */
		LOCAL("c");

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
		 * 
		 * @param feature
		 *            feature setting
		 * @param force
		 *            force online setting
		 * @param dependency
		 *            dependency setting
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
		 */
		CHANNEL("c"), /**
		 * Set in the master and channel
		 */
		BOTH("b"), /**
		 * Not set at all
		 */
		NONE("n");

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
		 * 
		 * @return true when the master is included, false if not
		 */
		public boolean inMaster() {
			return this == MASTER || this == BOTH;
		}

		/**
		 * Check whether the setting includes the channel
		 * 
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
