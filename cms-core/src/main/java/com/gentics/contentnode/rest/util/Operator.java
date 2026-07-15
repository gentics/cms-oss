package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.ws.rs.WebApplicationException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Operator class, that executes instances of {@link Callable} in a thread pool.
 * The calling thread may wait up to a given amount of time for the job to execute. If the job takes longer, it will be continued in the background
 * and send a notification to the user, when finished.
 */
public class Operator {
	/**
	 * Pool size
	 */
	protected static int poolSize = 10;

	/**
	 * Thread factory
	 */
	protected static ThreadFactory threadFactory = new PrefixedThreadFactory("operator");

	/**
	 * Generator for the internal "jobId"
	 */
	protected static AtomicLong jobIdGenerator = new AtomicLong();

	/**
	 * Map of currently running jobs, keys are the internal jobIds
	 */
	protected static Map<Long, RestCallable<?>> runningJobs = new HashMap<>();

	/**
	 * Executor instance TODO make pool size configurable
	 */
	protected static ExecutorService executor = Executors.newFixedThreadPool(poolSize, threadFactory);

	protected static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, new PrefixedThreadFactory("scheduled-operator"));

	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(Operator.class);

	/**
	 * Start the executor thread pool with given size
	 * @param newPoolsize size of the pool
	 */
	public static void start(int newPoolsize) {
		// if the executor is not shut down and the poolsize did not change, we do nothing
		if (executor != null && !executor.isShutdown() && poolSize == newPoolsize) {
			return;
		}
		poolSize = newPoolsize;
		logger.info("Starting new threadpool with poolsize " + poolSize);

		// keep the old executorservice
		ExecutorService oldExecutor = executor;

		// start a new executorservice (which will be used from now on)
		executor = Executors.newFixedThreadPool(poolSize, threadFactory);

		// shut the old executor service down.
		if (oldExecutor != null && !oldExecutor.isShutdown()) {
			oldExecutor.shutdown();
		}
	}

	/**
	 * Submit the given callable to the executor. If timeout is non-zero, the calling thread will wait up to the given timeout (in ms) for the result.
	 * If the callable takes longer (or is executed later), a notification is sent to the user, if done.
	 * @param description Job description (must be i18n). This will be used in generic notification messages to the user.
	 * @param timeout timeout in ms. If 0, the calling thread will wait, until the callable is executed.
	 * @param callable callable to execute
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return either the response from the callable (if executed in foreground) or a response containing a message, that the job is done in background
	 */
	public static <T extends GenericResponse> T execute(String description, long timeout, Callable<T> callable, Function<GenericResponse, T> responseTransformer) {
		return executeLocked(description, timeout, null, callable, null, null, responseTransformer);
	}

	/**
	 * Version of {@link #execute(String, long, Callable)} that will lock access to the callable with the given lock
	 * @param description Job description
	 * @param timeout timeout in ms
	 * @param lock optional lock
	 * @param callable callable to execute
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return response
	 */
	public static <T extends GenericResponse> T executeLocked(String description, long timeout, Lock lock, Callable<T> callable, Function<GenericResponse, T> responseTransformer) {
		return executeLocked(description, timeout, lock, callable, null, null, responseTransformer);
	}

	/**
	 * Version of {@link #execute(String, long, Callable)} that will lock access to the callable with the given lock
	 * @param description Job description
	 * @param timeout timeout in ms
	 * @param lock optional lock
	 * @param callable callable to execute
	 * @param errorHandler optional error handler
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return response
	 */
	public static <T extends GenericResponse> T executeLocked(String description, long timeout, Lock lock, Callable<T> callable, Function<Exception, WebApplicationException> errorHandler, Function<GenericResponse, T> responseTransformer) {
		return executeLocked(description, timeout, lock, callable, errorHandler, null, responseTransformer);
	}

	/**
	 * Version of {@link #execute(String, long, Callable)} that will lock access to the callable with the given lock
	 * @param description Job description
	 * @param timeout timeout in ms
	 * @param lock optional lock
	 * @param callable callable to execute
	 * @param errorHandler optional error handler
	 * @param backgroundCallback optional callback that is called when the operation is sent to background
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return response
	 */
	public static <T extends GenericResponse> T executeLocked(String description, long timeout, Lock lock,
			Callable<T> callable, Function<Exception, WebApplicationException> errorHandler,
			Runnable backgroundCallback, Function<GenericResponse, T> responseTransformer) {
		try {
			RestCallable<T> wrapper = new RestCallable<>(description, lock, callable, responseTransformer);
			Future<T> futureResult = executor.submit(wrapper);

			try {
				if (timeout <= 0) {
					return futureResult.get();
				} else {
					return futureResult.get(timeout, TimeUnit.MILLISECONDS);
				}
			} catch (TimeoutException e) {
				wrapper.sendToBackground();
				I18nString msg = new CNI18nString("job_sent_to_background");
				msg.setParameter("0", description);
				if (backgroundCallback != null) {
					backgroundCallback.run();
				}
				String translatedMsg = msg.toString();

				return responseTransformer.apply(new GenericResponse(new Message(Type.INFO, translatedMsg), new ResponseInfo(ResponseCode.OK, translatedMsg)).setInBackground(true));
			}
		} catch (Exception exception) {
			if (exception.getCause() instanceof ReadOnlyException) {
				return responseTransformer.apply(new GenericResponse(new Message(Message.Type.CRITICAL, exception.getCause().getLocalizedMessage()), new ResponseInfo(ResponseCode.FAILURE, "")));
			} else {
				// for ExecutionExceptions, we are more interested in the causing exception
				if (exception instanceof ExecutionException && exception.getCause() instanceof Exception) {
					exception = (Exception) exception.getCause();
				}
				logger.error("Error while " + description, exception);
				if (errorHandler != null) {
					throw errorHandler.apply(exception);
				} else {
					I18nString message = new CNI18nString("rest.general.error");
					return responseTransformer.apply(new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, "Error while "
							+ description + ": " + exception.getLocalizedMessage())));
				}
			}
		}
	}

	/**
	 * Submit the given callable to the executor. If timeout is non-zero, the calling thread will wait up to the given timeout (in ms) for the result.
	 * If the callable takes longer (or is executed later), a notification is sent to the user, if done.
	 * @param description Job description (must be i18n). This will be used in generic notification messages to the user.
	 * @param timeout timeout in ms. If 0, the calling thread will wait, until the callable is executed.
	 * @param callable callable to execute
	 * @return either the response from the callable (if executed in foreground) or a response containing a message, that the job is done in background
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @throws NodeException
	 */
	public static <T extends GenericResponse> T executeRethrowing(String description, long timeout, Callable<T> callable, Function<GenericResponse, T> responseTransformer) throws NodeException {
		return executeLockedRethrowing(description, timeout, null, callable, responseTransformer);
	}

	/**
	 * Version of {@link #executeRethrowing(String, long, Callable)} that will lock access to the callable with the given lock
	 * @param description Job description
	 * @param timeout timeout in ms
	 * @param lock optional lock
	 * @param callable callable to execute
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return response
	 */
	public static <T extends GenericResponse> T executeLockedRethrowing(String description, long timeout, Lock lock, Callable<T> callable, Function<GenericResponse, T> responseTransformer) throws NodeException {
		try {
			RestCallable<T> wrapper = new RestCallable<>(description, lock, callable, responseTransformer);
			wrapper.setThrowNodeException(true);
			Future<T> futureResult = executor.submit(wrapper);

			try {
				if (timeout <= 0) {
					return futureResult.get();
				} else {
					return futureResult.get(timeout, TimeUnit.MILLISECONDS);
				}
			} catch (TimeoutException e) {
				wrapper.sendToBackground();
				I18nString msg = new CNI18nString("job_sent_to_background");
				msg.setParameter("0", description);
				String translatedMsg = msg.toString();

				return responseTransformer.apply(new GenericResponse(new Message(Type.INFO, translatedMsg), new ResponseInfo(ResponseCode.OK, translatedMsg)).setInBackground(true));
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException)e.getCause();
			}
			logger.error("Error while " + description, e);
			I18nString message = new CNI18nString("rest.general.error");
			return responseTransformer.apply(new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, "Error while "
					+ description + ": " + e.getLocalizedMessage())));
		}
	}

	/**
	 * Execute the given list of already wrapped callables
	 * @param description description
	 * @param timeout timeout
	 * @param wrappedCallables list of wrapped callables
	 * @param responseTransformer function to transform a GenericResponse into the response object
	 * @return merged response
	 */
	protected static <T extends GenericResponse> T execute(String description, long timeout, List<RestCallable<T>> wrappedCallables, Function<GenericResponse, T> responseTransformer) {
		if (wrappedCallables.isEmpty()) {
			return responseTransformer.apply(new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "")));
		}
		try {
			QueueResult queueResult = new QueueResult(description, wrappedCallables.size());
			List<Future<T>> futureResults = new ArrayList<>();
			for (RestCallable<T> wrapper : wrappedCallables) {
				wrapper.addToQueue(queueResult);
				futureResults.add(executor.submit(wrapper));
			}

			try {
				T merged = responseTransformer.apply(new GenericResponse());
				long remainingTimeout = timeout;
				for (Future<T> futureResult : futureResults) {
					if (timeout <= 0) {
						mergeInto(futureResult.get(), merged);
					} else {
						long startWait = System.currentTimeMillis();
						mergeInto(futureResult.get(remainingTimeout, TimeUnit.MILLISECONDS), merged);
						long waitedFor = System.currentTimeMillis() - startWait;
						remainingTimeout = Math.max(remainingTimeout - waitedFor, 0);
					}
				}

				return merged;
			} catch (TimeoutException e) {
				for (RestCallable<T> wrapper : wrappedCallables) {
					wrapper.sendToBackground();
				}
				queueResult.sendToBackground();

				I18nString msg = new CNI18nString("job_sent_to_background");
				msg.setParameter("0", description);
				String translatedMsg = msg.toString();
				return responseTransformer.apply(new GenericResponse(new Message(Type.INFO, translatedMsg), new ResponseInfo(ResponseCode.OK, translatedMsg)).setInBackground(true));
			}
		} catch (Exception e) {
			logger.error("Error while " + description, e);
			I18nString message = new CNI18nString("rest.general.error");
			return responseTransformer.apply(new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, "Error while "
					+ description + ": " + e.getLocalizedMessage())));
		}
	}

	/**
	 * Merge the response into the given merged response
	 * @param response response to merge
	 * @param merged response to merge into
	 */
	protected static void mergeInto(GenericResponse response, GenericResponse merged) {
		if (merged.getResponseInfo() == null) {
			merged.setResponseInfo(response.getResponseInfo());
		} else if (merged.getResponseInfo().getResponseCode() == ResponseCode.OK && response.getResponseInfo() != null
				&& response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			merged.setResponseInfo(response.getResponseInfo());
		}

		if (!ObjectTransformer.isEmpty(response.getMessages())) {
			for (Message msg : response.getMessages()) {
				merged.addMessage(msg);
			}
		}
	}

	/**
	 * Shutdown the executor.
	 */
	public static void shutdown() {
		logger.info("Shutdown initiated");
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				logger.warn("Thread pool did not terminate in 10 seconds, forcing shutdown now");
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
		}
		scheduledExecutor.shutdown();
		try {
			if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				logger.warn("Scheduled Executor did not terminate in 10 seconds, forcing shutdown now");
				scheduledExecutor.shutdownNow();
	}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Get the executor
	 * @return executor
	 */
	public static ExecutorService getExecutor() {
		return executor;
	}

	/**
	 * Get the scheduled executor
	 * @return scheduled executor
	 */
	public static ScheduledExecutorService getScheduledExecutor() {
		return scheduledExecutor;
	}


	/**
	 * Create a new queue builder
	 * @return queue builder
	 */
	public static <T extends GenericResponse> QueueBuilder<T> queue(Function<GenericResponse, T> responseTransformer) {
		return new QueueBuilder<>(responseTransformer);
	}

	/**
	 * Create a lock with given type and key. All parameters must be non-null
	 * @param type lock type
	 * @param key lock key
	 * @return lock
	 * @throws NodeException
	 */
	public static Lock lock(LockType type, Object key) throws NodeException {
		if (type == null || key == null) {
			throw new NodeException("Cannot create lock without type and key");
		}
		return new Lock(type, key);
	}

	/**
	 * Called, when the given job is about to be called
	 * @param job job
	 */
	protected static void jobIsStarting(RestCallable<?> job) {
		runningJobs.put(job.jobId, job);
	}

	/**
	 * Called, when the give job finished execution
	 * @param job job
	 */
	protected static void jobFinished(RestCallable<?> job) {
		runningJobs.remove(job.jobId);
	}

	/**
	 * Get the currently running jobs
	 * @return collection of {@link RestCallable} wrappers for the currently running jobs
	 */
	public static Collection<RestCallable<?>> getCurrentlyRunningJobs() {
		return new ArrayList<>(runningJobs.values());
	}

	/**
	 * Queue Builder for building queues of operators.
	 */
	public static class QueueBuilder<T extends GenericResponse> {
		/**
		 * List of callable wrappers
		 */
		protected List<RestCallable<T>> wrappers = new ArrayList<>();

		/**
		 * Flag to mark whether the queue has been executed
		 */
		protected boolean executed = false;

		/**
		 * Response transformer
		 */
		protected Function<GenericResponse, T> responseTransformer;

		/**
		 * Create an instance
		 * @param responseTransformer response transformer
		 */
		protected QueueBuilder(Function<GenericResponse, T> responseTransformer) {
			this.responseTransformer = responseTransformer;
		}

		/**
		 * Add the given callable to the queue
		 * @param description description
		 * @param callable callable
		 * @return builder instance
		 * @throws NodeException
		 */
		public QueueBuilder<T> add(String description, Callable<T> callable) throws NodeException {
			return addLocked(description, null, callable);
		}

		/**
		 * Add the given callable to the queue with a lock
		 * @param description description
		 * @param lock lock
		 * @param callable callable
		 * @return builder instance
		 * @throws NodeException
		 */
		public QueueBuilder<T> addLocked(String description, Lock lock, Callable<T> callable) throws NodeException {
			if (executed) {
				throw new NodeException("Cannot add callable after queue has been executed");
			}
			wrappers.add(new RestCallable<>(description, lock, callable, responseTransformer));
			return this;
		}

		/**
		 * Let the operator execute all added callables. Wait for the results (with the given timeout in ms)
		 * @param description description
		 * @param timeout timeout (0 for no timeout)
		 * @return merged response
		 * @throws NodeException in case of an error
		 */
		public T execute(String description, long timeout) throws NodeException {
			if (executed) {
				throw new NodeException("The queue has already been executed");
			}

			executed = true;
			return Operator.execute(description, timeout, wrappers, responseTransformer);
		}
	}

	/**
	 * Queue result. Instances of this class will handle the responses of Callables in a queue.
	 * If all Callables are finished (at least one in the background), a single message will be sent to the user
	 */
	public static class QueueResult {
		protected String description;

		protected Integer userId;

		protected int queueSize = 0;

		protected int resultCounter = 0;

		/**
		 * Flag to mark, whether the job queue was sent to the background
		 */
		protected boolean background = false;

		protected GenericResponse mergedResponse = new GenericResponse(null, null);

		protected QueueResult(String description, int queueSize) throws NodeException {
			this.userId = TransactionManager.getCurrentTransaction().getUserId();
			this.description = description;
			this.queueSize = queueSize;
		}

		/**
		 * Send this job queue to the background. Once the job is finished, instant
		 * messages will be sent to the user
		 */
		public void sendToBackground() {
			background = true;
		}

		public synchronized void handleResponse(GenericResponse response) throws NodeException {
			mergeInto(response, mergedResponse);
			resultCounter++;

			if (resultCounter >= queueSize && background) {
				TransactionManager.execute(new Executable() {
					@Override
					public void execute() throws NodeException {
						Transaction t = TransactionManager.getCurrentTransaction();
						MessageSender messageSender = new MessageSender();
						t.addTransactional(messageSender);
						if (mergedResponse.getResponseInfo().getResponseCode() == ResponseCode.OK && mergedResponse.getMessages().isEmpty()) {
							CNI18nString messageText = new CNI18nString("backgroundjob_finished_successfully");

							messageText.addParameter(description);
							com.gentics.contentnode.messaging.Message message = new com.gentics.contentnode.messaging.Message(1, userId, messageText.toString(), RestCallable.INSTANT_TIME);
							messageSender.sendMessage(message);
						}

						for (com.gentics.contentnode.rest.model.response.Message restMessage : mergedResponse.getMessages()) {
							User sender = restMessage.getSender();
							int senderId = sender != null ? sender.getId() : 1;
							messageSender.sendMessage(new com.gentics.contentnode.messaging.Message(senderId, userId, restMessage.getMessage(), RestCallable.INSTANT_TIME));
						}
					}
				});
			}
		}
	}

	/**
	 * Lock implementation for synchronized access in callables
	 */
	public static class Lock {
		/**
		 * Lock type
		 */
		protected LockType type;

		/**
		 * Lock key
		 */
		protected Object key;

		/**
		 * Create an instance
		 * @param type lock type
		 * @param key lock key
		 */
		protected Lock(LockType type, Object key) {
			this.type = type;
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Lock) {
				Lock other = (Lock)obj;
				return this.type == other.type && ObjectTransformer.equals(key, other.key);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return type.hashCode() + key.hashCode();
		}
	}

	/**
	 * Enum of possible lock types
	 */
	public static enum LockType {
		channelSet, contentSet, contentPackage, devtoolPackage, fileName, motherId
	}
}
