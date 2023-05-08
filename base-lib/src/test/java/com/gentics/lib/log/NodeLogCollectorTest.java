package com.gentics.lib.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the {@link NodeLogCollector}
 */
@Category(BaseLibTest.class)
public class NodeLogCollectorTest {
	/**
	 * Appender names
	 */
	protected String[] appenderNames;

	/**
	 * Get the names of the configured appender names
	 */
	@Before
	public void setup() {
		appenderNames = getAppenders();
	}

	/**
	 * Check that the set of configured appenders is the same as before the test
	 */
	@After
	public void tearDown() {
		assertThat(getAppenders()).as("Appenders").containsOnly(appenderNames);
	}

	/**
	 * Test collecting logs with the default level
	 */
	@Test
	public void testCollectDefaultLevel() {
		NodeLogger blaLogger = NodeLogger.getNodeLogger("bla");
		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);

		try (NodeLogCollector collector = new NodeLogCollector(blaLogger)) {
			blaLogger.debug("This is debug");
			blaLogger.info("This is info");
			blaLogger.warn("This is warn");
			blaLogger.error("This is error");
			blaLogger.fatal("This is fatal");

			assertThat(collector.getLog()).as("Log messages").isEqualTo(getExpected(
					"This is error",
					"This is fatal"
			));
		}

		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
	}

	/**
	 * Test collecting logs with a specific level
	 */
	@Test
	public void testCollectSpecificLevel() {
		NodeLogger blaLogger = NodeLogger.getNodeLogger("bla");
		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
		try (NodeLogCollector collector = new NodeLogCollector(Level.INFO, blaLogger)) {
			blaLogger.debug("This is debug");
			blaLogger.info("This is info");
			blaLogger.warn("This is warn");
			blaLogger.error("This is error");
			blaLogger.fatal("This is fatal");

			assertThat(collector.getLog()).as("Log messages").isEqualTo(getExpected(
					"This is info",
					"This is warn",
					"This is error",
					"This is fatal"
			));
		}

		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
	}

	/**
	 * Test collecting log for a sub logger
	 */
	@Test
	public void testSubLogger() {
		NodeLogger blaLogger = NodeLogger.getNodeLogger("bla");
		NodeLogger blaSubLogger = NodeLogger.getNodeLogger("bla.sub");
		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
		assertThat(blaSubLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);

		try (NodeLogCollector collector = new NodeLogCollector(blaSubLogger)) {
			blaLogger.error("This is the bla logger");
			blaSubLogger.error("This is the bla sub logger");

			assertThat(collector.getLog()).as("Log messages").isEqualTo(getExpected(
					"This is the bla sub logger"
			));
		}

		assertThat(blaLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
		assertThat(blaSubLogger.getLevel()).as("Log level").isEqualTo(Level.ERROR);
	}

	/**
	 * Get the expected log for the given log lines
	 * @param logLines log lines
	 * @return expected log
	 */
	protected String getExpected(String...logLines) {
		StringBuilder log = new StringBuilder();
		for (String logLine : logLines) {
			log.append(logLine).append(System.lineSeparator());
		}
		return log.toString();
	}

	/**
	 * Get the current appenders
	 * @return names of the appenders
	 */
	protected String[] getAppenders() {
		LoggerContext context = LoggerContext.getContext(false);
		Configuration config = context.getConfiguration();
		Set<String> appenders = config.getAppenders().keySet();
		return appenders.toArray(new String[appenders.size()]);
	}
}
