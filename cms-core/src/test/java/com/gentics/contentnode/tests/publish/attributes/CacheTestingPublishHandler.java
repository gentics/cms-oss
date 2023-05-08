package com.gentics.contentnode.tests.publish.attributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.db.DB;
import com.gentics.lib.log.NodeLogger;

/**
 * Publish handler implementation that accesses attributes for updated or created objects and counts the number of used SQL Statements
 */
public class CacheTestingPublishHandler implements CnMapPublishHandler {
	/**
	 * Set of attributes to access
	 */
	protected static Set<String> attributes = new HashSet<>();

	/**
	 * Query Counter
	 */
	protected static QueryCounter counter = null;

	/**
	 * Get the query counter
	 * @return query counter
	 */
	public static QueryCounter get() {
		return counter;
	}

	/**
	 * Set the attributes to be accessed
	 * @param attributes set of attribute names
	 */
	public static void set(String... attributes) {
		CacheTestingPublishHandler.attributes.clear();
		CacheTestingPublishHandler.attributes.addAll(Arrays.asList(attributes));
	}

	/**
	 * Reset the query counter
	 */
	public static void reset() {
		counter = new QueryCounter(true, false);
		counter.stop();
		CacheTestingPublishHandler.attributes.clear();
	}

	@Override
	public void init(@SuppressWarnings("rawtypes") Map parameters) throws CnMapPublishException {
	}

	@Override
	public void open(long timestamp) throws CnMapPublishException {
	}

	@Override
	public void createObject(Resolvable object) throws CnMapPublishException {
		handle(object);
	}

	@Override
	public void updateObject(Resolvable object) throws CnMapPublishException {
		handle(object);
	}

	/**
	 * Handle an object by accessing the attributes
	 * @param object object to handle
	 * @throws CnMapPublishException
	 */
	protected void handle(Resolvable object) throws CnMapPublishException {
		if (counter != null) {
			try {
				counter.start();
				for (String name : attributes) {
					if (object.get(name) == null) {
						throw new CnMapPublishException("Attribute " + name + " was not set for " + object);
					}
				}
			} finally {
				counter.stop();
			}
		}
	}

	@Override
	public void deleteObject(Resolvable object) throws CnMapPublishException {
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public void close() {
	}

	@Override
	public void destroy() {
	}
}

class QueryCounter extends AbstractAppender {

	/**
	 * counter for SQL queries
	 */
	protected int counter = 0;

	/**
	 * Set to true, when the counter is started
	 */
	protected boolean started = true;

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
		l.removeAllAppenders();
		l.addAppender(this, Level.DEBUG);
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