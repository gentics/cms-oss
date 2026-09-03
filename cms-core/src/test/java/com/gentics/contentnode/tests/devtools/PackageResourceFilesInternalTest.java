package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.InvalidRequestException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.devtools.PackageFile;
import com.gentics.contentnode.rest.model.response.devtools.PackageFileListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for managing the files-internal storage of a devtool package
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceFilesInternalTest {
	private final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private File filesInternalRoot;

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().add(PACKAGE_NAME);
			trx.success();
		}
		filesInternalRoot = new File(new File(syncContext.getPackagesRoot(), PACKAGE_NAME), "files-internal");
	}

	@Test
	public void testListFilesEmpty() throws Exception {
		try (Trx trx = new Trx()) {
			PackageFileListResponse response = new PackageResourceImpl().listFiles(PACKAGE_NAME, "");
			assertThat(response.getItems()).isEmpty();
			trx.success();
		}
	}

	@Test
	public void testListFilesFlat() throws Exception {
		writeFile("a.txt", "a");
		writeFile("b.txt", "b");
		Files.createDirectories(filesInternalRoot.toPath().resolve("sub"));

		try (Trx trx = new Trx()) {
			PackageFileListResponse response = new PackageResourceImpl().listFiles(PACKAGE_NAME, "");
			List<PackageFile> files = response.getItems();

			assertThat(files).hasSize(3);
			// directories are sorted first, then alphabetically by name
			assertThat(files.get(0).getName()).isEqualTo("sub");
			assertThat(files.get(0).isDirectory()).isTrue();
			assertThat(files.get(1).getName()).isEqualTo("a.txt");
			assertThat(files.get(1).isDirectory()).isFalse();
			assertThat(files.get(1).getSize()).isEqualTo(1L);
			assertThat(files.get(1).getPath()).isEqualTo("a.txt");
			assertThat(files.get(2).getName()).isEqualTo("b.txt");
			trx.success();
		}
	}

	@Test
	public void testListFilesSubdirectory() throws Exception {
		Files.createDirectories(filesInternalRoot.toPath().resolve("sub"));
		writeFile("sub/nested.txt", "nested");
		writeFile("top.txt", "top");

		try (Trx trx = new Trx()) {
			PackageFileListResponse response = new PackageResourceImpl().listFiles(PACKAGE_NAME, "sub");
			assertThat(response.getItems()).hasSize(1);
			assertThat(response.getItems().get(0).getName()).isEqualTo("nested.txt");
			assertThat(response.getItems().get(0).getPath()).isEqualTo("sub/nested.txt");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testListFilesNonExistentPath() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().listFiles(PACKAGE_NAME, "doesnotexist");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testListFilesPathIsFile() throws Exception {
		writeFile("a.txt", "a");
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().listFiles(PACKAGE_NAME, "a.txt");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testListFilesPathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().listFiles(PACKAGE_NAME, "../../etc");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testListFilesMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().listFiles("doesnotexist", "");
			trx.success();
		}
	}

	@Test
	public void testGetFileHappyPath() throws Exception {
		writeFile("data.bin", "hello world");

		try (Trx trx = new Trx()) {
			Response response = new PackageResourceImpl().getFile(PACKAGE_NAME, "data.bin");
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			File entity = (File) response.getEntity();
			assertThat(new String(Files.readAllBytes(entity.toPath()), StandardCharsets.UTF_8)).isEqualTo("hello world");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFileMissingFile() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().getFile(PACKAGE_NAME, "doesnotexist.txt");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesInternalRoot.toPath().resolve("sub"));
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().getFile(PACKAGE_NAME, "sub");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testGetFilePathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().getFile(PACKAGE_NAME, "../../etc/passwd");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFileMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().getFile("doesnotexist", "a.txt");
			trx.success();
		}
	}

	@Test
	public void testSaveFileCreate() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("hello");
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			Response response = resource.saveFile(PACKAGE_NAME, "sub/new.txt", multiPart);
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
			trx.success();
		}

		File savedFile = new File(filesInternalRoot, "sub/new.txt");
		assertThat(savedFile).exists();
		assertThat(new String(Files.readAllBytes(savedFile.toPath()), StandardCharsets.UTF_8)).isEqualTo("hello");
	}

	@Test
	public void testSaveFileOverwrite() throws Exception {
		writeFile("existing.txt", "old content");

		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("new content");
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			Response response = resource.saveFile(PACKAGE_NAME, "existing.txt", multiPart);
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
			trx.success();
		}

		File savedFile = new File(filesInternalRoot, "existing.txt");
		assertThat(new String(Files.readAllBytes(savedFile.toPath()), StandardCharsets.UTF_8)).isEqualTo("new content");
	}

	@Test(expected = InvalidRequestException.class)
	public void testSaveFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesInternalRoot.toPath().resolve("sub"));
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().saveFile(PACKAGE_NAME, "sub", multiPart);
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testSaveFilePathTraversal() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().saveFile(PACKAGE_NAME, "../../etc/passwd", multiPart);
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testSaveFileMissingPackage() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().saveFile("doesnotexist", "a.txt", multiPart);
			trx.success();
		}
	}

	@Test
	public void testDeleteFileHappyPath() throws Exception {
		writeFile("todelete.txt", "bye");

		try (Trx trx = new Trx()) {
			Response response = new PackageResourceImpl().deleteFile(PACKAGE_NAME, "todelete.txt");
			assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
			trx.success();
		}

		assertThat(new File(filesInternalRoot, "todelete.txt")).doesNotExist();
	}

	@Test(expected = EntityNotFoundException.class)
	public void testDeleteFileNonExistent() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().deleteFile(PACKAGE_NAME, "doesnotexist.txt");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testDeleteFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesInternalRoot.toPath().resolve("sub"));
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().deleteFile(PACKAGE_NAME, "sub");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testDeleteFilePathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().deleteFile(PACKAGE_NAME, "../../etc/passwd");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testDeleteFileMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().deleteFile("doesnotexist", "a.txt");
			trx.success();
		}
	}

	private void writeFile(String relativePath, String content) throws IOException {
		File file = new File(filesInternalRoot, relativePath);
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}
}
