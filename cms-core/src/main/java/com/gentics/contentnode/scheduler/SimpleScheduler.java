package com.gentics.contentnode.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.lib.log.NodeLogger;

/**
 * Simple Scheduler implementation that uses a single thread to execute periodic tasks
 */
public class SimpleScheduler {
	/**
	 * Executor service
	 */
	protected static final Map<String, ScheduledExecutorService> executors = new HashMap<>();

	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(SimpleScheduler.class);

	/**
	 * Shutdown the executor.
	 */
	public static void shutdown() {
		logger.info("Shutting down all scheduler threads");

		synchronized (executors) {
			for (ScheduledExecutorService executor : executors.values()) {
				executor.shutdown();

				try {
					if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
						logger.error("Scheduler thread did not terminate in 10 seconds, forcing shutdown now");
						executor.shutdownNow();
					}
				} catch (InterruptedException e) {
					// Nothing more we can do here.
				}
			}

			executors.clear();
		}
	}

	/**
	 * Get the executor
	 * @return executor (never null)
	 * @throws NodeException if the executor is not started
	 */
	public static ScheduledExecutorService getExecutor(String name) throws NodeException {
		synchronized (executors) {
			ScheduledExecutorService executor = executors.get(name);

			// If the executor is not shut down and the poolsize did not change, we do nothing.
			if (executor != null && !executor.isShutdown()) {
				return executor;
			}

			logger.info("Starting new scheduler thread: " + name);

			// Start a new executor service for the specified name.
			executor = Executors.newSingleThreadScheduledExecutor(new PrefixedThreadFactory(name));
			executors.put(name, executor);

			return executor;
		}
	}
}
