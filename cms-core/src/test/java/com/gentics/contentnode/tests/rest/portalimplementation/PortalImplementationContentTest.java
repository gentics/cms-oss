package com.gentics.contentnode.tests.rest.portalimplementation;

import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
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
import com.gentics.contentnode.factory.object.PortalImplementationFileFactory;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.PortalImplementationFileListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationFileModel;
import com.gentics.contentnode.rest.model.PortalImplementationFolderModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel.Type;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PortalImplementationFileResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationFolderResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for managing the contents (folders and files) of Portal Implementations via the REST resource /portalimplementations
 */
public class PortalImplementationContentTest {
	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private final static String PATH = "portalimplementations";

	/**
	 * Admin user (member of the node group)
	 */
	private final static String ADMIN_LOGIN = "picontentadmin";

	/**
	 * Restricted user (member of a subgroup of the node group)
	 */
	private final static String USER_LOGIN = "picontentuser";

	private final static String PASSWORD = "test";

	private final static String PHP_MIMETYPE = "application/x-httpd-php";

	private static UserGroup nodeGroup;

	private static UserGroup subGroup;

	private static SystemUser admin;

	private static SystemUser user;

	/**
	 * Portal Implementation used in the tests
	 */
	private PortalImplementationModel impl;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		if (TransactionManager.getCurrentTransactionOrNull() != null) {
			TransactionManager.getCurrentTransaction().commit();
		}

		nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		subGroup = Trx.supply(() -> Creator.createUsergroup("PI Content Subgroup", "", nodeGroup));
		admin = Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, PASSWORD, "PI", "Admin", "", Arrays.asList(nodeGroup)));
		user = Trx.supply(() -> Creator.createUser(USER_LOGIN, PASSWORD, "PI", "User", "", Arrays.asList(subGroup)));
	}

	@Before
	public void setup() throws Exception {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (PortalImplementation existing : t.getObjects(PortalImplementation.class,
					DBUtils.select("SELECT id FROM portal_implementation", DBUtils.IDS))) {
				existing.delete(true);
			}
		});

		// the subgroup may access the admin parts and view portal implementations, but has no permissions on single portal implementations
		Trx.operate(() -> {
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(nodeGroup), PermHandler.FULL_PERM);
			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
		});

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationResponse response = client.get().base().path(PATH).request()
					.post(Entity.entity(new PortalImplementationModel().setKeyword("php").setName("PHP Portal").setType(Type.PHP),
							MediaType.APPLICATION_JSON_TYPE), PortalImplementationResponse.class);
			client.get().assertResponse(response);
			impl = response.getPortalImplementation();
		}
	}

	/**
	 * Test uploading a new file: meta information is stored in the DB, the contents in the dbfiles folder
	 * @throws Exception
	 */
	@Test
	public void testUpload() throws Exception {
		byte[] contents = "<?php echo 'Hello World'; ?>".getBytes(StandardCharsets.UTF_8);
		int before = (int) (System.currentTimeMillis() / 1000);
		PortalImplementationFileModel file = upload(ADMIN_LOGIN, "src/index.php", contents, PHP_MIMETYPE);
		int after = (int) (System.currentTimeMillis() / 1000);

		assertThat(file.getId()).as("File ID").isNotNull().isPositive();
		assertThat(file.getPath()).as("Path").isEqualTo("src/index.php");
		assertThat(file.getName()).as("Name").isEqualTo("index.php");
		assertThat(file.getFolder()).as("Folder").isEqualTo("src");
		assertThat(file.getMimetype()).as("Mimetype").isEqualTo(PHP_MIMETYPE);
		assertThat(file.getFilesize()).as("Filesize").isEqualTo(contents.length);
		assertThat(file.getCreator().getId()).as("Creator ID").isEqualTo(admin.getId());
		assertThat(file.getEditor().getId()).as("Editor ID").isEqualTo(admin.getId());
		assertThat(file.getCdate()).as("Creation date").isBetween(before, after);
		assertThat(file.getEdate()).as("Edit date").isBetween(before, after);

		String record = Trx.supply(() -> DBUtils.select(
				"SELECT path, mimetype, filesize, portal_implementation_id FROM portal_implementation_file WHERE id = ?",
				ps -> ps.setInt(1, file.getId()), rs -> {
					rs.next();
					return String.format("%s|%s|%d|%d", rs.getString("path"), rs.getString("mimetype"), rs.getLong("filesize"),
							rs.getInt("portal_implementation_id"));
				}));
		assertThat(record).as("DB record").isEqualTo(String.format("src/index.php|%s|%d|%d", PHP_MIMETYPE, contents.length, impl.getId()));

		assertThat(binFile(file.getId())).as("File in dbfiles").exists().hasBinaryContent(contents);
	}

	/**
	 * Test downloading the contents of a file
	 * @throws Exception
	 */
	@Test
	public void testDownload() throws Exception {
		byte[] contents = "<?php echo 'Download'; ?>".getBytes(StandardCharsets.UTF_8);
		upload(ADMIN_LOGIN, "src/index.php", contents, PHP_MIMETYPE);

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = target(client).path("content").path("src/index.php").request().get();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.OK.getStatusCode());
			assertThat(response.getMediaType().toString()).as("Content type").isEqualTo(PHP_MIMETYPE);
			assertThat(response.readEntity(byte[].class)).as("Downloaded contents").isEqualTo(contents);
		}
	}

	/**
	 * Test replacing the contents of an existing file: contents, editor and edit date change, creator and creation date stay
	 * @throws Exception
	 */
	@Test
	public void testReplace() throws Exception {
		setInstancePerm(PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE);
		PortalImplementationFileModel original = upload(ADMIN_LOGIN, "index.php", "old".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		Trx.operate(() -> DBUtils.update("UPDATE portal_implementation_file SET cdate = 1000, edate = 1000 WHERE id = ?", original.getId()));
		Trx.operate(t -> t.dirtObjectCache(PortalImplementationFile.class, original.getId()));

		byte[] newContents = "the new contents".getBytes(StandardCharsets.UTF_8);
		PortalImplementationFileModel replaced = upload(USER_LOGIN, "index.php", newContents, "text/plain");

		assertThat(replaced.getId()).as("File ID").isEqualTo(original.getId());
		assertThat(replaced.getMimetype()).as("Mimetype").isEqualTo("text/plain");
		assertThat(replaced.getFilesize()).as("Filesize").isEqualTo(newContents.length);
		assertThat(replaced.getCreator().getId()).as("Creator ID").isEqualTo(admin.getId());
		assertThat(replaced.getCdate()).as("Creation date").isEqualTo(1000);
		assertThat(replaced.getEditor().getId()).as("Editor ID").isEqualTo(user.getId());
		assertThat(replaced.getEdate()).as("Edit date").isGreaterThan(1000);
		assertThat(binFile(original.getId())).as("File in dbfiles").hasBinaryContent(newContents);
		assertThat(countFiles()).as("Number of files").isEqualTo(1);
	}

	/**
	 * Test that the mimetype is detected, if not specified in the request, and that empty files are allowed
	 * @throws Exception
	 */
	@Test
	public void testMimetypeDetectionAndEmptyFile() throws Exception {
		PortalImplementationFileModel css = upload(ADMIN_LOGIN, "assets/style.css", "body { color: red; }".getBytes(StandardCharsets.UTF_8),
				MediaType.APPLICATION_OCTET_STREAM);
		assertThat(css.getMimetype()).as("Detected mimetype").isEqualTo("text/css");

		PortalImplementationFileModel empty = upload(ADMIN_LOGIN, "empty.txt", new byte[0], "text/plain");
		assertThat(empty.getFilesize()).as("Filesize of empty file").isEqualTo(0);
		assertThat(binFile(empty.getId())).as("Empty file in dbfiles").exists().hasBinaryContent(new byte[0]);
	}

	/**
	 * Test that paths are case sensitive
	 * @throws Exception
	 */
	@Test
	public void testCaseSensitivePaths() throws Exception {
		PortalImplementationFileModel upper = upload(ADMIN_LOGIN, "Index.php", "upper".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		PortalImplementationFileModel lower = upload(ADMIN_LOGIN, "index.php", "lower".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		assertThat(lower.getId()).as("ID of lowercase file").isNotEqualTo(upper.getId());
		assertThat(countFiles()).as("Number of files").isEqualTo(2);
	}

	/**
	 * Test listing folders
	 * @throws Exception
	 */
	@Test
	public void testFolderListing() throws Exception {
		createStructure();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationFolderModel root = getFolder(client, null);
			assertThat(root.getPath()).as("Root path").isEmpty();
			assertThat(root.getFolders()).as("Subfolders of root").containsExactly("assets", "src");
			assertThat(names(root)).as("Files in root").containsExactly("index.php");

			PortalImplementationFolderModel src = getFolder(client, "src");
			assertThat(src.getPath()).as("Path").isEqualTo("src");
			assertThat(src.getName()).as("Name").isEqualTo("src");
			assertThat(src.getFolders()).as("Subfolders of src").containsExactly("lib");
			assertThat(names(src)).as("Files in src").containsExactly("a.php", "b.php");

			PortalImplementationFolderModel lib = getFolder(client, "src/lib");
			assertThat(lib.getName()).as("Name").isEqualTo("lib");
			assertThat(lib.getFolders()).as("Subfolders of src/lib").isEmpty();
			assertThat(names(lib)).as("Files in src/lib").containsExactly("c.php");

			Response response = target(client).path("folders").path("doesnotexist").request().get();
			assertThat(response.getStatus()).as("Status for non-existing folder").isEqualTo(Status.NOT_FOUND.getStatusCode());
			response.close();

			// a file is no folder
			response = target(client).path("folders").path("index.php").request().get();
			assertThat(response.getStatus()).as("Status for file as folder").isEqualTo(Status.NOT_FOUND.getStatusCode());
			response.close();
		}
	}

	/**
	 * Test getting the meta information of files and listing all files
	 * @throws Exception
	 */
	@Test
	public void testFileMetaInformation() throws Exception {
		createStructure();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationFileResponse response = target(client).path("files").path("src/lib/c.php").request()
					.get(PortalImplementationFileResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getFile().getPath()).as("Path").isEqualTo("src/lib/c.php");
			assertThat(response.getFile().getFolder()).as("Folder").isEqualTo("src/lib");

			Response notFound = target(client).path("files").path("src/nope.php").request().get();
			assertThat(notFound.getStatus()).as("Status for non-existing file").isEqualTo(Status.NOT_FOUND.getStatusCode());
			notFound.close();

			PortalImplementationFileListResponse list = target(client).path("files").request().get(PortalImplementationFileListResponse.class);
			client.get().assertResponse(list);
			assertThat(list.getItems().stream().map(PortalImplementationFileModel::getPath).collect(Collectors.toList())).as("All files")
					.containsOnly("index.php", "src/a.php", "src/b.php", "src/lib/c.php", "assets/style.css");
		}
	}

	/**
	 * Test moving and renaming a file and changing its mimetype
	 * @throws Exception
	 */
	@Test
	public void testUpdateFile() throws Exception {
		byte[] contents = "a".getBytes(StandardCharsets.UTF_8);
		PortalImplementationFileModel file = upload(ADMIN_LOGIN, "src/a.php", contents, PHP_MIMETYPE);

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationFileResponse response = target(client).path("files").path("src/a.php").request().put(
					Entity.entity(new PortalImplementationFileModel().setPath("/other/renamed.php/").setMimetype("text/x-php"), MediaType.APPLICATION_JSON_TYPE),
					PortalImplementationFileResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getFile().getId()).as("File ID").isEqualTo(file.getId());
			assertThat(response.getFile().getPath()).as("New path").isEqualTo("other/renamed.php");
			assertThat(response.getFile().getMimetype()).as("New mimetype").isEqualTo("text/x-php");

			Response old = target(client).path("files").path("src/a.php").request().get();
			assertThat(old.getStatus()).as("Status for old path").isEqualTo(Status.NOT_FOUND.getStatusCode());
			old.close();

			assertThat(download(client, "other/renamed.php")).as("Contents after move").isEqualTo(contents);
		}
	}

	/**
	 * Test moving a folder
	 * @throws Exception
	 */
	@Test
	public void testMoveFolder() throws Exception {
		createStructure();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationFolderResponse response = target(client).path("folders").path("src").request().put(
					Entity.entity(new PortalImplementationFolderModel().setPath("source/php"), MediaType.APPLICATION_JSON_TYPE),
					PortalImplementationFolderResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getFolder().getPath()).as("Moved folder path").isEqualTo("source/php");
			assertThat(response.getFolder().getFolders()).as("Subfolders of moved folder").containsExactly("lib");

			Response old = target(client).path("folders").path("src").request().get();
			assertThat(old.getStatus()).as("Status for old folder").isEqualTo(Status.NOT_FOUND.getStatusCode());
			old.close();

			assertThat(download(client, "source/php/lib/c.php")).as("Contents after move").isEqualTo("c".getBytes(StandardCharsets.UTF_8));
		}
		assertThat(paths()).as("All paths").containsOnly("index.php", "source/php/a.php", "source/php/b.php", "source/php/lib/c.php", "assets/style.css");
	}

	/**
	 * Test that moving a folder to an existing folder or file, or into itself fails
	 * @throws Exception
	 */
	@Test
	public void testMoveFolderInvalid() throws Exception {
		createStructure();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			for (String target : Arrays.asList("assets", "index.php")) {
				Response response = target(client).path("folders").path("src").request()
						.put(Entity.entity(new PortalImplementationFolderModel().setPath(target), MediaType.APPLICATION_JSON_TYPE));
				assertThat(response.getStatus()).as("Status for moving to " + target).isEqualTo(Status.CONFLICT.getStatusCode());
				response.close();
			}

			Response response = target(client).path("folders").path("src").request()
					.put(Entity.entity(new PortalImplementationFolderModel().setPath("src/lib/sub"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Status for moving into itself").isEqualTo(Status.BAD_REQUEST.getStatusCode());
			response.close();
		}
		assertThat(paths()).as("All paths").containsOnly("index.php", "src/a.php", "src/b.php", "src/lib/c.php", "assets/style.css");
	}

	/**
	 * Test deleting a file: DB record and file in dbfiles are removed
	 * @throws Exception
	 */
	@Test
	public void testDeleteFile() throws Exception {
		PortalImplementationFileModel file = upload(ADMIN_LOGIN, "src/a.php", "a".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		assertThat(binFile(file.getId())).as("File in dbfiles before deletion").exists();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = target(client).path("files").path("src/a.php").request().delete();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.NO_CONTENT.getStatusCode());
		}
		assertThat(countFiles()).as("Number of files").isEqualTo(0);
		assertThat(binFile(file.getId())).as("File in dbfiles after deletion").doesNotExist();
	}

	/**
	 * Test deleting a folder: all contained files are removed
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		createStructure();
		List<Integer> srcIds = Trx.supply(() -> DBUtils.select("SELECT id FROM portal_implementation_file WHERE path LIKE 'src/%'", DBUtils.IDLIST));
		assertThat(srcIds).as("Files in src").hasSize(3);

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = target(client).path("folders").path("src").request().delete();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.NO_CONTENT.getStatusCode());

			response = target(client).path("folders").path("doesnotexist").request().delete();
			assertThat(response.getStatus()).as("Response status for non-existing folder").isEqualTo(Status.NOT_FOUND.getStatusCode());
		}
		assertThat(paths()).as("All paths").containsOnly("index.php", "assets/style.css");
		for (Integer id : srcIds) {
			assertThat(binFile(id)).as("File in dbfiles after deletion").doesNotExist();
		}
	}

	/**
	 * Test that invalid paths are rejected
	 * @throws Exception
	 */
	@Test
	public void testInvalidPaths() throws Exception {
		upload(ADMIN_LOGIN, "valid.php", "v".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			for (String invalid : Arrays.asList("", "a//b", "a/../b", "./a", "a\\b", " a", "a /b", "a\tb", StringUtils.repeat("a", 513))) {
				Response response = target(client).path("files").path("valid.php").request()
						.put(Entity.entity(new PortalImplementationFileModel().setPath(invalid), MediaType.APPLICATION_JSON_TYPE));
				assertThat(response.getStatus()).as("Status for path '" + invalid + "'").isEqualTo(Status.BAD_REQUEST.getStatusCode());
				GenericResponse body = response.readEntity(GenericResponse.class);
				if (invalid.length() > PortalImplementationFile.MAX_PATH_LENGTH) {
					GCNAssertions.assertThat(body).containsMessage(Message.Type.CRITICAL, "portal_implementation_file.toolong", invalid,
							Integer.toString(PortalImplementationFile.MAX_PATH_LENGTH));
				} else {
					GCNAssertions.assertThat(body).containsMessage(Message.Type.CRITICAL, "portal_implementation_file.invalidpath", invalid);
				}
			}

			// upload with too long path
			Response response = target(client).path("content").path(StringUtils.repeat("b", 513)).request()
					.put(Entity.entity("b".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE));
			assertThat(response.getStatus()).as("Status for upload with too long path").isEqualTo(Status.BAD_REQUEST.getStatusCode());
			response.close();

			// maximum length is allowed
			PortalImplementationFileResponse ok = target(client).path("content").path(StringUtils.repeat("c", 512)).request()
					.put(Entity.entity("c".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE), PortalImplementationFileResponse.class);
			client.get().assertResponse(ok);
		}
		assertThat(paths()).as("All paths").containsOnly("valid.php", StringUtils.repeat("c", 512));
	}

	/**
	 * Test that a file cannot be stored, where a folder exists and vice versa
	 * @throws Exception
	 */
	@Test
	public void testConflicts() throws Exception {
		upload(ADMIN_LOGIN, "a", "a".getBytes(StandardCharsets.UTF_8), "text/plain");
		upload(ADMIN_LOGIN, "c/d", "d".getBytes(StandardCharsets.UTF_8), "text/plain");

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			// file below a file
			Response response = target(client).path("content").path("a/b").request().put(Entity.entity("b".getBytes(StandardCharsets.UTF_8), "text/plain"));
			assertThat(response.getStatus()).as("Status for file below file").isEqualTo(Status.CONFLICT.getStatusCode());
			GCNAssertions.assertThat(response.readEntity(GenericResponse.class)).containsMessage(Message.Type.CRITICAL, "portal_implementation_file.conflict",
					"a/b", "a");

			// file with the path of a folder
			response = target(client).path("content").path("c").request().put(Entity.entity("c".getBytes(StandardCharsets.UTF_8), "text/plain"));
			assertThat(response.getStatus()).as("Status for file with folder path").isEqualTo(Status.CONFLICT.getStatusCode());
			response.close();

			// move file to the path of another file
			response = target(client).path("files").path("a").request()
					.put(Entity.entity(new PortalImplementationFileModel().setPath("c/d"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Status for moving to existing file").isEqualTo(Status.CONFLICT.getStatusCode());
			response.close();
		}
		assertThat(paths()).as("All paths").containsOnly("a", "c/d");
	}

	/**
	 * Test that reading the contents requires view permission and changing the contents requires edit permission on the Portal Implementation
	 * @throws Exception
	 */
	@Test
	public void testPermissions() throws Exception {
		upload(ADMIN_LOGIN, "src/a.php", "a".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);

		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			// no permission on the portal implementation
			for (String part : Arrays.asList("folders", "files", "files/src/a.php", "content/src/a.php")) {
				Response response = target(client).path(part).request().get();
				assertThat(response.getStatus()).as("Status for reading " + part + " without permission").isEqualTo(Status.FORBIDDEN.getStatusCode());
				response.close();
			}
		}

		// view permission only
		setInstancePerm(PERM_VIEW);
		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			for (String part : Arrays.asList("folders", "folders/src", "files", "files/src/a.php", "content/src/a.php")) {
				Response response = target(client).path(part).request().get();
				assertThat(response.getStatus()).as("Status for reading " + part + " with view permission").isEqualTo(Status.OK.getStatusCode());
				response.close();
			}

			Response response = target(client).path("content").path("src/b.php").request().put(Entity.entity("b".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE));
			assertThat(response.getStatus()).as("Status for upload").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = target(client).path("files").path("src/a.php").request()
					.put(Entity.entity(new PortalImplementationFileModel().setPath("x.php"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Status for moving file").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = target(client).path("folders").path("src").request()
					.put(Entity.entity(new PortalImplementationFolderModel().setPath("x"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Status for moving folder").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = target(client).path("files").path("src/a.php").request().delete();
			assertThat(response.getStatus()).as("Status for deleting file").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = target(client).path("folders").path("src").request().delete();
			assertThat(response.getStatus()).as("Status for deleting folder").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();
		}
		assertThat(paths()).as("All paths").containsOnly("src/a.php");

		// edit permission
		setInstancePerm(PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE);
		upload(USER_LOGIN, "src/b.php", "b".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		assertThat(paths()).as("All paths").containsOnly("src/a.php", "src/b.php");
	}

	/**
	 * Test that deleting the Portal Implementation deletes its files
	 * @throws Exception
	 */
	@Test
	public void testDeletePortalImplementation() throws Exception {
		createStructure();
		List<Integer> ids = Trx.supply(() -> DBUtils.select("SELECT id FROM portal_implementation_file", DBUtils.IDLIST));
		assertThat(ids).as("Files").hasSize(5);

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = target(client).request().delete();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.NO_CONTENT.getStatusCode());
		}
		assertThat(countFiles()).as("Number of files").isEqualTo(0);
		for (Integer id : ids) {
			assertThat(binFile(id)).as("File in dbfiles after deletion").doesNotExist();
		}
	}

	/**
	 * Test that creating, updating and deleting files is logged
	 * @throws Exception
	 */
	@Test
	public void testActionLog() throws Exception {
		PortalImplementationFileModel file = upload(ADMIN_LOGIN, "src/a.php", "a".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		upload(ADMIN_LOGIN, "src/a.php", "changed".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = target(client).path("files").path("src/a.php").request().delete();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.NO_CONTENT.getStatusCode());
		}

		List<String> log = Trx.supply(() -> DBUtils.select("SELECT cmd_desc_id, o_id2, user_id, info FROM logcmd WHERE o_type = ? AND o_id = ? ORDER BY id",
				ps -> {
					ps.setInt(1, PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE);
					ps.setInt(2, file.getId());
				}, rs -> {
					List<String> entries = new ArrayList<>();
					while (rs.next()) {
						entries.add(String.format("%d|%d|%d|%s", rs.getInt("cmd_desc_id"), rs.getInt("o_id2"), rs.getInt("user_id"), rs.getString("info")));
					}
					return entries;
				}));
		assertThat(log).as("Logged actions").containsExactly(
				String.format("%d|%d|%d|PortalImplementationFile.create: src/a.php", ActionLogger.CREATE, impl.getId(), admin.getId()),
				String.format("%d|%d|%d|PortalImplementationFile.update: src/a.php", ActionLogger.EDIT, impl.getId(), admin.getId()),
				String.format("%d|%d|%d|PortalImplementationFile.delete: src/a.php", ActionLogger.DEL, impl.getId(), admin.getId()));
	}

	/**
	 * Test that files get a global ID and that the udate is set when files are created and updated
	 * @throws Exception
	 */
	@Test
	public void testUdateAndGlobalId() throws Exception {
		PortalImplementationFileModel file = upload(ADMIN_LOGIN, "src/a.php", "a".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);

		String[] uuidAndUdate = Trx.supply(() -> DBUtils.select("SELECT uuid, udate FROM portal_implementation_file WHERE id = ?", ps -> {
			ps.setInt(1, file.getId());
		}, rs -> rs.next() ? new String[] { rs.getString("uuid"), Integer.toString(rs.getInt("udate")) } : null));
		assertThat(uuidAndUdate).as("DB record").isNotNull();
		assertThat(uuidAndUdate[0]).as("uuid after create").isNotEmpty();
		assertThat(Integer.parseInt(uuidAndUdate[1])).as("udate after create").isPositive();

		PortalImplementationFile loaded = Trx.supply(t -> t.getObject(PortalImplementationFile.class, file.getId()));
		assertThat(loaded.getGlobalId()).as("Global ID of loaded file").isNotNull();
		assertThat(loaded.getGlobalId().toString()).as("Global ID of loaded file").isEqualTo(uuidAndUdate[0]);
		assertThat(loaded.getUdate()).as("udate of loaded file").isEqualTo(Integer.parseInt(uuidAndUdate[1]));

		assertThat(file.getGlobalId()).as("Global ID in upload response").isEqualTo(uuidAndUdate[0]);
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationFileResponse response = target(client).path("files").path("src/a.php").request().get(PortalImplementationFileResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getFile().getGlobalId()).as("Global ID in file response").isEqualTo(uuidAndUdate[0]);
		}

		// reset the udate, then replace the contents
		Trx.operate(() -> DBUtils.update("UPDATE portal_implementation_file SET udate = 0 WHERE id = ?", file.getId()));
		upload(ADMIN_LOGIN, "src/a.php", "changed".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);

		int udate = Trx.supply(() -> DBUtils.select("SELECT udate FROM portal_implementation_file WHERE id = ?", ps -> {
			ps.setInt(1, file.getId());
		}, rs -> rs.next() ? rs.getInt("udate") : 0));
		assertThat(udate).as("udate after update").isPositive();
	}

	/**
	 * Create a structure of files:
	 * <pre>
	 * index.php
	 * src/a.php
	 * src/b.php
	 * src/lib/c.php
	 * assets/style.css
	 * </pre>
	 * @throws Exception
	 */
	protected void createStructure() throws Exception {
		upload(ADMIN_LOGIN, "index.php", "index".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		upload(ADMIN_LOGIN, "src/a.php", "a".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		upload(ADMIN_LOGIN, "src/b.php", "b".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		upload(ADMIN_LOGIN, "src/lib/c.php", "c".getBytes(StandardCharsets.UTF_8), PHP_MIMETYPE);
		upload(ADMIN_LOGIN, "assets/style.css", "css".getBytes(StandardCharsets.UTF_8), "text/css");
	}

	/**
	 * Get the target for the tested portal implementation
	 * @param client client
	 * @return target
	 * @throws RestException
	 */
	protected WebTarget target(LoggedInClient client) throws RestException {
		return client.get().base().path(PATH).path(Integer.toString(impl.getId()));
	}

	/**
	 * Upload the file contents as the given user and assert success
	 * @param login login
	 * @param path path
	 * @param contents contents
	 * @param contentType content type
	 * @return file
	 * @throws Exception
	 */
	protected PortalImplementationFileModel upload(String login, String path, byte[] contents, String contentType) throws Exception {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			PortalImplementationFileResponse response = target(client).path("content").path(path).request()
					.put(Entity.entity(contents, contentType), PortalImplementationFileResponse.class);
			client.get().assertResponse(response);
			return response.getFile();
		}
	}

	/**
	 * Download the file contents
	 * @param client client
	 * @param path path
	 * @return contents
	 * @throws RestException
	 */
	protected byte[] download(LoggedInClient client, String path) throws RestException {
		return target(client).path("content").path(path).request().get(byte[].class);
	}

	/**
	 * Get the folder
	 * @param client client
	 * @param path path of the folder (null for root)
	 * @return folder
	 * @throws RestException
	 */
	protected PortalImplementationFolderModel getFolder(LoggedInClient client, String path) throws RestException {
		WebTarget target = target(client).path("folders");
		if (path != null) {
			target = target.path(path);
		}
		PortalImplementationFolderResponse response = target.request().get(PortalImplementationFolderResponse.class);
		client.get().assertResponse(response);
		return response.getFolder();
	}

	/**
	 * Get the names of the files in the folder
	 * @param folder folder
	 * @return list of names
	 */
	protected static List<String> names(PortalImplementationFolderModel folder) {
		return folder.getFiles().stream().map(PortalImplementationFileModel::getName).collect(Collectors.toList());
	}

	/**
	 * Get the paths of all files in the DB
	 * @return list of paths
	 * @throws NodeException
	 */
	protected static List<String> paths() throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT path FROM portal_implementation_file", rs -> {
			List<String> paths = new ArrayList<>();
			while (rs.next()) {
				paths.add(rs.getString("path"));
			}
			return paths;
		}));
	}

	/**
	 * Count the files in the DB
	 * @return count
	 * @throws NodeException
	 */
	protected static int countFiles() throws NodeException {
		return paths().size();
	}

	/**
	 * Get the file in the dbfiles folder
	 * @param fileId file ID
	 * @return file
	 */
	protected static File binFile(int fileId) {
		return new File(new File(ConfigurationValue.DBFILES_PATH.get(), PortalImplementationFileFactory.DBFILES_SUBFOLDER), fileId + ".bin");
	}

	/**
	 * Set the permissions of the subgroup on the tested portal implementation
	 * @param bits permission bits
	 * @throws NodeException
	 */
	protected void setInstancePerm(int... bits) throws NodeException {
		Trx.operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, impl.getId(), Arrays.asList(subGroup),
				Permissions.get(bits).toString()));
	}
}
