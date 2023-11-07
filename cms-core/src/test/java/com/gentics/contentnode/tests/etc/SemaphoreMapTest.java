package com.gentics.contentnode.tests.etc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.SemaphoreMap;

/**
 * Test cases for {@link SemaphoreMap}
 */
public class SemaphoreMapTest {
	/**
	 * Test {@link SemaphoreMap#locked(Object, long, TimeUnit)}
	 * @throws NodeException
	 */
	@Test
	public void testLock() throws NodeException {
		int lockKey = 4711;
		SemaphoreMap<Integer> map = new SemaphoreMap<>("test");
		map.init(lockKey);

		assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(1);

		map.acquire(lockKey, 1, TimeUnit.SECONDS);
		try {
			assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(0);
		} finally {
			map.release(lockKey);
		}

		assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(1);
	}

	/**
	 * Test that interrupting a thread holding the lock will release the lock
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	@Test
	public void testInterrupt() throws NodeException, InterruptedException {
		int lockKey = 4711;
		SemaphoreMap<Integer> map = new SemaphoreMap<>("test");
		map.init(lockKey);

		ExecutorService service = Executors.newFixedThreadPool(1);
		AtomicBoolean success = new AtomicBoolean(false);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch end = new CountDownLatch(1);
		try {
			// submit a callable, which will acquire a lock, the sleep for 10 seconds and set the success flag
			Future<Boolean> result = service.submit(() -> {
				map.acquire(lockKey, 1, TimeUnit.SECONDS);
				try {
					start.countDown();
					Thread.sleep(10 * 1000);
					success.set(true);
					return true;
				} finally {
					map.release(lockKey);
					end.countDown();
				}
			});

			// wait for the callable to be started
			start.await(5, TimeUnit.SECONDS);
			assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(0);

			// cancel the callable (and let the thread be interrupted)
			result.cancel(true);
			end.await(5, TimeUnit.SECONDS);

			// lock must have been released
			assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(1);
			assertThat(success.get()).as("Successful operation").isFalse();
		} finally {
			service.shutdownNow();
		}
	}

	/**
	 * Test throwing an exception while owning a lock
	 * @throws NodeException
	 */
	@Test
	public void testThrow() throws NodeException {
		int lockKey = 4711;
		SemaphoreMap<Integer> map = new SemaphoreMap<>("test");
		map.init(lockKey);

		map.acquire(lockKey);
		try {
			assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(0);
			throw new NodeException("Failure");
		} catch (NodeException e) {
			assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(0);
		} finally {
			map.release(lockKey);
		}
		assertThat(map.availablePermits(lockKey)).as("Available permits").isEqualTo(1);
	}
}
