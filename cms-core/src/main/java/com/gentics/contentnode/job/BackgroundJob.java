/*


 * 
 * 
 * @author floriangutmann
 * @date Dec 4, 2009
 * @version $Id: BackgroundJob.java,v 1.4 2010-01-29 15:18:48 norbert Exp $
 */
package com.gentics.contentnode.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.UnableToInterruptJobException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.scheduler.SchedulerUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * A Job that is running for a specified amount of time in foreground and sent
 * to background afterwards.<br />
 * If the job finishes in foreground mode the execute() method returns true and
 * results are available in getExceptions() and getResultMap().<br />
 * If the job finishes in Background, the method finishedInBackground() is
 * called with the results of the Job.
 * 
 * @author floriangutmann
 */
public abstract class BackgroundJob implements InterruptableJob {
    
	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(BackgroundJob.class);

	/**
	 * Parameter for the language id.
	 */
	public static final String PARAM_LANGUAGE_ID = "languageId";
    
	/**
	 * Parameter for the job id.
	 */
	public static final String PARAM_JOBID = "jobId";
    
	/**
	 * Map with JobInformation objects that can be accessed by the jobs
	 * String jobname => JobInformation
	 */
	private static Map<String, JobInformation> jobInformation = new HashMap<>();

	/**
	 * Flag to mark whether the job was interrupted
	 */
	private boolean interrupted = false;

	/**
	 * Get the JobInformation for a specific job.
	 * @param jobName The name of the job to get the information from.
	 */
	public static synchronized JobInformation getJobInformation(String jobName) {
		return (JobInformation) jobInformation.get(jobName);
	}
    
	/**
	 * Adds a JobInformation to the map with job information
	 */
	public static synchronized void addJobInformation(JobInformation information) {
		jobInformation.put(information.getJobName(), information);
	}

	/**
	 * Removes a JobInformation from the map with job information
	 */
	public static synchronized void removeJobInformation(String jobName) {
		jobInformation.remove(jobName);
	}

	/**
	 * Returns true if a job is currently in foreground.
	 * @param jobName The name of the job to check
	 */
	public static synchronized boolean isForegroundJob(String jobName) {
		JobInformation info = getJobInformation(jobName);

		if (info == null) {
			// no job info found
			return true;
		} else {
			return info.isForeground();
		}
	}
    
	/**
	 * List with exceptions that were produced by the job
	 */
	protected List<Exception> exceptions = new ArrayList<Exception>();
    
	/**
	 * Result of the job
	 */
	protected Object jobResult = null;

	/**
	 * Map with parameter, that are put into the JobDataMap.
	 */
	private Map<String, Serializable> parameters = new HashMap<>();

	/**
	 * Static id that is assigned to jobs
	 */
	private int jobId;

	/**
	 * Name of the job
	 */
	protected String jobName;

	/**
	 * Messages
	 */
	private List<NodeMessage> messages = new LinkedList<NodeMessage>();

	/**
	 * Executes the job. This method will block until the job is finished but
	 * not longer than the specified foreground time.
	 * @param foregroundTime Time in seconds that the Job should stay in foreground.
	 * @return True if the job is finished during foreground time. False if the
	 *         job has been sent to background.
	 * @throws NodeException
	 */
	public boolean execute(int foregroundTime) throws NodeException {
		return execute(foregroundTime, false, false, true);
	}

	/**
	 * Executes the job. This method will block until the job is finished but
	 * not longer than the specified foreground time.
	 * @param foregroundTime Time in seconds that the Job should stay in foreground.
	 * @param durability setting for {@link JobDetail#setDurability(boolean)}
	 * @param volatility setting for {@link JobDetail#setVolatility(boolean)}
	 * @param requestRecovery setting for {@link JobDetail#requestsRecovery()}
	 * @return True if the job is finished during foreground time. False if the
	 *         job has been sent to background.
	 * @throws NodeException
	 */
	public boolean execute(int foregroundTime, boolean durability, boolean volatility, boolean requestRecovery) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
        
		// Schedule the Job
		Scheduler sched = t.getNodeConfig().getPersistentScheduler();
        
		if (sched == null) {
			throw new NodeException("Could not get Scheduler from NodeConfig.");
		}
        
		this.jobId = SchedulerUtils.getJobId();
        
		jobName = createJobName();
		String triggerName = createTriggerName();

		JobDetail jobDetail = new JobDetail(jobName, Scheduler.DEFAULT_GROUP, this.getClass());

		jobDetail.setDurability(durability);
		jobDetail.setVolatility(volatility);
		jobDetail.setRequestsRecovery(requestRecovery);
		jobDetail.getJobDataMap().putAll(parameters);
		jobDetail.getJobDataMap().put(PARAM_LANGUAGE_ID, new Integer(ContentNodeHelper.getLanguageId()));
		jobDetail.getJobDataMap().put(PARAM_JOBID, jobId);
		Trigger trigger = TriggerUtils.makeImmediateTrigger(triggerName, 0, 1);

		trigger.setVolatility(volatility);
		trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

		// create the job information and schedule the job
		JobInformation info = new JobInformation(jobName);

		try {
			info.setForeground(true);
			BackgroundJob.addJobInformation(info);
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			throw new NodeException("Error while Scheduling Job!", e);
		}
        
		// Check if the Job is finished
		for (int i = 0; i < foregroundTime; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			if (info.getJobStatus() != JobInformation.JOB_NOTSTARTED
					&& info.getJobStatus() != JobInformation.JOB_RUNNING) {
				// job finished in foreground, get eventually caught exceptions and results
				this.exceptions = info.getExceptions();
				this.jobResult = info.getJobResult();
				this.messages = info.getMessages();

				// we don't need the information any more (job is finished anyway)
				removeJobInformation(jobName);
				return true;
			}

			// check whether the job thread is still alive
			Thread jobThread = info.getJobThread();
			if (jobThread != null && !jobThread.isAlive()) {
				// job thread is not alive, check whether the job just finished
				if (info.getJobStatus() == JobInformation.JOB_FINISHED) {
					// job finished in foreground, get eventually caught exceptions and results
					this.exceptions = info.getExceptions();
					this.jobResult = info.getJobResult();
					this.messages = info.getMessages();

					// we don't need the information any more (job is finished anyway)
					removeJobInformation(jobName);
					return true;
				} else {
					// thread unexpectedly terminated
					removeJobInformation(jobName);
					throw new NodeException("Error while running job, thread unexpectedly terminated");
				}
			}
		}

		synchronized (info) {
			if (info.getJobStatus() == JobInformation.JOB_INTERRUPTED) {
				removeJobInformation(jobName);
				return false;
			} else if (info.getJobStatus() == JobInformation.JOB_RUNNING || info.getJobStatus() == JobInformation.JOB_NOTSTARTED) {
				// job will run in background now
				info.setForeground(false);
			} else {
				this.exceptions = info.getExceptions();
				this.jobResult = info.getJobResult();

				// we don't need the information any more (job is finished anyway)
				removeJobInformation(jobName);
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds a parameter to the job that can be accessed in the JobDataMap in
	 * {@link BackgroundJob#execute(JobExecutionContext)}.
	 * 
	 * @param key The key under which the value is stored in the JobDataMap.
	 * @param value Value that should be stored in the JobDataMap.
	 */
	public void addParameter(String key, Serializable value) {
		parameters.put(key, value);
	}
    
	/**
	 * Get a list of exceptions that were produced by the running job.
	 * @return A list of exceptions.
	 */
	public List<Exception> getExceptions() {
		return exceptions;
	}
    
	/**
	 * Get the map with results produced by the job 
	 * @return A map with results produced by the job
	 */
	public Object getJobResult() {
		return jobResult;
	}

	/**
	 * Get the messages
	 * @return messages
	 */
	public List<NodeMessage> getMessages() {
		return messages;
	}

	/**
	 * Add a message
	 * @param message message to add
	 */
	public void addMessage(NodeMessage message) {
		messages.add(message);
	}

	/*
	 * (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public final void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		String jobName = jobExecutionContext.getJobDetail().getName();
		JobInformation info = BackgroundJob.getJobInformation(jobName);

		if (info == null) {
			// info was not found, so we are running a re-scheduled job (which
			// naturally runs in background)
			info = new JobInformation(jobName);
			info.setForeground(false);
			BackgroundJob.addJobInformation(info);
		}

		// set the job thread
		info.setJobThread(Thread.currentThread());

		// juhuu, we are running now!
		info.setJobStatus(JobInformation.JOB_RUNNING);

		try {
			Integer languageId = (Integer) jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_LANGUAGE_ID);

			if (languageId != null) {
				ContentNodeHelper.setLanguageId(languageId.intValue());
			}
			ContentNodeFactory factory = ContentNodeFactory.getInstance();

			factory.startTransaction(true);

			executeJob(jobExecutionContext);

			boolean foreground = true;

			synchronized (info) {
				info.setJobStatus(JobInformation.JOB_FINISHED);
				info.addExceptions(exceptions);
				info.setJobResult(jobResult);
				foreground = info.isForeground();
				info.addMessages(messages);
			}

			// Handle the Jobresults
			if (!foreground) {
				finishedInBackground(jobExecutionContext.getJobDetail().getJobDataMap(), exceptions, jobResult, messages);
				BackgroundJob.removeJobInformation(jobName);
			}
            
		} catch (Exception e) {
			logger.error("Error while executing Job " + jobExecutionContext.getJobDetail().getName(), e);
			synchronized (info) {
				info.addException(e);
				info.setJobStatus(JobInformation.JOB_FINISHED);
				if (!info.isForeground()) {
					BackgroundJob.removeJobInformation(jobName);
				}
			}
		}
	}
    
	/**
	 * Is called when the job is finished in background.
	 * 
	 * @param jobExecutionContext JobExecutionContext 
	 */
	public abstract void finishedInBackground(Map jobDataMap, List exceptions, Object jobResult, List<NodeMessage> messages);

	/**
	 * Is called when the container wants to shut down and the job should be
	 * finished as soon as possible. If the container is shut up again the job will be executed a second time.
	 */
	public void interrupt() throws UnableToInterruptJobException {
		interrupted = true;
		if (logger.isInfoEnabled()) {
			logger.info("Job " + getClass().getName() + " was interrupted");
		}
	}

	/**
	 * Executes the work of the job. All parameters that were added 
	 * with {@link BackgroundJob#addParameter} are available in the JobDataMap of the jobExecutionContext.<br />
	 * Within this method you can get a transaction initialized for the system user by {@link TransactionManager#getCurrentTransaction()}.
	 * Make sure to commit or rollback the transaction when you are finished.
	 * 
	 * @throws JobExecutionException when the job is interrupted. In this case, the results will not be handled.
	 */
	public abstract void executeJob(JobExecutionContext jobExecutionContext) throws JobExecutionException;

	/**
	 * Create the job name. By default this will be "BackgroundJob[XX]" where
	 * [XX] is an autoincremented number. This can be overwritten in
	 * implementations to give the job another name
	 * 
	 * @return job name
	 */
	protected String createJobName() {
		return "BackgroundJob" + jobId;
	}

	/**
	 * Create the trigger name. By default this will be "Trigger[XX]" where [XX]
	 * is an autoincremented number. This can be overwritten in implementations
	 * to give the trigger another name
	 * 
	 * @return trigger name
	 */
	protected String createTriggerName() {
		return "Trigger" + jobId;
	}
    
	public int getJobId() {
		return jobId;
	}

	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * If the job was interrupted, throw an exception
	 * @throws JobExecutionException
	 */
	protected void abortWhenInterrupted() throws JobExecutionException {
		if (interrupted) {
			if (logger.isInfoEnabled()) {
				logger.info("Job was interrupted");
			}
			throw new JobExecutionException("Job was interrupted");
		}
	}

	/**
	 * Class that holds information about a job.
	 * 
	 * @author floriangutmann
	 */
	public static class JobInformation {

		/**
		 * Status of job, when it was not yet started
		 */
		public final static int JOB_NOTSTARTED = 0;

		/**
		 * Status of job, when it is currently running
		 */
		public final static int JOB_RUNNING = 1;

		/**
		 * Status of job, when it finished (successful or not)
		 */
		public final static int JOB_FINISHED = 2;

		/**
		 * Status of job, when it was interrupted
		 */
		public final static int JOB_INTERRUPTED = 3;
        
		/**
		 * Name of the job
		 */
		private String jobName = null;
        
		/**
		 * Exceptions that occured during the execution of the job
		 */
		private List<Exception> exceptions = new ArrayList<Exception>();

		/**
		 * Map where the Job can store results
		 */
		private Object jobResult = null;

		/**
		 * Current job status
		 */
		private int jobStatus = JOB_NOTSTARTED;

		/**
		 * flag to mark whether the job is currently running in foreground or background
		 */
		private boolean foreground = true;

		/**
		 * Messages
		 */
		private List<NodeMessage> messages = new LinkedList<NodeMessage>();

		/**
		 * Thread that is running this job (if it is started)
		 */
		private Thread jobThread;

		/**
		 * Default constructor
		 */
		public JobInformation() {}
        
		/**
		 * Full constructor that sets all available information
		 * @param jobName name of the job
		 */
		public JobInformation(String jobName) {
			this.jobName = jobName;
		}

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public List<Exception> getExceptions() {
			return this.exceptions;
		}

		public void addExceptions(List<Exception> exceptions) {
			this.exceptions.addAll(exceptions);
		}

		public void addException(Exception e) {
			this.exceptions.add(e);
		}

		public Object getJobResult() {
			return jobResult;
		}

		public void setJobResult(Object jobResult) {
			this.jobResult = jobResult;
		}

		/**
		 * Get the current job status
		 * @return current job status
		 */
		public int getJobStatus() {
			return jobStatus;
		}

		/**
		 * Set the current job status
		 * @param jobStatus current job status
		 */
		public void setJobStatus(int jobStatus) {
			this.jobStatus = jobStatus;
		}

		/**
		 * Set the flag "foreground"
		 * @param foreground true when running in foreground, false for background
		 */
		public void setForeground(boolean foreground) {
			this.foreground = foreground;
		}

		/**
		 * Check whether the job is running in foreground
		 * @return true for running in foreground, false for background
		 */
		public boolean isForeground() {
			return foreground;
		}

		/**
		 * Get the messages
		 * @return messages
		 */
		public List<NodeMessage> getMessages() {
			return messages;
		}

		/**
		 * Add a message
		 * @param message message to add
		 */
		public void addMessage(NodeMessage message) {
			this.messages.add(message);
		}

		/**
		 * Add a list of messages
		 * @param messages list of messages
		 */
		public void addMessages(List<NodeMessage> messages) {
			this.messages.addAll(messages);
		}

		/**
		 * Set the job thread
		 * @param jobThread job thread
		 */
		public void setJobThread(Thread jobThread) {
			this.jobThread = jobThread;
		}

		/**
		 * Get the job thread
		 * @return job thread
		 */
		public Thread getJobThread() {
			return jobThread;
		}
	}
}
