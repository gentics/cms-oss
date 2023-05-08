/*
 * @author Stefan Hepp
 * @date 12.12.2005
 * @version $Id: VelocityRenderer.java,v 1.2 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.render.renderer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * A velocity implementation of a templaterenderer, which can parse velocity syntax in a given template
 * using the velocity engine. The resolvables in the rendertype stack are used as velocity objects.
 */
public class VelocityRenderer implements TemplateRenderer {

	public String render(RenderResult renderResult, String template) throws NodeException {
		// TODO implement.
		return template;
	}
}
