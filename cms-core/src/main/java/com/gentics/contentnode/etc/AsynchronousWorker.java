/*
 * @author jan
 * @date Aug 29, 2008
 * @version $Id: AsynchronousWorker.java,v 1.4 2008-12-02 13:54:33 jan Exp $
 */
package com.gentics.contentnode.etc;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.publish.AsynchronousWorkerLoadMonitor;
import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.publish.WorkLoadMonitor;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.log.NodeLogger;

public class AsynchronousWorker implements Runnable {

	/**
	 * State of the worker
	 */
	private volatile State state = State.initializing;

	/**
	 * A flag to indicate that the AsynchronousWorker is currently processing a job. Synchronize jobQueue whenever you touch it.
	 */
	private volatile boolean workInProgress = false;

	/**
	 * List of jobs, the worker has to process
	 */
	private final List<AsynchronousJob> jobQueue;

	/**
	 * List of processing times of the last up to {@link #FLOATING_AVERAGE_COUNT} jobs processed.
	 * This list will be used to calculate the floating average processing time in {@link #getFloatingAverage()}
	 */
	private List<Long> timeList = new Vector<Long>();

	/**
	 * Number of processing times, that shall be considered when getting the floating average processing time
	 */
	private static final int FLOATING_AVERAGE_COUNT = 100;

	/**
	 * Default Queue limit.
	 */
	public final static int DEFAULT_QUEUELIMIT = 1000;

	/**
	 * Queue limit. If the queue limit is reached, the method {@link #isFull()} will return true, but the worker will still accept new jobs.
	 */
	private int queueLimit = DEFAULT_QUEUELIMIT;

	/**
	 * Number of queued jobs (also counting grouped jobs individually)
	 */
	private int queuedJobs = 0;

	/**
	 * Delay in ms between checks for new jobs, if the {@link #jobQueue} is empty
	 */
	private int waitDelay = 30;

	/**
	 * Thread, the worker is using to process the jobs
	 */
	private Thread thread;

	/**
	 * Logger
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(AsynchronousWorker.class);

	/**
	 * True, if the worker shall exit, when processing a job fails (throws an exception), false if not
	 */
	private boolean onErrorExit = false;

	/**
	 * Stored exception, when one of the jobs fails
	 */
	private Exception storedException = null;

	/**
	 * Runnable, that will be run at the start of the worker with the worker's thread
	 */
	private Runnable initRoutine = null;

	/**
	 * Name of the worker
	 */
	private String name;

	/**
	 * Total processing time (in ms)
	 */
	private long totalProcessTime = 0;

	/**
	 * Start time (in ms)
	 */
	private long startTime = 0;

	/**
	 * counter for processed jobs
	 */
	private long jobCounter = 0;

	/**
	 * Current job group, if the last added job was groupable (and the group is not full)
	 */
	private JobGroup<GroupableAsynchronousJob> currentGroup = null;

	/**
	 * RenderResult for logging
	 */
	private RenderResult renderResult;

	/**
	 * Flag to determine whether jobs shall log to renderresult or not
	 */
	private boolean logJobs = true;

	/**
	 * Create a new worker
	 * @param name worker's name
	 * @param onErrorExit true if the worker shall exit on the first failing job, false if not
	 */
	public AsynchronousWorker(String name, boolean onErrorExit) {
		this(name, onErrorExit, DEFAULT_QUEUELIMIT);
	}

	/**
	 * Create a new worker
	 * @param name worker's name
	 * @param onErrorExit true if the worker shall exit on the first failing job, false if not
	 * @param queueLimit queue limit
	 */
	public AsynchronousWorker(String name, boolean onErrorExit, int queueLimit) {
		this.name = name;
		this.jobQueue = Collections.synchronizedList(new Vector<AsynchronousJob>());
		this.queuedJobs = 0;
		this.onErrorExit = onErrorExit;
		this.queueLimit = queueLimit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// set the start time
		startTime = System.currentTimeMillis();
		// if there is an initialization routine, run it now
		if (initRoutine != null) {
			initRoutine.run();
		}
		try {
			logInfoIgnoreException(name + " starting");
			// start the main loop
			while (state == State.running || !jobQueue.isEmpty()) {
				if (jobQueue.isEmpty()) {
					try {
						Thread.sleep(waitDelay);
					} catch (InterruptedException e) {// huh... somebody woke me up, maybe there's something to
						// do.
					}
					continue;
				}
				// get the first job from the queue
				AsynchronousJob job;
	
				synchronized (jobQueue) {
					job = jobQueue.remove(0);
					workInProgress = true;
					if (job instanceof JobGroup) {
						queuedJobs -= ((JobGroup) job).getQueueSize();
					} else {
						queuedJobs--;
					}
				}
				try {
					// process the job (measure the processing time)
					long start = System.currentTimeMillis();
	
					int workUnits = job.process(logJobs ? renderResult : null);
					long duration = System.currentTimeMillis() - start;
	
					// the job only count, if it is 'logged'
					if (job.isLogged()) {
						jobCounter += workUnits;
						totalProcessTime += duration;
						if (logJobs) {
							logInfo(name + " processed '" + job.getDescription() + "' in " + duration + " ms (remaining jobs: " + queuedJobs + ")");
						}
					}
	
					if (logger.isDebugEnabled()) {
						// store the last FLOATING_AVERAGE_COUNT processing times
						if (timeList.size() == FLOATING_AVERAGE_COUNT) {
							timeList.remove(0);
						}
						timeList.add(duration);
					}
	                
				} catch (Exception e) {
					storedException = e;
					logError(name + " failed to process the queue.", e);
					if (onErrorExit) {
						logger.info("Exiting.");
						state = State.failed;
					} else {
						logger.info("Continue.");
					}
				} finally {
					synchronized (jobQueue) {
						workInProgress = false;
					}
				}
			}
		} finally {
			// Make sure isFull() returns false, otherwise a full queue of a dead AsynchronousWorker
			// will hang the the Publish thread indefinitely
			queuedJobs = 0;
		}

		// calc the total duration of the queue
		long totalDuration = System.currentTimeMillis() - startTime;
		if (renderResult != null) {
			if (jobCounter == 0) {
				logInfoIgnoreException(name + " processed 0 jobs. Was idle for " + totalDuration + " ms");
			} else {
				long avg = totalProcessTime/jobCounter;
				long idleTime = totalDuration - totalProcessTime;
				int idleTimePerc = 100;
				if (totalDuration > 0) {
					idleTimePerc = (int)(idleTime * 100 / totalDuration);
				}
				logInfoIgnoreException(name + " processed " + jobCounter + " jobs in " + totalProcessTime + " ms (avg " + avg + " ms/job). Was idle for "
								+ idleTime + " ms (" + idleTimePerc + "%)");
			}
		}

		logger.debug("Asynchronous worker finished.");
	}

	/**
	 * Get the floating average processing time
	 * @return floating average processing time
	 */
	private float getFloatingAverage() {
		// avoid devision by zero in the last line!
		if (timeList.size() == 0) {
			return 0;
		}
        
		long sum = 0;

		for (Iterator<Long> i = timeList.iterator(); i.hasNext();) {
			sum += i.next().longValue();
		}
		return sum / timeList.size();
	}

	/**
	 * Log a message in the render result as info (if render result not null)
	 * @param message message
	 * @throws NodeException 
	 */
	private void logInfo(String message) throws NodeException {
		if (renderResult != null) {
			renderResult.info(Publisher.class, message);
		}
	}

	/**
	 * Log a message in the render result as info (if render result not null)
	 * @param message message
	 */
	private void logInfoIgnoreException(String message) {
		try {
			logInfo(message);
		} catch (NodeException ignored) {
		}
	}

	/**
	 * Log a message in the render result as error (if render result not null)
	 * @param message message
	 * @param e throwable
	 */
	private void logError(String message, Throwable e) {
		if (renderResult != null) {
			try {
				renderResult.error(Publisher.class, message, e);
			} catch (NodeException ignored) {
			}
		}
	}

	/**
	 * Wait until the jobQueue is empty (all pending jobs are done).
	 */
	public void flush() {
		if (state == State.running) {
			logger.info("Flushing asynchronous worker.");
			if (currentGroup != null) {
				jobQueue.add(currentGroup);
				queuedJobs += currentGroup.getQueueSize();
				currentGroup = null;
			}
			while (true) {
				synchronized (jobQueue) {
					if (jobQueue.isEmpty() && !workInProgress) {
						break;
					}
				}
				try {
					Thread.sleep(waitDelay);
				} catch (InterruptedException e) {}
			}
		}
	}

	/**
	 * Stop the worker
	 */
	public void stop() {
		if (state != State.stopped) {
			logInfoIgnoreException("Stopping " + name);
			state = State.stopped;
		}
	}

	/**
	 * Abort the worker.
	 */
	public void abort() {
		if (state != State.aborted) {
			logInfoIgnoreException("Aborting " + name);
			state = State.aborted;
		}
		jobQueue.clear();
	}

	/**
	 * Start the worker
	 */
	public void start() {
		state = State.running;
		thread = new Thread(Thread.currentThread().getThreadGroup(), this, name);
		thread.start();
	}

	/**
	 * This starts the asynchronous worker.
	 * @param initRoutine this runnable will be executed at the start of the
	 *        inner worker thread (e.g. not in the currentThread() but in the
	 *        thread of the worker itself - just like all the jobs)
	 */
	public void start(Runnable initRoutine) {
		this.initRoutine = initRoutine;
		start();
	}

	/**
	 * Throws the stored exception (if any)
	 */
	public void throwExceptionOnFailure() throws NodeException {
		if (storedException instanceof NodeException) {
			throw (NodeException) storedException;
		} else if (storedException != null) {
			throw new NodeException("The worker thread reported an error", storedException);
		}
	}

	/**
	 * Add a job to the worker
	 * @param job job
	 */
	public void addAsynchronousJob(AsynchronousJob job) throws NodeException {
		checkThreadState();
		switch (state) {
		case stopped:
		case aborted:
		case failed:
			return;
		case initializing:
		case died:
			throw new NodeException("Cannot add job to " + name + ": Worker is in state '" + state.name() + "'");
		default:
			break;
		}
		synchronized (jobQueue) {
			if (currentGroup != null && job instanceof GroupableAsynchronousJob) {
				GroupableAsynchronousJob gJob = (GroupableAsynchronousJob)job;
				if (currentGroup.isGroupable(gJob)) {
					currentGroup.addAsynchronousJob(gJob);
					if (currentGroup.isFull()) {
						jobQueue.add(currentGroup);
						queuedJobs += currentGroup.getQueueSize();
						currentGroup = null;
					}
				} else {
					// job is not groupable by this group
					// add the current group to the jobqueue
					jobQueue.add(currentGroup);
					queuedJobs += currentGroup.getQueueSize();

					// create a new current group from the groupable job
					currentGroup = gJob.getGroup();
				}
			} else {
				if (currentGroup != null) {
					jobQueue.add(currentGroup);
					queuedJobs += currentGroup.getQueueSize();
					currentGroup = null;
				}
				if (job instanceof GroupableAsynchronousJob) {
					GroupableAsynchronousJob gJob = (GroupableAsynchronousJob)job;
					currentGroup = gJob.getGroup();
				} else {
					jobQueue.add(job);
					queuedJobs++;
				}
			}
		}
	}

	/**
	 * Join the worker's thread. This will wait, until the worker's thread dies.
	 */
	public void join() {
		try {
			thread.join();
		} catch (InterruptedException e) {
			logger.info("The current thread has been interrupted while waiting for the worker to finish.");
		}
	}

	/**
	 * Check whether the worker is currently running
	 * If the internal state is {@link State#running}, but the thread is not alive, the state is changed to {@link State#died}
	 */
	protected void checkThreadState() {
		if (state == State.running) {
			if (thread == null || !thread.isAlive()) {
				state = State.died;
			}
		}
	}

	/**
	 * Check whether the queue of the worker is full.
	 * If the queue is considered full, the worker will still accept new jobs (by calling {@link #addAsynchronousJob(AsynchronousJob)}), so this method should be used in advance.
	 * Currently this will be done in the {@link WorkLoadMonitor} implementation {@link AsynchronousWorkerLoadMonitor}.
	 * @return true if the queue is full, false if not
	 */
	public boolean isFull() {
		return queuedJobs >= queueLimit;
	}

	/**
	 * Get the number of queued jobs
	 * @return number of queued jobs
	 */
	public int getQueuedJobs() {
		return queuedJobs;
	}

	/**
	 * Set the render result
	 * @param renderResult
	 */
	public void setRenderResult(RenderResult renderResult) {
		this.renderResult = renderResult;
	}

	/**
	 * Set whether jobs shall log to render result
	 * @param logJobs true iff jobs shall log
	 */
	public void setLogJobs(boolean logJobs) {
		this.logJobs = logJobs;
	}

	/**
	 * Possible states of the worker
	 */
	private static enum State {
		/**
		 * Worker is not yet started
		 */
		initializing,

		/**
		 * Worker is started and running
		 */
		running,

		/**
		 * Worker failed with an error
		 */
		failed,

		/**
		 * Worker thread unexpectedly died
		 */
		died,

		/**
		 * Worker has been stopped (normal termination)
		 */
		stopped,

		/**
		 * Worker has been aborted (abnormal termination)
		 */
		aborted;
	}
}
