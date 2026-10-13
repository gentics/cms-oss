package com.gentics.contentnode.tests.multichannelling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePreviewRequest;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PagePreviewResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.testutils.database.SQLUtilException;

/**
 * Page tests for multichannelling support.
 *
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class PageChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test inheritance and localizing of pages.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testPageInheritanceAndLocalizing() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.page_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeRootFolder.getId());

		testFolder.save();
		t.commit(false);
		t.refreshPermHandler();

		// Create first test page in the root folder.
		Page newPageFirst = t.createObject(Page.class);

		newPageFirst.setDescription(C.page_channel_test.new_page_description);
		newPageFirst.setFilename(C.page_channel_test.new_page_filename);
		newPageFirst.setFolderId(testFolder.getId());
		newPageFirst.setName(C.page_channel_test.new_page_name);
		newPageFirst.setTemplateId(C.page_channel_test.new_page_template_id);
		newPageFirst.save();

		// Create a second test page in the root folder.
		Page newPageSecond = t.createObject(Page.class);

		newPageSecond.setDescription(C.page_channel_test.new_page_for_l18n_description);
		newPageSecond.setFilename(C.page_channel_test.new_page_for_l18n_filename);
		newPageSecond.setFolderId(testFolder.getId());
		newPageSecond.setName(C.page_channel_test.new_page_for_l18n_name);
		newPageSecond.setTemplateId(C.page_channel_test.new_page_template_id);

		// Create channel set id if there is no id set yet
		Integer channelSetId = newPageSecond.getChannelSetId();

		newPageSecond.save();
		t.commit(false);

		// Create a local copy of the second page and set it's channel info - a.k.a. make a
		// localized copy in the specified channel.
		Page localCopy = (Page) newPageSecond.copy();

		localCopy.setChannelInfo(channelId, channelSetId);

		localCopy.save();
		t.commit(false);

		// Execute rest request (without http) and retrieve all pages for the test folder
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		PageListParameterBean pageListParams = new PageListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		PublishableParameterBean publishableParameterBean = new PublishableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		PageListResponse pageListResponse = ContentNodeRESTUtils.getPageResource().list(
			inFolder,
			pageListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			publishableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, pageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the page list is null", pageListResponse.getItems());
		assertTrue("Check if the page list contains elements", pageListResponse.getItems().size() == 2);

		// Need these to check if we tested all the test cases below.
		boolean isFirstChecked = false;
		boolean isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.Page page : pageListResponse.getItems()) {
			assertEquals("Check if page folder id is the same", testFolder.getId(), page.getFolderId());
			assertEquals("Check if page template id is the same", Integer.valueOf(C.page_channel_test.new_page_template_id), page.getTemplateId());

			// Test if the first page has the correct data and is not inherited
			if (page.getId().equals(newPageFirst.getId())) {
				assertEquals("Check if page name is the same", C.page_channel_test.new_page_name, page.getName());
				assertEquals("Check if page filename is the same", C.page_channel_test.new_page_filename, page.getFileName());
				assertEquals("Check if page description is the same", C.page_channel_test.new_page_description, page.getDescription());
				assertTrue("Check if the page is inherited", page.isInherited());
				isFirstChecked = true;
			}

			// Test if the second page has the correct data and is n
			if (page.getId().equals(localCopy.getId())) {
				assertEquals("Check if page name is the same", C.page_channel_test.new_page_for_l18n_name, page.getName());
				assertEquals("Check if page filename is the same", C.page_channel_test.new_page_for_l18n_filename, page.getFileName());
				assertEquals("Check if page description is the same", C.page_channel_test.new_page_for_l18n_description, page.getDescription());
				assertTrue("Check if the page is inherited", !page.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both pages", (isFirstChecked && isSecondChecked));


		// Check again with legacy endpoints.
		LegacyPageListResponse legacyPageListResponse = ContentNodeRESTUtils.getFolderResource().getPages(
			inFolder.folderId,
			inFolder,
			pageListParams,
			filter,
			sort,
			paging,
			publishableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyPageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the page list is null", legacyPageListResponse.getPages());
		assertTrue("Check if the page list contains elements", legacyPageListResponse.getPages().size() == 2);

		// Need these to check if we tested all the test cases below.
		isFirstChecked = false;
		isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.Page page : legacyPageListResponse.getPages()) {
			assertEquals("Check if page folder id is the same", testFolder.getId(), page.getFolderId());
			assertEquals("Check if page template id is the same", Integer.valueOf(C.page_channel_test.new_page_template_id), page.getTemplateId());

			// Test if the first page has the correct data and is not inherited
			if (page.getId().equals(newPageFirst.getId())) {
				assertEquals("Check if page name is the same", C.page_channel_test.new_page_name, page.getName());
				assertEquals("Check if page filename is the same", C.page_channel_test.new_page_filename, page.getFileName());
				assertEquals("Check if page description is the same", C.page_channel_test.new_page_description, page.getDescription());
				assertTrue("Check if the page is inherited", page.isInherited());
				isFirstChecked = true;
			}

			// Test if the second page has the correct data and is n
			if (page.getId().equals(localCopy.getId())) {
				assertEquals("Check if page name is the same", C.page_channel_test.new_page_for_l18n_name, page.getName());
				assertEquals("Check if page filename is the same", C.page_channel_test.new_page_for_l18n_filename, page.getFileName());
				assertEquals("Check if page description is the same", C.page_channel_test.new_page_for_l18n_description, page.getDescription());
				assertTrue("Check if the page is inherited", !page.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both pages", (isFirstChecked && isSecondChecked));
	}

	/**
	 * Checks if there is at most one translation for each language of a page and its channelset
	 * @param pageId the page id where to start the test
	 * @throws NodeException
	 */
	protected void assertNoMultipleTranslations(int pageId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final Page page = t.getObject(Page.class,pageId);
		DBUtils.executeStatement("select count(*), contentgroup_id, contentset_id from page where contentset_id = ? group by contentgroup_id, contentset_id, channel_id having count(*) > 1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, page.getContentsetId());
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				assertFalse("There must not be multiple language variants with the same language", rs.next());
			}
		});
	}

	protected void testLocalizedPageTranslation(int pageCreationNodeId, int pageTranslationNodeId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		testContext.getDBSQLUtils().executeQueryManipulation("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (10, 1, 1);");
		testContext.getDBSQLUtils().executeQueryManipulation("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (10, 2, 2);");

		// Get the master pages' node.
		Node pageCreationNode = t.getObject(Node.class, pageCreationNodeId);

		// Get the root folder from the master page's node.
		Folder masterRootFolder = pageCreationNode.getFolder();

		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.page_channel_test.test_folder_name);
		testFolder.setMotherId(masterRootFolder.getId());
		if (pageCreationNode.isChannel()) {
			testFolder.setChannelInfo(pageCreationNode.getId(), testFolder.getChannelSetId());
		}

		testFolder.save();
		t.commit(false);
		t.refreshPermHandler();

		// Create test page in the root folder.
		Page masterPageGerman = t.createObject(Page.class);
		ContentLanguage german = t.getObject(ContentLanguage.class, 1);
		ContentLanguage english = t.getObject(ContentLanguage.class, 2);

		masterPageGerman.setDescription(C.page_channel_test.new_page_description);
		masterPageGerman.setFilename(C.page_channel_test.new_page_filename);
		masterPageGerman.setFolderId(testFolder.getId());
		masterPageGerman.setName(C.page_channel_test.new_page_name);
		masterPageGerman.setTemplateId(C.page_channel_test.new_page_template_id);
		masterPageGerman.setLanguage(german);

		if (pageCreationNode.isChannel()) {
			masterPageGerman.setChannelInfo(pageCreationNode.getId(), masterPageGerman.getChannelSetId());
		}

		Integer channelSetId = masterPageGerman.getChannelSetId();

		masterPageGerman.save();
		t.commit(false);

		// localize:
		Page localizedGerman = (Page) masterPageGerman.copy();
		localizedGerman.setChannelInfo(pageTranslationNodeId, channelSetId);

		localizedGerman.save();
		t.commit(false);

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(t);

		// translate the localized page in the channel
		PageLoadResponse translatedInChannel = pageResource.translate(ObjectTransformer.getInt(localizedGerman.getId(), -1), "en", true, pageTranslationNodeId);
		assertTrue("Check if translating in channel succeeded", translatedInChannel.getResponseInfo().getResponseCode().equals(ResponseCode.OK));
		t.commit(false);

		translatedInChannel = pageResource.load(Integer.toString(translatedInChannel.getPage().getId()), false, false, false, true, false,
				false, false, false, false, false, pageTranslationNodeId, null);

		assertEquals("Check for language", "en", translatedInChannel.getPage().getLanguage());
		assertFalse("Check page status", translatedInChannel.getPage().isModified());

		PageLoadResponse masterPage = pageResource.load(ObjectTransformer.getString(masterPageGerman.getId(), ""), false, false, false, true, false,
				false, false, false, false, false, ObjectTransformer.getInt(pageCreationNode.getId(), 0), null);

		assertEquals("Check that the masterpage was translated in the master node", 2, masterPage.getPage().getLanguageVariants().size());
		assertFalse("Check that translated masterpage was taken offline", masterPage.getPage().getLanguageVariants().get(english.getId()).isOnline());

		// load the master page for the channel, including language variants
		PageLoadResponse localizedMasterPage = pageResource.load(ObjectTransformer.getString(masterPageGerman.getId(), ""), false, false, false, true, false,
				false, false, false, false, false, pageTranslationNodeId, null);
		assertResponseCode(localizedMasterPage);
		assertEquals("Check localized page", localizedGerman.getId(), localizedMasterPage.getPage().getId());
		assertEquals("Check language variants of localized page", 2, localizedMasterPage.getPage().getLanguageVariants().size());
		assertEquals("Check localized translation", translatedInChannel.getPage().getId(), localizedMasterPage.getPage().getLanguageVariants().get(english.getId()).getId());

		// repeat the translate call and check whether the same page is returned
		com.gentics.contentnode.rest.model.Page localTranslation = translatedInChannel.getPage();
		translatedInChannel = pageResource.translate(ObjectTransformer.getInt(localizedGerman.getId(), -1), "en", true, pageTranslationNodeId);
		assertEquals("Check the translation after translating again", localTranslation.getId(), translatedInChannel.getPage().getId());
		assertNoMultipleTranslations(masterPageGerman.getId());
	}

	/**
	 * Test translating a localized page in a channel into another language
	 * @throws NodeException
	 * @throws SQLUtilException
	 */
	@Test
	public void testLocalizedPageTranslation() throws Exception {
		testLocalizedPageTranslation(MASTER_ID, channelId);
	}

	/**
	 * Test translating a localized page in a channel into another language. The master page is created in a channel
	 * @throws NodeException
	 * @throws SQLUtilException
	 */
	@Test
	public void testLocalizedPageTranslationFromChannel() throws Exception {
		testLocalizedPageTranslation(channelId, subChannelId);
	}

	protected void testInheritedPageTranslation(int pageCreationNodeId, int pageTranslationNodeId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// First set up languages, and a page in the master node
		testContext.getDBSQLUtils().executeQueryManipulation("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (10, 1, 1);");
		testContext.getDBSQLUtils().executeQueryManipulation("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (10, 2, 2);");

		ContentLanguage german = t.getObject(ContentLanguage.class, 1);
		ContentLanguage english = t.getObject(ContentLanguage.class, 2);
		Node pageCreateNode = t.getObject(Node.class, pageCreationNodeId);

		Folder masterRootFolder = pageCreateNode.getFolder();

		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.page_channel_test.test_folder_name);
		testFolder.setMotherId(masterRootFolder.getId());

		testFolder.save();
		t.commit(false);
		t.refreshPermHandler();

		Page masterPageGerman = t.createObject(Page.class);

		masterPageGerman.setDescription(C.page_channel_test.new_page_description);
		masterPageGerman.setFilename(C.page_channel_test.new_page_filename);
		masterPageGerman.setFolderId(testFolder.getId());
		masterPageGerman.setName(C.page_channel_test.new_page_name);
		masterPageGerman.setTemplateId(C.page_channel_test.new_page_template_id);
		masterPageGerman.setLanguage(german);

		if (pageCreateNode.isChannel()) {
			masterPageGerman.setChannelInfo(pageCreateNode.getId(), masterPageGerman.getChannelSetId());
		}

		masterPageGerman.save();
		t.commit(false);

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(t);

		// translate the page in the channel where it is inherited
		PageLoadResponse translatedInChannel = pageResource.translate(ObjectTransformer.getInt(masterPageGerman.getId(), -1), "en", true, pageTranslationNodeId);
		assertTrue("Check if translating in channel succeeded", translatedInChannel.getResponseInfo().getResponseCode().equals(ResponseCode.OK));
		t.commit(false);

		translatedInChannel = pageResource.load(Integer.toString(translatedInChannel.getPage().getId()), false, false, false, true, false,
				false, false, false, false, false, pageTranslationNodeId, null);

		assertEquals("Check for language", "en", translatedInChannel.getPage().getLanguage());
		assertFalse("Check page status", translatedInChannel.getPage().isModified());

		PageLoadResponse masterPage = pageResource.load(ObjectTransformer.getString(masterPageGerman.getId(), ""), false, false, false, true, false,
				false, false, false, false, false, ObjectTransformer.getInt(pageCreateNode.getId(), 0), null);

		assertEquals("Check that the masterpage was translated in the master node", 2, masterPage.getPage().getLanguageVariants().size());
		assertFalse("Check that translated masterpage was taken offline", masterPage.getPage().getLanguageVariants().get(english.getId()).isOnline());

		// load the master page for the channel, including language variants
		PageLoadResponse inheritedMasterPage = pageResource.load(ObjectTransformer.getString(masterPageGerman.getId(), ""), false, false, false, true, false,
				false, false, false, false, false, pageTranslationNodeId, null);
		assertResponseCode(inheritedMasterPage);
		assertEquals("Check inherited page", masterPageGerman.getId(), inheritedMasterPage.getPage().getId());
		assertEquals("Check language variants of inherited page", 2, inheritedMasterPage.getPage().getLanguageVariants().size());
		assertEquals("Check localized translation", translatedInChannel.getPage().getId(), inheritedMasterPage.getPage().getLanguageVariants().get(english.getId()).getId());

		// repeat the translate call and check whether the same page is returned
		com.gentics.contentnode.rest.model.Page localTranslation = translatedInChannel.getPage();
		translatedInChannel = pageResource.translate(ObjectTransformer.getInt(masterPageGerman.getId(), -1), "en", true, pageTranslationNodeId);
		assertEquals("Check the translation after translating again", localTranslation.getId(), translatedInChannel.getPage().getId());

		assertNoMultipleTranslations(masterPageGerman.getId());
	}

	/**
	 * Test translating an inherited page in a channel into another language
	 *
	 * @throws NodeException
	 * @throws SQLUtilException
	 */
	@Test
	public void testInheritedPageTranslation() throws Exception {
		testInheritedPageTranslation(MASTER_ID, channelId);
	}

	/**
	 * Test translating an inherited page in a channel into another language. The master page is created in a channel
	 *
	 * @throws NodeException
	 * @throws SQLUtilException
	 */
	@Test
	public void testInheritedPageTranslationChannel() throws Exception {
		testInheritedPageTranslation(channelId, subChannelId);
	}

	/**
	 * Test creating new page in a channel. Check that it's not in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testPageAsLocalObjectInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new page.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.page_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Refresh the permissions so we can use the new folder.
		t.refreshPermHandler();

		// Create test page in the channel.
		Page newPageFirst = t.createObject(Page.class);

		newPageFirst.setDescription(C.page_channel_test.new_page_description);
		newPageFirst.setFilename(C.page_channel_test.new_page_filename);
		newPageFirst.setFolderId(testFolder.getId());
		newPageFirst.setName(C.page_channel_test.new_page_name);
		newPageFirst.setTemplateId(C.page_channel_test.new_page_template_id);

		// Set channel info for the new page.
		Integer channelSetId = newPageFirst.getChannelSetId();
		newPageFirst.setChannelInfo(channelId, channelSetId);

		newPageFirst.save();
		t.commit(false);

		// Get folder resource so we could use rest api to retrieve pages.
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();

		// Get all pages for our test folder in the channel we are working in.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));

		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		PageListParameterBean pageListParameterBean = new PageListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		PublishableParameterBean publishableParameterBean = new PublishableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		PageListResponse pageListResponse = pageResource.list(
			inFolder,
			pageListParameterBean.clone().setNodeId(channelId),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			publishableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, pageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the page list is null", pageListResponse.getItems());
		assertTrue("Check if the page list contains elements", pageListResponse.getItems().size() == 1);
		assertEquals("Check if page folder id is the same", testFolder.getId(), pageListResponse.getItems().get(0).getFolderId());
		assertEquals("Check if page template id is the same", Integer.valueOf(C.page_channel_test.new_page_template_id), pageListResponse.getItems().get(0).getTemplateId());
		assertEquals("Check if page name is the same", C.page_channel_test.new_page_name, pageListResponse.getItems().get(0).getName());
		assertEquals("Check if page filename is the same", C.page_channel_test.new_page_filename, pageListResponse.getItems().get(0).getFileName());
		assertEquals("Check if page description is the same", C.page_channel_test.new_page_description, pageListResponse.getItems().get(0).getDescription());
		assertTrue("Check if the page is inherited", !pageListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoints.
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		LegacyPageListResponse legacyPageListResponse = folderResource.getPages(
			inFolder.folderId,
			inFolder,
			pageListParameterBean.clone().setNodeId(channelId),
			filter,
			sort,
			paging,
			publishableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyPageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the page list is null", legacyPageListResponse.getPages());
		assertTrue("Check if the page list contains elements", legacyPageListResponse.getPages().size() == 1);
		assertEquals("Check if page folder id is the same", testFolder.getId(), legacyPageListResponse.getPages().get(0).getFolderId());
		assertEquals("Check if page template id is the same", Integer.valueOf(C.page_channel_test.new_page_template_id), legacyPageListResponse.getPages().get(0).getTemplateId());
		assertEquals("Check if page name is the same", C.page_channel_test.new_page_name, legacyPageListResponse.getPages().get(0).getName());
		assertEquals("Check if page filename is the same", C.page_channel_test.new_page_filename, legacyPageListResponse.getPages().get(0).getFileName());
		assertEquals("Check if page description is the same", C.page_channel_test.new_page_description, legacyPageListResponse.getPages().get(0).getDescription());
		assertTrue("Check if the page is inherited", !legacyPageListResponse.getPages().get(0).isInherited());

		// Get all pages for test folder in the master node.
		pageListResponse = pageResource.list(
			inFolder,
			pageListParameterBean,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			publishableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, pageListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the page list contains elements", pageListResponse.getItems().size() == 0);

		// Check again with legacy endpoint.
		legacyPageListResponse = folderResource.getPages(
			inFolder.folderId,
			inFolder,
			pageListParameterBean,
			filter,
			sort,
			paging,
			publishableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, legacyPageListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the page list contains elements", legacyPageListResponse.getPages().size() == 0);
	}

	/**
	 * Test creating a page in a channel and then localizing it in a subchannel
	 * @throws NodeException
	 */
	@Test
	public void testLocalizingPageFromChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new page.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.page_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Create test page in the channel.
		Page channelPage = t.createObject(Page.class);

		channelPage.setDescription(C.page_channel_test.new_page_description);
		channelPage.setFilename(C.page_channel_test.new_page_filename);
		channelPage.setFolderId(testFolder.getId());
		channelPage.setName(C.page_channel_test.new_page_name);
		channelPage.setTemplateId(C.page_channel_test.new_page_template_id);

		// Set channel info for the new page.
		Integer channelSetId = channelPage.getChannelSetId();
		channelPage.setChannelInfo(channelId, channelSetId);

		channelPage.save();
		t.commit(false);

		// localize the page in the subchannel
		Page subChannelPage = (Page) channelPage.copy();

		subChannelPage.setChannelInfo(subChannelId, channelSetId);
		subChannelPage.save();
		t.commit(false);

		// now check whether the page in the channel is a master and in the subchannel, it is inherited
		assertTrue("Check whether page in channel is a master", channelPage.isMaster());
		assertFalse("Check whether page in subchannel is no master", subChannelPage.isMaster());
	}

	/**
	 * Test deleting a local and an inherited page in a channel
	 * @throws Exception
	 */
	@Test
	public void testdeletePageFromChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// get the channel node that inherits the master node
		Node channel = t.getObject(Node.class, channelId);

		// get the root folder of the master
		Folder masterNodeFolder = masterNode.getFolder();

		// get the root folder of the channel
		Folder channelFolder = channel.getFolder();

		// Create a local test page in the channel
		Page localPage = t.createObject(Page.class);

		localPage.setName(C.page_channel_test.new_page_name);
		localPage.setFolderId(channelFolder.getId());
		localPage.setTemplateId(C.page_channel_test.new_page_template_id);

		// Set channel info for the new page.
		Integer channelSetId = localPage.getChannelSetId();
		localPage.setChannelInfo(channelId, channelSetId);

		localPage.save();
		t.commit(false);

		// Create an inherited test folder in the master
		Page inheritedPage = t.createObject(Page.class);

		inheritedPage.setName(C.page_channel_test.new_page_name);
		inheritedPage.setFolderId(masterNodeFolder.getId());
		inheritedPage.setTemplateId(C.page_channel_test.new_page_template_id);

		inheritedPage.save();
		t.commit(false);

		// now use the REST API to get the folders in the channel
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(t);

		// Test deleting the local folder in the channel
		assertEquals("Check response code", ResponseCode.OK, pageResource.delete(localPage.getId().toString(), channelId, null).getResponseInfo().getResponseCode());

		// Test deleting the inherited folder in the channel from the master (should error out)
		assertEquals("Check response code", ResponseCode.FAILURE,
				pageResource.delete(inheritedPage.getId().toString(), channelId, null).getResponseInfo().getResponseCode());

		// Delete from the master
		assertEquals("Check response code", ResponseCode.OK,
				pageResource.delete(inheritedPage.getId().toString(), MASTER_ID, null).getResponseInfo().getResponseCode());
	}

	/**
	 * Tests preview local page in a channel
	 * @throws Exception
	 */
	@Test
	public void testPagePreview() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page localPage = createLocalPageInLocalFolder(t, "Preview <node page.url><node page.name>");

		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(t);

		PagePreviewRequest pageRequest = new PagePreviewRequest();
		com.gentics.contentnode.rest.model.Page page = ModelBuilder.getPage(localPage, (Collection<Reference>) null);
		pageRequest.setPage(page);

		PagePreviewResponse pagePreviewResponse = pageResource.preview(pageRequest);

		assertEquals("Check response OK", ResponseCode.OK, pagePreviewResponse.getResponseInfo().getResponseCode());
		assertEquals("Check preview", "Preview #SomeName", pagePreviewResponse.getPreview());
	}

	/**
	 * Creates a local page in a local folder.
	 * @param transaction
	 * @param templateSource
	 * @return
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws InsufficientPrivilegesException
	 * @throws TransactionException
	 */
	private Page createLocalPageInLocalFolder(Transaction transaction, String templateSource) throws NodeException,
			ReadOnlyException, InsufficientPrivilegesException,
			TransactionException {
		// get the channel node that inherits the master node
		Node channel = transaction.getObject(Node.class, channelId);

		// get the root folder of the channel
		Folder channelFolder = channel.getFolder();

		Folder localFolder = transaction.createObject(Folder.class);
		localFolder.setName("Local folder");
		localFolder.setMotherId(channelFolder.getId());
		localFolder.setChannelInfo(channelId, localFolder.getChannelSetId());

		localFolder.save();
		transaction.commit(false);

		// create a master template with 1 page-editable tag
		Template masterTemplate = transaction.createObject(Template.class);
		masterTemplate.setFolderId(localFolder.getId());
		masterTemplate.setName("Master template");
		masterTemplate.setSource(templateSource);

		masterTemplate.save();

		// Create a local test page in the channel
		Page localPage = transaction.createObject(Page.class);

		localPage.setName(C.page_channel_test.new_page_name);
		localPage.setFolderId(localFolder.getId());
		localPage.setTemplateId(masterTemplate.getId());

		// Set channel info for the new page.
		Integer channelSetId = localPage.getChannelSetId();

		localPage.setChannelInfo(channelId, channelSetId);

		localPage.save();
		transaction.commit(false);
		return localPage;
	}

	/**
	 * test creating two pages with the same name in two unrelated channel nodes (no channel is an ancestor of the other)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testSideChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node sideChannel = t.getObject(Node.class, sideChannelId);
		Folder channelFolder = channel.getFolder();
		Folder sideChannelFolder = sideChannel.getFolder();

		Page p1 = t.createObject(Page.class);

		p1.setChannelInfo(channel.getId(), null);
		p1.setName("testpage");
		p1.setFolderId(channelFolder.getId());
		p1.setTemplateId(C.page_channel_test.new_page_template_id);

		Page p2 = t.createObject(Page.class);

		p2.setChannelInfo(sideChannel.getId(), null);
		p2.setName("testpage");
		p2.setFolderId(sideChannelFolder.getId());
		p2.setTemplateId(C.page_channel_test.new_page_template_id);

		p1.save();
		p2.save();

		assertEquals("testpage", p1.getName());
		assertEquals("testpage", p2.getName());
	}

	/**
	 * test creating two pages with the same name in two related channel nodes (second is an descendant of the first)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDescendantChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node subChannel = t.getObject(Node.class, subChannelId);
		Folder channelFolder = channel.getFolder();
		Folder subChannelFolder = subChannel.getFolder();

		Page p1 = t.createObject(Page.class);

		p1.setChannelInfo(channel.getId(), null);
		p1.setName("testpage");
		p1.setFolderId(channelFolder.getId());
		p1.setTemplateId(C.page_channel_test.new_page_template_id);

		Page p2 = t.createObject(Page.class);

		p2.setChannelInfo(subChannel.getId(), null);
		p2.setName("testpage");
		p2.setFolderId(subChannelFolder.getId());
		p2.setTemplateId(C.page_channel_test.new_page_template_id);

		p1.save();
		p2.save();

		assertEquals("testpage", p1.getName());
		assertEquals("testpage 1", p2.getName());
	}

	/**
	 * test creating two pages with the same name in two related channel nodes (second is an ancestor of the first)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testAncestorChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node subChannel = t.getObject(Node.class, subChannelId);
		Folder channelFolder = channel.getFolder();
		Folder subChannelFolder = subChannel.getFolder();

		Page p1 = t.createObject(Page.class);

		p1.setChannelInfo(subChannel.getId(), null);
		p1.setName("testpage");
		p1.setFolderId(subChannelFolder.getId());
		p1.setTemplateId(C.page_channel_test.new_page_template_id);

		Page p2 = t.createObject(Page.class);

		p2.setChannelInfo(channel.getId(), null);
		p2.setName("testpage");
		p2.setFolderId(channelFolder.getId());
		p2.setTemplateId(C.page_channel_test.new_page_template_id);

		p1.save();
		p2.save();

		assertEquals("testpage", p1.getName());
		assertEquals("testpage 1", p2.getName());
	}

	/**
	 * Test whether multichanneling is performed correctly when selecting a
	 * template and copying its page-editable tags using REST API (page/create).
	 * Related to RT#54768
	 *
	 * @throws NodeException
	 */
	@Test
	public void testPageCreateTemplateMultiChanneling() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		Folder masterFolder = masterNode.getFolder();

		// create a construct with a simple html part
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("testtag");
		construct.setName("Test (de)", 1);
		construct.setName("Test (en)", 2);
		construct.getNodes().add(masterNode);

		Part htmlPart = t.createObject(Part.class);
		htmlPart.setKeyname("html");
		htmlPart.setHidden(false);
		htmlPart.setEditable(1);
		htmlPart.setName("html", 1);
		htmlPart.setName("html", 2);
		htmlPart.setPartOrder(1);
		htmlPart.setPartTypeId(10);

		List<Part> parts = construct.getParts();
		parts.add(htmlPart);

		construct.save();

		// create a master template with 1 page-editable tag
		Template masterTemplate = t.createObject(Template.class);
		masterTemplate.setFolderId(masterFolder.getId());
		masterTemplate.setName("Master template");
		masterTemplate.setSource("<node tag1>");

		TemplateTag tag1 = t.createObject(TemplateTag.class);

		tag1.setConstructId(construct.getId());
		tag1.setPublic(true);
		tag1.setName("tag1");

		masterTemplate.getTags().put("tag1", tag1);

		masterTemplate.save();

		// create a channel template with 2 page-editable tags
		Template channelTemplate = (Template) masterTemplate.copy();
		channelTemplate.setChannelInfo(channelId, masterTemplate.getChannelSetId());
		channelTemplate.setName("Channel template");
		channelTemplate.setSource("<node tag1><node tag2>");

		TemplateTag tag2 = (TemplateTag) tag1.copy();
		tag2.setName("tag2");

		channelTemplate.getTags().put("tag2", tag2);

		channelTemplate.save();

		t.commit(false);

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(t);

		// create a page in the master channel
		PageCreateRequest req = new PageCreateRequest();
		req.setFolderId(Integer.toString(ObjectTransformer.getInt(masterFolder.getId(), -1)));
		req.setNodeId(MASTER_ID);
		// We swap channel and master templates to see if multichanneling
		// fallback is applied correctly
		req.setTemplateId(ObjectTransformer.getInt(channelTemplate.getId(), -1));

		PageLoadResponse masterResponse = pageResource.create(req);

		t.commit(false);
		pageResource.setTransaction(t);

		// create a page in the channel
		req = new PageCreateRequest();
		req.setFolderId(Integer.toString(ObjectTransformer.getInt(masterFolder.getId(), -1)));
		req.setNodeId(channelId);
		// We swap channel and master templates to see if multichanneling
		// fallback is applied correctly
		req.setTemplateId(ObjectTransformer.getInt(masterTemplate.getId(), -1));

		PageLoadResponse channelResponse = pageResource.create(req);

		assertNotNull(masterResponse.getPage());
		assertTrue(masterResponse.getPage().getTags().containsKey("tag1"));
		assertFalse(masterResponse.getPage().getTags().containsKey("tag2"));

		assertNotNull(channelResponse.getPage());
		assertTrue(channelResponse.getPage().isInherited() == false);
		assertTrue(channelResponse.getPage().getTags().containsKey("tag1"));
		assertTrue(channelResponse.getPage().getTags().containsKey("tag2"));

	}
	/**
	 * Test whether links to a localized object are updated properly
	 *
	 * @throws Exception
	 */
	@Test
	public void testHiding() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create Nodes/Channels
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		Folder masterRootFolder = masterNode.getFolder();

		Node channelNode = t.getObject(Node.class, channelId);
		Folder channelRootFolder = channelNode.getFolder();

		// create a construct
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(masterNode);
		construct.getNodes().add(channelNode);
		Part urlPart = t.createObject(Part.class);

		urlPart.setKeyname("url");
		urlPart.setHidden(false);
		urlPart.setEditable(1);
		urlPart.setName("Url", 1);
		urlPart.setName("Url", 2);
		urlPart.setPartOrder(1);
		urlPart.setPartTypeId(4);
		List<Part> parts = construct.getParts();

		parts.add(urlPart);
		construct.save();

		// create a template
		Template template = t.createObject(Template.class);
		template.setFolderId(masterRootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// Create Target page
		Page targetPage = t.createObject(Page.class);
		targetPage.setTemplateId(template.getId());
		targetPage.setFolderId(masterRootFolder.getId());
		targetPage.save();
		targetPage.publish();

		// Create Link page
		final Page linkPage = t.createObject(Page.class);
		linkPage.setTemplateId(template.getId());
		linkPage.setFolderId(channelRootFolder.getId());

		ContentTag tag = linkPage.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));
		tag.setName("link1");
		tag.setEnabled(true);
		PartType partType = tag.getValues().getByKeyname("url").getPartType();
		if (partType instanceof PageURLPartType) {
			((PageURLPartType) partType).setTargetPage(targetPage);
		} else {
			fail("Wrong parttype " + partType.getClass());
		}

		linkPage.save();
		linkPage.publish();
		t.commit(false);

		// Publish
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());
		testContext.getContext().publish(false, true);

		// Create Localized Link page
		Page subChannelPage = (Page) targetPage.copy();
		subChannelPage.setChannelInfo(channelId, targetPage.getChannelSetId());
		subChannelPage.save();
		subChannelPage.publish();
		t.commit(false);

		// Check Publish table
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		Map<Integer, Integer> expectedPublishQueueEntries = new HashMap<Integer, Integer>();
		expectedPublishQueueEntries.put(MASTER_ID, 0);
		expectedPublishQueueEntries.put(channelId, 1);
		expectedPublishQueueEntries.put(subChannelId, 1);
		expectedPublishQueueEntries.put(sideChannelId, 0);

		for (Map.Entry<Integer, Integer> entry : expectedPublishQueueEntries.entrySet()) {
			final int checkChannelId = entry.getKey();
			final int expected = entry.getValue();

			DBUtils.executeStatement("select count(*) from publishqueue where action = ? and obj_type = ? and obj_id = ? and channel_id = ?", new SQLExecutor() {
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setString(1, "DEPENDENCY");
					stmt.setInt(2, Page.TYPE_PAGE);
					stmt.setObject(3,linkPage.getId());
					stmt.setInt(4, checkChannelId);
				}
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					rs.next();
					assertThat(rs.getInt(1)).as("DEPENDENCY entries in publishtable for channel " + checkChannelId).isGreaterThanOrEqualTo(expected);
				}
			});
		}
	}

	/**
	 * Test creation of a master page in a local folder.
	 * This is not possible, so the page must automatically be created in the channel of the local folder.
	 * @throws Exception
	 */
	@Test
	public void testCreateMasterPageInLocalFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create local test folder in the channel
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("Local folder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.setChannelInfo(channelId, testFolder.getChannelSetId());

		testFolder.save();
		t.commit(false);

		// create a master page in the local test folder
		Page newPage = t.createObject(Page.class);
		newPage.setFolderId(testFolder.getId());
		newPage.setName(C.page_channel_test.new_page_name);
		newPage.setTemplateId(C.page_channel_test.new_page_template_id);
		newPage.save();
		t.commit(false);

		// Get folder resource so we can use rest api to retrieve files.
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();

		// get all inherited pages
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(ObjectTransformer.getString(testFolder.getId(), null));
		PageListParameterBean pageListParameterBean = new PageListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		PublishableParameterBean publishableParameterBean = new PublishableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		PageListResponse inheritedPagesResponse = pageResource.list(
			inFolder,
			pageListParameterBean.setInherited(true),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			publishableParameterBean,
			wastebinParameterBean);
		assertResponseCode(inheritedPagesResponse);
		assertEquals("Check # of inherited pages", 0, inheritedPagesResponse.getItems().size());

		// Check again with legacy endpoint.
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		LegacyPageListResponse legacyInheritedPagesResponse = folderResource.getPages(
			inFolder.folderId,
			inFolder,
			pageListParameterBean.setInherited(true),
			filter,
			sort,
			paging,
			publishableParameterBean,
			wastebinParameterBean);
		assertResponseCode(legacyInheritedPagesResponse);
		assertEquals("Check # of legacyInherited pages", 0, legacyInheritedPagesResponse.getPages().size());

		// get all local pages
		PageListResponse localPagesResponse = pageResource.list(
			inFolder,
			pageListParameterBean.setInherited(false),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			publishableParameterBean,
			wastebinParameterBean);
		assertResponseCode(localPagesResponse);
		assertEquals("Check # of local pages", 1, localPagesResponse.getItems().size());

		// Check again with legacy endpoint.
		LegacyPageListResponse legacyLocalPagesResponse = folderResource.getPages(
			inFolder.folderId,
			inFolder,
			pageListParameterBean.setInherited(false),
			filter,
			sort,
			paging,
			publishableParameterBean,
			wastebinParameterBean);
		assertResponseCode(localPagesResponse);
		assertEquals("Check # of local pages", 1, legacyLocalPagesResponse.getPages().size());

	}
}
