package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

import io.reactivex.Observable;

/**
 * Test cases for deleting pages
 */
public class PageDeleteTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node threeLanguages;
	private static Node oneLanguage;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		threeLanguages = supply(() -> createNode("threelanguages", "Three Languages", PublishTarget.NONE, getLanguage("de"), getLanguage("en"), getLanguage("fr")));
		oneLanguage = supply(() -> createNode("onelanguage", "One Language", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));

		template = supply(() -> createTemplate(threeLanguages.getFolder(), "Template"));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(threeLanguages));
		operate(() -> clear(oneLanguage));
	}

	/**
	 * Assert that the given page has been deleted
	 * @param page page
	 * @throws NodeException
	 */
	protected static void assertDeleted(Page page) throws NodeException {
		Page reloaded = execute(Page::reload, page);
		assertThat(reloaded).isNull();
	}

	/**
	 * Assert that the given page has not been deleted
	 * @param page page
	 * @throws NodeException
	 */
	protected static void assertNotDeleted(Page page) throws NodeException {
		Page reloaded = execute(Page::reload, page);
		assertThat(reloaded).isNotNull();
	}

	/**
	 * Get the given page language
	 * @param pageId page ID
	 * @param languageCode language code
	 * @return language variant (may be null)
	 * @throws NodeException
	 */
	protected static Page getLanguageVariant(int pageId, String languageCode) throws NodeException {
		return supply(t -> {
			Page page = t.getObject(Page.class, pageId);
			List<Page> variants = page.getLanguageVariants(false);
			return Observable.fromIterable(variants).filter(p -> StringUtils.equals(languageCode, p.getLanguage().getCode())).firstElement().blockingGet();
		});
	}

	/**
	 * Delete a single language
	 * @throws NodeException
	 */
	@Test
	public void deleteSingleLanguage() throws NodeException {
		Page german = supply(() -> createPage(threeLanguages.getFolder(), template, "German", null, getLanguage("de")));
		Page english = supply(t -> {
			PageLoadResponse response = getPageResource().translate(german.getId(), "en", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});
		Page french = supply(t -> {
			PageLoadResponse response = getPageResource().translate(german.getId(), "fr", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});

		operate(() -> {
			assertResponseOK(getPageResource().delete(Integer.toString(english.getId()), null));
		});

		assertNotDeleted(german);
		assertDeleted(english);
		assertNotDeleted(french);
	}

	/**
	 * Test that deleting the last visible language variant will delete also all invisible (i.e. not assigned to the node) language variants
	 * @throws NodeException
	 */
	@Test
	public void deleteLastVisibleLanguage() throws NodeException {
		Page origGerman = supply(() -> createPage(threeLanguages.getFolder(), template, "German", null, getLanguage("de")));
		supply(t -> {
			PageLoadResponse response = getPageResource().translate(origGerman.getId(), "en", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});
		supply(t -> {
			PageLoadResponse response = getPageResource().translate(origGerman.getId(), "fr", false, null);
			return t.getObject(Page.class, response.getPage().getId());
		});

		Integer targetId = supply(() -> {
			PageCopyRequest request = new PageCopyRequest();
			request.setCreateCopy(true);
			request.setNodeId(threeLanguages.getId());
			request.setSourcePageIds(Arrays.asList(origGerman.getId()));
			request.setTargetFolders(Arrays.asList(new TargetFolder(oneLanguage.getFolder().getId(), null)));
			PageCopyResponse response = getPageResource().copy(request, 0);
			assertResponseOK(response);
			List<com.gentics.contentnode.rest.model.Page> copies = response.getPages();

			return copies.get(0).getId();
		});

		Page german = getLanguageVariant(targetId, "de");
		Page english = getLanguageVariant(targetId, "en");
		Page french = getLanguageVariant(targetId, "fr");

		assertThat(german).as("German copy").isNotNull();
		assertThat(english).as("English copy").isNotNull();
		assertThat(french).as("French copy").isNotNull();

		// delete the english variant
		operate(() -> {
			assertResponseOK(getPageResource().delete(Integer.toString(english.getId()), null));
		});

		assertNotDeleted(german);
		assertDeleted(english);
		assertNotDeleted(french);

		// delete the german variant (which is the last one) -> also french must be deleted
		operate(() -> {
			assertResponseOK(getPageResource().delete(Integer.toString(german.getId()), null));
		});

		assertDeleted(german);
		assertDeleted(english);
		assertDeleted(french);
	}
}
