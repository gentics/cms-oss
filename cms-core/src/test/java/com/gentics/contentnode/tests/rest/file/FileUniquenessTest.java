package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createRestFileUploadMultiPart;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


import com.gentics.contentnode.rest.model.request.FileCopyRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.Creator;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Test cases for upload multiple files in parallel
 */
public class FileUniquenessTest {
	public final static int NUM_THREADS = 10;

	public final static String SHORT_FILENAME = "blah.txt";

	public final static String OTHER_SHORT_FILENAME = "blubb.txt";

	public final static String LONG_FILENAME_PATTERN = "abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz_%d.txt";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * REST Application used as binary data provider
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataResource.class).build()));

	private static Node node;

	private static Folder folder;

	private static SystemUser user;

	/**
	 * Callable that uploads a file (multipart) and returns the FileUploadResponse
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileUploadResponse> upload(String filename) {
		return () -> {
			MultiPart uploadMultiPart = null;
			try (Trx trx = new Trx(user)) {
				uploadMultiPart = createRestFileUploadMultiPart(filename, folder.getId(), node.getId(), "", false,
						"testcontent");
				FileResource resource = getFileResource();
				FileUploadResponse response = resource.create(uploadMultiPart);
				trx.success();
				return response;
			} catch (ParseException e) {
				throw new NodeException(e);
			} finally {
				if (uploadMultiPart != null) {
					try {
						uploadMultiPart.close();
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			}
		};
	}

	/**
	 * Create Callable instance that uploads a new file via the createSimple endpoint.
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileUploadResponse> uploadSimple(String filename) {
		return () -> {
			try (Trx trx = new Trx(user)) {
				HttpServletRequestWrapper httpServletRequest = mock(HttpServletRequestWrapper.class);

				try (
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("testcontent".getBytes(StandardCharsets.UTF_8))) {
					ServletInputStream servletInputStream = new ServletInputStream() {
						private boolean isFinished;

						@Override
						public boolean isFinished() {
							return isFinished;
						}

						@Override
						public boolean isReady() {
							return byteArrayInputStream.available() > 0;
						}

						@Override
						public void setReadListener(ReadListener readListener) {
						}

						public int read() throws IOException {
							int next = byteArrayInputStream.read();

							isFinished = next < 0;

							return next;
						}
					};

					when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
				}

				FileUploadResponse response = getFileResource().createSimple(
					httpServletRequest,
					folder.getId(),
					node.getId(),
					"binary",
					filename,
					"",
					false);

				assertResponseCodeOk(response);

				trx.success();

				return response;
			}
		};
	}

	/**
	 * Callable that uploads a file (multipart) and returns the FileUploadResponse
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileUploadResponse> uploadSimpleMultipart(String filename) {
		return () -> {
			MultiPart uploadMultiPart = null;

			try (Trx trx = new Trx(user)) {
				uploadMultiPart = createRestFileUploadMultiPart(filename, folder.getId(), node.getId(), "", false, "testcontent");

				HttpServletRequestWrapper httpServletRequest = mock(HttpServletRequestWrapper.class);
				FileUploadResponse response = getFileResource().createSimpleMultiPartFallback(
					uploadMultiPart,
					httpServletRequest,
					folder.getId().toString(),
					node.getId().toString(),
					"binary",
					filename,
					"",
					false);
				trx.success();

				return response;
			} catch (ParseException e) {
				throw new NodeException(e);
			} finally {
				if (uploadMultiPart != null) {
					try {
						uploadMultiPart.close();
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			}
		};
	}

	/**
	 * Callable that uploads a file in the specified folder and then moves it to the test folder.
	 * @param filename filename
	 * @param creationFolderId ID of the folder to create the file in.
	 * @return callable
	 */
	private static Callable<GenericResponse> uploadAndMove(String filename, Integer creationFolderId) {
		return () -> {
			MultiPart uploadMultiPart = null;
			FileUploadResponse uploadResponse;

			try (Trx trx = new Trx(user)) {
				uploadMultiPart = createRestFileUploadMultiPart(filename, creationFolderId, node.getId(), "", false, "testcontent");
				uploadResponse = getFileResource().create(uploadMultiPart);

				trx.success();
			} catch (ParseException e) {
				throw new NodeException(e);
			} finally {
				if (uploadMultiPart != null) {
					try {
						uploadMultiPart.close();
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			}

			try (Trx trx = new Trx(user)) {
				MultiObjectMoveRequest moveRequest = new MultiObjectMoveRequest();

				moveRequest.setFolderId(folder.getId());
				moveRequest.setNodeId(node.getId());
				moveRequest.setIds(Arrays.asList(uploadResponse.getFile().getId().toString()));

				GenericResponse moveResponse = getFileResource().move(moveRequest);

				trx.success();

				return moveResponse;
			}
		};
	}

	/**
	 * Callable that uploads a file in the specified folder and then copies it to the test folder.
	 * @param filename filename
	 * @param creationFolderId ID of the folder to create the file in.
	 * @return callable
	 */
	private static Callable<FileUploadResponse> uploadAndCopy(String filename, Integer creationFolderId) {
		return () -> {
			MultiPart uploadMultiPart = null;
			FileUploadResponse uploadResponse;

			try (Trx trx = new Trx(user)) {
				uploadMultiPart = createRestFileUploadMultiPart(filename, creationFolderId, node.getId(), "", false, "testcontent");
				uploadResponse = getFileResource().create(uploadMultiPart);

				trx.success();
			} catch (ParseException e) {
				throw new NodeException(e);
			} finally {
				if (uploadMultiPart != null) {
					try {
						uploadMultiPart.close();
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			}

			try (Trx trx = new Trx(user)) {
				FileCopyRequest copyRequest = new FileCopyRequest();
				TargetFolder targetFolder = new TargetFolder(folder.getId(), folder.getNode().getId());

				copyRequest.setTargetFolder(targetFolder);
				copyRequest.setNodeId(node.getId());
				copyRequest.setFile(uploadResponse.getFile());

				FileUploadResponse copyResponse = getFileResource().copyFile(copyRequest);

				trx.success();

				assertResponseCodeOk(copyResponse);

				return copyResponse;
			}
		};
	}

	/**
	 * Callable the creates a file from URL and returns the FileUploadResponse
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileUploadResponse> createFromUrl(String filename) {
		return () -> {
			try (Trx trx = new Trx(user)) {
				FileCreateRequest request = new FileCreateRequest();
				request.setFolderId(folder.getId());
				request.setName(filename);
				request.setNodeId(node.getId());
				request.setOverwriteExisting(false);
				request.setSourceURL(appContext.getBaseUri() + "binary");

				FileResource resource = getFileResource();
				FileUploadResponse response = resource.create(request);
				trx.success();
				return response;
			} catch (NodeException e) {
				e.printStackTrace();
				return null;
			}
		};
	}

	/**
	 * Create Callable instance that saves the file with new name, and then loads it and returns the FileLoadResponse
	 * @param fileId file ID
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileLoadResponse> update(int fileId, String filename) {
		return () -> {
			try (Trx trx = new Trx(user)) {
				FileSaveRequest request = new FileSaveRequest();
				com.gentics.contentnode.rest.model.File file = new com.gentics.contentnode.rest.model.File();
				file.setId(fileId);
				file.setName(filename);
				request.setFile(file);
				request.setFailOnDuplicate(false);

				FileResource resource = getFileResource();
				GenericResponse response = resource.save(fileId, request);
				assertResponseCodeOk(response);

				trx.success();
			} catch (NodeException e) {
				e.printStackTrace();
				return null;
			}

			try (Trx trx = new Trx(user)) {
				FileResource resource = getFileResource();
				FileLoadResponse response = resource.load(Integer.toString(fileId), false, false, null, null);
				trx.success();
				return response;
			}
		};
	}

	/**
	 * Create Callable instance that uploads the binary data for an existing file with new filename, then loads the file and returns the FileLoadResponse
	 * @param fileId file ID
	 * @param filename filename
	 * @return callable
	 */
	private static Callable<FileLoadResponse> upload(int fileId, String filename) {
		return () -> {
			MultiPart uploadMultiPart = null;
			try (Trx trx = new Trx(user)) {
				uploadMultiPart = createRestFileUploadMultiPart(filename, folder.getId(), node.getId(), "", false,
						"testcontent");
				FileResource resource = getFileResource();
				GenericResponse response = resource.save(fileId, uploadMultiPart);
				assertResponseCodeOk(response);
				trx.success();
			} catch (ParseException e) {
				throw new NodeException(e);
			} finally {
				if (uploadMultiPart != null) {
					try {
						uploadMultiPart.close();
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}
			}

			try (Trx trx = new Trx(user)) {
				FileResource resource = getFileResource();
				FileLoadResponse response = resource.load(Integer.toString(fileId), false, false, null, null);
				trx.success();
				return response;
			}
		};
	}

	/**
	 * Assert that the upload responses contain no duplicate filenames
	 * @param responses
	 */
	private static <T> void assertNoDuplicates(List<Future<T>> responses, Function<T, String> nameExtractor) {
		List<String> names = responses.stream().map(future -> {
			try {
				return nameExtractor.apply(future.get());
			} catch (InterruptedException | ExecutionException | NodeException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
		assertThat(names).as("Filenames").doesNotHaveDuplicates();
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		folder = supply(() -> createFolder(node.getFolder(), "Folder"));
		user = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			for (File file : folder.getFilesAndImages()) {
				file.delete(true);
			}
		});
	}

	/**
	 * Test filename uniqueness when uploading multiple files in parallel
	 * @throws NodeException
	 */
	@Test
	public void testUploadUniqueness() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(upload(SHORT_FILENAME)));
			}

			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness when uploading multiple files having long filenames in parallel,
	 * where the names would be duplicates after truncation
	 * @throws NodeException
	 */
	@Test
	public void testUploadLongname() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(upload(String.format(LONG_FILENAME_PATTERN, i))));
			}

			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness when fetching files from URL in parallel
	 * @throws NodeException
	 */
	@Test
	public void testFromURLUniqueness() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(createFromUrl(SHORT_FILENAME)));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness when fetching files from URL in parallel
	 * @throws NodeException
	 */
	@Test
	public void testFromURLLongname() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(createFromUrl(String.format(LONG_FILENAME_PATTERN, i))));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness by uploading and creating from URL in parallel
	 * @throws NodeException
	 */
	@Test
	public void testCreateMixed() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(i % 2 == 0 ? upload(SHORT_FILENAME) : createFromUrl(SHORT_FILENAME)));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness by renaming the files in parallel
	 * @throws Exception
	 */
	@Test
	public void testRename() throws Exception {
		List<Integer> fileIds = new ArrayList<>();
		for (int i = 0; i < NUM_THREADS; i++) {
			fileIds.add(upload(SHORT_FILENAME).call().getFile().getId());
		}

		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileLoadResponse>> responses = new ArrayList<>();

		try {
			for (int fileId : fileIds) {
				responses.add(service.submit(update(fileId, OTHER_SHORT_FILENAME)));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness by uploading the files again in parallel
	 * @throws Exception
	 */
	@Test
	public void testRenameWithUpload() throws Exception {
		List<Integer> fileIds = new ArrayList<>();
		for (int i = 0; i < NUM_THREADS; i++) {
			fileIds.add(upload(SHORT_FILENAME).call().getFile().getId());
		}

		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileLoadResponse>> responses = new ArrayList<>();

		try {
			for (int fileId : fileIds) {
				responses.add(service.submit(upload(fileId, OTHER_SHORT_FILENAME)));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness by renaming the files in parallel
	 * @throws Exception
	 */
	@Test
	public void testRenameLongname() throws Exception {
		List<Integer> fileIds = new ArrayList<>();
		for (int i = 0; i < NUM_THREADS; i++) {
			fileIds.add(upload(SHORT_FILENAME).call().getFile().getId());
		}

		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileLoadResponse>> responses = new ArrayList<>();

		try {
			for (int fileId : fileIds) {
				responses.add(service.submit(update(fileId, String.format(LONG_FILENAME_PATTERN, fileId))));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test filename uniqueness by uploading the files again in parallel
	 * @throws Exception
	 */
	@Test
	public void testRenameWithUploadLongname() throws Exception {
		List<Integer> fileIds = new ArrayList<>();
		for (int i = 0; i < NUM_THREADS; i++) {
			fileIds.add(upload(SHORT_FILENAME).call().getFile().getId());
		}

		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileLoadResponse>> responses = new ArrayList<>();

		try {
			for (int fileId : fileIds) {
				responses.add(service.submit(upload(fileId, String.format(LONG_FILENAME_PATTERN, fileId))));
			}
			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}
	/**
	 * Upload files using the createSimple endpoint.
	 */
	@Test
	public void testUploadSimple() throws NodeException {
		performUpload(FileUniquenessTest::uploadSimple);
	}

	/**
	 * Upload files using the multipart/form-data createSimple endpoint.
	 */
	@Test
	public void testUploadSimpleMultipart() throws NodeException {
		performUpload(FileUniquenessTest::uploadSimpleMultipart);
	}

	/**
	 * Upload files to different folders, then move in parallel to the same folder.
	 * @throws NodeException
	 */
	@Test
	public void testUploadAndMove() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<GenericResponse>> responses = new ArrayList<>();
		List<Integer> folderIds = new ArrayList<>();

		for (int i = 0; i < NUM_THREADS; i++) {
			String folderName = String.format("Folder_%04d", i);

			folderIds.add(supply(() -> Creator.createFolder(node.getFolder(), folderName, folderName).getId()));
		}

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(uploadAndMove(String.format("test.txt", i), folderIds.get(i))));
			}

			long numOk = responses.stream()
				.map(r -> {
					try {
						return r.get();
					} catch (ExecutionException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				})
				.map(r -> r.getResponseInfo().getResponseCode())
				.filter(code -> code == ResponseCode.OK)
				.count();

			assertThat(numOk)
				.as("Successfully moved files")
				.isEqualTo(1);
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Upload to different folders, then copy the files in parallel to the same folder.
	 * @throws NodeException
	 */
	@Test
	public void testUploadAndCopy() throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();
		List<Integer> folderIds = new ArrayList<>();

		for (int i = 0; i < NUM_THREADS; i++) {
			String folderName = String.format("Folder_%04d", i);

			folderIds.add(supply(() -> Creator.createFolder(node.getFolder(), folderName, folderName).getId()));
		}

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(uploadAndCopy(String.format(SHORT_FILENAME, i), folderIds.get(i))));
			}

			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Call the {@code uploader} function {@code NUM_THREADS} times and verify afterward, that all filenames are distinct.
	 * @param uploader The uploading function to tuse.
	 */
	private void performUpload(Function<String, Callable<FileUploadResponse>> uploader) throws NodeException {
		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		List<Future<FileUploadResponse>> responses = new ArrayList<>();

		try {
			for (int i = 0; i < NUM_THREADS; i++) {
				responses.add(service.submit(uploader.apply(String.format(SHORT_FILENAME, i))));
			}

			assertNoDuplicates(responses, r -> r.getFile().getName());
		} finally {
			service.shutdownNow();
		}
	}
}
