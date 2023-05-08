package com.gentics.contentnode.factory;

/**
 * Abstract implementation for a transactional. New transactionals can extend this class and if needed overwrite the default implementation.
 * 
 * @author johannes2
 * 
 */
public abstract class AbstractTransactional implements Transactional {

	/**
	 * By default the threshold is -1 and thus any handling will of singleton implementations be omitted within the {@link TransactionManager}
	 */
	public int getThreshold(Transaction t) {
		return -1;
	}

	/**
	 * By default all transactionals that use this abstract will not provide any singleton. Overwrite this method if you want to provide a singleton that can substitute
	 * for existing transactionals that were already added in the {@link TransactionManager}
	 */
	public Transactional getSingleton(Transaction t) {
		return null;
	}

	@Override
	public void onTransactionRollback(Transaction t) {
	}
}
