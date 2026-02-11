package com.gentics.contentnode.tests.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.JerseyRestClientImpl;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

/**
 * Test cases for the RestClient
 */
public class ClientTest {
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

	/**
	 * Login of the test user
	 */
	private final static String LOGIN = "test";

	/**
	 * Password of the test user
	 */
	private final static String PASSWORD = "test";

	/**
	 * Test user
	 */
	private static SystemUser testUser;

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test page
	 */
	private static Page page;

	/**
	 * ID of the root folder
	 */
	private static Integer folderId;

	/**
	 * Test file
	 */
	private static File file;

	/**
	 * Create static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setUpOnce() throws NodeException {
		// load group
		UserGroup nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user
		testUser = Trx.supply(() -> {
			return Creator.createUser(LOGIN, PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});

		// create test node
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		folderId = Trx.supply(() -> node.getFolder().getId());

		// create test page
		page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage"));

		// create test file
		file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "textfile.txt", "Testfile Data".getBytes()));

		// set permissions
		Trx.operate(() -> {
			String perms = new Permission(
					PermHandler.PERM_FOLDER_UPDATE,
					PermHandler.PERM_FOLDER_DELETE,
					PermHandler.PERM_FOLDER_CREATE,
					PermHandler.PERM_PAGE_CREATE,
					PermHandler.PERM_PAGE_VIEW,
					PermHandler.PERM_PAGE_UPDATE,
					PermHandler.PERM_PAGE_DELETE,
					PermHandler.PERM_TEMPLATE_VIEW,
					PermHandler.PERM_TEMPLATE_UPDATE,
					PermHandler.PERM_TEMPLATE_DELETE,
					PermHandler.PERM_VIEW)
					.toString();

			PermHandler.setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), perms);
		});
	}

	/**
	 * Truncate systemsession table for each test
	 * 
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.executeStatement("DELETE FROM systemsession", Transaction.UPDATE_STATEMENT));
	}

	/**
	 * Test login and logout
	 * 
	 * @throws NodeException
	 * @throws RestException
	 */
	@Test
	public void testLogin() throws NodeException, RestException {
		RestClient client = new JerseyRestClientImpl(restContext.getBaseUri());
		client.login(LOGIN, PASSWORD);

		// assert successful login
		assertThat(client.getSid()).as("SID").isNotNull().isNotEmpty();

		int numSessions = Trx.supply(() -> DBUtils.select("SELECT count(*) c FROM systemsession WHERE user_id = ? AND secret != ''", st -> {
			st.setInt(1, testUser.getId());
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("c");
			} else {
				return 0;
			}
		}));
		assertThat(numSessions).as("# of session after login").isEqualTo(1);

		UserLoadResponse user = client.base().path("user").path("me").request().get(UserLoadResponse.class);
		client.assertResponse(user);
		assertThat(user.getUser().getId()).as("User ID from REST").isEqualTo(testUser.getId());

		client.logout();

		// SID must be reset now
		assertThat(client.getSid()).as("SID after logout").isNull();
		// user must not have systemsession
		numSessions = Trx.supply(() -> DBUtils.select("SELECT count(*) c FROM systemsession WHERE user_id = ? AND secret != ''", st -> {
			st.setInt(1, testUser.getId());
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("c");
			} else {
				return 0;
			}
		}));
		assertThat(numSessions).as("# of session after logout").isEqualTo(0);
	}

	/**
	 * Test RestClient using a custom ClientRequestFilter
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClientFilter() throws Exception {
		final AtomicInteger filterCounter = new AtomicInteger();
		RestClient client = new JerseyRestClientImpl(() -> {
			ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
			return JerseyClientBuilder.createClient(clientConfig).register(JacksonFeature.class).register(new ClientRequestFilter() {
				@Override
				public void filter(ClientRequestContext requestContext) throws IOException {
					filterCounter.incrementAndGet();
				}
			});
		}, restContext.getBaseUri());

		assertThat(filterCounter.get()).as("Request count").isEqualTo(0);
		client.login(LOGIN, PASSWORD);
		assertThat(filterCounter.get()).as("Request count").isEqualTo(1);
		client.base().path("user").path("me").request().get(UserLoadResponse.class);
		assertThat(filterCounter.get()).as("Request count").isEqualTo(2);
		client.logout();
		assertThat(filterCounter.get()).as("Request count").isEqualTo(3);
	}

	/**
	 * Test file upload
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateFile() throws Exception {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			String fileName = "testfile.txt";
			byte[] contents = "Testfile contents".getBytes();
			String description = "This is my testfile";
			String folderId = Trx.supply(() -> node.getFolder().getId().toString());
			MultiPart multiPart = new MultiPart();
			// Note: the order of the body parts is relevant
			multiPart.bodyPart(new FormDataBodyPart("fileName", fileName));
			multiPart.bodyPart(new FormDataBodyPart("fileBinaryData", contents, MediaType.APPLICATION_OCTET_STREAM_TYPE));
			multiPart.bodyPart(new FormDataBodyPart("description", description));
			multiPart.bodyPart(new FormDataBodyPart("folderId", folderId));
			FileUploadResponse createResponse = client.get().base().path("file").path("create").request()
					.post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE), FileUploadResponse.class);
			client.get().assertResponse(createResponse);
		}
	}

	/**
	 * Test updating a file
	 * @throws Exception
	 */
	@Test
	public void testUpdateFile() throws Exception {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			String fileId = Integer.toString(file.getId());
			String fileName = file.getFilename();
			byte[] contents = "Testfile contents".getBytes();
			String description = "This is my testfile";
			MultiPart multiPart = new MultiPart();
			// Note: the order of the body parts is relevant
			multiPart.bodyPart(new FormDataBodyPart("fileName", fileName));
			multiPart.bodyPart(new FormDataBodyPart("fileBinaryData", contents,
				MediaType.APPLICATION_OCTET_STREAM_TYPE));
			multiPart.bodyPart(new FormDataBodyPart("description", description));
			multiPart.bodyPart(new FormDataBodyPart("folderId", Integer.toString(folderId)));
			FileUploadResponse updateResponse = client.get().base().path("file").path("save").path(fileId).request()
					.post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE), FileUploadResponse.class);
			client.get().assertResponse(updateResponse);
		}
	}

	/**
	 * Test loading a page
	 * @throws Exception
	 */
	@Test
	public void testLoadPage() throws Exception {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			PageLoadResponse loadResponse = client.get().base().path("page").path("load").path(page.getId().toString()).request().get(PageLoadResponse.class);
			client.get().assertResponse(loadResponse);
			assertThat(loadResponse.getPage()).as("Returned page").isNotNull();
			assertThat(loadResponse.getPage().getId()).as("Returned page ID").isEqualTo(page.getId());
		}
	}

	/**
	 * Test creating a folder
	 * @throws Exception
	 */
	@Test
	public void testCreateFolder() throws Exception {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FolderCreateRequest createRequest = new FolderCreateRequest();
			createRequest.setMotherId(Integer.toString(folderId));
			createRequest.setName("Name");

			FolderLoadResponse createResponse = client.get().base().path("folder").path("create").request()
					.post(Entity.json(createRequest), FolderLoadResponse.class);
			client.get().assertResponse(createResponse);
		}
	}

	/**
	 * Test deleting a folder
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			FolderCreateRequest createRequest = new FolderCreateRequest();
			createRequest.setMotherId(Integer.toString(folderId));
			createRequest.setName("Delete Me");

			FolderLoadResponse createResponse = client.get().base().path("folder").path("create").request()
					.post(Entity.json(createRequest), FolderLoadResponse.class);
			client.get().assertResponse(createResponse);

			Integer folderId = createResponse.getFolder().getId();

			GenericResponse deleteResponse = client.get().base().path("folder").path("delete").path(Integer.toString(folderId)).request().post(Entity.json(""),
					GenericResponse.class);
			client.get().assertResponse(deleteResponse);
		}
	}
}
