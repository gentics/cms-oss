package com.gentics.contentnode.rest.resource.impl;


import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.TranslationException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.TranslationServiceLoaderUtil;
import com.gentics.contentnode.etc.TranslationStats;
import com.gentics.contentnode.exception.InvalidRequestException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.TranslationRequest;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TranslationResponse;
import com.gentics.contentnode.rest.resource.TranslationResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.translation.LanguageVariantService;
import com.gentics.contentnode.translation.TagResolveService;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Authenticated
@RequiredFeature(Feature.AUTOMATIC_TRANSLATION)
@Produces(MediaType.APPLICATION_JSON)
@Path("/translation")
public class TranslationResourceImpl implements TranslationResource {

	/**
	 * The translation service used to translate
	 */
	private TranslationService translationService;

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			TranslationResourceImpl.class);

	@Override
	@POST
	@Path("/text")
	@Consumes(MediaType.APPLICATION_JSON)
	public TranslationResponse translate(final TranslationRequest request) throws NodeException {
		if (request == null || StringUtils.isBlank(request.getText()) || StringUtils.isBlank(
				request.getTargetLanguage())) {
			throw new InvalidRequestException(
					new CNI18nString("feature.translation.error.invalid_request").toString())
					.setResponseCode(ResponseCode.INVALIDDATA)
					.setStatus(Status.BAD_REQUEST);
		}
		try {
			var stats = new TranslationStats();
			var translationService = getTranslationService();
			var translatedText = translationService.translate(request.getText(),
					request.getSourceLanguage(),
					request.getTargetLanguage());

			stats.addRequest(request.getText().length(), translatedText.length());

			var response = new TranslationResponse();
			response.setText(translatedText);

			logger.debug("Translated custom text. %s".formatted(stats));
			return response;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}


	@Override
	@POST
	@Path("/page/{id}")
	public PageLoadResponse translate(final @PathParam("id") Integer pageId,
			final @QueryParam("language") String targetLanguageCode,
			final @QueryParam("locked") @DefaultValue("true") boolean locked,
			final @QueryParam("channelId") @DefaultValue("0") Integer channelId,
			@QueryParam("wait") @DefaultValue("2000") long waitMs
	) throws NodeException {
		if (StringUtils.isEmpty(targetLanguageCode)) {
			throw new MissingFieldException("language");
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.executeLockedRethrowing(new CNI18nString("feature.automatic_translation").toString(), waitMs, null, () -> {
				final var languageVariantService = new LanguageVariantService();
				final var loadedPage = languageVariantService.getPage(pageId.toString(), true);
				final var sourceLanguage = loadedPage.getLanguage();

				if (sourceLanguage == null) {
					throw new InvalidRequestException(
							new CNI18nString("feature.translation.error.translating_page").toString());
				}
				final var sourceLanguageCode = sourceLanguage.getCode();
				final var translatedNodePage = languageVariantService.translate(pageId, targetLanguageCode,
						locked, channelId, true);

				final var pageToTranslate = ModelBuilder.getPage(translatedNodePage,
						List.of(Reference.CONTENT_TAGS, Reference.OBJECT_TAGS_VISIBLE));

				try {
					var contentTags = this.getContentTags(pageToTranslate);
					final var tagResolveService = new TagResolveService(loadedPage.getOwningNode());
					var stats = new TranslationStats();

					var translationService = getTranslationService();
					tagResolveService.resolveAndTranslateContentTags(contentTags, translationService, sourceLanguageCode,
							targetLanguageCode, stats);

					final var sourceName = pageToTranslate.getName();
					final var translatedName = translationService.translate(sourceName,
							sourceLanguageCode,
							targetLanguageCode);
					stats.addRequest(sourceName.length(), translatedName.length());
					pageToTranslate.setName(translatedName);

					logger.debug("Translated page %s. %s".formatted(I18NHelper.getName(loadedPage), stats));
				} catch (TranslationException e) {
					this.deletePage(translatedNodePage);
					throw e;
				} catch (Exception e) {
					this.deletePage(translatedNodePage);
					throw new NodeException(e);
				}

				var updatedTranslatedNodePage = ModelBuilder.getPage(pageToTranslate, false);

				if (updatedTranslatedNodePage == null) {
					var errorMessage = new CNI18nString("feature.translation.error.page_variant_not_found");
					errorMessage.addParameter(pageId.toString());
					throw new NodeException(errorMessage.toString());
				}
				updatedTranslatedNodePage.save();
				trx.success();

				var response = createPageLoadResponse(updatedTranslatedNodePage, pageId);
				CNI18nString resultMessage = new CNI18nString("backgroundjob_finished_successfully");
				resultMessage.addParameter(new CNI18nString("feature.automatic_translation").toString());
				response.addMessage(new Message(Message.Type.SUCCESS, resultMessage.toString()));

				return response;
			}, PageLoadResponse::new);
		}
	}

	/**
	 * Deletes language variant
	 * @param translatedNodePage the page to delete
	 * @throws NodeException
	 */
	private void deletePage(Page translatedNodePage) throws NodeException {
		if(translatedNodePage == null) {
			return;
		}

		var response = supply(()-> new PageResourceImpl().delete(translatedNodePage.getId().toString(),
				translatedNodePage.getNode().getId(), true));

		if(response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			logger.info(String.format("Page Language Variant with id '%s' deleted successfully",
					translatedNodePage.getNode().getId()));
			return;
		}
		logger.error(response.getResponseInfo().getResponseMessage());
	}


	/**
	 * This method is used to check if a page is read-only.
	 *
	 * @param page The page to check.
	 * @return boolean Returns true if the page is read-only, false otherwise.
	 * @throws NodeException If there is an error while checking the page's read-only status.
	 */
	private boolean isPageReadOnly(Page page) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permissionHandler = t.getPermHandler();

		return !permissionHandler.canEdit(page);
	}

	/**
	 * This method is used to create a PageLoadResponse object.
	 *
	 * @param updatedTranslatedNodePage The updated page after translation.
	 * @param pageId                    The ID of the page.
	 * @return PageLoadResponse The response object containing the updated page information.
	 * @throws NodeException If there is an error while creating the PageLoadResponse.
	 */
	private PageLoadResponse createPageLoadResponse(Page updatedTranslatedNodePage, Integer pageId)
			throws NodeException {
		final var pageResponse = new PageLoadResponse();

		var responseMessage = new CNI18nString("feature.translation.success");
		responseMessage.addParameter(pageId.toString());

		if (updatedTranslatedNodePage.getChannel() != null) {
			var responseMessageWithChannel = String.format(" in channel '%s'",
					updatedTranslatedNodePage.getChannel().getId());
			responseMessage.addParameter(responseMessageWithChannel);
		}

		pageResponse.setPage(ModelBuilder.getPage(updatedTranslatedNodePage,
				List.of(Reference.CONTENT_TAGS, Reference.OBJECT_TAGS_VISIBLE)));
		pageResponse.setResponseInfo(
				new ResponseInfo(ResponseCode.OK, responseMessage.toString()));

		return pageResponse;
	}

	/**
	 * Helper method to get all the tags of Page with the type #{@link Type} CONTENTTAG
	 *
	 * @param page the page to obtain the content tags.
	 * @return the content tags
	 */
	private List<Tag> getContentTags(com.gentics.contentnode.rest.model.Page page) {
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
			this.translationService = TranslationServiceLoaderUtil.getTranslationService().orElseThrow(() -> new NodeException(
					"Translation service not available"));
		}
		logger.info(String.format("Translation service successfully requested. Translation service currently in use: '%s'", this.translationService.getClass()));
		return this.translationService;
	}

	/**
	 * Utility class for tests to set the translation service
	 *
	 * @param translationService the translation service to be used
	 */
	public void setTranslationService(TranslationService translationService) {
		this.translationService = translationService;
	}

}
