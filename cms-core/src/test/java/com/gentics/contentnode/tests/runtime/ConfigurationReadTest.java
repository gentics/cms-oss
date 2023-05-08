package com.gentics.contentnode.tests.runtime;

import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_FILES;
import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Test cases for reading configuration from multiple files
 */
@RunWith(value = Parameterized.class)
public class ConfigurationReadTest {
	@Parameters(name = "{index}: path {0}, files {1}")
	public static Collection<Object[]> data() throws URISyntaxException {
		Collection<Object[]> data = new ArrayList<>();
		URL confBaseURL = ConfigurationReadTest.class.getResource("conf");
		File confBaseFile = new File(confBaseURL.toURI());
		String path = confBaseFile.getAbsolutePath();

		// test reading from a single file
		data.add(new Object[] { path, "one.yml", (Consumer<MapPreferences>) prefs -> {
			assertPreference(prefs, "common", "one");
			assertPreference(prefs, "one", "set");
			assertPreference(prefs, "two", null);
			assertPreference(prefs, "three", null);
			assertPreference(prefs, "complex.one", "set");
			assertPreference(prefs, "complex.two", null);
			assertPreference(prefs, "complex.three", null);
			assertPreferenceArray(prefs, "array", "common", "one");
		} });

		// test reading from two files in order
		data.add(new Object[] { path, "one.yml,two.yml", (Consumer<MapPreferences>) prefs -> {
			assertPreference(prefs, "common", "two");
			assertPreference(prefs, "one", "set");
			assertPreference(prefs, "two", "set");
			assertPreference(prefs, "three", null);
			assertPreference(prefs, "complex.one", "set");
			assertPreference(prefs, "complex.two", "set");
			assertPreference(prefs, "complex.three", null);
			assertPreferenceArray(prefs, "array", "common", "two");
		} });

		// test reading from the same two files in reverse order
		data.add(new Object[] { path, "two.yml,one.yml", (Consumer<MapPreferences>) prefs -> {
			assertPreference(prefs, "common", "one");
			assertPreference(prefs, "one", "set");
			assertPreference(prefs, "two", "set");
			assertPreference(prefs, "three", null);
			assertPreference(prefs, "complex.one", "set");
			assertPreference(prefs, "complex.two", "set");
			assertPreference(prefs, "complex.three", null);
			assertPreferenceArray(prefs, "array", "common", "one");
		} });

		// test reading from three files (in order)
		data.add(new Object[] { path, "one.yml,two.yml,three.yml", (Consumer<MapPreferences>) prefs -> {
			assertPreference(prefs, "common", "three");
			assertPreference(prefs, "one", "set");
			assertPreference(prefs, "two", "set");
			assertPreference(prefs, "three", "set");
			assertPreference(prefs, "complex.one", "set");
			assertPreference(prefs, "complex.two", "set");
			assertPreference(prefs, "complex.three", "set");
			assertPreferenceArray(prefs, "array", "common", "three");
		} });

		// test reading from all .yml files of a directory
		data.add(new Object[] { path, "custom", (Consumer<MapPreferences>) prefs -> {
			assertPreference(prefs, "common", "two");
			assertPreference(prefs, "one", "set");
			assertPreference(prefs, "two", "set");
			assertPreference(prefs, "three", "set");
			assertPreference(prefs, "complex.one", "set");
			assertPreference(prefs, "complex.two", "set");
			assertPreference(prefs, "complex.three", "set");
			assertPreferenceArray(prefs, "array", "common", "two");
			assertPreference(prefs, "ignored", null);
		} });
		return data;
	}

	/**
	 * Assert that the preferences contains the given configuration value
	 * @param prefs preferences
	 * @param path path to check
	 * @param value expected value
	 */
	public static void assertPreference(MapPreferences prefs, String path, String value) {
		assertThat(prefs.getProperty(path)).as("Configuration value " + path).isEqualTo(value);
	}

	/**
	 * Assert that the preferences contains the given configuration value array
	 * @param prefs preferences
	 * @param path path to check
	 * @param values expected values (in order)
	 */
	public static void assertPreferenceArray(MapPreferences prefs, String path, String...values) {
		assertThat(prefs.getProperties(path)).as("Configuration value " + path).containsExactly(values);
	}

	@Parameter(0)
	public String path;

	@Parameter(1)
	public String files;

	@Parameter(2)
	public Consumer<MapPreferences> asserter;

	/**
	 * Set the configuration values
	 */
	@Before
	public void setup() {
		System.setProperty(CONF_PATH.getSystemPropertyName(), path);
		System.setProperty(CONF_FILES.getSystemPropertyName(), files);
	}

	/**
	 * Load the configuration and pass it to the asserter
	 * @throws NodeException
	 */
	@Test
	public void test() throws NodeException {
		asserter.accept(new MapPreferences(NodeConfigRuntimeConfiguration.loadConfiguration()));
	}
}
