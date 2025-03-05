package com.gentics.contentnode.tests.aloha;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for different configuration settings of the aloha plugins
 */
@RunWith(Parameterized.class)
public class AlohaPluginsConfigurationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected final static Map<String, String[]> TEST_CASES_NODE = new HashMap<>();

	protected final static Map<String, String[]> TEST_CASES_OTHER_NODE = new HashMap<>();

	static {
		// default test case
		TEST_CASES_NODE.put("",
				new String[] { "gcn/gcn", "common/ui", "common/block", "common/format", "common/autoparagraph",
						"common/highlighteditables", "common/list", "common/link", "common/table", "common/paste",
						"common/contenthandler", "common/commands", "common/dom-to-xhtml" });

		// test case "aloha_plugins_global.yml" (global configuration)
		TEST_CASES_NODE.put("aloha_plugins_global.yml", new String[] { "gcn/gcn", "project/specific", "extra/ribbon" });

		// test case "aloha_plugins_node.yml" (node specific configuration)
		TEST_CASES_NODE.put("aloha_plugins_node.yml",
				new String[] { "gcn/gcn", "common/characterpicker", "extra/cite", "common/format" });
		TEST_CASES_OTHER_NODE.put("aloha_plugins_node.yml", TEST_CASES_NODE.get(""));

		// test case "aloha_plugins_global_and_node.yml" (global and node specific configuration)
		TEST_CASES_NODE.put("aloha_plugins_global_and_node.yml",
				new String[] { "gcn/gcn", "common/characterpicker", "extra/cite", "common/format" });
		TEST_CASES_OTHER_NODE.put("aloha_plugins_global_and_node.yml",
				new String[] { "gcn/gcn", "project/specific", "extra/ribbon" });
	}

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Other test node
	 */
	private static Node other;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("host", "Test Node", PublishTarget.NONE));
		other = supply(() -> createNode("otherhost", "Other Test Node", PublishTarget.NONE));
	}

	@Parameters(name = "{index}: configuration {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String file : TEST_CASES_NODE.keySet()) {
			data.add(new Object[] {file});
		}
		return data;
	}

	@Parameter(0)
	public String file;

	@Before
	public void setup() throws NodeException {
		if (StringUtils.isBlank(file)) {
			System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), "");
		} else {
			URL configFileUrl = getClass().getResource(file);
			if (configFileUrl != null && StringUtils.equals(configFileUrl.getProtocol(), "file")) {
				System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), configFileUrl.getPath());
			}
		}
		NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();
	}

	@Test
	public void testForNode() throws NodeException {
		String[] plugins = supply(() -> {
			return new AlohaRenderer().getAlohaPlugins(node);
		}).split(Pattern.quote(","));

		assertThat(plugins).as("Plugins").containsExactly(TEST_CASES_NODE.get(file));
	}

	@Test
	public void testForOtherNode() throws NodeException {
		String[] plugins = supply(() -> {
			return new AlohaRenderer().getAlohaPlugins(other);
		}).split(Pattern.quote(","));

		assertThat(plugins).as("Plugins")
				.containsExactly(TEST_CASES_OTHER_NODE.getOrDefault(file, TEST_CASES_NODE.get(file)));
	}
}
