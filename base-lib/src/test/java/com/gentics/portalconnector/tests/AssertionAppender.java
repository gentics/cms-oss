package com.gentics.portalconnector.tests;

import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Appender to report errors in assertion text
 */
public class AssertionAppender extends AbstractAppender {

	/**
	 * String buffer that will collect the logged errors
	 */
	private StringBuffer buffer = new StringBuffer();

	/**
	 * Create instance
	 */
	public AssertionAppender() {
		super(UUID.randomUUID().toString(), null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
	}

	@Override
	public void append(LogEvent event) {
		if (event.getLevel().isMoreSpecificThan(Level.ERROR)) {
			buffer.append(event.getMessage().getFormattedMessage()).append("\n");
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#close()
	 */
	public void close() {
		buffer = new StringBuffer();
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
	 */
	public boolean requiresLayout() {
		return false;
	}

	/**
	 * Get the errors
	 * @return errors
	 */
	public String getErrors() {
		return buffer.toString();
	}

	/**
	 * Reset the logged errors
	 */
	public void reset() {
		buffer = new StringBuffer();
	}
}
