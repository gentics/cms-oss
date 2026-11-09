package com.gentics.contentnode.tests.publish.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.TemplateTagPartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.testutils.ContentRepositoryResource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for rendering page with updated dependencies
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR })
public abstract class AbstractRenderingMCUpdatedPagesSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(prefs -> {
		prefs.set("db.settings.login", "node");
	});

	@Rule
	public ContentRepositoryResource testCR = new ContentRepositoryResource();

	public static int HTMLLONG_CONSTRUCT_ID = 1;
	
	public static int FILEURL_CONSTRUCT_ID = 11;
	
	public static int PAGEURL_CONSTRUCT_ID = 9;
	
	public static int OVERVIEW_CONSTRUCT_ID = 5;
	
	public static int RENDER_CONSTRUCT_ID = 18;
	
	public static int DEPENDENCY_CONSTRUCT_ID = 19;
	
	public static int PAGE_TAG_CONSTRUCT_ID = 13;
	
	public static int TEMPLATE_TAG_CONSTRUCT_ID = 14;
	
	/**
	 * Name of the overview tag
	 */
	public final static String OVERVIEW_TAGNAME = "overview";

	/**
	 * Name of the overview part
	 */
	public final static String OVERVIEW_PARTNAME = "ds";

	private final PageCreationSettings pageCreationSettings;

	private final DependencyLocalizationSettings dependencyLocalizationSettings;

	private final DependencySettings dependencySettings;

	private final boolean updateInMaster;

	private Integer overviewReferenceObjectId;

	public final static String CR_BASE_PATH = "/tmp/gentics/CR/";

	/**
	 * Multichannelling datasources for the tests
	 */
	protected MCCRDatasource datasource;

	/**
	 * The master node for testing
	 */
	protected Node master;

	/**
	 * The channel for testing
	 */
	protected Node channel;

	/**
	 * The sub-channel for testing
	 */
	protected Node subChannel;

	protected String tagName;

	/**
	 * Part type ID for the rendering counter part type
	 */
	private static int PART_TYPE_ID = 22334;


	/**
	 * Create a datasource for the given node
	 * @return handle properties
	 * @throws Exception
	 */
	private Map<String, String> createDatasource() throws Exception {
		Map<String, String> handleProperties = testCR.getCRTestUtils().getTestDatabase().getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put("multichannelling", "true");
		dsProperties.put("attribute.path", getBasePath());

		// Get normal or writable datasource
		datasource = (MCCRDatasource) PortalConnectorFactory.createMultichannellingDatasource(handleProperties, dsProperties);
		assertNotNull("The datasource with properties {" + handleProperties + "} coult not be created.", datasource);

		return dsProperties;
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param instantPublishing true to use instant publishing into the contentrepository
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

		// create a folder
		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(rootFolder.getId());
		folder.setName("Home");
		folder.setPublishDir("/");
		folder.save();
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
		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?", 
				new Object[] { "mysql", handleProperties.get("url"), handleProperties.get("username"), handleProperties.get("passwd"), cr.getId()});

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
	 * @param instantPublishing true to use instant publishing into the contentrepository
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
	 * Assert page content to be equals with an expected content.
	 * 
	 * @param page
	 * @param channel
	 * @param expectedContent
	 * @throws Exception
	 */
	public void assertPageRenderedContent(Page page, Node channel, String expectedContent) throws Exception {
		datasource.setChannel(ObjectTransformer.getInt(channel.getId(), 0));
		Resolvable resolvable = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);
		
		String resolvableAttr = ObjectTransformer.getString(resolvable.get("content"), null);
		
		assertNotNull("Attribute content of master object should not be null.", resolvableAttr);
		assertEquals("Rendering content is not as expected", expectedContent, resolvableAttr);
	}
	
	/**
	 * Get the base path for the content repository (create the directories if they are missing).
	 * 
	 * @return CR base path
	 */
	public String getBasePath() {
		java.io.File diskBaseDir = new java.io.File(CR_BASE_PATH);

		if (!diskBaseDir.exists()) {
			assertTrue("Could not create directories for CR base directory", diskBaseDir.mkdirs());
		}

		return CR_BASE_PATH;
	}
	
	public Node getSubChannel() {
		return subChannel;
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
	 * Enumeration of the nodes variations we'll create the source page in.
	 */
	public enum PageCreationSettings {

		/**
		 * Only set in the channel
		 */
		MASTER, /**
		 * Set in the master and channel
		 */ CHANNEL;

	}
	
	/**
	 * Enumeration of the dependencies we're going to use in the variations.
	 */
	public enum DependencySettings {
		TEMPLATE, PAGE_URL, FILE_URL, OVERVIEW_PAGE, OVERVIEW_FILE, OVERVIEW_IMAGE, OVERVIEW_FOLDER, PAGE_TAG, TEMPLATE_TAG;
	}
	
	/**
	 * Enumeration of possible localizations
	 */
	public enum DependencyLocalizationSettings {

		/**
		 * Localized in channel
		 */
		CHANNEL, /**
		 * Localized in subchannel
		 */ SUBCHANNEL, /**
		 * Localized in both channel and subchannel
		 */ BOTH,
		
	}
	
	@Before
	public void setUp() throws Exception {

		testContext.updateCRReference(testCR);

		final boolean INSTANT_PUBLISH_DISABLED = false;

		// remove all logcmd entries
		testContext.getDBSQLUtils().executeQueryManipulation("DELETE FROM logcmd");

		// disable publishing of all nodes (we will create our own nodes)
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE node SET disable_publish = 1, publish_fs = 0, publish_contentmap = 0");

		// Check if there already is RenderingCounterPartType
		String selectSql = "SELECT * FROM type WHERE id = " + PART_TYPE_ID;
		ResultSet res = testContext.getDBSQLUtils().executeQuery(selectSql);

		if (!res.next()) {
			// Create the RenderingCounterPartType record in the database
			StringBuilder sql = new StringBuilder("INSERT INTO type (id, name, description, auto, javaclass) VALUES (");

			sql.append(PART_TYPE_ID).append(", 'RenderingCounter', '', 0, '").append(RenderingCounterPartType.class.getName());
			sql.append("')");
			
			testContext.getDBSQLUtils().executeQueryInsert(sql.toString());
		}

		// create master node
		master = createNode("masterNode", "masterNode", INSTANT_PUBLISH_DISABLED);

		// create channel node
		channel = createChannel(master, "channelNode", "channelNode", INSTANT_PUBLISH_DISABLED);

		// create subchannel node
		subChannel = createChannel(channel, "subChannelNode", "subChannelNode", INSTANT_PUBLISH_DISABLED);

	}

	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
		RenderingCounterPartType.resetRenderingCounter();
	}

	public void runPublishProcess() throws Exception {
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
	}

	/**
	 * Create a parameterized instance of the test case
	 * @param objectType {@link ObjectType} settings
	 */
	public AbstractRenderingMCUpdatedPagesSandboxTest(PageCreationSettings pageCreationSettings, DependencySettings dependencySettings, 
			DependencyLocalizationSettings dependencyLocalizationSettings, boolean updateInMaster) {
		this.pageCreationSettings = pageCreationSettings;
		this.dependencyLocalizationSettings = dependencyLocalizationSettings;
		this.dependencySettings = dependencySettings;
		this.updateInMaster = updateInMaster;
	}

	/**
	 * Test rendering of inherited page with dependencies
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderInheritedPage() throws Exception {
		Template template = createTemplate();
		Page page = createPage(template.getId());

		runPublishProcess();

		// wait a second, so we will get a new timestamp for the page version. Otherwise finding out the changes since the last publish run would not work correctly
		Thread.sleep(1000);
		testContext.getContext().startTransaction();

		updateDependency(page, template);
		
		// Reset rendering counter
		RenderingCounterPartType.resetRenderingCounter();

		runPublishProcess();

		assertRenderingOutput(page);

		// TODO activate this test (find out, how often we expect the source to be rendered for every test variation)
		// assertEquals("Rendering counts are not the number we've expected.", 1, RenderingCounterPartType.getRenderingCounter());
	}

	/**
	 * Asserts the rendered output of the page, based on the pre-setup page dependencies.
	 * 
	 * @param page
	 * @param template
	 * @throws Exception
	 */
	private void assertRenderingOutput(Page page) throws Exception {
		
		switch (dependencySettings) {
		case FILE_URL:
			assertFileUrlRenderingOutput(page);
			break;

		case PAGE_URL:
			assertPageUrlRenderingOutput(page);
			break;

		case PAGE_TAG:
			assertPageTagRenderingOutput(page);
			break;

		case TEMPLATE_TAG:
			assertTemplateTagRenderingOutput(page);
			break;

		case TEMPLATE:
			assertTemplateRenderingOutput(page);
			break;

		case OVERVIEW_PAGE:
		case OVERVIEW_FILE:
		case OVERVIEW_IMAGE:
		case OVERVIEW_FOLDER:
			assertOverviewRenderingOutput(page);
			break;
		}
		
	}
	
	/**
	 * Asserts the rendering output when configured with Overview dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertOverviewRenderingOutput(Page page) throws Exception {
		switch (dependencySettings) {
		case OVERVIEW_PAGE:
			if (pageCreationSettings == PageCreationSettings.MASTER) {
				if (updateInMaster) {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
				} else {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, TestPage-master TestPage-master-Overview");
				}
			}
			
			switch (dependencyLocalizationSettings) {
			case CHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-channel-Overview TestPage-master");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-channel-Overview TestPage-master");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
				}
				break;

			case SUBCHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-master TestPage-subchannel-Overview");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-master TestPage-master-Overview");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
				}
				break;
							
			case BOTH:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-channel-Overview TestPage-master");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-master TestPage-subchannel-Overview");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestPage-master Updated-Overview-Referenced-Page");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestPage-master TestPage-subchannel-Overview");
				}
				break;
			}
			break;
			
		case OVERVIEW_FILE:
			if (pageCreationSettings == PageCreationSettings.MASTER) {
				if (updateInMaster) {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
				} else {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, TestFile-master-Overview.txt");
				}
			}
			
			switch (dependencyLocalizationSettings) {
			case CHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFile-channel-Overview.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFile-channel-Overview.txt");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
				}
				break;

			case SUBCHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFile-subchannel-Overview.txt");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFile-master-Overview.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
				}
				break;
							
			case BOTH:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFile-channel-Overview.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFile-subchannel-Overview.txt");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-File.txt");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFile-subchannel-Overview.txt");
				}
				break;
			}
			break;
			
		case OVERVIEW_IMAGE:
			if (pageCreationSettings == PageCreationSettings.MASTER) {
				if (updateInMaster) {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
				} else {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, TestImageFile-master-Overview.jpg");
				}
			}
			
			switch (dependencyLocalizationSettings) {
			case CHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestImageFile-channel-Overview.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestImageFile-channel-Overview.jpg");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
				}
				break;

			case SUBCHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestImageFile-subchannel-Overview.jpg");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestImageFile-master-Overview.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
				}
				break;
							
			case BOTH:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestImageFile-channel-Overview.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestImageFile-subchannel-Overview.jpg");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Image.jpg");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestImageFile-subchannel-Overview.jpg");
				}
				break;
			}
			break;
			
		case OVERVIEW_FOLDER:
			if (pageCreationSettings == PageCreationSettings.MASTER) {
				if (updateInMaster) {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
				} else {
					assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, TestFolder-master-Overview");
				}
			}
			
			switch (dependencyLocalizationSettings) {
			case CHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFolder-channel-Overview");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFolder-channel-Overview");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
				}
				break;

			case SUBCHANNEL:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFolder-subchannel-Overview");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFolder-master-Overview");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
				}
				break;
							
			case BOTH:
				if (updateInMaster) {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, TestFolder-channel-Overview");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFolder-subchannel-Overview");
				} else {
					assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated-Overview-Referenced-Folder");
					assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, TestFolder-subchannel-Overview");
				}
				break;
			}
			break;
		}
	}
	
	/**
	 * Asserts the rendering output when configured with Template dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertTemplateRenderingOutput(Page page) throws Exception {
		if (pageCreationSettings == PageCreationSettings.MASTER) {
			if (updateInMaster) {
				assertPageRenderedContent(page, getMaster(), "TestTemplate-updated: RenderingCounterPartType");
			} else {
				assertPageRenderedContent(page, getMaster(), "TestTemplate-master: RenderingCounterPartType");
			}
		}
		
		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-channel: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-channel: RenderingCounterPartType");
			} else {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-updated: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-updated: RenderingCounterPartType");
			}
			break;

		case SUBCHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-updated: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-subchannel: RenderingCounterPartType");
			} else {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-master: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-updated: RenderingCounterPartType");
			}
			break;
						
		case BOTH:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-channel: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-subchannel: RenderingCounterPartType");
			} else {
				assertPageRenderedContent(page, getChannel(), "TestTemplate-updated: RenderingCounterPartType");
				assertPageRenderedContent(page, getSubChannel(), "TestTemplate-subchannel: RenderingCounterPartType");
			}
			break;
		}
	}
	
	/**
	 * Asserts the rendering output when configured with TemplateTag dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertTemplateTagRenderingOutput(Page page) throws Exception {
		if (pageCreationSettings == PageCreationSettings.MASTER) {
			if (updateInMaster) {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Updated content");
			} else {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Master content");
			}
		}
		
		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Channel content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Channel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated content");
			}
			break;
			
		case SUBCHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Master content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated content");
			}
			break;

		case BOTH: // both means updated just in channel
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Channel content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			}
			break;
		}
	}
	
	/**
	 * Asserts the rendering output when configured with PageTag dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertPageTagRenderingOutput(Page page) throws Exception {
		if (pageCreationSettings == PageCreationSettings.MASTER) {
			if (updateInMaster) {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Updated content");
			} else {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, Master content");
			}
		}
		
		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Channel content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Channel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated content");
			}
			break;
			
		case SUBCHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Master content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, Updated content");
			}
			break;

		case BOTH: // both means updated just in channel
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Channel content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, Updated content");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, SubChannel content");
			}
			break;
		}
	}
	
	/**
	 * Asserts the rendering output when configured with FileUrl dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertFileUrlRenderingOutput(Page page) throws Exception {
		if (pageCreationSettings == PageCreationSettings.MASTER) {
			if (updateInMaster) {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
			} else {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, /TestFile-master-DependencyFile.txt");
			}
		}
		
		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			if (updateInMaster) {
				// Updated in master but localized in channel
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestFile-channel-DependencyFile.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestFile-channel-DependencyFile.txt");
			} else {
				// Localized and update in channel
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
			}
			break;
			
		case SUBCHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestFile-subchannel-DependencyFile.txt");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestFile-master-DependencyFile.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
			}
			
			break;

		case BOTH: // both means updated just in channel but localized in channel and subchannel
			if (updateInMaster) {
				// Updated in master but localized in channel
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestFile-channel-DependencyFile.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestFile-subchannel-DependencyFile.txt");
			} else {
				// Localized and update in channel
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-File-Name.txt");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestFile-subchannel-DependencyFile.txt");
			}
			break;
		}
	}
	
	/**
	 * Asserts the rendering output when configured with PageUrl dependency.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void assertPageUrlRenderingOutput(Page page) throws Exception {
		if (pageCreationSettings == PageCreationSettings.MASTER) {
			if (updateInMaster) {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
			} else {
				assertPageRenderedContent(page, getMaster(), "RenderingCounterPartType, /TestPage-master-DependencyPage.html");
			}
		}
		
		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestPage-channel-DependencyPage.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestPage-channel-DependencyPage.html");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
			}
			break;
			
		case SUBCHANNEL:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestPage-subchannel-DependencyPage.html");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestPage-master-DependencyPage.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
			}
			break;
			
		case BOTH:
			if (updateInMaster) {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /TestPage-channel-DependencyPage.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestPage-subchannel-DependencyPage.html");
			} else {
				assertPageRenderedContent(page, getChannel(), "RenderingCounterPartType, /Updated-Dependency-Page-Name.html");
				assertPageRenderedContent(page, getSubChannel(), "RenderingCounterPartType, /TestPage-subchannel-DependencyPage.html");
			}
			break;
		}
	}
	
	/**
	 * Update dependency for our target page in a specified channel
	 * 
	 * @param page
	 * @param template
	 * @throws Exception
	 */
	private void updateDependency(Page page, Template template) throws Exception {
		// Determine which channel should we update the dependency in
		// (we're using separate flag updateInMaster because DependencyLocalizationSettings
		// does not include MASTER as an option value).
		Node updateNode = null;

		if (updateInMaster) {
			updateNode = getMaster();
		} else {
			switch (dependencyLocalizationSettings) {
			case BOTH:
			case CHANNEL:
				updateNode = getChannel();
				break;

			case SUBCHANNEL:
				updateNode = getSubChannel();
				break;
			}
		}
		
		assertNotNull("updateNode should be initialized", updateNode);
		
		// Check what kind of dependency we're currently working with and trigger an update procedure
		switch (dependencySettings) {
		case PAGE_URL:
			updatePageUrlDependency(page, updateNode);
			break;

		case FILE_URL:
			updateFileUrlDependency(page, updateNode);
			break;

		case OVERVIEW_PAGE:
		case OVERVIEW_FILE:
		case OVERVIEW_IMAGE:
		case OVERVIEW_FOLDER:
			updateOverviewDependency(page, updateNode);
			break;

		case PAGE_TAG:
			updatePageTagDependency(page, updateNode);
			break;

		case TEMPLATE_TAG:
			updateTemplateTagDependency(page, updateNode);
			break;

		case TEMPLATE:
			updateTemplateDependency(template, updateNode);
			break;

		default:
			break;
		}
	}
	
	/**
	 * Update Template dependency
	 * 
	 * @param targetTemplate
	 * @param node
	 * @throws Exception
	 */
	private void updateTemplateDependency(Template targetTemplate, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		t.setChannelId(node.getId());
		
		Template template = t.getObject(Template.class, targetTemplate.getId(), true);

		template.setName("Updated-Page-Template");
		template.setSource("TestTemplate-updated: <node htmllong>");
		
		t.resetChannel();
		
		template.save();
		t.commit(false);
	}
	
	/**
	 * Update reference page dependency
	 * 
	 * @param targetPage
	 * @param node
	 * @throws Exception
	 */
	private void updatePageUrlDependency(Page targetPage, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page page = t.getObject(Page.class, targetPage.getId());
		ContentTag contentTag = page.getContentTag("pageurl");
		Value pageValue = contentTag.getValues().iterator().next();
		PartType pagePartType = pageValue.getPartType();

		if (pagePartType instanceof PageURLPartType) {
			Integer dependencyPageId = ((PageURLPartType) pagePartType).getTargetPage().getId();
			
			t.setChannelId(node.getId());
			
			Page dependencyPage = t.getObject(Page.class, dependencyPageId, true);

			dependencyPage.setName("Updated-Dependency-Page-Name");
			dependencyPage.setFilename(dependencyPage.getName() + ".html");
			
			t.resetChannel();
			
			dependencyPage.save();
			dependencyPage.publish();
			t.commit(false);
		} else {
			fail("parttype should be a PageURLPartType");
		}
	}
	
	/**
	 * Update reference file dependency
	 * 
	 * @param targetPage
	 * @param node
	 * @throws Exception
	 */
	private void updateFileUrlDependency(Page targetPage, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page page = t.getObject(Page.class, targetPage.getId());
		ContentTag contentTag = page.getContentTag("file");
		Value pageValue = contentTag.getValues().iterator().next();
		PartType filePartType = pageValue.getPartType();

		if (filePartType instanceof FileURLPartType) {
			Integer dependencyFileId = ((FileURLPartType) filePartType).getTargetFile().getId();
			
			t.setChannelId(node.getId());
			
			File dependencyFile = t.getObject(File.class, dependencyFileId, true);

			dependencyFile.setName("Updated-Dependency-File-Name.txt");
			
			t.resetChannel();
			
			dependencyFile.save();
			t.commit(false);
		} else {
			fail("parttype should be a FileURLPartType");
		}
	}
	
	/**
	 * Update PageTag dependency
	 * 
	 * @param targetPage
	 * @param node
	 * @throws Exception
	 */
	private void updatePageTagDependency(Page targetPage, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page page = t.getObject(Page.class, targetPage.getId());
		ContentTag contentTag = page.getContentTag("pagetag");
		Value pageValue = contentTag.getValues().iterator().next();
		PartType partType = pageValue.getPartType();

		if (partType instanceof PageTagPartType) {
			Integer referencePageId = ((PageTagPartType) partType).getPageId();
			
			t.setChannelId(node.getId());
			
			Page referencePage = t.getObject(Page.class, referencePageId, true);
			ContentTag referenceTag = referencePage.getContentTag(tagName);

			referenceTag.setEnabled(true);
			referenceTag.getValues().iterator().next().setValueText("Updated content");
			
			t.resetChannel();
			
			referencePage.save();
			referencePage.publish();
			t.commit(false);
		} else {
			fail("parttype should be a PageTagPartType");
		}
	}
	
	/**
	 * Update TemplateTag dependency
	 * 
	 * @param targetPage
	 * @param node
	 * @throws Exception
	 */
	private void updateTemplateTagDependency(Page targetPage, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Page page = t.getObject(Page.class, targetPage.getId());
		ContentTag contentTag = page.getContentTag("templatetag");
		Value pageValue = contentTag.getValues().iterator().next();
		PartType partType = pageValue.getPartType();

		if (partType instanceof TemplateTagPartType) {
			TagContainer tagContainer = ((TemplateTagPartType) partType).getTagContainer();
			
			if (tagContainer instanceof Template) {
				Integer templateId = ((Template) tagContainer).getId();
				
				t.setChannelId(node.getId());
				
				Template referenceTemplate = t.getObject(Template.class, templateId, true);
				TemplateTag tag = referenceTemplate.getTemplateTag("htmllong");

				tag.setEnabled(true);
				tag.getValues().iterator().next().setValueText("Updated content");
				
				t.resetChannel();
				
				referenceTemplate.save();
				t.commit(false);
			} else {
				fail("tagContainer should be a Template");
			}
		} else {
			fail("parttype should be a PageTagPartType");
		}
	}
	
	/**
	 * Update Overview dependency
	 * 
	 * @param targetPage
	 * @param node
	 * @throws Exception
	 */
	private void updateOverviewDependency(Page targetPage, Node node) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		t.setChannelId(node.getId());
		
		switch (dependencySettings) {
		case OVERVIEW_PAGE:
			Page referencedPage = t.getObject(Page.class, overviewReferenceObjectId, true);

			referencedPage.setName("Updated-Overview-Referenced-Page");
			referencedPage.save();
			referencedPage.publish();
			break;

		case OVERVIEW_FILE:
			File referencedFile = t.getObject(File.class, overviewReferenceObjectId, true);

			referencedFile.setName("Updated-Overview-Referenced-File.txt");
			referencedFile.save();
			break;

		case OVERVIEW_IMAGE:
			ImageFile referencedImage = t.getObject(ImageFile.class, overviewReferenceObjectId, true);

			referencedImage.setName("Updated-Overview-Referenced-Image.jpg");
			referencedImage.save();
			break;

		case OVERVIEW_FOLDER:
			Folder referencedFolder = t.getObject(Folder.class, overviewReferenceObjectId, true);

			referencedFolder.setName("Updated-Overview-Referenced-Folder");
			referencedFolder.save();
			break;
		}
		
		t.resetChannel();
		t.commit(false);
	}
	
	/**
	 * Create target test page template
	 * 
	 * @return
	 * @throws Exception
	 */
	private Template createTemplate() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Create the template with two template tags - htmllong and overview
		Template template = t.createObject(Template.class);

		template.setFolderId(getHomeFolder(getMaster()).getId());
		template.setName("TestTemplate-master");
		
		Map<String, TemplateTag> templateTags = template.getTemplateTags();
		
		// Create htmllong template tag
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setConstructId(1);
		tag.setEnabled(true);
		tag.setName("htmllong");
		tag.setPublic(true);
		templateTags.put(tag.getName(), tag);

		if (dependencySettings == DependencySettings.OVERVIEW_PAGE || dependencySettings == DependencySettings.OVERVIEW_FILE
				|| dependencySettings == DependencySettings.OVERVIEW_IMAGE || dependencySettings == DependencySettings.OVERVIEW_FOLDER) {
			
			template.setSource("<node htmllong>,<node overview>");
			
			// Create overview template tag
			tag = t.createObject(TemplateTag.class);
			tag.setConstructId(OVERVIEW_CONSTRUCT_ID);
			tag.setEnabled(true);
			tag.setName(OVERVIEW_TAGNAME);
			tag.setPublic(true);
			templateTags.put(tag.getName(), tag);
		} else if (dependencySettings == DependencySettings.TEMPLATE) {
			template.setSource("TestTemplate-master: <node htmllong>");
		} else {
			template.setSource("<node htmllong>");
		}

		template.save();
		t.commit(false);
		
		if (dependencySettings == DependencySettings.TEMPLATE) {
			// Depending on the localization settings - localize the template
			Template localizedTemplate = null;

			switch (dependencyLocalizationSettings) {
			case CHANNEL:
				localizedTemplate = (Template) template.copy();
				localizedTemplate.setName("TestTemplate-channel");
				localizedTemplate.setChannelInfo(getChannel().getId(), template.getChannelSetId());
				localizedTemplate.setSource("TestTemplate-channel: <node htmllong>");
				localizedTemplate.save();
				t.commit(false);
				break;

			case SUBCHANNEL:
				localizedTemplate = (Template) template.copy();
				localizedTemplate.setName("TestTemplate-subchannel");
				localizedTemplate.setChannelInfo(getSubChannel().getId(), template.getChannelSetId());
				localizedTemplate.setSource("TestTemplate-subchannel: <node htmllong>");
				localizedTemplate.save();
				t.commit(false);
				break;

			case BOTH:
				localizedTemplate = (Template) template.copy();
				localizedTemplate.setName("TestTemplate-channel");
				localizedTemplate.setChannelInfo(getChannel().getId(), template.getChannelSetId());
				localizedTemplate.setSource("TestTemplate-channel: <node htmllong>");
				localizedTemplate.save();
				t.commit(false);
	
				localizedTemplate = (Template) template.copy();
				localizedTemplate.setName("TestTemplate-subchannel");
				localizedTemplate.setChannelInfo(getSubChannel().getId(), template.getChannelSetId());
				localizedTemplate.setSource("TestTemplate-subchannel: <node htmllong>");
				localizedTemplate.save();
				t.commit(false);
				break;

			default:
				break;
			}
		}
		
		return template;
	}
	
	/**
	 * Create target test page.
	 * 
	 * @param templateId
	 * @return
	 * @throws Exception
	 */
	private Page createPage(Integer templateId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Create the test page
		Page page = t.createObject(Page.class);

		page.setTemplateId(templateId, true);
		page.setName("TestPage-master");
		page.setFolderId(getHomeFolder(getMaster()).getId());
		
		// Depending on the creation settings - create it in master or in channel node
		if (pageCreationSettings == PageCreationSettings.CHANNEL) {
			page.setChannelInfo(getChannel().getId(), null);
		}
		
		StringBuffer htmllongTagValue = new StringBuffer("<node counter>");

		switch (dependencySettings) {
		case PAGE_URL:
			setupPageUrlDependency(page, htmllongTagValue);
			break;

		case FILE_URL:
			setupFileUrlDependency(page, htmllongTagValue);
			break;

		case OVERVIEW_PAGE:
		case OVERVIEW_FILE:
		case OVERVIEW_IMAGE:
		case OVERVIEW_FOLDER:
			setupOverviewDependency(page);
			break;

		case PAGE_TAG:
			setupPageTagDependency(page, htmllongTagValue);
			break;

		case TEMPLATE_TAG:
			setupTemplateTagDependency(page, htmllongTagValue);
			break;

		default:
			break;
		}
		
		// Add content tag using our rendering counter tag type 
		ContentTag counterContentTag = page.getContent().addContentTag(RENDER_CONSTRUCT_ID);

		counterContentTag.setEnabled(true);
		counterContentTag.setName("counter");
		
		// Get htmllong content tag and set its value to reference the dependencies - counter, file, page.
		ContentTag htmllongTag = page.getContentTag("htmllong");

		htmllongTag.getValues().iterator().next().setValueText(htmllongTagValue.toString());
		
		page.save();
		page.publish();
		t.commit(false);
		
		return page;
	}
	
	/**
	 * Prepare PageUrl dependency for the target page.
	 * 
	 * @param page
	 * @param htmllongTagValue
	 * @throws Exception
	 */
	private void setupPageUrlDependency(Page page, StringBuffer htmllongTagValue) throws Exception {
		// Create page content tag and use it as a reference to the dependency page
		ContentTag pageContentTag = page.getContent().addContentTag(PAGEURL_CONSTRUCT_ID);

		pageContentTag.setEnabled(true);
		pageContentTag.setName("pageurl");
		Value pageValue = pageContentTag.getValues().iterator().next();
		PartType pagePartType = pageValue.getPartType();

		if (pagePartType instanceof PageURLPartType) {
			Page dependencyPage = createPageDependency(getHomeFolder(getMaster()), "DependencyPage");

			((PageURLPartType) pagePartType).setTargetPage(dependencyPage);
		} else {
			fail("parttype should be a PageURLPartType");
		}
		htmllongTagValue.append(", <node pageurl>");
	}
	
	/**
	 * Prepare FileUrl dependency for the target page.
	 * 
	 * @param page
	 * @param htmllongTagValue
	 * @throws Exception
	 */
	private void setupFileUrlDependency(Page page, StringBuffer htmllongTagValue) throws Exception {
		// Create file content tag and use it as a reference to the dependency file
		ContentTag fileContentTag = page.getContent().addContentTag(FILEURL_CONSTRUCT_ID);

		fileContentTag.setEnabled(true);
		fileContentTag.setName("file");
		Value fileValue = fileContentTag.getValues().iterator().next();
		PartType filePartType = fileValue.getPartType();

		if (filePartType instanceof FileURLPartType) {
			File dependencyFile = createFileDependency(getHomeFolder(getMaster()), "DependencyFile");

			((FileURLPartType) filePartType).setTargetFile(dependencyFile);
		} else {
			fail("parttype should be a FileURLPartType");
		}
		htmllongTagValue.append(", <node file>");
	}
	
	/**
	 * Prepare Overview dependency for the target page.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void setupOverviewDependency(Page page) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the overview content tag and set the overview objects
		ContentTag overviewTag = page.getContentTag(OVERVIEW_TAGNAME);

		assertNotNull("Tag does not exist", overviewTag);
		Value overviewValue = overviewTag.getValues().getByKeyname(OVERVIEW_PARTNAME);
		PartType overviewPartType = overviewValue.getPartType();

		if (overviewPartType instanceof OverviewPartType) {
			Overview overview = ((OverviewPartType) overviewPartType).getOverview();

			overview.setSelectionType(Overview.SELECTIONTYPE_PARENT);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_ASC);
			overview.setMaxObjects(0);
			overview.setRecursion(false);
			
			List<OverviewEntry> entries = overview.getOverviewEntries();
			OverviewEntry entry = null;

			switch (dependencySettings) {
			case OVERVIEW_PAGE:
				overview.setObjectType(Page.TYPE_PAGE);
				overviewValue.setValueText(" <node page.name>");
				
				Page overviewPage = createPageDependency(getHomeFolder(getMaster()), "Overview");
				
				entry = t.createObject(OverviewEntry.class);
				entry.setObjectId(overviewPage.getId());
				entries.add(entry);
				
				overviewReferenceObjectId = overviewPage.getId();
				break;

			case OVERVIEW_FILE:
				overview.setObjectType(File.TYPE_FILE);
				overviewValue.setValueText(" <node file.name>");
				
				File overviewFile = createFileDependency(getHomeFolder(getMaster()), "Overview");
				
				entry = t.createObject(OverviewEntry.class);
				entry.setObjectId(overviewFile.getId());
				entries.add(entry);
				
				overviewReferenceObjectId = overviewFile.getId();
				break;

			case OVERVIEW_IMAGE:
				overview.setObjectType(ImageFile.TYPE_IMAGE);
				overviewValue.setValueText(" <node image.name>");
				
				ImageFile overviewImage = createImageDependency(getHomeFolder(getMaster()), "Overview");
				
				entry = t.createObject(OverviewEntry.class);
				entry.setObjectId(overviewImage.getId());
				entries.add(entry);
				
				overviewReferenceObjectId = overviewImage.getId();
				break;

			case OVERVIEW_FOLDER:
				overview.setObjectType(Folder.TYPE_FOLDER);
				overviewValue.setValueText(" <node folder.name>");
				
				Folder overviewFolder = createFolderDependency(getHomeFolder(getMaster()), "Overview");
				
				entry = t.createObject(OverviewEntry.class);
				entry.setObjectId(overviewFolder.getId());
				entries.add(entry);
				
				overviewReferenceObjectId = overviewFolder.getId();
				break;
			}
		}
	}
	
	/**
	 * Prepare PageTag dependency for the target page.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void setupPageTagDependency(Page page, StringBuffer htmllongTagValue) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Template template = t.createObject(Template.class);

		template.setFolderId(getHomeFolder(getMaster()).getId());
		template.setName("TestTemplate-PageTag");
		template.setSource("Test tamplet for page tag");
		template.save();
		t.commit(false);
		
		Page referencePage = t.createObject(Page.class);

		referencePage.setName("TestPage-ReferencePage-master");
		referencePage.setTemplateId(template.getId(), false);
		referencePage.setFolderId(getMaster().getFolder().getId());
		
		ContentTag htmllongContentTag = referencePage.getContent().addContentTag(HTMLLONG_CONSTRUCT_ID);
		tagName = htmllongContentTag.getName();

		htmllongContentTag.setEnabled(true);
		htmllongContentTag.getValues().iterator().next().setValueText("Master content");
		
		referencePage.save();
		referencePage.publish();
		t.commit(false);
		
		Page localizedReferencePage = null;
		ContentTag contentTag = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedReferencePage = (Page) referencePage.copy();
			localizedReferencePage.setChannelInfo(getChannel().getId(), referencePage.getChannelSetId());
			localizedReferencePage.setName("TestPage-ReferencePage-channel");
			
			contentTag = localizedReferencePage.getContentTag(htmllongContentTag.getName());
			contentTag.getValues().iterator().next().setValueText("Channel content");
			
			localizedReferencePage.save();
			localizedReferencePage.publish();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedReferencePage = (Page) referencePage.copy();
			localizedReferencePage.setChannelInfo(getSubChannel().getId(), referencePage.getChannelSetId());
			localizedReferencePage.setName("TestPage-ReferencePage-subchannel");
			
			contentTag = localizedReferencePage.getContentTag(htmllongContentTag.getName());
			contentTag.getValues().iterator().next().setValueText("SubChannel content");
			
			localizedReferencePage.save();
			localizedReferencePage.publish();
			t.commit(false);
			break;

		case BOTH:
			localizedReferencePage = (Page) referencePage.copy();
			localizedReferencePage.setChannelInfo(getChannel().getId(), referencePage.getChannelSetId());
			localizedReferencePage.setName("TestPage-ReferencePage-channel");
			
			contentTag = localizedReferencePage.getContentTag(htmllongContentTag.getName());
			contentTag.getValues().iterator().next().setValueText("Channel content");
			
			localizedReferencePage.save();
			localizedReferencePage.publish();
			t.commit(false);

			localizedReferencePage = (Page) referencePage.copy();
			localizedReferencePage.setChannelInfo(getSubChannel().getId(), referencePage.getChannelSetId());
			localizedReferencePage.setName("TestPage-ReferencePage-subchannel");
			
			contentTag = localizedReferencePage.getContentTag(htmllongContentTag.getName());
			contentTag.getValues().iterator().next().setValueText("SubChannel content");
			
			localizedReferencePage.save();
			localizedReferencePage.publish();
			t.commit(false);
			break;

		default:
			break;
		}
		
		ContentTag pageTagContentTag = page.getContent().addContentTag(PAGE_TAG_CONSTRUCT_ID);

		pageTagContentTag.setName("pagetag");
		pageTagContentTag.setEnabled(true);
		Value pageTagvalue = pageTagContentTag.getValues().iterator().next();
		PageTagPartType partType = (PageTagPartType) pageTagvalue.getPartType();

		partType.setPageTag(referencePage, referencePage.getContentTag(htmllongContentTag.getName()));
		
		htmllongTagValue.append(", <node pagetag>");
	}
	
	/**
	 * Prepare TemplateTag dependency for the target page.
	 * 
	 * @param page
	 * @throws Exception
	 */
	private void setupTemplateTagDependency(Page page, StringBuffer htmllongTagValue) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Template template = t.createObject(Template.class);

		template.setFolderId(getHomeFolder(getMaster()).getId());
		template.setName("TestTemplate-master-PageTag");
		template.setSource("<node htmllong>");
		
		Map<String, TemplateTag> templateTags = template.getTemplateTags();
		
		// Create htmllong template tag
		TemplateTag tag = t.createObject(TemplateTag.class);

		tag.setConstructId(1);
		tag.setEnabled(true);
		tag.setName("htmllong");
		tag.setPublic(true);
		tag.getValues().iterator().next().setValueText("Master content");
		templateTags.put(tag.getName(), tag);
		
		template.save();
		t.commit(false);
		
		Template localizedTemplate = null;
		Tag templateTag = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedTemplate = (Template) template.copy();
			localizedTemplate.setName("TestTemplate-channel-PageTag");
			localizedTemplate.setChannelInfo(getChannel().getId(), template.getChannelSetId());
			
			templateTag = localizedTemplate.getTag("htmllong");
			templateTag.getValues().iterator().next().setValueText("Channel content");
			
			localizedTemplate.save();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedTemplate = (Template) template.copy();
			localizedTemplate.setName("TestTemplate-subchannel-PageTag");
			localizedTemplate.setChannelInfo(getSubChannel().getId(), template.getChannelSetId());
			
			templateTag = localizedTemplate.getTag("htmllong");
			templateTag.getValues().iterator().next().setValueText("SubChannel content");
			
			localizedTemplate.save();
			t.commit(false);
			break;

		case BOTH:
			localizedTemplate = (Template) template.copy();
			localizedTemplate.setName("TestTemplate-channel-PageTag");
			localizedTemplate.setChannelInfo(getChannel().getId(), template.getChannelSetId());
			
			templateTag = localizedTemplate.getTag("htmllong");
			templateTag.getValues().iterator().next().setValueText("Channel content");
			
			localizedTemplate.save();
			t.commit(false);

			localizedTemplate = (Template) template.copy();
			localizedTemplate.setName("TestTemplate-subchannel-PageTag");
			localizedTemplate.setChannelInfo(getSubChannel().getId(), template.getChannelSetId());
			
			templateTag = localizedTemplate.getTag("htmllong");
			templateTag.getValues().iterator().next().setValueText("SubChannel content");
			
			localizedTemplate.save();
			t.commit(false);
			break;

		default:
			break;
		}
		
		ContentTag pageTagContentTag = page.getContent().addContentTag(TEMPLATE_TAG_CONSTRUCT_ID);

		pageTagContentTag.setName("templatetag");
		pageTagContentTag.setEnabled(true);
		Value pageTagvalue = pageTagContentTag.getValues().iterator().next();
		TemplateTagPartType partType = (TemplateTagPartType) pageTagvalue.getPartType();

		partType.setTemplateTag(template, tag);
		
		htmllongTagValue.append(", <node templatetag>");
	}
	
	/**
	 * Create folder dependency and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private Folder createFolderDependency(Folder parentFolder, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Folder folder = t.createObject(Folder.class);

		folder.setName("TestFolder-master-" + uniqueNamePostfix);
		folder.setMotherId(parentFolder.getId());
		folder.save();
		t.commit(false);
		
		Folder localizedFolder = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedFolder = (Folder) folder.copy();
			localizedFolder.setName("TestFolder-channel-" + uniqueNamePostfix);
			localizedFolder.setChannelInfo(getChannel().getId(), folder.getChannelSetId());
			localizedFolder.save();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedFolder = (Folder) folder.copy();
			localizedFolder.setName("TestFolder-subchannel-" + uniqueNamePostfix);
			localizedFolder.setChannelInfo(getSubChannel().getId(), folder.getChannelSetId());
			localizedFolder.save();
			t.commit(false);
			break;

		case BOTH:
			localizedFolder = (Folder) folder.copy();
			localizedFolder.setName("TestFolder-channel-" + uniqueNamePostfix);
			localizedFolder.setChannelInfo(getChannel().getId(), folder.getChannelSetId());
			localizedFolder.save();
			t.commit(false);

			localizedFolder = (Folder) folder.copy();
			localizedFolder.setName("TestFolder-subchannel-" + uniqueNamePostfix);
			localizedFolder.setChannelInfo(getSubChannel().getId(), folder.getChannelSetId());
			localizedFolder.save();
			t.commit(false);
			break;

		default:
			break;
		}
		
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
	private ImageFile createImageDependency(Folder parentFolder, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		ImageFile image = t.createObject(ImageFile.class);

		image.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		image.setName("TestImageFile-master-" + uniqueNamePostfix + ".jpg");
		image.setFolderId(parentFolder.getId());
		image.save();
		t.commit(false);
		
		File localizedImage = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedImage = (ImageFile) image.copy();
			localizedImage.setName("TestImageFile-channel-" + uniqueNamePostfix + ".jpg");
			localizedImage.setChannelInfo(getChannel().getId(), image.getChannelSetId());
			localizedImage.save();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedImage = (ImageFile) image.copy();
			localizedImage.setName("TestImageFile-subchannel-" + uniqueNamePostfix + ".jpg");
			localizedImage.setChannelInfo(getSubChannel().getId(), image.getChannelSetId());
			localizedImage.save();
			t.commit(false);
			break;

		case BOTH:
			localizedImage = (ImageFile) image.copy();
			localizedImage.setName("TestImageFile-channel-" + uniqueNamePostfix + ".jpg");
			localizedImage.setChannelInfo(getChannel().getId(), image.getChannelSetId());
			localizedImage.save();
			t.commit(false);

			localizedImage = (ImageFile) image.copy();
			localizedImage.setName("TestImageFile-subchannel-" + uniqueNamePostfix + ".jpg");
			localizedImage.setChannelInfo(getSubChannel().getId(), image.getChannelSetId());
			localizedImage.save();
			t.commit(false);
			break;

		default:
			break;
		}
		
		return image;
	}
	
	/**
	 * Create file dependency and localize it if specified in dependencyLocalizationSettings.
	 * 
	 * @param parentFolder
	 * @param uniqueNamePostfix
	 * @return
	 * @throws Exception
	 */
	private File createFileDependency(Folder parentFolder, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		File file = t.createObject(File.class);

		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("TestFile-master-" + uniqueNamePostfix + ".txt");
		file.setFolderId(parentFolder.getId());
		file.save();
		t.commit(false);
		
		File localizedFile = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedFile = (File) file.copy();
			localizedFile.setName("TestFile-channel-" + uniqueNamePostfix + ".txt");
			localizedFile.setChannelInfo(getChannel().getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedFile = (File) file.copy();
			localizedFile.setName("TestFile-subchannel-" + uniqueNamePostfix + ".txt");
			localizedFile.setChannelInfo(getSubChannel().getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);
			break;

		case BOTH:
			localizedFile = (File) file.copy();
			localizedFile.setName("TestFile-channel-" + uniqueNamePostfix + ".txt");
			localizedFile.setChannelInfo(getChannel().getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);

			localizedFile = (File) file.copy();
			localizedFile.setName("TestFile-subchannel-" + uniqueNamePostfix + ".txt");
			localizedFile.setChannelInfo(getSubChannel().getId(), file.getChannelSetId());
			localizedFile.save();
			t.commit(false);
			break;

		default:
			break;
		}
		
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
	private Page createPageDependency(Folder parentFolder, String uniqueNamePostfix) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Create the template with two template tags - htmllong and overview
		Template template = t.createObject(Template.class);

		template.setFolderId(getHomeFolder(getMaster()).getId());
		template.setName("TestTemplate-master-" + uniqueNamePostfix);
		template.setSource("Test template for dependency page");
		template.save();
		t.commit(false);
		
		Page page = t.createObject(Page.class);

		page.setName("TestPage-master-" + uniqueNamePostfix);
		page.setTemplateId(template.getId(), false);
		page.setFolderId(parentFolder.getId());
		page.getChannelSetId();
		page.save();
		page.publish();
		t.commit(false);
		
		Page localizedPage = null;

		switch (dependencyLocalizationSettings) {
		case CHANNEL:
			localizedPage = (Page) page.copy();
			localizedPage.setName("TestPage-channel-" + uniqueNamePostfix);
			localizedPage.setFilename(localizedPage.getName() + ".html");
			localizedPage.setChannelInfo(getChannel().getId(), page.getChannelSetId());
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);
			break;

		case SUBCHANNEL:
			localizedPage = (Page) page.copy();
			localizedPage.setName("TestPage-subchannel-" + uniqueNamePostfix);
			localizedPage.setFilename(localizedPage.getName() + ".html");
			localizedPage.setChannelInfo(getSubChannel().getId(), page.getChannelSetId());
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);
			break;

		case BOTH:
			localizedPage = (Page) page.copy();
			localizedPage.setName("TestPage-channel-" + uniqueNamePostfix);
			localizedPage.setFilename(localizedPage.getName() + ".html");
			localizedPage.setChannelInfo(getChannel().getId(), page.getChannelSetId());
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);

			localizedPage = (Page) page.copy();
			localizedPage.setName("TestPage-subchannel-" + uniqueNamePostfix);
			localizedPage.setFilename(localizedPage.getName() + ".html");
			localizedPage.setChannelInfo(getSubChannel().getId(), page.getChannelSetId());
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);
			break;

		default:
			break;
		}
		
		return page;
	}

	/**
	 * Get the home folder of the given node
	 * @param node node
	 * @return home folder (which is not the root folder)
	 * @throws Exception
	 */
	private Folder getHomeFolder(Node node) throws Exception {
		return node.getFolder().getChildFolders().get(0);
	}
}
