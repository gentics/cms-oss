/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: TemplateRenderer.java,v 1.3 2007-01-03 12:20:16 norbert Exp $
 */
package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;

/**
 * an interface to merge a template with variables, given by resolvable objects.
 * will parse template and look for variables. the variables are resolved.
 * resolved objects should implement renderable and will be transformed using
 * render(), otherwise toString() will be used.
 */
public interface TemplateRenderer {

	/**
	 * renders the template with the given renderType. unresolvable
	 * rendervariables will be rendered to empty strings (depending on renderType).
	 *        usually contains Resolvable objects, mostly Renderable.
	 * @param renderResult contains result values.
	 * @param template the template with variables.
	 * @return the rendered template.
	 */
	String render(RenderResult renderResult, String template) throws NodeException;

}
