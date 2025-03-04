package com.gentics.contentnode.etc;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper for collecting and logging execution durations
 */
public class TimingStats {
	/**
	 * Description of the timed execution
	 */
	protected String description;

	/**
	 * Logger to be used for logging
	 */
	protected NodeLogger logger;

	/**
	 * Counter for the number of invocations
	 */
	protected AtomicLong invocations = new AtomicLong();

	/**
	 * Counter for the total duration in ms
	 */
	protected AtomicLong totalTimeMs = new AtomicLong();

	/**
	 * Log level for logging invocations
	 */
	protected Level invocationLevel = Level.TRACE;

	/**
	 * Log level for logging statistics
	 */
	protected Level statisticsLevel = Level.DEBUG;

	/**
	 * Create an empty instance
	 */
	public TimingStats() {
	}

	/**
	 * Set the description
	 * @param description description
	 * @return fluent API
	 */
	public TimingStats as(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Set the logger to be used
	 * @param logger logger
	 * @return fluent API
	 */
	public TimingStats withLogger(NodeLogger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Set the log level for logging invocations (must not be null)
	 * @param level log level
	 * @return fluent API
	 */
	public TimingStats logInvocationsAs(@Nonnull Level level) {
		Objects.requireNonNull(level, "Log level must not be null");
		this.invocationLevel = level;
		return this;
	}

	/**
	 * Set the log level for logging statistics (must not be null)
	 * @param level log level
	 * @return fluent API
	 */
	public TimingStats logStatisticsAs(@Nonnull Level level) {
		Objects.requireNonNull(level, "Log level must not be null");
		this.statisticsLevel = level;
		return this;
	}

	/**
	 * Get an instance of {@link Timing} which must be used to measure the execution of some action (in a try-with-resources block):
	 * <pre>
	 * try (Timing timing = stats.withTiming() {
	 *    ... execute something
	 * }
	 * </pre>
	 * @return Timing instance
	 */
	public Timing withTiming() {
		return Timing.get(-1, duration -> {
			invocations.incrementAndGet();
			totalTimeMs.addAndGet(duration);
			if (logger != null && logger.isEnabled(invocationLevel)) {
				logger.log(invocationLevel, String.format("%s (%d ms)", description != null ? description : "(no description)", duration));
			}
		});
	}

	/**
	 * Call the given supplier and measure the execution duration
	 * @param <R> type of the return value
	 * @param todo supplier, which is executed
	 * @return return value
	 * @throws NodeException
	 */
	public <R> R supply(Supplier<R> todo) throws NodeException {
		try (Timing timing = withTiming()) {
			return todo.supply();
		}
	}

	/**
	 * Call the given function and measure the execution duration
	 * @param <R> type of the return value
	 * @param <P> type of the function parameter
	 * @param todo function, which is executed
	 * @param parameter parameter of the function
	 * @return return value
	 * @throws NodeException
	 */
	public <R, P> R apply(Function<P, R> todo, P parameter) throws NodeException {
		try (Timing timing = withTiming()) {
			return todo.apply(parameter);
		}
	}

	/**
	 * Call the given consumer and measure the execution duration
	 * @param <P> type of the consumer parameter
	 * @param todo consumer, which is executed
	 * @param parameter parameter of the consumer
	 * @throws NodeException
	 */
	public <P> void accept(Consumer<P> todo, P parameter) throws NodeException {
		try (Timing timing = withTiming()) {
			todo.accept(parameter);
		}
	}

	/**
	 * Call the given operator and measure the execution duration
	 * @param todo operator, which is executed
	 * @throws NodeException
	 */
	public void operate(Operator todo) throws NodeException {
		try (Timing timing = withTiming()) {
			todo.operate();
		}
	}

	/**
	 * Reset the statistics (number of invocations and total execution time)
	 */
	public void reset() {
		invocations.set(0);
		totalTimeMs.set(0);
	}

	/**
	 * Log execution statistics (if a logger has been given and the required log level is set)
	 */
	public void logStatistics() {
		if (logger != null && logger.isEnabled(statisticsLevel)) {
			long avg = 0;

			if (invocations.get() > 0) {
				avg = totalTimeMs.get() / invocations.get();
			}

			logger.log(statisticsLevel, String.format("%s invocations: %d, total: %d ms, avg: %d ms",
					description != null ? description : "(no description)", invocations.get(), totalTimeMs.get(), avg));
		}
	}
}
