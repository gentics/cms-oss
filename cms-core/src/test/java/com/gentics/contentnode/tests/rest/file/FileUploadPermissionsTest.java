package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for permission checking for file uploads
 */
public class FileUploadPermissionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static SystemUser testUser;

	private static Node nodeWithPerm;

	private static Node nodeWithoutPerm;

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		UserGroup nodeAdminGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
		testUser = Trx.supply(() -> Creator.createUser("login", "password", "Tester", "Tester", "", Arrays.asList(nodeAdminGroup)));

		try (Trx trx = new Trx(createSession(testUser.getLogin()), true)) {
			nodeWithPerm = createNode();
		}

		nodeWithoutPerm = Trx.supply(() -> createNode());
	}

	/**
	 * Test uploading a file with permission
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws NodeException 
	 */
	@Test
	public void testPermission() throws NodeException, ParseException, IOException {
		doUploadTest(nodeWithPerm, uploadResponse -> {
			ContentNodeRESTUtils.assertResponseOK(uploadResponse);
		});
	}

	/**
	 * Test uploading a file without permission
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws NodeException 
	 */
	@Test
	public void testNoPermission() throws NodeException, ParseException, IOException {
		doUploadTest(nodeWithoutPerm, uploadResponse -> {
			I18nString i18nMessage = new CNI18nString("rest.file.upload.missing_perm_folder");
			i18nMessage.setParameter("0", nodeWithoutPerm.getFolder().getId());

			ContentNodeRESTUtils.assertResponse(uploadResponse, ResponseCode.FAILURE,
					"You don't have permission to create files in the folder with id " + nodeWithoutPerm.getFolder().getId() + ".",
					new Message(Type.CRITICAL, i18nMessage.toString()));
		});
	}

	/**
	 * Do the upload test
	 * @param node node
	 * @param asserter asserter for the upload response
	 * @throws NodeException
	 * @throws ParseException
	 * @throws IOException
	 */
	protected void doUploadTest(Node node, Consumer<FileUploadResponse> asserter) throws NodeException, ParseException, IOException {
		MultiPart uploadMultiPart = null;
		try (Trx trx = new Trx(createSession(testUser.getLogin()), true)) {
			uploadMultiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart("blah.txt", node.getFolder().getId(), node.getId(), "", true,
					"testcontent");
			FileResource resource = ContentNodeRESTUtils.getFileResource();
			FileUploadResponse uploadResponse = resource.create(uploadMultiPart);
			asserter.accept(uploadResponse);
		} finally {
			if (uploadMultiPart != null) {
				uploadMultiPart.close();
			}
		}
	}
}
