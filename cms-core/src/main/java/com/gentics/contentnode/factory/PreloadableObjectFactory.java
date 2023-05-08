/*
 * @author Stefan Hepp
 * @date 12.12.2005
 * @version $Id: PreloadableObjectFactory.java,v 1.5 2006-02-02 01:07:17 stefan Exp $
 */
package com.gentics.contentnode.factory;

import java.util.Collection;

import com.gentics.contentnode.object.NodeObject;

/**
 * This is a special factory interface which can preload objects into the cache before a given object is loaded.
 */
public interface PreloadableObjectFactory {

	/**
	 * get list of all classes of objects which may trigger a preload-request for this factory.
	 *
	 * @return list of classes of all classes for which preloading supported.
	 */
	Class<? extends NodeObject>[] getPreloadTriggerClasses();

	/**
	 * Preload all objects which can be preloaded by this factory for a given object.
	 * The given Object may not yet be initialized and available, so don't request it from the factory.
	 *
	 * @param handle FactoryHandle for putting in preloaded objects
	 * @param objClass the triggering object's class
	 * @param objId the triggering object's id
	 */
	void preload(FactoryHandle handle, Class<? extends NodeObject> objClass, int objId);

	/**
	 * Preload all objects which can be preloaded by this factory for a list of given objects.
	 * The given Object may not yet be initialized and available, so don't request it from the factory.
	 *
	 * @param handle FactoryHandle for putting in preloaded objects
	 * @param objClass the triggering object's class
	 * @param ids list of Integers of the triggering object's ids
	 */
	void preload(FactoryHandle handle, Class<? extends NodeObject> objClass, Collection<? extends Object> ids);
}
