package com.gentics.contentnode.rest.resource;


import com.gentics.contentnode.rest.model.request.TranslationRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TranslationResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


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
	TranslationResponse translate(TranslationRequest request);


	/**
	 * Translate the page into the given language. When the language variant of the page exists, it is
	 * just locked and returned, otherwise the page is copied into the language variant and returned.
	 * This method fails, if the requested language is not available for the node of the page or the
	 * user has no permission to create/edit the given language variant
	 *
	 * @param id           id of the page to translate
	 * @param languageCode code of the language into which the page shall be translated
	 * @param locked       true if the translation shall be locked, false if not
	 * @param channelId    for multichannelling, specify channel in which to create page (can be 0 or
	 *                     equal to node ID to be ignored)
	 * @return page load response
	 */
	@POST
	@Path("/page/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	PageLoadResponse translate(@PathParam("id") Integer id,
			@QueryParam("language") String languageCode,
			@QueryParam("locked") @DefaultValue("true") boolean locked,
			@QueryParam("channelId") Integer channelId);
}
