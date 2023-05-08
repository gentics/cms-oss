package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for the various possibilities of synchronizing deleted objects with the CR
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MCCR})
public class SyncDeletedObjectsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static int timeSequence = 0;

	protected boolean mccr;

	protected boolean differential;

	private Node node;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: mccr {0}, differential {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean mccr : Arrays.asList(true, false)) {
			for (boolean differential : Arrays.asList(true, false)) {
				data.add(new Object[] {mccr, differential});
			}
		}
		return data;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		// activate the feature multichannelling_contentrepository
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("multichannelling_contentrepository", true);
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("multichannelling", true);
	}

	/**
	 * Get the next value of the time sequence
	 * @return next timestamp
	 */
	protected static int getNextTimestamp() {
		return ++timeSequence;
	}

	/**
	 * Create a test instance
	 * @param mccr true iff mccr datasource
	 * @param differential true iff synchronizing differentially
	 */
	public SyncDeletedObjectsTest(boolean mccr, boolean differential) {
		this.mccr = mccr;
		this.differential = differential;
	}

	/**
	 * Setup a node publishing into a contentrepository (according to the test settings)
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		node = ContentNodeTestDataUtils.createNode(TestEnvironment.getRandomHash(10), TestEnvironment.getRandomHash(10), PublishTarget.CONTENTREPOSITORY,
				mccr, false);

		// Add node_id as tagmap entry for all object types
		ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER,
				GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE,
				GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id", null, false, false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), ContentFile.TYPE_FILE,
				GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id", null, false, false, true, -1, null, null);

		DBUtils.executeUpdate("UPDATE contentrepository SET diffdelete = ? WHERE id = ?", new Object[] {differential ? 1 : 0, node.getContentrepositoryId()});
		t.commit(false);
	}

	/**
	 * Test the synchronization of a deleted folder
	 * @throws Exception
	 */
	@Test
	public void testSyncDeletedFolder() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		Folder folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createNodeObject(Folder.TYPE_FOLDER, node.getFolder(), "Folder").getId());
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(folder, true);

		t = testContext.startTransaction(getNextTimestamp());
		t.getObject(Folder.class, folder.getId(), true).delete();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(folder, false);
	}

	/**
	 * Test the synchronization of a deleted page
	 * @throws Exception
	 */
	@Test
	public void testSyncDeletedPage() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		Page page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, true);

		t = testContext.startTransaction(getNextTimestamp());
		t.getObject(Page.class, page.getId(), true).delete();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, false);
	}

	/**
	 * Test the synchronization of an offline page
	 * @throws Exception
	 */
	@Test
	public void testSyncOfflinePage() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		Page page = t.getObject(Page.class, ContentNodeTestDataUtils.createNodeObject(Page.TYPE_PAGE, node.getFolder(), "Page").getId());
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, true);

		t = testContext.startTransaction(getNextTimestamp());
		t.getObject(Page.class, page.getId(), true).takeOffline();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, false);
	}

	/**
	 * Test the synchronization of a deleted file
	 * @throws Exception
	 */
	@Test
	public void testSyncDeletedFile() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		File file = t.getObject(File.class, ContentNodeTestDataUtils.createNodeObject(File.TYPE_FILE, node.getFolder(), "testfile.txt").getId());
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(file, true);

		t = testContext.startTransaction(getNextTimestamp());
		t.getObject(File.class, file.getId(), true).delete();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(file, false);
	}

	/**
	 * Test the synchronization of a deleted image
	 * @throws Exception
	 */
	@Test
	public void testSyncDeletedImage() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		File image = t.getObject(ImageFile.class, ContentNodeTestDataUtils.createNodeObject(ImageFile.TYPE_IMAGE, node.getFolder(), "blume.jpg").getId());
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(image, true);

		t = testContext.startTransaction(getNextTimestamp());
		t.getObject(File.class, image.getId(), true).delete();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(image, false);
	}

	/**
	 * Test the synchronization of folders when folder publishing is turned off
	 * @throws Exception
	 */
	@Test
	public void testSyncTurnOffFolderPublishing() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		Folder folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createNodeObject(
				Folder.TYPE_FOLDER, node.getFolder(), "Folder").getId());
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(folder, true);

		t = testContext.startTransaction(getNextTimestamp());
		Node nodeEditable = t.getObject(Node.class, node.getId(), true);
		nodeEditable.setPublishContentMapFolders(false);
		nodeEditable.save();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(folder, false);
	}

	/**
	 * Test the synchronization of pages when folder publishing is turned off
	 * @throws Exception
	 */
	@Test
	public void testSyncTurnOffPagePublishing() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		Page page = ContentNodeTestDataUtils.createTemplateAndPage(
				node.getFolder(), "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, true);

		t = testContext.startTransaction(getNextTimestamp());
		Node nodeEditable = t.getObject(Node.class, node.getId(), true);
		nodeEditable.setPublishContentMapPages(false);
		nodeEditable.save();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(page, false);
	}

	/**
	 * Test the synchronization of files when folder publishing is turned off
	 * @throws Exception
	 */
	@Test
	public void testSyncTurnOffFilePublishing() throws Exception {
		Transaction t = testContext.startTransaction(getNextTimestamp());
		File file = t.getObject(File.class, ContentNodeTestDataUtils.createNodeObject(
				File.TYPE_FILE, node.getFolder(), "testfile.txt").getId());
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(file, true);

		t = testContext.startTransaction(getNextTimestamp());
		Node nodeEditable = t.getObject(Node.class, node.getId(), true);
		nodeEditable.setPublishContentMapFiles(false);
		nodeEditable.save();
		t.commit(false);

		testContext.publish(getNextTimestamp());
		assertObjectPublished(file, false);
	}

	/**
	 * Assert that the object was/was not published into the node's CR
	 * @param object object
	 * @param expected true iff the object is expected to be published
	 * @throws Exception
	 */
	protected void assertObjectPublished(NodeObject object, boolean expected) throws Exception {
		WriteableDatasource ds = node.getContentMap().getWritableDatasource();
		int type = ObjectTransformer.getInt(object.getTType(), 0);
		if (type == ImageFile.TYPE_IMAGE) {
			type = File.TYPE_FILE;
		}
		Resolvable publishedObject = PortalConnectorFactory.getContentObject(type + "." + object.getId(), ds);
		assertEquals("Check whether " + object + " was published", expected, publishedObject != null);
	}
}
