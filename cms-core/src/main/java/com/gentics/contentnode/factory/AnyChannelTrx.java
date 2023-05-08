package com.gentics.contentnode.factory;

/**
 * Autoclosable which sets the flag to check permissions in ANY channel to the transaction
 */
public class AnyChannelTrx implements AutoCloseable {
	/**
	 * Transaction, which was modified
	 */
	private Transaction tx;

	/**
	 * Old flag value
	 */
	private boolean oldFlag;

	/**
	 * Set the flag to true and store the old flag value
	 * @throws TransactionException
	 */
	public AnyChannelTrx() throws TransactionException {
		tx = TransactionManager.getCurrentTransaction();
		oldFlag = tx.isCheckAnyChannel();
		tx.setCheckAnyChannel(true);
	}

	@Override
	public void close() {
		tx.setCheckAnyChannel(oldFlag);
	}
}
