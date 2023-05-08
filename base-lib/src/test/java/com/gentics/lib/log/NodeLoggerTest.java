package com.gentics.lib.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the NodeLogger
 */
@Category(BaseLibTest.class)
public class NodeLoggerTest {
	/**
	 * Name of the logger
	 */
	public final static String LOGGER_NAME = "testlogger";

	/**
	 * Log Writer
	 */
	protected final static StringWriter logWriter = new StringWriter();

	@BeforeClass
	public static void setupOnce() {
		LoggerContext context = LoggerContext.getContext(false);
		Configuration config = context.getConfiguration();
		PatternLayout layout = PatternLayout.createDefaultLayout(config);
		Appender appender = WriterAppender.createAppender(layout, null, logWriter, "testwriter", false, true);
		appender.start();

		config.addAppender(appender);
		config.getLoggerConfig(LOGGER_NAME).addAppender(appender, null, null);
	}

	@Before
	public void setup() {
		StringBuffer buffer = logWriter.getBuffer();
		buffer.delete(0, buffer.length());
	}

	@Test
	public void testForceInfo() {
		NodeLogger.getNodeLogger(LOGGER_NAME).info("This is just info before the force");
		NodeLogger.getNodeLogger(LOGGER_NAME).forceInfo("This is forced info");
		NodeLogger.getNodeLogger(LOGGER_NAME).info("This is just info after the force");

		assertThat(logWriter.getBuffer().toString()).as("Logged messages").isEqualTo("This is forced info" + System.lineSeparator());
	}
}
