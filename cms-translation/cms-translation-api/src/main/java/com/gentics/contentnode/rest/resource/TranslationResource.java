package com.gentics.contentnode.rest.resource;


import com.gentics.contentnode.rest.model.request.TranslationRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TranslationResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;


/**
 * Resource used for translation.
 */
@Path("/translation")
public interface TranslationResource {


	/**
	 * Translate the text into the given language.
	 *
	 * @param request The translation request to be processed.
	 * @return The response of the translation.
	 */
	@POST
	@Path("/text")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	TranslationResponse translate(TranslationRequest request) throws Exception;


	/**
	 * Translate the page into the given language. When the language variant of the page exists, it is
	 * just locked and returned, otherwise the page is copied into the language variant and returned.
	 * This method fails, if the requested language is not available for the node of the page or the
	 * user has no permission to create/edit the given language variant
	 *
	 * @param id                 id of the page to translate
	 * @param targetLanguageCode code of the language into which the page shall be translated
	 * @param locked             true if the translation shall be locked, false if not
	 * @param channelId          for multichannelling, specify channel in which to create page (can be
	 *                           0 or equal to node ID to be ignored)
	 * @param waitMs wait timeout in ms. When set to 0, response will be sent, when the action completes
	 * @return page load response
	 */
	@POST
	@Path("/page/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	PageLoadResponse translate(@PathParam("id") Integer id,
			@QueryParam("language") String targetLanguageCode,
			@QueryParam("locked") @DefaultValue("true") boolean locked,
			@QueryParam("channelId") Integer channelId,
			@QueryParam("wait") @DefaultValue("1000") long waitMs
	) throws Exception;
}
