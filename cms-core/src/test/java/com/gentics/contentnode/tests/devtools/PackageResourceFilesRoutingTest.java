package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.devtools.PackageFile;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.devtools.PackageFileListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedConstructInPackageListResponse;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for the HTTP routing of the endpoints managing the files and files-internal storages of a devtool package.
 * Requests are sent to a running REST application, so that the path templates of the endpoints are resolved by Jersey.
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceFilesRoutingTest {
	private final static String PACKAGE_NAME = "testpackage";

	private final static String PASSWORD = "password";

	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[] { PackageSynchronizer.FILES_DIR }, new Object[] { PackageSynchronizer.FILES_INTERNAL_DIR });
	}

	@Parameter(0)
	public String mode;

	private File packageRoot;

	private File filesRoot;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		TransactionManager.getCurrentTransaction().commit();

		// set systemuser password
		Trx.operate(() -> {
			SystemUser systemUser = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, 1);
			update(systemUser, user -> {
				user.setPassword(SystemUserFactory.hashPassword(PASSWORD, user.getId()));
			});
		});
	}

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().add(PACKAGE_NAME);
			trx.success();
		}
		packageRoot = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		filesRoot = new File(packageRoot, mode);
	}

	@Test
	public void testListRootWithoutTrailingSlash() throws Exception {
		writeFile("a.txt", "a");

		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			Response response = packageTarget(client).path(mode).request().get();
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			assertThat(response.readEntity(PackageFileListResponse.class).getItems()).extracting(PackageFile::getPath).containsExactly("a.txt");
		}
	}

	@Test
	public void testListSubdirectory() throws Exception {
		writeFile("a.txt", "a");
		writeFile("sub/b.txt", "b");

		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			Response response = packageTarget(client).path(mode).queryParam("path", "sub").request().get();
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			assertThat(response.readEntity(PackageFileListResponse.class).getItems()).extracting(PackageFile::getPath).containsExactly("sub/b.txt");
		}
	}

	@Test
	public void testGetFile() throws Exception {
		writeFile("sub/a.txt", "hello world");

		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			Response response = packageTarget(client).path(mode).path("sub/a.txt").request().get();
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			assertThat(response.readEntity(String.class)).isEqualTo("hello world");
		}
	}

	@Test
	public void testSaveFile() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD);
				FormDataMultiPart multiPart = new FormDataMultiPart()) {
			multiPart.field("file", "new content");
			Response response = packageTarget(client).path(mode).path("sub/new.txt").request()
					.post(Entity.entity(multiPart, multiPart.getMediaType()));
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
		}

		assertThat(new String(Files.readAllBytes(new File(filesRoot, "sub/new.txt").toPath()), StandardCharsets.UTF_8)).isEqualTo("new content");
	}

	@Test
	public void testDeleteFile() throws Exception {
		writeFile("sub/todelete.txt", "bye");

		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			Response response = packageTarget(client).path(mode).path("sub/todelete.txt").request().delete();
			assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
		}

		assertThat(new File(filesRoot, "sub/todelete.txt")).doesNotExist();
	}

	@Test
	public void testListUnknownMode() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			assertStorageNotFound(packageTarget(client).path("bogus").request().get(), "bogus");
		}
	}

	@Test
	public void testGetFileUnknownMode() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			assertStorageNotFound(packageTarget(client).path("bogus").path("a.txt").request().get(), "bogus");
		}
	}

	@Test
	public void testDeleteFileUnknownMode() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			assertStorageNotFound(packageTarget(client).path("bogus").path("a.txt").request().delete(), "bogus");
		}
	}

	/**
	 * Test that a POST request to a path of an existing endpoint that does not support POST (constructs/{construct}),
	 * which is routed to {@link PackageResourceImpl#saveFile(String, String, String, org.glassfish.jersey.media.multipart.MultiPart)},
	 * is rejected and does not write anything
	 */
	@Test
	public void testSaveFileExistingEndpointAsMode() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD);
				FormDataMultiPart multiPart = new FormDataMultiPart()) {
			multiPart.field("file", "new content");
			Response response = packageTarget(client).path("constructs").path("a.txt").request()
					.post(Entity.entity(multiPart, multiPart.getMediaType()));
			assertStorageNotFound(response, "constructs");
		}

		assertThat(new File(packageRoot, "constructs/a.txt")).doesNotExist();
	}

	/**
	 * Test that existing endpoints below the package still take precedence over the files storage endpoints
	 */
	@Test
	public void testExistingEndpointTakesPrecedence() throws Exception {
		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			Response response = packageTarget(client).path("constructs").request().get();
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			assertThat(response.readEntity(PagedConstructInPackageListResponse.class).getItems()).isEmpty();
		}
	}

	/**
	 * Get the web target of the test package
	 * @param client logged in client
	 * @return web target
	 * @throws RestException
	 */
	private WebTarget packageTarget(LoggedInClient client) throws RestException {
		return client.get().base().path("devtools").path("packages").path(PACKAGE_NAME);
	}

	/**
	 * Assert that the response is the 404 response for the unknown files storage
	 * @param response response
	 * @param unknownMode unknown files storage, which must be mentioned in the response message
	 */
	private void assertStorageNotFound(Response response, String unknownMode) {
		assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
		assertThat(response.readEntity(GenericResponse.class).getResponseInfo().getResponseMessage()).contains("'" + unknownMode + "'");
	}

	private void writeFile(String relativePath, String content) throws IOException {
		File file = new File(filesRoot, relativePath);
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}
}
