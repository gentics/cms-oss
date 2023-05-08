/*
 * @author norbert
 * @date 23.05.2005
 * @version $Id: SchedulingInterface.java,v 1.2 2005-08-01 07:41:40 laurin Exp $
 */
package com.gentics.lib.scheduler;

import java.util.concurrent.ScheduledExecutorService;

/**
 * interface for classes that have to schedule some jobs run automatically
 * within the portalwrapper
 * @author norbert
 */
public interface SchedulingInterface {

	/**
	 * method to schedule jobs
	 * @param scheduler scheduler use to schedule the jobs
	 */
	void scheduleJobs(ScheduledExecutorService scheduler);
}
