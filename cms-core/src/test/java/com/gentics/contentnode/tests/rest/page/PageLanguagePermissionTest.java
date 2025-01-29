package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.RolePermissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for handling language specific permissions
 */
public class PageLanguagePermissionTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext(false);
	private static Node testNode;
	private static Folder testFolder;
	private static Template template;
	private static Role testRole;
	private static UserGroup testGroup;
	private static SystemUser testUser;
	private static Object[] allowedLanguageIds;
	private Page germanPage;
	private Page englishPage;
	private Page italianPage;
	private Page frenchPage;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		testNode = supply(() -> createNode("hostname", "Test Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en"),
				getLanguage("it"), getLanguage("fr")));

		testFolder = create(Folder.class, f -> {
			f.setMotherId(testNode.getFolder().getId());
			f.setName("Testfolder");
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(testFolder.getId());
			t.setMlId(1);
			t.setName("Test Template");
		}).build();

		testRole = create(Role.class, r -> {
			r.setName("Test Role", 1);
		}).build();

		consume(r -> {
			RolePermissions rolePerm = new RolePermissions();
			// no general page permissions
			rolePerm.setPagePerm(0, Permissions.get());

			// read, create, update, publish on "de" pages
			rolePerm.setPagePerm(getLanguage("de").getId(), Permissions.get(PermType.readitems.getPageRoleBit(),
					PermType.createitems.getPageRoleBit(), PermType.updateitems.getPageRoleBit(), PermType.publishpages.getPageRoleBit()));

			// read, create, update on "en" pages
			rolePerm.setPagePerm(getLanguage("en").getId(),
					Permissions.get(PermType.readitems.getPageRoleBit(), PermType.createitems.getPageRoleBit(), PermType.updateitems.getPageRoleBit()));

			// read on "it" pages
			rolePerm.setPagePerm(getLanguage("it").getId(), Permissions.get(PermType.readitems.getPageRoleBit()));

			// no perm on "fr" pages
			rolePerm.setPagePerm(getLanguage("fr").getId(), Permissions.get());

			PermHandler.setRolePermissions(r.getId(), rolePerm);
		}, testRole);

		testGroup = create(UserGroup.class, g -> {
			g.setMotherId(ContentNodeTestDataUtils.NODE_GROUP_ID);
			g.setName("Test Group");
		}).build();

		testUser = create(SystemUser.class, s -> {
			s.setActive(true);
			s.setFirstname("tester");
			s.setLastname("tester");
			s.setLogin("tester");
			s.getUserGroups().add(testGroup);
		}).build();

		operate(() -> {
			SetPermsRequest req = new SetPermsRequest();
			req.setGroupId(testGroup.getId());
			req.setRoleIds(Collections.singleton(testRole.getId()));
			req.setSubObjects(true);
			req.setPerm(Permissions.toString(Permissions.set(Permissions.get(), PermType.read)));
			doSetPermissions(TypePerms.folder, testNode.getFolder().getId(), req);
		});

		allowedLanguageIds = supply(
				() -> Arrays.asList(getLanguage("de").getId(), getLanguage("en").getId(), getLanguage("it").getId()))
				.toArray(new Object[3]);
	}

	/**
	 * Get the expected "no permission" message for the given page
	 * @param page page
	 * @return "no permission" message (translated)
	 * @throws NodeException
	 */
	protected static String getNoPermMessage(Page page) throws NodeException {
		return supply(testUser -> {
			I18nString message = new CNI18nString("page.nopermission");
			message.setParameter("0", Integer.toString(page.getId()));
			return message.toString();
		});
	}

	/**
	 * Get the list of pages from the test folder, using the legacy method /folder/getPages/...
	 * @param language language
	 * @param langFallback true for language fallback
	 * @return response
	 * @throws NodeException
	 */
	@SuppressWarnings("deprecation")
	protected static LegacyPageListResponse getPages(String language, boolean langFallback) throws NodeException {
		return supply(testUser, () -> {
			return getFolderResource().getPages(Integer.toString(testFolder.getId()), new InFolderParameterBean(),
					new PageListParameterBean().setLanguageVariants(true).setLanguage(language).setLangFallback(
							langFallback),
					new LegacyFilterParameterBean(), new LegacySortParameterBean(), new LegacyPagingParameterBean(),
					new PublishableParameterBean(), new WastebinParameterBean());
		});
	}

	/**
	 * Get the list of pages from the test folder
	 * @param language language
	 * @param langFallback true for language fallback
	 * @return response
	 * @throws NodeException
	 */
	protected static PageListResponse list(String language, boolean langFallback) throws NodeException {
		return supply(testUser, () -> {
			return getPageResource().list(new InFolderParameterBean().setFolderId(Integer.toString(testFolder.getId())),
					new PageListParameterBean().setLanguageVariants(true).setLanguage(language)
							.setLangFallback(langFallback),
					new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(),
					new PublishableParameterBean(), new WastebinParameterBean());
		});
	}

	@Before
	public void setup() throws NodeException {
		germanPage = create(Page.class, p -> {
			p.setFolderId(testFolder.getId());
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("de"));
			p.setName("German");
		}).unlock().build();

		englishPage = create(Page.class, p -> {
			p.setFolderId(testFolder.getId());
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanPage.getContentsetId());
			p.setName("English");
		}).unlock().build();

		italianPage = create(Page.class, p -> {
			p.setFolderId(testFolder.getId());
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("it"));
			p.setContentsetId(germanPage.getContentsetId());
			p.setName("Italian");
		}).unlock().build();

		frenchPage = create(Page.class, p -> {
			p.setFolderId(testFolder.getId());
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("fr"));
			p.setContentsetId(germanPage.getContentsetId());
			p.setName("French");
		}).unlock().build();

		testFolder = execute(Folder::reload, testFolder);
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> clear(testFolder));
	}

	/**
	 * Test reading the german page
	 * @throws NodeException
	 */
	@Test
	public void testReadDe() throws NodeException {
		PageLoadResponse loadResponse = supply(testUser, () -> {
			return getPageResource().load(germanPage.getGlobalId().toString(), false, false, false,
					true, false, false, false, false, false, false, null, null);
		});
		assertResponseOK(loadResponse);
		assertThat(loadResponse.getPage().getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Test reading the english page
	 * @throws NodeException
	 */
	@Test
	public void testReadEn() throws NodeException {
		PageLoadResponse loadResponse = supply(testUser, () -> {
			return getPageResource().load(englishPage.getGlobalId().toString(), false, false, false,
					true, false, false, false, false, false, false, null, null);
		});
		assertResponseOK(loadResponse);
		assertThat(loadResponse.getPage().getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Test reading the italian page
	 * @throws NodeException
	 */
	@Test
	public void testReadIt() throws NodeException {
		PageLoadResponse loadResponse = supply(testUser, () -> {
			return getPageResource().load(italianPage.getGlobalId().toString(), false, false, false,
					true, false, false, false, false, false, false, null, null);
		});
		assertResponseOK(loadResponse);
		assertThat(loadResponse.getPage().getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Test reading the french page
	 * @throws NodeException
	 */
	@Test
	public void testReadFr() throws NodeException {
		PageLoadResponse loadResponse = supply(testUser, () -> {
			return getPageResource().load(frenchPage.getGlobalId().toString(), false, false, false,
					true, false, false, false, false, false, false, null, null);
		});
		assertResponse(loadResponse, ResponseCode.PERMISSION, getNoPermMessage(frenchPage));
	}

	/**
	 * Get getPages in german with language fallback
	 * @throws NodeException
	 */
	@Test
	public void testFolderGetPagesDeFallback() throws NodeException {
		LegacyPageListResponse listResponse = getPages("de", true);
		assertResponseOK(listResponse);
		assertThat(listResponse.getPages()).as("Page list").hasSize(1);
		assertThat(listResponse.getPages().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getPages().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get getPages in german without language fallback
	 * @throws NodeException
	 */
	@Test
	public void testFolderGetPagesDeNoFallback() throws NodeException {
		LegacyPageListResponse listResponse = getPages("de", false);
		assertResponseOK(listResponse);
		assertThat(listResponse.getPages()).as("Page list").hasSize(1);
		assertThat(listResponse.getPages().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getPages().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get getPages in french with language fallback
	 * @throws NodeException
	 */
	@Test
	public void testFolderGetPagesFrFallback() throws NodeException {
		LegacyPageListResponse listResponse = getPages("fr", true);
		assertResponseOK(listResponse);
		assertThat(listResponse.getPages()).as("Page list").hasSize(1);
		assertThat(listResponse.getPages().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getPages().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get getPages in french without language fallback
	 * @throws NodeException
	 */
	@Test
	public void testFolderGetPagesFrNoFallback() throws NodeException {
		LegacyPageListResponse listResponse = getPages("fr", false);
		assertResponseOK(listResponse);
		assertThat(listResponse.getPages()).as("Page list").isEmpty();
	}

	/**
	 * Get list in german with language fallback
	 * @throws NodeException
	 */
	@Test
	public void testListDeFallback() throws NodeException {
		PageListResponse listResponse = list("de", true);
		assertResponseOK(listResponse);
		assertThat(listResponse.getItems()).as("Page list").hasSize(1);
		assertThat(listResponse.getItems().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getItems().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get list in german without language fallback
	 * @throws NodeException
	 */
	@Test
	public void testListDeNoFallback() throws NodeException {
		PageListResponse listResponse = list("de", false);
		assertResponseOK(listResponse);
		assertThat(listResponse.getItems()).as("Page list").hasSize(1);
		assertThat(listResponse.getItems().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getItems().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get list in french with language fallback
	 * @throws NodeException
	 */
	@Test
	public void testListFrFallback() throws NodeException {
		PageListResponse listResponse = list("fr", true);
		assertResponseOK(listResponse);
		assertThat(listResponse.getItems()).as("Page list").hasSize(1);
		assertThat(listResponse.getItems().get(0)).as("Page").hasFieldOrPropertyWithValue("language", "de");
		assertThat(listResponse.getItems().get(0).getLanguageVariants()).as("language variants").isNotEmpty().containsOnlyKeys(allowedLanguageIds);
	}

	/**
	 * Get list in french without language fallback
	 * @throws NodeException
	 */
	@Test
	public void testListFrNoFallback() throws NodeException {
		PageListResponse listResponse = list("fr", false);
		assertResponseOK(listResponse);
		assertThat(listResponse.getItems()).as("Page list").isEmpty();
	}

	/**
	 * Test publishing the german page
	 * @throws NodeException
	 */
	@Test
	public void testPublishDe() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			return getPageResource().publish(Integer.toString(germanPage.getId()), null, req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOnline().isNotQueued();
	}

	/**
	 * Test publishing the german page
	 * @throws NodeException
	 */
	@Test
	public void testPublishEn() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			return getPageResource().publish(Integer.toString(englishPage.getId()), null, req);
		});
		assertResponseOK(response);

		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOffline().isQueued();
	}

	/**
	 * Test publishing the italian page
	 * @throws NodeException
	 */
	@Test
	public void testPublishIt() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			return getPageResource().publish(Integer.toString(italianPage.getId()), null, req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(italianPage));
	}

	/**
	 * Test publishing the french page
	 * @throws NodeException
	 */
	@Test
	public void testPublishFr() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			return getPageResource().publish(Integer.toString(frenchPage.getId()), null, req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(frenchPage));
	}

	/**
	 * Test publishing the german page (all languages)
	 * @throws NodeException
	 */
	@Test
	public void testPublishDeAllLang() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			req.setAlllang(true);
			return getPageResource().publish(Integer.toString(germanPage.getId()), null, req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOnline().isNotQueued();
		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOffline().isQueued();
		italianPage = execute(Page::reload, italianPage);
		GCNAssertions.assertThat(italianPage).as("Page").isOffline().isNotQueued();
		frenchPage = execute(Page::reload, frenchPage);
		GCNAssertions.assertThat(frenchPage).as("Page").isOffline().isNotQueued();
	}

	/**
	 * Test publishing the english page (all languages)
	 * @throws NodeException
	 */
	@Test
	public void testPublishEnAllLang() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			req.setAlllang(true);
			return getPageResource().publish(Integer.toString(englishPage.getId()), null, req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOnline().isNotQueued();
		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOffline().isQueued();
		italianPage = execute(Page::reload, italianPage);
		GCNAssertions.assertThat(italianPage).as("Page").isOffline().isNotQueued();
		frenchPage = execute(Page::reload, frenchPage);
		GCNAssertions.assertThat(frenchPage).as("Page").isOffline().isNotQueued();
	}

	/**
	 * Test publishing the italian page (all languages)
	 * @throws NodeException
	 */
	@Test
	public void testPublishItAllLang() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			req.setAlllang(true);
			return getPageResource().publish(Integer.toString(italianPage.getId()), null, req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(italianPage));
	}

	/**
	 * Test publishing the french page (all languages)
	 * @throws NodeException
	 */
	@Test
	public void testPublishFrAllLang() throws NodeException {
		GenericResponse response = supply(testUser, () -> {
			PagePublishRequest req = new PagePublishRequest();
			req.setAlllang(true);
			return getPageResource().publish(Integer.toString(frenchPage.getId()), null, req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(frenchPage));
	}

	@Test
	public void testTakeOfflineDe() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			return getPageResource().takeOffline(Integer.toString(germanPage.getId()), req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOffline().isNotQueued();
	}

	@Test
	public void testTakeOfflineEn() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			return getPageResource().takeOffline(Integer.toString(englishPage.getId()), req);
		});
		assertResponseOK(response);

		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOnline().isQueued();
	}

	@Test
	public void testTakeOfflineIt() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			return getPageResource().takeOffline(Integer.toString(italianPage.getId()), req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(italianPage));
	}

	@Test
	public void testTakeOfflineFr() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			return getPageResource().takeOffline(Integer.toString(frenchPage.getId()), req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(frenchPage));
	}

	@Test
	public void testTakeOfflineDeAllLang() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			req.setAlllang(true);
			return getPageResource().takeOffline(Integer.toString(germanPage.getId()), req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOffline().isNotQueued();
		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOnline().isQueued();
		italianPage = execute(Page::reload, italianPage);
		GCNAssertions.assertThat(italianPage).as("Page").isOnline().isNotQueued();
		frenchPage = execute(Page::reload, frenchPage);
		GCNAssertions.assertThat(frenchPage).as("Page").isOnline().isNotQueued();
	}

	@Test
	public void testTakeOfflineEnAllLang() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			req.setAlllang(true);
			return getPageResource().takeOffline(Integer.toString(englishPage.getId()), req);
		});
		assertResponseOK(response);

		germanPage = execute(Page::reload, germanPage);
		GCNAssertions.assertThat(germanPage).as("Page").isOffline().isNotQueued();
		englishPage = execute(Page::reload, englishPage);
		GCNAssertions.assertThat(englishPage).as("Page").isOnline().isQueued();
		italianPage = execute(Page::reload, italianPage);
		GCNAssertions.assertThat(italianPage).as("Page").isOnline().isNotQueued();
		frenchPage = execute(Page::reload, frenchPage);
		GCNAssertions.assertThat(frenchPage).as("Page").isOnline().isNotQueued();
	}

	@Test
	public void testTakeOfflineItAllLang() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			req.setAlllang(true);
			return getPageResource().takeOffline(Integer.toString(italianPage.getId()), req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(italianPage));
	}

	@Test
	public void testTakeOfflineFrAllLang() throws NodeException {
		publishAll();
		GenericResponse response = supply(testUser, () -> {
			PageOfflineRequest req = new PageOfflineRequest();
			req.setAlllang(true);
			return getPageResource().takeOffline(Integer.toString(frenchPage.getId()), req);
		});
		assertResponse(response, ResponseCode.PERMISSION, getNoPermMessage(frenchPage));
	}

	/**
	 * Publish all pages
	 * @throws NodeException
	 */
	protected void publishAll() throws NodeException {
		germanPage = update(germanPage, p -> {
		}).publish().unlock().build();
		englishPage = update(englishPage, p -> {
		}).publish().unlock().build();
		italianPage = update(italianPage, p -> {
		}).publish().unlock().build();
		frenchPage = update(frenchPage, p -> {
		}).publish().unlock().build();
	}
}
