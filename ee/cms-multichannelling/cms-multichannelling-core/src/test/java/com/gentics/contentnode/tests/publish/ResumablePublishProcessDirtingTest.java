package com.gentics.contentnode.tests.publish;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Test case for publishing with resumable publish process, when there are "duplicate" PublishQueue entries for a channel:
 * There is a possibility, that the PublishQueue contains entries for different objects of the same channelset for a channel.
 * This must not result in handling the same object (the correct channelset variant) for the channel twice, because the publish
 * process would fail.
 */
public class ResumablePublishProcessDirtingTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Setup the environment by activating the necessary features and adding some configuration
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] {1});
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, true);
		prefs.setFeature("instant_cr_publishing", true);
		prefs.setFeature(Feature.RESUMABLE_PUBLISH_PROCESS, true);
		prefs.setFeature(Feature.MULTICHANNELLING, true);
		prefs.setFeature(Feature.ATTRIBUTE_DIRTING, true);
		prefs.setFeature("mccr", true);
		prefs.setProperty("multithreaded_publishing.batchsize", "10");
	}

	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test publish process with duplicate entries for a folder in a channel (one for the master, one for the localized copy)
	 * @throws Exception
	 */
	@Test
	public void testDuplicateFolderEntry() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.CONTENTREPOSITORY, true, false);
		ContentRepository cr = master.getContentRepository();
		ContentNodeTestDataUtils.addTagmapEntry(cr, Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "name", "name", null, false, false, false, -1, null, null);
		cr = t.getObject(ContentRepository.class, cr.getId(), true);
		cr.setInstantPublishing(true);
		cr.save();
		t.commit(false);

		Node channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
		channel = t.getObject(Node.class, channel.getId(), true);
		channel.setPublishContentmap(true);
		channel.setContentrepositoryId(cr.getId());
		channel.save();
		t.commit(false);

		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(master.getFolder().getId());
		folder.setName("Folder");
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);

		testContext.publish(false);
		t = testContext.startSystemUserTransaction();

		// change the folder
		folder = t.getObject(Folder.class, folder.getId(), true);
		folder.setName("Folder Modified");
		folder.save();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(Folder.class, master, folder);
		ContentNodeTestUtils.assertDirtedObjects(Folder.class, channel, folder);

		// localized the folder
		Folder localizedFolder = (Folder)folder.copy();
		localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(Folder.class, master, folder);
		ContentNodeTestUtils.assertDirtedObjects(Folder.class, channel, localizedFolder);

		// now we add dirtqueue entries for the master objects and the channel
		DBUtils.executeInsert(
				"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) VALUES (?, ?, ?, ?, ?)",
				new Object[] { Folder.TYPE_FOLDER, folder.getId(),
						Action.DEPENDENCY.toString(), channel.getId(),
						t.getUnixTimestamp() });
		t.commit(false);

		// publish again
		testContext.publish(false);
	}

	/**
	 * Test publish process with duplicate entries for a file in a channel (one for the master, one for the localized copy)
	 * @throws Exception
	 */
	@Test
	public void testDuplicateFileEntry() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.CONTENTREPOSITORY, true, false);
		ContentRepository cr = master.getContentRepository();
		ContentNodeTestDataUtils.addTagmapEntry(cr, ContentFile.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "name", "name", null, false, false, false, -1, null, null);
		cr = t.getObject(ContentRepository.class, cr.getId(), true);
		cr.setInstantPublishing(true);
		cr.save();
		t.commit(false);

		Node channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
		channel = t.getObject(Node.class, channel.getId(), true);
		channel.setPublishContentmap(true);
		channel.setContentrepositoryId(cr.getId());
		channel.save();
		t.commit(false);

		File file = t.createObject(File.class);
		file.setFolderId(master.getFolder().getId());
		file.setName("file.txt");
		file.setFileStream(new ByteArrayInputStream("file contents".getBytes()));
		file.save();
		t.commit(false);

		testContext.publish(false);
		t = testContext.startSystemUserTransaction();

		// change the file
		file = t.getObject(File.class, file.getId(), true);
		file.setName("file_modified.txt");
		file.save();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(File.class, master, file);
		ContentNodeTestUtils.assertDirtedObjects(File.class, channel, file);

		// localized the file
		File localizedFile = (File)file.copy();
		localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
		localizedFile.save();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(File.class, master, file);
		ContentNodeTestUtils.assertDirtedObjects(File.class, channel, localizedFile);

		// now we add dirtqueue entries for the master objects and the channel
		DBUtils.executeInsert(
				"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) VALUES (?, ?, ?, ?, ?)",
				new Object[] { File.TYPE_FILE, file.getId(),
						Action.DEPENDENCY.toString(), channel.getId(),
						t.getUnixTimestamp() });
		t.commit(false);

		// publish again
		testContext.publish(false);
	}

	/**
	 * Test publish process with duplicate entries for a page in a channel (one for the master, one for the localized copy)
	 * @throws Exception
	 */
	@Test
	public void testDuplicatePageEntry() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.CONTENTREPOSITORY, true, false);
		ContentRepository cr = master.getContentRepository();
		ContentNodeTestDataUtils.addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "name", "name", null, false, false, false, -1, null, null);
		cr = t.getObject(ContentRepository.class, cr.getId(), true);
		cr.setInstantPublishing(true);
		cr.save();
		t.commit(false);

		Node channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
		channel = t.getObject(Node.class, channel.getId(), true);
		channel.setPublishContentmap(true);
		channel.setContentrepositoryId(cr.getId());
		channel.save();
		t.commit(false);

		Template template = t.createObject(Template.class);
		template.setFolderId(master.getFolder().getId());
		template.setMlId(1);
		template.setName("Template");
		template.setSource("bla");
		template.save();
		t.commit(false);

		Page page = t.createObject(Page.class);
		page.setFolderId(master.getFolder().getId());
		page.setTemplateId(template.getId());
		page.setName("Page");
		page.save();
		page.publish();
		t.commit(false);

		testContext.publish(false);
		t = testContext.startSystemUserTransaction();

		// change the page
		page = t.getObject(Page.class, page.getId(), true);
		page.setName("Page Modified");
		page.save();
		page.publish();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(Page.class, master, page);
		ContentNodeTestUtils.assertDirtedObjects(Page.class, channel, page);

		// localize the page
		Page localizedPage = (Page)page.copy();
		localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
		localizedPage.save();
		localizedPage.publish();
		t.commit(false);

		// wait for the dirtqueue worker
		testContext.waitForDirtqueueWorker();

		// check dirted objects
		ContentNodeTestUtils.assertDirtedObjects(Page.class, master, page);
		ContentNodeTestUtils.assertDirtedObjects(Page.class, channel, localizedPage);

		// now we add dirtqueue entries for the master objects and the channel
		DBUtils.executeInsert(
				"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) VALUES (?, ?, ?, ?, ?)",
				new Object[] { Page.TYPE_PAGE, page.getId(),
						Action.DEPENDENCY.toString(), channel.getId(),
						t.getUnixTimestamp() });
		t.commit(false);

		// publish again
		testContext.publish(false);
	}
}
