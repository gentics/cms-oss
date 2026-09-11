package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.exception.NodeException;
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
			// we do this by getting the real edit mode, which will also take into consideration, whether
			// we are rendering a foreign object or not (not edit mode for foreign objects)
			int editMode = RenderUtils.getRealEditMode(renderType);

			currentEditMode = renderType.getEditMode();

			if (editMode != currentEditMode) {
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
