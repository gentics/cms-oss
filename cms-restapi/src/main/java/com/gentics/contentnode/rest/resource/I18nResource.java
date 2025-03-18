package com.gentics.contentnode.rest.resource;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.request.SetLanguageRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LanguageResponse;
import com.gentics.contentnode.rest.model.response.UILanguagesResponse;

/**
 * Resource to translate given keys (optionally including parameters)
 */
@Path("/i18n")
public interface I18nResource {
	@GET
	@Path("/list")
	UILanguagesResponse list() throws Exception;

	/**
	 * Translate the given key and optional parameters (variant with key given in the path)
	 * @param key translation key
	 * @param parameters optional parameters
	 * @return translated string
	 */
	@GET
	@Path("/t/{key}")
	@Produces({ "text/plain; charset=UTF-8"})
	String translateFromPath(@PathParam("key") String key, @QueryParam("p") List<String> parameters) throws Exception;

	/**
	 * Translate the given key and optional parameters (variant with key given as query parameter)
	 * @param key translation key
	 * @param parameters optional parameters
	 * @return translated string
	 */
	@GET
	@Path("/t")
	@Produces({ "text/plain; charset=UTF-8"})
	String translateFromParam(@QueryParam("k") String key, @QueryParam("p") List<String> parameters) throws Exception;

	/**
	 * Set the current session language
	 * @param request request to set the current session language
	 * @return response
	 */
	@POST
	@Path("/set")
	GenericResponse setLanguage(SetLanguageRequest request) throws Exception;

	/**
	 * Get the current session language
	 * @return session language
	 */
	@GET
	@Path("/get")
	LanguageResponse getLanguage() throws Exception;
}
