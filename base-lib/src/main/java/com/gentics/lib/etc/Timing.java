package com.gentics.lib.etc;

import java.util.Stack;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link AutoCloseable} implementation that measures the duration between creation and {@link Timing#close()}.
 * If the duration is bigger than the threshold (which defaults to 0), the consumer is called with the duration
 */
public class Timing implements AutoCloseable {
	public static ThreadLocal<Stack<AutoCloseable>> loggingStack = ThreadLocal.withInitial(() -> new Stack<>());

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
		return get(-1, consumer);
	}

	public static Timing log(String description) {
		Timing t = get(-1, duration -> System.out.println("%s: %d ms".formatted(description, duration)));
		loggingStack.get().push(t);
		return t;
	}

	public static Timing subLog(String description) {
		if (loggingStack.get().isEmpty()) {
			return get(Long.MAX_VALUE, duration -> {});
		} else {
			return log("%s%s".formatted(StringUtils.repeat('\t', loggingStack.get().size()), description));
		}
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
	public void close() {
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		if (consumer != null && duration > threshold) {
			consumer.accept(duration);
		}
		loggingStack.get().remove(this);
	}
}
