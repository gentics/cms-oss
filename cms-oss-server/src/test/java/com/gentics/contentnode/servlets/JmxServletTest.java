package com.gentics.contentnode.servlets;


import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_FILES;
import static com.gentics.contentnode.runtime.ConfigurationValue.CONF_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PropertyNodeConfig;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.security.AccessControlService;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.Test;


class JmxServletTest extends JerseyTest {


	@Inject
	private static JmxServlet jmxServlet;


	@Override
	public DeploymentContext configureDeployment() {
		System.setProperty(CONF_PATH.getSystemPropertyName(), "src/test/resources/");

		var binder = new AbstractBinder() {
			@Override
			protected void configure() {
				bind(getAccessControlService()).to(AccessControlService.class);
			}
		};

		var serviceLocator =
				ServiceLocatorUtilities.createAndPopulateServiceLocator();
		ServiceLocatorUtilities.bind(serviceLocator, binder);

		final var resourceConfig = new ResourceConfig();
		resourceConfig.register(serviceLocator);

		jmxServlet = serviceLocator.getService(JmxServlet.class);
		jmxServlet.setConfigurationSupplier(getConfigSupplier());

		return ServletDeploymentContext.builder(resourceConfig)
				.servlet(jmxServlet)
				.servletPath("/jmx")
				.build();
	}

	@Override
	public TestContainerFactory getTestContainerFactory() {
		return new GrizzlyWebTestContainerFactory();
	}

	@Test
	public void givenJmxServlet_withDefaultConfiguration_shouldReturnValidResponse() {
		configureAccess("default.yml");

		var servletResponse = target("/")
				.request(MediaType.APPLICATION_JSON);

		assertThat(servletResponse.get().getStatusInfo()).isEqualTo(Status.OK);

		Map<String, String> memoryNode = JsonPath.read(servletResponse.get(String.class),
				"$['java.lang:type=Memory']");

		assertThat(memoryNode).containsKey("HeapMemoryUsage");
		assertThat(memoryNode).containsKey("NonHeapMemoryUsage");
		assertThat(memoryNode).containsKey("ObjectPendingFinalizationCount");
	}

	@Test
	public void givenJmxServlet_whenSingleMemoryAttributeIsSpecified_shouldReturnValidResponse() {
		configureAccess("single_value.yml");

		var servletResponse = target("/")
				.request(MediaType.APPLICATION_JSON);

		assertThat(servletResponse.get().getStatusInfo()).isEqualTo(Status.OK);

		Map<String, String> memoryNode = JsonPath.read(servletResponse.get(String.class),
				"$['java.lang:type=Memory']");

		assertThat(memoryNode).containsKey("HeapMemoryUsage");
	}

	@Test
	public void givenJmxServlet_whenAccessControlIsDisabled_shouldReturnValidJson() {
		configureAccess("allow_all.yml");

		var servletResponse = target("/")
				.request(MediaType.APPLICATION_JSON);

		assertThat(servletResponse.get().getStatusInfo())
				.as("Disabled Access Control (secured=false) should allow access from all Ips")
				.isEqualTo(Status.OK);
		assertThat(servletResponse.get(String.class)).isNotEmpty();
	}

	@Test
	public void givenJmxServlet_whenAccessControlIsEnabledAndIpIsInAllowList_shouldReturnValidJson() {
		configureAccess("allow_ip.yml");

		var servletResponse = target("/")
				.request();

		assertThat(servletResponse.get().getStatusInfo())
				.as("Access with Ip in allow list should be able to access this resource")
				.isEqualTo(Status.OK);
		assertThat(servletResponse.get(String.class)).isNotEmpty();
	}

	@Test
	public void givenJmxServlet_whenAccessControlIsEnabledAndIpIsNotInAllowList_shouldDenyAccess() {
		configureAccess("deny_access.yml");

		var servletResponse = target("/")
				.request()
				.get()
				.getStatusInfo();

		assertThat(servletResponse.getReasonPhrase())
				.as("Access with Ip not in allow list should not be able to access this resource")
				.contains("Access denied");
	}

	private void configureAccess(String configFile) {
		System.setProperty(CONF_FILES.getSystemPropertyName(), "jmx/" + configFile);
		jmxServlet.setConfigurationSupplier(
				() -> {
					try {
						Map<String, Object> data = NodeConfigRuntimeConfiguration
								.loadConfiguration();

						return new PropertyNodeConfig(data)
								.getDefaultPreferences();
					} catch (NodeException e) { throw new RuntimeException(e); }
				}
		);
		jmxServlet.setAccessControlService(getAccessControlService());
	}

	private AccessControlService getAccessControlService() {
		return new AccessControlService("jmx", getConfigSupplier());
	}

	private Supplier<NodePreferences> getConfigSupplier() {
		return () -> {
			try {
				Map<String, Object> data = NodeConfigRuntimeConfiguration
						.loadConfiguration();

				return new PropertyNodeConfig(data)
						.getDefaultPreferences();
			} catch (NodeException e) { throw new RuntimeException(e); }
		};
	}

}

