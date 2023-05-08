package com.gentics.contentnode.etc;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Queue implementation, that only lets the consumer {@link #take()} elements, if no elements were {@link #put(Object)}
 * within the given delay
 *
 * @param <U> type of the elements in the queue
 */
public class QueueWithDelay<U> {
	/**
	 * Time in nanoseconds, when the last object was put into the queue
	 */
	private long lastPutNs = 0;

	/**
	 * Delay in nanoseconds
	 */
	private long delayNs = 0;

	/**
	 * Internal queue
	 */
	private BlockingQueue<DelayedElement> queue = new DelayQueue<>();

	/**
	 * Optional comparator for sorting elements
	 */
	private Comparator<U> comparator;

	/**
	 * Create an instance with the given delay
	 * @param delay delay
	 * @param unit time unit
	 */
	public QueueWithDelay(long delay, TimeUnit unit) {
		this(delay, unit, null);
	}

	/**
	 * Create an instance with the given delay and a comparator for the elements put into the queue.
	 * Elements will be taken out of the queue in the order of the comparator (if one given)
	 * @param delay delay
	 * @param unit time unit
	 * @param comparator optional comparator (may be null)
	 */
	public QueueWithDelay(long delay, TimeUnit unit, Comparator<U> comparator) {
		this.delayNs = TimeUnit.NANOSECONDS.convert(delay, unit);
		this.comparator = comparator;
	}

	/**
	 * Put the element into the queue
	 * @param element element
	 */
	public void put(U element) {
		lastPutNs = System.nanoTime();
		queue.add(new DelayedElement(element, lastPutNs));
	}

	/**
	 * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
	 * @return the head of the queue
	 * @throws InterruptedException
	 */
	public U take() throws InterruptedException {
		return queue.take().element;
	}

	/**
	 * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
	 * @param timeout timeout
	 * @param unit unit
	 * @return the head of the queue or null if the timeout occurs
	 * @throws InterruptedException
	 */
	public U poll(long timeout, TimeUnit unit) throws InterruptedException {
		QueueWithDelay<U>.DelayedElement delayed = queue.poll(timeout, unit);
		return delayed != null ? delayed.element : null;
	}

	/**
	 * Removes a single instance of this element from the queue
	 * @param element element to remove
	 * @return true, iff the queue changed
	 */
	public boolean remove(U element) {
		return queue.remove(element);
	}

	/**
	 * Return true when the queue contains the given element
	 * @param element element to check
	 * @return true iff the queue contains the element
	 */
	public boolean contains(U element) {
		return queue.contains(element);
	}

	/**
	 * Returns <tt>true</tt> if this collection contains no elements.
	 *
	 * @return <tt>true</tt> if this collection contains no elements
	 */
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	@Override
	public String toString() {
		return String.format("delay %d, queue %s", delayNs, queue);
	}

	/**
	 * Internal implementation of elements in the {@link QueueWithDelay#queue}
	 *
	 * @param <V> type of the wrapped element
	 */
	private class DelayedElement implements Delayed {
		/**
		 * Wrapped element
		 */
		U element;

		/**
		 * Time this object was put into the queue in nanoseconds
		 */
		long putTimeNs;

		/**
		 * Create an instance
		 * @param element wrapped element
		 * @param putTimeNs put time in nanoseconds
		 */
		private DelayedElement(U element, long putTimeNs) {
			this.element = element;
			this.putTimeNs = putTimeNs;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Delayed o) {
			if (o instanceof QueueWithDelay.DelayedElement) {
				if (comparator != null) {
					return comparator.compare(element, ((DelayedElement) o).element);
				} else {
					return Long.compare(putTimeNs, ((DelayedElement) o).putTimeNs);
				}
			}
			return 0;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof QueueWithDelay.DelayedElement) {
				return element.equals(((DelayedElement) obj).element);
			} else {
				return element.equals(obj);
			}
		}

		@Override
		public long getDelay(TimeUnit unit) {
			// the delay reaches 0, when the last put is longer than delayNs in the past
			return unit.convert(Math.max(delayNs - System.nanoTime() + lastPutNs, 0), TimeUnit.NANOSECONDS);
		}

		@Override
		public String toString() {
			return String.format("%s @%d", element, putTimeNs);
		}
	}
}
