package com.gentics.contentnode.factory.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.PublishableNodeObject;

/**
 * Interface for {@link PublishableNodeObject}s, which allow to add hooks for specific actions on them (like saving, deleting, publishing and taking offline)
 *
 * @param <T> type of the object
 */
public interface ExtensiblePublishableObject<T extends PublishableNodeObject> extends ExtensibleObject<T> {
	@Override
	List<? extends ExtensiblePublishableObjectService<T>> getServices();

	/**
	 * Called when the object is published.
	 * The default implementation (which should not be overwritten) forwards the call to all services
	 * @param object published object
	 * @param wasOnline true, when the object was online before
	 * @param userId ID of the publishing user
	 * @throws NodeException when publishing has to fail
	 */
	default void onPublish(T object, boolean wasOnline, int userId) throws NodeException {
		for (ExtensiblePublishableObjectService<T> service : getServices()) {
			service.onPublish(object, wasOnline, userId);
		}
	}

	/**
	 * Called when the object is taken offline.
	 * The default implementation (which should not be overwritten) forwards the call to all services
	 * @param object object, which is taken offline
	 * @param wasOnline true, when the object was online before
	 * @param userId ID of the user
	 * @throws NodeException when taking offline has to fail
	 */
	default void onTakeOffline(T object, boolean wasOnline, int userId) throws NodeException {
		for (ExtensiblePublishableObjectService<T> service : getServices()) {
			service.onTakeOffline(object, wasOnline, userId);
		}
	}
}
