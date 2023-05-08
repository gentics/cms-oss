package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;

/**
 * Autoclosable implementation that will commit or roll back the current transaction on {{@link #close()}}
 */
public class AutoCommit implements AutoCloseable {
	/**
	 * Transaction to commit/roll back
	 */
	private Transaction t;

	/**
	 * Flag whether the transaction shall be stopped also
	 */
	private boolean stop = false;

	/**
	 * Flag for marking success
	 */
	private boolean success = false;

	/**
	 * Create an instance, that will commit/roll back but not stop
	 * @throws NodeException
	 */
	public AutoCommit() throws NodeException {
		this(false);
	}

	/**
	 * Create an instance, that will commit/roll back
	 * @param stop true to stop the transaction, false to keep it running
	 * @throws NodeException
	 */
	public AutoCommit(boolean stop) throws NodeException {
		t = TransactionManager.getCurrentTransaction();
		this.stop = stop;
	}

	/**
	 * Set success. The transaction will be committed, if this method has been called, or rolled back otherwise
	 */
	public void success() {
		this.success = true;
	}

	@Override
	public void close() throws NodeException {
		if (success) {
			t.commit(stop);
		} else {
			t.rollback(stop);
		}
	}
}
