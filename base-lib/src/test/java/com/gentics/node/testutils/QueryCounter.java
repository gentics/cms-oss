package com.gentics.node.testutils;

import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.lib.db.DB;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper class that can be used as appender to the DB logger to count the number of issued SQL queries
 */
public class QueryCounter extends AbstractAppender {

	/**
	 * counter for SQL queries
	 */
	protected int counter = 0;

	/**
	 * Statements
	 */
	protected StringBuffer statements = null;

	/**
	 * Thread, that created this counter instance
	 * This will be set, if counting shall be restricted to this thread
	 */
	protected Thread createdBy = null;

	/**
	 * Create an instance. This will additionally set the loglevel for the DB
	 * class to DEBUG. Will remove all previous appenders and will add this
	 * instance as new appender.
	 * 
	 * @param logStatements
	 *            true if statements should be logged as well
	 * @param threadOnly
	 *            true to log only statements that are issued from the thread
	 *            which created this instance
	 */
	public QueryCounter(boolean logStatements, boolean threadOnly) {
		super(UUID.randomUUID().toString(), null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
		if (threadOnly) {
			createdBy = Thread.currentThread();
		}
		if (logStatements) {
			statements = new StringBuffer();
		}

		// use the logging mechanism to count the number of statements
		NodeLogger l = NodeLogger.getNodeLogger(DB.class);

		l.setLevel(Level.DEBUG);

		l.removeAllAppenders();
		l.addAppender(this);
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.Appender#close()
	 */
	public void close() {}

	/* (non-Javadoc)
	 * @see org.apache.log4j.Appender#requiresLayout()
	 */
	public boolean requiresLayout() {
		return false;
	}

	@Override
	public void append(LogEvent event) {
		if (isStarted()) {
			// if counting shall be restricted to the creating thread, we check the current thread now
			if (createdBy != null) {
				if (!createdBy.equals(Thread.currentThread())) {
					return;
				}
			}
			counter++;
			if (statements != null) {
				statements.append(event.getMessage().getFormattedMessage()).append("\n");
			}
		}
	}

	/**
	 * Get the current count
	 * @return current count
	 */
	public int getCount() {
		return counter;
	}

	/**
	 * Get the logged statements
	 * @return logged statements or null if logging was not activated
	 */
	public String getLoggedStatements() {
		return statements != null ? statements.toString() : null;
	}
}
