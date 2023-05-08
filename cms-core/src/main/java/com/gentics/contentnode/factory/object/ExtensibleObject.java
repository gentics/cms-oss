package com.gentics.contentnode.factory.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for {@link NodeObject}s, which allow to add hooks for specific actions on them (like saving and deleting)
 *
 * @param <T> type of the object
 */
public interface ExtensibleObject<T extends NodeObject> {
	/**
	 * Get the list of {@link ExtensibleObjectService} implementations that implement the hooks
	 * @return list of services
	 */
	List<? extends ExtensibleObjectService<T>> getServices();

	/**
	 * Called, when the object is saved.
	 * The default implementation (which should not be overwritten) forwards the call to all services
	 * @param object saved object
	 * @param isNew true, when the object was created
	 * @param contentModified true, when also the content of the object was modified
	 * @param userId ID of the user saving the object
	 * @throws NodeException in cases, where saving the object must fail
	 */
	default void onSave(T object, boolean isNew, boolean contentModified, int userId) throws NodeException {
		for (ExtensibleObjectService<T> service : getServices()) {
			service.onSave(object, isNew, contentModified, userId);
		}
	}

	/**
	 * Called, when the object is deleted.
	 * The default implementation (which should not be overwritten) forwards the call to all services
	 * @param object deleted object
	 * @param wastebin true, when the object was put into the wastebin
	 * @param userId ID of the user deleting the object
	 * @throws NodeException in cases, where deleting the object must fail
	 */
	default void onDelete(T object, boolean wastebin, int userId) throws NodeException {
		for (ExtensibleObjectService<T> service : getServices()) {
			service.onDelete(object, wastebin, userId);
		}
	}
}
