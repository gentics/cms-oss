package com.gentics.contentnode.tests;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.SYSTEM_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
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

	/**
	 * Start of the Aloha Editor settings, which are rendered into the page in edit mode
	 */
	private final static String ALOHA_SETTINGS_MARKER = "Aloha.settings = ";

	private static DBTestContext testContext = new DBTestContext();

	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);

	private static String pageId;

	/**
	 * Page only rendered read-only, so that no other test case locks it
	 */
	private static String readOnlyPageId;

	/**
	 * Page with an editable in the template's h1 ("title"), a block ("image") and an editable without an element
	 * around it ("subtitle"), only rendered read-only
	 */
	private static String taggedPageId;

	private static ObjectMapper mapper = new ObjectMapper();

	private OkHttpClient client;

	private List<Cookie> storedCookies = Collections.synchronizedList(new ArrayList<>());

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		Node node = supply(() -> createNode());
		Template template = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Template");
			tmpl.setSource("<html><head></head><body>Test Content</body></html>");
		}).build();
		Page page = create(Page.class, p -> {
			p.setFolder(node, node.getFolder());
			p.setTemplateId(template.getId());
			p.setName("Page");
		}).unlock().build();
		pageId = Integer.toString(page.getId());
		Page readOnlyPage = create(Page.class, p -> {
			p.setFolder(node, node.getFolder());
			p.setTemplateId(template.getId());
			p.setName("Read only page");
		}).unlock().build();
		readOnlyPageId = Integer.toString(readOnlyPage.getId());

		// an editable (one inline editable part) and a block (one part, which is not inline editable)
		Construct textConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("text");
			c.setName("Text", 1);
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setEditable(2);
				p.setHidden(false);
				p.setKeyname("text");
			}).doNotSave().build());
		}).build();
		Construct htmlConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("html");
			c.setName("HTML", 1);
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("html");
			}).doNotSave().build());
		}).build();
		Template taggedTemplate = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Tagged Template");
			tmpl.setSource("<html><head></head><body><h1><node title></h1><node image><node subtitle></body></html>");
			tmpl.getTemplateTags().put("title", create(TemplateTag.class, tag -> {
				tag.setConstructId(textConstruct.getId());
				tag.setEnabled(true);
				tag.setName("title");
				tag.setPublic(true);
			}).doNotSave().build());
			tmpl.getTemplateTags().put("subtitle", create(TemplateTag.class, tag -> {
				tag.setConstructId(textConstruct.getId());
				tag.setEnabled(true);
				tag.setName("subtitle");
				tag.setPublic(true);
			}).doNotSave().build());
			tmpl.getTemplateTags().put("image", create(TemplateTag.class, tag -> {
				tag.setConstructId(htmlConstruct.getId());
				tag.setEnabled(true);
				tag.setName("image");
				tag.setPublic(true);
			}).doNotSave().build());
		}).build();
		Page taggedPage = create(Page.class, p -> {
			p.setFolder(node, node.getFolder());
			p.setTemplateId(taggedTemplate.getId());
			p.setName("Tagged page");
			p.getContentTag("title").getValues().getByKeyname("text").setValueText("Page title");
			p.getContentTag("image").getValues().getByKeyname("html").setValueText("<img src=\"/image.png\" alt=\"\">");
			p.getContentTag("subtitle").getValues().getByKeyname("text").setValueText("Subtitle");
		}).unlock().build();
		taggedPageId = Integer.toString(taggedPage.getId());

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(nodeGroup)));
		UserGroup viewerGroup = supply(() -> createUserGroup("Viewers", SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("Viewer", "Viewer", null, "viewer", "viewer", Arrays.asList(viewerGroup)));

		// grant the group permission to view and edit pages in the node, so that the
		// page can be locked for editing (real=edit) via the AlohaPageServlet
		String perms = new PermHandler.Permission(
			PermHandler.PERM_VIEW,
			PermHandler.PERM_PAGE_VIEW,
			PermHandler.PERM_PAGE_UPDATE).toString();

		// the viewers may only view the pages
		String viewPerms = new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString();

		operate(() -> {
			PermHandler.setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
			PermHandler.setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(viewerGroup), viewPerms);
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(viewerGroup), viewPerms);
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
	 * When rendering the page read-only with the tag markers requested ({@code tagmarkers=true}), the page is
	 * rendered in the preview mode for Aloha ({@code RenderType.EM_ALOHA_PREVIEW}): like in edit mode, but without
	 * the Aloha Editor scripts and without being locked.
	 * @throws Exception
	 */
	@Test
	public void testPreviewModeWithTagMarkersDoesNotLock() throws Exception {
		String content = get(String.class, "/alohapage", entry("real", "view"), entry("realid", readOnlyPageId), entry("tagmarkers", "true"));
		assertThat(content).as("Rendered page in preview mode with tag markers").doesNotContain(ALOHA_SCRIPT_MARKER);

		boolean locked = supply(t -> t.getObject(Page.class, Integer.parseInt(readOnlyPageId)).getContent().isLocked());
		assertThat(locked).as("Page locked after rendering it in preview mode with tag markers").isFalse();
	}

	/**
	 * With the tag markers requested, the blocks and the editables of the page are annotated like in edit mode
	 * (the editable additionally with the data- attributes), and the page contains no Aloha Editor settings
	 * @throws Exception
	 */
	@Test
	public void testPreviewModeWithTagMarkersAnnotatesTags() throws Exception {
		String content = get(String.class, "/alohapage", entry("real", "view"), entry("realid", taggedPageId), entry("tagmarkers", "true"));

		assertThat(content).as("Rendered page with tag markers")
			.containsPattern("<h1 data-gcn-pageid=\"" + taggedPageId + "\" data-gcn-tagid=\"\\d+\" data-gcn-tagname=\"title\"[^>]* id=\"GENTICS_EDITABLE_\\d+\">Page title</h1>")
			.containsPattern("<div data-gcn-pageid=\"" + taggedPageId + "\" data-gcn-tagid=\"\\d+\" data-gcn-tagname=\"image\"[^>]* class=\"aloha-block\" id=\"GENTICS_BLOCK_\\d+\"><img src=\"/image.png\" alt=\"\"></div>")
			.containsPattern("<div data-gcn-pageid=\"" + taggedPageId + "\" data-gcn-tagid=\"\\d+\" data-gcn-tagname=\"subtitle\"[^>]* id=\"GENTICS_EDITABLE_\\d+\">Subtitle</div>")
			.doesNotContain(ALOHA_SCRIPT_MARKER)
			.doesNotContain(ALOHA_SETTINGS_MARKER);
	}

	/**
	 * Without the tag markers, the page rendered read-only contains no annotations
	 * @throws Exception
	 */
	@Test
	public void testPreviewModeWithoutTagMarkers() throws Exception {
		String content = get(String.class, "/alohapage", entry("real", "view"), entry("realid", taggedPageId));

		assertThat(content).as("Rendered page without tag markers")
			.contains("<h1>Page title</h1><img src=\"/image.png\" alt=\"\">Subtitle")
			.doesNotContain("data-gcn-tagname");
	}

	/**
	 * A user, who may only view the page, gets the page with the tag markers too
	 * @throws Exception
	 */
	@Test
	public void testTagMarkersNeedViewPermissionOnly() throws Exception {
		storedCookies.clear();
		login("viewer", "viewer");

		String content = get(String.class, "/alohapage", entry("real", "view"), entry("realid", taggedPageId), entry("tagmarkers", "true"));

		assertThat(content).as("Rendered page with tag markers for a viewer").contains("data-gcn-tagname=\"title\"", "data-gcn-tagname=\"image\"");
	}

	/**
	 * Execute login, and store the sid
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void login() throws IOException, NodeException {
		login("tester", "tester");
	}

	/**
	 * Execute login as the given user, and store the sid
	 * @param login login
	 * @param password password
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void login(String login, String password) throws IOException, NodeException {
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setLogin(login);
		loginRequest.setPassword(password);
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
