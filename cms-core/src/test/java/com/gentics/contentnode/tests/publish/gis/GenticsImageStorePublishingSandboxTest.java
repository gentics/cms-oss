package com.gentics.contentnode.tests.publish.gis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.TestAppender;
import com.gentics.lib.log.NodeLogger;

public class GenticsImageStorePublishingSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext(false);

	private static final String HOST_A = "www.hosta.tdl";
	private static final int PAGE_ID_IN_NODE_A = 65;

	private static final String HOST_B = "www.hostb.tdl";
	private static final int PAGE_ID_IN_NODE_B = 64;

	private static final String HOST_C = "www.hostc.tdl";
	private static final int PAGE_ID_IN_NODE_C = 63;

	private static final String HOST_D = "www.hostd.tdl";
	private static final int PAGE_ID_IN_NODE_D = 66;

	private static final String PUBDIR_NAME = "/Content.Node/";
	private static final String IMAGESTORE_PATH_WITHIN_NODE = "/GenticsImageStore/300/auto/prop/Content.Node/";

	private static final TestAppender testAppender = new TestAppender();

	private PublishInfo invokePublish() throws Exception {
		ContentNodeTestContext context = testContext.getContext();
		PublishInfo info = context.publish(false);
		return info;
	}

	/**
	 * Publishes the given page and clears the publish table before doing so
	 *
	 * @param pageId
	 * @throws Exception
	 */
	private void publishPage(int pageId) throws Exception {
		DBUtils.executeUpdate("DELETE FROM imagestoreimage", null);
		DBUtils.executeUpdate("DELETE FROM publish", null);
		DBUtils.executeUpdate("DELETE FROM publishqueue", null);

		testContext.markAllPagesAsPublished();
		Page page = testContext.getContext().getTransaction().getObject(Page.class, pageId, true);
		page.setDescription("lulu");
		page.save(true);
		page.publish();
		testContext.getContext().getTransaction().commit();
		PublishInfo info = invokePublish();

		// Publish should be successful
		assertEquals("The publish run should terminate with a success code. Messages: " + info.getMessages(), PublishInfo.RETURN_CODE_SUCCESS,
				info.getReturnCode());
	}

	/**
	 * This method contains some asserts on the original files that should be published
	 * @throws NodeException
	 */
	public void checkNormalFilePublish() throws NodeException {
		// Check that the original files exist as expected
		File originalImageInNodeA = new File(testContext.getContext().getPubDir(), HOST_A + PUBDIR_NAME + "node_a.jpeg");

		assertTrue("The original image should be published. {" + originalImageInNodeA + "}", originalImageInNodeA.exists());

		File originalImageInNodeB = new File(testContext.getContext().getPubDir(), HOST_B + PUBDIR_NAME + "node_b.jpg");

		assertTrue("The original image should be published. {" + originalImageInNodeB + "}", originalImageInNodeB.exists());

		File originalImageInNodeC = new File(testContext.getContext().getPubDir(), HOST_C + PUBDIR_NAME + "node_c.jpg");

		assertTrue("The original image should be published. {" + originalImageInNodeC + "}", originalImageInNodeC.exists());

		File originalImageInNodeD = new File(testContext.getContext().getPubDir(), HOST_D + PUBDIR_NAME + "node_d.jpg");

		assertTrue("The original image  {" + originalImageInNodeD + "} should not exist because node d is not publishing in the filesystem.",
				!originalImageInNodeD.exists());

	}

	@BeforeClass
	public static void setupOnce() {
		NodeLogger logger = NodeLogger.getNodeLogger(FolderFactory.class.getCanonicalName() + "$FactoryFolder");

		logger.removeAllAppenders();
		logger.addAppender(testAppender);
	}

	@Before
	public void setup() throws IOException, NodeException {
		FileUtils.deleteDirectory(testContext.getContext().getPubDir());
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("tag_image_resizer", true);

		testAppender.reset();
	}

	@After
	public void tearDown() throws Exception {
		FileUtils.deleteDirectory(testContext.getContext().getPubDir());
	}

	/**
	 * This test publishes a page in node A which is a node that publishes into the filesystem. The page contains references to Node B, C, D
	 *
	 * @throws Exception
	 */
	@Test
	public void testGISPublishNodeA() throws Exception {
		publishPage(PAGE_ID_IN_NODE_A);
		checkNormalFilePublish();
		boolean[] assertConfig = { true, true, true, false };

		assertResizedImages(assertConfig);
		assertThat(testAppender.getMessages())
			.as("Log messages must be empty")
			.isEmpty();
	}

	/**
	 * This test publishes a page in node B which is a node that publishes into the filesystem
	 *
	 * @throws Exception
	 */
	@Test
	public void testGISPublishNodeB() throws Exception {
		publishPage(PAGE_ID_IN_NODE_B);
		checkNormalFilePublish();
		boolean[] assertConfig = { true, true, true, false };

		assertResizedImages(assertConfig);
		assertThat(testAppender.getMessages())
			.as("Log messages must be empty")
			.isEmpty();
	}

	/**
	 * This test publishes a page in a node that only publishes into a CR
	 *
	 * @throws Exception
	 */
	@Test
	public void testGISPublishNodeD() throws Exception {
		publishPage(PAGE_ID_IN_NODE_D);
		checkNormalFilePublish();
		// GIS does not work for nodes that publish only in CR
		boolean[] assertConfig = { false, false, false, false };
		assertResizedImages(assertConfig);
		assertThat(testAppender.getMessages())
			.as("Log messages must be empty")
			.isEmpty();
	}

	/**
	 * Check that the resized images exist or were omitted due to node specific settings
	 *
	 * @param imagesPerNode
	 * @throws Exception
	 */
	public void assertResizedImages(boolean[] imagesPerNode) throws Exception {

		File resizedImageInNodeA = new File(testContext.getContext().getPubDir(), HOST_A + IMAGESTORE_PATH_WITHIN_NODE + "node_a.jpeg");

		assertEquals("The resized image {" + resizedImageInNodeA + "} was not written into the FS.", imagesPerNode[0], resizedImageInNodeA.exists());

		File resizedImageInNodeB = new File(testContext.getContext().getPubDir(), HOST_B + IMAGESTORE_PATH_WITHIN_NODE + "node_b.jpg");

		assertEquals("The resized image {" + resizedImageInNodeB + "} was not written into the FS.", imagesPerNode[1], resizedImageInNodeB.exists());

		File resizedImageInNodeC = new File(testContext.getContext().getPubDir(), HOST_C + IMAGESTORE_PATH_WITHIN_NODE + "node_c.jpg");

		assertEquals("The resized image {" + resizedImageInNodeC + "} was not written into the FS.", imagesPerNode[2], resizedImageInNodeC.exists());

		File resizedImageInNodeD = new File(testContext.getContext().getPubDir(), HOST_D + IMAGESTORE_PATH_WITHIN_NODE + "node_d.jpg");

		assertEquals("The resized image {" + resizedImageInNodeD + "} was not written into the FS.", imagesPerNode[3], resizedImageInNodeD.exists());

	}
}
