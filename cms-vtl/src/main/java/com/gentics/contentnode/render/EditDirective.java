package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * Velocity Directive for rendering objects in edit mode (if the page was
 * originally rendered in edit mode). When a page is rendered in one of the edit
 * modes, velocity tags will change the rendermode temporarily to preview,
 * because in general, velocity should not be rendered in edit mode. When
 * rendering this directive, the originally set rendermode will be restored
 * while rendering the object.
 */
public class EditDirective extends RenderDirective {
	@Override
	public String getName() {
		return "gtx_edit";
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		RenderType renderType = null;
		boolean editModeSet = false;
		int currentEditMode = 0;

		try {
			// switch back to the original rendermode, if the rendermode was changed for rendering the velocity tag
			renderType = TransactionManager.getCurrentTransaction().getRenderType();
			// we do this by getting rendermde.editMode from the CMSResolver, which will also take into consideration, whether
			// we are rendering a foreign object or not (not edit mode for foreign objects)
			int editMode = Optional.ofNullable(renderType.getCMSResolver()).map(cms -> {
				try {
					return ObjectTransformer.getInt(PropertyResolver.resolve(cms, "rendermode.editMode", false), -1);
				} catch (UnknownPropertyException e) {
					return -1;
				}
			}).orElse(-1);

			currentEditMode = renderType.getEditMode();

			if (editMode > 0 && editMode != currentEditMode) {
				renderType.setEditMode(editMode);
				editModeSet = true;
			}

			return super.render(context, writer, node);
		} catch (NodeException e) {
			throw new MethodInvocationException("Error while rendering object", e, getName(), context.getCurrentTemplateName(), node.jjtGetChild(0).getLine(),
					node.jjtGetChild(0).getColumn());
		} finally {
			// if the original rendermode was restored for rendering the directive, we switch back to the rendermode,
			// that was chosen for rendering the velocity tag
			if (editModeSet) {
				renderType.setEditMode(currentEditMode);
			}
		}
	}
}
