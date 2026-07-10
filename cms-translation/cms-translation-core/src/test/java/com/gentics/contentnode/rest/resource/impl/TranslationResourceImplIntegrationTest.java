package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.TranslationServiceLoaderUtil;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.MultiTagCreateRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.TagCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(value = Parameterized.class)
@GCNFeature(set = {Feature.AUTOMATIC_TRANSLATION})
public class TranslationResourceImplIntegrationTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	public final static Integer FOLDER_ID = 28;
	static final String textKeyword = "key";
	static int textConstructId;
	static int linkConstructId;
	static SystemUser user;
	static TranslationResourceImpl translationResource = new TranslationResourceImpl();
	@Parameter(0)
	public String contentFileName;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		user = supply(t -> t.getObject(SystemUser.class, 1));

		final var node = supply(
				() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		textConstructId = supply(
				() -> createConstruct(node, HTMLTextPartType.class, textKeyword, "text"));

		linkConstructId = supply(
				() -> createConstruct(node, FileURLPartType.class, textKeyword, "link1"));

		changeSessionLanguage();
		TranslationServiceLoaderUtil.reloadService();
	}

	private static void changeSessionLanguage() throws NodeException {
		operate(user, t -> {
			DBUtils.update("UPDATE systemsession SET language = ? WHERE id = ?",
					getLanguage("en").getId(),
					t.getSession().getSessionId());
		});
	}

	/**
	 * Get test variation data
	 * @return variation data
	 */
	@Parameters(name = "{index}: contentFileName {0}")
	public static Collection<Object[]> data() {
		final var basePath = "/translate";
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {basePath + "/simple-website.txt"});
		data.add(new Object[] {basePath + "/content-tag.txt"});
		return data;
	}

	@Test
	public void givenPageInSourceLanguage_shouldBeTranslatedIntoTargetLanguage()
			throws Exception {
		final var sourceLanguage = "de";
		final var targetLanguage = "en";

		try (Trx trx = new Trx(user)) {
			var pageToTranslate = createPage(sourceLanguage);
			createTags(pageToTranslate, contentFileName);

			var response = translationResource.translate(
					pageToTranslate.getId(),
					targetLanguage,
					true, 0, 0);

			var responseInfo = response.getResponseInfo();
			assertThat(responseInfo.getResponseCode())
					.withFailMessage("Expected OK but was %s", responseInfo.getResponseMessage())
					.isEqualTo(ResponseCode.OK);

			PageLoadResponse pageTranslateResponse = (PageLoadResponse) response;
			var translatedPage = pageTranslateResponse.getPage();
			var pageTags = translatedPage.getTags();

			assertThat(translatedPage.getLanguage()).isEqualTo(targetLanguage);
			assertThat(pageTags).hasSizeGreaterThanOrEqualTo(3);
		}
	}

	/**
	 * Helper method to create page tags via REST API
	 *
	 * @param page            page to add the tags
	 * @param contentFileName
	 */
	private void createTags(Page page, String contentFileName) throws IOException, NodeException {
		var createTagsRequest = new MultiTagCreateRequest();
		var create = new HashMap<String, TagCreateRequest>();
		addTagCreateRequest(create, "content1", null, textKeyword,
				readContentFromFile(this.contentFileName));
		addTagCreateRequest(create, "content2", null, textKeyword,
				"Auch andere Inhalte sollen übersetzt werden.");
		addTagCreateRequest(create, "link1", linkConstructId, null, "Das ist der Link Text");

		createTagsRequest.setCreate(create);

		var response = getPageResource().createTags(page.getId().toString(), createTagsRequest);
		assertThat(response.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
	}

	private String readContentFromFile(String fileName) throws IOException {
		StringWriter writer = new StringWriter();
		try (InputStream in = getClass().getResourceAsStream(fileName)) {
			IOUtils.copy(in, writer, Charset.forName("UTF-8"));
		}
		return writer.toString();
	}

	private void addTagCreateRequest(Map<String, TagCreateRequest> map, String id,
			Integer constructId, String keyword, String text) {
		TagCreateRequest request = new TagCreateRequest();
		request.setConstructId(constructId);
		request.setKeyword(keyword);
		request.setMagicValue(text);

		map.put(id, request);
	}

	/**
	 * Helper method to create a page via REST API
	 *
	 * @param language language of the page to create
	 * @return the created page
	 */
	private Page createPage(String language) throws NodeException {
		try (Trx trx = new Trx(user)) {
			PageResourceImpl pageResource = new PageResourceImpl();
			PageCreateRequest pageCreateRequest = new PageCreateRequest();
			pageCreateRequest.setFolderId(Integer.toString(FOLDER_ID));
			pageCreateRequest.setLanguage(language);
			pageCreateRequest.setPageName("Meine Seite");

			PageLoadResponse loadResponse = pageResource.create(pageCreateRequest);
			TransactionManager.getCurrentTransaction().commit(false);

			assertThat(loadResponse.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
			assertThat(loadResponse.getPage()).isNotNull();
			assertThat(loadResponse.getPage().getLanguage()).isEqualTo(language);
			return loadResponse.getPage();
		}
	}

}
