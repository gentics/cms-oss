package com.gentics.contentnode.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Appender implementation that logs into an internal message list.
 *
 * <p>
 *     Optionally a minimum loglevel can be specified, then all messages with
 *     a lower priority are ignored.
 * </p>
 */
public class TestAppender extends AbstractAppender {

	protected List<String> messages = new ArrayList<>();

	protected Level minLogLevel;

	/**
	 * Create an instance without a minimum loglevel.
	 */
	public TestAppender() {
		this(null);
	}

	/**
	 * Create an instance with the specified minimum loglevel.
	 *
	 * @param minLogLevel The minimum loglevel
	 */
	public TestAppender(Level minLogLevel) {
		super(UUID.randomUUID().toString(), null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
		this.minLogLevel = minLogLevel;
	}

	@Override
	public void append(LogEvent event) {
		if (minLogLevel == null || event.getLevel().isMoreSpecificThan(minLogLevel)) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}

	@Override
	public String toString() {
		return messages.isEmpty() ? "" : String.join("\n", messages) + "\n";
	}

	/**
	 * Get the logged messages.
	 *
	 * @return The logged messages
	 */
	public List<String> getMessages() {
		return messages;
	}

	/**
	 * Clear the messages list.
	 */
	public void reset() {
		messages.clear();
	}
}
