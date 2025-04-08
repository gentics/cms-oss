package com.gentics.contentnode.factory;

import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.InstantPublisher.Result;

/**
 * Interface for implementations that can collect instant publishing results
 */
public interface InstantPublishingResultCollector {
	/**
	 * Add the instant publishing result for the given object
	 * @param object object which was instant published (at least tried)
	 * @param result result
	 */
	void addInstantPublishingResult(NodeObject object, Result result);

	/**
	 * Get the instant publishing result for the object identified by type and id, or null if not found.
	 * @param objType object type
	 * @param objId object id
	 * @return result or null
	 */
	Result getInstantPublishingResult(int objType, int objId);
}
