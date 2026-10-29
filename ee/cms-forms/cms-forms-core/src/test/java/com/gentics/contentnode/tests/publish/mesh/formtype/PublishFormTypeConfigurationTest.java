package com.gentics.contentnode.tests.publish.mesh.formtype;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.allSubsets;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.publish.mesh.form.AbstractFormPublishTest;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

/**
 * Test for writing the form type configurations into the plugins during the publish process
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@Category(MeshTest.class)
@RunWith(Parameterized.class)
@ConfigBy(PublishFormTypeConfigurationTest.class)
public class PublishFormTypeConfigurationTest extends AbstractFormPublishTest implements ConfigModifier {
	/**
	 * Form type configurations
	 */
	protected final static FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

	/**
	 * Map of webroot paths to the node holding the form type configs per plugin
	 */
	protected final static Map<String, String> configPathPerPlugin = Map.of("forms", "Forms Plugin", "acta-nova",
			"acta_nova_plugin/config");

	/**
	 * Prepare the form type configurations
	 */
	static {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, List<String>> configs = Map.of("forms", Arrays.asList("forms_one", "forms_two", "forms_empty"), "acta-nova",
				Arrays.asList("andp_one", "andp_two", "andp_empty"));

		configs.entrySet().stream().forEach(entry -> {
			String pluginName = entry.getKey();
			boolean external = Strings.CI.equals(pluginName, "andp");

			entry.getValue().forEach(formType -> {
				FormTypeConfiguration config = new FormTypeConfiguration(Map.of("de",
						"Name of %s (de)".formatted(formType), "en", "Name of %s (en)".formatted(formType)),
						pluginName);
				config.external = external;

				if (!Strings.CI.endsWith(formType, "empty")) {
					config.pluginConfiguration = mapper.createObjectNode();
					config.pluginConfiguration.put("type", formType);
				}

				formTypeConfiguration.put(formType, config);
			});
		});
	}

	@Parameters(name = "{index}: active types: {0}")
	public static Collection<Object[]> data() {
		return allSubsets(formTypeConfiguration.keySet()).stream().map(set -> new Object[] { set })
				.collect(Collectors.toList());
	}

	@Parameter(0)
	public Set<String> activeFormTypes;

	/**
	 * Prepare the test case
	 */
	@Before
	public void setup() throws NodeException {
		super.setup();

		// add the form types of the test case to the node (and remove the others)
		operate(() -> {
			for (String formType : formTypeConfiguration.keySet()) {
				if (activeFormTypes.contains(formType)) {
					FormFactory.addFormTypeToNode(formType, node.getId());
				} else {
					FormFactory.removeFormTypeFromNode(formType, node.getId());
				}
			}
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Test whether the form type configurations have been written
	 * @throws NodeException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	@Test
	public void test() throws NodeException, JsonMappingException, JsonProcessingException {
		Map<String, ObjectNode> configPerPlugin = new HashMap<>();

		for (String formType : activeFormTypes) {
			FormTypeConfiguration config = formTypeConfiguration.get(formType);
			configPerPlugin.computeIfAbsent(config.pluginName, key -> mapper.createObjectNode()).set(formType,
					Optional.ofNullable(config.pluginConfiguration).orElse(mapper.createObjectNode()));
		}

		for (Map.Entry<String, ObjectNode> entry : configPerPlugin.entrySet()) {
			String pluginName = entry.getKey();
			JsonNode expectedWrittenConfig = entry.getValue();
			JsonNode existing = getConfigFromMesh(configPathPerPlugin.get(pluginName));

			assertThat(existing).as("Written configuration").isEqualTo(expectedWrittenConfig);
		}
	}

	/**
	 * Set the form type configurations
	 */
	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}

	/**
	 * Get the configuration written to the plugin
	 * @param path plugin specific path
	 * @return configuration
	 * @throws NodeException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	protected JsonNode getConfigFromMesh(String path) throws NodeException, JsonMappingException, JsonProcessingException {
		String projectName = execute(n -> MeshPublisher.getMeshProjectName(n), node);
		BranchResponse latestBranch = mesh.client().findBranches(projectName).blockingGet().getData().stream()
				.filter(BranchResponse::getLatest).findFirst().orElseThrow();
		MeshWebrootResponse configResponse = mesh.client()
				.webroot(projectName, "%s/%s".formatted(latestBranch.getPathPrefix(), path)).blockingGet();
		String configString = configResponse.getNodeResponse().getFields().getStringField("formtypeconfigs").getString();
		return mapper.readTree(configString);
	}
}
