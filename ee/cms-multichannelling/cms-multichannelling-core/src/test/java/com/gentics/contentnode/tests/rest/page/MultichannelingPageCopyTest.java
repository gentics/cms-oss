package com.gentics.contentnode.tests.rest.page;

import static com.gentics.api.lib.etc.ObjectTransformer.getInteger;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Various testcases for the page copy rest api call.
 * 
 * @author johannes2
 * 
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class MultichannelingPageCopyTest extends AbstractPageCopyTest {
	/**
	 * Copy a page from the master into a subfolder in the master. Create a
	 * channel that inherits the folder. The channel folder contains a localized
	 * page with the same filename as the source page. It should not be possible
	 * to copy the source page into the target folder since this would cause a
	 * conflict with the existing channel page. The response should contain
	 * information which page conflicted with what.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionByInheritanceFromMasterCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder masterFolder = Creator.createFolder(nodeA.getFolder(), "masterFolder", "/test");
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/channelA");

		// Create a local page in the channel which conflicts with the new copy
		Page conflictingPageInChannel = Creator.createPage(DUMMY_PAGE_NAME, masterFolder, template, null, channelA);
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
		addTargetFolder(copyRequest, masterFolder);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, false);
		t = TransactionManager.getCurrentTransaction();
		assertEquals("Check whether correct number of copys have been made.", 0, response.getPages().size());
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());

		// Validate the i18n message
		CNI18nString expected = new CNI18nString("page_copy.name_conflict_detected");
		expected.addParameter(I18NHelper.getLocation(sourcePage));
		expected.addParameter(I18NHelper.getLocation(masterFolder));
		assertMessage(response, expected);

		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());
	}

	/**
	 * Copy a single page from channel to different folder in subchannel
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyCrossChannels() throws Exception {

		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "channel", "nowhere.tld", "/test");
		Node subChannel = ContentNodeTestDataUtils.createChannel(channel, "subchannel", "somewhere.tld", "/test2");
		Folder folderInSubchannel = Creator.createFolder(subChannel.getFolder(), "subfolder", "/123");
		Page testPage = Creator.createPage(DUMMY_PAGE_NAME, channel.getFolder(), template, en);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		addTargetFolder(copyRequest, folderInSubchannel);
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(),-1));

		// Invoke copy
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_NAME + ".en1.html");
		expectedPage.setName("Kopie von " + DUMMY_PAGE_NAME);
		assertCopiedPages(folderInSubchannel, expectedPage);

	}

	/**
	 * Copy a single test page from a localized folder in channel A to a
	 * localized folder in channel B.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyCrossLocalizedFoldersCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create test fixture
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/test");
		Node channelB = ContentNodeTestDataUtils.createChannel(nodeB, "ChannelB", "nowhere2.tld", "/test2");
		Folder folderInNodeB = Creator.createFolder(nodeB.getFolder(), "nodeBFolder", "/test");
		Folder folderInNodeA = Creator.createFolder(nodeA.getFolder(), "nodeAFolder", "/test");
		Folder localizedFolderInChannelA = ContentNodeTestDataUtils.localizeFolder(channelA, folderInNodeA);
		Folder localizedFolderInChannelB = ContentNodeTestDataUtils.localizeFolder(channelB, folderInNodeB);
		Page testPage = Creator.createPage("DummyPage", localizedFolderInChannelA, template, en, channelA);
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(),-1));
		addTargetFolder(copyRequest, localizedFolderInChannelB);

		// Invoke copy call
		copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_NAME + ".en1.html");
		expectedPage.setName(DUMMY_PAGE_NAME);

		t.setChannelId(channelB.getId());
		assertCopiedPages(localizedFolderInChannelB, expectedPage);
		t.resetChannel();

	}

	/**
	 * Copy a single test page from a localized folder in channel A to a
	 * localized folder in channel B.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyCrossLocalizedFolders() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create test fixture
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/test");
		Node channelB = ContentNodeTestDataUtils.createChannel(nodeB, "ChannelB", "nowhere2.tld", "/test2");
		Folder folderInNodeB = Creator.createFolder(nodeB.getFolder(), "nodeBFolder", "/test");
		Folder folderInNodeA = Creator.createFolder(nodeA.getFolder(), "nodeAFolder", "/test");
		Folder localizedFolderInChannelA = ContentNodeTestDataUtils.localizeFolder(channelA, folderInNodeA);
		Folder localizedFolderInChannelB = ContentNodeTestDataUtils.localizeFolder(channelB, folderInNodeB);
		Page testPage = Creator.createPage("DummyPage", localizedFolderInChannelA, template, en, channelA);
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(testPage.getId(),-1));
		addTargetFolder(copyRequest, localizedFolderInChannelB);

		// Invoke copy call
		copy(copyRequest, true);
		t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_NAME + ".en1.html");
		expectedPage.setName("Kopie von " + DUMMY_PAGE_NAME);

		t.setChannelId(channelB.getId());
		assertCopiedPages(localizedFolderInChannelB, expectedPage);
		t.resetChannel();

	}

	/**
	 * Copy a single test page from a localized folder into a different master
	 * node with a new subfolder.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBatchCopyLocalizedFolderPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Folder masterNodeFolderA = Creator.createFolder(nodeA.getFolder(), "SubfolderNodeA", "/test2");
		Folder masterNodeFolderB = Creator.createFolder(nodeB.getFolder(), "SubfolderNodeB", "/test2");
		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/test");
		Folder localizedFolderAInChannel = ContentNodeTestDataUtils.localizeFolder(channel, masterNodeFolderA);
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, localizedFolderAInChannel, template);
		t.commit(false);

		// Prepare the copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
		addTargetFolder(copyRequest, masterNodeFolderB);

		// Invoke copy call
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setName(DUMMY_PAGE_NAME);
		expectedPage.setFileName(DUMMY_PAGE_FILENAME);
		assertCopiedPages(masterNodeFolderA, expectedPage);

	}

	/**
	 * Test whether a localized page can be copied to a different folder in the
	 * same channel.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyOfLocalizedPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup the test fixture
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/test");
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Page localizedPage = ContentNodeTestDataUtils.localizePage(channelA, sourcePage);
		Folder targetFolder = Creator.createFolder(channelA.getFolder(), "SubfolderInChannelA", "/test");
		t.commit(false);

		// Prepare the request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.getSourcePageIds().add(getInteger(localizedPage.getId(),-1));
		addTargetFolder(copyRequest, targetFolder);
		copyRequest.setCreateCopy(ENABLED);

		// Invoke copy call
		copy(copyRequest, true);

		com.gentics.contentnode.rest.model.Page expectedPage = new com.gentics.contentnode.rest.model.Page();
		expectedPage.setFileName(DUMMY_PAGE_FILENAME);
		expectedPage.setName("Kopie von " + DUMMY_PAGE_NAME);
		expectedPage.setFolderId(getInteger(targetFolder.getId(),-1));
		assertCopiedPages(targetFolder, expectedPage);
	}

	/**
	 * Test copying a page from from master node into the same folder in the
	 * channel of this node. Verify that a conflict is being detected.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyToSubchannelCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "channel", "no.where.tld", "/test");
		com.gentics.contentnode.rest.model.Folder targetFolder = ModelBuilder.getFolder(channel.getFolder().getMaster());
		targetFolder.setChannelId(getInteger(channel.getId(),-1));
		t.commit(false);

		// Prepare copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
		addTargetFolder(copyRequest, targetFolder);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, false);
		t = TransactionManager.getCurrentTransaction();
		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());

		// Validate the i18n message
		CNI18nString expected = new CNI18nString("page_copy.name_conflict_detected");
		expected.addParameter(I18NHelper.getLocation(sourcePage));
		expected.addParameter(I18NHelper.getLocation(nodeA.getFolder()));
		assertMessage(response, expected);

		assertEquals("No pages should have been copied and added to the reponse", 0, response.getPages().size());

		com.gentics.contentnode.rest.model.Page sourceRestPage = ModelBuilder.getPage(sourcePage);
		t.setChannelId(channel.getId());
		assertCopiedPages(channel.getFolder(), sourceRestPage);
		t.resetChannel();
	}

	/**
	 * Test whether a copy call fails when the user has no create perms in the
	 * target folder which has channel specific permissions.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyNoPageCreateInTarget2() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "channel", "no.where.tld", "/test");
		Folder folderInChannel = Creator.createFolder(channel.getFolder(), "FolderInChannel", "/");
		Folder targetFolder = folderInChannel;

		// Setup permissions
		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		UserGroup testUserGroup = Creator.createUsergroup("maingroup", "", parentGroup);
		SystemUser testUser = Creator.createUser("testuser", "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { testUserGroup }));
		Set<Integer> setOfRestrictedNodes = new HashSet<Integer>();
		setOfRestrictedNodes.add(getInteger(channel.getId(),-1));
		setOfRestrictedNodes.add(getInteger(nodeA.getId(),-1));
		testUser.getGroupNodeRestrictions().put(getInteger(testUserGroup.getId(),-1), setOfRestrictedNodes);
		testUser.save();

		List<UserGroup> groupsWithNoPerm = Arrays.asList(new UserGroup[] { testUserGroup });
		PermHandler.setPermissions(Folder.TYPE_FOLDER, getInteger(folderInChannel.getId(),-1), groupsWithNoPerm, ONLY_VIEW_PERM);
		PermHandler.setPermissions(Node.TYPE_NODE, getInteger(nodeA.getFolder().getId(),-1), groupsWithNoPerm, new Permission(PermHandler.FULL_PERM).toString());
		PermissionStore.initialize(true);
		t.commit(false);

		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			// Prepare copy request
			PageCopyRequest copyRequest = new PageCopyRequest();
			copyRequest.setCreateCopy(ENABLED);
			copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
			addTargetFolder(copyRequest, targetFolder);

			// Invoke copy call
			assertError(() -> getPageResource().copy(copyRequest, 0), InsufficientPrivilegesException.class,
					ResponseCode.FAILURE, "Error during copy process. Please check the messages.");
		}
	}

	/**
	 * Copy a single page from a master folder to a folder in a channel and a
	 * subchannel. Both the subchannel and channel folder are inherited by a
	 * master folder which is not the parent folder of the source page. Make
	 * sure that the filename is updated correctly.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionDetectionToChannel() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder masterFolder = Creator.createFolder(nodeA.getFolder(), "masterFolder", "/test");
		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "Channel", "nowhere.tld", "/channel");
		Node subChannel = ContentNodeTestDataUtils.createChannel(channel, "Subchannel", "somewhere.tld", "/subchannel");
		t.commit(false);

		// Load the folder in the scope of the channel
		com.gentics.contentnode.rest.model.Folder inheritedFolderInChannel = ModelBuilder.getFolder(masterFolder);
		inheritedFolderInChannel.setChannelId(getInteger(channel.getId(),-1));

		com.gentics.contentnode.rest.model.Folder inheritedFolderInSubChannel = ModelBuilder.getFolder(masterFolder);
		inheritedFolderInSubChannel.setChannelId(getInteger(subChannel.getId(),-1));

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(ENABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
		addTargetFolder(copyRequest, inheritedFolderInChannel);
		addTargetFolder(copyRequest, inheritedFolderInSubChannel);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, true);
		assertEquals("Check whether correct number of copys have been made.", 2, response.getPages().size());
		t = TransactionManager.getCurrentTransaction();

		// Verify the copied pages
		t.setChannelId(channel.getId());
		com.gentics.contentnode.rest.model.Page firstCopy = new com.gentics.contentnode.rest.model.Page();
		firstCopy.setName("Kopie von " + DUMMY_PAGE_NAME);
		firstCopy.setFileName(DUMMY_PAGE_FILENAME);
		// The folder contains the copy
		assertCopiedPages(masterFolder, firstCopy);
		t.resetChannel();

		// The second copy must be renamed or a multichanneling inheritance
		// collision would occure. The original dummy page filename is already
		// ocupied by the channelA page copy.
		t.setChannelId(subChannel.getId());
		com.gentics.contentnode.rest.model.Page secondCopy = new com.gentics.contentnode.rest.model.Page();
		secondCopy.setName("Kopie 2 von " + DUMMY_PAGE_NAME);
		secondCopy.setFileName(DUMMY_PAGE_NAME + "1.html");
		// The folder contains the copys
		assertCopiedPages(masterFolder, firstCopy, secondCopy);
		t.resetChannel();
	}

	/**
	 * Copy a single page from the master node to the same inherited folder in a
	 * channel and a subchannel. Invoke the copy call with create copy disabled
	 * and check that the copy fails.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPathCollisionDetectionToChannelCreateCopyDisabled() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeA.getFolder(), template);
		Folder masterFolder = Creator.createFolder(nodeA.getFolder(), "masterFolder", "/test");
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/channelA");
		Node subChannelA = ContentNodeTestDataUtils.createChannel(channelA, "subchannelA", "somewhere.tld", "/channelB");

		com.gentics.contentnode.rest.model.Folder inheritedFolderInChannelA = ModelBuilder.getFolder(masterFolder);
		inheritedFolderInChannelA.setChannelId(getInteger(channelA.getId(),-1));

		com.gentics.contentnode.rest.model.Folder inheritedFolderInSubChannelA = ModelBuilder.getFolder(masterFolder);
		inheritedFolderInChannelA.setChannelId(getInteger(subChannelA.getId(),-1));
		t.commit(false);

		// Setup copy request
		PageCopyRequest copyRequest = new PageCopyRequest();
		copyRequest.setCreateCopy(DISABLED);
		copyRequest.getSourcePageIds().add(getInteger(sourcePage.getId(),-1));
		addTargetFolder(copyRequest, inheritedFolderInChannelA);
		addTargetFolder(copyRequest, inheritedFolderInSubChannelA);

		// Invoke copy call
		PageCopyResponse response = copy(copyRequest, false);
		t = TransactionManager.getCurrentTransaction();
		String explanation = "Copy should fail since the source page will be inherited from the master. Copying the page to the other channels would cause a conflict with the inheritance of multichanneling. Copying the pages is therefore not allowed.";
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ") - " + explanation, ResponseCode.FAILURE, response.getResponseInfo()
				.getResponseCode());

		assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
				.getResponseMessage());
		CNI18nString expected = new CNI18nString("page_copy.name_conflict_detected");
		expected.addParameter(I18NHelper.getLocation(sourcePage));
		expected.addParameter(I18NHelper.getLocation(masterFolder));
		assertMessage(response, expected);
		assertEquals("No pages should have been copied and added to the reponse", 0, response.getPages().size());

		// Load the inherited folder in channel A
		t.setChannelId(channelA.getId());
		Folder inheritedNodeFolderInChannelA = t.getObject(Folder.class, masterFolder.getId());
		assertCopiedPages(inheritedNodeFolderInChannelA, EMPTY_LIST_OF_PAGES);
		t.resetChannel();

		// Load the inherited folder in subchannel A
		t.setChannelId(subChannelA.getId());
		Folder inheritedNodeFolderInSubchannelA = t.getObject(Folder.class, masterFolder.getId());
		assertCopiedPages(inheritedNodeFolderInSubchannelA, EMPTY_LIST_OF_PAGES);
		t.resetChannel();

	}

	/**
	 * Test whether a copy call fails when trying to copy a localized page from
	 * a channel folder which the user can't read.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPageCopyViewPermInSource() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Setup test fixture
		Folder nodeFolder = Creator.createFolder(nodeA.getFolder(), "testFolder", "/test123");
		Node channel = ContentNodeTestDataUtils.createChannel(nodeA, "ChannelA", "nowhere.tld", "/test");
		Page sourcePage = Creator.createPage(DUMMY_PAGE_NAME, nodeFolder, template);
		Page localizedSourcePage = ContentNodeTestDataUtils.localizePage(channel, sourcePage);
		t.commit(false);
		t.setChannelId(channel.getId());
		sourcePage = t.getObject(Page.class, sourcePage.getId());
		CNI18nString expected = new CNI18nString("page_copy.missing_view_perm_for_source_page");
		expected.addParameter(I18NHelper.getLocation(sourcePage));
		expected.addParameter(I18NHelper.getLocation(sourcePage.getFolder()));
		String expectedMessage = expected.toString();
		t.resetChannel();

		// Create group with no read perm to source folder
		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		UserGroup testUserGroup = Creator.createUsergroup("maingroup", "", parentGroup);
		SystemUser testUser = Creator.createUser("testuser", "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { testUserGroup }));

		List<UserGroup> groupsWithNoPerm = Arrays.asList(new UserGroup[] { testUserGroup });
		PermHandler.setPermissions(Node.TYPE_CHANNEL, getInteger(channel.getFolder().getId(),-1), groupsWithNoPerm, new Permission(PermHandler.EMPTY_PERM).toString());
		PermissionStore.initialize(true);
		t.commit(false);

		try (DBSessionClosure ses = new DBSessionClosure(testUser.getId())) {
			// Prepare the copy request
			PageCopyRequest copyRequest = new PageCopyRequest();
			copyRequest.setCreateCopy(ENABLED);
			copyRequest.getSourcePageIds().add(getInteger(localizedSourcePage.getId(),-1));
			addTargetFolder(copyRequest, nodeB.getFolder());

			// Invoke copy call
			PageResource pageResource = ContentNodeRESTUtils.getPageResource();
			PageCopyResponse response = pageResource.copy(copyRequest, 0);
			assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ")", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());

			assertEquals("Reponse did not contain the expected error message.", "Error during copy process. Please check the messages.", response.getResponseInfo()
					.getResponseMessage());
			assertEquals(expectedMessage, response.getMessages().get(0).getMessage());
		}
	}

	/**
	 * Test copying a page between local channel folders
	 * @throws Exception
	 */
	@Test
	public void testCopyBetweenLocalFolders() throws Exception {
		// create channels with local folders
		Node channelA = ContentNodeTestDataUtils.createChannel(nodeA, "Channel A", "channela", "/");
		Folder folderA = ContentNodeTestDataUtils.createFolder(nodeA.getFolder(), "Folder A", channelA);
		Node channelB = ContentNodeTestDataUtils.createChannel(nodeB, "Channel B", "channelb", "/");
		Folder folderB = ContentNodeTestDataUtils.createFolder(nodeB.getFolder(), "Folder B", channelB);

		// create source page in folderA
		Page page = ContentNodeTestDataUtils.createPage(folderA, template, "Page", channelA);
		testContext.getContext().startTransaction();

		// copy the page into folderB (channelB)
		PageCopyRequest request = new PageCopyRequest();
		request.setCreateCopy(true);
		request.setSourcePageIds(Arrays.asList(ObjectTransformer.getInt(page.getId(), 0)));
		request.setNodeId(ObjectTransformer.getInt(channelA.getId(), 0));
		request.setTargetFolders(Arrays.asList(new TargetFolder(ObjectTransformer.getInt(folderB.getId(), 0),  ObjectTransformer.getInt(channelB.getId(), 0))));
		copy(request, true);

		Transaction t = TransactionManager.getCurrentTransaction();
		folderB = t.getObject(Folder.class, folderB.getId());
		t.setChannelId(channelB.getId());
		try {
			List<Page> pages = folderB.getPages();
			assertEquals("Check # of pages in target folder", 1, pages.size());
			assertEquals("Check channel of page ", channelB, pages.get(0).getChannel());
			assertFalse("Page must not be inherited", pages.get(0).isInherited());
		} finally {
			t.resetChannel();
		}
	}
}