/*
 * @author floriangutmann
 * @date Nov 18, 2009
 * @version $Id: Transactional.java,v 1.3 2010-10-19 11:20:01 norbert Exp $
 */
package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.TransactionalTriggerEvent;

/**
 * A Transactional can be attachted to a {@link Transaction} and provides some methods that are called during the commit process.<br />
 * All Transactionals are removed if it Transaction is stopped.
 * 
 * @author floriangutmann
 */
public interface Transactional {

	/**
	 * This method is called before the database connection is commited. If a NodeException is thrown, the transaction will be rolled back.
	 * 
	 * @param t
	 *            A reference to the calling transaction.
	 */
	void onDBCommit(Transaction t) throws NodeException;

	/**
	 * This method is called after the database connection is committed successfully.<br />
	 * Be aware that the database connection has already been committed at the time this function is called. If anything is written (insert, update, delete) into the DB,
	 * this method must return true, so that the db connection is committed again.
	 * 
	 * @param t
	 *            A reference to the calling transcation.
	 * @return true when the db connection needs to be committed again, false if not
	 */
	boolean onTransactionCommit(Transaction t);

	/**
	 * This method will return the threshold for the usage of the {@link #getSingleton(Transaction)} Transactional. The singleton will be used instead of the already
	 * listed transactions when the threshold is exceeded. Refer to {@link TransactionManager} for more infomation.
	 * 
	 * @param t
	 * @return
	 */
	int getThreshold(Transaction t);

	/**
	 * This method will return a transactional that can substitute already listed Transactionals within the {@link TransactionManager}. Examples for this can be found in
	 * the {@link TransactionalTriggerEvent} class.
	 * 
	 * @param t
	 * @return
	 */
	Transactional getSingleton(Transaction t);

	/**
	 * This method is called before the transaction is rolled back
	 * @param t transaction
	 */
	void onTransactionRollback(Transaction t);

	/**
	 * This method is called after the transaction has been rolled back
	 * @param t transaction
	 */
	default void afterTransactionRollback(Transaction t) {
	}
}
