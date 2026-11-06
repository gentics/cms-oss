package com.gentics.contentnode.tests.publish;

import com.gentics.contentnode.testutils.GCNFeature;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for publishing a file that is deleted during the publish process.
 * Deleting the file is done in the instance of {@link DeleteFilePublishHandler} while publishing the folder (which is done before the files are published)
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR })
public class PublishDeletedFileTest {
	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: publish target {0}, multithreaded {1}, mccr {2}, instant {3}, fs attr {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testData = new ArrayList<Object[]>();
		for (PublishTarget target : Arrays.asList(PublishTarget.CONTENTREPOSITORY, PublishTarget.BOTH)) {
			for (boolean multithreaded : Arrays.asList(true, false)) {
				for (boolean mccr : Arrays.asList(true, false)) {
					for (boolean instant : Arrays.asList(true, false)) {
						for (boolean fsAttr : Arrays.asList(true, false)) {
							testData.add(new Object[] {target, multithreaded, mccr, instant, fsAttr});
						}
					}
				}
			}
		}
		return testData;
	}

	/**
	 * Test context
	 */
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Publish Target
	 */
	protected PublishTarget target;

	/**
	 * Multithreaded publishing
	 */
	protected boolean multithreaded;

	/**
	 * Publishing into a mccr
	 */
	protected boolean mccr;

	/**
	 * Instant publishing
	 */
	protected boolean instant;

	/**
	 * Write binary content into filesystem
	 */
	protected boolean fsAttr;

	/**
	 * Node to test in
	 */
	protected Node node;

	/**
	 * Create a test instance
	 * @param target publish target
	 * @param multithreaded multithreaded publishing
	 * @param mccr mccr content repository
	 * @param instant instant publishing
	 * @param fsAttr filesystem attribute
	 */
	public PublishDeletedFileTest(PublishTarget target, boolean multithreaded, boolean mccr, boolean instant, boolean fsAttr) {
		this.target = target;
		this.multithreaded = multithreaded;
		this.mccr = mccr;
		this.instant = instant;
		this.fsAttr = fsAttr;
	}

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING, true);
		prefs.setFeature(Feature.CR_FILESYSTEM_ATTRIBUTES, true);
		prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, multithreaded);

		Transaction t = TransactionManager.getCurrentTransaction();
		node = ContentNodeTestDataUtils.createNode(testContext.getDBSQLUtils().getTestDatabase().getDBName(), "Test Node",
				target, mccr, false);

		ContentRepository cr = t.getObject(ContentRepository.class, node.getContentrepositoryId(), true);
		TagmapEntry binarycontent = t.createObject(TagmapEntry.class);
		binarycontent.setTagname("binarycontent");
		binarycontent.setMapname("binarycontent");
		binarycontent.setAttributeTypeId(6);
		binarycontent.setFilesystem(fsAttr);
		binarycontent.setObject(10008);
		cr.getEntries().add(binarycontent);
		cr.setBasepath(new java.io.File(System.getProperty("java.io.tmpdir"), "cr").getAbsolutePath());
		cr.setInstantPublishing(instant);
		cr.save();

		File file = t.createObject(File.class);
		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("textfile.txt");
		file.setFolderId(node.getFolder().getId());
		file.save();
		t.commit(false);

		testContext.getDBSQLUtils().executeQueryManipulation(
				"INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) VALUES ('TestHandler', " + node.getContentrepositoryId()
						+ ", '" + DeleteFilePublishHandler.class.getName() + "', 'id=" + file.getId() + "')");
	}

	/**
	 * Tear down resources
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Start a publish process that publishes a folder and a file.
	 * In the publish handler for the folder, delete the file
	 * Check that the publish process succeeds
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		testContext.publish((int)(System.currentTimeMillis()/1000L));
	}
}
