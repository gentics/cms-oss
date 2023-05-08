package com.gentics.contentnode.logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;

/**
 * Appender implementation that logs to messages
 */
public class MessageAppender extends AbstractAppender {
	/**
	 * List of logged messages
	 */
	protected List<Message> messages = new ArrayList<Message>();

	/**
	 * Create an instance
	 * @param name appender name
	 * @param layout appender layout
	 */
	public MessageAppender(Layout<? extends Serializable> layout) {
		super(UUID.randomUUID().toString(), null, layout, true, Property.EMPTY_ARRAY);
	}

	@Override
	public void append(LogEvent event) {
		messages.add(new Message(level2Type(event.getLevel()), event.getMessage().getFormattedMessage(), null, event.getTimeMillis()));
	}

	/**
	 * Convert the log level to a message type
	 * @param level log level
	 * @return message type
	 */
	protected Type level2Type(Level level) {
		if (level.isMoreSpecificThan(Level.ERROR)) {
			return Type.CRITICAL;
		} else if (level.isMoreSpecificThan(Level.WARN)) {
			return Type.WARNING;
		} else {
			return Type.INFO;
		}
	}
}