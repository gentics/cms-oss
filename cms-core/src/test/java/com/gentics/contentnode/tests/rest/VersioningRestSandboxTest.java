package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.setRenderType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.DefaultPageVersionNumberGenerator;
import com.gentics.contentnode.factory.object.PageVersionNumberGenerator;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.model.response.TagListResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

public class VersioningRestSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private final int changedPageId = 60; // PageWithChangesAndDifferentFolderAfterPublishedVersion
	private final int includingPageId = 61; // PageThatIncludesPageThatHasChangesAfterBeingPublished
	private final int nodeId = 11;
	private final String publishedVersion = "2.0";
	private final String tag = "content";
	private final String part = "html";

	/**
	 * ID of the user "editor"
	 */
	private final static int USER_EDITOR_ID = 26;

	/**
	 * ID of the user "system"
	 */
	private final static int USER_SYSTEM_ID = 1;

	@Before
	public void setUp() throws Exception {
		// clean invalid versioning data for values
		DBUtils.executeUpdate(
				"delete value_nodeversion from value_nodeversion left join contenttag on value_nodeversion.contenttag_id = contenttag.id left join page on contenttag.content_id = page.content_id where page.cdate > value_nodeversion.nodeversiontimestamp",
				null);
	}

	@After
	public void tearDown() throws Exception {
		// reset the lock_time, because this is changed in some tests
		if (testContext.getContext() != null) {
			testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("lock_time", (String)null);
		}

	}

	@Test
	public void testVersionedPublishingAlwaysRendersCurrentFolderButVersionedContent() throws Exception {
		// regenerate the logcmd entries, necessary to correctly fix the page version numbers
		int[] publishTimes = new int[] {1322470882, 1330958743, 1331042924};
		for (int publishTime : publishTimes) {
			DBUtils.executeInsert("INSERT INTO logcmd (user_id, cmd_desc_id, o_type, o_id, timestamp) VALUES (?, ?, ?, ?, ?)", new Object[] {1, ActionLogger.PAGEPUB, com.gentics.contentnode.object.Page.TYPE_PAGE, changedPageId, publishTime});
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		// get the page versions, which will fix the page version numbers
		t.getObject(com.gentics.contentnode.object.Page.class, changedPageId).getVersions();
		t.commit(false);

		// We set a specific version to the published version because at the time
		// of this writing the db dump is loaded from an older GCN version that doesn't
		// have the published column in the nodeversion table and is brought up-to-date
		// via the executable changelog which inserts 0 as the default.
		DBUtils.executeUpdate("UPDATE nodeversion SET published = ? WHERE o_type = ? AND o_id = ?", new Object[] {
			0, 10007, changedPageId
		});
		DBUtils.executeUpdate("UPDATE nodeversion SET published = ? WHERE o_type = ? AND o_id = ? AND nodeversion = ?", new Object[] {
			1, 10007, changedPageId, publishedVersion
		});

		PortalCache.getCache(NodeFactory.CACHEREGION).clear();

		/*
		 TODO: The above DB acces is a hack since moving pages between folders isn't yet supported
		 by the REST API. When the REST API supports moving pages between folders, the
		 above should be replaced with the following code.

		 String publishedContent = "<node folder.name>Madagascar";
		 String newestUnpublishedContent = "<node folder.name>Himalayas";
		 int fromFolderId = 51; // FolderWherePageIsPublished
		 int toFolderId = 52; // FolderWherePageIsMovedAfterPublish
		 
		 savePageWithNewContent(changedPageId, tag, part, publishedContent);
		 movePage(changedPageId, fromFolderId);
		 publishPage(changedPageId);
		 savePageWithNewContent(changedPageId, tag, part, newestUnpublishedContent);
		 movePage(changedPageId, toFolderId)
		 */

		// Rendering the page in publish mode isn't yet supported via the REST API, so render directly
		setRenderType(RenderType.EM_PUBLISH);
		t.getRenderType().setHandleDependencies(false);

		List<com.gentics.contentnode.object.Page> pages = t.getObjects(com.gentics.contentnode.object.Page.class, Collections.singleton(includingPageId));

		assertEquals("Check number of pages", 1, pages.size());
		RenderResult renderResult = new RenderResult();
		String pageContent = pages.get(0).render(renderResult);

		// The content of the page is now "<node folder.name>Himalayas".
		// We expect the folder always to reflect the current folder where the page is located.
		// The content on the other hand must come from the page version that was published, and not
		// from the current page version.
		// In this case the current folder is "FolderWherePageIsMovedAfterPublished" and the
		// published content is "Madagascar".
		assertEquals("FolderWherePageIsMovedAfterPublishMadagascar", pageContent);
	}

	/**
	 * Test saving a page with new content with creation of a page version. Save
	 * the page again (with the same content) and check that no new version was
	 * generated this time.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSaveWithCreateVersion() throws Exception {
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();

		Page page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> initialVersions = page.getVersions();
		String initialVersionNumber = page.getCurrentVersion().getNumber();

		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Version", true, true);

		page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> updatedVersions = page.getVersions();
		String updatedVersionNumber = page.getCurrentVersion().getNumber();

		assertEquals("Check # of versions after saving", initialVersions.size() + 1, updatedVersions.size());
		assertTrue("Check that version number changed", !StringUtils.isEqual(initialVersionNumber, updatedVersionNumber));
		assertEquals("Check version number after saving", gen.getNextVersionNumber(initialVersionNumber, false), updatedVersionNumber);

		// now save again, without really changing anything
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Version", true, true);

		// this must not create a new version
		page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> againUpdatedVersions = page.getVersions();
		String againUpdatedVersionNumber = page.getCurrentVersion().getNumber();

		assertEquals("Check # of versions after saving", updatedVersions.size(), againUpdatedVersions.size());
		assertEquals("Check version number after saving", updatedVersionNumber, againUpdatedVersionNumber);
	}

	/**
	 * Test saving a page with new content without creation of a page version
	 * @throws Exception
	 */
	@Test
	public void testSaveWithoutCreateVersion() throws Exception {
		Page page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> initialVersions = page.getVersions();
		String initialVersionNumber = page.getCurrentVersion().getNumber();

		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Version", true, false);

		page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> updatedVersions = page.getVersions();
		String updatedVersionNumber = page.getCurrentVersion().getNumber();

		assertEquals("Check # of versions after saving", initialVersions.size(), updatedVersions.size());
		assertEquals("Check version number after saving", initialVersionNumber, updatedVersionNumber);
	}

	/**
	 * Test that a version is created, when a user starts editing a page
	 * @throws Exception
	 */
	@Test
	public void testVersionWhenLocking() throws Exception {
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();

		// get the initial page version of the page
		Page page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> initialVersions = page.getVersions();
		String initialVersionNumber = page.getCurrentVersion().getNumber();

		// modify the page, but don't create a new version (the page will be unlocked)
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Version", true, false);

		// start editing the page, this is supposed to create a new version now
		page = loadPage(changedPageId, USER_SYSTEM_ID, true, true);
		List<PageVersion> updatedVersions = page.getVersions();
		String updatedVersionNumber = page.getCurrentVersion().getNumber();

		assertEquals("Check # of versions after saving", initialVersions.size() + 1, updatedVersions.size());
		assertEquals("Check version number after saving", gen.getNextVersionNumber(initialVersionNumber, false), updatedVersionNumber);
	}

	/**
	 * Test that a version is automatically created, when another user starts
	 * editing a page, which the previous editor left over locked.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVersionWhenLockingFromAnotherUser() throws Exception {
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();

		// set the lock timeout to 1s
		TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().setProperty("lock_time", "1");

		// get the initial page version of the page
		Page page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		List<PageVersion> initialVersions = page.getVersions();
		String initialVersionNumber = page.getCurrentVersion().getNumber();

		// modify the page, don't unlock and don't create a new version
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Version", false, false);

		// wait 1.5 secs for the lock to time out
		Thread.sleep(1500);

		// start editing the page
		page = loadPage(changedPageId, USER_EDITOR_ID, true, true);
		List<PageVersion> updatedVersions = page.getVersions();
		String updatedVersionNumber = page.getCurrentVersion().getNumber();
		User lastEditor = page.getCurrentVersion().getEditor();

		assertTrue("Check whether page is editable for new user", !page.isReadOnly());
		assertEquals("Check # of versions after saving", initialVersions.size() + 1, updatedVersions.size());
		assertEquals("Check version number after saving", gen.getNextVersionNumber(initialVersionNumber, false), updatedVersionNumber);
		assertEquals("Check version editor after saving", USER_SYSTEM_ID, lastEditor.getId().intValue());
	}

	/**
	 * Test that no new version is automatically created, when continuing to
	 * edit a page, which the editor left over locked.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNoVersionWhenContinueEditing() throws Exception {
		// get the initial page version of the page
		Page page = loadPage(changedPageId, USER_EDITOR_ID, true, true);
		List<PageVersion> initialVersions = page.getVersions();
		String initialVersionNumber = page.getCurrentVersion().getNumber();

		// modify the page, don't unlock and don't create a new version
		savePageWithNewContent(changedPageId, USER_EDITOR_ID, tag, part, "Version", false, false);

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// continue editing the page
		page = loadPage(changedPageId, USER_EDITOR_ID, true, true);
		List<PageVersion> updatedVersions = page.getVersions();
		String updatedVersionNumber = page.getCurrentVersion().getNumber();

		assertTrue("Check whether page is editable for new user", !page.isReadOnly());
		assertEquals("Check # of versions after saving", initialVersions.size(), updatedVersions.size());
		assertEquals("Check version number after saving", initialVersionNumber, updatedVersionNumber);
	}

	/**
	 * Test cancel editing a page. The last page version must be restored
	 * @throws Exception
	 */
	@Test
	public void testPageCancel() throws Exception {
		// change the page content and create a version
		savePageWithNewContent(changedPageId, USER_EDITOR_ID, tag, part, "This is the last saved content", true, true);

		GlobalId pageGlobalId = getPageGlobalId(changedPageId);
		GlobalId tagGlobalId = getTagGlobalId(changedPageId, tag);
		GlobalId valueGlobalId = getValueGlobalId(changedPageId, tag, part);
		Page page = loadPage(changedPageId, USER_EDITOR_ID, false, true);
		List<PageVersion> initialVersions = page.getVersions();
		Set<String> initialTagNames = page.getTags().keySet();
		String initialVersionNumber = page.getCurrentVersion().getNumber();
		String initialContent = page.getTags().get(tag).getProperties().get(part).getStringValue();

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// continue editing the page, but create no versions
		savePageWithNewContent(changedPageId, USER_EDITOR_ID, tag, part, "Modified", false, false);
		createTag(changedPageId, USER_EDITOR_ID, "htmllong", 0);

		page = loadPage(changedPageId, USER_EDITOR_ID, false, true);
		String modifiedContent = page.getTags().get(tag).getProperties().get(part).getStringValue();

		assertEquals("Check content after modification", "Modified", modifiedContent);

		assertEquals("Check page globalId after modification", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check tag globalId after modification", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check value globalId after modification", valueGlobalId, getValueGlobalId(changedPageId, tag, part));

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// now cancel editing the page
		cancelPageEdit(changedPageId, USER_EDITOR_ID);

		// load the page again
		page = loadPage(changedPageId, USER_EDITOR_ID, false, true);
		List<PageVersion> cancelledVersions = page.getVersions();
		Set<String> cancelledTagNames = page.getTags().keySet();
		String cancelledVersionNumber = page.getCurrentVersion().getNumber();
		String cancelledContent = page.getTags().get(tag).getProperties().get(part).getStringValue();

		assertFalse("Page must not be locked", page.isLocked());
		assertEquals("Check # of tags in the page after cancelling", initialTagNames.size(), cancelledTagNames.size());
		assertEquals("Check # of versions after cancelling", initialVersions.size(), cancelledVersions.size());
		assertEquals("Check version number after cancelling", initialVersionNumber, cancelledVersionNumber);
		assertEquals("Check content after cancelling", initialContent, cancelledContent);

		assertEquals("Check page globalId after cancel", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check tag globalId after cancel", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check value globalId after cancel", valueGlobalId, getValueGlobalId(changedPageId, tag, part));
	}

	/**
	 * Test that canceling a published (and unchanged) page does not change the page status
	 * @throws Exception
	 */
	@Test
	public void testPublishedPageCancel() throws Exception {
		// save the page
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "This is the last saved content", true, true);

		// publish the page
		publishPage(changedPageId, USER_SYSTEM_ID);

		// cancel editing
		cancelPageEdit(changedPageId, USER_SYSTEM_ID);

		// load the page
		Page page = loadPage(changedPageId, USER_SYSTEM_ID, false, true);

		assertTrue("Check page status after canceling", page.isOnline());
		assertFalse("Check page modified after canceling", page.isModified());
	}

	/**
	 * Test restoring a page version
	 * @throws Exception
	 */
	@Test
	public void testRestoreVersion() throws Exception {
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();

		// Save version 1
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Kathmandu", true, true);

		// get the global id
		GlobalId pageGlobalId = getPageGlobalId(changedPageId);

		// Determine timestamp to restore later
		Page page1 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		String version1 = page1.getCurrentVersion().getNumber();
		int v1Timestamp = page1.getCurrentVersion().getTimestamp();

		// get the global ID's of tag and value
		GlobalId tagGlobalId = getTagGlobalId(changedPageId, tag);
		GlobalId valueGlobalId = getValueGlobalId(changedPageId, tag, part);

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Save version 2
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Moscau", true, true);
		Page page2 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		String version2 = page2.getCurrentVersion().getNumber();

		assertEquals("Check new version number", gen.getNextVersionNumber(version1, false), version2);

		// the global ids of the page must not have changed
		assertEquals("Check global id of the page after modification", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check global id of the tag after modification", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check global id of the value after modification", valueGlobalId, getValueGlobalId(changedPageId, tag, part));

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Restore version 1 with saved timestamp
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		@SuppressWarnings("unused")
		PageLoadResponse restoreResponse = pageResource.restoreVersion(String.valueOf(changedPageId), v1Timestamp);

		// Verify it was correctly restored
		Page page3 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		String html = page3.getTags().get(tag).getProperties().get(part).getStringValue();

		assertEquals("Kathmandu", html);

		// new version must have been created
		String version3 = page3.getCurrentVersion().getNumber();

		assertEquals("Check restored version number", gen.getNextVersionNumber(version2, false), version3);

		// the global ids of the page must not have changed
		assertEquals("Check global id of the page after restore", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check global id of the tag after restore", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check global id of the value after restore", valueGlobalId, getValueGlobalId(changedPageId, tag, part));
	}

	/**
	 * Test restoring a page version where one of the tags got deleted in the meantime
	 * @throws Exception
	 */
	@Test
	public void testRestoreVersionWithDeletedTagConstruct() throws Exception {
		testContext.getContext().startTransaction(USER_SYSTEM_ID);

		Node node = TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);
		int constructId = ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "dummyconstruct", "textpart");
		Tag dummyTag = createTag(changedPageId, USER_SYSTEM_ID, null, constructId);

		// Save version 1
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Kathmandu", true, true);

		// Commit transaction to make sure no tags exists with the dummy construct.
		// Otherwise deleting the construct would throw an error.
		Transaction transaction = TransactionManager.getCurrentTransaction();
		transaction.commit(false);

		// Determine timestamp to restore later
		Page currentPage = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		int v1Timestamp = currentPage.getCurrentVersion().getTimestamp();

		// We wait for the next second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Remove the tag out of the page so we can safely delete its construct
		deleteTag(changedPageId, USER_SYSTEM_ID, dummyTag.getName());

		testContext.getContext().startTransaction(USER_SYSTEM_ID);
		transaction = TransactionManager.getCurrentTransaction();

		// Delete the construct again
		transaction.getObject(Construct.class, constructId).delete();

		// Commit transaction
		transaction.commit(false);

		// Save version 2
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Moscau", true, true);

		// We wait for the next second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Restore version 1 with saved timestamp
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		PageLoadResponse restoreResponse = pageResource.restoreVersion(String.valueOf(changedPageId), v1Timestamp);
		assertEquals("Restore has to be successful", ResponseCode.OK, restoreResponse.getResponseInfo().getResponseCode());

		// Verify it was correctly restored
		Page page3 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);

		assertEquals("Test-tag with deleted construct must not exist", null, page3.getTags().get(dummyTag.getName()));

		final int dummyTagId = dummyTag.getId();
		// Get all values from the deleted tag
		DBUtils.executeStatement("SELECT id FROM value WHERE contenttag_id = ?",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement preparedStatement) throws SQLException {
						preparedStatement.setInt(1, dummyTagId);
					}

					@Override
					public void handleResultSet(ResultSet resultSet) throws SQLException, NodeException {
						assertEquals("No values should have been restored for the deleted tag", 0,
								resultSet.getFetchSize());
					}
		});
	}

	/**
	 * Test restoring a previous version of a tag only
	 * @throws Exception
	 */
	@Test
	public void testRestoreTagVersion() throws Exception {
		// create a second tag
		Tag newTag = createTag(changedPageId, USER_SYSTEM_ID, "htmllong", 0);

		// Save version 1
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, newTag.getName(), part, "Initial content for new tag", false, false);
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Initial content for old tag", true, true);

		// get the global id
		GlobalId pageGlobalId = getPageGlobalId(changedPageId);

		// Determine timestamp to restore later
		Page page1 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		int v1Timestamp = page1.getCurrentVersion().getTimestamp();

		// get the global ID's of tag and value
		GlobalId tagGlobalId = getTagGlobalId(changedPageId, tag);
		GlobalId valueGlobalId = getValueGlobalId(changedPageId, tag, part);

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Save version 2
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, newTag.getName(), part, "Modified content for new tag", false, false);
		savePageWithNewContent(changedPageId, USER_SYSTEM_ID, tag, part, "Modified content for old tag", true, true);
		Page page2 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		assertEquals("Check modified tag contents", "Modified content for new tag", page2.getTags().get(newTag.getName()).getProperties().get(part).getStringValue());
		assertEquals("Check modified tag contents", "Modified content for old tag", page2.getTags().get(tag).getProperties().get(part).getStringValue());

		// the global ids of the page must not have changed
		assertEquals("Check global id of the page after modification", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check global id of the tag after modification", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check global id of the value after modification", valueGlobalId, getValueGlobalId(changedPageId, tag, part));

		// We wait for a second to work around the issue that saving a page twice
		// in the same second only generates a single version.
		Thread.sleep(1000);

		// Restore version 1 with saved timestamp
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		@SuppressWarnings("unused")
		TagListResponse restoreResponse = pageResource.restoreTag(String.valueOf(changedPageId), tag, v1Timestamp);

		// Verify it was correctly restored
		Page page3 = loadPage(changedPageId, USER_SYSTEM_ID, false, true);
		assertEquals("Check not restored tag contents", "Modified content for new tag", page3.getTags().get(newTag.getName()).getProperties().get(part).getStringValue());
		assertEquals("Check restored tag contents", "Initial content for old tag", page3.getTags().get(tag).getProperties().get(part).getStringValue());

		// the global ids of the page must not have changed
		assertEquals("Check global id of the page after restore", pageGlobalId, getPageGlobalId(changedPageId));
		assertEquals("Check global id of the tag after restore", tagGlobalId, getTagGlobalId(changedPageId, tag));
		assertEquals("Check global id of the value after restore", valueGlobalId, getValueGlobalId(changedPageId, tag, part));
	}

	/**
	 * Load the page with given id in a new transaction with the given user
	 * @param pageId page ID
	 * @param userId user ID
	 * @param forUpdate true if the page shall be locked
	 * @param versionInfo true if version information shall be fetched
	 * @return returned page
	 * @throws NodeException
	 */
	private Page loadPage(int pageId, int userId, boolean forUpdate, boolean versionInfo) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		TransactionManager.getCurrentTransaction().getRenderType().setEditMode(RenderType.EM_PREVIEW);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		PageLoadResponse response = pageResource.load(String.valueOf(pageId), forUpdate, false, false, false, false, false, false, versionInfo, false, false, 0, null);

		assertEquals("Check page load response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		return response.getPage();
	}

	/**
	 * Save a page with the given content set in the given tag/part. Optionally let the REST API create a page version
	 * @param pageId page id
	 * @param userId user ID
	 * @param tag name of the tag to modify
	 * @param part name of the part to modify
	 * @param newContent content to be set into the tag/part
	 * @param unlock true when the page shall be unlocked
	 * @param createVersion true when a new page version shall be created
	 * @throws NodeException
	 */
	private void savePageWithNewContent(int pageId, int userId, String tag, String part, String newContent, boolean unlock, boolean createVersion) throws NodeException {
		Page page = loadPage(pageId, userId, true, false);
		
		// change the page
		Property html = page.getTags().get(tag).getProperties().get(part);

		html.setStringValue(newContent);
		
		// save the page
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());
		PageSaveRequest saveRequest = new PageSaveRequest(page);

		saveRequest.setUnlock(unlock);
		saveRequest.setCreateVersion(createVersion);
		GenericResponse saveResponse = pageResource.save(String.valueOf(pageId), saveRequest);

		assertEquals(ResponseCode.OK, saveResponse.getResponseInfo().getResponseCode());
	}

	/**
	 * Create a new tag in the page
	 * @param pageId       page id
	 * @param userId       user id
	 * @param keyword      keyword of the construct, set to null if you want
	 *                     to use the constructId parameter instead
	 * @param constructId  set to 0 if you want to use the keyword parameter instead
	 * @return the created tag
	 * @throws NodeException
	 */
	private Tag createTag(int pageId, int userId, String keyword, int constructId) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		ContentTagCreateRequest request = new ContentTagCreateRequest();

		if (keyword != null && !keyword.isEmpty()) {
			request.setKeyword(keyword);
		}

		if (constructId > 0) {
			request.setConstructId(constructId);
		}

		TagCreateResponse response = pageResource.createTag(Integer.toString(pageId), null, null, request);

		assertEquals(ResponseCode.OK, response.getResponseInfo().getResponseCode());
		return response.getTag();
	}

	/**
	 * Delete a tag in the page
	 * @param pageId page id
	 * @param userId user id
	 * @param keyword keyword of the construct
	 * @throws NodeException
	 */
	private void deleteTag(int pageId, int userId, String keyword) throws NodeException {
		Page page = loadPage(pageId, userId, true, false);
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		PageSaveRequest pageSaveRequest = new PageSaveRequest();

		List<String> tagsToDelete = new ArrayList<String>();
		tagsToDelete.add(keyword);

		pageSaveRequest.setPage(page);
		pageSaveRequest.setDelete(tagsToDelete);
		pageResource.save(Integer.toString(pageId), pageSaveRequest);
	}

	/**
	 * Cancel editing the page
	 * @param pageId page ID
	 * @param userId user ID
	 * @throws NodeException
	 */
	private void cancelPageEdit(int pageId, int userId) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		TransactionManager.getCurrentTransaction().getRenderType().setEditMode(RenderType.EM_PREVIEW);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		GenericResponse response = pageResource.cancel(pageId, null);

		assertEquals("Check page cancel response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Publish the page
	 * @param pageId page ID
	 * @param userId user ID
	 * @throws NodeException
	 */
	private void publishPage(int pageId, int userId) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		testContext.getContext().startTransaction(userId);
		TransactionManager.getCurrentTransaction().getRenderType().setEditMode(RenderType.EM_PREVIEW);
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		GenericResponse response = pageResource.publish(Integer.toString(pageId), null, new PagePublishRequest());

		assertEquals("Check page cancel response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Get the global id of the page
	 * @param pageId page id
	 * @return global id
	 * @throws NodeException
	 */
	private GlobalId getPageGlobalId(int pageId) throws NodeException {
		GlobalId globalId = GlobalId.getGlobalId("page", pageId);
		assertNotNull("Page must have a globalId", globalId);
		return globalId;
	}

	/**
	 * Get the global id of the tag
	 * @param pageId page id
	 * @param tagName tagname
	 * @return global id
	 * @throws NodeException
	 */
	private GlobalId getTagGlobalId(int pageId, String tagName) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page page = t.getObject(com.gentics.contentnode.object.Page.class, pageId);
		assertNotNull("Could not get page " + pageId, page);
		ContentTag contentTag = page.getContentTag(tagName);
		assertNotNull("Could not get tag " + tagName + " for " + page, contentTag);
		GlobalId globalId = contentTag.getGlobalId();
		assertNotNull("Tag must have a globalId", globalId);
		return globalId;
	}

	/**
	 * Get the global id of the value in the tag of the page
	 * @param pageId page id
	 * @param tagName tag name
	 * @param partName part name
	 * @return global id
	 * @throws NodeException
	 */
	private GlobalId getValueGlobalId(int pageId, String tagName, String partName) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Page page = t.getObject(com.gentics.contentnode.object.Page.class, pageId);
		assertNotNull("Could not get page " + pageId, page);
		ContentTag contentTag = page.getContentTag(tagName);
		assertNotNull("Could not get tag " + tagName + " for " + page, contentTag);
		Value value = contentTag.getValues().getByKeyname(partName);
		assertNotNull("Could not get value " + partName + " for " + contentTag, value);
		GlobalId globalId = value.getGlobalId();
		assertNotNull("Value must have a globalId", globalId);
		return globalId;
	}
}
