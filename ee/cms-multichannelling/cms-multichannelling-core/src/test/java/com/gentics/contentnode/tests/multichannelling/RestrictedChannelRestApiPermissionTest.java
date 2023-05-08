package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.tests.rest.file.BinaryDataResource;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;

/**
 * Test cases for creating an object in the channel with a user that is restricted to the channel
 * where this object will be created in.
 */
public class RestrictedChannelRestApiPermissionTest extends AbstractMultichannellingSandboxTest {

	SystemUser userWithPerm = null;

	Folder folder = null;

	File file = null;

	/**
	 * REST Application used as binary data provider
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));

	@Override
	public void setUp() throws Exception {
		super.setUp();

		restrictUserToChannel(DBTestContext.USER_WITH_PERMS, channelId);
		TransactionManager.getCurrentTransaction().commit(false);
		testContext.getContext().startTransaction(DBTestContext.USER_WITH_PERMS);
		Transaction t = TransactionManager.getCurrentTransaction();

		userWithPerm = t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS);

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		Node testChannel = t.getObject(Node.class, channelId);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder in the master node
		folder = t.createObject(Folder.class);
		folder.setName("Folder");
		folder.setMotherId(masterNodeRootFolder.getId());
		folder.save();
		t.commit(false);
		file = ContentNodeTestDataUtils.createFile(folder, "blubb.txt", "Original content".getBytes(),testChannel);
	}

	/**
	 * Test creating a file in this scenario
	 * @throws NodeException
	 */
	@Test
	public void testCreateFile() throws Exception {
		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(userWithPerm.getLogin()), true)) {
			FileResourceImpl fileResource = new FileResourceImpl();
			MultiPart multiPart = null;
			FileUploadResponse fileUploadResponse = null;

			try {
				multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
						"blah.txt", folder.getId(), channelId, "", true, "testcontent");
				fileUploadResponse = fileResource.create(multiPart);
				ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
			} finally {
				if (multiPart != null) {
					multiPart.cleanup();
				}
			}
		}
	}

	/**
	 * Test creating a file from URL in this scenario
	 * @throws NodeException
	 */
	@Test
	public void testCreateFileFromURL() throws Exception {
		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(userWithPerm.getLogin()), true)) {
				FileCreateRequest request = new FileCreateRequest();
				request.setFolderId(folder.getId());
				request.setName("blah.txt");
				request.setNodeId(channelId);
				request.setOverwriteExisting(false);
				request.setSourceURL(appContext.getBaseUri() + "binary");

				FileResource resource = getFileResource();
				FileUploadResponse response = resource.create(request);
				ContentNodeTestUtils.assertResponseCodeOk(response);
				trx.success();
		}
	}

	/**
	 * Test overwriting an existing file from URL in this scenario
	 * @throws NodeException
	 */
	@Test
	public void testOverwriteExistingFileFromURL() throws Exception {
		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(userWithPerm.getLogin()), true)) {
			Transaction t = trx.getTransaction();
			File originalFile = t.getObject(File.class, file.getId());
			String originalContent = IOUtils.toString(originalFile.getFileStream(), StandardCharsets.UTF_8);

			FileCreateRequest request = new FileCreateRequest();
			request.setFolderId(folder.getId());
			request.setName("blubb.txt");
			request.setNodeId(channelId);
			request.setOverwriteExisting(true);
			request.setSourceURL(appContext.getBaseUri() + "binary");

			FileResource resource = getFileResource();
			FileUploadResponse response = resource.create(request);
			ContentNodeTestUtils.assertResponseCodeOk(response);
			trx.success();
			t = trx.getTransaction();
			// Check if content changed
			File testFile = t.getObject(File.class, file.getId());
			String newContent = IOUtils.toString(testFile.getFileStream(), StandardCharsets.UTF_8);
			assertNotEquals(originalContent, newContent);
		}
	}
}
