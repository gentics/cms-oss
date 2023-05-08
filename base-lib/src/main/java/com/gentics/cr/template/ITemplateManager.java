package com.gentics.cr.template;

/**
 *
 * Last changed: $Date: 2010-04-01 15:25:54 +0200 (Do, 01 Apr 2010) $
 *
 * @version $Revision: 545 $
 * @author $Author: supnig@constantinopel.at $
 *
 */
public interface ITemplateManager {

	/**
	 * Deploy and Object into the Render Context.
	 *
	 * @param key key of the object
	 * @param value object
	 */
	public void put(String key, Object value);

	/**
	 * Render the given template into a String.
	 *
	 * @param templatename name of the template to render
	 * @param templatesource template source
	 * @return rendered template as string.
	 * @throws Exception in case of rendering errors
	 */
	public String render(String templatename, String templatesource)
			throws Exception;

}
