/*
 * Created on May 31, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gentics.lib.image;

import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import com.gentics.lib.log.NodeLogger;
import com.sun.media.jai.codec.SeekableStream;

/**
 * @author Liviu
 * @version: $Id: GenticsImageResizer.java,v 1.4 2010-09-28 17:01:34 norbert Exp $
 */
public class GenticsImageResizer {
    
	private static final NodeLogger logger = NodeLogger.getNodeLogger(GenticsImageResizer.class);
    
	/**
	 * Crops an image using the given topleft point and the with and height parameter
	 * @param image
	 * @param topleftx
	 * @param toplefty
	 * @param with
	 * @param height
	 * @param imagetype
	 * @return cropped image as bytearray
	 */
	public static byte[] crop(byte[] image, float topleftx, float toplefty, float width, float height, String imagetype) {
		byte[] resized = null;

		// Loads the image from the given byte array
		InputStream is = new ByteArrayInputStream(image);
		SeekableStream s = SeekableStream.wrapInputStream(is, true);
		PlanarImage img = (PlanarImage) JAI.create("stream", s);
        
		int originalHeight = img.getHeight();
		int originalWidth = img.getWidth();
        
		if ((topleftx >= 0 && topleftx < originalWidth) && (toplefty >= 0 && toplefty < originalHeight) && ((topleftx + width) < originalWidth)
				&& ((toplefty + height) < originalHeight)) {
        
			// ... and create parameters for the crop operation
			ParameterBlock pb = new ParameterBlock();

			pb.addSource(img);
			// Params are added in x, y, width, height order
			pb.add(topleftx);
			pb.add(toplefty);
			pb.add(width);
			pb.add(height);
    
			if (logger.isDebugEnabled()) {
				logger.debug("Cropping Images - {toplefty: " + toplefty + ", topleftx: " + topleftx + ", width: " + width + ", height: " + height + "}");
			}
            
			// Now crop into a new PlanarImage
			img = JAI.create("crop", pb, null);
		} else {
			logger.error(
					"Cannot crop image (w:" + originalWidth + " h:" + originalHeight + ") {toplefty: " + toplefty + ", topleftx: " + topleftx + ", width: " + width
					+ ", height: " + height + "} , skipping crop");
		}

		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			JAI.create("encode", img, stream, imagetype == null ? "JPEG" : imagetype, null);
			resized = stream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resized;
	}

	public static byte[] resize(byte[] image, int maxwidth, int maxheight, String imagetype) {
		if ("x-png".equals(imagetype)) {
			imagetype = "png";
		}
		if ("pjpeg".equals(imagetype)) {
			imagetype = "jpeg";
		}
		byte[] resized = null;

		// Loads the image from the given byte array
		InputStream is = new ByteArrayInputStream(image);
		SeekableStream s = SeekableStream.wrapInputStream(is, true);
		PlanarImage img = (PlanarImage) JAI.create("stream", s);

		// only do some resizing if at least one of the values is positive
		if (maxwidth > 0 || maxheight > 0) {
			// values <= 0 will be ignored
			if (maxwidth <= 0) {
				maxwidth = (img.getWidth() * maxheight) / img.getHeight();
			} else if (maxheight <= 0) {
				maxheight = (img.getHeight() * maxwidth) / img.getWidth();
			}

			// compute what/how to scale
			float scaleFactor = Math.min(maxwidth / (float) img.getWidth(), maxheight / (float) img.getHeight());

			if (logger.isDebugEnabled()) {
				logger.debug(
						"Resizing Images - scaleFactor: {" + scaleFactor + "}, maxwidth: {" + maxwidth + "}, maxheight: {" + maxheight + "}, image width: {"
						+ img.getWidth() + "}, image height: {" + img.getHeight() + "}");
			}
			// we scale the image
			ParameterBlockJAI pb = new ParameterBlockJAI("scale");

			pb.addSource(img);
			pb.setParameter("xScale", scaleFactor); // x Scale Factor
			pb.setParameter("yScale", scaleFactor); // y Scale Factor
			pb.setParameter("xTrans", 0.0F); // x Translate amount
			pb.setParameter("yTrans", 0.0F); // y Translate amount
			pb.setParameter("interpolation", new InterpolationNearest());
			img = JAI.create("scale", pb, null);
		}

		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			JAI.create("encode", img, stream, imagetype == null ? "JPEG" : imagetype, null);
			resized = stream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resized;
	}

}
