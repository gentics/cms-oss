package com.gentics.contentnode.tests.config;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.response.NodeSettingsResponse;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for node specific configuration
 */
@RunWith(value = Parameterized.class)
public class NodeConfigTest {
	public final static String GENERAL_MAGIC_LINK_CONSTRUCT = "generalMagicLinkConstruct";

	public final static String SPECIFIC_MAGIC_LINK_CONSTRUCT = "specificMagicLinkConstruct";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Configuration "style"
	 */
	public static enum ConfigStyle {
		/**
		 * Configured with local node ID
		 */
		id,

		/**
		 * Configured with node UUID
		 */
		uuid,

		/**
		 * Configured with node name
		 */
		name;

		/**
		 * Get the style specific configuration key for the node
		 * @return key
		 */
		public String getKey() {
			switch (this) {
			case id:
				return Integer.toString(node.getId());
			case uuid:
				return node.getGlobalId().toString();
			case name:
				try {
					return supply(() -> node.getFolder().getName());
				} catch (NodeException e) {
					throw new RuntimeException(e);
				}
			default:
				fail("Unknown config style");
				return null;
			}
		}
	}

	private static Node node;
	private static Node otherNode;
	private static Page page;
	private static Page otherPage;
	private static SystemUser systemUser;

	private static Integer generalMagicLinkConstructId;
	private static Integer specificMagicLinkConstructId;

	private Page testPage;

	private Node testNode;

	@Parameters(name = "{index}: config style {0}, specific {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (ConfigStyle configStyle : ConfigStyle.values()) {
			for (boolean specific : Arrays.asList(true, false)) {
				data.add(new Object[] { configStyle, specific });
			}
		}
		return data;
	}

	/**
	 * Create nodes, pages and get systemuser
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		otherNode = supply(() -> createNode());

		page = supply(() -> createTemplateAndPage(node.getFolder(), "Page"));
		otherPage = supply(() -> createTemplateAndPage(otherNode.getFolder(), "Page"));
		systemUser = supply(t -> t.getObject(SystemUser.class, 1));

		generalMagicLinkConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, GENERAL_MAGIC_LINK_CONSTRUCT, "part"));
		specificMagicLinkConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, SPECIFIC_MAGIC_LINK_CONSTRUCT, "part"));
	}

	@Parameter(0)
	public ConfigStyle configStyle;

	@Parameter(1)
	public boolean specific;

	/**
	 * Get tested objects and prepare configuration according to tested style
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws NodeException
	 */
	@Before
	public void setup() throws IOException, URISyntaxException, NodeException {
		testPage = specific ? page : otherPage;
		testNode = specific ? node : otherNode;
		prepareConfig(configStyle.getKey());
	}

	/**
	 * Test node specific aloha plugins
	 * @throws NodeException
	 */
	@Test
	public void testAlohaPlugins() throws NodeException {
		String renderedScripts = Trx.supply(systemUser, () -> {
			try (RenderTypeTrx rTTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, testPage, false, false, false)) {
				AlohaRenderer alohaRenderer = new AlohaRenderer();
				rTTrx.get().setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, false);
				RenderResult result = new RenderResult();
				alohaRenderer.render(result, "");
				return result.getParameters().get(AlohaRenderer.ALOHA_SCRIPTS_PARAMETER)[0];
			}
		});

		if (specific) {
			assertThat(renderedScripts).as("Aloha Scripts").isEqualTo("<script type=\"text/javascript\" src=\"/alohaeditor/gcmsui-scripts-launcher.js\"></script><script src=\"/alohaeditor/TEST/lib/aloha.js\" data-aloha-plugins=\"gcn/gcn,specific/specific\"></script>");
		} else {
			assertThat(renderedScripts).as("Aloha Scripts").isEqualTo("<script type=\"text/javascript\" src=\"/alohaeditor/gcmsui-scripts-launcher.js\"></script><script src=\"/alohaeditor/TEST/lib/aloha.js\" data-aloha-plugins=\"gcn/gcn,general/general\"></script>");
		}
	}

	/**
	 * Test node specific aloha settings
	 * @throws NodeException
	 */
	@Test
	public void testAlohaSettings() throws NodeException {
		JsonNode settings = Trx.supply(systemUser, () -> {
			try (RenderTypeTrx rTTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, testPage, false, false, false)) {
				AlohaRenderer alohaRenderer = new AlohaRenderer();
				RenderResult result = new RenderResult();
				return alohaRenderer.getAlohaSettings(testPage.getOwningNode(), result, rTTrx.get(), new ObjectMapper());
			}
		});

		if (specific) {
			assertThat(settings.get("test").toString()).as("Aloha settings").isEqualTo("\"specific\"");
		} else {
			assertThat(settings.get("test").toString()).as("Aloha settings").isEqualTo("\"general\"");
		}
	}

	/**
	 * Test node specific node settings
	 * @throws Exception
	 */
	@Test
	public void testNodeSettings() throws Exception {
		try (Trx trx = new Trx()) {
			NodeSettingsResponse settings = getNodeResource().settings(Integer.toString(testNode.getId()));
			if (specific) {
				assertThat(settings.getData().toString()).as("Node settings").isEqualTo("{\"test\":\"specific_node\"}");
			} else {
				assertThat(settings.getData().toString()).as("Node settings").isEqualTo("{\"test\":\"general_node\"}");
			}
			trx.success();
		}
	}

	/**
	 * Test getting the magic link construct
	 * @throws NodeException
	 */
	@Test
	public void testMagicLinkConstruct() throws NodeException {
		String magicLinkConstructKeyword = Trx.supply(() -> AlohaRenderer.getMagicLinkConstructKeyword(testNode));
		String expected = specific ? SPECIFIC_MAGIC_LINK_CONSTRUCT : GENERAL_MAGIC_LINK_CONSTRUCT;
		assertThat(magicLinkConstructKeyword).as("Magic Link Construct").isEqualTo(expected);
	}

	/**
	 * Prepare the config file with the given configuration key
	 * @param key configuration key
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws NodeException
	 */
	protected void prepareConfig(String key) throws IOException, URISyntaxException, NodeException {
		String config = FileUtils.readFileToString(new File(NodeConfigTest.class.getResource("node_specific_config.yml").toURI()));
		config = config.replaceAll("\\{\\{key\\}\\}", key);
		config = config.replaceAll("\\{\\{" + GENERAL_MAGIC_LINK_CONSTRUCT + "\\}\\}", Integer.toString(generalMagicLinkConstructId));
		config = config.replaceAll("\\{\\{" + SPECIFIC_MAGIC_LINK_CONSTRUCT + "\\}\\}", Integer.toString(specificMagicLinkConstructId));
		File configFile = new File(testContext.getGcnBasePath(), "node_specific_config.yml");
		FileUtils.write(configFile, config);

		System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), configFile.getAbsolutePath());
		NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();
	}
}
