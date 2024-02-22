package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * {@link AutoCloseable} implementation that measures the duration between creation and {@link Timing#close()}.
 * If the duration is bigger than the threshold (which defaults to 0), the consumer is called with the duration
 */
public class Timing implements AutoCloseable {
	/**
	 * Threshold in ms
	 */
	private long threshold = 0;

	/**
	 * Consumer to be called with the duration
	 */
	private Consumer<Long> consumer;

	/**
	 * Start Time (in ms)
	 */
	private long startTime;

	/**
	 * Get an instance with threshold 0 (consumer will always be called)
	 * @param consumer consumer, which will get called with the duration in ms
	 * @return instance
	 */
	public static Timing get(Consumer<Long> consumer) {
		return get(0, consumer);
	}

	/**
	 * Get an instance with threshold and consumer
	 * @param threshold threshold in ms
	 * @param consumer consumer, which will get called with the duration in ms
	 * @return instance
	 */
	public static Timing get(long threshold, Consumer<Long> consumer) {
		return new Timing(threshold, consumer);
	}

	/**
	 * Create instance
	 * @param threshold threshold in ms
	 * @param consumer consumer
	 */
	private Timing(long threshold, Consumer<Long> consumer) {
		this.threshold = threshold;
		this.consumer = consumer;
		startTime = System.currentTimeMillis();
	}

	@Override
	public void close() throws NodeException {
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		if (consumer != null && duration > threshold) {
			consumer.accept(duration);
		}
	}
}
