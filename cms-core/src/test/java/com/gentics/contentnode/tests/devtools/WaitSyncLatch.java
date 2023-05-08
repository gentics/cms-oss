package com.gentics.contentnode.tests.devtools;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.QueueWithDelay;

/**
 * Autoclosable implementation that will wait until the Synchronizer does not handle any more events for some time
 */
public class WaitSyncLatch implements AutoCloseable {
	/**
	 * UUID of the listeners
	 */
	protected Set<UUID> listeners = new HashSet<>();

	/**
	 * Internal queue for handling the delay. Every time an event is handled by the synchronizer, an object is put into this queue,
	 * which will reset the delay counter. In the {@link #close()} method, the element is taken out of the queue, which will block
	 * until no more events are handled within the given delay.
	 */
	protected QueueWithDelay<Object> queue;

	/**
	 * Timeout for waiting for the first event to be handled
	 */
	protected long timeout;

	/**
	 * Time unit for delay and timeout
	 */
	protected TimeUnit unit;

	/**
	 * Create an instance
	 * 
	 * @param delay
	 *            delay in the given unit for handled events. The autoclosable
	 *            will wait in the {@link #close()} method until no more events
	 *            are handled within the given delay.
	 * @param timeout
	 *            wait timeout for the first event to occur.
	 * @param unit
	 *            time unit for delay and timeout
	 */
	public WaitSyncLatch(long delay, long timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
		queue = new QueueWithDelay<>(delay, unit);
		listeners.add(Synchronizer.addListenerToFS((object, path) -> {
			queue.remove(this);
			queue.put(this);
		}));
		listeners.add(Synchronizer.addListenerFromFS((object, path) -> {
			queue.remove(this);
			queue.put(this);
		}));
		listeners.add(Synchronizer.addPackageListener((name, event) -> {
			queue.remove(this);
			queue.put(this);
		}));
	}

	@Override
	public void close() throws Exception {
		try {
			if (queue.poll(timeout, unit) == null) {
				if (queue.isEmpty()) {
					fail("Failed to handle a single event within timeout");
				} else {
					queue.take();
				}
			}
		} finally {
			listeners.forEach(uuid -> Synchronizer.removeListener(uuid));
		}
	}
}
