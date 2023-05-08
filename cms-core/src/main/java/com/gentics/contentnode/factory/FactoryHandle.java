/*
 * @author Stefan Hepp
 * @date 12.12.2005
 * @version $Id: FactoryHandle.java,v 1.13.2.2.2.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * Factory handle given to transaction objects. Instances of this interface
 * allow privileged access to the NodeFactory.
 */
public interface FactoryHandle {

	/**
	 * get the current factory of this handle.
	 * TODO make this transaction-save.
	 * @return the current factory.
	 */
	NodeFactory getFactory();

	/**
	 * add an object into the cache.
	 *
	 * @param clazz class under which the the object is to be put in the cache.
	 * @param obj the object to put.
	 * @param versionTimestamp version timestamp
	 */
	void putObject(Class<? extends NodeObject> clazz, NodeObject obj, int versionTimestamp) throws NodeException;

	/**
	 * Get the object factory for a specific class.
	 * @param clazz Class for which the ObjectFactory should be returned
	 * @return The specific ObjectFactory
	 */
	ObjectFactory getObjectFactory(Class<? extends NodeObject> clazz);

	/**
	 * Removes all deleteLists for the given transaction
	 */
	void removeDeleteLists(Transaction t);
    
	/**
	 * Flushes all ObjectFactories so that all cached operations are performed
	 * @throws NodeException if an internal error occurs
	 */
	void flushAll() throws NodeException;

	/**
	 * Get an object from the nodefactory
	 * 
	 * @param clazz class of the object
	 * @param id id of the object
	 * @param forUpdate indicates whether the object should be loaded for update or not
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param multichannelFallback true for multichannel fallback, false if not
	 * @param logErrorIfNotFound if true, a warning message is sent to the logs if the object was not found
	 * @return the object or null if the object was not found
	 * @throws ReadOnlyException if the object was requested for update but is only available for reading
	 * @throws NodeException if an error occurred while loading the object
	 */
	<T extends NodeObject> T getObject(Class<T> clazz, Integer id, boolean forUpdate, int versionTimestamp, boolean multichannelFallback, boolean logErrorIfNotFound) throws NodeException, ReadOnlyException;

	/**
	 * Get a bunch of objects from the factory.
	 * @param clazz class of the objects
	 * @param ids list of ids of the objects
	 * @param forUpdate true when the objects shall be fetched for update, false if not
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param allowMultichannellingFallback if true, multichannelling fallback is allowed
	 * @return list of objects
	 * @throws NodeException when an internal error occurred
	 * @throws ReadOnlyException when the objects should be fetched for update, but at least one object is only available Readonly
	 */
	<T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate, int versionTimestamp, boolean allowMultichannellingFallback) throws NodeException, ReadOnlyException;

	/**
	 * create a new (editable) object from the factory.
	 *
	 * @param clazz class of the object.
	 * @return the new object.
	 * @throws NodeException in case of errors
	 */
	<T extends NodeObject> T createObject(Class<T> clazz) throws NodeException;

	/**
	 * create a new objectinfo object.
	 *
	 * @param clazz class of the object.
	 * @param editable if the object is editable.
	 * @return new objectinfo object.
	 */
	NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, boolean editable);

	/**
	 * Create a new objectinfo object for a non-editable object.
	 * @param clazz class of the object
	 * @param versionTimestamp version timestamp of the object. -1 for the current version
	 * @return new objectinfo object.
	 */
	NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, int versionTimestamp);

	/**
	 * Get the class for a given ttype.
	 * @param objType the ttype the find the correspondig class to.
	 * @return the mapped class for this ttype, or null if the ttype is unknown.
	 */
	Class<? extends NodeObject> getClass(int objType);

	/**
	 * Get the class of objects stored in the given table
	 * @param tableName name of the table
	 * @return class of objects
	 */
	Class<? extends NodeObject> getClass(String tableName);

	/**
	 * Try to get the TType of a given objectclass.
	 * @param clazz class of the object.
	 * @return the corresponding ttype, or 0 if no ttype is mapped to this class.
	 */
	int getTType(Class<? extends NodeObject> clazz);

	/**
	 * Dirt the object cache for the given object
	 * @param clazz object class
	 * @param id object id
	 * @throws NodeException
	 */
	void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id) throws NodeException;

	/**
	 * Get the field data from the given object
	 * @param object object
	 * @return map of field data
	 * @throws NodeException
	 */
	Map<String, Object> getFieldData(NodeObject object) throws NodeException;

	/**
	 * Set the field data to the given object
	 * @param object object
	 * @param dataMap data map
	 * @throws NodeException
	 */
	void setFieldData(NodeObject object, Map<String, Object> dataMap) throws NodeException;

	/**
	 * Get the table name into which objects of the given class are stored
	 * @param clazz class
	 * @return table name
	 */
	String getTable(Class<? extends NodeObject> clazz);
}
