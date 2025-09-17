package com.gentics.contentnode.testutils;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * Autoclosable that prints the time between creation and closing to std out (for debugging)
 */
public class TimeLogger implements AutoCloseable {
	/**
	 * Optional name
	 */
	protected String name;

	/**
	 * Start timestamp
	 */
	protected long start;

	/**
	 * Create a time logger without name
	 */
	public TimeLogger() {
		this(null);
	}

	/**
	 * Create a time logger with name
	 * @param name
	 */
	public TimeLogger(String name) {
		this.name = name;
		this.start = System.currentTimeMillis();
	}

	@Override
	public void close() {
		long duration = System.currentTimeMillis() - start;
		if (!ObjectTransformer.isEmpty(name)) {
			System.out.print(name + ": ");
		}
		System.out.println(DurationFormatUtils.formatDurationWords(duration, true, false));
	}
}
