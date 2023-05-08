package com.gentics.contentnode.factory;

/**
 * AutoClosable that enables/disables the level2 Cache of the current transaction
 */
public class Level2CacheTrx implements AutoCloseable {
	/**
	 * Transaction
	 */
	private Transaction t;

	/**
	 * Flag to reset to
	 */
	private boolean level2Cache;

	/**
	 * Enable/disable the level2 Cache of the current transaction
	 * @param enable true to enable, false to disable
	 * @throws TransactionException
	 */
	public Level2CacheTrx(boolean enable) throws TransactionException {
		t = TransactionManager.getCurrentTransaction();
		level2Cache = t.enableLevel2Cache(enable);
	}

	@Override
	public void close() {
		// reset to the old value
		t.enableLevel2Cache(level2Cache);
	}
}
