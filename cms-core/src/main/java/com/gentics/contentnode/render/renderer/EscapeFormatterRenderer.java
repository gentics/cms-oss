/*
 * @author: Stefan Hepp
 * @date: 05.02.2006
 * @version: $Id: EscapeFormatterRenderer.java,v 1.5 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.contentnode.render.renderer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.lib.etc.StringUtils;

/**
 * This is a special templaterenderer which can be used to escape code.
 */
public class EscapeFormatterRenderer implements TemplateRenderer {

	private static EscapeFormatterRenderer renderer = null;

	public EscapeFormatterRenderer() {}

	/**
	 * escape the template code.
	 *
	 * @param renderResult the current renderResult.
	 * @param template the code to be escaped.
	 * @return the escaped code.
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		// it is important to first replace the backslashes, since we use backslashes as escape characters
		return template.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'").replaceAll("\r?\n", "\\\\n").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
				"\r", "\\\\n");
	}
}
