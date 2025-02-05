package com.gentics.lib.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Vector;

import jakarta.servlet.ServletException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.TestFileProvider;
import com.gentics.testutils.fs.FileUtils;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Variation test for gentics image store.
 *
 * This test can record/store hashes for comparison. Please set the STORE_HASHES
 * setting to true when you want record new checksums.
 *
 * @author johannes2
 *
 */
@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class GenticsImageStoreVariationTest {

	private static final boolean STORE_HASHES = false;

	public static NodeLogger logger = NodeLogger.getNodeLogger(GenticsImageStoreVariationTest.class);

	private static final File HASHES_FILE = new File("src/test/resources/", GenticsImageStoreVariationTest.class.getSimpleName() + "_hashes.properties");

	public static GenticsImageStoreTestUtils imageStore;

	String currentFilename;
	ResizeMode currentMode;
	boolean isCropandresize;
	ResizeSize currentResizeSize;
	int currentImageDimensions[];

	private static File targetDirectory;
	private static Properties hashes = new Properties();

	public static enum ResizeMode {
		FORCE("force"), PROP("prop"), SIMPLE("simple"), SMART("smart");

		private final String key;

		private ResizeMode(String key) {
			this.key = key;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return key;
		}
	}

	public static enum ResizeSize {
		SAME, LARGER, SMALLER
	}

	/**
	 * Constructor for new test variation testcase
	 *
	 * @param fileName
	 * @param mode
	 * @param cropandresize
	 * @param resizeSize
	 * @param cropSize
	 * @throws IOException
	 */
	public GenticsImageStoreVariationTest(String fileName, ResizeMode mode, boolean cropandresize, ResizeSize resizeSize) throws IOException {
		this.currentFilename = fileName;
		this.currentMode = mode;
		this.isCropandresize = cropandresize;
		this.currentResizeSize = resizeSize;

		InputStream ins = GenericTestUtils.getPictureResource(currentFilename);
		currentImageDimensions = imageStore.getDimensions(ins);
	}

	@BeforeClass
	public static void setupOnce() throws IOException {

		System.setProperty("java.awt.headless", "true");
		// PropertyNodeConfig nodeConfig = new PropertyNodeConfig(new
		// Properties());
		targetDirectory = new File(System.getProperty("java.io.tmpdir"), "genticsimagestoretest_" + TestEnvironment.getRandomHash(5));
		// Update the tmpdir to avoid conflicts with other tests
		System.setProperty("java.io.tmpdir", targetDirectory.getAbsolutePath());
		targetDirectory.mkdirs();
		imageStore = new GenticsImageStoreTestUtils(logger, targetDirectory, true);

		// Load the hashes
		if (HASHES_FILE.exists() && !STORE_HASHES) {
			hashes.load(new FileInputStream(HASHES_FILE));
		}
	}

	@AfterClass
	public static void tearDown() {

		// Only remove target directory when we don't want to store results
		if (!STORE_HASHES && targetDirectory.exists()) {
			FileUtils.deleteFile(targetDirectory);
		}

		// Only store hashes when told so
		if (STORE_HASHES) {
			OutputStream os = null;
			try {
				os = new FileOutputStream(HASHES_FILE);
				hashes.store(os, null);
			} catch (IOException e) {
				logger.error("Could not store hashes", e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Get the test parameters
	 *
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: file: {0}, mode: {1}, crop: {2}, resize_size: {3}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	/**
	 * Get the test parameters.
	 *
	 * @return collection of test parameter sets
	 */
	static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (String fileName : TestFileProvider.getImageFilenames()) {
			for (boolean cropandresize : Arrays.asList(true, false)) {
				for (ResizeMode mode : ResizeMode.values()) {
					for (ResizeSize resizeSize : ResizeSize.values()) {
						testData.add(new Object[] { fileName, mode, cropandresize, resizeSize });
					}
				}
			}
		}

		return testData;
	}

	/**
	 * Resize the current image with the specified options. Verify that the
	 * image has the correct resolution and compare stored md5 checksums of the
	 * output image files.
	 *
	 * @throws ServletException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NodeException
	 */
	@Test
	public void testResize() throws ServletException, IOException, NoSuchAlgorithmException, NodeException {
		int cropAreaSize[] = { currentImageDimensions[0] / 4, currentImageDimensions[1] / 4 };
		int cropStartPoint[] = { currentImageDimensions[0] / 2, currentImageDimensions[1] / 2 };

		int size[] = getResizeSize(currentImageDimensions, currentResizeSize);

		String targetFilename = "resized_m_" + currentMode + "_dim_" + currentResizeSize + "_crop_" + String.valueOf(isCropandresize) + "_" + currentFilename;
		if (!STORE_HASHES && (hashes.getProperty(targetFilename + "_size") == null || hashes.getProperty(targetFilename + "_hash") == null)) {
			fail("The hashes property file does not contain assertion information for the target file {" + targetFilename
					+ "}. Please run this test in the STORE_HASHES mode and assert the images manually to update the hashes file.");
		}

		File targetFile = new File(targetDirectory, targetFilename);

		imageStore.resizeImage(currentFilename, targetFile, currentMode, size[0], size[1], isCropandresize, cropStartPoint[0], cropStartPoint[1], cropAreaSize[0], cropAreaSize[1]);

		String hash = generateMD5(targetFile);

		Point resultDim = JavaImageUtils.getImageDimensions(new FileInputStream(targetFile), null);
		String sizeText = resultDim.getX() + "x" + resultDim.getY();
		if (currentMode == ResizeMode.FORCE) {
			assertEquals("The width of the image does not match the expected width.", size[0], resultDim.getX(), 2.0);
			assertEquals("The height of the image does not match the expected height.", size[1], resultDim.getY(), 2.0);
		}

		// We do not compare when this run is just used to store reference data
		if (!STORE_HASHES) {
			assertEquals("The reference resolution for the image does not match the result image. {" + targetFilename + "}", hashes.getProperty(targetFilename + "_size"), sizeText);
			assertEquals("The md5 checksum of the resulting imagefile does not match the reference for image {" + targetFilename + "}",
					hashes.getProperty(targetFilename + "_hash"), hash);

		} else {
			hashes.setProperty(targetFilename + "_size", sizeText);
			hashes.setProperty(targetFilename + "_hash", hash);
		}
	}

	private String generateMD5(File file) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] data = org.apache.commons.io.FileUtils.readFileToByteArray(file);
		byte[] hash = md.digest(data);

		// converting byte array to Hexadecimal String
		StringBuilder sb = new StringBuilder(2 * hash.length);
		for (byte b : hash) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	/**
	 * Return the resize size for the given resize size case.
	 *
	 * @param dimensions
	 * @param currentResizeSize
	 * @return
	 */
	private int[] getResizeSize(int dimensions[], ResizeSize currentResizeSize) {
		switch (currentResizeSize) {
		case LARGER:
			return new int[] { dimensions[0] * 2, dimensions[1] * 3 };
		case SAME:
			return dimensions;
		case SMALLER:
			return new int[] { dimensions[0] / 3, dimensions[1] / 2 };
		default:
			fail("No valid size specified");
		}
		return null;
	}

}
