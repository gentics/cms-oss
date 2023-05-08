package com.gentics.contentnode.rest.resource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.response.LanguageListResponse;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.response.ContentLanguageResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

/**
 * Resource to manage cms languages
 * @HTTP 401 No valid sid and session secret cookie were provided.
 * @HTTP 403 User has insufficient permissions.
 */
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Path("language")
public interface LanguageResource {
	/**
	 * Load a list of languages
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @return response containing a list of languages
	 * @throws Exception
	 * @HTTP 200 The list of languages is returned.
	 */
	@GET
	LanguageList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Create a new content language
	 * @param language language
	 * @return Response containing the created content language
	 * @throws Exception
	 * @HTTP 200 The language has been created.
	 * @HTTP 400 No request body was sent.
	 * @HTTP 400 Not all required fields had non-null values.
	 * @HTTP 409 The given code or language name is already used by another language.
	 */
	@POST
	ContentLanguageResponse create(ContentLanguage language) throws Exception;

	/**
	 * Load the language with given id
	 * @param id language ID
	 * @return Response containing the requested language
	 * @throws Exception
	 * @HTTP 200 Language {id} is returned.
	 * @HTTP 404 Language {id} was not found.
	 */
	@GET
	@Path("/{id}")
	ContentLanguageResponse get(@PathParam("id") String id) throws Exception;

	/**
	 * Update the language with given id
	 * @param id language ID
	 * @param language Updated language
	 * @return Response containing the updated language
	 * @throws Exception
	 * @HTTP 200 Language {id} was updated.
	 * @HTTP 400 No request body was sent.
	 * @HTTP 400 Not all required fields had non-null values.
	 * @HTTP 404 Language {id} was not found.
	 * @HTTP 409 The given code or language name is already used by another language.
	 */
	@PUT
	@Path("/{id}")
	ContentLanguageResponse update(@PathParam("id") String id, ContentLanguage language) throws Exception;

	/**
	 * Delete the language with given ID
	 * @param id language ID
	 * @return empty response
	 * @throws Exception
	 * @HTTP 204 Language {id} was deleted.
	 * @HTTP 404 Language {id} was not found.
	 * @HTTP 409 Language {id} cannot be deleted, because it is used.
	 */
	@DELETE
	@Path("/{id}")
	Response delete(@PathParam("id") String id) throws Exception;

	/**
	 * Load all available languages
	 * @return list of available languages
	 * @throws Exception
	 * @HTTP 200 The list of languages is returned.
	 * @deprecated
	 */
	@GET
	@Path("list")
	LanguageListResponse list() throws Exception;
}
