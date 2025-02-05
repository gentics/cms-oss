package com.gentics.contentnode.rest.resource.impl;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.request.SetLanguageRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LanguageResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.UILanguagesResponse;
import com.gentics.contentnode.rest.resource.I18nResource;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Resource to translate given keys (optionally including parameters)
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/i18n")
public class I18nResourceImpl implements I18nResource {
	@Override
	@GET
	@Path("/list")
	public UILanguagesResponse list() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {

			UILanguagesResponse response = ListBuilder
					.from(UserLanguageFactory.getActive(), UserLanguage.TRANSFORM2REST).to(new UILanguagesResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/t/{key}")
	@Produces({ "text/plain; charset=UTF-8"})
	public String translateFromPath(@PathParam("key") String key, @QueryParam("p") List<String> parameters) throws NodeException {
		return translate(key, parameters);
	}

	@Override
	@GET
	@Path("/t")
	@Produces({ "text/plain; charset=UTF-8"})
	public String translateFromParam(@QueryParam("k") String key, @QueryParam("p") List<String> parameters) throws NodeException {
		return translate(key, parameters);
	}

	@Override
	@POST
	@Path("/set")
	public GenericResponse setLanguage(SetLanguageRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UserLanguage language = UserLanguageFactory.getByCode(request.getCode());

			if (language == null) {
				throw new WebApplicationException(
						Response.status(Status.OK).entity(new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid language code provided"))).build());
			}

			DBUtils.update("UPDATE systemsession SET language = ? WHERE id = ?", language.getId(), trx.getTransaction().getSession().getSessionId());
			trx.success();

			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully modified language"));
		}
	}

	@Override
	@GET
	@Path("/get")
	public LanguageResponse getLanguage() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			int languageId = trx.getTransaction().getSession().getLanguageId();
			LanguageResponse response = new LanguageResponse();

			UserLanguage language = UserLanguageFactory.getById(languageId);

			if (language == null) {
				throw new WebApplicationException(
						Response.status(Status.OK).entity(new GenericResponse(null, new ResponseInfo(ResponseCode.FAILURE, "Invalid language found in session"))).build());
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully returned language"));
			response.setCode(language.getCode());

			trx.success();
			return response;
		}
	}

	/**
	 * Do the actual translation
	 * @param key translation key
	 * @param parameters optional parameters
	 * @return translated string
	 * @throws NodeException
	 */
	protected String translate(String key, List<String> parameters) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			CNI18nString i18nString = new CNI18nString(key);

			if (parameters != null) {
				for (String p : parameters) {
					i18nString.addParameter(p);
				}
			}
			trx.success();
			return i18nString.toString();
		}
	}
}
