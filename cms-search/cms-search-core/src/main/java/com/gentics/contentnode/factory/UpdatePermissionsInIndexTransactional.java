package com.gentics.contentnode.factory;

import java.util.Collection;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.search.Indexer;

/**
 * Transactional for updating permission info in the indices for the given folders
 */
public class UpdatePermissionsInIndexTransactional extends AbstractTransactional {
	/**
	 * Collection of folder IDs
	 */
	protected Collection<Integer> folderIds;

	/**
	 * Create an instance
	 * @param folderIds folder IDs
	 */
	public UpdatePermissionsInIndexTransactional(Collection<Integer> folderIds) {
		this.folderIds = folderIds;
	}

	@Override
	public void onDBCommit(Transaction t) throws NodeException {
	}

	@Override
	public boolean onTransactionCommit(Transaction t) {
		// change group assignments for folders in Index
		Indexer.updatePermissions(folderIds, true);
		return false;
	}
}
