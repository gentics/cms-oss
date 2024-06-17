package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
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
import com.gentics.contentnode.translation.TranslationService;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;


@GCNFeature(set = {Feature.TRANSLATION})
public class TranslationResourceImplTest {

	public final static Integer FOLDER_ID = 28;
	static final String textKeyword = "key";
	static final String checkboxKeyword = "checkboxkey";
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	static int textConstructId;
	static int checkBoxConstructId;
	static SystemUser user;
	static TranslationResourceImpl translationResource = new TranslationResourceImpl();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		user = supply(t -> t.getObject(SystemUser.class, 1));

		final var node = supply(
				() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		textConstructId = supply(
				() -> createConstruct(node, HTMLTextPartType.class, textKeyword, "text"));

		checkBoxConstructId = supply(
				() -> createConstruct(node, CheckboxPartType.class, checkboxKeyword, "check"));

		TranslationService mockedTranslationService = Mockito.mock(TranslationService.class);
		when(mockedTranslationService.translate(anyString(),anyString(), anyString())).thenReturn("Translated text");
		translationResource.setTranslationService(mockedTranslationService);
	}


	@Test
	public void givenPageInSourceLanguage_shouldBeTranslatedIntoTargetLanguage()
			throws NodeException {
		final var sourceLanguage = "de";
		final var targetLanguage = "en";
		var pageToTranslate = createPage(sourceLanguage);

		try (Trx trx = new Trx(user)) {
			createTags(pageToTranslate);

			var pageTranslateRequest = translationResource.translate(pageToTranslate.getId(),
					targetLanguage,
					true, 0);

			assertThat(pageTranslateRequest.getPage().getLanguage()).isEqualTo(targetLanguage);

			var pageTags = pageTranslateRequest.getPage().getTags();
			assertThat(pageTags).hasSizeGreaterThanOrEqualTo(3);
		}
	}

	/**
	 * Helper method to create page tags via REST API
	 *
	 * @param page page to add the tags
	 */
	private void createTags(Page page) throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		var createTagsRequest = new MultiTagCreateRequest();
		var create = new HashMap<String, TagCreateRequest>();
		addTagCreateRequest(create, "content1", null, textKeyword,
				"Mein Blogeintrag muss übersetzt werden.");
		addTagCreateRequest(create, "content2", null, textKeyword,
				"Auch andere Inhalte sollen übersetzt werden.");
		addTagCreateRequest(create, "non-content", checkBoxConstructId, null, null);

		createTagsRequest.setCreate(create);

		var response = pageResource.createTags(page.getId().toString(), createTagsRequest);
		assertThat(response.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
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
			pageCreateRequest.setPageName("Page title");

			PageLoadResponse loadResponse = pageResource.create(pageCreateRequest);
			TransactionManager.getCurrentTransaction().commit(false);

			assertThat(loadResponse.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
			assertThat(loadResponse.getPage()).isNotNull();
			assertThat(loadResponse.getPage().getLanguage()).isEqualTo(language);
			return loadResponse.getPage();
		}
	}

}
