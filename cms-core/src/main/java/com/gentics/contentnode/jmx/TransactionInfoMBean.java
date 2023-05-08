package com.gentics.contentnode.jmx;

/**
 * Management Bean Interface for Transaction Info
 */
public interface TransactionInfoMBean {
	/**
	 * Total number of created transactions
	 * @return total transactions
	 */
	long getTotal();

	/**
	 * Number of currently open transactions
	 * @return open transactions
	 */
	int getOpen();

	/**
	 * Maximum number of transactions open at the same time
	 * @return maximum open transactions
	 */
	int getMaxOpen();
}
