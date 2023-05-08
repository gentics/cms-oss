/*
 * @author Stefan Hepp
 * @date 31.01.2006
 * @version $Id: BatchObjectFactory.java,v 1.6 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * An extended factory which can request more than one object per time.
 */
public interface BatchObjectFactory extends ObjectFactory {

	/**
	 * load a list of objects instead of only one. This allowes optimized data-retrieval. 
	 * @param clazz
	 * @param ids list of Integers of all ids to load
	 * @param info info about the type of objects to load
	 * @throws TransactionException TODO
	 *
	 * @see ObjectFactory#loadObject(Class, Object, NodeObjectInfo)
	 * 
	 * @return list of nodeobjects
	 * @throws NodeException 
	 */
	<T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException;

	/**
	 * Load the versioned data for the given class of objects and put into the cache.
	 * The version timestamps are defined per main object ID (main objects are specified by the given mainClass)
	 * 
	 * Example: When preparing the contenttags by content_id, the mainClazz would by Content.class and the keys of the timestamps map would be the content_id's.
	 * @param handle factory handle
	 * @param clazz class of the objects to prepare
	 * @param mainClazz class of the main objects (id's will be the keys of the timestamps map)
	 * @param timestamps map specifying the main objects, for which the data has to be prepared and the timestamps for each main object
	 * @return the set of prepared versioned objects
	 * @throws NodeException
	 */
	<T extends NodeObject> Set<T> batchLoadVersionedObjects(FactoryHandle handle, Class<T> clazz, Class<? extends NodeObject> mainClazz,
			Map<Integer, Integer> timestamps) throws NodeException;
}
