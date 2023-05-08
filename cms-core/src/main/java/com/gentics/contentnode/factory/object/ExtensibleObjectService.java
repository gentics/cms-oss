package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for services that extend extensible {@link NodeObject}s.
 * Implementations can be added to enhance factory methods
 */
public interface ExtensibleObjectService<T extends NodeObject> {
	/**
	 * Called, when the object is saved
	 * @param object saved object
	 * @param isNew true when the object was created
	 * @param contentModified true when the content of the object also changed (only for objects, which have a "content")
	 * @param userId ID of the user saving the object
	 * @throws NodeException if saving the object must fail
	 */
	void onSave(T object, boolean isNew, boolean contentModified, int userId) throws NodeException;

	/**
	 * Called, when the object is deleted (or put into the wastebin)
	 * @param object deleted object
	 * @param wastebin true when the object was put into the wastebin
	 * @param userId ID of the user deleting the object
	 * @throws NodeException if deleting the object must fail
	 */
	void onDelete(T object, boolean wastebin, int userId) throws NodeException;
}
