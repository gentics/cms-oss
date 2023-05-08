package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.PublishableNodeObject;

/**
 * Interface for services that extend extensible {@link PublishableNodeObject}s.
 * Implementations can be added to enhance factory methods
 */
public interface ExtensiblePublishableObjectService<T extends PublishableNodeObject> extends ExtensibleObjectService<T> {
	/**
	 * Called when the object is published
	 * @param object published object
	 * @param wasOnline true when the object was online before
	 * @param userId ID of the publisher
	 * @throws NodeException if publishing has to fail
	 */
	void onPublish(T object, boolean wasOnline, int userId) throws NodeException;

	/**
	 * Called when the object is taken offline
	 * @param object object which is taken offline
	 * @param wasOnline true when the object was online before
	 * @param userId ID of the user taking the object offline
	 * @throws NodeException if taking offline has to fail
	 */
	void onTakeOffline(T object, boolean wasOnline, int userId) throws NodeException;
}
