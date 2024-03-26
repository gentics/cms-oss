/*
 * @author Stefan Hepp
 * @date 23.1.2006
 * @version $Id: PublishController.java,v 1.14 2010-01-08 13:22:43 norbert Exp $
 */
package com.gentics.contentnode.publish;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.AttributedThreadGroup;

/**
 * This is the main controller for the publish process. This class should be used to
 * access and interact with the publisher threads.
 *
 * TODO implement lots of synchronizings, honor configkey in sync
 */
public class PublishController {

	private static Thread runningPublisher = null;
	private static Publisher publisher = null;
	private static PublishInfo currentPublishInfo = null;
	private static PublishInfo previousPublishInfo = null;

	/**
	 * State of the publisher
	 */
	private static State state = State.stopped;

	private static NodeLogger logger = NodeLogger.getNodeLogger(PublishController.class);

	/**
	 * Loader for instances of {@link InstantPublishService}
	 */
	protected static ServiceLoaderUtil<InstantPublishService> instantPublishServiceLoader = ServiceLoaderUtil
			.load(InstantPublishService.class);

	/**
	 * No instances allowed
	 */
	private PublishController() {
	}

	/**
	 * check if the current publisher is running.
	 * @return true, if the publisher is running.
	 */
	public static boolean isRunning() {
		try {
			return DistributionUtil.call(new CheckRunStatusTask());
		} catch (Exception e) {
			logger.error("Error while checking run status of publish process", e);
			return false;
		}
	}

	/**
	 * Check if the publish process is running on this instance
	 * @return true, if the publisher is running on this instance
	 */
	public static boolean isRunningLocally() {
		// if the running publisher has died, we set it to null
		if (runningPublisher != null && !runningPublisher.isAlive()) {
			runningPublisher = null;
			// publisher = null;
		}
		return runningPublisher != null;
	}

	/**
	 * start a new publish process, if the publisher is not running.
	 * @param force true, if single-page errors should be ignored.
	 * @param timestamp timestamp of the publish transaction
	 * @return true, if the publisher was started, or false if it was already running.
	 */
	public synchronized static boolean startPublish(boolean force, long timestamp) {
		// check whether the publisher is already running
		if (isRunning()) {
			return false;
		}

		try {
			return DistributionUtil.call(new StartPublisherTask(force, timestamp));
		} catch (Exception e) {
			logger.error("Error while starting publish process", e);
			return false;
		}
	}

	/**
	 * Stop the running publish process
	 * @return true if the publish process was stopped, false if it was not running
	 */
	public static synchronized boolean stopPublish() {
		if (!isRunning()) {
			return false;
		}

		try {
			return DistributionUtil.call(new StopPublisherTask());
		} catch (Exception e) {
			logger.error("Error while stopping publish process", e);
			return false;
		}
	}

	/**
	 * Stop the locally running publish process
	 * @param block true to block until the publish process is stopped
	 * @return true if the publish process was stopped, false if it was not running
	 */
	public static synchronized boolean stopPublishLocally(boolean block) {
		if (!isRunningLocally()) {
			return false;
		}
		logger.info("Stopping publish run.");
		state = State.cancelled;
		runningPublisher.getThreadGroup().interrupt();
		runningPublisher.interrupt();
		if (block) {
			try {
				runningPublisher.join();
			} catch (InterruptedException e) {
				logger.error("Thread got interrupted while waiting for publish thread to exit.", e);
			}
		}
		return true;
	}

	/**
	 * Wait for a running publish process to exit.
	 * @return true when a publish process was running and exited, false if not
	 */
	public static boolean joinPublisherLocally() {
		if (!isRunningLocally()) {
			return false;
		}
		try {
			runningPublisher.join();
			return true;
		} catch (InterruptedException e) {
			logger.error("Thread got interrupted while waiting for publish thread to exit.", e);
			return false;
		}
	}

	/**
	 * get a publishinfo of the currently running publisher, or null if no publisher is running.
	 * @return a publishinfo object, or null if not running.
	 */
	public static PublishInfo getPublishInfo() {
		try {
			return DistributionUtil.call(new GetPublishInfoTask());
		} catch (Exception e) {
			logger.error("Error while getting publish info", e);
			return null;
		}
	}

	/**
	 * get a publishinfo of the previous publish process, or null if no publish process has finished yet.
	 * @return a publishinfo object, or null if not no previous publish process has run
	 */
	public static PublishInfo getPreviousPublishInfo() {
		try {
			return DistributionUtil.call(new GetPreviousPublishInfoTask());
		} catch (Exception e) {
			logger.error("Error while getting previous publish info", e);
			return null;
		}
	}

	/**
	 * "resets" the publisher by simply removing the reference to the publisher.
	 *
	 * @return true if reference was removed, false if publisher is still running.
	 */
	public static synchronized boolean resetPublisher() {
		if (!isRunning()) {
			runningPublisher = null;
			publisher = null;
			return true;
		}
		return false;
	}

	/**
	 * Get the verbose publish log of the current publisher
	 * @return publish log file or null
	 */
	public static File getPublishLog() {
		if (publisher != null) {
			return publisher.getLogFile(true);
		} else {
			return null;
		}
	}

	/**
	 * Mark the given object as instant published.
	 *
	 * During the publish process, {@link #wasInstantPublished}
	 * should be used to determine whether or not to publish
	 * an object, because it was already instant published.
	 *
	 * @param object The instant published object.
	 */
	public static void instantPublished(NodeObject object) {
		for (InstantPublishService service : instantPublishServiceLoader) {
			service.succeeded(object.getTType(), object.getId());
		}

		if (!isRunning() || publisher == null) {
			return;
		}

		publisher.instantPublished(object);
	}

	/**
	 * Revoke a prior call to {@link #instantPublished}, because
	 * the actual instant publication failed.
	 *
	 * @param object The object that could not be instant published.
	 */
	public static void instantPublishFailed(NodeObject object) {
		for (InstantPublishService service : instantPublishServiceLoader) {
			service.failed(object.getTType(), object.getId());
		}

		if (!isRunning() || publisher == null) {
			return;
		}

		publisher.instantPublishFailed(object);
	}

	/**
	 * Check whether the given object was instant published
	 * since the current publish run started.
	 *
	 * @param object The object to check.
	 * @return <code>true</code> if the object was instant
	 *		published since the current publish run started,
	 *		and <code>false</code> otherwise.
	 */
	public static boolean wasInstantPublished(NodeObject object) {
		if (!isRunning() || publisher == null) {
			return false;
		}

		return publisher.wasInstantPublished(object);
	}

	/**
	 * Check whether the given object was instant published
	 * since the current publish run started.
	 *
	 * @param objType object type
	 * @param objId object ID
	 * @return <code>true</code> if the object was instant
	 *		published since the current publish run started,
	 *		and <code>false</code> otherwise.
	 */
	public static boolean wasInstantPublished(int objType, int objId) {
		if (!isRunning() || publisher == null) {
			return false;
		}

		return publisher.wasInstantPublished(objType, objId);
	}

	/**
	 * Get the state of the publisher process
	 * @return state of the publisher process
	 */
	public static State getState() {
		if (!isRunning() || publisher == null) {
			state = State.stopped;
		}

		return state;
	}

	/**
	 * Change the state from {@link State#init} to {@link State#running}
	 */
	public static void setRunning() {
		if (state == State.init) {
			state = State.running;
		}
	}

	/**
	 * Change the state from {@link State#init} or {@link State#running} to {@link State#error}
	 * @param error error that occurred
	 */
	public static void setError(Throwable error) {
		switch (state) {
		case init:
		case running:
			state = State.error;
			if (publisher != null) {
				publisher.setError(error);
			}
			if (runningPublisher != null && !runningPublisher.isInterrupted()) {
				runningPublisher.interrupt();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Change the state to {@link State#stopped}
	 */
	public static void setStopped() {
		state = State.stopped;
	}

	/**
	 * Get prepared PublishData or null
	 * @return PublishData instance or null
	 */
	public static PublishData getPublishData() {
		if (publisher != null) {
			return publisher.getPublishData();
		} else {
			return null;
		}
	}

	/**
	 * Callable to check whether publisher is running
	 */
	public static class CheckRunStatusTask implements Callable<Boolean>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 5516221027740604112L;

		@Override
		public Boolean call() throws Exception {
			return isRunningLocally();
		}
	}

	/**
	 * Callable to start the publisher
	 */
	public static class StartPublisherTask implements Callable<Boolean>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -7844471768887796758L;

		protected boolean force;

		protected long timestamp;

		/**
		 * Create instance of the callable
		 * @param force true, if single-page errors should be ignored.
		 * @param timestamp timestamp of the publish transaction
		 */
		public StartPublisherTask(boolean force, long timestamp) {
			this.force = force;
			this.timestamp = timestamp;
		}

		@Override
		public Boolean call() throws Exception {
			// run the publisher in a new thread
			publisher = new Publisher(force);
			publisher.setTimestamp(timestamp);

			previousPublishInfo = currentPublishInfo;
			currentPublishInfo = publisher.getPublishInfo();

			AttributedThreadGroup threadGroup = new AttributedThreadGroup("Publisher thread group");

			state = State.init;
			runningPublisher = new Thread(threadGroup, publisher, "Publisher Thread");
			runningPublisher.start();

			return true;
		}
	}

	/**
	 * Callable to stop the publisher
	 */
	public static class StopPublisherTask implements Callable<Boolean>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 1741276836789795546L;

		@Override
		public Boolean call() throws Exception {
			return stopPublishLocally(false);
		}
	}

	/**
	 * Task to get the publish info from the running publisher
	 */
	public static class GetPublishInfoTask implements Callable<PublishInfo>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -5277549110742050647L;

		@Override
		public PublishInfo call() throws Exception {
			return currentPublishInfo;
		}
	}

	/**
	 * Task to get the publish info from the previous publish process (may be {@code null}).
	 */
	public static class GetPreviousPublishInfoTask implements Callable<PublishInfo>, Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 5059862163345120751L;

		@Override
		public PublishInfo call() throws Exception {
			return previousPublishInfo;
		}
	}

	/**
	 * Possible states of the publish process
	 */
	public static enum State {
		/**
		 * The publish process is not running (publisher thread and all other threads have been stopped)
		 */
		stopped,

		/**
		 * The publish process is initializing (publisher thread has been started)
		 */
		init,

		/**
		 * The publish process is currently running, no errors have been encountered
		 */
		running,

		/**
		 * The publish process is running, but is about to be cancelled
		 */
		cancelled,

		/**
		 * The publish process is running, but errors have been encountered (publish process must be stopped)
		 */
		error
	}
}
