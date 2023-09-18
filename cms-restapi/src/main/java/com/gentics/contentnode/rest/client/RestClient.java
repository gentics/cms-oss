package com.gentics.contentnode.rest.client;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.gentics.contentnode.rest.client.exceptions.AuthRequiredRestException;
import com.gentics.contentnode.rest.client.exceptions.FailureRestException;
import com.gentics.contentnode.rest.client.exceptions.InvalidDataRestException;
import com.gentics.contentnode.rest.client.exceptions.MaintenanceModeRestException;
import com.gentics.contentnode.rest.client.exceptions.NotFoundRestException;
import com.gentics.contentnode.rest.client.exceptions.PermissionRestException;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.response.AuthenticationResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.VersionResponse;
import com.gentics.contentnode.rest.version.Main;

/**
 * <p>This client provides wrappers, helper-methods and exception-handling to facilitate requests to the REST API.
 * It is initialized with a URL pointing to the base-location providing the services. After a successful login,
 * a WebTarget-object can be retrieved that is then used to assemble and send requests; response-objects are
 * returned from the server, containing the requested data and further information in the case of an error.</p>
 *
 * <p>The Rest API client builds upon an underlying Jersey-client: detailed information about the use of the
 * WebTarget-object (base) to build requests can be found at http://jersey.java.net/ .</p>
 */
public class RestClient {
	private WebTarget base;
	// cookie handler (for storing cookies per client)
	private CookieHandler cookieHandler = new CookieManager();
	private String sid;
	private JerseyClient jerseyClient;

	/**
	 * Initializes the REST-API client with a given base-URL and sets the necessary cookies;
	 * prepares the client for a subsequent login
	 *
	 * @param baseUrl An URL pointing to the base of the RESTful service provider
	 * (Example: http://[hostname]/CNPortletapp/rest/)
	 */
	public RestClient(String baseUrl) {
		this(() -> {
			ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
			return JerseyClientBuilder.createClient(clientConfig).register(ObjectMapperProvider.class).register(JacksonFeature.class)
					.register(MultiPartFeature.class);
		}, baseUrl);
	}

	/**
	 * Initialize the REST-API client with a Jersey Client supplied by the given clientSupplier and the base URL.
	 * 
	 * The default Jersey Client is created like this:
	 * 
	 * <pre>
	 * new RestClient(() -> {
	 * 		ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
	 * 		return JerseyClientBuilder.createClient(clientConfig).register(JacksonFeature.class).register(MultiPartFeature.class);
	 * }, "http://myhost/CNPortletapp/rest");
	 * </pre>
	 * @param clientSupplier supplier for the Jersey Client
	 * @param baseUrl URL pointing to the base of the REST service provider (Example: http://[hostname]/CNPortletapp/rest/)
	 */
	public RestClient(Supplier<JerseyClient> clientSupplier, String baseUrl) {
		jerseyClient = clientSupplier.get();
		base = jerseyClient.target(baseUrl);
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

	/**
	 * Logs the specified user into the system using the password given
	 *
	 * @param username user name
	 * @param password password
	 * @throws RestException If the login failed
	 */
	public void login(String username, String password) throws RestException {
		LoginRequest request = new LoginRequest();

		request.setLogin(username);
		request.setPassword(password);
		LoginResponse response = base.path("auth").path("login").request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), LoginResponse.class);

		assertResponse(response);

		sid = response.getSid();
	}

	/**
	 * Performs login on an SSO system - before this works,
	 * necessary filters have to be defined
	 *
	 * @throws RestException If the login via SSO failed
	 */
	public void ssologin() throws RestException {
		String response = base.path("auth").path("ssologin").request(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.TEXT_PLAIN).get(String.class);

		if (response.equals(ResponseCode.NOTFOUND.toString())) {
			throw new NotFoundRestException("");
		} else if (response.equals(ResponseCode.FAILURE.toString())) {
			throw new FailureRestException("");
		} else {
			sid = response;
		}
	}

	/**
	 * Logs out the current user
	 *
	 * @throws RestException If the logout failed
	 */
	public void logout() throws RestException {
		GenericResponse response = base.path("auth").path("logout").path(sid).request(MediaType.APPLICATION_JSON_TYPE).post(null, GenericResponse.class);

		sid = null;
		assertResponse(response);
	}

	/**
	 * Authenticate with given sid and session secret
	 * @param sid SID
	 * @param sessionSecret session secret
	 * @return user
	 * @throws RestException if authentication fails
	 */
	public User authenticate(String sid, String sessionSecret) throws RestException {
		AuthenticationResponse response = base.path("auth").path("validate").path(sid + sessionSecret).request(MediaType.APPLICATION_JSON_TYPE)
				.get(AuthenticationResponse.class);
		assertResponse(response);
		return response.getUser();
	}

	/**
	 * Analyzes the response of a finished request and asserts that it was executed without errors;
	 * if a problem occurred during the request, a specialized RestException is thrown
	 *
	 * @param response The response of the request that needs to be checked
	 * @throws RestException Thrown if the request was not successful, and contains further information of the reason of failure
	 */
	public void assertResponse(GenericResponse response) throws RestException {
		if (response == null) {
			throw new RestException("No response returned.");
		} else if (response.getResponseInfo() == null) {
			throw new RestException("No response-information contained in the given response.");
		} else {
			String msg = response.getResponseInfo().getResponseMessage();

			switch (response.getResponseInfo().getResponseCode()) {
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

	/**
	 * Checks if the version of the REST API on the server is the same that is used
	 * by the client; if there is a mismatch between the two versions, a RestException is thrown
	 *
	 * @throws RestException Mismatch between the versions detected
	 */
	public void assertMatchingVersion() throws RestException {
		VersionResponse serverVersion = base.queryParam("sid", sid).path("admin").path("version").request(MediaType.APPLICATION_JSON_TYPE)
				.get(VersionResponse.class);

		assertResponse(serverVersion);
		String clientVersion = Main.getImplementationVersion();

		if (!clientVersion.equals(serverVersion.getVersion())) {
			throw new RestException("The version of the client does not match the version on the server!");
		}
	}

	/**
	 * Provides access to the WebTarget that is used as the base for all commands to the server
	 *
	 * @return The base resource, with the active SID already set
	 * @throws RestException If no valid SID is registered with the client
	 */
	public WebTarget base() throws RestException {
		if (sid != null) {
			return base.queryParam("sid", sid);
		} else {
			throw new AuthRequiredRestException("No valid SID is associated with this client. Log in first!");
		}
	}

	/**
	 * Get the ID of the active session, as generated during login
	 * @return session ID
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * Set the ID of the session that should be used.
	 */
	public void setSid(String sid) {
		this.sid = sid;
	}

	/**
	 * Get the underlying Jersey Client, used to define filters and configure advanced settings
	 * @return Jersey Client
	 */
	public Client getJerseyClient() {
		return jerseyClient;
	}

	/**
	 * Get the cookies currently stored in the client
	 * @return stored cookies
	 * @throws RestException
	 */
	public List<Cookie> getCookies() throws RestException {
		CookieStore cookieStore = ApacheConnectorProvider.getCookieStore(jerseyClient);
		if (cookieStore != null) {
			return cookieStore.getCookies();
		} else {
			throw new RestException("Could not find CookieStore for Client");
		}
	}

	/**
	 * Get the cookie handler
	 * @return cookie handler
	 */
	public CookieHandler getCookieHandler() {
		return cookieHandler;
	}
}
