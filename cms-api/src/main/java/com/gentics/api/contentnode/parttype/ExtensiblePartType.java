/*
 * @author norbert
 * @date 07.03.2007
 * @version $Id: ExtensiblePartType.java,v 1.2 2007-11-13 10:03:47 norbert Exp $
 */
package com.gentics.api.contentnode.parttype;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for extended parttypes
 */
public interface ExtensiblePartType {

	/**
	 * Render the parttype
	 * @return rendered content of the parttype
	 * @throws NodeException when the parttype could not be rendered
	 */
	String render() throws NodeException;

	/**
	 * Perform cleanup operations after rendering.
	 * When overwriting this method, make sure to call {@link #cleanAfterRender()}.
	 */
	void cleanAfterRender();
}
