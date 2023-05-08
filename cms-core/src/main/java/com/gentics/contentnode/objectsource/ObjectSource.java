/*
 * @author Stefan Hepp
 * @date 30.12.2005
 * @version $Id: ObjectSource.java,v 1.4 2007-01-03 12:20:16 norbert Exp $
 */
package com.gentics.contentnode.objectsource;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.render.RenderResult;

import java.util.List;

/**
 * An objectsource can retrieve a list of objects and translate these objects using a given template.
 */
public interface ObjectSource {

	/**
	 * Get a list of all objects in this objectsource.
	 * @return a list of NodeObjects of the objects in this objectsource.
	 * @throws NodeException 
	 */
	List<? extends NodeObject> getSelectedObjects() throws NodeException;

	/**
	 * translate the objects using a template per object.
	 * @param renderResult the current renderresult.
	 * @param objects the list of objects to translate, usually the result of {@link #getSelectedObjects()}.
	 * @param template the template to use per object.
	 *
	 * @return the merged code of all translated objects.
	 * @throws NodeException
	 */
	String translate(RenderResult renderResult, List<? extends NodeObject> objects, String template) throws NodeException;
    
}
