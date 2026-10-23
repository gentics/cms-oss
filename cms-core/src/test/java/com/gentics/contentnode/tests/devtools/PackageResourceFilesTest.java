package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.contentnode.devtools.PackageSynchronizer;
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
 * Test cases for managing the files and files-internal storages of a devtool package. Parameterized to run the
 * same set of checks against both storages, since they only differ in which subdirectory/REST methods back them.
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceFilesTest {
	private final static String PACKAGE_NAME = "testpackage";

	@FunctionalInterface
	private interface ListFiles {
		PackageFileListResponse call(PackageResource resource, String name, String path) throws Exception;
	}

	@FunctionalInterface
	private interface GetOrDeleteFile {
		Response call(PackageResource resource, String name, String path) throws Exception;
	}

	@FunctionalInterface
	private interface SaveFile {
		Response call(PackageResource resource, String name, String path, MultiPart multiPart) throws Exception;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[] {
						"files", PackageSynchronizer.FILES_DIR,
						(ListFiles) PackageResource::listFiles,
						(GetOrDeleteFile) PackageResource::getFile,
						(SaveFile) PackageResource::saveFile,
						(GetOrDeleteFile) PackageResource::deleteFile },
				new Object[] {
						"files-internal", PackageSynchronizer.FILES_INTERNAL_DIR,
						(ListFiles) PackageResource::listFilesInternal,
						(GetOrDeleteFile) PackageResource::getFileInternal,
						(SaveFile) PackageResource::saveFileInternal,
						(GetOrDeleteFile) PackageResource::deleteFileInternal });
	}

	@Parameter(0)
	public String variantName;

	@Parameter(1)
	public String filesDirName;

	@Parameter(2)
	public ListFiles listFiles;

	@Parameter(3)
	public GetOrDeleteFile getFile;

	@Parameter(4)
	public SaveFile saveFile;

	@Parameter(5)
	public GetOrDeleteFile deleteFile;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private PackageResource resource;

	private File filesRoot;

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			new PackageResourceImpl().add(PACKAGE_NAME);
			trx.success();
		}
		resource = new PackageResourceImpl();
		filesRoot = new File(new File(syncContext.getPackagesRoot(), PACKAGE_NAME), filesDirName);
	}

	@Test
	public void testListFilesEmpty() throws Exception {
		try (Trx trx = new Trx()) {
			PackageFileListResponse response = listFiles.call(resource, PACKAGE_NAME, "");
			assertThat(response.getItems()).isEmpty();
			trx.success();
		}
	}

	@Test
	public void testListFilesFlat() throws Exception {
		writeFile("a.txt", "a");
		writeFile("b.txt", "b");
		Files.createDirectories(filesRoot.toPath().resolve("sub"));

		try (Trx trx = new Trx()) {
			PackageFileListResponse response = listFiles.call(resource, PACKAGE_NAME, "");
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
		Files.createDirectories(filesRoot.toPath().resolve("sub"));
		writeFile("sub/nested.txt", "nested");
		writeFile("top.txt", "top");

		try (Trx trx = new Trx()) {
			PackageFileListResponse response = listFiles.call(resource, PACKAGE_NAME, "sub");
			assertThat(response.getItems()).hasSize(1);
			assertThat(response.getItems().get(0).getName()).isEqualTo("nested.txt");
			assertThat(response.getItems().get(0).getPath()).isEqualTo("sub/nested.txt");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testListFilesNonExistentPath() throws Exception {
		try (Trx trx = new Trx()) {
			listFiles.call(resource, PACKAGE_NAME, "doesnotexist");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testListFilesPathIsFile() throws Exception {
		writeFile("a.txt", "a");
		try (Trx trx = new Trx()) {
			listFiles.call(resource, PACKAGE_NAME, "a.txt");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testListFilesPathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			listFiles.call(resource, PACKAGE_NAME, "../../etc");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testListFilesMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			listFiles.call(resource, "doesnotexist", "");
			trx.success();
		}
	}

	@Test
	public void testGetFileHappyPath() throws Exception {
		writeFile("data.bin", "hello world");

		try (Trx trx = new Trx()) {
			Response response = getFile.call(resource, PACKAGE_NAME, "data.bin");
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			File entity = (File) response.getEntity();
			assertThat(new String(Files.readAllBytes(entity.toPath()), StandardCharsets.UTF_8)).isEqualTo("hello world");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFileMissingFile() throws Exception {
		try (Trx trx = new Trx()) {
			getFile.call(resource, PACKAGE_NAME, "doesnotexist.txt");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesRoot.toPath().resolve("sub"));
		try (Trx trx = new Trx()) {
			getFile.call(resource, PACKAGE_NAME, "sub");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testGetFilePathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			getFile.call(resource, PACKAGE_NAME, "../../etc/passwd");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testGetFileMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			getFile.call(resource, "doesnotexist", "a.txt");
			trx.success();
		}
	}

	@Test
	public void testSaveFileCreate() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("hello");
		try (Trx trx = new Trx()) {
			Response response = saveFile.call(resource, PACKAGE_NAME, "sub/new.txt", multiPart);
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
			trx.success();
		}

		File savedFile = new File(filesRoot, "sub/new.txt");
		assertThat(savedFile).exists();
		assertThat(new String(Files.readAllBytes(savedFile.toPath()), StandardCharsets.UTF_8)).isEqualTo("hello");
	}

	@Test
	public void testSaveFileOverwrite() throws Exception {
		writeFile("existing.txt", "old content");

		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("new content");
		try (Trx trx = new Trx()) {
			Response response = saveFile.call(resource, PACKAGE_NAME, "existing.txt", multiPart);
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
			trx.success();
		}

		File savedFile = new File(filesRoot, "existing.txt");
		assertThat(new String(Files.readAllBytes(savedFile.toPath()), StandardCharsets.UTF_8)).isEqualTo("new content");
	}

	@Test(expected = InvalidRequestException.class)
	public void testSaveFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesRoot.toPath().resolve("sub"));
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			saveFile.call(resource, PACKAGE_NAME, "sub", multiPart);
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testSaveFilePathTraversal() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			saveFile.call(resource, PACKAGE_NAME, "../../etc/passwd", multiPart);
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testSaveFileMissingPackage() throws Exception {
		MultiPart multiPart = ContentNodeTestDataUtils.createPackageFileUploadMultiPart("data");
		try (Trx trx = new Trx()) {
			saveFile.call(resource, "doesnotexist", "a.txt", multiPart);
			trx.success();
		}
	}

	@Test
	public void testDeleteFileHappyPath() throws Exception {
		writeFile("todelete.txt", "bye");

		try (Trx trx = new Trx()) {
			Response response = deleteFile.call(resource, PACKAGE_NAME, "todelete.txt");
			assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
			trx.success();
		}

		assertThat(new File(filesRoot, "todelete.txt")).doesNotExist();
	}

	@Test(expected = EntityNotFoundException.class)
	public void testDeleteFileNonExistent() throws Exception {
		try (Trx trx = new Trx()) {
			deleteFile.call(resource, PACKAGE_NAME, "doesnotexist.txt");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testDeleteFilePathIsDirectory() throws Exception {
		Files.createDirectories(filesRoot.toPath().resolve("sub"));
		try (Trx trx = new Trx()) {
			deleteFile.call(resource, PACKAGE_NAME, "sub");
			trx.success();
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void testDeleteFilePathTraversal() throws Exception {
		try (Trx trx = new Trx()) {
			deleteFile.call(resource, PACKAGE_NAME, "../../etc/passwd");
			trx.success();
		}
	}

	@Test(expected = EntityNotFoundException.class)
	public void testDeleteFileMissingPackage() throws Exception {
		try (Trx trx = new Trx()) {
			deleteFile.call(resource, "doesnotexist", "a.txt");
			trx.success();
		}
	}

	private void writeFile(String relativePath, String content) throws IOException {
		File file = new File(filesRoot, relativePath);
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}
}
