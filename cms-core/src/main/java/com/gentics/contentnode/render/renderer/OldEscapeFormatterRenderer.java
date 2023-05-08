/*
 * @author: Stefan Hepp
 * @date: 05.02.2006
 * @version: $Id: OldEscapeFormatterRenderer.java,v 1.2 2008-11-10 10:54:30 norbert Exp $
 */
package com.gentics.contentnode.render.renderer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * This is a special templaterenderer which can be used to escape code in the old php style.
 */
public class OldEscapeFormatterRenderer implements TemplateRenderer {

	/**
	 * escape the template code.
	 *
	 * @param renderResult the current renderResult.
	 * @param template the code to be escaped.
	 * @return the escaped code.
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		// it is important to first replace the backslashes, since we use backslashes as escape characters
		return template.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'");
	}
}
