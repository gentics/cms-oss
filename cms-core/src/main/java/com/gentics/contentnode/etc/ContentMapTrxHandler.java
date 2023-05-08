package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentMap.ContentMapTrx;

/**
 * Interface for Handler implementations.
 * The {@link #handle()} method of the instance passed to a {@link ContentMapTrx} will be called after the transaction has been successfully committed.
 */
public interface ContentMapTrxHandler {
	/**
	 * Handle method. This method will be called, after the transaction has been successfully committed.
	 * @throws NodeException
	 */
	void handle() throws NodeException;
}
