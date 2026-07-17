package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for permission checking for file uploads
 */
@RunWith(Parameterized.class)
public class FileUploadPermissionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static SystemUser testUser;

	private static Node nodeWithPerm;

	private static Node nodeWithoutPerm;

	private static Auth authentication;

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		UserGroup nodeAdminGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
		testUser = Trx.supply(() -> Creator.createUser("login", "password", "Tester", "Tester", "", Arrays.asList(nodeAdminGroup)));
		authentication = new Auth(testUser);

		try (Trx trx = new Trx(createSession(testUser.getLogin()), true)) {
			nodeWithPerm = createNode();
		}

		nodeWithoutPerm = Trx.supply(() -> createNode());
	}

	@Parameters(name = "{index}: auth {0}")
	public static Collection<Object[]> data() {
		return Stream.of(AuthType.LOGIN, AuthType.TOKEN).map(type -> new Object[] { type }).collect(Collectors.toList());
	}

	@Parameter(0)
	public AuthType type;

	/**
	 * Test uploading a file with permission
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws NodeException 
	 */
	@Test
	public void testPermission() throws NodeException, ParseException, IOException {
		FileUploadResponse response = authentication.withAuth(type, () -> assertSuccess(doUploadTest(nodeWithPerm), ""));
		assertThat(response).as("Response").hasFieldOrPropertyWithValue("file.creator.id", testUser.getId());
	}

	/**
	 * Test uploading a file without permission
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws NodeException 
	 */
	@Test
	public void testNoPermission() throws NodeException, ParseException, IOException {
		String msg = I18NHelper.get("folder.nopermission");
		authentication.withAuth(type, () -> assertError(doUploadTest(nodeWithoutPerm),
				InsufficientPrivilegesException.class, ResponseCode.PERMISSION, msg, new Message(Type.CRITICAL, msg)));
	}

	/**
	 * Do the upload test
	 * @param node node
	 * @param asserter asserter for the upload response
	 * @throws NodeException
	 */
	protected Supplier<FileUploadResponse> doUploadTest(Node node) throws NodeException {
		int folderId = supply(() -> node.getFolder().getId());
		return () -> {
			try (MultiPart uploadMultiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart("blah.txt",
					folderId, node.getId(), "", true, "testcontent")) {
				return getFileResource().create(uploadMultiPart);
			} catch (IOException | ParseException e) {
				throw new NodeException(e);
			}
		};
	}
}
