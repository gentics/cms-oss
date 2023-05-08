/*
 * @author norbert
 * @date 20.09.2006
 * @version $Id: SmarterResizeFilter.java,v 1.3 2009-12-16 16:12:23 herbert Exp $
 */
package com.gentics.lib.image;

import java.awt.RenderingHints;
import java.util.Properties;

import javax.media.jai.PlanarImage;

import org.jmage.filter.FilterException;
import org.jmage.filter.size.CropFilter;
import org.jmage.filter.size.SmartResizeFilter;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * Make the SmartResizeFilter even smarter
 */
public class SmarterResizeFilter extends SmartResizeFilter {

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	public static final String FACTORLIMIT = "FACTORLIMIT";

	protected double factorLimit = -1.;

	/**
	 * Create an instance of the smarter resize filter
	 */
	public SmarterResizeFilter() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jmage.filter.ConfigurableImageFilter#initialize(java.util.Properties)
	 */
	public void initialize(Properties filterProperties) throws org.jmage.filter.FilterException {
		try {
			scaledWidth = Integer.valueOf(filterProperties.getProperty(WIDTH, "-1")).intValue();
			scaledHeight = Integer.valueOf(filterProperties.getProperty(HEIGHT, "-1")).intValue();

			factorLimit = ObjectTransformer.getDouble(filterProperties.getProperty(FACTORLIMIT), factorLimit);

			paramMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			paramMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			paramMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paramMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			hints = new RenderingHints(paramMap);

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

	/* (non-Javadoc)
	 * @see org.jmage.filter.ImageFilter#filter(javax.media.jai.PlanarImage)
	 */
	public PlanarImage filter(PlanarImage image) throws FilterException {
		// if no scaling was set, do nothing
		if (scaledWidth < 0 && scaledHeight < 0) {
			return image;
		}
		int width = image.getWidth();
		int height = image.getHeight();

		int usedScaledWidth = scaledWidth;
		int usedScaledHeight = scaledHeight;

		// if one of the parameters was not set, it defaults to the original value
		if (usedScaledHeight < 0) {
			usedScaledHeight = height;
		}
		if (usedScaledWidth < 0) {
			usedScaledWidth = width;
		}

		float scaledAR = ((float) usedScaledWidth / usedScaledHeight);
		float aR = ((float) width / height);

		if (scaledAR > aR) {
			PlanarImage temp = resize(image, usedScaledWidth, (height * usedScaledWidth) / width);
			int heightAdjust = temp.getHeight() - usedScaledHeight;

			image = cropHeight(temp, heightAdjust);
		} else {
			PlanarImage temp = resize(image, Math.round((float) (width * usedScaledHeight) / (float) height), usedScaledHeight);
			int widthAdjust = temp.getWidth() - usedScaledWidth;

			image = cropWidth(temp, widthAdjust);
		}
		return image;
	}

	/**
	 * Resize the given image to the new width and height
	 * @param image image to resize
	 * @param newWidth new width
	 * @param newHeight new height
	 * @return resized image
	 * @throws FilterException
	 */
	protected PlanarImage resize(PlanarImage image, int newWidth, int newHeight) throws FilterException {
		ResizeFilter resizer = new ResizeFilter();
		Properties props = new Properties();

		props.put(ResizeFilter.HEIGHT, String.valueOf(newHeight));
		props.put(ResizeFilter.WIDTH, String.valueOf(newWidth));
		props.put(ResizeFilter.MODE, "unproportional");
		if (factorLimit > 0) {
			props.put(ResizeFilter.FACTORLIMIT, String.valueOf(factorLimit));
		}
		resizer.initialize(props);
		return resizer.filter(image);
	}

	/**
	 * Crop the height of the image to the given height
	 * @param image image to crop
	 * @param heightAdjust new height
	 * @return cropped image
	 */
	protected PlanarImage cropHeight(PlanarImage image, int heightAdjust) throws FilterException {

		CropFilter cropper = new CropFilter();
		Properties props = new Properties();
		int top = heightAdjust / 2;
		int bottom = heightAdjust - top;

		props.put("TOP", String.valueOf(top));
		props.put("BOTTOM", String.valueOf(bottom));
		cropper.initialize(props);
		image = cropper.filter(image);
		return image;
	}

	/**
	 * Crop the width of the image to the given value
	 * @param image image to crop
	 * @param widthAdjust new width
	 * @return cropped image
	 */
	protected PlanarImage cropWidth(PlanarImage image, int widthAdjust) throws FilterException {

		CropFilter cropper = new CropFilter();
		Properties props = new Properties();
		int left = widthAdjust / 2;
		int right = widthAdjust - left;

		props.put("LEFT", String.valueOf(left));
		props.put("RIGHT", String.valueOf(right));
		cropper.initialize(props);
		image = cropper.filter(image);
		return image;
	}
}
