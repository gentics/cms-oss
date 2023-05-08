package com.gentics.contentnode.tests.multichannelling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for creation of objects in multichannelling environments
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class MCObjectCreationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Channel structure
	 */
	private static ChannelStructure channelStructure;

	@BeforeClass
	public static void setupOnce() throws Exception {
		channelStructure = new ChannelStructure();
	}

	@Parameters(name = "{index}: folder {0}, localization {1}, object {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (ChannelStructure.Channel folderChannel : ChannelStructure.Channel.values()) {
			if (!folderChannel.isMainTree()) {
				continue;
			}
			for (ChannelStructure.Channel localizationChannel : ChannelStructure.Channel.values()) {
				if (localizationChannel != ChannelStructure.Channel.NONE && !localizationChannel.channelOf(folderChannel)) {
					continue;
				}
				for (ChannelStructure.Channel objectChannel : ChannelStructure.Channel.values()) {
					if (objectChannel == ChannelStructure.Channel.NONE) {
						continue;
					}
					if (objectChannel != folderChannel && !objectChannel.channelOf(folderChannel)) {
						continue;
					}
					data.add(new Object[] {folderChannel, localizationChannel, objectChannel});
				}
			}
		}

		return data;
	}

	/**
	 * Folder to test in
	 */
	protected Folder folder;

	/**
	 * Node where to create the object in
	 */
	protected Node objectNode;

	/**
	 * Create a test instance
	 * @param folderChannel folder channel
	 * @param localizationChannel localization channel
	 * @param objectChannel object channel
	 * @throws Exception 
	 */
	public MCObjectCreationTest(ChannelStructure.Channel folderChannel, ChannelStructure.Channel localizationChannel, ChannelStructure.Channel objectChannel)
			throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		FolderResource res = ContentNodeRESTUtils.getFolderResource();

		// create the folder
		FolderCreateRequest createFolder = new FolderCreateRequest();
		createFolder.setName("Folder");
		createFolder.setFailOnDuplicate(false);
		createFolder.setMotherId(channelStructure.getNode(ChannelStructure.Channel.MASTER).getFolder().getId().toString());

		FolderLoadResponse response = res.create(createFolder);
		t.commit(false);
		ContentNodeRESTUtils.assertResponseOK(response);
		folder = response.getFolder();

		// create the localization
		if (localizationChannel != ChannelStructure.Channel.NONE) {
			ContentNodeTestDataUtils.localize(t.getObject(com.gentics.contentnode.object.Folder.class, folder.getId()),
					channelStructure.getNode(localizationChannel));
			t.commit(false);
		}

		objectNode = channelStructure.getNode(objectChannel);

		testContext.startSystemUserTransaction();
	}

	/**
	 * Test creation of a page
	 * @throws Exception
	 */
	@Test
	public void testCreatePage() throws Exception {
		// create a template
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentNodeTestDataUtils.createTemplate(t.getObject(com.gentics.contentnode.object.Folder.class, folder.getId()), "Template");
		t.commit(false);

		// create a page
		PageCreateRequest request = new PageCreateRequest();
		PageResource res = ContentNodeRESTUtils.getPageResource();
		request.setFailOnDuplicate(false);
		request.setFolderId(folder.getId().toString());
		request.setPageName("Page");
		request.setNodeId(objectNode.getId());
		PageLoadResponse response = res.create(request);
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	/**
	 * Test copying a page
	 * @throws Exception
	 */
	@Test
	public void testCopyPage() throws Exception {
		// create a template
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Folder sourceFolder = t.getObject(com.gentics.contentnode.object.Folder.class, folder.getId());
		Template template = ContentNodeTestDataUtils.createTemplate(sourceFolder, "Template");
		t.commit(false);

		// create a page
		Page page = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Page", objectNode);

		testContext.startSystemUserTransaction();

		// move the page into the folder
		PageResource res = ContentNodeRESTUtils.getPageResource();
		PageCopyRequest request = new PageCopyRequest();
		request.setCreateCopy(true);
		request.setSourcePageIds(Arrays.asList(page.getId()));
		request.setNodeId(objectNode.getId());
		request.setTargetFolders(Arrays.asList(new TargetFolder(folder.getId(), objectNode.getId())));
		PageCopyResponse response = res.copy(request, 0);
		ContentNodeRESTUtils.assertResponseOK(response);
	}
}
