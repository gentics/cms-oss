package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.search.Indexer;

/**
 * Transactional for updating node IDs in the indices for objects in the given node
 */
public class UpdateNodeIdsInIndexTransactional extends AbstractTransactional {
	/**
	 * Node ID
	 */
	protected int nodeId;

	/**
	 * Create an instance
	 * @param nodeId node ID
	 */
	public UpdateNodeIdsInIndexTransactional(int nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public void onDBCommit(Transaction t) throws NodeException {
	}

	@Override
	public boolean onTransactionCommit(Transaction t) {
		Indexer.updateNodeIds(nodeId);
		return false;
	}
}
