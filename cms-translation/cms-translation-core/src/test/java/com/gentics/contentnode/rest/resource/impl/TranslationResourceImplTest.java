package com.gentics.contentnode.rest.resource.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.InvalidRequestException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.MultiTagCreateRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.TagCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import jakarta.ws.rs.core.Response.Status;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@GCNFeature(set = {Feature.AUTOMATIC_TRANSLATION})
public class TranslationResourceImplTest {

	public final static Integer FOLDER_ID = 28;
	static final String textKeyword = "key";
	static final String checkboxKeyword = "checkboxkey";
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	static int textConstructId;
	static int checkBoxConstructId;
	static int linkConstructId ;
	static SystemUser user;
	static Auth userAuth;
	static TranslationResourceImpl translationResource = new TranslationResourceImpl();
	TranslationService mockedTranslationService = Mockito.mock(TranslationService.class);


	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		user = supply(t -> t.getObject(SystemUser.class, 1));
		userAuth = new Auth(user);

		final var node = supply(
				() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		textConstructId = supply(
				() -> createConstruct(node, HTMLTextPartType.class, textKeyword, "text"));

		linkConstructId = supply(
				() -> createConstruct(node, FileURLPartType.class, textKeyword, "link1"));

		checkBoxConstructId = supply(
				() -> createConstruct(node, CheckboxPartType.class, checkboxKeyword, "check"));
	}

	@Parameters(name = "{index}: auth: {0}")
	public static Collection<Object[]> data() {
		return Stream.of(AuthType.LOGIN, AuthType.TOKEN).map(type -> new Object[] {type}).collect(Collectors.toList());
	}

	@Parameter(0)
	public AuthType authType;

	@Test
	public void givenPageInSourceLanguage_shouldBeTranslatedIntoTargetLanguage()
			throws Exception {
		when(mockedTranslationService.translate(anyString(), anyString(), anyString())).thenReturn(
				"Translated text");
		translationResource.setTranslationService(mockedTranslationService);

		final var sourceLanguage = "de";
		final var targetLanguage = "en";

		userAuth.withAuth(authType, () -> {
			var pageToTranslate = createPage(sourceLanguage);
			createTags(pageToTranslate);

			var pageTranslateRequest = assertSuccess(() -> translationResource.translate(pageToTranslate.getId(),
					targetLanguage,
					true, 0, 0), null);

			assertThat(pageTranslateRequest.getPage().getLanguage()).isEqualTo(targetLanguage);

			var pageTags = pageTranslateRequest.getPage().getTags();
			assertThat(pageTags).hasSizeGreaterThanOrEqualTo(3);
		});
	}

	@Test
	public void givenTranslatorReturnsCorruptMarkup_shouldThrowException() throws Exception {
		when(mockedTranslationService.translate(anyString(), anyString(), anyString())).thenReturn(
				"<h1>My blog is great <!-- something is missing here -->");
		translationResource.setTranslationService(mockedTranslationService);

		final var sourceLanguage = "de";
		final var targetLanguage = "en";
		var pageToTranslate = createPage(sourceLanguage);

		String msg = supply(() -> I18NHelper.get("feature.translation.error", I18NHelper.get("feature.translation.error.reason.markup_invalid")));

		userAuth.withAuth(authType, () -> {
			createTags(pageToTranslate);

			Throwable thrown = catchThrowable(() -> translationResource.translate(pageToTranslate.getId(),
					targetLanguage,
					true, 0, 0));
			assertThat(thrown).as("Thrown exception").isInstanceOf(RestMappedException.class);
			assertThat(((RestMappedException) thrown).getStatus()).as("Response status").isEqualTo(Status.INTERNAL_SERVER_ERROR);
			var response = ((RestMappedException) thrown).getRestResponse();

			assertThat(response.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.FAILURE) ;
			final var expectedMessage = "The page could not be translated because the markup of the translated page is invalid";
			final var actualMessage = response.getResponseInfo().getResponseMessage();

			assertThat(actualMessage).contains(expectedMessage);
		});
	}

	@Test
	public void givenNonExistentTargetLanguage_shouldThrowException() throws Exception {
		when(mockedTranslationService.translate(anyString(), anyString(), anyString())).thenReturn(
				"<h1>My blog is great</h1>");
		translationResource.setTranslationService(mockedTranslationService);

		final var sourceLanguage = "de";
		final var targetLanguage = "ch";
		var pageToTranslate = createPage(sourceLanguage);

		String msg = supply(() -> I18NHelper.get("feature.translation.error.language_not_existent", targetLanguage));

		userAuth.withAuth(authType, () -> {
			createTags(pageToTranslate);

			Throwable thrown = catchThrowable(() -> translationResource.translate(pageToTranslate.getId(),
					targetLanguage,true, 0, 0));
			assertThat(thrown).as("Thrown exception").isInstanceOf(InvalidRequestException.class);
			assertThat(((RestMappedException) thrown).getStatus()).as("Response status").isEqualTo(Status.BAD_REQUEST);
			var response = ((RestMappedException) thrown).getRestResponse();

			final var expectedMessage = "Error while translating page: invalid language code";
			final var actualMessage = response.getResponseInfo().getResponseMessage();

			assertThat(actualMessage).contains(expectedMessage);
		});
	}

	/**
	 * Helper method to create page tags via REST API
	 *
	 * @param page page to add the tags
	 */
	private void createTags(Page page) throws NodeException {
		var createTagsRequest = new MultiTagCreateRequest();
		var create = new HashMap<String, TagCreateRequest>();
		addTagCreateRequest(create, "content1", null, textKeyword,
				"Mein Blogeintrag muss übersetzt werden.");
		addTagCreateRequest(create, "content2", null, textKeyword,
				"Auch andere Inhalte sollen übersetzt werden.");
		addTagCreateRequest(create, "non-content", checkBoxConstructId, null, null);
		addTagCreateRequest(create, "link1", linkConstructId , null, "Das ist der Link Text");

		createTagsRequest.setCreate(create);

		assertSuccess(() -> getPageResource().createTags(page.getId().toString(), createTagsRequest), null);
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
		PageCreateRequest pageCreateRequest = new PageCreateRequest();
		pageCreateRequest.setFolderId(Integer.toString(FOLDER_ID));
		pageCreateRequest.setLanguage(language);
		pageCreateRequest.setPageName("Page title");

		PageLoadResponse loadResponse = assertSuccess(() -> getPageResource().create(pageCreateRequest), null);

		assertThat(loadResponse.getPage()).isNotNull();
		assertThat(loadResponse.getPage().getLanguage()).isEqualTo(language);
		return loadResponse.getPage();
	}
}
