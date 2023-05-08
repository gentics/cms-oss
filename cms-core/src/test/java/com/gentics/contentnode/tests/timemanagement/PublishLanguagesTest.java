package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * General test cases for the time management
 */
@RunWith(value = Parameterized.class)
public class PublishLanguagesTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node noLangNode;
	private static Node langNode;
	private static Template template;

	@Parameters(name = "{index}: allLanguages {0}, languages {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean allLanguages : Arrays.asList(true, false)) {
			for (boolean languages : Arrays.asList(true, false)) {
				data.add(new Object[] { allLanguages, languages });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		noLangNode = supply(() -> createNode());
		langNode = supply(() -> createNode("languages", "Languages", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));

		template = supply(() -> createTemplate(noLangNode.getFolder(), "Template"));
	}

	@Parameter(0)
	public boolean allLanguages;

	@Parameter(1)
	public boolean languages;

	/**
	 * Test taking a page offline
	 * @throws NodeException
	 */
	@Test
	public void testOffline() throws NodeException {
		Page page = null;
		Page variant = null;
		if (languages) {
			page = supply(() -> createPage(langNode.getFolder(), template, "Page", null, getLanguage("de")));
			variant = execute(p -> {
				Page translation = (Page)p.copy();
				translation.setLanguage(getLanguage("en"));
				translation.setName("English");
				translation.save();
				return translation.reload();
			}, page);
		} else {
			page = supply(() -> createPage(noLangNode.getFolder(), template, "Page"));
		}
		consume(p -> update(p, Page::publish), page);
		page = execute(Page::reload, page);
		assertThat(page).isOnline();
		if (variant != null) {
			consume(p -> update(p, Page::publish), variant);
			variant = execute(Page::reload, variant);
			assertThat(variant).isOnline();
		}

		consume(p -> {
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAlllang(allLanguages);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(p.getId()), request);
			assertResponseCodeOk(response);
		}, page);

		page = execute(Page::reload, page);
		assertThat(page).isOffline();

		if (variant != null) {
			variant = execute(Page::reload, variant);
			if (allLanguages) {
				assertThat(variant).isOffline();
			} else {
				assertThat(variant).isOnline();
			}
		}
	}

	/**
	 * Test publishing a page
	 * @throws NodeException
	 */
	@Test
	public void testPublish() throws NodeException {
		Page page = null;
		Page variant = null;
		if (languages) {
			page = supply(() -> createPage(langNode.getFolder(), template, "Page", null, getLanguage("de")));
			variant = execute(p -> {
				Page translation = (Page)p.copy();
				translation.setLanguage(getLanguage("en"));
				translation.setName("English");
				translation.save();
				return translation.reload();
			}, page);
		} else {
			page = supply(() -> createPage(noLangNode.getFolder(), template, "Page"));
		}
		assertThat(page).isOffline();
		if (variant != null) {
			assertThat(variant).isOffline();
		}

		consume(p -> {
			PagePublishRequest request = new PagePublishRequest();
			request.setAlllang(allLanguages);
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(p.getId()), null, request);
			assertResponseCodeOk(response);
		}, page);

		page = execute(Page::reload, page);
		assertThat(page).isOnline();

		if (variant != null) {
			variant = execute(Page::reload, variant);
			if (allLanguages) {
				assertThat(variant).isOnline();
			} else {
				assertThat(variant).isOffline();
			}
		}
	}
}
