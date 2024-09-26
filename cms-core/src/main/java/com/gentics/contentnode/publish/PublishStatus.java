/*
 * @author Stefan Hepp
 * @date 23.1.2006
 * @version $Id: PublishState.java,v 1.2 2006-02-05 18:16:44 stefan Exp $
 */
package com.gentics.contentnode.publish;

import com.gentics.lib.log.NodeLogger;

/**
 * The publishstatus interface is used by the the publish processes to
 * update/store their current progress informations and logs.
 *
 * Note: the processes have own instances of the publishstatus. The cummulative
 * progressinfo must sum/merge all the infos.
 */
public interface PublishStatus extends PublishInfo {

	// TODO add method to set status to 'failed', so it can be checked by filewrite, etc..

	/**
	 * set the current progress of the process in percent.
	 * @param percent the current progess in percent.
	 */
	void setProgress(int percent);

	/**
	 * set the estimated remaining duration in seconds.
	 * @param seconds the remaining duration of the progress in seconds.
	 */
	void setEstimatedDuration(int seconds);

	/**
	 * update the status message about the current progress.
	 * @param message a new status message about the current progress.
	 */
	void updateStatusMessage(String message);

	/**
	 * Get a logger where publish procresses can log their status-logs to.
	 * You should use this logger to log informations about the current progress.
	 * You do not need to additionally log information to the system-log, as this is
	 * done by the statuslogger itself.
	 *
	 * @return a logger where status informations about the current progress can be logged.
	 */
	NodeLogger getStatusLogger();

}
