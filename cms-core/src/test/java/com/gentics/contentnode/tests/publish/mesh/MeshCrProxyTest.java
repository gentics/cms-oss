package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertErrorCode;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Test cases for the Mesh CR Proxy
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
public class MeshCrProxyTest {
	/**
	 * Mesh CR Prefix
	 */
	public final static String MESH_PREFIX_TEST = "test";

	/**
	 * Other Mesh CR Prefix
	 */
	public final static String MESH_PREFIX_OTHER = "other";

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Integer testCrId;

	private static Integer otherCrId;

	private static Integer nonMeshCrId;

	private static MeshRestClient testClient;

	private static MeshRestClient otherClient;

	private static MeshRestClient nonMeshClient;

	private static MeshRestClient invalidClient;

	private static String sid;

	private static String sessionSecret;

	private static UserGroup testGroup;
	
	private static SystemUser testUser;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		testCrId = createMeshCR(mesh, MESH_PREFIX_TEST);
		otherCrId = createMeshCR(mesh, MESH_PREFIX_OTHER);
		nonMeshCrId = create(ContentRepository.class, create -> {
			create.setName("Non Mesh CR");
			create.setCrType(Type.cr);
			create.setDbType("hsql");
		}).build().getId();

		testGroup = create(UserGroup.class, create -> {
			create.setName("Testgroup");
			create.setMotherId(ContentNodeTestDataUtils.NODE_GROUP_ID);
		}).build();

		testUser = create(SystemUser.class, create -> {
			create.setLogin("test");
			create.setActive(true);
			create.getUserGroups().add(testGroup);
		}).build();

		testUser = update(testUser, update -> {
			update.setPassword(SystemUserFactory.hashPassword("test", update.getId()));
		}).build();

		OkHttpClient okHttpClient = new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(30))
				.connectTimeout(Duration.ofSeconds(30))
				.writeTimeout(Duration.ofSeconds(30))
				.readTimeout(Duration.ofSeconds(30))
				.addInterceptor(chain -> {
					Request original = chain.request();
					HttpUrl originalHttpUrl = original.url();

					HttpUrl url = originalHttpUrl.newBuilder().addQueryParameter("sid", sid).build();
					return chain.proceed(original.newBuilder().url(url)
							.addHeader("Cookie", "GCN_SESSION_SECRET=" + sessionSecret).build());
				})
				.build();

		testClient = new MeshRestOkHttpClientImpl(
				new MeshRestClientConfig.Builder().setHost("localhost").setPort(restContext.getPort()).setSsl(false)
						.setBasePath(String.format("/rest/contentrepositories/%d/proxy/api/v2", testCrId)).build(),
				okHttpClient);
		otherClient = new MeshRestOkHttpClientImpl(
				new MeshRestClientConfig.Builder().setHost("localhost").setPort(restContext.getPort()).setSsl(false)
						.setBasePath(String.format("/rest/contentrepositories/%d/proxy/api/v2", otherCrId)).build(),
				okHttpClient);
		nonMeshClient = new MeshRestOkHttpClientImpl(
				new MeshRestClientConfig.Builder().setHost("localhost").setPort(restContext.getPort()).setSsl(false)
						.setBasePath(String.format("/rest/contentrepositories/%d/proxy/api/v2", nonMeshCrId)).build(),
				okHttpClient);
		invalidClient = new MeshRestOkHttpClientImpl(
				new MeshRestClientConfig.Builder().setHost("localhost").setPort(restContext.getPort()).setSsl(false)
						.setBasePath(String.format("/rest/contentrepositories/%d/proxy/api/v2", 4711)).build(),
				okHttpClient);
	}

	protected static LoggedInClient asTestUser() throws RestException, NodeException, IOException, URISyntaxException {
		LoggedInClient client = restContext.client("test", "test");
		sid = client.get().getSid();
		List<String> cookies = client.get().getCookieHandler()
				.get(new URI("http://localhost:" + restContext.getPort() + "/"), Collections.emptyMap())
				.getOrDefault("Cookie", Collections.emptyList());
		sessionSecret = cookies.stream().filter(value -> Strings.CS.startsWith(value, SessionToken.SESSION_SECRET_COOKIE_NAME + "="))
				.map(value -> Strings.CS.removeStart(value, SessionToken.SESSION_SECRET_COOKIE_NAME + "=")).findFirst()
				.orElseThrow(() -> new NodeException("Could not find session secret cookie"));
		return client;
	}

	/**
	 * Setup permissions on the CRs for the test user
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> {
			List<UserGroup> groups = Arrays.asList(testGroup);

			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read)));
			PermHandler.setPermissions(PermHandler.TYPE_CONADMIN, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read)));
			PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORIES, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read)));

			PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, testCrId, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read, PermType.update)));
			PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, otherCrId, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read, PermType.update)));
			PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, nonMeshCrId, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read, PermType.update)));
		});
		testClient.logout().blockingGet();
		otherClient.logout().blockingGet();
	}

	/**
	 * Test an anonymous request
	 * @throws RestException
	 * @throws NodeException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testAnonymousAccess() throws RestException, NodeException, IOException, URISyntaxException {
		try (LoggedInClient client = asTestUser()) {
			UserResponse me = testClient.me().blockingGet();
			assertThat(me).as("User Response").hasFieldOrPropertyWithValue("username", "anonymous");
		}
	}

	/**
	 * Test login
	 * @throws RestException
	 * @throws NodeException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testLogin() throws RestException, NodeException, IOException, URISyntaxException {
		try (LoggedInClient client = asTestUser()) {
			testClient.setLogin("admin", "admin").login().blockingGet();
			UserResponse me = testClient.me().blockingGet();
			assertThat(me).as("User Response").hasFieldOrPropertyWithValue("username", "admin");

			UserResponse otherMe = otherClient.me().blockingGet();
			assertThat(otherMe).as("User Response").hasFieldOrPropertyWithValue("username", "anonymous");
		}
	}

	/**
	 * Test accessing without edit permission
	 * @throws NodeException
	 * @throws RestException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testNoPermission() throws NodeException, RestException, IOException, URISyntaxException {
		// revoke update permission on CR
		operate(() -> {
			List<UserGroup> groups = Arrays.asList(testGroup);
			PermHandler.setPermissions(ContentRepository.TYPE_CONTENTREPOSITORY, testCrId, groups,
					Permissions.toString(Permissions.set(Permissions.get(), PermType.read)));
		});

		try (LoggedInClient client = asTestUser()) {
			testClient.me().blockingGet();
			fail("This should fail with an Error");
		} catch (Throwable e) {
			assertErrorCode(e, Response.Status.FORBIDDEN);
		}
	}

	/**
	 * Test accessing for non-mesh CR
	 */
	@Test
	public void testNonMeshCr() {
		try (LoggedInClient client = asTestUser()) {
			nonMeshClient.me().blockingGet();
			fail("This should fail with an Error");
		} catch (Throwable e) {
			assertErrorCode(e, Response.Status.CONFLICT);
		}
	}

	/**
	 * Test accessing for non-CR
	 */
	@Test
	public void testInvalidCr() {
		try (LoggedInClient client = asTestUser()) {
			invalidClient.me().blockingGet();
			fail("This should fail with an Error");
		} catch (Throwable e) {
			assertErrorCode(e, Response.Status.NOT_FOUND);
		}
	}
}
