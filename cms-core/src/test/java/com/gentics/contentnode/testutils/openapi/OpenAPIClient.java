package com.gentics.contentnode.testutils.openapi;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.AuthenticationResponse;
import org.openapitools.client.model.GenericResponse;
import org.openapitools.client.model.LoginRequest;
import org.openapitools.client.model.LoginResponse;
import org.openapitools.client.model.ResponseInfo;
import org.openapitools.client.model.VersionResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.exceptions.AuthRequiredRestException;
import com.gentics.contentnode.rest.client.exceptions.FailureRestException;
import com.gentics.contentnode.rest.client.exceptions.InvalidDataRestException;
import com.gentics.contentnode.rest.client.exceptions.MaintenanceModeRestException;
import com.gentics.contentnode.rest.client.exceptions.NotFoundRestException;
import com.gentics.contentnode.rest.client.exceptions.PermissionRestException;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.version.Main;

import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

/**
 * A class to test the generated client of OpenAPI specification
 */
public class OpenAPIClient implements RestClient {

	private CookieHandler cookieHandler = new CookieManager();
	private final DefaultApi client;
	private String sid;
	private WebTarget base;

	public OpenAPIClient(String url) {
		this.client = new DefaultApi();
		this.client.getApiClient().setBasePath(url.endsWith("/") ? url.substring(0, url.length()-1) : url);
		this.base = this.client.getApiClient().getHttpClient().target(url);
		// add client filters to handle cookies
		base.register((ClientRequestFilter) requestContext -> {
			Map<String, List<String>> cookies = cookieHandler.get(requestContext.getUri(), requestContext.getStringHeaders());
			for (Map.Entry<String, List<String>> entry : cookies.entrySet()) {
				for (String value : entry.getValue()) {
					requestContext.getHeaders().add(entry.getKey(), value);
				}
			}
		}).register((ClientResponseFilter) (requestContext, responseContext) -> {
			cookieHandler.put(requestContext.getUri(), responseContext.getHeaders());
		});
	}

	@Override
	public void login(String username, String password) throws RestException {
		LoginRequest request = new LoginRequest();

		request.setLogin(username);
		request.setPassword(password);
		try {
			LoginResponse response = client.login(sid, request);
			assertResponseObject(response);
			assertResponseInfo(response.getResponseInfo());
			this.sid = response.getSid();
		} catch (ApiException e) {
			throw new RestException("OpenAPI login failed", e);
		}
		// TODO FIXME We need a fully featured CMS API REST client, in order to perform targeted API calls, 
		// and not building Jersey context manually
		LoginResponse response = base.path("auth").path("login").request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), LoginResponse.class);

		assertResponseObject(response);
		assertResponseInfo(response.getResponseInfo());
		sid = response.getSid();
	}

	@Override
	public void ssologin() throws RestException {
		String response;
		try {
			response = client.ssoLogin();
			if (response.equals(ResponseCode.NOTFOUND.toString())) {
				throw new NotFoundRestException("");
			} else if (response.equals(ResponseCode.FAILURE.toString())) {
				throw new FailureRestException("");
			} else {
				sid = response;
			}
		} catch (ApiException e) {
			throw new RestException("OpenAPI sso login failed", e);
		}

		// TODO FIXME We need a fully featured CMS API REST client, in order to perform targeted API calls, 
		// and not building Jersey context manually
		response = base.path("auth").path("ssologin").request(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.TEXT_PLAIN).get(String.class);
		if (response.equals(ResponseCode.NOTFOUND.toString())) {
			throw new NotFoundRestException("");
		} else if (response.equals(ResponseCode.FAILURE.toString())) {
			throw new FailureRestException("");
		} else {
			sid = response;
		}
	}

	@Override
	public void logout() throws RestException {
		GenericResponse response;
//		try {
//			response = client.logout(sid, true);
//			sid = null;
//			assertResponseObject(response);
//			assertResponseInfo(response.getResponseInfo());
//		} catch (ApiException e) {
//			throw new RestException("OpenAPI logout failed", e);
//		}

		// TODO FIXME We need a fully featured CMS API REST client, in order to perform targeted API calls, 
		// and not building Jersey context manually
		response = base.path("auth").path("logout").path(sid).request(MediaType.APPLICATION_JSON_TYPE).post(null, GenericResponse.class);
		sid = null;
		assertResponseObject(response);
		assertResponseInfo(response.getResponseInfo());
	}

	@Override
	public User authenticate(String sid, String sessionSecret) throws RestException {
		try {
			AuthenticationResponse response = client.validate(sid + sessionSecret);
			assertResponseObject(response);
			assertResponseInfo(response.getResponseInfo());
		} catch (ApiException e) {
			throw new RestException("OpenAPI authenticate failed", e);
		}
		return null;
	}

	public void assertResponseObject(Object response) throws RestException {
		if (response == null) {
			throw new RestException("No response returned.");
		}
	}

	public void assertResponseInfo(ResponseInfo responseInfo) throws RestException {
		if (responseInfo == null) {
			throw new RestException("No response-information contained in the given response.");
		} else {
			String msg = responseInfo.getResponseMessage();

			switch (responseInfo.getResponseCode()) {
			case OK:
				// Response successfully received
				break;

			case INVALIDDATA:
				throw new InvalidDataRestException(msg);

			case PERMISSION:
				throw new PermissionRestException(msg);

			case MAINTENANCEMODE:
				throw new MaintenanceModeRestException(msg);

			case NOTFOUND:
				throw new NotFoundRestException(msg);

			case FAILURE:
				throw new FailureRestException(msg);

			case AUTHREQUIRED:
				throw new AuthRequiredRestException(msg);

			default:
				throw new RestException(msg);
			}
		}
	}

	@Override
	public void assertMatchingVersion() throws RestException {
		VersionResponse serverVersion;
		try {
			serverVersion = client.currentVersion();
			assertResponseObject(serverVersion);
		} catch (ApiException e) {
			throw new RestException("OpenAPI version failed", e);
		}
		String clientVersion = Main.getImplementationVersion();

		if (!clientVersion.equals(serverVersion.getVersion())) {
			throw new RestException("The version of the client does not match the version on the server!");
		}
	}

	@Override
	public WebTarget base() throws RestException {
		if (sid != null) {
			return base.queryParam("sid", sid);
		} else {
			throw new AuthRequiredRestException("No valid SID is associated with this client. Log in first!");
		}
	}

	@Override
	public String getSid() {
		return sid;
	}

	@Override
	public void setSid(String sid) {
		this.sid = sid;
	}

	@Override
	public List<Cookie> getCookies() throws RestException {
		CookieStore cookieStore = ApacheConnectorProvider.getCookieStore(client.getApiClient().getHttpClient());
		if (cookieStore != null) {
			return cookieStore.getCookies();
		} else {
			throw new RestException("Could not find CookieStore for Client");
		}
	}

	@Override
	public CookieHandler getCookieHandler() {
		return cookieHandler;
	}

	public DefaultApi getGeneratedClient() {
		return client;
	}

	@Override
	public void assertResponse(com.gentics.contentnode.rest.model.response.GenericResponse response) throws RestException {
		assertResponseObject(response);
		ObjectMapper mapper = Synchronizer.mapper();
		try {
			String responseString = mapper.writeValueAsString(response.getResponseInfo());
			ResponseInfo responseInfo = mapper.readValue(responseString, org.openapitools.client.model.ResponseInfo.class);
			assertResponseInfo(responseInfo);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
