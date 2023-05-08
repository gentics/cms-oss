package com.gentics.contentnode.factory;

/**
 * AutoClosable for setting the multichannelling disabled flag on the current transaction and resets it on close.
 * @author escitalopram
 *
 */
public class NoMcTrx implements AutoCloseable {

	private Transaction tx;

	/**
	 * Sets the multichannelling disabled flag.
	 * @throws TransactionException
	 */
	public NoMcTrx() throws TransactionException {
		this(true);
	}

	/**
	 * Sets the multichannelling disabled flag to the specified value
	 * @param new value for the multichannelling disabled flag
	 * @throws TransactionException
	 */
	public NoMcTrx(boolean flag) throws TransactionException {
		tx = TransactionManager.getCurrentTransaction();
		tx.setDisableMultichannellingFlag(flag);
	}

	@Override
	public void close() {
		tx.resetDisableMultichannellingFlag();
	}

}
