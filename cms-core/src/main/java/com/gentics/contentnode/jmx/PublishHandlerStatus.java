package com.gentics.contentnode.jmx;

/**
 * Stati of a publish handler
 */
public enum PublishHandlerStatus {
	IDLE, INIT, OPEN, CREATEOBJECT, UPDATEOBJECT, DELETEOBJECT, COMMIT, ROLLBACK, CLOSE, DESTROY
}
