package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;

/**
 * Directive for rendering GenticsImageStore URLs
 */
public class GisDirective extends Directive {
	private static final String PATH_CROPANDRESIZE = "cropandresize";

	private static final String PATH_SIZE_AUTO = "auto";

	public static final String PATH_PREFIX = "/GenticsImageStore/";

	/**
	 * First argument must be the image
	 */
	public final static int ARGS_IMAGE = 0;

	/**
	 * Second argument must be the resizeinfo
	 */
	public final static int ARGS_RESIZE = 1;

	/**
	 * Third argument may be the cropinfo
	 */
	public final static int ARGS_CROP = 2;

	/**
	 * Minimum number of arguments
	 */
	public final static int MIN_ARGUMENTS = 2;

	/**
	 * Maximum number of arguments
	 */
	public final static int MAX_ARGUMENTS = 3;

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

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#getName()
	 */
	@Override
	public String getName() {
		return "gtx_gis";
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#getType()
	 */
	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
		super.init(rs, context, node);

		int argCount = node.jjtGetNumChildren();
		if (argCount < MIN_ARGUMENTS || argCount > MAX_ARGUMENTS) {
			throw new TemplateInitException("#" + getName() + "() requires between " + MIN_ARGUMENTS + " and " + MAX_ARGUMENTS
					+ " arguments, but was called with " + argCount + " arguments", context.getCurrentTemplateName(), node.getColumn(), node.getLine());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#render(org.apache.velocity.context.InternalContextAdapter, java.io.Writer, org.apache.velocity.runtime.parser.node.Node)
	 */
	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			ImageFile image = getImage(context, node);
			ResizeInfo resizeInfo = getResizeInfo(context, node);
			CropInfo cropInfo = getCropInfo(context, node);

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
		} catch (NodeException e) {
			throw new MethodInvocationException("Error while rendering object", e, getName(), context.getCurrentTemplateName(), node.jjtGetChild(0)
					.getLine(), node.jjtGetChild(0).getColumn());
		}
	}

	/**
	 * Get the image to render
	 * @param node node
	 * @return image or null, if not found
	 * @throws NodeException
	 */
	protected ImageFile getImage(InternalContextAdapter context, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Object imageChild = node.jjtGetChild(ARGS_IMAGE).value(context);
		if (imageChild instanceof Resolvable) {
			Resolvable resolvable = (Resolvable)imageChild;
			if (ObjectTransformer.getBoolean(resolvable.get("isimage"), false)) {
				return t.getObject(ImageFile.class, ObjectTransformer.getInt(resolvable.get("id"), 0));
			}
		} else {
			return t.getObject(ImageFile.class, ObjectTransformer.getInt(imageChild, 0));
		}

		return null;
	}

	/**
	 * Get the resize info
	 * @param context context
	 * @param node node
	 * @return resize info
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected ResizeInfo getResizeInfo(InternalContextAdapter context, Node node) throws NodeException {
		return new ResizeInfo((Map<Object, Object>)node.jjtGetChild(ARGS_RESIZE).value(context));
	}

	/**
	 * Get the crop info (may be null)
	 * @param context context
	 * @param node node
	 * @return crop info (or null)
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected CropInfo getCropInfo(InternalContextAdapter context, Node node) throws NodeException {
		if (node.jjtGetNumChildren() > ARGS_CROP) {
			return new CropInfo((Map<Object, Object>)node.jjtGetChild(ARGS_CROP).value(context));
		} else {
			return null;
		}
	}

	/**
	 * Calculates the CropInfo for the image based on the target size and the focal point of the image.
	 * @param image the image that is being cropped
	 * @param resizeInfo the ResizeInfo containing the target size
	 * @return the CropInfo for the image
	 * @throws NodeException
	 */
	protected CropInfo calculateFpCropInfo(ImageFile image, ResizeInfo resizeInfo) throws NodeException {
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
	protected int calculateFpCropStart(int fpCoordinate, int sourceDimension, int croppedDimension) {
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
	protected void renderPreview(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException {
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
	protected void renderUrl(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws NodeException, IOException {
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
	protected void renderPhpWidget(ImageFile image, ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException, NodeException {
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

	public static Optional<Pair<ResizeInfo, CropInfo>> unrenderPrefix(String maybePrefix) {
		return Optional.ofNullable(maybePrefix)
				//.filter(prefix -> prefix.startsWith(PATH_PREFIX))
				.map(prefix -> prefix.split("/"))
				.map(parts -> {
					try {
						ResizeInfo resizeInfo = null;
						CropInfo cropInfo = null;
						int width = PATH_SIZE_AUTO.equals(parts[0]) ? -1 : Integer.parseInt(parts[0]);
						int height = PATH_SIZE_AUTO.equals(parts[1]) ? -1 : Integer.parseInt(parts[1]);
						String mode;
						String type = null;
						if (PATH_CROPANDRESIZE.equals(parts[2])) {
							mode = parts[3];
							int cropX = Integer.parseInt(parts[4]);
							int cropY = Integer.parseInt(parts[5]);
							int cropWidth = Integer.parseInt(parts[6]);
							int cropHeight = Integer.parseInt(parts[7]);
							cropInfo = new CropInfo(cropX, cropY, cropWidth, cropHeight);
						} else {
							mode = parts[2];
						}
						resizeInfo = new ResizeInfo(width, height, mode, type);
						return Pair.of(resizeInfo, cropInfo);
					} catch (Throwable e) {
						throw new IllegalStateException(e);
					}
				});
	}
	/**
	 * Render the GIX Prefix into the writer
	 * @param resizeInfo resize info
	 * @param cropInfo crop info (may be null)
	 * @param writer writer
	 * @throws IOException
	 */
	protected void renderPrefix(ResizeInfo resizeInfo, CropInfo cropInfo, Writer writer) throws IOException {
		writer.write(PATH_PREFIX);
		writer.write(resizeInfo.width > 0 ? Integer.toString(resizeInfo.width) : PATH_SIZE_AUTO);
		writer.write("/");
		writer.write(resizeInfo.height > 0 ? Integer.toString(resizeInfo.height) : PATH_SIZE_AUTO);
		writer.write("/");
		if (cropInfo != null) {
			writer.write(PATH_CROPANDRESIZE + "/");
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
	protected String getPublishUrl(ImageFile image) throws NodeException {
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

		ResizeInfo(int width, int height, String mode, String type) throws NodeException {
			if (width < 0 && height < 0) {
				throw new NodeException("Error while rendering gis url: either width or height must be set to positive integer");
			}
			this.width = width;
			this.height = height;
			if (mode != null) {
				this.mode = Mode.valueOf(mode);
				if (this.mode == null) {
					throw new NodeException("Error while rendering gis url: unknown mode \"" + mode + "\"");
				}
			}
			if (type != null) {
				this.type = Type.valueOf(type);
				if (this.type == null) {
					throw new NodeException("Error while rendering gis url: unknown type \"" + type + "\"");
				}
			}
		}

		/**
		 * Create an instance, filled with data from the given map
		 * @param map map
		 * @throws MethodInvocationException
		 */
		public ResizeInfo(Map<Object, Object> map) throws NodeException {
			this(
				ObjectTransformer.getInt(map.get(WIDTH_ARG), -1), 
				ObjectTransformer.getInt(map.get(HEIGHT_ARG), -1), 
				ObjectTransformer.getString(map.get(MODE_ARG), null), 
				ObjectTransformer.getString(map.get(TYPE_ARG), null)
			);
		}

		public Mode getMode() {
			return mode;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public Type getType() {
			return type;
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

		CropInfo(int x, int y, int width, int height) throws NodeException {
			if (x < 0) {
				throw new NodeException("Error while rendering gis url: x-coordinate for cropping must not be negative (was " + x + ")");
			}
			this.x = x;
			if (y < 0) {
				throw new NodeException("Error while rendering gis url: y-coordinate for cropping must not be negative (was " + y + ")");
			}
			this.y = y;
			if (width <= 0) {
				throw new NodeException("Error while rendering gis url: cropping width must be a positive integer");
			}
			this.width = width;
			if (height <= 0) {
				throw new NodeException("Error while rendering gis url: cropping height must be a positive integer");
			}
			this.height = height;
		}

		/**
		 * Create an instance, filled with data from the given map
		 * @param map map with crop data
		 * @throws NodeException
		 */
		public CropInfo(Map<Object, Object> map) throws NodeException {
			this(
				ObjectTransformer.getInt(map.get(X_ARG), 0),
				ObjectTransformer.getInt(map.get(Y_ARG), 0),
				ObjectTransformer.getInt(map.get(WIDTH_ARG), 0),
				ObjectTransformer.getInt(map.get(HEIGHT_ARG), 0)
			);
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
	}

	/**
	 * Resize modes
	 */
	public enum Mode {
		prop, force, smart, fpsmart
	}

	/**
	 * Render types
	 */
	public enum Type {
		url, phpwidget
	}
}
