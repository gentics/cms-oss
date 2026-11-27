package com.gentics.contentnode.server;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.SYSTEM_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for proxying requests to static files to the portal, when a page preview is rendered by the portal
 */
public class PortalStaticFileTest {
	/**
	 * Name of the devtool package containing a static file
	 */
	private final static String PACKAGE_NAME = "portalstatic";

	/**
	 * Content of the static file in the devtool package
	 */
	private final static String PACKAGE_FILE_CONTENT = "/* from package */";

	/**
	 * Content rendered by the fake portal for the page preview
	 */
	private final static String PORTAL_PREVIEW_CONTENT = "<html><head><link rel=\"stylesheet\" href=\"/static/portal.css\"></head><body>Portal preview</body></html>";

	/**
	 * Prefix of the content returned by the fake portal for static files (followed by the request URI)
	 */
	private final static String PORTAL_STATIC_PREFIX = "portal static file ";

	private static DBTestContext testContext = new DBTestContext();

	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);

	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Fake portal
	 */
	private static HttpServer portal;

	/**
	 * Cookie headers, the fake portal received with requests to static files
	 */
	private static List<String> portalReceivedCookies = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Node with mesh preview URL
	 */
	private static Node portalNode;

	/**
	 * Node without mesh preview URL
	 */
	private static Node otherNode;

	/**
	 * Page in the node with mesh preview URL
	 */
	private static Page portalPage;

	/**
	 * Page in the node without mesh preview URL
	 */
	private static Page otherPage;

	private List<Cookie> storedCookies = Collections.synchronizedList(new ArrayList<>());

	private OkHttpClient client;

	/**
	 * Session secret of the logged in user "tester"
	 */
	private String sessionSecret;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		portal = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		portal.createContext("/", PortalStaticFileTest::handlePortalRequest);
		portal.start();

		File filesDir = new File(new File(ConfigurationValue.PACKAGES_PATH.get(), PACKAGE_NAME), "files");
		filesDir.mkdirs();
		FileUtils.write(new File(filesDir, "existing.css"), PACKAGE_FILE_CONTENT, StandardCharsets.UTF_8);

		Integer meshCrId = createMeshCR("localhost", 1234, "portalstatic");

		portalNode = supply(() -> createNode("portal", "Portal Node", PublishTarget.NONE));
		portalNode = update(portalNode, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(meshCrId);
			n.setMeshPreviewUrl(String.format("http://localhost:%d/preview", portal.getAddress().getPort()));
		}).build();
		// node publishing into the same mesh CR, but without mesh preview URL
		otherNode = supply(() -> createNode("other", "Other Node", PublishTarget.NONE));
		otherNode = update(otherNode, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(meshCrId);
		}).build();

		portalPage = createPage(portalNode);
		otherPage = createPage(otherNode);

		UserGroup group = supply(t -> t.getObject(UserGroup.class, SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
		// user without permission on the portal node (the new group gets a copy of the permissions of the mother group, so revoke them)
		UserGroup noPermGroup = supply(() -> createUserGroup("No Permission", SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("NoPerm", "NoPerm", null, "noperm", "noperm", Arrays.asList(noPermGroup)));
		String noPerms = new PermHandler.Permission(new int[0]).toString();
		operate(() -> {
			PermHandler.setPermissions(Folder.TYPE_FOLDER, portalNode.getFolder().getId(), Arrays.asList(noPermGroup), noPerms);
			PermHandler.setPermissions(Node.TYPE_NODE, portalNode.getFolder().getId(), Arrays.asList(noPermGroup), noPerms);
		});
		String perms = new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString();
		operate(() -> {
			for (Node node : Arrays.asList(portalNode, otherNode)) {
				PermHandler.setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(group), perms);
				PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(group), perms);
			}
		});
	}

	@AfterClass
	public static void tearDownOnce() {
		if (portal != null) {
			portal.stop(0);
		}
	}

	/**
	 * Create a page in the given node
	 * @param node node
	 * @return page
	 * @throws Exception
	 */
	private static Page createPage(Node node) throws Exception {
		Template template = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Template");
			tmpl.setSource("<html><head></head><body>CMS rendered</body></html>");
		}).build();
		return create(Page.class, p -> {
			p.setFolder(node, node.getFolder());
			p.setTemplateId(template.getId());
			p.setName("Page");
		}).unlock().build();
	}

	/**
	 * Handle a request to the fake portal
	 * @param exchange exchange
	 * @throws IOException
	 */
	private static void handlePortalRequest(HttpExchange exchange) throws IOException {
		String uri = exchange.getRequestURI().toString();
		byte[] body;
		if ("POST".equals(exchange.getRequestMethod())) {
			exchange.getRequestBody().readAllBytes();
			body = PORTAL_PREVIEW_CONTENT.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
		} else if (uri.startsWith("/static/")) {
			portalReceivedCookies.addAll(exchange.getRequestHeaders().getOrDefault("Cookie", Collections.emptyList()));
			body = (PORTAL_STATIC_PREFIX + uri).getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/css");
			exchange.getResponseHeaders().add("X-Portal", "true");
			exchange.getResponseHeaders().add("Set-Cookie", "portalcookie=bla; Path=/");
		} else {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
			return;
		}

		if ("HEAD".equals(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(200, -1);
		} else {
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		}
		exchange.close();
	}

	@Before
	public void setup() throws Exception {
		portalReceivedCookies.clear();
		client = new OkHttpClient.Builder()
			.callTimeout(Duration.ofSeconds(10))
			.cookieJar(new CookieJar() {
				@Override
				public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
					storedCookies.addAll(cookies);
				}

				@Override
				public List<Cookie> loadForRequest(HttpUrl url) {
					return storedCookies;
				}
			})
			.build();

		sessionSecret = login("tester", "tester");
	}

	/**
	 * Login with the given credentials (using the client with the cookie jar)
	 * @param login login
	 * @param password password
	 * @return session secret
	 * @throws IOException
	 */
	private String login(String login, String password) throws IOException {
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setLogin(login);
		loginRequest.setPassword(password);
		try (Response response = client.newCall(new Request.Builder().url(url("/rest/auth/login").build())
				.post(RequestBody.create(mapper.writeValueAsBytes(loginRequest), MediaType.parse("application/json"))).build()).execute()) {
			assertThat(response.code()).as("Login response code").isEqualTo(200);
			return Cookie.parseAll(response.request().url(), response.headers()).stream()
					.filter(cookie -> SessionToken.SESSION_SECRET_COOKIE_NAME.equals(cookie.name())).map(Cookie::value).findFirst()
					.orElseThrow(() -> new AssertionError("Login response does not set the session secret cookie"));
		}
	}

	/**
	 * Rendering the preview with the AlohaPageServlet by the portal must set the cookie
	 * @throws Exception
	 */
	@Test
	public void testAlohaPageSetsCookie() throws Exception {
		try (Response response = client.newCall(new Request.Builder()
				.url(url("/alohapage").addQueryParameter("real", "newview").addQueryParameter("realid", portalPage.getId().toString()).build())
				.build()).execute()) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Rendered preview").contains("Portal preview");
			assertPreviewCookie(response.headers("Set-Cookie"), portalNode);
		}
	}

	/**
	 * Rendering the preview with the REST endpoint by the portal must set the cookie
	 * @throws Exception
	 */
	@Test
	public void testRenderContentSetsCookie() throws Exception {
		try (Response response = client.newCall(new Request.Builder()
				.url(url("/rest/page/render/content/" + portalPage.getId()).build()).build()).execute()) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Rendered preview").contains("Portal preview");
			assertPreviewCookie(response.headers("Set-Cookie"), portalNode);
		}
	}

	/**
	 * Rendering the preview of a page in a node without mesh preview URL must not set the cookie
	 * @throws Exception
	 */
	@Test
	public void testNoCookieWithoutPortal() throws Exception {
		try (Response response = client.newCall(new Request.Builder()
				.url(url("/alohapage").addQueryParameter("real", "newview").addQueryParameter("realid", otherPage.getId().toString()).build())
				.build()).execute()) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Rendered preview").contains("CMS rendered");
			assertThat(response.headers("Set-Cookie")).as("Set-Cookie headers")
				.noneMatch(header -> header.startsWith(RenderUtils.PORTAL_PREVIEW_NODE_COOKIE + "="));
		}
		try (Response response = client.newCall(new Request.Builder()
				.url(url("/rest/page/render/content/" + otherPage.getId()).build()).build()).execute()) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.headers("Set-Cookie")).as("Set-Cookie headers")
				.noneMatch(header -> header.startsWith(RenderUtils.PORTAL_PREVIEW_NODE_COOKIE + "="));
		}
	}

	/**
	 * Files existing in the devtool package are served from the package, also when the cookie is set
	 * @throws Exception
	 */
	@Test
	public void testPackageFileServedWithCookie() throws Exception {
		try (Response response = getStatic("/static/" + PACKAGE_NAME + "/files/existing.css", portalNode.getId(), sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Response body").isEqualTo(PACKAGE_FILE_CONTENT);
			assertThat(response.header("X-Portal")).as("X-Portal header").isNull();
			assertThat(response.header("etag")).as("etag header").isNotEmpty();
			assertThat(response.header("cache-control")).as("cache-control header").isEqualTo("no-cache");
		}
		assertThat(portalReceivedCookies).as("Requests to the portal").isEmpty();
	}

	/**
	 * Files existing in the devtool package are served from the package without the cookie
	 * @throws Exception
	 */
	@Test
	public void testPackageFileServedWithoutCookie() throws Exception {
		try (Response response = getStatic("/static/" + PACKAGE_NAME + "/files/existing.css", null, null)) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Response body").isEqualTo(PACKAGE_FILE_CONTENT);
		}
	}

	/**
	 * Files not found in the devtool package are proxied to the portal (including the query string)
	 * @throws Exception
	 */
	@Test
	public void testMissingPackageFileProxied() throws Exception {
		String path = "/static/" + PACKAGE_NAME + "/files/missing.css?v=1&x=a%20b";
		try (Response response = getStatic(path, portalNode.getId(), sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Response body").isEqualTo(PORTAL_STATIC_PREFIX + path);
			assertThat(response.header("X-Portal")).as("X-Portal header").isEqualTo("true");
			assertThat(response.header("Content-Type")).as("Content-Type header").startsWith("text/css");
			assertThat(response.headers("Set-Cookie")).as("Set-Cookie headers from portal").isEmpty();
		}
	}

	/**
	 * Static files, which do not belong to a package, are proxied to the portal. The CMS cookies are not forwarded.
	 * @throws Exception
	 */
	@Test
	public void testOtherStaticFileProxied() throws Exception {
		try (Response response = getStatic("/static/portal.css", portalNode.getId(), sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Response body").isEqualTo(PORTAL_STATIC_PREFIX + "/static/portal.css");
		}
		assertThat(portalReceivedCookies).as("Cookies received by the portal").isEmpty();
	}

	/**
	 * HEAD requests are proxied to the portal
	 * @throws Exception
	 */
	@Test
	public void testHeadProxied() throws Exception {
		try (Response response = new OkHttpClient().newCall(new Request.Builder().url(url("/static/portal.css").build()).head()
				.header("Cookie", RenderUtils.PORTAL_PREVIEW_NODE_COOKIE + "=" + portalNode.getId() + "; "
						+ SessionToken.SESSION_SECRET_COOKIE_NAME + "=" + sessionSecret).build()).execute()) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.header("X-Portal")).as("X-Portal header").isEqualTo("true");
		}
	}

	/**
	 * Without the cookie, static files not found in a package are not found
	 * @throws Exception
	 */
	@Test
	public void testNotFoundWithoutCookie() throws Exception {
		try (Response response = getStatic("/static/portal.css", null, sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(404);
		}
		try (Response response = getStatic("/static/" + PACKAGE_NAME + "/files/missing.css", null, sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(404);
		}
		assertThat(portalReceivedCookies).as("Requests to the portal").isEmpty();
	}

	/**
	 * With the cookie containing a node without mesh preview URL, static files not found in a package are not found
	 * @throws Exception
	 */
	@Test
	public void testNotFoundForNodeWithoutPreviewUrl() throws Exception {
		try (Response response = getStatic("/static/portal.css", otherNode.getId(), sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(404);
		}
	}

	/**
	 * With the cookie containing an unknown node, static files not found in a package are not found
	 * @throws Exception
	 */
	@Test
	public void testNotFoundForUnknownNode() throws Exception {
		try (Response response = getStatic("/static/portal.css", 999999, sessionSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(404);
		}
	}

	/**
	 * With the cookie, but without session, the request is not proxied
	 * @throws Exception
	 */
	@Test
	public void testUnauthorizedWithoutSession() throws Exception {
		try (Response response = getStatic("/static/portal.css", portalNode.getId(), null)) {
			assertThat(response.code()).as("Response code").isEqualTo(401);
		}
		assertThat(portalReceivedCookies).as("Requests to the portal").isEmpty();
	}

	/**
	 * With the cookie, but with an invalid session, the request is not proxied
	 * @throws Exception
	 */
	@Test
	public void testUnauthorizedWithInvalidSession() throws Exception {
		try (Response response = getStatic("/static/portal.css", portalNode.getId(), "4711" + "x".repeat(15))) {
			assertThat(response.code()).as("Response code").isEqualTo(401);
		}
		try (Response response = getStatic("/static/portal.css", portalNode.getId(), "invalid")) {
			assertThat(response.code()).as("Response code").isEqualTo(401);
		}
		assertThat(portalReceivedCookies).as("Requests to the portal").isEmpty();
	}

	/**
	 * With the cookie and the session of a user, who is not allowed to view the node, the request is not proxied
	 * @throws Exception
	 */
	@Test
	public void testForbiddenWithoutPermission() throws Exception {
		String noPermSecret = login("noperm", "noperm");
		try (Response response = getStatic("/static/portal.css", portalNode.getId(), noPermSecret)) {
			assertThat(response.code()).as("Response code").isEqualTo(403);
		}
		assertThat(portalReceivedCookies).as("Requests to the portal").isEmpty();
	}

	/**
	 * Files existing in the devtool package are served without session
	 * @throws Exception
	 */
	@Test
	public void testPackageFileServedWithCookieWithoutSession() throws Exception {
		try (Response response = getStatic("/static/" + PACKAGE_NAME + "/files/existing.css", portalNode.getId(), null)) {
			assertThat(response.code()).as("Response code").isEqualTo(200);
			assertThat(response.body().string()).as("Response body").isEqualTo(PACKAGE_FILE_CONTENT);
		}
	}

	/**
	 * Assert that the set-cookie headers contain the preview node cookie for the given node
	 * @param setCookieHeaders set-cookie headers
	 * @param node expected node
	 */
	private void assertPreviewCookie(List<String> setCookieHeaders, Node node) {
		assertThat(setCookieHeaders).as("Set-Cookie headers").anySatisfy(header -> {
			assertThat(header).startsWith(RenderUtils.PORTAL_PREVIEW_NODE_COOKIE + "=" + node.getId() + ";")
				.containsIgnoringCase("Path=/").containsIgnoringCase("HttpOnly");
		});
	}

	/**
	 * Get a URL builder for the given path on the CMS
	 * @param path path
	 * @return url builder
	 */
	private HttpUrl.Builder url(String path) {
		return new HttpUrl.Builder().scheme("http").host("localhost").port(OSSRunner.getPort()).encodedPath(path);
	}

	/**
	 * Request the given static file (without cookie jar, optionally setting the preview node cookie, the session secret cookie and another cookie)
	 * @param pathAndQuery encoded path and query
	 * @param nodeId optional node ID for the preview node cookie
	 * @param secret optional session secret
	 * @return response
	 * @throws IOException
	 */
	private Response getStatic(String pathAndQuery, Integer nodeId, String secret) throws IOException {
		Request.Builder builder = new Request.Builder()
				.url(String.format("http://localhost:%d%s", OSSRunner.getPort(), pathAndQuery));
		List<String> cookies = new ArrayList<>();
		if (nodeId != null) {
			cookies.add(RenderUtils.PORTAL_PREVIEW_NODE_COOKIE + "=" + nodeId);
		}
		if (secret != null) {
			cookies.add(SessionToken.SESSION_SECRET_COOKIE_NAME + "=" + secret);
		}
		if (!cookies.isEmpty()) {
			cookies.add("othercookie=secret");
			builder.header("Cookie", String.join("; ", cookies));
		}
		return new OkHttpClient().newCall(builder.build()).execute();
	}
}
