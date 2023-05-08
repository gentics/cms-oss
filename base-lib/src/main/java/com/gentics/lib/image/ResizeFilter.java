/*
 * @author norbert
 * @date 12.09.2006
 * @version $Id: ResizeFilter.java,v 1.6 2009-12-16 16:12:23 herbert Exp $
 */
package com.gentics.lib.image;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.jmage.filter.ConfigurableImageFilter;
import org.jmage.filter.FilterException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * ResizeFilter, can handle proportional and unproportional resizing
 */
public class ResizeFilter extends ConfigurableImageFilter {
	public static final String HEIGHT = "HEIGHT";

	public static final String DEFAULT_HEIGHT = "-1";

	public static final int HEIGHT_LIMIT = 10000;

	public static final String WIDTH = "WIDTH";

	public static final String DEFAULT_WIDTH = "-1";

	public static final int WIDTH_LIMIT = 10000;

	public static final String MODE = "MODE";

	public static final String FACTORLIMIT = "FACTORLIMIT";

	protected boolean proportional = true;

	protected int scaledWidth = -1;

	protected int scaledHeight = -1;

	protected double factorLimit = -1.;

	protected Map paramMap = null;

	protected RenderingHints hints = null;

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	public ResizeFilter() {
		paramMap = new HashMap();
	}

	/**
	 * Initialize the ImageFilter
	 */
	public void initialize(Properties filterProperties) throws org.jmage.filter.FilterException {
		try {
			scaledWidth = Integer.valueOf(filterProperties.getProperty(WIDTH, DEFAULT_WIDTH)).intValue();
			scaledHeight = Integer.valueOf(filterProperties.getProperty(HEIGHT, DEFAULT_HEIGHT)).intValue();
			proportional = !"unproportional".equals(filterProperties.getProperty(MODE));

			factorLimit = ObjectTransformer.getDouble(filterProperties.getProperty(FACTORLIMIT), factorLimit);

			paramMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			paramMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			paramMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paramMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			paramMap.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
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

	/**
	 * Filter the image, when one of the scaled values is not given it is
	 * calculated proportional
	 * @throws org.jmage.filter.FilterException if an error occurs during
	 *         filtering
	 */
	public PlanarImage filter(PlanarImage image) throws FilterException {
		// no rescaling -> just return the image
		if (scaledHeight < 0 && scaledWidth < 0) {
			return image;
		}
		int width = image.getWidth();
		int height = image.getHeight();

		// initialize scaled parameters
		int usedScaledHeight = scaledHeight;
		int usedScaledWidth = scaledWidth;

		// only one value given -> calculate the other to preserve original
		// ratio
		if (usedScaledHeight < 0) {
			// calculate height
			usedScaledHeight = Math.round(proportional ? (float) height * (float) usedScaledWidth / (float) width : height);
		} else if (usedScaledWidth < 0) {
			// calculate width
			usedScaledWidth = Math.round(proportional ? (float) width * (float) usedScaledHeight / (float) height : width);
		} else {
			if (proportional) {
				if (usedScaledWidth * height != usedScaledHeight * width) {
					// the ratio is modified but the mode is proportional, so rescale the image
					if (usedScaledWidth * height < usedScaledHeight * width) {
						// recalculate scaledHeight
						usedScaledHeight = Math.round((float) height * (float) usedScaledWidth / (float) width);
					} else {
						// recalculate scaledWidht
						usedScaledWidth = Math.round((float) width * (float) usedScaledHeight / (float) height);
					}
				}
			}
		}

		if (usedScaledHeight > HEIGHT_LIMIT) {
			throw new FilterException("The height of {" + usedScaledHeight + "} exceeds the limit of {" + HEIGHT_LIMIT + "} ");
		}
		if (usedScaledWidth > WIDTH_LIMIT) {
			throw new FilterException("The width of {" + usedScaledWidth + "} exceeds the limit of {" + WIDTH_LIMIT + "} ");
		}
        
		if (factorLimit >= 0.) {
			return stepResize(image, usedScaledWidth, usedScaledHeight, 1);
		} else {
			return doResize(image, usedScaledWidth, usedScaledHeight);
		}
	}

	/**
	 * Do the actual resizing of the image to the given width and height
	 * @param image image to resize
	 * @param newWidth new width
	 * @param newHeight new height
	 * @return resized image
	 */
	protected PlanarImage doResize(PlanarImage image, int newWidth, int newHeight) {
		// optimization if the resizing should do nothing
		if (image.getWidth() == newWidth && image.getHeight() == newHeight) {
			return image;
		}

		BufferedImage img = image.getAsBufferedImage();
		// only use "quality resizing" if factorLimit is not set .. since otherwise we have multi-step resizing .. in .. multi-step resizing..
		BufferedImage resized = getScaledInstance(img, newWidth, newHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, factorLimit < 0);
		// BufferedImage resized = getScaledInstance(img, newWidth, newHeight, RenderingHints.VALUE_INTERPOLATION_, true);
		// BufferedImage resized = createResizedCopy(img, newWidth, newHeight, true);
		// waaaaaaaaaaaaaaarum .. why doesn't this work with transparent gifs :(
		// Image resized = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        

		ParameterBlock parameterblock = new ParameterBlock();

		parameterblock.add(resized);
		return JAI.create("awtimage", parameterblock, hints);

		// ParameterBlock parameterblock = new ParameterBlock();
		// parameterblock.addSource(image);
		// javax.media.jai.RenderableOp renderableop = JAI.createRenderable("renderable",
		// parameterblock);
		// return (PlanarImage) renderableop.createScaledRendering(newWidth,
		// newHeight, hints);
	}
    
	/**
	 * stolen from
	 * http://java.sun.com/products/java-media/2D/reference/faqs/index.html#Q_How_do_I_create_a_resized_copy
	 * 
	 * 
	 * @deprecated NOT USED ANY MORE
	 */
	BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight,
			boolean preserveAlpha) {
		int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
		Graphics2D g = scaledBI.createGraphics();

		if (preserveAlpha) {
			g.setComposite(AlphaComposite.Src);
		}
		if (((BufferedImage) originalImage).getColorModel().getTransparency() == Transparency.OPAQUE) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		}
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		return scaledBI;
	}
    
	/**
	 * stolen from http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
	 * 
	 * 
	 * Convenience method that returns a scaled instance of the
	 * provided {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance,
	 *    in pixels
	 * @param targetHeight the desired height of the scaled instance,
	 *    in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step
	 *    scaling technique that provides higher quality than the usual
	 *    one-step technique (only useful in downscaling cases, where
	 *    {@code targetWidth} or {@code targetHeight} is
	 *    smaller than the original dimensions, and generally only when
	 *    the {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
			int targetHeight, Object hint, boolean higherQuality) {
		int type = (img.getColorModel().getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        
		if (img.getType() == BufferedImage.TYPE_3BYTE_BGR) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
        
		BufferedImage ret = (BufferedImage) img;
		int w, h;

		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality) {
				if (w > targetWidth) {
					w /= 2;
					if (w < targetWidth) {
						w = targetWidth;
					}
				} else if (w < targetWidth) {
					w *= 2;
					if (w > targetWidth) {
						w = targetWidth;
					}
				}
			}

			if (higherQuality) {
				if (h > targetHeight) {
					h /= 2;
					if (h < targetHeight) {
						h = targetHeight;
					}
				} else if (h < targetHeight) {
					h *= 2;
					if (h > targetHeight) {
						h = targetHeight;
					}
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();

			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			if (img.getColorModel().getTransparency() != Transparency.OPAQUE) {
				g2.setComposite(AlphaComposite.Src);
			}
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

	/**
	 * Do a recursive step-by-step resizing
	 * @param image image to resize
	 * @param newWidth new width
	 * @param newHeight new height
	 * @param stepCounter step counter of the recursion
	 * @return resized image
	 */
	protected PlanarImage stepResize(PlanarImage image, int newWidth, int newHeight, int stepCounter) {
		// determine the original size
		int width = image.getWidth();
		int height = image.getHeight();

		if (logger.isDebugEnabled()) {
			logger.debug("resize " + width + " x " + height + " to " + newWidth + " x " + newHeight);
		}

		if (factorLimit <= 0.) {
			return doResize(image, newWidth, newHeight);
		}

		// determine the factors
		float widthFactor = (float) newWidth / (float) width;
		float heightFactor = (float) newHeight / (float) height;

		if (widthFactor < heightFactor) {
			// width factor is smaller
			if (widthFactor < factorLimit) {
				if (logger.isDebugEnabled()) {
					logger.debug("width factor " + widthFactor + " < " + factorLimit);
				}
				// need to resize in steps, resize the width with the
				// factorlimit and resize to the correct format
				int tempNewWidth = (int) Math.round((double) width * factorLimit);
				int tempNewHeight = Math.round((float) newHeight * (float) tempNewWidth / (float) newWidth);

				if (logger.isDebugEnabled()) {
					logger.debug("temp resized " + width + " x " + height + " to " + tempNewWidth + " x " + tempNewHeight + " (step " + stepCounter + ")");
				}
				// do the step resize and do the recursion call
				return stepResize(doResize(image, tempNewWidth, tempNewHeight), newWidth, newHeight, stepCounter + 1);
			} else {
				// we can resize in one step
				if (logger.isDebugEnabled()) {
					logger.debug("\tresized " + width + " x " + height + " to " + newWidth + " x " + newHeight + " (step " + stepCounter + ")");
				}
				return doResize(image, newWidth, newHeight);
			}
		} else {
			// height factor is smaller
			if (heightFactor < factorLimit) {
				if (logger.isDebugEnabled()) {
					logger.debug("height factor " + heightFactor + " < " + factorLimit);
				}
				// need to resize in steps, resize the height with the
				// factorlimit and resize to the correct format
				int tempNewHeight = (int) Math.round((double) height * factorLimit);
				int tempNewWidth = Math.round((float) newWidth * (float) tempNewHeight / (float) newHeight);

				if (logger.isDebugEnabled()) {
					logger.debug("\ttemp resized " + width + " x " + height + " to " + tempNewWidth + " x " + tempNewHeight + " (step " + stepCounter + ")");
				}
				// do the step resize and do the recursion call
				return stepResize(doResize(image, tempNewWidth, tempNewHeight), newWidth, newHeight, stepCounter + 1);
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("\tresized " + width + " x " + height + " to " + newWidth + " x " + newHeight + " (step " + stepCounter + ")");
				}
				// we can resize in one step
				return doResize(image, newWidth, newHeight);
			}
		}
	}
}
