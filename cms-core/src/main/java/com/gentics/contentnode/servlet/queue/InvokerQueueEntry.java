/*
 * @author herbert
 * @date Feb 8, 2008
 * @version $Id: InvokerQueueEntry.java,v 1.2 2008-03-05 10:49:52 norbert Exp $
 */
package com.gentics.contentnode.servlet.queue;

import com.gentics.contentnode.servlet.CNInvokerQueue;

/**
 * Interface of a "invoker queue entry" for the 
 * {@link CNInvokerQueue} which represents any item which can
 * be executed (synchronized).
 * 
 * Every queue entry consists of one ID parameter which uniquely
 * identifies this entry.
 * 
 * Classes implementing this interface must have a default 
 * constructor !
 * 
 * 
 * 
 * This interface represents:
 * 1.) One queue entry
 * 2.) a type of queue entry (e.g. 'nodecopy' - which is 
 * responsible for unserializing queue entries from the db.)
 * 
 * @author herbert
 */
public interface InvokerQueueEntry {
    
	/**
	 * Should return the type (e.g. 'nodecopy', 'export dryrun', 'export')
	 * @return
	 */
	public String getType();
    
	public String getIdParameter();
    
	public String getAdditionalParameters();
    
	/**
	 * Allows setting of an additional parameter from the outside.
	 */
	public void setParameter(String key, String value);

	/**
	 * Get the parameter with given key
	 * @param key key of the parameter
	 * @return value of the parameter
	 */
	public String getParameter(String key);

	public InvokerQueueEntry createQueueEntry(String type, String idParameter, String additionalParameters);

	public void invoke();
    
	/**
	 * Should return the progress in percent.
	 * @return progress between 0 and 100 / -1 if not implemented.
	 */
	public int getProgress();

	/**
	 * Do some cleanup after invoking the queue entry. This method will be
	 * called after call to {@link #invoke()}, and after the transaction has
	 * been committed.
	 */
	public void cleanUp();
}
