package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.PartType;

/**
 * Form Directive
 */
public class FormDirective extends Directive {
	/**
	 * First argument must be the form
	 */
	public final static int ARGS_FORM = 0;

	/**
	 * Second argument must be the options
	 */
	public final static int ARGS_OPTIONS = 1;

	@Override
	public String getName() {
		return "gtx_form";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
		try {
			Form form = getForm(context, node);
			FormRendering.Options options = RenderUtils.getVtlDirectiveObject(FormRendering.Options.class, context, node, ARGS_OPTIONS, FormRendering.Options::new);

			return FormRendering.render(form, options, writer);
		} catch (NodeException e) {
			throw new MethodInvocationException("Error while rendering object", e, getName(), context.getCurrentTemplateName(), node.jjtGetChild(0)
					.getLine(), node.jjtGetChild(0).getColumn());
		}
	}

	/**
	 * Get the form to render
	 * @param node node
	 * @return form or null, if not found
	 * @throws NodeException
	 */
	protected Form getForm(InternalContextAdapter context, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Object formChild = unwrapRenderableResolvable(node.jjtGetChild(ARGS_FORM).value(context));
		if (formChild instanceof Form) {
			return (Form) formChild;
		}
		if (formChild instanceof Value) {
			PartType partType = ((Value)formChild).getPartType();
			if (partType instanceof CmsFormPartType) {
				return ((CmsFormPartType)partType).getTarget();
			}
		}
		if (formChild != null) {
			return t.getObject(Form.class, ObjectTransformer.getInt(formChild, 0));
		}
		return null;
	}

	/**
	 * Unwrap instances of {@link RenderableResolvable} (recursively)
	 * @param object object to unwrap
	 * @return unwrapped object
	 */
	protected Object unwrapRenderableResolvable(Object object) {
		if (object instanceof RenderableResolvable) {
			return unwrapRenderableResolvable(((RenderableResolvable)object).getWrappedObject());
		} else {
			return object;
		}
	}
}
