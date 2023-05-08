package com.gentics.contentnode.rest.resource.impl.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import io.reactivex.Flowable;

/**
 * Resource implementation for custom proxy
 */
@Path("/proxy")
@Authenticated
public class ProxyResource {
	/**
	 * Response header that shall not be forwarded
	 */
	protected final static List<String> OMIT_RESPONSE_HEADERS = Arrays.asList("transfer-encoding");

	/**
	 * Value of the issuer claim
	 */
	protected final static String JWT_ISSUER = "Gentics CMS";

	/**
	 * Name of the claim containing the gcms groups
	 */
	protected final static String GROUPS_CLAIM = "gcms_groups";

	/**
	 * Proxy key (part of the request path)
	 */
	@PathParam("key")
	protected String key;

	/**
	 * URL path, that shall be forwarded to the proxied request
	 */
	@PathParam("path")
	protected String path;

	@Context
	protected UriInfo uriInfo;

	@Context
	protected HttpHeaders httpHeaders;

	/**
	 * DELETE request without extra path
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@DELETE
	@Path("/{key}")
	public Response deleteNoPath() throws NodeException, HttpException, IOException {
		return delete();
	}

	/**
	 * GET request without extra path
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@GET
	@Path("/{key}")
	public Response getNoPath() throws NodeException, HttpException, IOException {
		return get();
	}

	/**
	 * HEAD request without extra path
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@HEAD
	@Path("/{key}")
	public Response headNoPath() throws NodeException, HttpException, IOException {
		return head();
	}

	/**
	 * OPTIONS request without extra path
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@OPTIONS
	@Path("/{key}")
	public Response optionsNoPath() throws NodeException, HttpException, IOException {
		return options();
	}

	/**
	 * POST request without extra path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@POST
	@Path("/{key}")
	public Response postNoPath(InputStream requestBody) throws NodeException, HttpException, IOException {
		return post(requestBody);
	}

	/**
	 * PUT request without extra path
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@PUT
	@Path("/{key}")
	public Response putNoPath(InputStream requestBody) throws NodeException, HttpException, IOException {
		return put(requestBody);
	}

	/**
	 * DELETE request
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@DELETE
	@Path("/{key}/{path: .*}")
	public Response delete() throws NodeException, HttpException, IOException {
		return forward(new DeleteMethod());
	}

	/**
	 * GET request
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@GET
	@Path("/{key}/{path: .*}")
	public Response get() throws NodeException, HttpException, IOException {
		return forward(new GetMethod());
	}

	/**
	 * HEAD request
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@HEAD
	@Path("/{key}/{path: .*}")
	public Response head() throws NodeException, HttpException, IOException {
		return forward(new HeadMethod());
	}

	/**
	 * OPTIONS request
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@OPTIONS
	@Path("/{key}/{path: .*}")
	public Response options() throws NodeException, HttpException, IOException {
		return forward(new OptionsMethod());
	}

	/**
	 * POST request
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@POST
	@Path("/{key}/{path: .*}")
	public Response post(InputStream requestBody) throws NodeException, HttpException, IOException {
		return forward(new PostMethod(), m -> {
			if (requestBody != null) {
				m.setRequestEntity(new InputStreamRequestEntity(requestBody));
			}
		});
	}

	/**
	 * PUT request
	 * @param requestBody request body as InputStream
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	@PUT
	@Path("/{key}/{path: .*}")
	public Response put(InputStream requestBody) throws NodeException, HttpException, IOException {
		return forward(new PutMethod(), m -> {
			if (requestBody != null) {
				m.setRequestEntity(new InputStreamRequestEntity(requestBody));
			}
		});
	}

	/**
	 * Validate existence configuration for {@link #key} and required permissions
	 * @param methodName request method name ("GET", "POST", ...)
	 * @return custom proxy configuration
	 * @throws NodeException
	 */
	protected CustomProxy validate(String methodName) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
			Object configObject = prefs.getPropertyObject("custom_proxy");

			if (!(configObject instanceof Map)) {
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			Map<?, ?> configMap = (Map<?, ?>)configObject;

			if (!configMap.containsKey(key)) {
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			ObjectMapper mapper = new ObjectMapper();
			CustomProxy customProxy = mapper.convertValue(configMap.get(key), CustomProxy.class);

			if (!customProxy.allowAccess(methodName)) {
				throw new WebApplicationException(Status.FORBIDDEN);
			}

			return customProxy;
		}
	}

	/**
	 * Forward the request to the given request method
	 * @param method request method
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	protected <T extends HttpMethodBase> Response forward(T method) throws NodeException, HttpException, IOException {
		return forward(method, null);
	}

	/**
	 * Forward the request to the given request method, optionally prepare the request with the prepareHandler
	 * @param method request method
	 * @param prepareHandler optional prepare handler
	 * @return response
	 * @throws NodeException
	 * @throws HttpException
	 * @throws IOException
	 */
	protected <T extends HttpMethodBase> Response forward(T method, Consumer<T> prepareHandler) throws NodeException, HttpException, IOException {
		CustomProxy customProxy = validate(method.getName());

		String baseUrl = customProxy.getBaseUrl(uriInfo.getQueryParameters());
		if (StringUtils.isBlank(path)) {
			method.setPath(baseUrl);
		} else {
			// prepare the path to be safe
			List<String> encodedSegments = Flowable.fromArray(StringUtils.split(path, '/')).map(segment -> URLEncoder.encode(segment, "UTF-8"))
					.toList().blockingGet();
			String encodedPath = StringUtils.join(encodedSegments, '/');
			method.setPath(String.format(baseUrl.endsWith("/") ? "%s%s" : "%s/%s", baseUrl, encodedPath));
		}
		NameValuePair[] queryParams = uriInfo.getQueryParameters().entrySet().stream().flatMap(entry -> {
			String key = entry.getKey();
			return entry.getValue().stream().map(value -> new NameValuePair(key, value));
		}).toArray(NameValuePair[]::new);
		method.setQueryString(queryParams);

		for (Map.Entry<String, List<String>> headerEntry : httpHeaders.getRequestHeaders().entrySet()) {
			// TODO filter headers to be added
			String name = headerEntry.getKey();
			for (String value : headerEntry.getValue()) {
				method.addRequestHeader(name, value);
			}
		}

		if (customProxy.getHeaders() != null) {
			for (Map.Entry<String, String> headerEntry : customProxy.getHeaders().entrySet()) {
				method.addRequestHeader(headerEntry.getKey(), headerEntry.getValue());
			}
		}

		if (prepareHandler != null) {
			prepareHandler.accept(method);
		}

		CustomProxyJWT jwtConfig = customProxy.getJwt();
		if (jwtConfig.isEnabled()) {
			SystemUser user = null;
			Set<String> groupNames;
			try (Trx trx = ContentNodeHelper.trx()) {
				Transaction t = trx.getTransaction();
				user = t.getObject(SystemUser.class, t.getUserId());
				groupNames = user.getUserGroups().stream().map(group -> jwtConfig.prefix(group.getName()))
						.collect(Collectors.toSet());
				trx.success();
			}

			try {
				String login = jwtConfig.prefix(user.getLogin());
				JwtBuilder builder = Jwts.builder().setSubject(login)
					.claim("preferred_username", login)
					.claim("given_name", user.getFirstname())
					.claim("family_name", user.getLastname())
					.claim("email", user.getEmail())
					.claim(GROUPS_CLAIM, groupNames)
					.setIssuer(JWT_ISSUER)
					.setIssuedAt(new Date());
				String encodedJwt = KeyProvider.sign(builder).compact();
				method.addRequestHeader("Authorization", String.format("Bearer %s", encodedJwt));
			} catch (Exception e) {
				throw new NodeException(e);
			}
		}

		HttpClient httpClient = new HttpClient();
		CustomProxyProxy httpProxy = customProxy.getProxy();
		if (httpProxy != null) {
			HostConfiguration hostConfiguration = new HostConfiguration();
			hostConfiguration.setProxy(httpProxy.getHost(), httpProxy.getPort());
			httpClient.setHostConfiguration(hostConfiguration);
		}
		int responseCode = httpClient.executeMethod(method);

		ResponseBuilder responseBuilder = Response.status(responseCode);
		for (Header header : method.getResponseHeaders()) {
			String name = header.getName();
			if (OMIT_RESPONSE_HEADERS.contains(name.toLowerCase())) {
				continue;
			}
			responseBuilder.header(name, header.getValue());
		}
		return responseBuilder.entity(method.getResponseBodyAsStream()).build();
	}
}
