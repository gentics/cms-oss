package com.gentics.contentnode.image;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.request.FileCreateRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.resource.BinaryOutput;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.scheduler.ConvertImagesJob;
import com.gentics.contentnode.tests.rest.file.BinaryDataImageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for WebP image conversion on upload and via the conversion job.
 */
@GCNFeature(set = { Feature.WEBP_CONVERSION })
@RunWith(Parameterized.class)
public class WebpConversionTest {

	private final String CONVERSION_FAILURE_MESSAGE = "Das Bild wurde gespeichert, aber die automatische Konvertierung nach WebP ist fehlgeschlagen.";

	@Parameterized.Parameters(name = "{index}: input file {0}, result file {1}, should convert {2}, skip extension {3}")
	public static Collection<Object[]> data() {
		var data = new ArrayList<Object[]>();

		for (var type: BinaryDataImageResource.ImageType.values()) {
			var origFilename = type.filename();

			if (type.canConvert()) {
				data.add(new Object[] {origFilename, FilenameUtils.removeExtension(origFilename) + ".webp", true, true});
				data.add(new Object[] {origFilename, FilenameUtils.removeExtension(origFilename) + ".webp", true, false});
			} else {
				data.add(new Object[] {origFilename, origFilename, false, true});
				data.add(new Object[] {origFilename, origFilename, false, false});
			}
		}

		return data;
	}

	private static DBTestContext testContext = new DBTestContext();
	private static RESTAppContext restContext = new RESTAppContext(RESTAppContext.Type.jetty);
	@ClassRule
	public static RESTAppContext binarySourceContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(BinaryDataImageResource.class).build()));

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	@Parameter(0)
	public String inputFilename;

	@Parameter(1)
	public String resultFilename;

	@Parameter(2)
	public boolean shouldConvert;

	@Parameter(3)
	public boolean skipExtension;

	private static Node node;
	private static Folder folder;
	private static Folder targetFolder;
	private static SystemUser systemUser;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode("Master", "Master", ContentNodeTestDataUtils.PublishTarget.NONE, ContentNodeTestDataUtils.getLanguage("de"), ContentNodeTestDataUtils.getLanguage("en")));
		folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "testfolder"));
		targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "targetfolder"));

		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences()
			.setProperty("contentnode.maxfilesize", "%d".formatted(5 * 1024 * 1024));

		Trx.consume(n -> n.activateFeature(Feature.WEBP_CONVERSION), node);

		Trx.operate(t -> {
			Folder f = t.getObject(folder, true);

			f.setPublishDir("/testfolder/");
			f.save();

			f = t.getObject(targetFolder, true);
			f.setPublishDir("/targetfolder/");
			f.save();
		});

		systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(
			systemUser,
			t -> {
				for (ImageFile image : node.getFolder().getImages(new Folder.FileSearch().setRecursive(true))) {
					image.delete(true);
				}
			});
	}

	/**
	 * Check that file created from URL is converted.
	 */
	@Test
	public void testCreateFromUrl() throws NodeException, IOException {
		String[] mimeTypeParts = FileUtil.getMimeTypeByExtension(inputFilename).split("/");
		MediaType uploadedMediaType = new MediaType(mimeTypeParts[0], mimeTypeParts[1]);

		FileUploadResponse response = Trx.supply(systemUser, () -> {
			FileCreateRequest request = new FileCreateRequest();

			request.setFolderId(folder.getId());
			request.setName(getRequestFilename());
			request.setNodeId(node.getId());
			request.setOverwriteExisting(false);
			request.setSourceURL(binarySourceContext.getBaseUri() + "binary/%s".formatted(FilenameUtils.getExtension(inputFilename)));

			return getFileResource().create(request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		if (!shouldConvert) {
			assertThat(response.getMessages().get(1).getMessage())
					.as("Response message")
					.isEqualTo(CONVERSION_FAILURE_MESSAGE);
		}

		assertFilenameAndType(response.getFile(), uploadedMediaType, shouldConvert);
	}

	/**
	 * Check that file created from multipart request is converted.
	 */
	@Test
	public void testCreateFromMultipart() throws NodeException, IOException {
		AtomicReference<MediaType> uploadedMediaType = new AtomicReference<>(null);
		byte[] inputData = IOUtils.toByteArray(GenericTestUtils.getPictureResource(inputFilename));
		MultiPart multiPart = Trx.supply(
			systemUser,
			() -> createRestFileUploadMultiPart(folder.getId(), node.getId(), "", true, inputData, uploadedMediaType));

		FileUploadResponse response = Trx.supply(systemUser, () -> getFileResource().create(multiPart));

		ContentNodeTestUtils.assertResponseCodeOk(response);

		if (!shouldConvert) {
			assertThat(response.getMessages().get(1).getMessage())
				.as("Response message")
				.isEqualTo(CONVERSION_FAILURE_MESSAGE);
		}

		assertFilenameAndType(response.getFile(), uploadedMediaType.get(), shouldConvert);
	}

	/**
	 * Check that file created from non-multipart post request is converted.
	 * @throws IOException
	 * @throws NodeException
	 */
	@Test
	public void testCreateSimple() throws IOException, NodeException {
		String[] mimeTypeParts = FileUtil.getMimeTypeByExtension(inputFilename).split("/");
		MediaType uploadedMediaType = new MediaType(mimeTypeParts[0], mimeTypeParts[1]);
		HttpServletRequestWrapper httpServletRequest = mock(HttpServletRequestWrapper.class);

		when(httpServletRequest.getInputStream()).thenReturn(createServletInputStream(GenericTestUtils.getPictureResource(inputFilename)));

		FileUploadResponse response = Trx.supply(
			systemUser,
			() -> getFileResource().createSimple(httpServletRequest, folder.getId(), node.getId(), "binary", inputFilename, "", true));

		ContentNodeTestUtils.assertResponseCodeOk(response);

		if (!shouldConvert) {
			assertThat(response.getMessages().get(1).getMessage())
				.as("Response message")
				.isEqualTo(CONVERSION_FAILURE_MESSAGE);
		}

		assertFilenameAndType(response.getFile(), uploadedMediaType, shouldConvert);
	}

	/**
	 * Check that file created fom multipart request with extra parameters is converted.
	 */
	@Test
	public void testCreateSimpleMultipartFallback() throws IOException, NodeException {
		AtomicReference<MediaType> uploadedMediaType = new AtomicReference<>(null);
		byte[] inputData = IOUtils.toByteArray(GenericTestUtils.getPictureResource(inputFilename));
		MultiPart multiPart = Trx.supply(
			systemUser,
			() -> createRestFileUploadMultiPart(folder.getId(), node.getId(), "", true, inputData, uploadedMediaType));

		HttpServletRequestWrapper httpServletRequest = mock(HttpServletRequestWrapper.class);
		FileUploadResponse response = Trx.supply(
			systemUser,
			() -> getFileResource().createSimpleMultiPartFallback(
				multiPart,
				httpServletRequest,
				folder.getId().toString(),
				node.getId().toString(),
				"binary",
				getRequestFilename(),
				"",
				false));

		ContentNodeTestUtils.assertResponseCodeOk(response);

		if (!shouldConvert) {
			assertThat(response.getMessages().get(1).getMessage())
				.as("Response message")
				.isEqualTo(CONVERSION_FAILURE_MESSAGE);
		}

		assertFilenameAndType(response.getFile(), uploadedMediaType.get(), shouldConvert);
	}

	/**
	 * Check that files are converted by conversion job.
	 */
	@Test
	public void testConversionJob() throws NodeException, IOException {
		AtomicReference<MediaType> uploadedMediaType = new AtomicReference<>(null);
		byte[] inputData = IOUtils.toByteArray(GenericTestUtils.getPictureResource(inputFilename));
		FileUploadResponse response;

		// Create the file with the feature deactivated.
		try (FeatureClosure feature = new FeatureClosure(Feature.WEBP_CONVERSION, false)) {
			MultiPart multiPart = Trx.supply(
				systemUser,
				() -> createRestFileUploadMultiPart(folder.getId(), node.getId(), "", true, inputData, uploadedMediaType));

			response = Trx.supply(systemUser, () -> getFileResource().create(multiPart));
		}

		ContentNodeTestUtils.assertResponseCodeOk(response);
		assertFilenameAndType(response.getFile(), uploadedMediaType.get(), false);

		Integer fileId = response.getFile().getId();
		ImageLoadResponse loadResponse;
		List<String> conversionLog = new ArrayList<>();

		// Start the conversion job without the feature being activated ...
		try (FeatureClosure feature = new FeatureClosure(Feature.WEBP_CONVERSION, false)) {
			Trx.operate(systemUser, () -> new ConvertImagesJob().convert(conversionLog));
			loadResponse = Trx.supply(systemUser, () -> getImageResource().load(fileId.toString(), false, false, node.getId(), null));
		}

		// ... and verify that the file was not converted.
		ContentNodeTestUtils.assertResponseCodeOk(loadResponse);
		assertFilenameAndType(loadResponse.getImage(), uploadedMediaType.get(), false);

		// Start the conversion job with the feature activated ...
		Trx.operate(systemUser, () -> new ConvertImagesJob().convert(conversionLog));
		loadResponse = Trx.supply(systemUser, () -> getImageResource().load(fileId.toString(), false, false, node.getId(), null));

		// ... and check that the file is converted.
		ContentNodeTestUtils.assertResponseCodeOk(loadResponse);
		assertFilenameAndType(loadResponse.getImage(), uploadedMediaType.get(), shouldConvert);
	}

	/**
	 * Create a multipart body for a request.
	 *
	 * @param folderId    The folder ID.
	 * @param nodeId      The node ID.
	 * @param description The file description.
	 * @param overwrite   Whether to override existing files.
	 * @param inputData   The file data.
	 * @return A multipart body for a file create request.
	 */
	private MultiPart createRestFileUploadMultiPart(Integer folderId, Integer nodeId, String description, boolean overwrite, byte[] inputData, AtomicReference<MediaType> uploadedMediaType) throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		BodyPartEntity entity = mock(BodyPartEntity.class);

		when(entity.getInputStream()).thenReturn(new ByteArrayInputStream(inputData));

		String[] mimeTypeParts = FileUtil.getMimeTypeByExtension(inputFilename).split("/");

		// Make sure that the MIME type for the test image was recognized correctly.
		assertThat(mimeTypeParts.length).as("MIME type parts").isEqualTo(2);
		assertThat(mimeTypeParts[0]).as("Main MIME type").isEqualTo("image");

		MediaType mediaType = new MediaType(mimeTypeParts[0], mimeTypeParts[1]);

		if (uploadedMediaType != null) {
			uploadedMediaType.set(mediaType);
		}

		FormDataBodyPart binaryData = null;

		try {
			binaryData = new FormDataBodyPart(
				new FormDataContentDisposition("form-data; name=\"fileBinaryData\"; filename=\"" + getRequestFilename() + "\""),
				entity,
				mediaType);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		binaryData.setMediaType(mediaType);

		MultiPart multiPart = null;

		try {
			multiPart = new MultiPart()
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"folderId\""), folderId.toString()))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"nodeId\""), nodeId.toString()))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"" + SessionToken.SESSION_ID_QUERY_PARAM_NAME + "\""), t.getSessionId()))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"" + SessionToken.SESSION_SECRET_COOKIE_NAME + "\""), t.getSession().getSessionSecret()))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"fileName\""), getRequestFilename()))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"description\""), description))
				.bodyPart(new FormDataBodyPart(new FormDataContentDisposition("form-data; name=\"overwrite\""), (overwrite ? "true" : "false")))
				.bodyPart(binaryData);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		// some dummy value to make the FileResourceImpl happy
		multiPart.getHeaders().add("content-length", "0");

		return multiPart;
	}

	/**
	 * Create a {@link ServletInputStream} from a generic input stream.
	 * @param input The input stream to convert.
	 * @return A {@code ServletInputStream} yielding the data of the given input stream.
	 */
	private ServletInputStream createServletInputStream(InputStream input) throws IOException {
		ByteArrayInputStream data = new ByteArrayInputStream(IOUtils.toByteArray(input));

		ServletInputStream servletInputStream = new ServletInputStream() {
			public int read() {
				return data.read();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
			}
		};

		input.close();

		return servletInputStream;
	}

	/**
	 * Make sure that {@code file} has the expected filename and type.
	 * @param file The file to check.
	 * @param expectConverted Whether the file is expected to be converted to WebP.
	 */
	private void assertFilenameAndType(File file, MediaType uploadedMediaType, boolean expectConverted) throws NodeException, IOException {
		String filetype;
		String filename;
		Response response = Trx.supply(systemUser, () -> getFileResource().loadContent(file.getId().toString(), file.getMasterNodeId()));

		var entity = (BinaryOutput) response.getEntity();
		String mimeTypeFromContent;

		try (var imageInputStream = entity.inputStream()) {
			mimeTypeFromContent = FileUtil.getMimeTypeByContent(imageInputStream, file.getName());
		}

		if (expectConverted) {
			filetype = "image/webp";
			filename = resultFilename;
		} else {
			filetype = uploadedMediaType.toString();
			filename = inputFilename;
		}

		assertThat(file.getFileType())
			.as("Created file type")
			.isEqualTo(filetype);

		assertThat(mimeTypeFromContent)
			.as("MIME type detected from file contents")
			.isEqualTo(file.getFileType());

		assertThat(file.getName())
			.as("Created file name")
			.endsWith(filename);
	}

	/**
	 * Get the filename for the image that should be sent to the FileResource.
	 *
	 * <p>
	 *     When {@link #skipExtension} is {@code true} this is the {@link #inputFilename} without the extension,
	 *     otherwise it is the unmodified {@link #inputFilename}.
	 * </p>
	 *
	 * @return The filename for the image that should be sent to the FileResource.
	 */
	private String getRequestFilename() {
		return skipExtension ? FilenameUtils.removeExtension(inputFilename) : inputFilename;
	}
}
