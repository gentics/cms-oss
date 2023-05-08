/*
 * @author norbert
 * @date 26.02.2007
 * @version $Id: NBSPFormatterRenderer.java,v 1.1 2007-02-26 11:04:40 norbert Exp $
 */
package com.gentics.contentnode.render.renderer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.lib.etc.StringUtils;

/**
 * TemplateRenderer implementation for the :nbsp formatting of tags (converts empty strings into &nbsp;)
 */
public class NBSPFormatterRenderer implements TemplateRenderer {
	private final static String NBSP = "&nbsp;";

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		return StringUtils.isEmpty(template) ? NBSP : template;
	}
}
