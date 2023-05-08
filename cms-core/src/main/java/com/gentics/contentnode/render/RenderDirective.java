package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.ParserTreeConstants;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.parser.tag.struct.ParseStructRenderer;
import com.gentics.contentnode.render.RenderableResolvable.Scope;
import com.gentics.lib.render.Renderable;

/**
 * Velocity Directive for rendering objects.
 * If a string is given, the system will try to resolve the string in the current scope and handle the resolved object afterwards.
 * For objects given to the directive that implement {@link Renderable},
 * the method {@link Renderable#render(com.gentics.contentnode.render.RenderResult)} will be called. Otherwise the method {@link #toString()} will be called.
 */
public class RenderDirective extends Directive {

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#getName()
	 */
	@Override
	public String getName() {
		return "gtx_render";
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
		if (argCount == 0) {
			throw new TemplateInitException("#" + getName() + "() requires exactly one argument", context.getCurrentTemplateName(), node.getColumn(),
					node.getLine());
		}
		if (argCount > 1) {
			// use line/col of second argument
			throw new TemplateInitException("#" + getName() + "() requires exactly one argument", context.getCurrentTemplateName(), node.jjtGetChild(1)
					.getColumn(), node.jjtGetChild(1).getLine());
		}

		Node childNode = node.jjtGetChild(0);
		if (childNode.getType() != ParserTreeConstants.JJTREFERENCE && childNode.getType() != ParserTreeConstants.JJTSTRINGLITERAL) {
			throw new TemplateInitException("#" + getName() + "()  argument must be a reference or a string", context.getCurrentTemplateName(),
					childNode.getColumn(), childNode.getLine());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.directive.Directive#render(org.apache.velocity.context.InternalContextAdapter, java.io.Writer, org.apache.velocity.runtime.parser.node.Node)
	 */
	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			RenderResult renderResult = t.getRenderResult();
			Object value = node.jjtGetChild(0).value(context);
			if (value instanceof String) {
				value = renderType.getStack().resolve((String)value);
			}
			if (value instanceof RenderableResolvable) {
				RenderableResolvable renderable = (RenderableResolvable)value;
				if (renderable.getWrappedObject() instanceof Tag) {
					try (Scope scope = renderable.scope()) {
						writer.write(renderTag((Tag)renderable.getWrappedObject(), renderType, renderResult));
					}
				} else {
					writer.write(renderable.toString());
				}
			} else if (value instanceof Tag) {
				writer.write(renderTag((Tag)value, renderType, renderResult));
			} else if (value instanceof GCNRenderable) {
				writer.write(((GCNRenderable) value).render(renderResult));
			} else if (value != null) {
				writer.write(value.toString());
			}
			return true;
		} catch (NodeException e) {
			throw new MethodInvocationException("Error while rendering object", e, getName(), context.getCurrentTemplateName(), node.jjtGetChild(0)
					.getLine(), node.jjtGetChild(0).getColumn());
		}
	}

	/**
	 * Render the given tag in the current edit mode
	 * @param tag tag to render
	 * @param renderType rendertype
	 * @param result render result
	 * @return rendered tag
	 * @throws NodeException
	 */
	protected String renderTag(Tag tag, RenderType renderType, RenderResult result) throws NodeException {
		int editMode = renderType.getEditMode();

		if ((editMode == RenderType.EM_EDIT || editMode == RenderType.EM_ALOHA) && tag.isEditable()) {
			StringBuffer source = new StringBuffer();
			List<ParserTag> omitTags = new ArrayList<ParserTag>();
			List<ParserTag> omitTagsEdit = new ArrayList<ParserTag>();
			ParseStructRenderer.renderEditableTag(source, tag.render(result), tag, omitTags, omitTagsEdit, result);
			return source.toString();
		} else if (editMode == RenderType.EM_ALOHA_READONLY) {
			AlohaRenderer alohaRenderer = (AlohaRenderer) RendererFactory.getRenderer(ContentRenderer.RENDERER_ALOHA);
			return alohaRenderer.block(tag.render(result), tag, result);
		} else {
			return tag.render(result);
}
	}
}
