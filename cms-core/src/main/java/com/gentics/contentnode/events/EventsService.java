package com.gentics.contentnode.events;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for implementations that add event triggers
 */
public interface EventsService {
	/**
	 * Trigger the event on the object
	 * @param object object on which the event is triggered
	 * @param eventMask event mask
	 * @param properties optional properties
	 * @throws NodeException
	 */
	void trigger(NodeObject object, int eventMask, String... properties) throws NodeException;
}
