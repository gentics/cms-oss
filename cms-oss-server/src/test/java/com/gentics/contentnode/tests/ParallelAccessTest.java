package com.gentics.contentnode.tests;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.SYSTEM_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
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
 * Test cases for execution of parallel requests with the same session
 */
public class ParallelAccessTest {
	private static String TEMPLATE_SOURCE = "This is the rendered page";

	private static DBTestContext testContext = new DBTestContext();

	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);

	private static RandomStringGenerator randomString = RandomStringGenerator.builder()
			.withinRange(new char[] { 'a', 'z' }).build();

	private static Node node;
	private static Template template;
	private static Page page;

	private static String pageId;

	private static int THREADS = 10;

	private static int REQUESTS = 10000;

	private static ObjectMapper mapper = new ObjectMapper();

	private static Folder folder;

	private static String folderId;

	private OkHttpClient client;
	
	protected List<Cookie> storedCookies = Collections.synchronizedList(new ArrayList<>());

	protected ExecutorService service;

	private String sid;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = create(Template.class, create -> {
			create.setName("Template");
			create.setFolderId(node.getFolder().getId());
			create.setSource(TEMPLATE_SOURCE);
		}).build();
		page = supply(() -> createPage(node.getFolder(), template, "Page"));
		pageId = Integer.toString(page.getId());

		folder = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Testfolder");
		}).build();

		folderId = Integer.toString(folder.getId());

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, SYSTEM_GROUP_ID));
		supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(nodeGroup)));
	}

	@Before
	public void setup() {
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

		service = Executors.newFixedThreadPool(THREADS);
	}

	@After
	public void tearDown() {
		if (service != null) {
			service.shutdown();
			service = null;
		}
	}

	/**
	 * Execute requests to render a page over the aloha page servlet and get the messages in parallel
	 * @throws Exception
	 */
	@Test
	public void testAlohaPageAndListMessages() throws Exception {
		login();

		List<Future<Void>> responses = new ArrayList<>();
		AtomicInteger succeeded = new AtomicInteger();
		AtomicInteger failed = new AtomicInteger();

		for (int i = 0; i < REQUESTS; i++) {
			boolean list = i % 2 == 0;
			responses.add(service.submit(() -> {
				if (list) {
					listMessages();
				} else {
					String content = renderPage();
					assertThat(content).contains(TEMPLATE_SOURCE);
				}
				return null;
			}));
		}

		// await all responses
		for (Future<Void> future : responses) {
			try {
				future.get(1, TimeUnit.MINUTES);
				succeeded.incrementAndGet();
			} catch (Exception e) {
				failed.incrementAndGet();
			}
		}
		assertThat(succeeded.get()).as("Succeeded requestes").isEqualTo(REQUESTS);
	}

	/**
	 * Execute requests to update a folder in parallel
	 * @throws Exception
	 */
	@Test
	public void testUpdateFolder() throws Exception {
		login();

		List<Future<Void>> responses = new ArrayList<>();
		AtomicInteger succeeded = new AtomicInteger();
		AtomicInteger failed = new AtomicInteger();

		for (int i = 0; i < REQUESTS; i++) {
			responses.add(service.submit(() -> {
				updateFolder();
				return null;
			}));
		}

		// await all responses
		for (Future<Void> future : responses) {
			try {
				future.get(1, TimeUnit.MINUTES);
				succeeded.incrementAndGet();
			} catch (Exception e) {
				failed.incrementAndGet();
			}
		}
		assertThat(succeeded.get()).as("Succeeded requestes").isEqualTo(REQUESTS);
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

		sid = loginResponse.getSid();
	}

	/**
	 * Get the messages list
	 * @return response
	 * @throws IOException
	 * @throws NodeException
	 */
	protected GenericResponse listMessages() throws IOException, NodeException {
		return get(GenericResponse.class, "/rest/msg/list");
	}

	/**
	 * Render the page
	 * @return page content
	 * @throws IOException
	 * @throws NodeException
	 */
	protected String renderPage() throws IOException, NodeException {
		return get(String.class, "/alohapage", entry("real", "newview"), entry("realid", pageId));
	}

	/**
	 * Update the folder
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void updateFolder() throws IOException, NodeException {
		com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();
		folder.setName(randomString.generate(10));
		FolderSaveRequest update = new FolderSaveRequest();
		update.setFolder(folder);

		post(update, GenericResponse.class, String.format("/rest/folder/save/%s", folderId));
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
			try (InputStream in = response.body().byteStream()) {
				return mapper.readValue(in, classOfT);
			}
		}
	}

	/**
	 * Execute a post request
	 * @param <T> response type
	 * @param body request body object
	 * @param classOfT class of the response
	 * @param path request path
	 * @param params optional request parameters
	 * @return response
	 * @throws IOException
	 * @throws NodeException
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	protected final <T> T post(Object body, Class<T> classOfT, String path, Map.Entry<String, String>... params) throws IOException, NodeException {
		path = StringUtils.prependIfMissing(path, "/");

		Builder urlBuilder = new HttpUrl.Builder()
			.scheme("http")
			.host("localhost")
			.port(OSSRunner.getPort())
			.encodedPath(path);

		addQueryParameters(urlBuilder, params);

		Request request = new Request.Builder()
			.url(urlBuilder.build())
			.method("POST", RequestBody.create(MediaType.get("application/json"), mapper.writeValueAsString(body)))
			.build();

		Response response = client.newCall(request).execute();
		assertThat(response.code()).as("Response code").isEqualTo(200);

		if (classOfT.isAssignableFrom(String.class)) {
			return (T) response.body().string();
		} else {
			try (InputStream in = response.body().byteStream()) {
				return mapper.readValue(in, classOfT);
			}
		}
	}

	/**
	 * Add query parameters to the URL builder
	 * @param urlBuilder URL builder
	 * @param params optional request parameters
	 */
	@SafeVarargs
	protected final void addQueryParameters(Builder urlBuilder, Map.Entry<String, String>...params) {
		urlBuilder.addQueryParameter("sid", sid);
		for (Map.Entry<String, String> param : params) {
			urlBuilder.addQueryParameter(param.getKey(), param.getValue());
		}
	}
}
