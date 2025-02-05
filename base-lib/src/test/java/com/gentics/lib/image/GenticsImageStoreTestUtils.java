package com.gentics.lib.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletException;

import com.gentics.api.lib.upload.FileInformation;
import com.gentics.lib.image.GenticsImageStoreVariationTest.ResizeMode;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.fs.FileUtils;
import com.sksamuel.scrimage.ImmutableImage;

public class GenticsImageStoreTestUtils extends GenticsImageStore {

	public static NodeLogger logger;

	File targetdir;

	boolean skipmd5sumcheck;

	public GenticsImageStoreTestUtils(NodeLogger logger, File targetdir, boolean skipmd5sumcheck) {
		GenticsImageStoreTestUtils.logger = logger;
		this.targetdir = targetdir;
		// this.sourcedir = sourcedir;
		this.skipmd5sumcheck = skipmd5sumcheck;

	}

	/**
	 * Get Dimensions of a file. Returns -1, -1 if file could not be opend/found
	 *
	 * @param imageFileStream
	 * @return
	 * @throws IOException
	 */
	public int[] getDimensions(InputStream imageFileStream) throws IOException {
		return getDimensions(getBufferedImage(imageFileStream));
	}

	/**
	 * Get Dimensions of a file. Returns -1, -1 if file could not be opend/found
	 *
	 * @param imageFileStream
	 * @return
	 * @throws IOException
	 */
	public int[] getDimensions(File imageFile) throws IOException {
		return getDimensions(getBufferedImage(imageFile));
	}

	/**
	 * Get a {@link BufferedImage} instance from the given input stream
	 * @param imageFileStream input stream
	 * @return BufferedImage
	 * @throws IOException
	 */
	public BufferedImage getBufferedImage(InputStream imageFileStream) throws IOException {
		BufferedImage buffered = ImageIO.read(imageFileStream);
		if (buffered == null) {
			ImmutableImage immutable = ImmutableImage.loader().fromStream(imageFileStream);
			if (immutable != null) {
				buffered = immutable.awt();
			}
		}
		return buffered;
	}

	/**
	 * Get a {@link BufferedImage} instance from the given file
	 * @param imageFile file
	 * @return BufferedImage
	 * @throws IOException
	 */
	public BufferedImage getBufferedImage(File imageFile) throws IOException {
		BufferedImage buffered = ImageIO.read(imageFile);
		if (buffered == null) {
			ImmutableImage immutable = ImmutableImage.loader().fromFile(imageFile);
			if (immutable != null) {
				buffered = immutable.awt();
			}
		}
		return buffered;
	}

	/**
	 * Get Dimensions of a file. Returns -1, -1 if file could not be opend/found
	 *
	 * @param imagefile
	 * @return
	 */
	public int[] getDimensions(BufferedImage image) {
		int[] dim = { -1, -1 };

		try {
			if (image == null) {
				return dim;
			}
			dim[0] = image.getWidth();
			dim[1] = image.getHeight();
		} catch (Exception e) {
			logger.info("IOException", e);
			return dim;
		}
		return dim;
	}

	public void compareImage(BufferedImage imga, BufferedImage imgb) {

		Raster ra = imga.getData();
		DataBuffer dba = ra.getDataBuffer();
		int sizea = dba.getSize();

		Raster rb = imga.getData();
		DataBuffer dbb = rb.getDataBuffer();
		int sizeb = dbb.getSize();

		assertThat(sizea).as("Image size").isEqualTo(sizeb);

		for (int i = 0; i < sizea; i++) {
			int pxa = dba.getElem(i);
			int pxb = dbb.getElem(i);

			assertThat(pxa).as("Pixel at position " + i).isEqualTo(pxb);

		}
	}

	/**
	 * Remove files in directory. This method creates the directory if it does not exist.
	 *
	 * @param directory
	 * @return
	 */
	public static boolean removeFiles(File directory) {

		// Create directory if it does not exist
		if (!directory.isDirectory()) {
			try {
				return directory.mkdir();
			} catch (Exception e) {
				logger.error("Directory for export could not be created", e);
				return false;
			}
		}

		// Delete files in directory
		File[] files = directory.listFiles();
		int i = 0;

		while (i < files.length) {
			if (!files[i].delete()) {
				return false;
			}
			i++;
		}

		return true;

	}

	/**
	 * Do a Resize and crop operation on an imagefile
	 */
	public boolean doDefaultCropOperation(String fn, int width, int height, String mode) throws ServletException, IOException {
		return doOperation(fn, width, height, mode, true, "0", "0", Integer.toString(width), Integer.toString(height));
	}

	/**
	 * Do a Resize and crop operation on an imagefile
	 */
	public boolean doCropOperation(String fn, int width, int height, String mode, String topLeftX, String topLeftY) throws ServletException, IOException {
		return doOperation(fn, width, height, mode, true, topLeftX, topLeftY, Integer.toString(width), Integer.toString(height));
	}

	/**
	 * Do only a ResizeOperation on an imagefile
	 */
	public boolean doOperation(String fn, int width, int height, String mode) throws ServletException, IOException {
		return doOperation(fn, width, height, mode, false, null, null, null, null);
	}

	/**
	 * Do a Resize/crop Operation on an imagefile
	 */
	public boolean doOperation(String fn, int width, int height, String mode, boolean crop, String topLeftX, String topLeftY, String cropWidth,
			String cropHeight) throws ServletException, IOException {

		// Create a tempfile from the resource
		File tmpFile = new File(System.getProperty("java.io.tmpdir"), fn);

		InputStream ins = GenericTestUtils.getPictureResource(fn);

		FileUtils.writeToFile(tmpFile, ins);

		FileInformation sourceimageinfo = new FileInformation(tmpFile);

		FileInformation targetimageinfo;

		targetimageinfo = invokeGIS(mode, String.valueOf(width), String.valueOf(height), crop, topLeftX, topLeftY, cropWidth, cropHeight,
				sourceimageinfo);

		String target = this.targetdir.getAbsolutePath() + "/Resized_" + mode + "_" + fn;

		logger.info("writing to: " + target);
		this.writeImage(targetimageinfo, target);

		File resizedFile = new File(target);

		if (width > 0 && height > 0) {
			assertThat(resizedFile.length())
				.as("Result file size")
				.isGreaterThan(0);
		}

		return true;

	}

	public void resizeImage(String sourceFilename, File targetFile, ResizeMode mode, int width, int height, boolean crop, int topLeftX, int topLeftY,
			int cropWidth, int cropHeight) throws ServletException, IOException {

		// Create a tempfile from the resource
		File tmpFile = new File(System.getProperty("java.io.tmpdir"), sourceFilename);
		InputStream ins = GenericTestUtils.getPictureResource(sourceFilename);
		FileUtils.writeToFile(tmpFile, ins);
		FileInformation sourceimageinfo = new FileInformation(tmpFile);

		FileInformation targetimageinfo = invokeGIS(mode.toString(), String.valueOf(width), String.valueOf(height), crop, String.valueOf(topLeftX),
				String.valueOf(topLeftY), String.valueOf(cropWidth), String.valueOf(cropHeight), sourceimageinfo);

		logger.info("writing to: " + targetFile);
		writeImage(targetimageinfo, targetFile.getAbsolutePath());

	}

}
