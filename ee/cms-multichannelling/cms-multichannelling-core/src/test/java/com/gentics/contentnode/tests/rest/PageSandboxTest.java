package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests various methods used for pages using the REST API.
 * 
 * @author taylor
 */
public class PageSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Valid page ID
	 */
	private static final String PAGE_ID = "45";

	/**
	 * Expected page online status
	 */
	private static final boolean PAGE_ONLINE = true;

	/**
	 * Expected page modified status
	 */
	private static final boolean PAGE_MODIFIED = false;

	/**
	 * Set some features
	 */
	@Before
	public void setUp() throws Exception {
		// activate multichannelling
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
	}

	private Page createAndPublishPage() throws Exception {
		// Create page object
		Transaction t = testContext.startTransaction(1);
		Node node = t.createObject(Node.class);
		node.setHostname("blah");
		node.setPublishDir("/");
		node.setPublishFilesystem(true);

		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName("testnode");
		rootFolder.setPublishDir("/");

		node.setFolder(rootFolder);
		node.save();

		Template template = Creator.createTemplate("testtemplate", "Dies ist ein Test", rootFolder);

		Page page = Creator.createPage("somePage", rootFolder, template, null);
		page.save();
		page.publish();

		testContext.publish(true);
		return page;
	}

	@Test
	public void testSearchPageByLiveUrl() throws Exception {
		Page page = createAndPublishPage();
		String liveUrl = "blah/" + page.getFilename();
		PageLoadResponse response = getPageResource().search(false, false, false, false, false, false, false, false, false, page.getNode().getId(), liveUrl);
		assertEquals("Check for the correct response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertNotNull("The page should have been found.", response.getPage());
		assertEquals(page.getFilename(), response.getPage().getFileName());

		response = getPageResource().search(false, false, false, false, false, false, false, false, false, page.getNode().getId(), liveUrl);
		assertEquals("Check for the correct response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertNotNull("The page should have been found.", response.getPage());
		assertEquals(page.getFilename(), response.getPage().getFileName());

		response = getPageResource().search(false, false, false, false, false, false, false, false, false, page.getNode().getId(), "bogus");
		assertEquals("Check for the correct response code", ResponseCode.NOTFOUND, response.getResponseInfo().getResponseCode());

		response = getPageResource().search(false, false, false, false, false, false, false, false, false, null, "bogus");
		assertEquals("Check for the correct response code", ResponseCode.NOTFOUND, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Tests getting the page status
	 */
	@Test
	public void testGetPageStatus() throws Exception {

		// load the page
		com.gentics.contentnode.rest.model.Page page = getPageResource().load(PAGE_ID, false, false, false, false, false, false, false, false, false, false, null, null).getPage();

		assertNotNull("Check for the page {" + PAGE_ID + "}", page);
		assertEquals("Check for the correct online flag for page {" + PAGE_ID + "}", PAGE_ONLINE, page.isOnline());
		assertEquals("Check for the correct modified flag for page {" + PAGE_ID + "}", PAGE_MODIFIED, page.isModified());
	}

	/**
	 * Users should be able to translate a page without having update rights on the source language.
	 * Also, translation rights should be required on the to create a translation.
	 * Roles are used to assign different rights to different languages.
	 * This is the non-multichannelling version of the test method with the same name in TranslationRightsTest
	 * @throws NodeException
	 */
	@Test
	public void testTranslationRights() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create node with languages assigned + folder + template + page
		ContentLanguage startLanguage = Creator.createLanguage("Klingon", "tlh");
		ContentLanguage destLanguage = Creator.createLanguage("Tengwar", "teng");
		ContentLanguage destLanguage2 = Creator.createLanguage("Interlingua", "ina");

		Node node = Creator.createNode("testnode", "blah", "/", "/", Arrays.asList(new ContentLanguage[]{startLanguage, destLanguage, destLanguage2}));

		Folder rootFolder = node.getFolder();

		Template template = Creator.createTemplate("testtemplate", "Dies ist ein Test", rootFolder);

		Page page = Creator.createPage(null, rootFolder, template, startLanguage);
		page.unlock();

		// Create group with no rights to pages
		UserGroup translatorGroup = Creator.createUsergroup("testtranslator", "", t.getObject(UserGroup.class, 1));

		// No permissions on Pages/Images/Files
		String permissions = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_FOLDER_CREATE, PermHandler.PERM_FOLDER_UPDATE,
				PermHandler.PERM_FOLDER_DELETE, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_CREATE, PermHandler.PERM_TEMPLATE_UPDATE,
				PermHandler.PERM_TEMPLATE_DELETE, PermHandler.PERM_TEMPLATE_LINK).toString();
		PermHandler.setPermissions(Folder.TYPE_FOLDER, rootFolder.getId(), Arrays.asList(new UserGroup[] { translatorGroup }), permissions);
		PermHandler.setPermissions(Node.TYPE_NODE, rootFolder.getId(), Arrays.asList(new UserGroup[] { translatorGroup }), permissions);

		// Create roles with read permission on lang #1 and all permissions on lang#2
		Map<ContentLanguage,String> languagePermissions = new HashMap<ContentLanguage, String>();

		Permission viewPerm = new Permission(PermHandler.ROLE_VIEW);
		Permission allPerm = new Permission(PermHandler.ROLE_VIEW, PermHandler.ROLE_UPDATE, PermHandler.ROLE_TRANSLATE);
		languagePermissions.put(startLanguage, viewPerm.toString());
		languagePermissions.put(destLanguage,  allPerm.toString());
		languagePermissions.put(destLanguage2, viewPerm.toString());

		int roleId = Creator.createRole("testrole", languagePermissions, null, null);

		// Assign role & group to folder
		int roleUserGroupId = DBUtils.executeInsert("insert into role_usergroup (role_id, usergroup_id) values (?,?)", new Object[] { roleId, translatorGroup.getId() }).get(0);
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId, Folder.TYPE_FOLDER, rootFolder.getId() });
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId, Node.TYPE_NODE, rootFolder.getId() });

		// Create new user
		SystemUser translationUser = Creator.createUser("testtranslator", "testtranslator", "test", "translator", "blah@rg.hh", Arrays.asList(new UserGroup[]{translatorGroup}));

		// Commit & restart transaction as testtranslator
		testContext.getContext().startTransaction(translationUser.getId());
		t = testContext.getContext().getTransaction();

		// Re-synchronize Permission store with database contents changed previously
		PermissionStore.initialize(true);

		assertEquals("Current User ID must match testtranslator's ID", translationUser.getId().intValue(), t.getUserId());

		PageLoadResponse result = getPageResource().translate(page.getId(), destLanguage.getCode(), true, 0);

		assertEquals("translation operation must be successful", ResponseCode.OK, result.getResponseInfo().getResponseCode());

		// translate page to language without permissions
		result = getPageResource().translate(page.getId(), destLanguage2.getCode(), true, 0);

		assertEquals("translation must fail", ResponseCode.PERMISSION, result.getResponseInfo().getResponseCode());

		t.commit();
	}

	/**
	 * Test if we can corrupt the page version list by changing its order
	 * @throws NodeException
	 */
	@Test
	public void testPageVersionCacheCorruption() throws NodeException {

		// Create page object
		Transaction t = testContext.startTransaction(1);
		Node node = t.createObject(Node.class);
		node.setHostname("blah");
		node.setPublishDir("/");

		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName("testnode");
		rootFolder.setPublishDir("/");

		node.setFolder(rootFolder);
		node.save();

		Template template = Creator.createTemplate("testtemplate", "Dies ist ein Test", rootFolder);

		Page page = Creator.createPage(null, rootFolder, template, null);

		testContext.startTransaction(2);

		// Create some versions
		page.setName("test1");
		page.save();

		testContext.startTransaction(3);

		page.setName("test2");
		page.save();

		page.publish(4, null);

		testContext.startTransaction(5);

		// try to corrupt pageversion list
		NodeObjectVersion[] original = page.getVersions();
		assertTrue("Must have at least 2 versions", original.length > 1);

		NodeObjectVersion[] backup = original.clone();

		Collections.reverse(Arrays.asList(original));

		NodeObjectVersion[] newresult = page.getVersions();
		assertArrayEquals("corrupted page cache", backup, newresult);
	}

	@Test
	public void testLoadChannelData() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node master = ContentNodeTestDataUtils.createNode("Master", "master", "/Content.Node", null, false, false);
		Node channel = ContentNodeTestDataUtils.createChannel(master, "Channel",  "channel", "/Content,Node");

		Template template = t.createObject(Template.class);
		template.setName("Template");
		template.setMlId(1);
		template.setSource("<node page.name>");
		template.getFolders().add(master.getFolder());
		template.save();
		t.commit(false);

		// create a master page
		PageCreateRequest create = new PageCreateRequest();
		create.setNodeId(ObjectTransformer.getInteger(master.getId(), null));
		create.setFolderId(ObjectTransformer.getString(master.getFolder().getId(), null));
		create.setTemplateId(ObjectTransformer.getInteger(template.getId(), null));
		PageLoadResponse masterPageResponse = getPageResource().create(create);

		// check master page mc settings
		com.gentics.contentnode.rest.model.Page masterPage = masterPageResponse.getPage();
		assertNotNull("The created page must not be null", masterPage);
		assertNotNull("ContentId must be set", masterPage.getContentId());
		assertEquals("The page was not part of the master",master.getId(),masterPage.getInheritedFromId());
		assertEquals("The page's master was not correct.",master.getId(),masterPage.getMasterNodeId());
		assertNotNull("ChannelsetId must be set", masterPage.getChannelSetId());
		assertNull("ChannelId must be empty", masterPage.getChannelId());
		assertTrue("Page must be a master", masterPage.isMaster());

		// localize the page
		Page localizedCopy = (Page)t.getObject(Page.class, masterPage.getId()).copy();
		localizedCopy.setChannelInfo(channel.getId(), t.getObject(Page.class, masterPage.getId()).getChannelSetId());
		localizedCopy.save();
		t.commit(false);

		PageLoadResponse localizedPageResponse = getPageResource().load(ObjectTransformer.getString(masterPage.getId(), null), false, false, false, false, false, false, false, false, false, false, ObjectTransformer.getInteger(channel.getId(), 0), null);
		com.gentics.contentnode.rest.model.Page localizedPage = localizedPageResponse.getPage();
		assertNotNull("Localized page must not be null", localizedPage);
		assertNotNull("ContentId must be set", localizedPage.getContentId());
		assertEquals("The page was not inherted from the channel instead of the master.",channel.getId(),localizedPage.getInheritedFromId());
		assertEquals("The masterId of the page is still the master",master.getId(), masterPage.getMasterNodeId());
		assertEquals("ChannelsetId must be equal", masterPage.getChannelSetId(), localizedPage.getChannelSetId());
		assertEquals("ChannelId must be ok", channel.getId(), localizedPage.getChannelId());
		assertFalse("Page must not be a master", localizedPage.isMaster());
	}

	/**
	 * Test if a TransactionalPageCreateVersion prevents deletion of a page
	 * via REST-API (RT#58521)
	 * @throws NodeException
	 */
	@Test
	public void testDeleteWithTransactionalCreatePageVersion() throws NodeException {
		Transaction t = testContext.startTransaction(1);
		Node node = Creator.createNode("blah", "blah", "/", "/", Arrays.asList(new ContentLanguage[] {}));
		Folder rootFolder = node.getFolder();

		Template template = Creator.createTemplate("testtemplate", "Dies ist ein Test", rootFolder);

		Page page = Creator.createPage(null, rootFolder, template, null);

		t = testContext.startTransaction(2);

		/* Add another templatetag that must have its corresponding
		 * contenttag added to the page before it can be edited.
		 */

		Part textPart = Creator.createTextPartUnsaved("dingens", 21, 2, "asdf");
		Construct construct = Creator.createConstruct("dingens", "dingens", "dingens", Arrays.asList(new Part[] { textPart }));

		TemplateTag tt = t.createObject(TemplateTag.class);
		tt.setPublic(true);
		tt.setConstructId(construct.getId());
		tt.setName("dingens");

		template.getTemplateTags().put("dingens", tt);

		template.save();

		t = testContext.startTransaction(3);

		PageResourceImpl pri = new PageResourceImpl();
		pri.delete(page.getId().toString(), null, null);

		t = testContext.startTransaction(4);

		assertTrue("The page must be gone after deletion", t.getObject(Page.class, page.getId()) == null);
	}

	/**
	 * Check whether creating a new tag tries to overwrite an existing
	 * non-editable template tag with the same name.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testAddContentTagWithTemplateTagName() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = Creator.createNode("blah-test-node", "blah-test", "/", "/", Collections.<ContentLanguage>emptyList());
		Construct c = Creator.createConstruct("irrelevantconstruct", "test.png", "blah", Arrays.asList(Creator.createTextPartUnsaved("text", 1, 1, "xxx")));
		Folder rootFolder = node.getFolder();

		Template tpl = Creator.createTemplate("newtesttemplate", "the source", rootFolder);
		TemplateTag tt = t.createObject(TemplateTag.class);
		tt.setConstructId(c.getId());
		tt.setPublic(false);
		tt.setName("irrelevantconstruct1");
		tpl.getTemplateTags().put("irrelevantconstruct1", tt);
		tpl.save();
		t.commit(false);

		Page page = Creator.createPage("testpage", rootFolder, tpl, null);
		ContentTag result = page.getContent().addContentTag((Integer)c.getId());
		assertFalse("New ContentTags must not reuse names of TemplateTags that are not editable in page.","irrelevantconstruct1".equals(result.getName()));
	}

	/**
	 * Creates a page, tries loading and saving different settings of
	 * multichnanelling exclusion and disinheriting.
	 *
	 * @throws NodeException
	 */
	@Test
	public void pageMcExcludeTest() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentLanguage> noLanguage = Collections.emptyList();
		Node master = Creator.createNode("blub", "blub", "/", "/", noLanguage);
		Node channel = Creator.createNode("blub2", "blub2", "/", "/", noLanguage, master);
		Template tpl = Creator.createTemplate("template", "hello world", master.getFolder());

		// Create a page and check the default settings
		Page p = Creator.createPage("testplage", master.getFolder(), tpl);
		assertEquals("Exclusion state must be false by default", false, p.isExcluded());
		assertTrue("Disinherit list must be empty by default", p.getDisinheritedChannels().isEmpty());

		// Set a channel disinheritment
		p = t.getObject(Page.class, p.getId(), true);
		p.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(new Node[]{channel})), false);

		p = t.getObject(Page.class, p.getId());
		assertFalse("page must not be excluded", p.isExcluded());
		assertFalse("page must disinherit something", p.getDisinheritedChannels().isEmpty());
		assertEquals("channel node must be disinherited", channel, p.getDisinheritedChannels().iterator().next());

		// Set multichannelling exclusion (removes disinheriting)
		p = t.getObject(Page.class, p.getId(), true);
		p.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
		p = t.getObject(Page.class, p.getId());
		assertTrue("page must be excluded", p.isExcluded());
		assertTrue("page must disinherit nothing", p.getDisinheritedChannels().isEmpty());
	}
}
