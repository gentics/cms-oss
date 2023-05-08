/*
 * @author norbert
 * @date 14.02.2008
 * @version $Id: IWorkPhase.java,v 1.4 2008-02-26 10:36:11 herbert Exp $
 */
package com.gentics.lib.etc;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

public interface IWorkPhase {

	/**
	 * Initializes this workphases and all subphases.
	 * Must be called before {@link #begin()}
	 * 
	 * After all is done, {@link #finish(boolean)} should be called on the root phase.
	 * 
	 * @throws NodeException if something goes wrong.
	 *
	 */
	public abstract void init() throws NodeException;

	/**
	 * Should be called once the whole publish run & co is done. - only on the
	 * root phase !! this method it self will recursively call finish on all sub
	 * phases.
	 * @param isRepresentative this should be true if the user thinks this is a
	 *        "representative" publish run. (this is, e.g. when 10% of all pages
	 *        have been rendered)
	 * @throws NodeException
	 */
	public abstract void finish(boolean isRepresentative) throws NodeException;

	/**
	 * Adds the given amount of work units.
	 * @param work
	 */
	public abstract void addWork(int work);

	/**
	 * Adds another "subPhase" to this work phase
	 * @param workPhase
	 */
	public abstract void addSubPhase(IWorkPhase workPhase);

	/**
	 * Marks one further work as 'done'.
	 *
	 */
	public abstract void doneWork();

	/**
	 * Marks the given number of units as 'done'.
	 * @param units number of done units
	 */
	public abstract void doneWork(int units);

	/**
	 * Marks this work phase as having begun. Must be called before any work
	 * is done. - before this is called on any sub phase {@link #init()} must be called 
	 * on the root phase.
	 */
	public abstract void begin();

	public abstract boolean isCurrentlyRunning();

	/**
	 * Unique id of this phase.
	 * @return id
	 */
	public abstract String getId();

	/**
	 * userfriendly name for this phase.
	 * @return name
	 */
	public abstract String getName();

	/**
	 * Returns the current phase, or null if it is not running.
	 * 
	 * @return null if it is not running, a running subphase, or null.
	 */
	public abstract IWorkPhase getCurrentPhase();

	/**
	 * Marks this whole work phase as completed.
	 *
	 * Use {@link #doneWork()} if you have completed one single task.
	 */
	public abstract void done();

	public abstract int getDeviation();

	/**
	 * Returns the ETA in ms
	 * @return eta in ms.
	 */
	public abstract int getETA();

	/**
	 * Returns the total number of work units from itself and all subphases.
	 * @return total number of work units to complete.
	 */
	public abstract int getTotalWork();

	/**
	 * Returns the total number of work units which are already completed.
	 * @return total number of work units which are completed.
	 */
	public abstract int getTotalDoneWork();

	/**
	 * Returns the number of workunits for this phase (without subphases)
	 * @return work units
	 * @see #getTotalWork()
	 */
	public abstract int getWork();

	/**
	 * Returns the number of workunits of this phase which are already completed
	 * 
	 * @return completed work units
	 * @see #getTotalDoneWork()
	 */
	public abstract int getDoneWork();

	/**
	 * Returns the 'name' column of the nodesetup table entry.
	 */
	public abstract String getAverageMsName();

	/**
	 * Returns the 'name' column of the nodesetup table entry.
	 */
	public abstract String getAverageItemsName();

	public abstract int getPhaseCount(IWorkPhase stopPhase);

	public abstract List getSubPhases();

	public abstract long getStartTime();

	public abstract long getEndTime();

	public abstract boolean isDone();

	/**
	 * Create a subphase with given id. The returned object will be added to
	 * this instance as subphase.
	 * @param id id of the subphase
	 * @param name name of the subphase
	 * @return a subphase
	 */
	public abstract IWorkPhase createSubPhase(String id, String name);

	public abstract int getProgress();

	public abstract void renderRecursiveWorkPhase(StringBuffer ret, int i);

	public int getAbsoluteProgress();
    
	public void setWeight(int weight);
    
	public int getWeight();
}
