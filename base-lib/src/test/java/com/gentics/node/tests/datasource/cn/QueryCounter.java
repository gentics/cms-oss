package com.gentics.node.tests.datasource.cn;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Internal helper class that can be used as appender to the DB logger to count the number of issued SQL queries
 */
public class QueryCounter extends AbstractAppender {

	/**
	 * counter for SQL queries
	 */
	private static final ThreadLocal<AtomicInteger> counter = new ThreadLocal<AtomicInteger>();

	/**
	 * Create instance
	 */
	public QueryCounter() {
		super(UUID.randomUUID().toString(), null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
	}

	/**
	 * Return the current count of the internal counter and reset the counter
	 * 
	 * @return
	 */
	public int getCountAndReset() {
		int count = counter.get().intValue();
		reset();
		return count;
	}

	@Override
	public void append(LogEvent event) {
		// do not log anything, but count
		AtomicInteger prevCount = counter.get();
		if (prevCount == null) {
			prevCount = new AtomicInteger(0);
			counter.set(prevCount);
		}
		prevCount.incrementAndGet();
	}

	/**
	 * Reset the query counter for the current thread.
	 */
	public void reset() {
		if (counter.get() == null) {
			counter.set(new AtomicInteger());
		}
		counter.get().set(0);
	}
}