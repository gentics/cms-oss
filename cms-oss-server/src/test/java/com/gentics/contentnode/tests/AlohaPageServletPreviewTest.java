package com.gentics.contentnode.tests;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.server.OSSRunner;
import com.gentics.contentnode.server.OSSRunnerContext;
import com.gentics.contentnode.testutils.DBTestContext;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.SYSTEM_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Test cases for the {@link com.gentics.contentnode.servlets.AlohaPageServlet}.
 *
 * <p>Aloha Editor scripts must only be included in the rendered page when the page is
 * rendered for editing ({@code real=edit}), not when rendered read-only/preview
 * ({@code real=view} or {@code real=newview}), so that e.g. forms are rendered like on
 * the published page instead of being handled by Aloha.</p>
 */
public class AlohaPageServletPreviewTest {
	/**
	 * Script which is always included (unconditionally, regardless of node configuration)
	 * whenever Aloha Editor is loaded into the rendered page
	 */
	private final static String ALOHA_SCRIPT_MARKER = "gcmsui-scripts-launcher.js";

	private static DBTestContext testContext = new DBTestContext();

	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);

	private static String pageId;

	private static ObjectMapper mapper = new ObjectMapper();

	private OkHttpClient client;

	private List<Cookie> storedCookies = Collections.synchronizedList(new ArrayList<>());

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		Node node = supply(() -> createNode());
		Template template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Template");
			tmpl.setSource("<html><head></head><body>Test Content</body></html>");
		}));
		Page page = supply(() -> createPage(node.getFolder(), template, "Page"));
		pageId = Integer.toString(page.getId());

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(nodeGroup)));

		// grant the group permission to view and edit pages in the node, so that the
		// page can be locked for editing (real=edit) via the AlohaPageServlet
		String perms = new PermHandler.Permission(
			PermHandler.PERM_VIEW,
			PermHandler.PERM_PAGE_VIEW,
			PermHandler.PERM_PAGE_UPDATE).toString();

		operate(() -> {
			PermHandler.setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
		});
	}

	@Before
	public void setup() throws IOException, NodeException {
		client = new OkHttpClient.Builder()
			.callTimeout(Duration.ofSeconds(10))
			.connectTimeout(Duration.ofSeconds(10))
			.writeTimeout(Duration.ofSeconds(10))
			.readTimeout(Duration.ofSeconds(10))
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

		login();
	}

	/**
	 * When rendering the page for editing, the Aloha Editor scripts must be included
	 * @throws Exception
	 */
	@Test
	public void testEditModeIncludesAlohaScripts() throws Exception {
		String content = renderPage("edit");
		assertThat(content).as("Rendered page in edit mode").contains(ALOHA_SCRIPT_MARKER);
	}

	/**
	 * When rendering the page read-only (preview), the Aloha Editor scripts must not be included
	 * @throws Exception
	 */
	@Test
	public void testPreviewModeExcludesAlohaScripts() throws Exception {
		String content = renderPage("newview");
		assertThat(content).as("Rendered page in preview mode").doesNotContain(ALOHA_SCRIPT_MARKER);
	}

	/**
	 * Execute login, and store the sid
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void login() throws IOException, NodeException {
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setLogin("tester");
		loginRequest.setPassword("tester");
		LoginResponse loginResponse = post(loginRequest, LoginResponse.class, "/rest/auth/login");
	}

	/**
	 * Render the page in the given "real" mode
	 * @param real "edit", "view" or "newview"
	 * @return rendered page content
	 * @throws IOException
	 * @throws NodeException
	 */
	protected String renderPage(String real) throws IOException, NodeException {
		return get(String.class, "/alohapage", entry("real", real), entry("realid", pageId));
	}

	/**
	 * Execute a get request
	 * @param <T> response type
	 * @param classOfT class of the response
	 * @param path request path
	 * @param params optional request parameters
	 * @return response
	 * @throws IOException
	 * @throws NodeException
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	protected final <T> T get(Class<T> classOfT, String path, Map.Entry<String, String>... params)
			throws IOException, NodeException {
		path = StringUtils.prependIfMissing(path, "/");

		Builder urlBuilder = new HttpUrl.Builder()
			.scheme("http")
			.host("localhost")
			.port(OSSRunner.getPort())
			.encodedPath(path);

		addQueryParameters(urlBuilder, params);

		Request request = new Request.Builder()
			.url(urlBuilder.build())
			.build();

		Response response = client.newCall(request).execute();
		assertThat(response.code()).as("Response code").isEqualTo(200);

		if (classOfT.isAssignableFrom(String.class)) {
			return (T) response.body().string();
		} else {
			return mapper.readValue(response.body().byteStream(), classOfT);
		}
	}

	/**
	 * Execute a post request
	 * @param <T> response type
	 * @param body request body object
	 * @param classOfT class of the response
	 * @param path request path
	 * @return response
	 * @throws IOException
	 * @throws NodeException
	 */
	protected final <T> T post(Object body, Class<T> classOfT, String path) throws IOException, NodeException {
		path = StringUtils.prependIfMissing(path, "/");

		Builder urlBuilder = new HttpUrl.Builder()
			.scheme("http")
			.host("localhost")
			.port(OSSRunner.getPort())
			.encodedPath(path);

		Request request = new Request.Builder()
			.url(urlBuilder.build())
			.post(RequestBody.create(mapper.writeValueAsBytes(body), MediaType.parse("application/json")))
			.build();

		Response response = client.newCall(request).execute();
		assertThat(response.code()).as("Response code").isEqualTo(200);

		return mapper.readValue(response.body().byteStream(), classOfT);
	}

	/**
	 * Add the sid and the given parameters to the url builder
	 * @param urlBuilder url builder
	 * @param params parameters
	 */
	protected final void addQueryParameters(Builder urlBuilder, Map.Entry<String, String>... params) {
		for (Map.Entry<String, String> param : params) {
			urlBuilder.addQueryParameter(param.getKey(), param.getValue());
		}
	}
}
