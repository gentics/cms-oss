package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.log.ActionLogger.Log;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.fum.FUMResponseStatus;
import com.gentics.contentnode.rest.model.fum.FUMResult;
import com.gentics.contentnode.rest.model.fum.FUMStatus;
import com.gentics.contentnode.rest.model.fum.FUMStatusResponse;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.rest.file.fum.FUMResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for the File Upload Manipulator
 */
@RunWith(Parameterized.class)
public class FileUploadManipulatorTest {
	private static final String TESTCONTENT = "testcontent";

	@ClassRule
	public static RESTAppContext fumContext = new RESTAppContext(new ResourceConfig(FUMResource.class));

	@ClassRule
	public static RESTAppContext binarySourceContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));
	@ClassRule
	public static RESTAppContext binaryAltSourceContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryAltDataResource.class).build()));
	@ClassRule
	public static RESTAppContext bigBinarySourceContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BigFileResource.class).build()));

	@Parameters(name = "{index}: useMultipart {0}, appUrl {2}")
	public static Collection<Object[]> data() {
		return List.of(
				new Object[] {true, null, null, TESTCONTENT.getBytes(StandardCharsets.UTF_8)},
				new Object[] {false, binarySourceContext, "binary", BinaryDataResource.CONTENT.getBytes(StandardCharsets.UTF_8)},
				new Object[] {false, binaryAltSourceContext, "binaryalt", BinaryAltDataResource.CONTENT.getBytes(StandardCharsets.UTF_8)},
				new Object[] {false, bigBinarySourceContext, "bigbinary", BigFileResource.get()}
			);
	}

	@Parameter(0)
	public boolean useMultipart;
	@Parameter(1)
	public RESTAppContext appContext;
	@Parameter(2)
	public String appUrl;
	@Parameter(3)
	public byte[] expected;

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext().config(prefs -> {
		prefs.set("fileupload_manipulator_accept_host", "127.0.0.1");
		prefs.set("contentnode.maxfilesize", Integer.toString(5 * 1024 * 1024));
	});

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static Node node;

	private static SystemUser testUser;

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		UserGroup nodeAdminGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
		testUser = Trx.supply(() -> update(Creator.createUser("login", "password", "Tester", "Tester", "tester@gentics.com", Arrays.asList(nodeAdminGroup)),
				u -> u.setDescription("This is the test User")));

		try (Trx trx = new Trx(createSession(testUser.getLogin()), true)) {
			node = createNode();
		}
	}

	@Before
	public void setup() throws NodeException {
		FUMResource.requestConsumer = null;
		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM logcmd", null));
	}

	/**
	 * Test upload with invalid FUM URL
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testInvalid() throws NodeException, ParseException, IOException {
		doUploadTest("invalid", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.FAILURE,
					null,
					new Message(Type.CRITICAL, new CNI18nString("rest.file.upload.fum_failure").toString()));
		});
	}

	/**
	 * Test loading the file
	 * @throws Exception
	 */
	@Test
	public void testLoadFile() throws NodeException, ParseException, IOException {
		AtomicReference<AssertionError> atomicError = new AtomicReference<>();

		FUMResource.requestConsumer = request -> {
			try {
				assertThat(request.getUrl()).as("File URL").isNotNull();
				HttpClient client = new HttpClient();
				GetMethod fileLoad = new GetMethod(request.getUrl());
				int status = client.executeMethod(fileLoad);
				assertThat(status).as("File load response status").isEqualTo(200);
				assertThat(new String(fileLoad.getResponseBody(), StandardCharsets.UTF_8)).as("Loaded file").isEqualTo(new String(expected, StandardCharsets.UTF_8));
			} catch (AssertionError e) {
				atomicError.set(e);
			} catch (IOException e) {
				throw new NodeException(e);
			}
		};

		doUploadTest("accept", uploadResponse -> {
			ContentNodeRESTUtils.assertResponseOK(uploadResponse);
		});

		if (atomicError.get() != null) {
			throw atomicError.get();
		}
	}

	/**
	 * Test getting the user info
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testUser() throws NodeException, ParseException, IOException {
		AtomicReference<AssertionError> atomicError = new AtomicReference<>();

		FUMResource.requestConsumer = request -> {
			try {
				assertThat(request.getUser()).as("User").isNotNull()
					.has(attribute("id", testUser.getId()))
					.has(attribute("firstName", testUser.getFirstname()))
					.has(attribute("lastName", testUser.getLastname()))
					.has(attribute("email", testUser.getEmail()))
					.has(attribute("description", testUser.getDescription()))
					.has(attribute("login", null))
					.has(attribute("password", null))
					.has(attribute("groups", null));
			} catch (AssertionError e) {
				atomicError.set(e);
			}
		};

		doUploadTest("accept", uploadResponse -> {
			ContentNodeRESTUtils.assertResponseOK(uploadResponse);
		});

		if (atomicError.get() != null) {
			throw atomicError.get();
		}
	}

	/**
	 * Test upload with FUM accepting file
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testAccept() throws NodeException, ParseException, IOException {
		AtomicInteger fileId = new AtomicInteger();

		doUploadTest("accept", uploadResponse -> {
			ContentNodeRESTUtils.assertResponseOK(uploadResponse);
			fileId.set(uploadResponse.getFile().getId());
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "oId", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_ACCEPTED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId())
			);
	}

	/**
	 * Test upload with FUM accepting file and sending a custom message
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testAcceptMsg() throws NodeException, ParseException, IOException {
		doUploadTest("accept/msg", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.OK, String.format("saved file with id: %d", uploadResponse.getFile().getId()),
					new Message(Type.SUCCESS, FUMResource.ACCEPT_MSG));
		});
	}

	/**
	 * Test upload with FUM denying file
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testDeny() throws NodeException, ParseException, IOException {
		doUploadTest("deny", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.FAILURE,
					FUMResource.DENY_MSG,
					new Message(Type.CRITICAL, FUMResource.DENY_MSG));
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_DENIED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setUserId(testUser.getId())
			);
	}

	/**
	 * Test upload with FUM postponing file
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testPostpone() throws NodeException, ParseException, IOException {
		doUploadTest("postpone", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.OK,
					String.format("saved file with id: %d", uploadResponse.getFile().getId()),
					new Message(Type.SUCCESS, new CNI18nString("rest.file.upload.fum_postponed").toString()));
		});
	}

	/**
	 * Test upload with FUM changing filename
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testChangeFilename() throws NodeException, ParseException, IOException {
		doUploadTest("change/filename", uploadResponse -> {
			ContentNodeRESTUtils.assertResponseOK(uploadResponse);
			assertThat(uploadResponse.getFile().getName()).as("filename").isEqualTo(FUMResource.FILENAME);
			ContentNodeRESTUtils.getFileResource().delete(uploadResponse.getFile().getId().toString(), node.getId(), true);
		});
	}

	/**
	 * Test accepting the file after postponing
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testAcceptPostponed() throws NodeException, ParseException, IOException {
		AtomicReference<AssertionError> atomicError = new AtomicReference<>();
		AtomicReference<String> postponeUrl = new AtomicReference<>();

		FUMResource.requestConsumer = request -> {
			try {
				assertThat(request.getPostponeurl()).as("Postpone URL").isNotNull();
				postponeUrl.set(request.getPostponeurl());
			} catch (AssertionError e) {
				atomicError.set(e);
			}
		};

		AtomicInteger fileId = new AtomicInteger();
		doUploadTest("postpone", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.OK,
					String.format("saved file with id: %d", uploadResponse.getFile().getId()),
					new Message(Type.SUCCESS, new CNI18nString("rest.file.upload.fum_postponed").toString()));
			fileId.set(uploadResponse.getFile().getId());
		});

		if (atomicError.get() != null) {
			throw atomicError.get();
		}

		// assert that temp files exist
		String tmpFilename = postponeUrl.get().substring(postponeUrl.get().lastIndexOf('/') + 1);
		File tmpFile = new File(System.getProperty("java.io.tmpdir"), tmpFilename);
		File tmpIdFile = new File(System.getProperty("java.io.tmpdir"), tmpFilename + ".id");
		assertThat(tmpFile).as("tmp file").exists();
		assertThat(tmpIdFile).as("tmp id file").exists();

		Trx.operate(t -> {
			assertThat(t.getObject(com.gentics.contentnode.object.File.class, fileId.get())).as("File before accepting").isNotNull();
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "oId", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_POSTPONED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId())
			);

		// accept
		HttpClient client = new HttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		PostMethod acceptPostponed = new PostMethod(postponeUrl.get());
		acceptPostponed.setRequestHeader("Accept", "application/json");
		FUMResult result = new FUMResult().setStatus(FUMResponseStatus.ACCEPTED);
		acceptPostponed.setRequestEntity(new StringRequestEntity(objectMapper.writeValueAsString(result), "application/json", "UTF-8"));
		int status = client.executeMethod(acceptPostponed);
		assertThat(status).as("Accept postponed response status").isEqualTo(200);

		FUMStatusResponse response = objectMapper.readValue(acceptPostponed.getResponseBodyAsString(), FUMStatusResponse.class);
		assertThat(response.getStatus()).as("Accept postponed status").isEqualTo(FUMStatus.OK);

		assertThat(tmpFile).as("tmp file").doesNotExist();
		assertThat(tmpIdFile).as("tmp id file").doesNotExist();

		Trx.operate(t -> {
			assertThat(t.getObject(com.gentics.contentnode.object.File.class, fileId.get())).as("File after accepting").isNotNull();
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "oId", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_POSTPONED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_ACCEPTED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(1)
			);
	}

	/**
	 * Test denying the file after postponing
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	@Test
	public void testDenyPostponed() throws NodeException, ParseException, IOException {
		AtomicReference<AssertionError> atomicError = new AtomicReference<>();
		AtomicReference<String> postponeUrl = new AtomicReference<>();

		FUMResource.requestConsumer = request -> {
			try {
				assertThat(request.getPostponeurl()).as("Postpone URL").isNotNull();
				postponeUrl.set(request.getPostponeurl());
			} catch (AssertionError e) {
				atomicError.set(e);
			}
		};

		AtomicInteger fileId = new AtomicInteger();
		doUploadTest("postpone", uploadResponse -> {
			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.OK,
					String.format("saved file with id: %d", uploadResponse.getFile().getId()),
					new Message(Type.SUCCESS, new CNI18nString("rest.file.upload.fum_postponed").toString()));
			fileId.set(uploadResponse.getFile().getId());
		});

		if (atomicError.get() != null) {
			throw atomicError.get();
		}

		// assert that temp files exist
		String tmpFilename = postponeUrl.get().substring(postponeUrl.get().lastIndexOf('/') + 1);
		File tmpFile = new File(System.getProperty("java.io.tmpdir"), tmpFilename);
		File tmpIdFile = new File(System.getProperty("java.io.tmpdir"), tmpFilename + ".id");
		assertThat(tmpFile).as("tmp file").exists();
		assertThat(tmpIdFile).as("tmp id file").exists();

		Trx.operate(t -> {
			assertThat(t.getObject(com.gentics.contentnode.object.File.class, fileId.get())).as("File before denying").isNotNull();
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "oId", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_POSTPONED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId())
			);

		// deny
		HttpClient client = new HttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		PostMethod acceptPostponed = new PostMethod(postponeUrl.get());
		acceptPostponed.setRequestHeader("Accept", "application/json");
		FUMResult result = new FUMResult().setStatus(FUMResponseStatus.DENIED);
		acceptPostponed.setRequestEntity(new StringRequestEntity(objectMapper.writeValueAsString(result), "application/json", "UTF-8"));
		int status = client.executeMethod(acceptPostponed);
		assertThat(status).as("Accept postponed response status").isEqualTo(200);

		FUMStatusResponse response = objectMapper.readValue(acceptPostponed.getResponseBodyAsString(), FUMStatusResponse.class);
		assertThat(response.getStatus()).as("Accept postponed status").isEqualTo(FUMStatus.OK);

		assertThat(tmpFile).as("tmp file").doesNotExist();
		assertThat(tmpIdFile).as("tmp id file").doesNotExist();

		Trx.operate(t -> {
			assertThat(t.getObject(com.gentics.contentnode.object.File.class, fileId.get())).as("File after denying").isNull();
		});

		assertThat(getFumEntries()).as("LogCmd entries")
			.usingElementComparatorOnFields("cmdDescId", "oType", "oId", "userId")
			.containsOnly(
					new Log().setCmdDescId(ActionLogger.FUM_START).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_POSTPONED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(testUser.getId()),
					new Log().setCmdDescId(ActionLogger.FUM_DENIED).setOType(com.gentics.contentnode.object.File.TYPE_FILE).setOId(fileId.get()).setUserId(1)
			);
	}

	/**
	 * Do the upload test
	 * @param url FUM url
	 * @param asserter asserter for the upload response
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	protected void doUploadTest(String url, Consumer<FileUploadResponse> asserter) throws NodeException, ParseException, IOException {
		if (useMultipart) {
			doUploadTestMultipart(url, asserter);
		} else {
			doUploadTestRequest(url, asserter);
		}
	}
	protected void doUploadTestMultipart(String url, Consumer<FileUploadResponse> asserter) throws NodeException, ParseException, IOException {
		MultiPart uploadMultiPart = null;
		try (Trx trx = new Trx(createSession(testUser.getLogin()), true);
				PropertyTrx pTrx = new FileUploadManipulatorURL(url);
				PropertyTrx localServer = new PropertyTrx("cn_local_server", "http://localhost:" + restContext.getPort())) {
			String data = TESTCONTENT;
			uploadMultiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart("blah.txt", node.getFolder().getId(), node.getId(), "", true,
					data);
			FileResource resource = ContentNodeRESTUtils.getFileResource();
			FileUploadResponse uploadResponse = resource.create(uploadMultiPart);
			if (uploadResponse.getFile() != null) {
				assertThat(uploadResponse.getFile().getFileSize()).isEqualTo(data.getBytes(StandardCharsets.UTF_8).length);
				assertThat(uploadResponse.getFile().getFileSize().longValue()).isEqualTo(new java.io.File(ConfigurationValue.DBFILES_PATH.get(), uploadResponse.getFile().getId() + ".bin").length());
			}
			asserter.accept(uploadResponse);
		} finally {
			if (uploadMultiPart != null) {
				uploadMultiPart.close();
			}
		}
	}

	protected void doUploadTestRequest(String url, Consumer<FileUploadResponse> asserter) throws NodeException, ParseException, IOException {
		FileCreateRequest fileUploadRequest = new FileCreateRequest();
		try (Trx trx = new Trx(createSession(testUser.getLogin()), true);
				PropertyTrx pTrx = new FileUploadManipulatorURL(url);
				PropertyTrx localServer = new PropertyTrx("cn_local_server", "http://localhost:" + restContext.getPort())) {
			fileUploadRequest.setName("blah.txt");
			fileUploadRequest.setNodeId(node.getId());
			fileUploadRequest.setFolderId(node.getFolder().getId());
			fileUploadRequest.setOverwriteExisting(true);
			fileUploadRequest.setDescription("");
			fileUploadRequest.setSourceURL(appContext.getBaseUri() + appUrl);
			FileResource resource = ContentNodeRESTUtils.getFileResource();
			FileUploadResponse uploadResponse = resource.create(fileUploadRequest);
			if (uploadResponse.getFile() != null) {
				assertThat(uploadResponse.getFile().getFileSize().intValue()).isEqualTo(expected.length);
				assertThat(uploadResponse.getFile().getFileSize().longValue()).isEqualTo(new java.io.File(ConfigurationValue.DBFILES_PATH.get(), uploadResponse.getFile().getId() + ".bin").length());
				assertThat(IOUtils.toByteArray(new FileInputStream(new java.io.File(ConfigurationValue.DBFILES_PATH.get(), uploadResponse.getFile().getId() + ".bin"))))
					.as("Binary data").usingEquals(Arrays::equals).isEqualTo(expected);
			}
			asserter.accept(uploadResponse);
		}
	}

	/**
	 * Get all FUM related logcmd entries
	 * @return list of logcmd entries
	 * @throws NodeException
	 */
	protected List<Log> getFumEntries() throws NodeException {
		return Trx.supply(() -> ActionLogger.getLogCmd(ActionLogger.FUM_START, ActionLogger.FUM_ACCEPTED, ActionLogger.FUM_DENIED, ActionLogger.FUM_POSTPONED,
				ActionLogger.FUM_ERROR));
	}

	/**
	 * Autocloseable that sets the fum URL
	 */
	public static class FileUploadManipulatorURL extends PropertyTrx {
		public FileUploadManipulatorURL(String url) throws NodeException {
			super("fileupload_manipulator_url", String.format("%sfum/%s", fumContext.getBaseUri(), url));
		}
	}
}
