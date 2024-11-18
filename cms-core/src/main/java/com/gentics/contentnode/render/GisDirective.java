package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

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
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.render.GisRendering.CropInfo;
import com.gentics.contentnode.render.GisRendering.ResizeInfo;

/**
 * Directive for rendering GenticsImageStore URLs
 */
public class GisDirective extends Directive {
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
			ImageFile image = getImage(context, node);
			ResizeInfo resizeInfo = getResizeInfo(context, node);
			CropInfo cropInfo = getCropInfo(context, node);

			return GisRendering.render(image, resizeInfo, cropInfo, writer);
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
}
