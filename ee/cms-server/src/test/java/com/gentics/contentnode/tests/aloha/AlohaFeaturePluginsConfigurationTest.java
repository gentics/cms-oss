package com.gentics.contentnode.tests.aloha;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.LINK_CHECKER })
public class AlohaFeaturePluginsConfigurationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private final static String TEST_NODE_NAME = "Test Node";

	private static Node testNode;

	private final static String TEST_CHANNEL_NAME = "Test Channel";

	private static Node testChannel;

	private final static String LINKCHECKER_TEST_NODE_NAME = "LinkChecker Test Node";

	private static Node linkCheckerTestNode;

	private final static String LINKCHECKER_TEST_CHANNEL_NAME = "LinkChecker Test Channel";

	private static Node linkCheckerTestChannel;

	private final static String DEFAULT_CONFIG = "default.yml";

	private final static String GLOBAL_CONFIG = "aloha_plugins_global.yml";

	private final static String NODE_CONFIG = "aloha_plugins_node.yml";

	private final static String CHANNEL_CONFIG = "aloha_plugins_channel.yml";

	private final static List<String> configurations = Arrays.asList(DEFAULT_CONFIG, GLOBAL_CONFIG, NODE_CONFIG, CHANNEL_CONFIG);

	private final static String[] DEFAULT_PLUGINS = new String[] { "gcn/gcn", "common/ui", "common/block",
			"common/format", "common/autoparagraph", "common/highlighteditables", "common/list", "common/link",
			"common/table", "common/paste", "common/contenthandler", "common/commands", "common/dom-to-xhtml",
			"gcn/spellcheck" };

	private final static String[] DEFAULT_PLUGINS_LINK_CHECKER = new String[] { "gcn/gcn", "gcn/gcnlinkchecker",
			"common/ui", "common/block", "common/format", "common/autoparagraph", "common/highlighteditables",
			"common/list", "common/link", "common/table", "common/paste", "common/contenthandler", "common/commands",
			"common/dom-to-xhtml", "gcn/spellcheck" };

	private final static String[] GLOBAL_PLUGINS = new String[] { "gcn/gcn", "project/specific", "extra/ribbon" };

	private final static String[] GLOBAL_PLUGINS_LINK_CHECKER = new String[] {"gcn/gcn", "gcn/gcnlinkchecker", "project/specific", "extra/ribbon"};

	private final static String[] NODE_PLUGINS = new String[] { "gcn/gcn", "common/characterpicker", "extra/cite",
			"common/format" };

	private final static String[] NODE_PLUGINS_LINK_CHECKER = new String[] { "gcn/gcn", "gcn/gcnlinkchecker",
			"common/characterpicker", "extra/cite", "common/format" };

	private static Map<String, Node> NODES = new HashMap<>();

	private static Map<Pair<String, String>, String[]> EXPECTED_PLUGINS = new HashMap<>();

	static {
		// default config
		EXPECTED_PLUGINS.put(Pair.of(TEST_NODE_NAME, DEFAULT_CONFIG), DEFAULT_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(TEST_CHANNEL_NAME, DEFAULT_CONFIG), DEFAULT_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_NODE_NAME, DEFAULT_CONFIG), DEFAULT_PLUGINS_LINK_CHECKER);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_CHANNEL_NAME, DEFAULT_CONFIG), DEFAULT_PLUGINS_LINK_CHECKER);

		// global config
		EXPECTED_PLUGINS.put(Pair.of(TEST_NODE_NAME, GLOBAL_CONFIG), GLOBAL_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(TEST_CHANNEL_NAME, GLOBAL_CONFIG), GLOBAL_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_NODE_NAME, GLOBAL_CONFIG), GLOBAL_PLUGINS_LINK_CHECKER);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_CHANNEL_NAME, GLOBAL_CONFIG), GLOBAL_PLUGINS_LINK_CHECKER);

		// node config
		EXPECTED_PLUGINS.put(Pair.of(TEST_NODE_NAME, NODE_CONFIG), NODE_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(TEST_CHANNEL_NAME, NODE_CONFIG), DEFAULT_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_NODE_NAME, NODE_CONFIG), NODE_PLUGINS_LINK_CHECKER);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_CHANNEL_NAME, NODE_CONFIG), DEFAULT_PLUGINS_LINK_CHECKER);

		// channel config
		EXPECTED_PLUGINS.put(Pair.of(TEST_NODE_NAME, CHANNEL_CONFIG), DEFAULT_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(TEST_CHANNEL_NAME, CHANNEL_CONFIG), NODE_PLUGINS);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_NODE_NAME, CHANNEL_CONFIG), DEFAULT_PLUGINS_LINK_CHECKER);
		EXPECTED_PLUGINS.put(Pair.of(LINKCHECKER_TEST_CHANNEL_NAME, CHANNEL_CONFIG), NODE_PLUGINS_LINK_CHECKER);
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		testNode = supply(() -> createNode(TEST_NODE_NAME, "test.node", "/", "/", false, false, false));
		NODES.put(TEST_NODE_NAME, testNode);
		testChannel = supply(() -> createChannel(testNode, TEST_CHANNEL_NAME, "test.channel", "/"));
		NODES.put(TEST_CHANNEL_NAME, testChannel);

		linkCheckerTestNode = supply(() -> createNode(LINKCHECKER_TEST_NODE_NAME, "linkchecker.test.node", "/", "/",
				false, false, false, Feature.LINK_CHECKER));
		NODES.put(LINKCHECKER_TEST_NODE_NAME, linkCheckerTestNode);
		linkCheckerTestChannel = supply(() -> createChannel(linkCheckerTestNode, LINKCHECKER_TEST_CHANNEL_NAME,
				"linkchecker.test.channel", "/"));
		NODES.put(LINKCHECKER_TEST_CHANNEL_NAME, linkCheckerTestChannel);
	}

	@Parameters(name = "{index}: node {0}, configuration {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String nodeName : Arrays.asList(TEST_NODE_NAME, TEST_CHANNEL_NAME, LINKCHECKER_TEST_NODE_NAME,
				LINKCHECKER_TEST_CHANNEL_NAME)) {
			for (String file : configurations) {
				data.add(new Object[] { nodeName, file });
			}
		}
		return data;
	}

	@Parameter(0)
	public String nodeName;

	@Parameter(1)
	public String file;

	protected Node node;

	@Before
	public void setup() throws NodeException {
		node = NODES.get(nodeName);
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
		assertThat(plugins).as("Plugins").containsExactly(EXPECTED_PLUGINS.get(Pair.of(nodeName, file)));
	}
}
