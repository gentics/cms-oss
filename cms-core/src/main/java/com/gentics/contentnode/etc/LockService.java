package com.gentics.contentnode.etc;

import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for locking implementations
 */
public interface LockService {
	/**
	 * Acquire the lock with the given key (must be released by calling {@link #release(String)}) after that
	 * @param key lock key
	 * @throws InterruptedException
	 */
	void acquire(String key) throws InterruptedException;

	/**
	 * Try to acquire the given lock for some time.
	 * If acquiring succeeds, the lock must be released by calling {@link #release(String)}.
	 * @param key lock key
	 * @param timeout wait timeout
	 * @param unit unit of the wait timeout
	 * @throws NodeException if acquiring failed (e.g. due to timeout)
	 * @throws InterruptedException
	 */
	void acquire(String key, long timeout, TimeUnit unit) throws NodeException, InterruptedException;

	/**
	 * Release the given lock, which was acquired by either {@link #acquire(String)} or {@link #acquire(String, long, TimeUnit)}
	 * @param key lock key
	 */
	void release(String key);
}
