package com.gentics.contentnode.tests.etc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.contentnode.etc.MapPreferences;

/**
 * Test reading configuration properties from an instance of MapPreferences
 */
@RunWith(value = Parameterized.class)
public class MapPreferencesTest {
	/**
	 * Preferences
	 */
	public static MapPreferences prefs;

	@Parameters(name = "{index}: property {0}, expected {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		// merged config
		data.add(new Object[] {"zero", true});
		data.add(new Object[] {"one", true});
		data.add(new Object[] {"two", true});
		data.add(new Object[] {"three", true});
		data.add(new Object[] {"merged.zero", true});
		data.add(new Object[] {"merged.one", true});
		data.add(new Object[] {"merged.two", true});
		data.add(new Object[] {"merged.three", true});

		// old prefixes still work
		data.add(new Object[] {"contentnode.global.config.merged.one", true});
		data.add(new Object[] {"global.config.merged.two", true});
		data.add(new Object[] {"config.merged.three", true});

		// unrecognized prefixes will not work
		data.add(new Object[] {"global.merged.two", null});

		// check for substitutions
		data.add(new Object[] {"properties.existent", "This is the value of the system property"});
		data.add(new Object[] {"properties.nonexistent", "${sys:com.gentics.contentnode.nonexistent}"});
		data.add(new Object[] {"properties.array", Arrays.asList("This is the value of the system property", "${sys:com.gentics.contentnode.nonexistent}")});
		data.add(new Object[] {"properties.sub.existent", "This is the value of the system property"});
		data.add(new Object[] {"properties.sub.nonexistent", "${sys:com.gentics.contentnode.nonexistent}"});
		data.add(new Object[] {"properties.sub.array", Arrays.asList("This is the value of the system property", "${sys:com.gentics.contentnode.nonexistent}")});

		return data;
	}

	/**
	 * Read the configuration
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		System.setProperty("com.gentics.contentnode.existent", "This is the value of the system property");
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setDefaultMergeable(true);

		try (InputStream in = MapPreferencesTest.class.getResourceAsStream("uncleaned_settings.yml")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> data = mapper.readValue(in, Map.class);
			prefs = new MapPreferences(data);
		}
	}

	@Parameter(0)
	public String property;

	@Parameter(1)
	public Object expected;

	@Test
	public void testProperty() throws Exception {
		Object value = prefs.getPropertyObject(property);
		assertThat(value).as("Value for " + property).isEqualTo(expected);
	}
}
