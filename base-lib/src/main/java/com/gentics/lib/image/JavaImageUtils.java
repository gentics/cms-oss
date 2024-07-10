/*
 * @author johannes2
 * @date Aug 13, 2010
 * @version $Id: JavaImageUtils.java,v 1.1.4.1 2011-02-02 13:30:29 norbert Exp $
 */
package com.gentics.lib.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.lang.StringUtils;
import org.jmage.filter.FilterException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.sksamuel.scrimage.Dimension;
import com.sksamuel.scrimage.ImmutableImage;

import net.sf.image4j.codec.ico.ICODecoder;

/**
 * This class contains multiple utility methods that help to deal with images.
 */
public final class JavaImageUtils {

	/**
	 * Default DPI: 0x0
	 */
	protected final static Point DEFAULT_DPI = new Point(0, 0);

	/**
	 * Extracts the dpi meta information from the file. The given InputStream is not closed after usage, so this is up to the caller.
	 * @param in Stream from which the image could be loaded.
	 * @param fallback if true, resort to default values in case of an io error while retrieving the information
	 * @return Point including the dpi information within x and y fields. If the DPI information can't be read, DEFAULT_DPI is returned.
	 */
	public static Point getImageDpiResolution(InputStream in) throws NodeException {
		int physicalWidthDpi = 0;
		int physicalHeightDpi = 0;

		try {
			ImageInfo imageInfo = Imaging.getImageInfo(in, null);

			physicalWidthDpi = imageInfo.getPhysicalWidthDpi();
			physicalHeightDpi = imageInfo.getPhysicalHeightDpi();

			if (physicalWidthDpi <= 0 || physicalHeightDpi <= 0) {
				return DEFAULT_DPI;
			}
		} catch (Exception e) {
			return DEFAULT_DPI;
		}

		return new Point(physicalWidthDpi, physicalHeightDpi);
	}

	/**
	 * Resizes the given image to the given specs
	 * 
	 * @param stream
	 * @param mode The mode of the operation (eg. cropandresize)
	 * @param resizeMode The type of the resize (eg. smart, force)
	 * @param cropHeight
	 * @param cropWidth
	 * @param cropStartX
	 * @param cropStartY
	 * @return
	 * @throws IOException
	 * @throws FilterException
	 */
	public static PlanarImage getResizedImage(InputStream stream, String mode, String resizeMode, int height, int width, int cropHeight, int cropWidth, int cropStartX, int cropStartY) throws IOException, FilterException {

		// Read the image and create a planar image for the filter
		BufferedImage renderedImage = ImageIO.read(stream);
		PlanarImage originalImage = PlanarImage.wrapRenderedImage(renderedImage);
    	
		PlanarImage processedImage = originalImage;

		// Only crop when cropandresize is desired
		if ("cropandresize".equalsIgnoreCase(mode)) {
			// Crop
			CropFilter cropFilter = new CropFilter();
			Properties cropProperties = new Properties();

			cropProperties.setProperty(CropFilter.HEIGHT, String.valueOf(cropHeight));
			cropProperties.setProperty(CropFilter.WIDTH, String.valueOf(cropWidth));
			cropProperties.setProperty(CropFilter.TOPLEFTX, String.valueOf(cropStartX));
			cropProperties.setProperty(CropFilter.TOPLEFTY, String.valueOf(cropStartY));
			cropFilter.initialize(cropProperties);
			processedImage = cropFilter.filter(processedImage);
		}

		// Resizer    	
		Properties resizeProperties = new Properties();

		if ("force".equalsIgnoreCase(resizeMode)) {
			resizeProperties.setProperty("MODE", "unproportional");	
		} 
		resizeProperties.setProperty(SmarterResizeFilter.HEIGHT, String.valueOf(height));
		resizeProperties.setProperty(SmarterResizeFilter.WIDTH, String.valueOf(width));
    	
		if ("smart".equalsIgnoreCase(resizeMode)) {
			SmarterResizeFilter smartResizeFilter = new SmarterResizeFilter();

			smartResizeFilter.initialize(resizeProperties);
			processedImage = smartResizeFilter.filter(processedImage);
		} else {
			ResizeFilter resizeFilter = new ResizeFilter();

			resizeFilter.initialize(resizeProperties);
			processedImage = resizeFilter.filter(processedImage);
		}
    	
		if (processedImage == null) {
			throw new FilterException("Resizing failed. Resulting image is null.");
		}
    	
		return processedImage;

	}
    
	/**
	 * Get Dimensions of a file. The given InputStream is not closed after usage, so this is up to the caller.
	 * @param in Stream that contains the image binary data.
	 * @param fileType optional filetype to give a hint about the image type
	 * @return Point including the x and y dimension
	 */
	public static Point getImageDimensions(InputStream in, String fileType) throws NodeException {
		int dimX = -1;
		int dimY = -1;

		// Read the image and extract the information
		try {
			BufferedImage img1 = null;
			ImmutableImage img2 = null;

			// ico's are not supported by ImageIO
			// com.twelvemonkeys.imageio which should add ico support throws an exception
			if ("image/x-icon".equals(fileType)) {
				List<BufferedImage> imgs = ICODecoder.read(in);

				if (!ObjectTransformer.isEmpty(imgs)) {
					img1 = imgs.get(0);
				}
			} else if (StringUtils.startsWith(fileType, "image/webp")) {
				img2 = ImmutableImage.loader().fromStream(in);
			} else {
				img1 = ImageIO.read(in);
			}

			if (img1 != null) {
				dimX = img1.getWidth();
				dimY = img1.getHeight();
			} else if (img2 != null) {
				Dimension dimensions = img2.dimensions();
				dimX = dimensions.getX();
				dimY = dimensions.getY();
			} else {
				throw new NodeException("The image could not be read from stream.");
			}

			return new Point(dimX, dimY);
		} catch (IOException e) {
			throw new NodeException("The image could not be read.", e);
		}
	}

}
