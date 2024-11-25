package com.gentics.contentnode.init;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.lib.log.NodeLogger;

/**
 * Test cases for handling logging with log4j2.
 * Tests cover
 * <ol>
 * <li>loading the configuration from nodelog.yml file (with automatic scanning and reconfiguration)
 * <li>programmatic adding of "runtime" logger (with log level INFO)</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class NodeLoggerConfigurationTest {
	/**
	 * Name of the name based logger
	 */
	public final static String LOGGER_NAME = "testlogger";

	/**
	 * Configuration file nodelog.yml
	 */
	public static File nodelogYml;

	/**
	 * Class based logger
	 */
	public static NodeLogger classBasedLogger;

	/**
	 * Name based logger
	 */
	public static NodeLogger nameBasedLogger;

	/**
	 * Runtime logger
	 */
	public static NodeLogger runtimeLogger;

	@BeforeClass
	public static void setupOnce() throws IOException {
		nodelogYml = File.createTempFile("nodelog", ".yml");

		// prepare configuration with level DEBUG
		copyConfig(Level.DEBUG);

		// configure the configuration factory and file
		System.setProperty("log4j2.configurationFactory", CustomConfigFactory.class.getName());
		System.setProperty("log4j2.configurationFile", nodelogYml.getAbsolutePath());

		// prepare the loggers
		classBasedLogger = NodeLogger.getNodeLogger(NodeLoggerConfigurationTest.class);
		nameBasedLogger = NodeLogger.getNodeLogger(LOGGER_NAME);
		runtimeLogger = NodeLogger.getNodeLogger("runtime");

		// initial assertion of log levels
		assertCurrentLevel("Class based logger", classBasedLogger, Level.DEBUG);
		assertCurrentLevel("Name based logger", nameBasedLogger, Level.DEBUG);
		assertCurrentLevel("Runtime logger", runtimeLogger, Level.INFO);
	}

	@AfterClass
	public static void tearDownOnce() {
		if (nodelogYml != null) {
			FileUtils.deleteQuietly(nodelogYml);
		}
	}

	@Parameters(name = "{index}: level {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Level level : Level.values()) {
			if (level != Level.OFF) {
				data.add(new Object[] { level });
			}
		}
		return data;
	}

	/**
	 * Copy the nodelog.yml file contents with given level over the {@link #nodelogYml} file.
	 * This is expected to trigger the automatic reconfiguration, because the file sets the monitor interval to 1s
	 * @param level log level
	 * @throws IOException
	 */
	public static void copyConfig(Level level) throws IOException {
		String nodelogContents = null;
		try (InputStream in = NodeLoggerConfigurationTest.class.getResourceAsStream("nodelog.yml");
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			IOUtils.copy(in, out);
			nodelogContents = out.toString(StandardCharsets.UTF_8);
		}

		nodelogContents = nodelogContents.replaceAll(Pattern.quote("{{level}}"), level.name().toLowerCase());

		try (InputStream in = new ByteArrayInputStream(nodelogContents.getBytes(StandardCharsets.UTF_8));
				FileOutputStream out = new FileOutputStream(nodelogYml)) {
			IOUtils.copy(in, out);
		}
	}

	@Parameter(0)
	public Level level;

	/**
	 * Set the log levels to {@link #level} in the nodelog.yml file and wait 1500 ms
	 * so that log4j2 has the chance to reconfigure the loggers
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Before
	public void setup() throws IOException, InterruptedException {
		copyConfig(level);
		Thread.sleep(1500);
	}

	/**
	 * Test that the class based logger has the currently configured log level
	 */
	@Test
	public void testClassBasedLogger() {
		assertCurrentLevel("Class based logger", classBasedLogger, level);
	}

	/**
	 * Test that the name based logger has the currently configured log level
	 */
	@Test
	public void testNameBasedLogger() {
		assertCurrentLevel("Name based logger", nameBasedLogger, level);
	}

	/**
	 * Test that the runtime logger has log level INFO
	 */
	@Test
	public void testRuntimeLogger() {
		assertCurrentLevel("Runtime logger", runtimeLogger, Level.INFO);
	}

	/**
	 * Assert that the given logger has exactly the given log level set
	 * @param description logger description
	 * @param logger logger to test
	 * @param expectedLevel expected level
	 */
	protected static void assertCurrentLevel(String description, NodeLogger logger, Level expectedLevel) {
		for (Level level : Level.values()) {
			boolean expectedFlag = level.isMoreSpecificThan(expectedLevel);
			assertThat(logger.isEnabled(level)).as(String.format("%s has level %s enabled", description, level))
					.isEqualTo(expectedFlag);
		}
	}
}
