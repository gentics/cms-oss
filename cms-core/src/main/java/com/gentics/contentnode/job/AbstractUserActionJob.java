/*
 * @author norbert
 * @date 15.12.2009
 * @version $Id: AbstractUserActionJob.java,v 1.2.4.1 2011-02-16 16:47:58 tobiassteiner Exp $
 */
package com.gentics.contentnode.job;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Abstract Class for all Jobs that are started from user interactions.
 * Implements basic functionality, like getting userid and sessionid from the
 * job data, invoking the method (which is implemented by the individual
 * subclasses) and handling of errors, etc.
 */
public abstract class AbstractUserActionJob extends BackgroundJob {

	/**
	 * Parameter that specifies the userid of the user who started the job
	 */
	public static final String PARAM_USERID = "userId";
    
	/**
	 * Parameter that specifies a sessionId of the user from PARAM_USERID
	 */
	public static final String PARAM_SESSIONID = "sessionId";

	/**
	 * Result if everything went Ok
	 */
	public static final String RESULT_OK = "resultOK";

	/**
	 * Result if something went wrong
	 */
	public static final String RESULT_FAILURE = "resultFailure";

	/**
	 * Result when a InsufficientPrivilegesException occurred
	 */
	public static final String RESULT_INSUFFICIENT_PRIVILEGES = "insufficientPrivileges";
    
	/**
	 * Result when a NodeException occurred (Internal error)
	 */
	public static final String RESULT_INTERNAL_ERROR = "internalError";

	/**
	 * Transaction of this job. Is stored in the Job instance to be able to
	 * interrupt it out of other threads.
	 */
	protected Transaction t;

	/**
	 * flag to mark whether the job was interruptd
	 */
	protected boolean interrupted = false;

	/**
	 * id of the user doing the job
	 */
	protected Integer userId;

	/**
	 * session id of the user
	 */
	protected String sessionId;
    
	/**
	 * Create an instance of the Job
	 */
	public AbstractUserActionJob() {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.job.BackgroundJob#executeJob(org.quartz.JobExecutionContext)
	 */
	public void executeJob(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
		Transaction mainTransaction = null;
		String[] dirtAnalysisInfo = null;

		// Get the userId
		userId = (Integer) map.get(PARAM_USERID);
		sessionId = (String) map.get(PARAM_SESSIONID);

		if (userId == null || sessionId == null || !getJobParameters(map)) {
			exceptions.add(new NodeException("Error while executing job. Not all mandatory parameters have been set"));
			jobResult = RESULT_INTERNAL_ERROR;
			return;
		}

		// Start the log for dirt analysis
		try {
			mainTransaction = TransactionManager.getCurrentTransaction();
			dirtAnalysisInfo = getDirtAnalysisInfo();
			if (dirtAnalysisInfo != null) {
				Events.triggerLogEvent(new Integer(0), dirtAnalysisInfo, sessionId, Events.LOGGING_START);
				mainTransaction.commit(false);
			}
		} catch (Exception e) {
			logger.error("Error while writing log start for dirt Analysis", e);
			rollbackTransaction(mainTransaction);
			exceptions.add(e);
			jobResult = RESULT_INTERNAL_ERROR;
			return;
		}
        
		// Get a transaction with permissions of the user
		try {
			synchronized (this) {
				if (interrupted) {
					throw new JobExecutionException();
				}
				ContentNodeFactory factory = ContentNodeFactory.getInstance();

				t = factory.startTransaction(sessionId, userId, true);
                
				// TODO: do we need to authenticate the session? Jobs are started from
				// the scheduler, so I would assume that sessions need to be authenticated
				// before the job is added.
			}
		} catch (Exception e) {
			logger.error("Error while initializing Transaction", e);
			exceptions.add(new NodeException("Error while initializing Transaction", e));
			jobResult = RESULT_INTERNAL_ERROR;
			return;
		}

		try {
			processAction();
			t.commit();
			logger.debug("The transaction has been committed for job {" + jobName + "}");
			if (jobResult == null) {
				jobResult = RESULT_OK;
			}
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			rollbackTransaction(t);
			exceptions.add(e);
			jobResult = RESULT_INSUFFICIENT_PRIVILEGES;
			return;
		} catch (Exception e) {
			logger.error("Error occured during background job", e);
			rollbackTransaction(t);
			exceptions.add(e);
			jobResult = RESULT_INTERNAL_ERROR;
			return;
		} finally {
			// Write the end log for dirt analysis
			try {
				TransactionManager.setCurrentTransaction(mainTransaction);
				if (dirtAnalysisInfo != null) {
					Events.triggerLogEvent(new Integer(0), null, sessionId, Events.LOGGING_END);
				}
				mainTransaction.commit();
			} catch (NodeException e) {
				logger.error("Error while writing log end for dirt Analysis", e);
				rollbackTransaction(mainTransaction);
				exceptions.add(e);
				jobResult = RESULT_INTERNAL_ERROR;
				return;
			}
		}
	}

	/**
	 * This method gets the specific dirt analysis information, if the job
	 * wishes to support dirt analysis or null of not. The returned String[]
	 * must at least contain [objType, objId, cmdDescId] and may optionally
	 * contain the number of additionally manipulated objects as fourth value.
	 * The default implementation returns null (no dirt analysis).
	 * @return specific dirt analysis information, should return null
	 * @throws NodeException 
	 */
	protected String[] getDirtAnalysisInfo() throws NodeException {
		return null;
	}

	/**
	 * This method extracts specific job parameters from the map and stores them
	 * locally. The default implementation does nothing (only returns true),
	 * subclasses may implement this method if necessary.
	 * @param map JobParameterMap
	 * @return true if all mandatory parameters were given, false if at least
	 *         one was missing
	 */
	protected boolean getJobParameters(JobDataMap map) {
		return true;
	}

	/**
	 * Rolles back a transaction and loggs occuring errors
	 * @param t Transaction to rollback
	 */
	private void rollbackTransaction(Transaction t) {
		if (t != null) {
			if (t.isOpen()) {
				try {
					t.rollback();
				} catch (TransactionException te) {
					logger.error("Error occured when rolling back transaction", te);
				}
			}
		}
	}

	/**
	 * Check whether the transaction was interrupted, if yes, it is rolled back
	 * (if still open) and a {@link JobExecutionException} is thrown
	 * @throws NodeException when an error occurred during rollback
	 * @throws JobExecutionException when the transaction was interrupted
	 */
	protected void checkForInterruption() throws NodeException, JobExecutionException {
		if (t.isInterrupted()) {
			if (t.isOpen()) {
				t.rollback();
			}
			throw new JobExecutionException();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.job.BackgroundJob#finishedInBackground(java.util.Map, java.util.List, java.lang.Object, java.util.List)
	 */
	public void finishedInBackground(Map jobDataMap, List exceptions, Object jobResult, List<NodeMessage> messages) {
		logger.info("Job finished in Background"); // TODO remove
        
		// Obtain a Transaction
		Transaction t;

		try {
			ContentNodeFactory factory = ContentNodeFactory.getInstance();

			t = factory.startTransaction(true);
		} catch (NodeException e) {
			logger.error("Error while initializing Transaction when finished in Background", e);
			return;
		}
        
		MessageSender messageSender = new MessageSender();

		t.addTransactional(messageSender);
		Message message = null;
		int instantTime = 7200;
		int userId = ((Integer) jobDataMap.get(PARAM_USERID)).intValue();
        
		// Create message depending on the jobResult
		if (RESULT_INSUFFICIENT_PRIVILEGES.equals(jobResult)) {
			InsufficientPrivilegesException e = (InsufficientPrivilegesException) exceptions.get(0);
			CNI18nString msg = new CNI18nString("job_error");

			msg.addParameter(getJobDescription());
			message = new Message(1, userId, msg + "\n" + e.getLocalizedMessage(), instantTime);
		} else if (RESULT_INTERNAL_ERROR.equals(jobResult)) {
			for (Iterator it = exceptions.iterator(); it.hasNext();) {
				Exception e = (Exception) it.next();

				logger.error("Error occured during " + getJobDescription(), e);
				CNI18nString messageText = new CNI18nString("backgroundjob_unexpected_error");

				messageText.addParameter(getJobDescription());
				message = new Message(1, userId, messageText.toString(), instantTime);
			}
		} else if (RESULT_OK.equals(jobResult)) {
			CNI18nString messageText = new CNI18nString("backgroundjob_finished_successfully");

			messageText.addParameter(getJobDescription());
			message = new Message(1, userId, messageText.toString(), instantTime);
		} else if (RESULT_FAILURE.equals(jobResult)) {
			CNI18nString messageText = new CNI18nString("backgroundjob_finished_with_errors");

			messageText.addParameter(getJobDescription());
			message = new Message(1, userId, messageText.toString(), instantTime);
		}
        
		messageSender.sendMessage(message);

		// also send messages that were returned from the job
		for (NodeMessage nodeMessage : messages) {
			messageSender.sendMessage(new Message(1, userId, nodeMessage.getMessage(), instantTime));
		}
        
		// Commit The transaction 
		try {
			t.commit(true);
		} catch (NodeException e) {
			logger.error("Error while committing Transaction when finished in Background", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.job.BackgroundJob#interrupt()
	 */
	public synchronized void interrupt() throws UnableToInterruptJobException {
		if (t != null) {
			t.interrupt();
		}
		interrupted = true;
	}

	/**
	 * Get the i18ned job description (to be used in messages to the user)
	 * @return job description as localized string
	 */
	public abstract String getJobDescription();

	/**
	 * Abstract method to process the action, needs to be implemented by the
	 * subclasses. When this method is called, the curren transaction (also
	 * stored in {@link AbstractUserActionJob#t}) is initialized with the
	 * permissions of the given user. The transaction will be rolled back or
	 * committed by the abstract base class, so there is no need to do this in
	 * the implementation of this method.
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 * @throws JobExecutionException
	 */
	protected abstract void processAction() throws InsufficientPrivilegesException,
				NodeException, JobExecutionException;
}
