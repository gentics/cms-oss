package com.gentics.contentnode.rest.resource.impl;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.TranslationRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TranslationResponse;
import com.gentics.contentnode.rest.resource.TranslationResource;
import com.gentics.contentnode.translation.GenericTranslationService;
import com.gentics.contentnode.translation.TranslationService;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
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


@Path("/translation")
public class TranslationResourceImpl extends AuthenticatedContentNodeResource implements
		TranslationResource {

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	@Override
	@POST
	@Path("/text")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TranslationResponse translate(TranslationRequest request) {
		if (request == null || StringUtils.isBlank(request.getText()) || StringUtils.isBlank(
				request.getTargetLanguage())) {
			throw new WebApplicationException(
					"Invalid request. Either translation text or target language is not set.",
					Status.BAD_REQUEST);
		}

		try {
			TranslationService translationService = new GenericTranslationService.Builder()
					.withProviderFromConfig()
					.build();

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
	@Produces(MediaType.APPLICATION_JSON)
	public PageLoadResponse translate(final @PathParam("id") Integer id,
			final @QueryParam("language") String targetLanguageCode,
			final @QueryParam("locked") @DefaultValue("true") boolean locked,
			final @QueryParam("channelId") @DefaultValue("0") Integer channelId) {
		final var pageResource = new PageResourceImpl();
		var pageToTranslateResponse = pageResource.translate(id, targetLanguageCode, locked,
				channelId);
		var pageToTranslate = pageToTranslateResponse.getPage();

		if (pageToTranslate == null) {
			throw new WebApplicationException(pageToTranslateResponse.getResponseInfo()
					.getResponseMessage());
		}

		try {
			var contentTags = this.getContentTags(pageToTranslate);

			for (var contentTag : contentTags) {
				var contentTagProperties = contentTag.getProperties();
				this.translateContentTags(contentTagProperties, pageToTranslate.getLanguage(),
						targetLanguageCode);
			}

			PageSaveRequest pageSaveRequest = new PageSaveRequest();
			pageSaveRequest.setPage(pageToTranslate);
			pageResource.save(pageToTranslate.getId().toString(), pageSaveRequest);

			PageLoadResponse pageResponse = new PageLoadResponse();
			pageResponse.setPage(pageToTranslate);

			return pageResponse;
		} catch (Exception e) {
			logger.error("Error while translating page", e);
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	private void translateContentTags(Map<String, Property> contentTagProperties,
			String sourceLanguageCode, String targetLanguageCode)
			throws NodeException {
		TranslationService translationService = new GenericTranslationService.Builder()
				.withProviderFromConfig()
				.build();

		// todo: multiple props possible
		for (var contentTagProperty : contentTagProperties.entrySet()) {
			if (contentTagProperty != null && !StringUtils.isBlank(
					contentTagProperty.getValue().getStringValue())) {
				Property propertyToTranslate = contentTagProperty.getValue();

				var extractedText = propertyToTranslate.getStringValue();
				var translatedText = translationService.translate(extractedText, sourceLanguageCode,
						targetLanguageCode);

				propertyToTranslate.setStringValue(translatedText);
				contentTagProperties.put(contentTagProperty.getKey(), propertyToTranslate);
			}
		}
	}

	private ArrayList<Tag> getContentTags(com.gentics.contentnode.rest.model.Page page) {
		var contentTags = new ArrayList<Tag>();

		page.getTags().forEach((s, tag) -> {
			if (tag.getType() == Type.CONTENTTAG) {
				contentTags.add(tag);
			}
		});

		return contentTags;
	}


}
