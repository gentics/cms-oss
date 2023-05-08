package com.gentics.lib.db;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for a restore processor, which will be called before and after rows are restored.
 */
public interface TableVersionRestoreProcessor {
	/**
	 * This method is called before records are restored
	 * @param tableVersion table version instance
	 * @param data result processor containing the id's of the records which will be restored
	 * @throws NodeException
	 */
	void preRestore(TableVersion tableVersion, SimpleResultProcessor data) throws NodeException;

	/**
	 * This method will be called after records are restored
	 * @param tableVersion table version instance
	 * @param data result processor containing the id's of the records which will be restored
	 * @throws NodeException
	 */
	void postRestore(TableVersion tableVersion, SimpleResultProcessor data) throws NodeException;
}
