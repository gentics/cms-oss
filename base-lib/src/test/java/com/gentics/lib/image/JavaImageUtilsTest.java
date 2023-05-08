package com.gentics.lib.image;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.jmage.filter.FilterException;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.testutils.TestFileProvider;
import com.gentics.testutils.fs.FileUtils;
import org.junit.experimental.categories.Category;

/**
 * This test will test some JavaImageUtil functions.
 *
 * @author johannes2
 */
@Category(BaseLibTest.class)
public class JavaImageUtilsTest {


	/**
	 * Test Dimension extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG1(), null);

		assertEquals("X Resolution does not match.", 300, p.x);
		assertEquals("Y Resolution does not match.", 300, p.y);

	}

	/**
	 * Test Dimension extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG2() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG2(), null);

		assertEquals("X Resolution does not match.", 1160, p.x);
		assertEquals("Y Resolution does not match.", 1376, p.y);
	}

	/**
	 * Test Dimension extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG3() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG3(), null);

		assertEquals("X Resolution does not match.", 311, p.x);
		assertEquals("Y Resolution does not match.", 211, p.y);
	}

	/**
	 * Test Dimentsion extraction of JPG's - CMYK
	 * See http://stackoverflow.com/questions/8118712/java-cmyk-to-rgb-with-profile-output-is-too-dark/12132556#12132556
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG4() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG4(), null);

		assertEquals("X Resolution does not match.", 3192, p.x);
		assertEquals("Y Resolution does not match.", 714, p.y);
	}

	/**
	 * Test Dimension extraction of JPG's that trigger a bug in ImageIO See
	 * http://stackoverflow.com/questions/4470958
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG5() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG5(), null);

		assertEquals("X dimension does not match.", 430, p.x);
		assertEquals("Y dimension does not match.", 180, p.y);
	}

	/**
	 * Test Dimension extraction of a JPG where the image header and its
	 * content doesn't match.
	 * imageio originally throwed the following error: "java.lang.IllegalArgumentException:
	 * Numbers of source Raster bands and source color space components do not match"
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionJPG6() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestJPG6(), null);

		assertEquals("X dimension does not match.", 1688, p.x);
		assertEquals("Y dimension does not match.", 2430, p.y);
	}

	/**
	 * Test Dimension extraction of GIF's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionGIF() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestGIF(), null);

		assertEquals("X Resolution does not match.", 1160, p.x);
		assertEquals("Y Resolution does not match.", 1376, p.y);
	}

	/**
	 * Test Dimension extraction of BMP's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionBMP() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestBMP(), null);

		assertEquals("X Resolution does not match.", 285, p.x);
		assertEquals("Y Resolution does not match.", 350, p.y);
	}

	/**
	 * Test Dimension extraction of PNG's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionPNG() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestTransparentPNG(), null);

		assertEquals("X Resolution does not match.", 200, p.x);
		assertEquals("Y Resolution does not match.", 188, p.y);
	}

	/**
	 * Test Dimension extraction of PNG's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionPNG1() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestPNG(), null);

		assertEquals("X Resolution does not match.", 311, p.x);
		assertEquals("Y Resolution does not match.", 211, p.y);
	}

	/**
	 * Test Dimension extraction of ICO's
	 * @throws Exception
	 */
	@Test
	public void testImageDimensionExtractionICO() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getTestICO(), "image/x-icon");

		assertEquals("X Resolution does not match.", 38, p.x);
		assertEquals("Y Resolution does not match.", 38, p.y);
	}

	/**
	 * Test Dimension extraction when using garbage data. A exception should be thrown.
	 */
	@Test(expected = NodeException.class)
	public void testImageDimensionExtractionGARBAGE() throws Exception {
		Point p = JavaImageUtils.getImageDimensions(TestFileProvider.getGarbageData(), null);

		assertEquals("X Resolution does not match.", -1, p.x);
		assertEquals("Y Resolution does not match.", -1, p.y);

	}

	/**
	 * Test DPI extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionJPG() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestJPG1());

		assertEquals("X DPI does not match.", 0, p.x);
		assertEquals("Y DPI does not match.", 0, p.y);
	}

	/**
	 * Test DPI extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionJPG2() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestJPG2());

		assertEquals("X DPI does not match.", 600, p.x);
		assertEquals("Y DPI does not match.", 600, p.y);
	}

	/**
	 * Test DPI extraction of JPG's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionJPG3() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestJPG3());

		assertEquals("X DPI does not match.", 66, p.x);
		assertEquals("Y DPI does not match.", 44, p.y);
	}

	/**
	 * Test DPI extraction of JPGs
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionJPG5() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestJPG5());

		assertEquals("X DPI does not match.", 72, p.x);
		assertEquals("Y DPI does not match.", 72, p.y);
	}

	/**
	 * Test DPI extraction of BMP's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionBMP() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestBMP());

		assertEquals("X DPI does not match.", 0, p.x);
		assertEquals("Y DPI does not match.", 0, p.y);
	}

	/**
	 * Test DPI extraction of GIF's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionGIF() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestGIF());

		assertEquals("X DPI does not match.", 72, p.x);
		assertEquals("Y DPI does not match.", 72, p.y);
	}

	/**
	 * Test DPI extraction of CYMK Jpeg's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionJPG4() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestJPG4());

		assertEquals("X DPI does not match.", 72, p.x);
		assertEquals("Y DPI does not match.", 72, p.y);
	}

	/**
	 * Test DPI extraction of PNG's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionPNG() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestTransparentPNG());

		assertEquals("X DPI does not match.", 0, p.x);
		assertEquals("Y DPI does not match.", 0, p.y);
	}

	/**
	 * Test DPI extraction of PNG's
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionPNG1() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestPNG());

		assertEquals("X DPI does not match.", 66, p.x);
		assertEquals("Y DPI does not match.", 44, p.y);
	}

	/**
	 * Test DPI extraction of ICO's
	 * @throws Exception
	 */
	@Test
	@Ignore("This test fails for unknown reason when executed with java 7")
	public void testImageDPIExtractionICO() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getTestICO());

		assertEquals("X DPI does not match.", 72, p.x);
		assertEquals("Y DPI does not match.", 72, p.y);
	}

	/**
	 * Test DPI extraction of garbage data. Should result in a exception.
	 * @throws Exception
	 */
	@Test
	public void testImageDPIExtractionGARBAGE() throws Exception {
		Point p = JavaImageUtils.getImageDpiResolution(TestFileProvider.getGarbageData());

		assertEquals("X DPI does not match.", 0, p.x);
		assertEquals("Y DPI does not match.", 0, p.y);
	}

	@Test
	public void testImageResize() throws FilterException, IOException {
		InputStream ins = TestFileProvider.getTestJPG1();
		PlanarImage image = JavaImageUtils.getResizedImage(ins, null, null, 1800, 1800, 190, 190, 140, 140);
		// Store the resized image
		ImageIO.write(image, "jpg", new FileOutputStream(new java.io.File(System.getProperty("java.io.tmpdir"), "test2.jpg")));
	}

	/**
	 * Test the resizing of an image and saving that image using the cycle stream wrapper
	 */
	@Test
	public void testImageResizeWrapper() throws Exception {
		InputStream ins = TestFileProvider.getTestJPG2();
		PlanarImage image = JavaImageUtils.getResizedImage(ins, "cropandresize", "force", 800, 100, 1376, 1160, 0, 0);

		// Connect the outputstream from the imageio facility with the
		// inputstream from our filefactory.
		// CycleStreamWrapper csw = new CycleStreamWrapper();
		ByteArrayOutputStream boas = new ByteArrayOutputStream();

		ImageIO.write(image.getAsBufferedImage(), "jpg", boas);

		byte[] result = boas.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(result);

		FileUtils.writeToFile(new File(System.getProperty("java.io.tmpdir"), "test2wsup.jpg"), bais);
	}

}
