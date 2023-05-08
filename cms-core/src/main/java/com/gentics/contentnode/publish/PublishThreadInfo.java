/*
 * @author jan
 * @date Oct 20, 2008
 * @version $Id: PublishThreadInfo.java,v 1.1 2008-10-21 11:40:41 jan Exp $
 */
package com.gentics.contentnode.publish;

import java.io.Serializable;

/**
 * This is class for storing information about how the publishing threads spend their time
 * You can get an instance for the current thread from the multiconnectiontransaction (TransactionManager.currentTransaction())
 * 
 * @author jan
 *
 */
public class PublishThreadInfo implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8777371794727169922L;

	private long timePublish = 0;
    
	private long timeWaitingLoad = 0;
    
	private long timeWaitingDB = 0;
    
	private String threadName;
    
	private int state = STATE_NOT_INITIALIZED;
    
	public static final int STATE_NOT_INITIALIZED = -1;
    
	public static final int WAITING_LOAD = 1;
    
	public static final int WORKING = 2;
    
	public static final int FINISHED = 3;
    
	public PublishThreadInfo(Thread thread) {
		this.threadName = thread.getName();
	}
    
	public void increaseTimePublish(long time) {
		timePublish += time;
	}
    
	public void increaseTimeWaitingLoad(long time) {
		timeWaitingLoad += time;
	}
    
	public void increaseTimeWaitingDB(long time) {
		timeWaitingDB += time;
	}

	/**
	 * @return the timePublish
	 */
	public long getTimePublish() {
		return timePublish;
	}

	/**
	 * @return the timeWaitingLoad
	 */
	public long getTimeWaitingLoad() {
		return timeWaitingLoad;
	}

	/**
	 * @return the timeWaitingDB
	 */
	public long getTimeWaitingDB() {
		return timeWaitingDB;
	}
    
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("Statistics for thread {" + threadName + "} - ");
		sb.append("worked: " + timePublish + "ms; ");
		sb.append("waited because of high load: " + timeWaitingLoad + "ms; ");
		sb.append("waited for database: " + timeWaitingDB + "ms");
		return sb.toString();
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(int state) {
		this.state = state;
	}
    
}
