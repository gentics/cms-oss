package com.gentics.contentnode.render.renderer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * EchoRenderer simply returns the template itself
 */
public class EchoRenderer implements TemplateRenderer {
	@Override
	public String render(RenderResult renderResult, String template) throws NodeException {
		return template;
	}
}
