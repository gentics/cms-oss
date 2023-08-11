package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.assertMeshCr;
import static com.gentics.contentnode.rest.util.MiscUtils.load;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.ContentRepositoryProxyResource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.mesh.core.rest.auth.LoginRequest;

import io.reactivex.Flowable;

/**
 * Implementation of {@link ContentRepositoryProxyResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/contentrepositories")
@Authenticated
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = ContentRepository.TYPE_CONTENTREPOSITORIES, bit = PermHandler.PERM_VIEW)
public class ContentRepositoryProxyResourceImpl implements ContentRepositoryProxyResource {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ContentRepositoryProxyResourceImpl.class);

	/**
	 * Lowercase names of request headers, which will not be forwarded
	 */
	protected final static List<String> IGNORED_REQUEST_HEADERS = Arrays
			.asList(StringUtils.lowerCase(HttpHeaders.CONTENT_LENGTH));

	/**
	 * Lowercase names of response headers, which will not be forwarded
	 */
	protected final static List<String> IGNORED_RESPONSE_HEADERS = Arrays.asList(
			StringUtils.lowerCase(HttpHeaders.CONTENT_LENGTH));

	@Context
	protected UriInfo uriInfo;

	@Context
	protected HttpHeaders httpHeaders;

	@Override
	@DELETE
	@Path("/{id}/proxy")
	public Response deleteNoPath(@PathParam("id") String id) throws NodeException {
		return delete(id, null);
	}

	@Override
	@GET
	@Path("/{id}/proxy")
	public Response getNoPath(@PathParam("id") String id) throws NodeException {
		return get(id, null);
	}

	@Override
	@HEAD
	@Path("/{id}/proxy")
	public Response headNoPath(@PathParam("id") String id) throws NodeException {
		return head(id, null);
	}

	@Override
	@OPTIONS
	@Path("/{id}/proxy")
	public Response optionsNoPath(@PathParam("id") String id) throws NodeException {
		return options(id, null);
	}

	@Override
	@POST
	@Path("/{id}/proxy")
	public Response postNoPath(@PathParam("id") String id, InputStream requestBody) throws NodeException {
		return post(id, null, requestBody);
	}

	@Override
	@PUT
	@Path("/{id}/proxy")
	public Response putNoPath(@PathParam("id") String id, InputStream requestBody) throws NodeException {
		return put(id, null, requestBody);
	}

	@Override
	@DELETE
	@Path("/{id}/proxy/{path: .*}")
	public Response delete(@PathParam("id") String id, @PathParam("path") String path) throws NodeException {
		return forward(id, path, new HttpDelete());
	}

	@Override
	@GET
	@Path("/{id}/proxy/{path: .*}")
	public Response get(@PathParam("id") String id, @PathParam("path") String path) throws NodeException {
		return forward(id, path, new HttpGet());
	}

	@Override
	@HEAD
	@Path("/{id}/proxy/{path: .*}")
	public Response head(@PathParam("id") String id, @PathParam("path") String path) throws NodeException {
		return forward(id, path, new HttpHead());
	}

	@Override
	@OPTIONS
	@Path("/{id}/proxy/{path: .*}")
	public Response options(@PathParam("id") String id, @PathParam("path") String path) throws NodeException {
		return forward(id, path, new HttpOptions());
	}

	@Override
	@POST
	@Path("/{id}/proxy/{path: .*}")
	public Response post(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws NodeException {
		return forward(id, path, new HttpPost(), m -> {
			if (requestBody != null) {
				m.setEntity(new InputStreamEntity(requestBody));
			}
		});
	}

	@Override
	@PUT
	@Path("/{id}/proxy/{path: .*}")
	public Response put(@PathParam("id") String id, @PathParam("path") String path, InputStream requestBody) throws NodeException {
		return forward(id, path, new HttpPut(), m -> {
			if (requestBody != null) {
				m.setEntity(new InputStreamEntity(requestBody));
			}
		});
	}

	@Override
	@POST
	@Path("/{id}/proxylogin")
	public Response login(@PathParam("id") String id) throws NodeException {
		ContentRepository cr = validate(id);

		String username = cr.getUsername();
		String password = cr.getPassword();

		if (StringUtils.isBlank(password)) {
			throw new RestMappedException(I18NHelper.get("meshcr.apitoken.missing", cr.getName())).setMessageType(Message.Type.CRITICAL)
				.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		} else if (StringUtils.isBlank(username)) {
			// password is an API token
			return forward(id, "/api/v2/auth/login", new HttpGet(), m -> {
				m.setHeader("Authorization", "Bearer " + password);
			});
		} else {
			// do login request
			return forward(id, "/api/v2/auth/login", new HttpPost(), m -> {
				LoginRequest loginRequest = new LoginRequest();
				loginRequest.setUsername(username);
				loginRequest.setPassword(password);
				m.setEntity(new StringEntity(loginRequest.toJson(), ContentType.APPLICATION_JSON));
			});
		}
	}

	/**
	 * Validate existence and permission of Mesh CR with given id
	 * @param id CR id
	 * @return Mesh CR instance
	 * @throws NodeException
	 */
	protected ContentRepository validate(String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = load(ContentRepository.class, id, ObjectPermission.edit);

			if (cr.getCrType() != ContentRepositoryModel.Type.mesh) {
				assertMeshCr(cr);
			}
			return cr;
		}
	}

	/**
	 * Forward the request to the given request method
	 * @param id CR id
	 * @param path request path (may be null)
	 * @param method request method
	 * @return response
	 * @throws NodeException
	 */
	protected <T extends HttpRequestBase> Response forward(String id, String path, T method) throws NodeException {
		return forward(id, path, method, null);
	}

	/**
	 * Forward the request to the given request method, optionally prepare the request with the prepareHandler
	 * @param id CR id
	 * @param path request path (may be null)
	 * @param method request method
	 * @param prepareHandler optional prepare handler
	 * @return response
	 * @throws NodeException
	 */
	protected <T extends HttpRequestBase> Response forward(String id, String path, T method, Consumer<T> prepareHandler) throws NodeException {
		ContentRepository cr = validate(id);

		Matcher urlMatcher = MeshPublisher.URL_PATTERN.matcher(cr.getUrl());
		if (!urlMatcher.matches()) {
			throw new RestMappedException(I18NHelper.get("meshcr.invalid.url", cr.getUrl(), cr.getName())).setMessageType(Message.Type.CRITICAL)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		boolean ssl = false;
		int defaultPort = 80;
		if (urlMatcher.group("protocol") != null) {
			String protocol = urlMatcher.group("protocol");
			if (protocol.startsWith("https")) {
				ssl = true;
				defaultPort = 443;
			}
		}
		String host = urlMatcher.group("host");
		int port = defaultPort;
		if (urlMatcher.group("port") != null) {
			port = Integer.parseInt(urlMatcher.group("port"));
		}

		try {
			String url = null;
			String baseUrl = String.format("%s://%s:%d/", ssl ? "https" : "http", host, port);
			if (StringUtils.isBlank(path)) {
				url = baseUrl;
			} else {
				// prepare the path to be safe
				List<String> encodedSegments = Flowable.fromArray(StringUtils.split(path, '/')).map(segment -> URLEncoder.encode(segment, "UTF-8"))
						.toList().blockingGet();
				String encodedPath = StringUtils.join(encodedSegments, '/');
				url = String.format(baseUrl.endsWith("/") ? "%s%s" : "%s/%s", baseUrl, encodedPath);
			}
			URIBuilder uriBuilder = new URIBuilder(url);
			uriInfo.getQueryParameters().entrySet().stream().forEach(entry -> {
				String param = entry.getKey();
				entry.getValue().forEach(value -> {
					uriBuilder.addParameter(param, value);
				});
			});
			method.setURI(uriBuilder.build());
		} catch (URISyntaxException e) {
			throw new NodeException(e);
		}

		for (Map.Entry<String, List<String>> headerEntry : httpHeaders.getRequestHeaders().entrySet()) {
			String name = headerEntry.getKey();
			if (IGNORED_REQUEST_HEADERS.contains(StringUtils.lowerCase(name))) {
				continue;
			}
			for (String value : headerEntry.getValue()) {
				method.addHeader(name, value);
			}
		}

		if (prepareHandler != null) {
			prepareHandler.accept(method);
		}

		try {
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			CloseableHttpResponse response = httpClient.execute(method);
			int responseCode = response.getStatusLine().getStatusCode();

			ResponseBuilder responseBuilder = Response.status(responseCode);
			for (Header header : response.getAllHeaders()) {
				String name = header.getName();
				if (IGNORED_RESPONSE_HEADERS.contains(StringUtils.lowerCase(name))) {
					continue;
				}
				String value = header.getValue();
				if (StringUtils.equalsIgnoreCase(name, HttpHeaders.SET_COOKIE)) {
					value = value.replaceFirst("Path=[^\\s]+", String.format("Path=%s/contentrepositories/%s/proxy/",
							StringUtils.removeEnd(uriInfo.getBaseUri().getPath().toString(), "/"), id));
				}
				responseBuilder.header(name, value);
			}

			// add new StreamingResource implementation, that closes the response after streaming everything (or when streaming fails)
			StreamingOutput streamingOutput = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						IOUtils.copy(response.getEntity().getContent(), output);
					} finally {
						response.close();
						httpClient.close();
					}
				}
			};
			return responseBuilder.entity(streamingOutput).build();
		} catch (UnknownHostException e) {
			throw new RestMappedException(I18NHelper.get("meshcr.unknown.host", cr.getName(), cr.getUrl())).setMessageType(Message.Type.CRITICAL)
				.setResponseCode(ResponseCode.FAILURE).setStatus(Status.BAD_GATEWAY);
		} catch (HttpHostConnectException e) {
			throw new RestMappedException(I18NHelper.get("meshcr.connect.error", cr.getName(), cr.getUrl())).setMessageType(Message.Type.CRITICAL)
				.setResponseCode(ResponseCode.FAILURE).setStatus(Status.BAD_GATEWAY);
		} catch (IOException e) {
			logger.error(String.format("Error while connecting to Mesh CR %s via URL %s", cr.getName(), cr.getUrl()), e);
			throw new RestMappedException(I18NHelper.get("meshcr.io.error", cr.getUrl(), cr.getName())).setMessageType(Message.Type.CRITICAL)
				.setResponseCode(ResponseCode.FAILURE).setStatus(Status.BAD_GATEWAY);
		}
	}
}
