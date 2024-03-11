package com.gentics.contentnode.etc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of a semaphore map, that can be used to synchonize access to resources identified by keys of type T
 * The synchronization will also be done with all available instances of {@link LockService}
 *
 * @param <T> key class
 */
public class SemaphoreMap <T> {
	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(SemaphoreMap.class);

	/**
	 * Loader for {@link LockService} implementations
	 */
	protected static ServiceLoader<LockService> lockServiceLoader = ServiceLoader.load(LockService.class);

	/**
	 * Map holding the semaphores
	 */
	protected final Map<T, Semaphore> semaphoreMap = new HashMap<>();

	/**
	 * Map holding SemaphoreAcquirer information
	 */
	protected final Map<T, SemaphoreAcquirer> semaphoreAcquirerMap = new HashMap<>();

	/**
	 * SemaphoreMap name
	 */
	protected String name;

	/**
	 * Lock Key Pattern
	 */
	protected String lockKeyPattern;

	/**
	 * Create an instance with the given name
	 * @param name map name
	 */
	public SemaphoreMap(String name) {
		this.name = name;
		lockKeyPattern = name + "_%s";
	}

	/**
	 * Initialize the map with the given key.
	 * This will create a Semaphore (if not done before)
	 * @param key key
	 */
	public synchronized void init(T key) {
		semaphoreMap.computeIfAbsent(key, k -> new Semaphore(1, true));
	}

	/**
	 * Try to acquire the semaphore
	 * @param key semaphore key
	 * @throws TransactionException
	 */
	public void acquire(T key) throws TransactionException {
		long startTime = System.currentTimeMillis();
		String lockKey = getLockKey(key);
		try {
			for (LockService service : lockServiceLoader) {
				service.acquire(lockKey);
			}
			semaphoreMap.get(key).acquire();
			semaphoreAcquirerMap.put(key, new SemaphoreAcquirer(Thread.currentThread().getName(), Thread.currentThread().getStackTrace()));
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Waited %d ms for semaphore %s for %s", (System.currentTimeMillis() - startTime), this.name, key));
			}
		} catch (InterruptedException e) {
			SemaphoreAcquirer acquirer = semaphoreAcquirerMap.get(key);
			logger.error(String.format("Waited %d ms for semaphore %s for %s, before getting interrupted", (System.currentTimeMillis() - startTime), this.name, key));
			if (acquirer != null) {
				logger.error(String.format("Last acquirer %s", acquirer));
			}
			for (LockService service : lockServiceLoader) {
				service.release(lockKey);
			}
			throw new TransactionException(String.format("Interrupted while waiting for semaphore %s for %s", this.name, key));
		}
	}

	/**
	 * Try to acquire the semaphore with timeout
	 * @param key semaphore key
	 * @param timeout timeout
	 * @param unit timeout unit
	 * @throws NodeException
	 */
	public void acquire(T key, long timeout, TimeUnit unit) throws NodeException {
		long startTime = System.currentTimeMillis();
		String lockKey = getLockKey(key);
		try {
			for (LockService service : lockServiceLoader) {
				service.acquire(lockKey, timeout, unit);
			}
			if (!semaphoreMap.get(key).tryAcquire(timeout, unit)) {
				for (LockService service : lockServiceLoader) {
					service.release(lockKey);
				}
				SemaphoreAcquirer acquirer = semaphoreAcquirerMap.get(key);
				throw new NodeException(String.format("Timeout of %d %s reached when trying to acquire lock %s for %s. Last Acquirer: %s", timeout, unit.name(), key, this.name, acquirer != null ? acquirer : "unknown"));
			}
			semaphoreAcquirerMap.put(key, new SemaphoreAcquirer(Thread.currentThread().getName(), Thread.currentThread().getStackTrace()));
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Waited %d ms for semaphore %s for %s", (System.currentTimeMillis() - startTime), this.name, key));
			}
		} catch (InterruptedException e) {
			SemaphoreAcquirer acquirer = semaphoreAcquirerMap.get(key);
			logger.error(String.format("Waited %d ms for semaphore %s for %s, before getting interrupted", (System.currentTimeMillis() - startTime), this.name, key));
			if (acquirer != null) {
				logger.error(String.format("Last acquirer %s", acquirer));
			}
			for (LockService service : lockServiceLoader) {
				service.release(lockKey);
			}
			throw new TransactionException(String.format("Interrupted while waiting for semaphore %s for %s", this.name, key));
		}
	}

	/**
	 * Release a semaphore
	 * @param key semaphore key
	 */
	public void release(T key) {
		semaphoreMap.get(key).release();
		String lockKey = getLockKey(key);
		for (LockService service : lockServiceLoader) {
			service.release(lockKey);
		}
	}

	/**
	 * Get the number of available permits for the semaphore for the given key
	 * @param key key
	 * @return available permits
	 */
	public int availablePermits(T key) {
		return Optional.ofNullable(semaphoreMap.get(key)).map(Semaphore::availablePermits).orElse(0);
	}

	/**
	 * Get the LockService lock key
	 * @param key semaphore key
	 * @return lock key
	 */
	protected String getLockKey(T key) {
		return String.format(lockKeyPattern, key);
	}

	/**
	 * Class to encapsulate information about the last Thread acquiring a specific semaphore
	 */
	protected static class SemaphoreAcquirer {
		/**
		 * Acquirer information (contains Thread name and stack trace)
		 */
		protected String info;

		/**
		 * Create an instance
		 * @param name thread name
		 * @param stackTrace stack trace
		 */
		public SemaphoreAcquirer(String name, StackTraceElement[] stackTrace) {
			StringBuilder info = new StringBuilder();
			info.append("Thread {").append(name).append("} @").append(System.currentTimeMillis()).append("\n");

			for (StackTraceElement element : stackTrace) {
				info.append("\t").append(element.toString()).append("\n");
			}

			this.info = info.toString();
		}

		@Override
		public String toString() {
			return info;
		}
	}
}
