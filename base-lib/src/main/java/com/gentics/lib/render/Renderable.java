/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: Renderable.java,v 1.3 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.lib.render;

import com.gentics.api.lib.exception.NodeException;

/**
 * Objects which implement this interface can render itself. Rendering is done
 * regarding a given renderType. Messages are added to the renderResult object.
 *
 * @see TemplateRenderer
 */
public interface Renderable {

	/**
	 * Render the current object to a string
	 * @return the rendered code.
	 * @throws NodeException when the object could not be rendered
	 */
	String render() throws NodeException;
}
