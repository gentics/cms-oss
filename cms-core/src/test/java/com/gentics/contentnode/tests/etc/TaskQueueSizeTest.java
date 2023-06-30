package com.gentics.contentnode.tests.etc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.contentnode.etc.TaskQueueSize;

/**
 * Test cases for the {@link TaskQueueSize}
 */
public class TaskQueueSizeTest {
	/**
	 * Executor service
	 */
	public static ExecutorService service = Executors.newCachedThreadPool();

	/**
	 * Test {@link TaskQueueSize#awaitEmpty()}
	 * @throws InterruptedException
	 */
	@Test
	public void testAwaitEmpty() throws InterruptedException {
		// create instance
		TaskQueueSize taskQueue = new TaskQueueSize();

		// "schedule" 5 tasks
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();

		CountDownLatch counter = new CountDownLatch(1);
		// let another thread wait for the taskQueue to become empty
		AtomicBoolean empty = new AtomicBoolean(false);
		service.submit(() -> {
			try {
				taskQueue.awaitEmpty();
				empty.set(true);
				counter.countDown();
			} catch (InterruptedException e) {
			}
		});

		// finish a task, 4 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(empty.get()).as("Task queue is empty").isFalse();

		// finish a task, 3 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(empty.get()).as("Task queue is empty").isFalse();

		// finish a task, 2 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(empty.get()).as("Task queue is empty").isFalse();

		// finish a task, 1 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(empty.get()).as("Task queue is empty").isFalse();

		// after finishing the last task, the queue should be empty
		taskQueue.finish();
		counter.await(1, TimeUnit.SECONDS);
		assertThat(empty.get()).as("Task queue is empty").isTrue();
	}

	/**
	 * Test {@link TaskQueueSize#awaitNotFull()}
	 * @throws InterruptedException
	 */
	@Test
	public void testAwaitNotFull() throws InterruptedException {
		// create instance, which is "full" with 3 tasks
		TaskQueueSize taskQueue = new TaskQueueSize(3);

		// "schedule" 5 tasks
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();
		taskQueue.schedule();

		CountDownLatch counter = new CountDownLatch(1);
		// let another thread wait for the taskQueue to become "not full"
		AtomicBoolean notFull = new AtomicBoolean(false);
		service.submit(() -> {
			try {
				taskQueue.awaitNotFull();
				notFull.set(true);
				counter.countDown();
			} catch (InterruptedException e) {
			}
		});

		// finish a task, 4 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(notFull.get()).as("Task queue is not full").isFalse();

		// finish a task, 3 remaining
		taskQueue.finish();
		Thread.sleep(100);
		assertThat(notFull.get()).as("Task queue is not full").isFalse();

		// finish a task, 2 remaining
		taskQueue.finish();
		counter.await(1, TimeUnit.SECONDS);
		assertThat(notFull.get()).as("Task queue is not full").isTrue();
	}
}
