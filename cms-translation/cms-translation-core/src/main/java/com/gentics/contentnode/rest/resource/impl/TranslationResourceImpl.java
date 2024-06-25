package com.gentics.contentnode.rest.resource.impl;


import static com.gentics.contentnode.etc.JsoupValidator.VALIDATOR;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.TranslationRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TranslationResponse;
import com.gentics.contentnode.rest.resource.TranslationResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.translation.TranslationService;
import com.gentics.contentnode.translation.TranslationServiceProvider;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;

@Authenticated
@RequiredFeature(Feature.TRANSLATION)
@Produces(MediaType.APPLICATION_JSON)
@Path("/translation")
public class TranslationResourceImpl implements TranslationResource {

	private final NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	/**
	 * The translation service used to translate tests
	 */
	private TranslationService translationService;

	@Override
	@POST
	@Path("/text")
	@Consumes(MediaType.APPLICATION_JSON)
	public TranslationResponse translate(final TranslationRequest request) {
		if (request == null || StringUtils.isBlank(request.getText()) || StringUtils.isBlank(
				request.getTargetLanguage())) {
			throw new WebApplicationException(
					"Invalid request. Either translation text or target language is not set.",
					Status.BAD_REQUEST);
		}

		try {
			translationService = getTranslationService();

			var translatedText = translationService.translate(request.getText(),
					request.getSourceLanguage(),
					request.getTargetLanguage());

			var response = new TranslationResponse();
			response.setText(translatedText);

			return response;
		} catch (NodeException e) {
			logger.error("Error while translating text ", e);
			throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
		}
	}


	@Override
	@POST
	@Path("/page/{id}")
	public PageLoadResponse translate(final @PathParam("id") Integer pageId,
			final @QueryParam("language") String targetLanguageCode,
			final @QueryParam("locked") @DefaultValue("true") boolean locked,
			final @QueryParam("channelId") @DefaultValue("0") Integer channelId) {
		try (Trx trx = ContentNodeHelper.trx()) {
			final var loadedPage = getPage(pageId);
			final var translatedNodePage = createLanguageVariant(loadedPage, targetLanguageCode);
			final var sourceLanguageCode = loadedPage.getLanguage().getCode();

			var pageToTranslate = ModelBuilder.getPage(translatedNodePage,
					List.of(Reference.CONTENT_TAGS));
			var contentTags = this.getContentTags(pageToTranslate);
			translationService = getTranslationService();

			for (var contentTag : contentTags) {
				var contentTagProperties = contentTag.getProperties();
				this.translateContentTags(translationService, contentTagProperties, sourceLanguageCode,
						targetLanguageCode);
			}

			final var sourceTitle = pageToTranslate.getName();
			final var translatedTitle = translationService.translate(sourceTitle, sourceLanguageCode,
					targetLanguageCode);
			pageToTranslate.setName(translatedTitle);

			var updatedTranslatedNodePage = ModelBuilder.getPage(pageToTranslate, false);

			if (updatedTranslatedNodePage == null) {
				throw new NodeException(
						String.format("Page language variant for source page '%s' not found.", pageId));
			}
			updatedTranslatedNodePage.save();

			final var pageResponse = new PageLoadResponse();
			pageResponse.setPage(ModelBuilder.getPage(updatedTranslatedNodePage));

			pageResponse.setResponseInfo(
					new ResponseInfo(ResponseCode.OK, String.format("Translated page { %s } successfully", pageId)));
			trx.getTransaction().commit(false);

			return pageResponse;
		} catch (Exception e) {
			logger.error("Error while translating page", e);
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Loads the page object with the given pageId
	 * @param pageId the page id
	 * @return the node page
	 * @throws NodeException
	 */
	private Page getPage(int pageId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.getObject(Page.class, pageId);

		if (page == null) {
			throw new NodeException(String.format("Error occurred while loading page %s", pageId));
		}

		return page;
	}

	/**
	 * Creates a new language variant of a page for the given target Language
	 * @param page the that should have a new language variant
	 * @param targetLanguage the target language
	 * @return the new page for the specified language
	 * @throws NodeException
	 */
	private Page createLanguageVariant(Page page, String targetLanguage) throws NodeException {
		var languageVariant = (Page) page.copy();
		var contentTargetLanguage = LanguageFactory.get(targetLanguage);

		if (contentTargetLanguage == null) {
			throw new NodeException(
					String.format("Target language '%s' could not be resolved.", targetLanguage));
		}

		languageVariant.setLanguage(contentTargetLanguage);
		languageVariant.save();

		return languageVariant;
	}

	/**
	 * Translates the text properties of the given content tags
	 *
	 * @param translationService   the service that translates the text
	 * @param contentTagProperties the tags that should be translated
	 * @param sourceLanguageCode   the language code of the source text
	 * @param targetLanguageCode   the language code of the target text
	 * @throws NodeException
	 */
	private void translateContentTags(TranslationService translationService,
			Map<String, Property> contentTagProperties,
			String sourceLanguageCode, String targetLanguageCode) throws NodeException {
		for (var contentTagProperty : contentTagProperties.entrySet()) {
			if (contentTagProperty != null && !StringUtils.isBlank(
					contentTagProperty.getValue().getStringValue())) {
				Property propertyToTranslate = contentTagProperty.getValue();

				var extractedText = propertyToTranslate.getStringValue();
				var translatedText = translationService.translate(extractedText, sourceLanguageCode,
						targetLanguageCode, VALIDATOR);

				propertyToTranslate.setStringValue(translatedText);
				contentTagProperties.put(contentTagProperty.getKey(), propertyToTranslate);
			}
		}
	}

	/**
	 * Helper method to get all the tags of Page with the type #{@link Type} CONTENTTAG
	 *
	 * @param page the page to obtain the content tags.
	 * @return the content tags
	 */
	private ArrayList<Tag> getContentTags(com.gentics.contentnode.rest.model.Page page) {
		var contentTags = new ArrayList<Tag>();
		var pageTags = page.getTags();

		if (pageTags == null) {
			return contentTags;
		}

		pageTags.forEach((s, tag) -> {
			if (tag.getType() == Type.CONTENTTAG) {
				contentTags.add(tag);
			}
		});

		return contentTags;
	}

	/**
	 * Gets the translation service corresponding to the provided configuration.
	 *
	 * @return the translation service
	 * @throws NodeException if the translation service cannot be instantiated
	 */
	public TranslationService getTranslationService() throws NodeException {
		if (this.translationService == null) {
			return new TranslationServiceProvider.Builder()
					.withProviderFromConfig()
					.build();
		}

		return this.translationService;
	}

	/**
	 * Utility class for tests to set the translation service
	 *
	 * @param translationService
	 */
	public void setTranslationService(TranslationService translationService) {
		this.translationService = translationService;
	}

}
