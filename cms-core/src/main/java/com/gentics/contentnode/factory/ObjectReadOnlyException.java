package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Exception that is thrown, when a readonly object should be modified
 */
public class ObjectReadOnlyException extends ReadOnlyException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2622887591260753930L;

	/**
	 * Create an instance
	 * @param object object
	 */
	public ObjectReadOnlyException(NodeObject object) {
		super("Object instance {" + object + "} is readonly and cannot be modified");
	}
}
