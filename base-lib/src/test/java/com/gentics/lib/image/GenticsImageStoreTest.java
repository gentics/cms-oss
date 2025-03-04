package com.gentics.lib.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import jakarta.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.tests.category.BaseLibTest;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.TestFileProvider;
import com.gentics.testutils.fs.FileUtils;

@Category(BaseLibTest.class)
public class GenticsImageStoreTest {

	/**
	 * blume.jpg DPI: 600x600 Resolution: 1160x1376
	 */
	final String jpgImage2 = "blume.jpg";

	public static NodeLogger logger = NodeLogger.getNodeLogger(GenticsImageStoreTest.class);

	private GenticsImageStoreTestUtils utils;

	private File imagetargetdirectory;

	@Before
	public void setUp() throws Exception {

		System.setProperty("java.awt.headless", "true");
		imagetargetdirectory = new File(System.getProperty("java.io.tmpdir"), "genticsimagestoretest");

		// Clear working directory
		assertTrue(GenticsImageStoreTestUtils.removeFiles(imagetargetdirectory));

		enableJpegQuality();
	}

	private void enableJpegQuality() {
		Properties props = new Properties();
		props.setProperty("contentnode.global.config.image_resizer_jpeg_quality", ".9");

		initConfiguration(props);
	}

	private void disableJpeqQuality() {
		initConfiguration(new Properties());
	}

	private void initConfiguration(Properties props) {
		try {

//			PropertyNodeConfig nodeConfig = new PropertyNodeConfig(props);

			utils = new GenticsImageStoreTestUtils(logger, imagetargetdirectory, false);
		} catch (Exception e) {
			logger.info("Error", e);
		}
	}

	public void executeTestCase(String fn, String md5, String mode,
			int[] targetdim, int[] referencedim) throws Exception {
		assertTrue(utils.doOperation(fn, targetdim[0], targetdim[1], mode));
		assertEqualsImage(fn, mode, referencedim);
	}

	public void executeCropTestCase(String fn, String md5, String mode,
			int[] targetdim, int[] referencedim) throws Exception {
		assertTrue(utils.doDefaultCropOperation(fn, targetdim[0], targetdim[1], mode));
		assertEqualsImage(fn, mode, referencedim);
	}

	public void executeCropTestCase(String fn, String md5, String mode,
			int[] targetdim, int[] referencedim, String[] topLeftDim) throws Exception {
		assertTrue(utils.doCropOperation(fn, targetdim[0], targetdim[1], mode, topLeftDim[0], topLeftDim[1]));
		assertEqualsImage(fn, mode, referencedim);
	}

	private void assertEqualsImage(String fn, String mode, int[] referencedim)
			throws IOException {
		File imagefile = new File(this.imagetargetdirectory.getAbsoluteFile() + "/Resized_" + mode + "_" + fn);
		File referenceFile = new File(this.imagetargetdirectory.getAbsoluteFile() + "/Reference_" + mode + "_" + fn);

		try (FileOutputStream out = new FileOutputStream(referenceFile)) {
			IOUtils.copy(GenericTestUtils.getPictureResource("Reference_" + mode + "_" + fn), out);
		}

		int[] sourcedim = utils.getDimensions(imagefile);

		logger.info("" + sourcedim[0] + " x " + sourcedim[1]);
		assertThat(sourcedim).as("Image dimensions").isEqualTo(referencedim);

		try {
			BufferedImage img1 = utils.getBufferedImage(imagefile);
			BufferedImage img2 = utils.getBufferedImage(referenceFile);
			utils.compareImage(img1, img2);
		} catch (Exception e) {
			fail("Reading images failed", e);
		}

	}

	public void executeTestCase(String fn, String md5sum, String mode) throws Exception {
		int[] targetdim = { 200, 188 };
		int[] referencedim = { 200, 188 };

		executeTestCase(fn, md5sum, mode, targetdim, referencedim);
	}

	@Test
	public void TestSimpleWebP() throws ServletException, IOException {
		String fn = "blume.webp";

		assertTrue(utils.doOperation(fn, 200, 200, "smart"));
	}

	@Test
	public void testSimpleJPG() throws Exception {
		String fn = "blume2.jpeg";

		assertTrue(utils.doOperation(fn, 200, 200, "smart"));
	}

	@Test
	public void testSmartJPG() throws Exception {
		String fn = "blume2.jpeg";

		executeTestCase(fn, "1c1a0fd7ae0f4032e8147acaf2981ce5", "smart");
	}

	@Test
	public void testSmartPNG() throws Exception {
		String fn = "transparent_spider.png";

		executeTestCase(fn, "781b24bbc5dcad490d86d4010c7ec944", "smart");
	}

	@Test
	public void testSmartGIF() throws Exception {
		int[] targetdim = { 200, 221 };
		int[] referencedim = { 200, 221 };
		String fn = "transparent.gif";

		executeTestCase(fn, "37053da8b4c45e098cfbbb74c07cf5cd", "smart", targetdim, referencedim);
	}

	// mode: prop
	@Test
	public void testPropJPG() throws Exception {
		String fn = "blume2.jpeg";
		int[] targetdim = { 200, 188 };
		int[] referencedim = { 158, 188 };

		executeTestCase(fn, "255bc97f387c372f2a5e92667dc76441", "prop", targetdim, referencedim);
	}

	@Test
	public void testPropWebP() throws Exception {
		String fn = "blume.webp";
		int[] targetdim = { 200, 188 };
		int[] referencedim = { 158, 188 };

		executeTestCase(fn, "18b7214aa55c9e1b313d02c018ade264", "prop", targetdim, referencedim);
	}

	@Test
	public void testAdobeImage() throws Exception {
		String fn = "Highlight-Luftfahrt.jpeg";
		int[] targetdim = { 186, 105 };
		int[] referencedim = { 186, 105};

		executeTestCase(fn, "671d13e7e8e67cbd8f68eb7921e4b41a", "prop", targetdim, referencedim);
	}

	@Test
	public void testPropCYMKJPG() throws Exception {
		String fn = "image-dpi72x72-res3192x714-cmyk.jpg";
		int[] targetdim = { 200, 188 };
		int[] referencedim = { 200, 45 };

		executeTestCase(fn, "672a5b491e1cde28783456dc0773a651", "prop", targetdim, referencedim);
	}

	@Test
	public void testPropPNG() throws Exception {
		String fn = "transparent_spider.png";
		int[] targetdim = { 200, 188 };
		int[] referencedim = { 200, 150 };

		executeTestCase(fn, "de482079f425c668e7585326f75c2dd3", "prop", targetdim, referencedim);
	}

	@Test
	public void testPropGIF() throws Exception {
		String fn = "flower.gif";
		int[] targetdim = { 120, 500 };
		int[] referencedim = { 120, 142 };

		executeTestCase(fn, "8774de2c27e0804e2ef4a7b5c942a2a2", "prop", targetdim, referencedim);
	}

	// mode:smart
	@Test
	public void testsimpleBMPBottomUP() throws Exception {
		String fn = "testimg.bmp";

		executeTestCase(fn, "sfsf", "smart");
	}

	// mode:force
	@Test
	public void testForceJPG() throws Exception {
		String fn = "blume2.jpeg";

		executeTestCase(fn, "de680238edf7b47b1f4b3ae0563eef68", "force");
	}

	@Test
	public void testForceWebP() throws Exception {
		String fn = "blume.webp";

		executeTestCase(fn, "8ad3aa0c8d1ec4a56e38b1b14737cff6", "force");
	}

	@Test
	public void testForcePNG() throws Exception {
		String fn = "transparent_spider.png";

		executeTestCase(fn, "c47fda8da0d03520b3afbaba6c54f151", "force");
	}

	@Test
	public void testForceGIF() throws Exception {
		String fn = "flower.gif";
		int[] targetdim = { 100, 241 };
		int[] referencedim = { 100, 241 };

		executeTestCase(fn, "0ad3fcf3f1ade6d6e2c5aeeffbc9acb5", "force", targetdim, referencedim);
	}

	/**
	 * Test special kind of JPG file that has the ability to make ImageIO fail on loading
	 *
	 * @throws Exception
	 */
	@Test
	public void testSpecialJPG() throws Exception {
		String fn = "image-dpi72x72-res430x180-imageio-bug.jpg";

		executeTestCase(fn, "70ae4dc3a7fdec2d3e43ca1183bfb4d8", "force", new int[] { 100, 100 }, new int[] { 100, 100 });
		executeTestCase(fn, "3e642bc72d5c02f84b5e2c2f673c3d4a", "prop", new int[] { 100, 100 }, new int[] { 100, 42 });
		executeTestCase(fn, "3e642bc72d5c02f84b5e2c2f673c3d4a", "simple", new int[] { 100, 100 }, new int[] { 100, 42 });
		executeTestCase(fn, "455c2c455315ab84336e36a666757637", "smart", new int[] { 100, 100 }, new int[] { 100, 100 });
	}

	/**
	 * If the cropping is bigger than the original, the cropping returns the original.
	 * @throws Exception
	 */
	@Test
	public void testBiggerCropForceGIF() throws Exception {
		String fn = "flower.gif";
		String mode = "force";

		File tmpFile = new File(System.getProperty("java.io.tmpdir"), fn);
		InputStream ins = GenericTestUtils.getPictureResource(fn);
		FileUtils.writeToFile(tmpFile, ins);

		int[] targetdim = { 4000, 2410 };
		int[] originalDimensions = utils.getDimensions(tmpFile);

		executeCropTestCase(fn, "0ad3fcf3f1ade6d6e2c5aeeffbc9acb5", mode, targetdim, originalDimensions);

	}

	@Test
	public void testCropDisableJpegQualityProp() throws Exception {
		disableJpeqQuality();
		assertCropping();
	}

	@Test
	public void testCrop() throws Exception {
		assertCropping();
	}

	@Test
	public void testTopLeftCrop() throws Exception {
		String fn = "image-dpi72x72-res430x180-imageio-bug.jpg";
		int[] targetdim = { 99, 45 };
		int[] referencedim = { 99, 45 };
		String[] topLefDim = { "15", "20" };

		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "force", targetdim, referencedim, topLefDim);
		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "prop", targetdim, referencedim, topLefDim);
		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "simple", targetdim, referencedim, topLefDim);
	}

	@Test
	public void testSimpleCropResize() throws Exception {
		String fn = "blume2.jpeg";
		assertTrue(utils.doOperation(fn, 200, 200, "smart", true, "10", "8", "5", "15"));
	}

	@Test(expected = NodeException.class)
	public void testFailingResize() throws Exception {
		GenticsImageStore.handleResizeAction(null, null, null, null, true, null, null, null, null);
	}

	@Test(expected = NodeException.class)
	public void testBogusFilePathSource() throws Exception {
		invokeGIS("somewhere", null, null, GenticsImageStore.SMART_MODE_KEY, false, 210, 200);
	}

	@Test(expected = NodeException.class)
	public void testBogusURISource() throws Exception {
		GenticsImageStore.handleResizeAction(null, new URI("http://www.gentics.com/nothing.jpg").toString(), null, null, true, null, "",
				new Properties(), new Properties());
	}

	@Test
	public void testURISource() throws Exception {
		File testImage = File.createTempFile("testimage", ".jpeg");
		try {
			FileUtils.writeToFile(testImage, TestFileProvider.getTestJPG1());
			GenticsImageStoreResizeResponse response = invokeGIS(null, testImage.toURI().toString(), null, GenticsImageStore.SMART_MODE_KEY, false,
					210, 200);
			Point dim = JavaImageUtils.getImageDimensions(new ByteArrayInputStream(response.getImageData()), "image/jpeg");
			assertEquals("The width of the result image was not correct.", 300, dim.y, 0);
			assertEquals("The height of the result image was not correct.", 300, dim.x, 0);
		} catch (NodeException e) {
			throw e;
		} finally {
			if (testImage != null) {
				testImage.delete();
			}
		}
	}

	@Test
	public void testResizeByFISSource() throws Exception {
		InputStream is = TestFileProvider.getTestJPG1();
		int height = 210;
		int width = 200;
		GenticsImageStoreResizeResponse response = invokeGIS(null, null, is, GenticsImageStore.SMART_MODE_KEY, false, height, width);
		Point dim = JavaImageUtils.getImageDimensions(new ByteArrayInputStream(response.getImageData()), "image/jpeg");
		assertEquals("The width of the result image was not correct.", 300, dim.y, 0);
		assertEquals("The height of the result image was not correct.", 300, dim.x, 0);
	}

	public static GenticsImageStoreResizeResponse invokeGIS(String filePath, String fileUri, InputStream is, String mode, boolean cropandresize,
			int height, int width) throws NodeException {

		Properties cropProperties = new Properties();
		if (cropandresize) {
			cropProperties.put("cropandresize", "true");
			cropProperties.put("TOPLEFTX", 10);
			cropProperties.put("TOPLEFTY", 10);
			cropProperties.put("WIDTH", 33);
			cropProperties.put("HEIGHT", 33);
		}

		Properties filterChainProperties = new Properties();
		filterChainProperties.put("HEIGHT", height);
		filterChainProperties.put("WIDTH", width);
		String filterChain = "smart".equals(mode) ? SmarterResizeFilter.class.getCanonicalName() : ResizeFilter.class.getCanonicalName();
		return GenticsImageStore.handleResizeAction(filePath, fileUri, is, null, cropandresize, null, filterChain, filterChainProperties,
				cropProperties);
	}

	private void assertCropping() throws Exception {
		String fn = "image-dpi72x72-res430x180-imageio-bug.jpg";
		int[] targetdim = { 99, 45 };
		int[] referencedim = { 99, 45 };

		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "force", targetdim, referencedim);
		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "prop", targetdim, referencedim);
		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "simple", targetdim, referencedim);
		executeCropTestCase(fn, "672a5b491e1cde28783456dc0773a651", "smart", targetdim, referencedim);
	}

}
