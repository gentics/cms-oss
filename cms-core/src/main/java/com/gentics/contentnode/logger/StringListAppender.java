package com.gentics.contentnode.logger;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Appender implementation, that adds the formatted log messages to the given list of strings
 */
public class StringListAppender extends AbstractAppender {
	private List<String> messages;

	/**
	 * Create an instance
	 * @param name appender name
	 * @param layout appender layout
	 */
	public StringListAppender(Layout<? extends Serializable> layout, List<String> messages) {
		super(UUID.randomUUID().toString(), null, layout, true, Property.EMPTY_ARRAY);
		this.messages = messages;
	}

	@Override
	public void append(LogEvent event) {
		messages.add(event.getMessage().getFormattedMessage());
	}
}
