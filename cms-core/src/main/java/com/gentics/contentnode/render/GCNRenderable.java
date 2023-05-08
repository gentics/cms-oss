package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.render.Renderable;

public interface GCNRenderable extends Renderable {

	/**
	 * Render the current object to a string
	 * 
	 * @param renderResult
	 *            container for return-messages.
	 * @return the rendered code.
	 * @throws NodeException
	 *             when the object could not be rendered
	 */
	String render(RenderResult renderResult) throws NodeException;

}
