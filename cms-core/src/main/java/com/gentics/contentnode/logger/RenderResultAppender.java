package com.gentics.contentnode.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.render.RenderResult;

public class RenderResultAppender extends AbstractAppender {

	public RenderResultAppender(String name) {
		super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
	}

	@Override
	public void append(LogEvent event) {
		// only log errors of level greater or equal ERROR
		if (event.getLevel().isMoreSpecificThan(Level.ERROR)) {
			try {
				RenderResult result = TransactionManager.getCurrentTransaction().getRenderResult();

				result.addMessage(new DefaultNodeMessage(event.getLevel(), event.getClass(), event.getMessage().getFormattedMessage()), false);
			} catch (TransactionException e1) {// we shouldn't log this error because this will probably end in an endless recursion ?
			}
		}
	}
}
