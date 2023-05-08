/*
 * @author norbert
 * @date 11.09.2006
 * @version $Id: ContrastFilter.java,v 1.1 2006-09-27 15:16:33 norbert Exp $
 */
package com.gentics.lib.image;

import java.awt.image.renderable.ParameterBlock;
import java.util.Properties;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.jmage.filter.ConfigurableImageFilter;
import org.jmage.filter.FilterException;

import com.gentics.lib.log.NodeLogger;

/**
 * The contrast filter 
 */
public class ContrastFilter extends ConfigurableImageFilter {

	/**
	 * name of the contrast parameter (ranges between -100 and 100)
	 */
	public static final String CONTRAST = "CONTRAST";

	/**
	 * name of the brightness parameter (ranges between -100 and 100)
	 */
	public static final String BRIGHTNESS = "BRIGHTNESS";

	/**
	 * value to change the contrast (ranges between -127.5 and + 127.5)
	 */
	protected float contrast = 0;

	/**
	 * value to change the brightness (ranges between -255 and 255)
	 */
	protected int brightness = 0;

	/**
	 * the logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(ContrastFilter.class.getName());

	/*
	 * (non-Javadoc)
	 * @see org.jmage.filter.ConfigurableImageFilter#initialize(java.util.Properties)
	 */
	public void initialize(Properties filterProperties) throws FilterException {
		try {
			// read the contrast value, default is 0
			contrast = Float.valueOf(filterProperties.getProperty(CONTRAST, "0")).floatValue();
			if (contrast < -100) {
				contrast = -100;
			} else if (contrast > 100) {
				contrast = 100;
			}
			contrast *= 1.275f;

			// read the brightness value, default is 0
			float fBrightness = Float.valueOf(filterProperties.getProperty(BRIGHTNESS, "0")).floatValue();

			if (fBrightness < -100) {
				fBrightness = -100;
			} else if (fBrightness > 100) {
				fBrightness = 100;
			}
			brightness = Math.round(fBrightness * 2.5f);

			this.filterProperties = filterProperties;
			if (logger.isDebugEnabled()) {
				logger.debug(INITIALIZED);
			}
		} catch (Throwable t) {
			String message = NOT_INITIALIZED + t.getMessage();

			this.filterProperties = null;
			logger.error(message);
			throw new FilterException(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmage.filter.ImageFilter#filter(javax.media.jai.PlanarImage)
	 */
	public PlanarImage filter(PlanarImage image) throws FilterException {
		// set brightness and contrast
		return contrast(image, brightness, contrast);
	}

	/**
	 * Modify the given image by setting brightness and contrast
	 * @param myImage image to be modified
	 * @param level brightness level (between -255 and 255)
	 * @param contrast contrast value (between -127.5 and 127.5)
	 * @return modified image
	 */
	private PlanarImage contrast(PlanarImage myImage, int level, float contrast) throws FilterException {

		// pre-adjust Brightness to set a centre for continuative
		// contrast-modifications
		if (level != 0) {
			try {
				if (level < -255) {
					level = -255;
				}
				if (level > 255) {
					level = 255;
				}
				double[] constants = { (float) level, (float) level, (float) level};
				ParameterBlock pb = new ParameterBlock();

				pb.addSource(myImage);
				pb.add(constants);
				myImage = JAI.create("addconst", pb, null);
			} catch (Exception e) {
				e.printStackTrace();
				return myImage;
			}
		}

		// when no contrast modification shall be done, return the image now
		if (contrast == 0.0) {
			return myImage;
		}

		// start contrast
		try {

			int numBands = myImage.getNumBands();

			// bp is used to compress or pull colorrange
			// bp = BoostPixel ;)
			float[][][] bp = new float[numBands][2][];

			// decrease contrast
			if (contrast < 0) {
				if (contrast < -127.5F) {
					contrast = -127.5F;
				}
				contrast = 127.5F + contrast;

				float innerMax = (255.0F / 2.0F) + contrast;
				float innerMin = (255.0F / 2.0F) - contrast;

				for (int i = 0; i < numBands; i++) {
					bp[i][0] = new float[] { -0.1F, 0.0F, 255.0F, 255.1F};
					bp[i][1] = new float[] { 0.0F, innerMin, innerMax, 255.0F};
				}
			} // increase contrast
			else {
				if (contrast == 0.0F) {
					contrast = 0.00001F;
				}
				if (contrast > 127.5F) {
					contrast = 127.5F;
				}
				contrast = 127.5F - contrast;

				float innerMax = (255.0F / 2.0F) + contrast;
				float innerMin = (255.0F / 2.0F) - contrast;

				for (int i = 0; i < numBands; i++) {
					bp[i][0] = new float[] { 0.0F, innerMin, innerMax, 255.0F};
					bp[i][1] = new float[] { 0.0F, 0.0F, 255.0F, 255.0F};
				}
			}

			return JAI.create("piecewise", myImage, bp);

		} catch (Exception e) {
			throw new FilterException("Error while applying contrast filter: " + e.getLocalizedMessage());
		}
	}
}
