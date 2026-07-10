package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Users should be able to translate a page without having update rights on the source language.
 * Also, translation rights should be required on the to create a translation.
 * Roles are used to assign different rights to different languages.
 * @throws NodeException
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class TranslationRightsTests {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/*
	 * These do not change throughout the variations
	 */
	private static ContentLanguage startLanguage;
	private static ContentLanguage destLanguage;
	private static ContentLanguage destLanguage2;
	private static UserGroup translatorGroup;
	private static SystemUser translationUser;
	private static int roleId;

	/*
	 * variables used in each variation
	 */
	private Page page;
	private Folder rootFolder;
	private Node createChannel;
	private Node translateChannel;
	private boolean checkMasterChannel;
	private boolean localize;
	private Node master;
	private Node channel;

	/*
	 * variation constructor arguments
	 */
	private PageType pageType;
	private boolean singleChannelPermissions;
	private boolean lock;

	/**
	 * Setup languages and a translator's role
	 * @throws NodeException
	 */
	@BeforeClass
	public static void staticInitialize() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		testContext.getContext().stopDirtQueueWorker();

		startLanguage = Creator.createLanguage("Klingon", "tlh");
		destLanguage = Creator.createLanguage("Tengwar", "teng");
		destLanguage2 = Creator.createLanguage("Interlingua", "ina");
		translatorGroup = Creator.createUsergroup("testtranslator", "", t.getObject(UserGroup.class, 1));
		translationUser = Creator.createUser("testtranslator", "testtranslator", "test", "translator", "blah@rg.hh",
				Arrays.asList(new UserGroup[] { translatorGroup }));

		Map<ContentLanguage, String> languagePermissions = new HashMap<ContentLanguage, String>();

		Permission viewPerm = new Permission(PermHandler.ROLE_VIEW);
		Permission allPerm = new Permission(PermHandler.ROLE_VIEW, PermHandler.ROLE_UPDATE, PermHandler.ROLE_TRANSLATE);
		languagePermissions.put(startLanguage, viewPerm.toString());
		languagePermissions.put(destLanguage, allPerm.toString());
		languagePermissions.put(destLanguage2, viewPerm.toString());
		roleId = Creator.createRole("testrole", languagePermissions, null, null);
	}

	/***
	 * Test constructor
	 * @param pageType the kind of page to translate
	 * @param singleChannelPermissions whether the permissions should be restricted to the channel containing the page to translate
	 * @param lock passed on as lock parameter to the REST-API
	 */
	public TranslationRightsTests(PageType pageType, boolean singleChannelPermissions, boolean lock) {
		this.pageType = pageType;
		this.singleChannelPermissions = singleChannelPermissions;
		this.lock = lock;
	}

	/**
	 * setup nodes, folders, templates, pages, permissions and a test user in the translator group
	 * @throws NodeException
	 */
	@Before
	public void initialize() throws NodeException {
		testContext.getContext().startTransaction();
		master = Creator.createNode("testnode", "blah", "/", "/", Arrays.asList(new ContentLanguage[] { startLanguage, destLanguage, destLanguage2 }));
		channel = Creator.createNode("channel", "blah2", "/", "/", Arrays.asList(new ContentLanguage[] { startLanguage, destLanguage, destLanguage2 }), master);
		switch (pageType) {
		case INHERITED:
			createChannel = master;
			translateChannel = channel;
			checkMasterChannel = true;
			break;
		case LOCAL:
			createChannel = channel;
			translateChannel = channel;
			break;
		case LOCALIZED:
			createChannel = master;
			translateChannel = channel;
			checkMasterChannel = true;
			localize = true;
			break;
		case MASTER:
			createChannel = master;
			translateChannel = master;
			break;
		default:
			break;
		}

		rootFolder = master.getFolder();

		Template template = Creator.createTemplate("testtemplate", "Dies ist ein Test", rootFolder);

		page = Creator.createPage(null, rootFolder, template, startLanguage, createChannel);

		if (localize) {
			page.unlock();
			Page localized = (Page) page.copy();
			localized.setChannelInfo(channel.getId(), page.getChannelSetId());
			localized.save();
			page = localized;
		}

		page.unlock();

		// No permissions on Pages/Images/Files
		String permissions = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_FOLDER_CREATE, PermHandler.PERM_FOLDER_UPDATE,
				PermHandler.PERM_FOLDER_DELETE, PermHandler.PERM_TEMPLATE_VIEW, PermHandler.PERM_TEMPLATE_CREATE, PermHandler.PERM_TEMPLATE_UPDATE,
				PermHandler.PERM_TEMPLATE_DELETE, PermHandler.PERM_TEMPLATE_LINK).toString();
		PermHandler.setPermissions(Folder.TYPE_FOLDER, (Integer) rootFolder.getId(), Arrays.asList(new UserGroup[] { translatorGroup }), permissions);
		PermHandler.setPermissions(Node.TYPE_NODE, (Integer) rootFolder.getId(), Arrays.asList(new UserGroup[] { translatorGroup }), permissions);

		// Assign role & group to folder
		int roleUserGroupId = DBUtils.executeInsert("insert into role_usergroup (role_id, usergroup_id) values (?,?)",
				new Object[] { roleId, translatorGroup.getId() }).get(0);
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId,
				Folder.TYPE_FOLDER, rootFolder.getId() });
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId,
				Node.TYPE_NODE, rootFolder.getId() });

		// restrict translation group access to channel
		DBUtils.executeUpdate("DELETE FROM user_group_node where user_group_id in (SELECT id FROM user_group WHERE user_id = ? AND usergroup_id = ?)",
				new Object[] { translationUser.getId(), translatorGroup.getId() });
		if (singleChannelPermissions) {
			DBUtils.executeUpdate("INSERT INTO user_group_node (user_group_id, node_id) SELECT id, ? FROM user_group WHERE user_id = ? AND usergroup_id = ?",
					new Object[] { translateChannel.getId(), translationUser.getId(), translatorGroup.getId() });
		}

		PermissionStore.initialize(true);

		testContext.getContext().startTransaction((Integer) translationUser.getId());
	}

	/**
	 * Test translation via REST-API
	 * @throws Exception
	 */
	@Test
	public void testTranslationRights() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			assertEquals("Current User ID must match testtranslator's ID", translationUser.getId().intValue(), t.getUserId());

			PageLoadResponse result = getPageResource().translate((Integer) page.getId(), destLanguage.getCode(), lock, (Integer) translateChannel.getId());

			assertEquals("translation operation must be successful", ResponseCode.OK, result.getResponseInfo().getResponseCode());
			assertTrue("translation must not be offline", result.getPage().isOnline());

			Integer translatedPageID = result.getPage().getId();

			// Try again to check for duplicate translations
			result = getPageResource().translate((Integer) page.getId(), destLanguage.getCode(), lock, (Integer) translateChannel.getId());

			assertEquals("translation operation must be successful", ResponseCode.OK, result.getResponseInfo().getResponseCode());
			assertEquals("multiple translations of same language and node are not permitted", translatedPageID, result.getPage().getId());

			// translate page to language without permissions
			result = getPageResource().translate((Integer) page.getId(), destLanguage2.getCode(), lock, (Integer) translateChannel.getId());

			assertEquals("translation must fail", ResponseCode.PERMISSION, result.getResponseInfo().getResponseCode());

			t.setChannelId(translateChannel.getId());
			try {
				page = t.getObject(Page.class, page.getId());
				List<Page> languages = page.getLanguageVariants(false);
				assertEquals("must have exactly 2 language variants of the page in the translation channel", 2, languages.size());
			} finally {
				t.resetChannel();
			}

			if (checkMasterChannel) {
				t.setChannelId(master.getId());
				try {
					Page masterPage = page.getMaster();
					List<Page> languages = masterPage.getLanguageVariants(false);
					assertEquals("must have exactly 2 language variants of the master page", 2, languages.size());
					Page masterVariant = masterPage.getLanguageVariant(destLanguage.getCode());
					assertFalse("master translation must be offline", masterVariant.isOnline());
				} finally {
					t.resetChannel();
				}
			}

		} catch (Exception e) {
			t.rollback(false);
			throw e;
		}
	}

	@Parameters(name = "{index}: Page {0}, SingleChannel permission: {1}, locked: {2}")
	public static Iterable<Object[]> data() {
		List<Object[]> result = new ArrayList<Object[]>();
		for (boolean lock : new Boolean[] { false, true }) {
			for (PageType pt : PageType.values()) {
				result.add(new Object[] { pt, false, lock });
				if (pt != PageType.MASTER) {
					result.add(new Object[] { pt, true, lock });
				}
			}
		}
		return result;
	}

	enum PageType {
		MASTER, LOCAL, LOCALIZED, INHERITED
	}
}
