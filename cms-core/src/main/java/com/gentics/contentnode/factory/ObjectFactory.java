/*
 * @author Stefan Hepp
 * @date 25.12.2005
 * @version $Id: ObjectFactory.java,v 1.14.4.1 2011-01-31 12:43:21 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.util.Collection;
import java.util.Date;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * An objectfactory creates and loads objects.
 * TODO store objects back
 */
public interface ObjectFactory {
	/**
	 * Reloads the configuration of the ObjectFactory
	 */
	void reloadConfiguration();
    
	/**
	 * Removes the deleteList for the specified transaction.
	 * @param t The transaction for which to remove the deleteList.
	 */
	void removeDeleteList(Transaction t);
    
	/**
	 * Performs all cached operations
	 */
	void flush() throws NodeException;
    
	/**
	 * create a new (editable) object of the given class.
	 *
	 * @param handle a handle of the factory.
	 * @param clazz the type of the object to create.
	 * @return a new editable object.
	 * @throws NodeException
	 */
	<T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException;

	/**
	 * load an object from the database.
	 * @param clazz the type of the object to create
	 * @param id id of the object to load
	 * @param info info about the object to load.
	 * @return the new object, or null if it does not exist.
	 * @throws NodeException when loading the object failed
	 */
	<T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException;

	/**
	 * Get an editable copy of the given object
	 * @param object object
	 * @param info info about the object to load
	 * @return editable copy (if possible)
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	<T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException;

	/**
	 * revalidate all objects or a specific object of a given class, and reload it or clear it.
	 *
	 * @param handle
	 * @param clazz
	 * @param id the id of the object to revalidate, or 0 if all objects in the cache should be checked.
	 * @param date date of last check
	 * @return true if objects have been checked, else false.
	 */
	boolean revalidate(FactoryHandle handle, Class<? extends NodeObject> clazz, int id, Date date);

	/**
	 * store an object back to the database.
	 *
	 * @param handle a handle to the factory.
	 * @param clazz the class of the object.
	 * @param object the object to be stored.
	 */
	<T extends NodeObject> void store(FactoryHandle handle, Class<T> clazz, T object);

	int getDeleteListsSize(Transaction t);

	/**
	 * If the feature "multichannelling" is activated, we probably need to substitute
	 * the given object to be the local copy of master object.
	 * @param object original object
	 * @return corrected object
	 * @throws NodeException if something goes wrong
	 */
	<T extends NodeObject> T doMultichannellingFallback(T object) throws NodeException;

	/**
	 * Get the effective udate for the dicuser entries of the given output user id
	 * @param outputUserId output user id
	 * @return maximum udate
	 * @throws NodeException
	 */
	int getEffectiveOutputUserUdate(int outputUserId) throws NodeException;

	/*
	 * Check whether versioning is supported for instances of the class
	 * @param clazz class
	 * @return true if versioning is supported, false if not
	 */
	boolean isVersioningSupported(Class<? extends NodeObject> clazz);

	/**
	 * Update the non versioned data in the versionedObject to the current version
	 * @param versionedObject versioned object
	 * @param currentVersion current version
	 * @throws NodeException
	 */
	void updateNonVersionedData(NodeObject versionedObject, NodeObject currentVersion) throws NodeException;

	/**
	 * Check if an object is in the deleted list
	 * @param clazz	the class for which the delete list is searched
	 * @param obj	the object to check for
	 * @return	true iff obj is in the deleted list
	 * @throws TransactionException
	 */
	public boolean isInDeletedList(Class<? extends NodeObject> clazz, NodeObject obj) throws TransactionException;

	/**
	 * Check if an object with given id is in the deleted list
	 * @param clazz	the class for which the delete list is searched
	 * @param id id of the object to check for
	 * @return true iff an object with the given id is in the deleted list
	 * @throws NodeException
	 */
	public boolean isInDeletedList(Class<? extends NodeObject> clazz, Integer id) throws NodeException;

	/**
	 * Remove the IDs of deleted objects from the modifiable collection id IDs
	 * @param clazz object class
	 * @param ids modifiable ID collection
	 * @throws NodeException
	 */
	public void removeDeleted(Class<? extends NodeObject> clazz, Collection<Integer> ids) throws NodeException;

	/**
	 * Get the IDs of objects in the delete list
	 * @param clazz class of objects
	 * @return collection containing the IDs
	 * @throws NodeException
	 */
	public Collection<Integer> getDeletedIds(Class<? extends NodeObject> clazz) throws NodeException;

	/**
	 * Optional initialization of the factory
	 * @throws NodeException
	 */
	default void initialize() throws NodeException {
	}
}
