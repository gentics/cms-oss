package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.exception.MethodInvocationException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;

/**
 * Helper class containing functionality for rendering gis URLs common to velocity and handlebars
 */
public class GisRendering {
	/**
	 * "mode" parameter
	 */
	public final static String MODE_ARG = "mode";

	/**
	 * "width" parameter
	 */
	public final static String WIDTH_ARG = "width";

	/**
	 * "height" parameter
	 */
	public final static String HEIGHT_ARG = "height";

	/**
	 * "type" parameter
	 */
	public final static String TYPE_ARG = "type";

	/**
	 * "x" parameter
	 */
	public final static String X_ARG = "x";

	/**
	 * "y" parameter
	 */
	public final static String Y_ARG = "y";

	public static boolean render(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws NodeException, IOException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		// no image, so render #
		if (image == null) {
			writer.write("#");
			return true;
		}

		if (resizeInfo.mode == Mode.fpsmart) {
			if (cropInfo != null) {
				throw new NodeException("Error while rendering gis url: if fpsmart mode is used, crop options may not be specified");
			}
			resizeInfo.mode = Mode.smart;
			cropInfo = calculateFpCropInfo(image, resizeInfo);
		}

		if (renderType.getEditMode() == RenderType.EM_PUBLISH) {
			switch (resizeInfo.type) {
			case phpwidget:
				renderPhpWidget(image, resizeInfo, cropInfo, writer);
				break;
			case url:
			default:
				renderUrl(image, resizeInfo, cropInfo, writer);
				break;
			}
		} else {
			renderPreview(image, resizeInfo, cropInfo, writer);
		}

		return true;
	}

	/**
	 * Calculates the CropInfo for the image based on the target size and the focal point of the image.
	 * @param image the image that is being cropped
	 * @param resizeInfo the ResizeInfo containing the target size
	 * @return the CropInfo for the image
	 * @throws NodeException
	 */
	protected static CropInfo calculateFpCropInfo(ImageFile image, ResizeInfo resizeInfo) throws NodeException {
		if (resizeInfo.width < 0 || resizeInfo.height < 0) {
			throw new NodeException("Error while rendering gis url: if fpsmart mode is used, both width and height must be specified");
		}

		if (image.getSizeX() <= 0 || image.getSizeY() <= 0) {
			return null;
		}

		Map<Object, Object> cropRect = new HashMap<>();
		double sourceAR = (double) image.getSizeX() / (double) image.getSizeY();
		double targetAR = (double) resizeInfo.width / (double) resizeInfo.height;

		if (targetAR > sourceAR) {
			// Keep width and crop height.
			cropRect.put(X_ARG, 0);
			cropRect.put(WIDTH_ARG, image.getSizeX());
			int sourceHeight = image.getSizeY();
			int fpY = (int) (image.getFpY() * (float) sourceHeight);
			int croppedHeight = (int) ((double) image.getSizeX() / targetAR);
			int cropY = calculateFpCropStart(fpY, sourceHeight, croppedHeight);
			cropRect.put(Y_ARG, cropY);
			cropRect.put(HEIGHT_ARG, croppedHeight);
		} else {
			// Keep height and crop width.
			cropRect.put(Y_ARG, 0);
			cropRect.put(HEIGHT_ARG, image.getSizeY());
			int sourceWidth = image.getSizeX();
			int fpX = (int) (image.getFpX() * (float) sourceWidth);
			int croppedWidth = (int) ((double) image.getSizeY() * targetAR);
			int cropX = calculateFpCropStart(fpX, sourceWidth, croppedWidth);
			cropRect.put(X_ARG, cropX);
			cropRect.put(WIDTH_ARG, croppedWidth);
		}

		return new CropInfo(cropRect);
	}

	/**
	 * Calculates the starting coordinate for a one dimension crop (either crop width or crop height)
	 * using the specified focal point coordinate.
	 * @param fpCoordinate the absolute focal point coordinate for the dimension that is being cropped (absolute fpX or absolute fpY)
	 * @param sourceDimension the source dimension (width or height) of the original image
	 * @param croppedDimension the cropped dimension (width or height)
	 * @return the starting coordinate (X or Y) of the crop rectangle
	 */
	protected static int calculateFpCropStart(int fpCoordinate, int sourceDimension, int croppedDimension) {
		int croppedDimHalf = croppedDimension / 2;
		int start = fpCoordinate - croppedDimHalf;
		if (start < 0) {
			start = 0;
		} else {
			if ((start + croppedDimension) > sourceDimension) {
				start = sourceDimension - croppedDimension;
			}
		}
		return start;
	}

	/**
	 * Render the preview URL into the writer
	 * @param image image
	 * @param resizeInfo resize info
	 * @param cropInfo crop info (may be null)
	 * @param writer writer
	 * @throws IOException
	 */
	protected static void renderPreview(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException {
		renderPrefix(resizeInfo, cropInfo, writer);
		writer.write(ObjectTransformer.getString(image.get("url"), null));
	}

	/**
	 * Render the publish URL into the writer
	 * @param image image
	 * @param resizeInfo resize info
	 * @param cropInfo crop info (may be null)
	 * @param writer writer
	 * @throws NodeException 
	 * @throws IOException 
	 */
	protected static void renderUrl(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws NodeException, IOException {
		PropertyResolver resolver = new PropertyResolver(image);

		writer.write(ObjectTransformer.getBoolean(resolver.resolve("folder.node.https", false), false) ? "https://" : "http://");
		writer.write(ObjectTransformer.getString(resolver.resolve("folder.node.host", false), ""));
		renderPrefix(resizeInfo, cropInfo, writer);
		writer.write(getPublishUrl(image));
	}

	/**
	 * Render the php widget into the writer
	 * @param image image
	 * @param resizeInfo resize info
	 * @param cropInfo crop info (may be null)
	 * @param writer writer
	 * @throws IOException 
	 * @throws NodeException 
	 */
	protected static void renderPhpWidget(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException, NodeException {
		String publishPath = getPublishUrl(image);

		writer.write("<?php $this->widget('genticsimagestore.widgets.GenticsImageStoreWidget', array('id' => ");
		writer.write(Integer.toString(image.getId()));
		writer.write(", 'publishPath' => \"");
		writer.write(publishPath.replaceAll("\"", "\\\""));
		writer.write("\", 'resizeOptions' => array('width' => ");
		writer.write(resizeInfo.width > 0 ? Integer.toString(resizeInfo.width) : "\"auto\"");
		writer.write(", 'height' => ");
		writer.write(resizeInfo.height > 0 ? Integer.toString(resizeInfo.height) : "\"auto\"");
		writer.write(", 'mode' => \"");
		writer.write(resizeInfo.mode.toString());
		writer.write("\")");
		if (cropInfo != null) {
			writer.write(", 'cropOptions' => array('x' => ");
			writer.write(Integer.toString(cropInfo.x));
			writer.write(", 'y' => ");
			writer.write(Integer.toString(cropInfo.y));
			writer.write(", 'width' => ");
			writer.write(Integer.toString(cropInfo.width));
			writer.write(", 'height' => ");
			writer.write(Integer.toString(cropInfo.height));
			writer.write(")");
		}
		writer.write(")); ?>");
	}

	/**
	 * Render the GIX Prefix into the writer
	 * @param resizeInfo resize info
	 * @param cropInfo crop info (may be null)
	 * @param writer writer
	 * @throws IOException
	 */
	protected static void renderPrefix(ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException {
		writer.write("/GenticsImageStore/");
		writer.write(resizeInfo.width > 0 ? Integer.toString(resizeInfo.width) : "auto");
		writer.write("/");
		writer.write(resizeInfo.height > 0 ? Integer.toString(resizeInfo.height) : "auto");
		writer.write("/");
		if (cropInfo != null) {
			writer.write("cropandresize/");
		}
		writer.write(resizeInfo.mode.toString());
		if (cropInfo != null) {
			writer.write("/");
			writer.write(Integer.toString(cropInfo.x));
			writer.write("/");
			writer.write(Integer.toString(cropInfo.y));
			writer.write("/");
			writer.write(Integer.toString(cropInfo.width));
			writer.write("/");
			writer.write(Integer.toString(cropInfo.height));
		}
	}

	/**
	 * Get the publish path of the image (node.pub_dir_bin + folder.pub_dir + image.name)
	 * @param image image
	 * @return publish path
	 * @throws NodeException
	 */
	protected static String getPublishUrl(ImageFile image) throws NodeException {
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_REL, RenderUrl.LINK_REL, null);
		urlFactory.setAllowAutoDetection(false);
		urlFactory.setLinkManagement(LinkManagement.OFF);
		return urlFactory.createRenderUrl(ImageFile.class, image.getId()).toString();
	}

	/**
	 * Resize info
	 */
	public static class ResizeInfo {
		/**
		 * Resize mode
		 */
		Mode mode = Mode.prop;

		/**
		 * Target width, -1 means "auto"
		 */
		int width = -1;

		/**
		 * Target height, -1 means "auto"
		 */
		int height = -1;

		/**
		 * Render type
		 */
		Type type = Type.url;

		/**
		 * Create an instance, filled with data from the given map
		 * @param map map
		 * @throws MethodInvocationException
		 */
		public ResizeInfo(Map<Object, Object> map) throws NodeException {
			width = ObjectTransformer.getInt(map.get(WIDTH_ARG), width);
			height = ObjectTransformer.getInt(map.get(HEIGHT_ARG), height);

			if (width < 0 && height < 0) {
				throw new NodeException("Error while rendering gis url: either width or height must be set to positive integer");
			}

			String modeArg = ObjectTransformer.getString(map.get(MODE_ARG), null);
			if (modeArg != null) {
				mode = Mode.valueOf(modeArg);
				if (mode == null) {
					throw new NodeException("Error while rendering gis url: unknown mode \"" + modeArg + "\"");
				}
			}

			String typeArg = ObjectTransformer.getString(map.get(TYPE_ARG), null);
			if (typeArg != null) {
				type = Type.valueOf(typeArg);
				if (type == null) {
					throw new NodeException("Error while rendering gis url: unknown type \"" + typeArg + "\"");
				}
			}
		}
	}

	/**
	 * Crop Info
	 */
	public static class CropInfo {
		/**
		 * x of the top/left corner
		 */
		int x = 0;

		/**
		 * y of the top/left corner
		 */
		int y = 0;

		/**
		 * Crop width
		 */
		int width = 0;

		/**
		 * Crop height
		 */
		int height = 0;

		/**
		 * Create an instance, filled with data from the given map
		 * @param map map with crop data
		 * @throws NodeException
		 */
		public CropInfo(Map<Object, Object> map) throws NodeException {
			x = ObjectTransformer.getInt(map.get(X_ARG), x);
			if (x < 0) {
				throw new NodeException("Error while rendering gis url: x-coordinate for cropping must not be negative (was " + x + ")");
			}
			y = ObjectTransformer.getInt(map.get(Y_ARG), y);
			if (y < 0) {
				throw new NodeException("Error while rendering gis url: y-coordinate for cropping must not be negative (was " + y + ")");
			}

			width = ObjectTransformer.getInt(map.get(WIDTH_ARG), width);
			if (width <= 0) {
				throw new NodeException("Error while rendering gis url: cropping width must be a positive integer");
			}
			height = ObjectTransformer.getInt(map.get(HEIGHT_ARG), height);
			if (height <= 0) {
				throw new NodeException("Error while rendering gis url: cropping height must be a positive integer");
			}
		}
	}

	/**
	 * Resize modes
	 */
	public static enum Mode {
		prop, force, smart, fpsmart
	}

	/**
	 * Render types
	 */
	public static enum Type {
		url, phpwidget
	}
}
