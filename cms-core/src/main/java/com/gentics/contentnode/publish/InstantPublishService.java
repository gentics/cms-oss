package com.gentics.contentnode.publish;

/**
 * Interface for implementations which need to handle successful or failed instant publishing operations
 */
public interface InstantPublishService {
	/**
	 * Called when instant publishing of an object succeeded
	 * @param objType object type
	 * @param objId object ID
	 */
	void succeeded(int objType, int objId);

	/**
	 * Called when instant publishing of an object failed
	 * @param objType object type
	 * @param objId object ID
	 */
	void failed(int objType, int objId);
}
