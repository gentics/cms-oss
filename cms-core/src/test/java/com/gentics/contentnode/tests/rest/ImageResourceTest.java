package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.jmage.filter.FilterException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.request.ImageResizeRequest;
import com.gentics.contentnode.rest.model.request.ImageRotate;
import com.gentics.contentnode.rest.model.request.ImageRotateRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.image.ResizeFilter;
import com.gentics.lib.image.SmarterResizeFilter;

import fi.iki.santtu.md5.MD5;
import fi.iki.santtu.md5.MD5OutputStream;

/**
 * Tests for the REST {@link ImageResourceImpl} class.
 */
public class ImageResourceTest {
	public final static String IMAGE_NAME = "image-dpi66x44-res311x211.jpg";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private ImageFile testImage;

	private static String rotateCWMd5;

	private static String rotateCCWMd5;

	private static String rotateAndResizeCWMd5;

	private static String rotateAndResizeCCWMd5;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException, FilterException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());

		// determine MD5 Sums of resized and rotated test image
		rotateCWMd5 = rotateCW();
		rotateCCWMd5 = rotateCCW();
		rotateAndResizeCWMd5 = rotateAndResizeCW();
		rotateAndResizeCCWMd5 = rotateAndResizeCCW();
	}

	/**
	 * Rotate the test image CW
	 * @return MD5 sum of rotated image
	 * @throws IOException
	 */
	protected static String rotateCW() throws IOException {
		PlanarImage image = null;
		try (InputStream originalBinaryStream = ImageResourceTest.class.getResourceAsStream(IMAGE_NAME)) {
			image = PlanarImage.wrapRenderedImage(ImageIO.read(originalBinaryStream));
		}
		image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_90), null);

		MD5OutputStream md5OS = new MD5OutputStream(new NullOutputStream());
		ImageIO.write(image.getAsBufferedImage(), "jpg", md5OS);
		return MD5.asHex(md5OS.hash());
	}

	/**
	 * Rotate the test image CCW
	 * @return MD5 sum of rotated image
	 * @throws IOException
	 */
	protected static String rotateCCW() throws IOException {
		PlanarImage image = null;
		try (InputStream originalBinaryStream = ImageResourceTest.class.getResourceAsStream(IMAGE_NAME)) {
			image = PlanarImage.wrapRenderedImage(ImageIO.read(originalBinaryStream));
		}
		image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_270), null);

		MD5OutputStream md5OS = new MD5OutputStream(new NullOutputStream());
		ImageIO.write(image.getAsBufferedImage(), "jpg", md5OS);
		return MD5.asHex(md5OS.hash());
	}

	/**
	 * Rotate CW and resize the test image to 100x200
	 * @return MD5 sum of rotated and resized image
	 * @throws IOException
	 * @throws FilterException
	 */
	protected static String rotateAndResizeCW() throws IOException, FilterException {
		PlanarImage image = null;
		try (InputStream originalBinaryStream = ImageResourceTest.class.getResourceAsStream(IMAGE_NAME)) {
			image = PlanarImage.wrapRenderedImage(ImageIO.read(originalBinaryStream));
		}
		image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_90), null);

		Properties resizeProperties = new Properties();
		resizeProperties.setProperty("MODE", "unproportional");	
		resizeProperties.setProperty(SmarterResizeFilter.HEIGHT, String.valueOf(100));
		resizeProperties.setProperty(SmarterResizeFilter.WIDTH, String.valueOf(200));

		ResizeFilter resizeFilter = new ResizeFilter();
		resizeFilter.initialize(resizeProperties);
		image = resizeFilter.filter(image);

		MD5OutputStream md5OS = new MD5OutputStream(new NullOutputStream());
		ImageIO.write(image.getAsBufferedImage(), "jpg", md5OS);
		return MD5.asHex(md5OS.hash());
	}

	/**
	 * Rotate CCW and resize test image to 200x400
	 * @return MD5 sum of rotated and resized image
	 * @throws IOException
	 * @throws FilterException
	 */
	protected static String rotateAndResizeCCW() throws IOException, FilterException {
		PlanarImage image = null;
		try (InputStream originalBinaryStream = ImageResourceTest.class.getResourceAsStream(IMAGE_NAME)) {
			image = PlanarImage.wrapRenderedImage(ImageIO.read(originalBinaryStream));
		}
		image = JAI.create("transpose", new ParameterBlock().addSource(image).add(TransposeDescriptor.ROTATE_270), null);

		Properties resizeProperties = new Properties();
		resizeProperties.setProperty("MODE", "unproportional");	
		resizeProperties.setProperty(SmarterResizeFilter.HEIGHT, String.valueOf(200));
		resizeProperties.setProperty(SmarterResizeFilter.WIDTH, String.valueOf(400));

		ResizeFilter resizeFilter = new ResizeFilter();
		resizeFilter.initialize(resizeProperties);
		image = resizeFilter.filter(image);

		MD5OutputStream md5OS = new MD5OutputStream(new NullOutputStream());
		ImageIO.write(image.getAsBufferedImage(), "jpg", md5OS);
		return MD5.asHex(md5OS.hash());
	}

	@Before
	public void setup() throws NodeException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(ImageResourceTest.class.getResourceAsStream(IMAGE_NAME), out);
		testImage = (ImageFile) supply(() -> createImage(node.getFolder(), IMAGE_NAME, out.toByteArray()));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> clear(node));
	}

	@Test
	public void testSaveImage() throws Exception {
		// Check the initial default value of the image
		assertEquals("Check fpx of the image", 0.5f, testImage.getFpX(), 0);
		assertEquals("Check fpy of the image", 0.5f, testImage.getFpY(), 0);

		float fpX = 0.8f;
		float fpY = 0.4f;

		saveImageFP(fpX, fpY);
		// Now load the image again and assert the fp values
		ImageFile updated = execute(ImageFile::reload, testImage);
		assertEquals("Check fpx of the image", fpX, updated.getFpX(), 0);
		assertEquals("Check fpy of the image", fpY, updated.getFpY(), 0);
	}

	@Test
	public void testLoadImageTooBig() throws NodeException, IOException {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("images_maxdimensions", "100x100");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(ImageResourceTest.class.getResourceAsStream(IMAGE_NAME), out);
		try {
			supply(() -> createImage(node.getFolder(), "too_big_image.jpg", out.toByteArray()));
			fail("Should not allow an uploaded image violating max dimensions configuration");
		} catch (NodeException e) {
			assertThat(e.getMessage())
				.as("Expected exception")
				.matches("^Das Bild 'too_big_image.jpg' \\(ID #\\d+, GID = [0-9a-z.-]+\\) mit den Dimensionen 311x211 überschreitet die erlaubten Dimensionen 100x100.$");
		} finally {
			testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("images_maxdimensions", FileFactory.DEFAULT_MAX_DIMENSIONS);
		}		
	}

	@Test
	public void testLoadImageInvalidConfig() throws NodeException, IOException {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("images_maxdimensions", "bogus");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(ImageResourceTest.class.getResourceAsStream(IMAGE_NAME), out);
		supply(() -> createImage(node.getFolder(), "image.jpg", out.toByteArray()));
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("images_maxdimensions", FileFactory.DEFAULT_MAX_DIMENSIONS);	
	}

	@Test
	public void testLoadImage() throws NodeException {
		ImageLoadResponse response = supply(() -> getImageResource().load(Integer.toString(testImage.getId()), false, false, null, null));
		assertEquals(0.5f, response.getImage().getFpX(), 0);
		assertEquals(0.5f, response.getImage().getFpY(), 0);

		float fpX = 0.8f;
		float fpY = 0.4f;

		saveImageFP(fpX, fpY);
		response = supply(() -> getImageResource().load(Integer.toString(testImage.getId()), false, false, null, null));
		assertEquals(fpX, response.getImage().getFpX(), 0);
		assertEquals(fpY, response.getImage().getFpY(), 0);
	}

	@Test
	public void testCopyImage() throws Exception {
		ImageResizeRequest request = new ImageResizeRequest();
		request.setFpX(0.2f);
		request.setFpY(0.1f);
		// Create copy
		request.setCopyFile(true);
		Image image = new Image();
		image.setId(testImage.getId());
		request.setImage(image);
		request.setHeight(100);
		request.setWidth(200);
		request.setTargetFormat("jpg");
		request.setResizeMode("force");

		FileUploadResponse saveResponse = supply(() -> getImageResource().resize(request));
		assertResponseOK(saveResponse);

		ImageLoadResponse response = supply(() -> getImageResource().load(String.valueOf(saveResponse.getFile().getId()), false, false, null, null));
		Image resizedImage = response.getImage();
		assertEquals(0.2f, resizedImage.getFpX(), 0);
		assertEquals(0.1f, resizedImage.getFpY(), 0);
		assertEquals(100, resizedImage.getSizeY().intValue());
		assertEquals(200, resizedImage.getSizeX().intValue());
		assertNotEquals("A new file should have been created", saveResponse.getFile().getId().intValue(), image.getId().intValue());
	}

	@Test
	public void testResizeImage() throws Exception {
		ImageResizeRequest request = new ImageResizeRequest();
		request.setFpX(0.2f);
		request.setFpY(0.1f);
		// Don't copy
		request.setCopyFile(false);
		Image image = new Image();
		image.setId(testImage.getId());
		request.setImage(image);
		request.setHeight(100);
		request.setWidth(200);
		request.setTargetFormat("jpg");
		request.setResizeMode("force");

		FileUploadResponse saveResponse = supply(() -> getImageResource().resize(request));
		assertResponseOK(saveResponse);

		ImageLoadResponse response = supply(() -> getImageResource().load(String.valueOf(saveResponse.getFile().getId()), false, false, null, null));
		Image resizedImage = response.getImage();
		assertEquals(0.2f, resizedImage.getFpX(), 0);
		assertEquals(0.1f, resizedImage.getFpY(), 0);
		assertEquals(100, resizedImage.getSizeY().intValue());
		assertEquals(200, resizedImage.getSizeX().intValue());
		assertEquals("A file should have the same id.", saveResponse.getFile().getId().intValue(), image.getId().intValue());
	}

	@Test
	public void testResizeAndRotateCW() throws Exception {
		ImageResizeRequest request = new ImageResizeRequest();
		request.setCopyFile(false);
		Image image = new Image();
		image.setId(testImage.getId());
		request.setImage(image);
		request.setHeight(100);
		request.setWidth(200);
		request.setTargetFormat("jpg");
		request.setResizeMode("force");
		request.setRotate(ImageRotate.cw);

		FileUploadResponse saveResponse = supply(() -> getImageResource().resize(request));
		assertResponseOK(saveResponse);

		ImageLoadResponse response = supply(() -> getImageResource().load(String.valueOf(saveResponse.getFile().getId()), false, false, null, null));
		Image resizedImage = response.getImage();
		assertEquals(100, resizedImage.getSizeY().intValue());
		assertEquals(200, resizedImage.getSizeX().intValue());
		assertEquals("A file should have the same id.", saveResponse.getFile().getId().intValue(), image.getId().intValue());

		testImage = execute(ImageFile::reload, testImage);
		assertThat(testImage.getMd5()).as("MD5 Sum of resized and rotated Image").isEqualTo(rotateAndResizeCWMd5);
	}

	@Test
	public void testResizeAndRotateCCW() throws Exception {
		ImageResizeRequest request = new ImageResizeRequest();
		request.setCopyFile(false);
		Image image = new Image();
		image.setId(testImage.getId());
		request.setImage(image);
		request.setHeight(200);
		request.setWidth(400);
		request.setTargetFormat("jpg");
		request.setResizeMode("force");
		request.setRotate(ImageRotate.ccw);

		FileUploadResponse saveResponse = supply(() -> getImageResource().resize(request));
		assertResponseOK(saveResponse);

		ImageLoadResponse response = supply(() -> getImageResource().load(String.valueOf(saveResponse.getFile().getId()), false, false, null, null));
		Image resizedImage = response.getImage();
		assertEquals(200, resizedImage.getSizeY().intValue());
		assertEquals(400, resizedImage.getSizeX().intValue());
		assertEquals("A file should have the same id.", saveResponse.getFile().getId().intValue(), image.getId().intValue());

		testImage = execute(ImageFile::reload, testImage);
		assertThat(testImage.getMd5()).as("MD5 Sum of resized and rotated Image").isEqualTo(rotateAndResizeCCWMd5);
	}

	@Test
	public void testRotateCW() throws NodeException {
		Image image = new Image();
		image.setId(testImage.getId());

		ImageLoadResponse response = supply(() -> getImageResource().rotate(new ImageRotateRequest().setImage(image)
				.setCopyFile(false).setRotate(ImageRotate.cw).setTargetFormat("jpg")));
		assertResponseOK(response);
		assertThat(response.getImage()).as("Image").isNotNull();
		assertThat(response.getImage().getSizeX()).as("Image width").isEqualTo(211);
		assertThat(response.getImage().getSizeY()).as("Image height").isEqualTo(311);
		assertThat(response.getImage().getId()).as("Image ID").isEqualTo(image.getId());
		ImageFile resized = supply(t -> t.getObject(ImageFile.class, response.getImage().getId()));
		assertThat(resized).as("Resized Image").hasFieldOrPropertyWithValue("md5", rotateCWMd5);
	}

	@Test
	public void testRotateCCW() throws NodeException {
		Image image = new Image();
		image.setId(testImage.getId());

		ImageLoadResponse response = supply(() -> getImageResource().rotate(new ImageRotateRequest().setImage(image)
				.setCopyFile(false).setRotate(ImageRotate.ccw).setTargetFormat("jpg")));
		assertResponseOK(response);
		assertThat(response.getImage()).as("Image").isNotNull();
		assertThat(response.getImage().getSizeX()).as("Image width").isEqualTo(211);
		assertThat(response.getImage().getSizeY()).as("Image height").isEqualTo(311);
		assertThat(response.getImage().getId()).as("Image ID").isEqualTo(image.getId());
		ImageFile resized = supply(t -> t.getObject(ImageFile.class, response.getImage().getId()));
		assertThat(resized).as("Resized Image").hasFieldOrPropertyWithValue("md5", rotateCCWMd5);
	}

	@Test
	public void testRotateCWAsCopy() throws NodeException {
		Image image = new Image();
		image.setId(testImage.getId());

		ImageLoadResponse response = supply(() -> getImageResource().rotate(new ImageRotateRequest().setImage(image)
				.setCopyFile(true).setRotate(ImageRotate.cw).setTargetFormat("jpg")));
		assertResponseOK(response);
		assertThat(response.getImage()).as("Image").isNotNull();
		assertThat(response.getImage().getSizeX()).as("Image width").isEqualTo(211);
		assertThat(response.getImage().getSizeY()).as("Image height").isEqualTo(311);
		assertThat(response.getImage().getId()).as("Image ID").isNotEqualTo(image.getId());
		ImageFile resized = supply(t -> t.getObject(ImageFile.class, response.getImage().getId()));
		assertThat(resized).as("Resized Image").hasFieldOrPropertyWithValue("md5", rotateCWMd5);
	}

	@Test
	public void testRotateCCWAsCopy() throws NodeException {
		Image image = new Image();
		image.setId(testImage.getId());

		ImageLoadResponse response = supply(() -> getImageResource().rotate(new ImageRotateRequest().setImage(image)
				.setCopyFile(true).setRotate(ImageRotate.ccw).setTargetFormat("jpg")));
		assertResponseOK(response);
		assertThat(response.getImage()).as("Image").isNotNull();
		assertThat(response.getImage().getSizeX()).as("Image width").isEqualTo(211);
		assertThat(response.getImage().getSizeY()).as("Image height").isEqualTo(311);
		assertThat(response.getImage().getId()).as("Image ID").isNotEqualTo(image.getId());
		ImageFile resized = supply(t -> t.getObject(ImageFile.class, response.getImage().getId()));
		assertThat(resized).as("Resized Image").hasFieldOrPropertyWithValue("md5", rotateCCWMd5);
	}

	/**
	 * Get an image resource, that can be used to test REST calls The folder
	 * resource will have the current transaction set
	 * 
	 * @return image resource
	 * @throws NodeException
	 */
	private ImageResource getImageResource() throws NodeException {
		ImageResourceImpl imageResource = new ImageResourceImpl();
		imageResource.setTransaction(TransactionManager.getCurrentTransaction());
		return imageResource;
	}

	private void saveImageFP(float fpX, float fpY) throws NodeException {
		ImageSaveRequest request = new ImageSaveRequest();
		Image restImage = new Image();
		restImage.setFpX(fpX);
		restImage.setFpY(fpY);
		request.setImage(restImage);
		operate(() -> getImageResource().save(testImage.getId(), request));
	}
}
