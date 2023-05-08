package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.ContentRepositoryResource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for resumable publish process. When the feature
 * {@link Feature#RESUMABLE_PUBLISH_PROCESS} is set, and a publish process
 * fails, all objects, that were completely published, will be removed from the
 * publish queue.
 * Fully published means:
 * <ol>
 * <li>The page has been written into the publish table (and the transaction committed)</li>
 * <li>The object has been written into the contentmap (with instant publish) if applicable</li>
 * </ol>
 */
@RunWith(value = Parameterized.class)
public class ResumablePublishProcessTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Rule
	public ContentRepositoryResource testCR = new ContentRepositoryResource();

	/**
	 * ID of the contentrepository to be used
	 */
	public final static int CR_ID = 1;

	/**
	 * Number of objects, that can be published before the publish process will fail
	 */
	public final static int NUM_PUBLISH_OBJECTS = 5;

	/**
	 * true for multithreaded
	 */
	protected boolean multithreaded;

	/**
	 * type of the tested objects
	 */
	protected TYPE type;

	/**
	 * true for resumable publish process
	 */
	protected boolean resumable;

	/**
	 * true if instant publishing is used
	 */
	protected boolean instantPublishing;

	/**
	 * true if the publish process shall have an error
	 */
	protected boolean publishError;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: publishError {0}, multithreaded {1}, type {2}, resumable_publish_process {3}, instant_publishing {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean publishError : Arrays.asList(true, false)) {
			for (boolean multithreaded : Arrays.asList(true, false)) {
				for (TYPE type : TYPE.values()) {
					for (boolean resumable : Arrays.asList(true, false)) {
						for (boolean instantPublishing : Arrays.asList(true, false)) {
							data.add(new Object[] {publishError, multithreaded, type, resumable, instantPublishing});
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param publishError true if the publish process shall have an error, false if not
	 * @param multithreaded true for multithreaded
	 * @param type type of the tested objects
	 * @param resumable true for resumable publish processes
	 * @param instantPublishing true for instant publishing
	 */
	public ResumablePublishProcessTest(boolean publishError, boolean multithreaded, TYPE type, boolean resumable, boolean instantPublishing) {
		this.publishError = publishError;
		this.multithreaded = multithreaded;
		this.type = type;
		this.resumable = resumable;
		this.instantPublishing = instantPublishing;
	}

	@Before
	public void setUp() throws Exception {
		testContext.updateCRReference(testCR);
		DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] {1});
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, multithreaded);
		prefs.setFeature(Feature.RESUMABLE_PUBLISH_PROCESS, resumable);
		prefs.setFeature("instant_cr_publishing", true);
	}

	/**
	 * Test the publish process
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<NodeObject> dirtedObjects = new ArrayList<NodeObject>();

		// create a test node
		Node testNode = ContentNodeTestDataUtils.createNode("Test Node", "testnode", "/", "/", false, false);
		int nodeId = ObjectTransformer.getInt(testNode.getId(), 0);

		Folder root = testNode.getFolder();
		dirtedObjects.add(root);

		// contentrepository with failing publish handler
		ContentRepository cr = t.getObject(ContentRepository.class, CR_ID, true);
		assertNotNull("Could not find contentrepository", cr);
		cr.setInstantPublishing(instantPublishing);
		cr.save();
		t.commit(false);

		DBUtils.executeUpdate("INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) VALUES (?, ?, ?, ?)", new Object[] { "Test Publish Handler",
				cr.getId(), TestPublishHandler.class.getName() , "publishError:" + publishError});
		t.commit(false);

		testNode = t.getObject(Node.class, testNode.getId(), true);
		testNode.setContentrepositoryId(cr.getId());
		testNode.setPublishContentmap(true);
		testNode.setPublishFilesystem(false);
		testNode.setPublishDisabled(false);
		testNode.save();
		t.commit(false);

		// disable instant publishing while creating the test objects
		// we don't want them to be published before the publish process
		t.setInstantPublishingEnabled(false);

		// create test objects
		switch (type) {
		case FOLDER:
			for (int i = 1; i <= 5; i++) {
				dirtedObjects.add(createFolder(root, "Folder " + i));
			}
			break;
		case FILE:
			for (int i = 1; i <= 5; i++) {
				dirtedObjects.add(createFile(root, "file-" + i + ".bin"));
			}
			break;
		case PAGE:
			Template template = createTemplate(root, "Template", "Page [<node page.name>]");
			for (int i = 1; i <= 5; i++) {
				dirtedObjects.add(createPage(root, template, "Page " + i));
			}
			break;
		}

		// enable instant publishing
		t.setInstantPublishingEnabled(true);

		// get the contentmap
		ContentMap contentMap = testNode.getContentMap();
		assertNotNull("Could not get the contentmap for the node", contentMap);

		// reset the publish handler
		TestPublishHandler.reset();
		// run the publish process and expect it to fail
		assertEquals("Check publish status", publishError ? PublishInfo.RETURN_CODE_ERROR : PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext()
				.publish(false, true, System.currentTimeMillis(), !publishError).getReturnCode());

		// check publish queue
		assertFalse("Some objects must have been published", TestPublishHandler.publishedObjects.isEmpty());

		for (NodeObject object : dirtedObjects) {
			int objType = ObjectTransformer.getInt(object.getTType(), 0);
			int objId = ObjectTransformer.getInt(object.getId(), 0);
			String contentId = objType + "." + objId;

			// check whether object should have been published
			boolean published = TestPublishHandler.publishedObjects.contains(contentId);

			// an object is expected in the cr if it was handled by the publish process AND we have instant publishing
			assertPublishCR(contentMap.getWritableDatasource(), object, published && (!publishError || instantPublishing));
			if (object instanceof Page && published) {
				// objects that have been completely handled by the publish process are expected to be in the publish table
				assertPublishTable(object, nodeId, "Page [" + ((Page) object).getName() + "]", published);
			}

			// the publishqueue entry for the object must exist, if the publish process is expected to fail AND at least one of the following is true:
			// 1. the object was not published (has to be published in the next publish run)
			// 2. the publish process is not resumable
			// 3. the cr does not have instant publishing, so the object cannot have been fully published
			boolean expectPublishQueueEntries = publishError && (!published || !resumable || !instantPublishing);
			assertPublishQueueEntry(object, nodeId, expectPublishQueueEntries);
		}
	}

	/**
	 * Create a new folder
	 * @param mother mother folder
	 * @param name folder name
	 * @return the new folder
	 * @throws NodeException
	 */
	protected Folder createFolder(Folder mother, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder newFolder = t.createObject(Folder.class);
		newFolder.setName(name);
		newFolder.setMotherId(mother.getId());
		newFolder.setPublishDir("/");
		newFolder.save();
		t.commit(false);
		return t.getObject(Folder.class, newFolder.getId());
	}

	/**
	 * Create a new file
	 * @param folder file folder
	 * @param name file name
	 * @return the new file
	 * @throws NodeException
	 */
	protected File createFile(Folder folder, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		File newFile = t.createObject(File.class);
		newFile.setName(name);
		newFile.setFileStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));
		newFile.setFolderId(folder.getId());
		newFile.save();
		t.commit(false);
		return t.getObject(File.class, newFile.getId());
	}

	/**
	 * Create a new template
	 * @param folder folder
	 * @param name template name
	 * @param source template source
	 * @return new template
	 * @throws NodeException
	 */
	protected Template createTemplate(Folder folder, String name, String source) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template newTemplate = t.createObject(Template.class);
		newTemplate.setFolderId(folder.getId());
		newTemplate.setName(name);
		newTemplate.setSource(source);
		newTemplate.save();
		t.commit(false);
		return t.getObject(Template.class, newTemplate.getId());
	}

	/**
	 * Create a new page and publish it
	 * @param folder folder
	 * @param template template
	 * @param name page name
	 * @return new page
	 * @throws NodeException
	 */
	protected Page createPage(Folder folder, Template template, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page newPage = t.createObject(Page.class);
		newPage.setFolderId(folder.getId());
		newPage.setName(name);
		newPage.setTemplateId(template.getId());
		newPage.save();
		t.commit(false);
		newPage.publish();
		t.commit(false);
		return t.getObject(Page.class, newPage.getId());
	}

	/**
	 * Check whether the object was published into the datasource
	 * @param ds ds
	 * @param object object
	 * @param expected true if the object is expected to be published, false if not
	 * @throws Exception
	 */
	protected void assertPublishCR(Datasource ds, NodeObject object, boolean expected) throws Exception {
		String contentId = object.getTType() + "." + object.getId();
		Resolvable contentObject = PortalConnectorFactory.getContentObject(contentId, ds);
		if (expected) {
			assertNotNull(object + " should have been published into the cr", contentObject);
		} else {
			assertNull(object + " should not have been published into cr", contentObject);
		}
	}

	/**
	 * Check the publish table entry for the given page
	 * @param object object (which should be a page)
	 * @param nodeId node id
	 * @param content expected content (if expected is "true")
	 * @param expected true if an entry is expected to exist in the publish table
	 * @throws Exception
	 */
	protected void assertPublishTable(final NodeObject object, final int nodeId, String content, boolean expected) throws Exception {
		final List<String> publishedContents = new ArrayList<String>();
		DBUtils.executeStatement("SELECT * FROM publish WHERE page_id = ? AND node_id = ? AND active = 1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, object.getId());
				stmt.setInt(2, nodeId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					publishedContents.add(rs.getString("source"));
				}
			}
		});

		if (publishedContents.size() > 1) {
			fail("Found too many ("+publishedContents.size()+") publish table entries for " + object);
		}
		if (expected) {
			if (publishedContents.isEmpty()) {
				fail("Did not find expected publish table entry for " + object);
			} else {
				assertEquals("Check publish table entry for " + object, content, publishedContents.get(0));
			}
		} else {
			if (!publishedContents.isEmpty()) {
				fail("Found unexpected publish table entry for " + object);
			}
		}
	}

	/**
	 * Check the number of publishqueue entries for the given object
	 * @param object object
	 * @param nodeId node id
	 * @param expected TODO
	 * @throws Exception
	 */
	protected void assertPublishQueueEntry(final NodeObject object, final int nodeId, boolean expected) throws Exception {
		final List<Integer> result = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT count(*) c FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, object.getTType());
				stmt.setObject(2, object.getId());
				stmt.setInt(3, nodeId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					result.add(rs.getInt("c"));
				}
			}
		});

		assertTrue("Could not count publishqueue entries for " + object, result.size() == 1);
		if (expected) {
			assertTrue("Did not find expected publishqueue entries for " + object, result.get(0) > 0);
		} else {
			assertFalse("Found unexpected publishqueue entries for " + object, result.get(0) > 0);
		}
	}

	public static enum TYPE {
		FOLDER,
		FILE,
		PAGE
	}

	/**
	 * Test publish handler
	 */
	public static class TestPublishHandler implements CnMapPublishHandler {
		/**
		 * List of successful published objects
		 */
		protected static List<String> publishedObjects = new ArrayList<String>();

		/**
		 * Publish handler counter
		 */
		protected static int counter = 0;

		/**
		 * True if the publish process shall have an error, false if not
		 */
		protected boolean publishError = true;

		@Override
		public void init(@SuppressWarnings("rawtypes") Map parameters) throws CnMapPublishException {
			publishError = ObjectTransformer.getBoolean(parameters.get("publishError"), publishError);
		}

		@Override
		public void open(long timestamp) throws CnMapPublishException {
		}

		@Override
		public void createObject(Resolvable object) throws CnMapPublishException {
			allowPublishing(object);
		}

		@Override
		public void updateObject(Resolvable object) throws CnMapPublishException {
			allowPublishing(object);
		}

		@Override
		public void deleteObject(Resolvable object) throws CnMapPublishException {
		}

		@Override
		public void commit() {
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() {
		}

		@Override
		public void destroy() {
		}

		/**
		 * Either allow publishing of the object (and store its contentid) or fail (after a specified number of objects)
		 * @param object object to be published
		 * @throws NodeException
		 */
		protected synchronized void allowPublishing(Resolvable object) throws CnMapPublishException {
			counter++;
			if (publishError && counter > NUM_PUBLISH_OBJECTS) {
				throw new CnMapPublishException("The publish process deliberately fails at object #" + counter);
			} else {
				publishedObjects.add(ObjectTransformer.getString(object.get("contentid"), null));
			}
		}

		/**
		 * Reset the internal counter and the stored published objects
		 */
		protected static void reset() {
			counter = 0;
			publishedObjects.clear();
		}
	}
}
