package com.gentics.contentnode.tests.runtime;

import static com.gentics.contentnode.runtime.ConfigurationValue.NODE_DB_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Test cases for getting configuration values
 */
public class ConfigurationValueTest {
	/**
	 * Mocked node preferences
	 */
	private static NodePreferences nodePreferences = mock(MapPreferences.class);

	/**
	 * Clear the system property and mocked configuration value
	 */
	@Before
	public void setup() {
		System.setProperty(NODE_DB_NAME.getSystemPropertyName(), "");
		when(nodePreferences.getProperty(NODE_DB_NAME.getConfigurationProperty())).thenReturn(null);
	}

	/**
	 * Test reading from system property
	 */
	@Test
	public void testReadFromSystemProperty() {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedRuntimeConfiguration = mockStatic(NodeConfigRuntimeConfiguration.class)) {
			mockedRuntimeConfiguration.when(NodeConfigRuntimeConfiguration::getPreferences).thenReturn(nodePreferences);

			System.setProperty(NODE_DB_NAME.getSystemPropertyName(), "from_systemproperty");
			when(nodePreferences.getProperty(NODE_DB_NAME.getConfigurationProperty())).thenReturn("from_configuration");

			assertThat(NODE_DB_NAME.get()).as("configuration from environment").isEqualTo("from_systemproperty");
		}
	}

	/**
	 * Test reading from the configuration
	 */
	@Test
	public void testReadFromConfiguration() {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedRuntimeConfiguration = mockStatic(NodeConfigRuntimeConfiguration.class)) {
			mockedRuntimeConfiguration.when(NodeConfigRuntimeConfiguration::getPreferences).thenReturn(nodePreferences);

			when(nodePreferences.getProperty(NODE_DB_NAME.getConfigurationProperty())).thenReturn("from_configuration");

			assertThat(NODE_DB_NAME.get()).as("configuration from environment").isEqualTo("from_configuration");
		}
	}

	/**
	 * Test reading the default value
	 */
	@Test
	public void testReadDefaultValue() {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedRuntimeConfiguration = mockStatic(NodeConfigRuntimeConfiguration.class)) {
			mockedRuntimeConfiguration.when(NodeConfigRuntimeConfiguration::getPreferences).thenReturn(nodePreferences);

			assertThat(NODE_DB_NAME.get()).as("configuration from environment").isEqualTo("node_utf8");
		}
	}
}
