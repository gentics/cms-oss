package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

import okhttp3.FormBody;
import okhttp3.FormBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Abstract base class for form publish tests
 */
@Category(MeshTest.class)
@ConfigBy(FormTypeConfigurationModifier.class)
public abstract class AbstractFormPublishTest {
	/**
	 * Name of the HandlebarsPartType Part
	 */
	public final static String HBS_PART_NAME = "hbs";

	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "form_project";

	public final static String formsPluginFileName = "mesh-forms-plugin.jar";

	public final static String formsPluginFilePath = "target/plugins/" + formsPluginFileName;

	public static File formsPluginJarFile;

	public static File formsPluginConfigDir;

	public final static String andpPluginFileName = "mesh-andp-plugin.jar";

	public final static String andpPluginFilePath = "target/plugins/" + andpPluginFileName;

	public static File andpPluginJarFile;

	public static File andpPluginConfigDir;

	public static File licenseKeyFile;

	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	static {
		try {
			// forms plugin
			formsPluginJarFile = new File(formsPluginFilePath);
			assertThat(formsPluginJarFile).as("Forms Plugin File").exists().canRead().isFile();
			formsPluginConfigDir = new File(FormPublishTest.class.getResource("/forms/config").toURI());

			// andp plugin
			andpPluginJarFile = new File(andpPluginFilePath);
			assertThat(andpPluginJarFile).as("ANDP Plugin File").exists().canRead().isFile();
			andpPluginConfigDir = new File(FormPublishTest.class.getResource("/andp/config").toURI());

			licenseKeyFile = new File(FormPublishTest.class.getResource("/license.key").toURI());
		} catch (URISyntaxException e) {
			fail("Error while preparing plugin", e);
		}
	}

	private static final String EXTERNAL_CMS_URL = "http://cms.domain.test";
	protected static final String FIELD_DOWNLOAD_BASE_URL = "downloadBaseUrl";
	protected static final String FMT_DOWNLOAD_BASE_URL = EXTERNAL_CMS_URL + "/editor/#proxy/rest/form/%d/data";

	@ClassRule
	public static DBTestContext context = new DBTestContext().config(prefs -> {
		prefs.setProperty("cn_external_server", EXTERNAL_CMS_URL);
	});

	/**
	 * Start a container with Mesh and the deployed forms plugin
	 */
	@ClassRule
	public static MeshContext mesh = new MeshContext()
			.withFileSystemBind(formsPluginJarFile.getAbsolutePath(), "/plugins/" + formsPluginFileName, BindMode.READ_ONLY)
			.withFileSystemBind(formsPluginConfigDir.getAbsolutePath(), "/plugins/forms", BindMode.READ_ONLY)
			.withFileSystemBind(andpPluginJarFile.getAbsolutePath(), "/plugins/" + andpPluginFileName, BindMode.READ_ONLY)
			.withFileSystemBind(andpPluginConfigDir.getAbsolutePath(), "/plugins/andp", BindMode.READ_ONLY)
			.withFileSystemBind(licenseKeyFile.getAbsolutePath(), "/mesh/license.key", BindMode.READ_ONLY);

	protected static final Logger log = LoggerFactory.getLogger(AbstractFormPublishTest.class);

	protected static Node node;

	protected static Integer crId;

	protected static ContentRepository otherCR;

	protected static Construct formConstruct;

	protected static Integer hbsConstructId;

	protected static Template template;

	protected static OkHttpClient okHttpClient;

	protected static Template eMailTemplate;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Rule
	public ExceptionChecker checker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, getLanguage("de"), getLanguage("en")));

		otherCR = supply(() -> createContentRepository("Other CR", false, false, ""));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
		}));
		node = update(node, n -> {
			n.setContentrepositoryId(crId);
			n.activateFeature(Feature.FORMS);
		});

		formConstruct = supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("form");
			c.setName("form", 1);
			c.getNodes().add(node);

			// HBS Part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname(HBS_PART_NAME);
				p.setName(HBS_PART_NAME, 1);
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
					v.setPart(p);
					v.setValueText("{{#gtx_form cms.tag.parts.form type=\"java\"}}{{this}}{{else}}No Form{{/gtx_form}}");
				}, false));
			}, false));

			// Form part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("form");
				p.setName("form", 1);
				p.setPartTypeId(getPartTypeId(CmsFormPartType.class));
			}, false));
		}));

		hbsConstructId = supply(() -> createConstruct(node, HandlebarsPartType.class, HBS_PART_NAME, HBS_PART_NAME));

		template = supply(() -> update(createTemplate(node.getFolder(), "Template"), tmpl -> {
			tmpl.setSource("<node form>");
			tmpl.getTemplateTags().put("form", create(TemplateTag.class, tt -> {
				tt.setConstructId(formConstruct.getId());
				tt.setEnabled(true);
				tt.setName("form");
				tt.setPublic(true);
			}, false));
		}));

		MarkupLanguage formsTemplateMarkupLanguage = supply(t -> t.getObject(MarkupLanguage.class, DBUtils.select("SELECT id FROM ml WHERE feature = ?",
				ps -> ps.setString(1, "forms"), DBUtils.firstInt("id"))));
		assertThat(formsTemplateMarkupLanguage).as("Forms e-mail template ml").isNotNull();

		eMailTemplate = create(Template.class, create -> {
			create.setSource("<node content>");
			create.setName("E-Mail Template");
			create.addFolder(node.getFolder());
			create.setMlId(formsTemplateMarkupLanguage.getId());
			create.getTemplateTags().put("content", create(TemplateTag.class, tt -> {
				tt.setConstructId(hbsConstructId);
				tt.setEnabled(true);
				tt.setName("content");
				tt.setPublic(true);
			}, false));
		});

		okHttpClient = new OkHttpClient();
	}

	@Before
	public void setup() throws NodeException {
		cleanMesh(mesh.client());

		operate(() -> clear(node));

		operate(() -> {
			DBUtils.update("DELETE FROM publishqueue");
			DBUtils.update("DELETE FROM dirtqueue");
		});

		node = update(node, n -> {
			n.setContentrepositoryId(crId);
			n.activateFeature(Feature.FORMS);
		});

		operate(() -> node.deactivateFeature(Feature.DISABLE_INSTANT_DELETE));

		operate(t -> {
			update(t.getObject(ContentRepository.class, crId), upd -> {
				upd.setInstantPublishing(false);
			});
		});

		for (Node channel : execute(Node::getChannels, node)) {
			consume(c -> {
				c.delete(true);
			}, channel);
		}
	}

	/**
	 * Prepare for instant publishing (run the publish process to activate the forms plugin and activate instant publishing for the CR)
	 * @throws Exception
	 */
	protected void prepareForInstantPublishing() throws Exception {
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(t -> {
			update(t.getObject(ContentRepository.class, crId), upd -> {
				upd.setInstantPublishing(true);
			});
		});
	}

	/**
	 * Post some data to a form
	 * @param projectName project name
	 * @param pluginName plugin name
	 * @param formUuid form uuid
	 * @param data data as key/value pairs
	 * @throws IOException
	 */
	@SafeVarargs
	protected final void postFormData(String projectName, String pluginName, String formUuid, Pair<String, String>...data) throws IOException {
		ObjectNode dataObject = mapper.createObjectNode();
		dataObject.put("formID", formUuid);
		dataObject.put("hasValidationErrors", false);
		ObjectNode properties = dataObject.withObjectProperty("properties");
		for (Pair<String, String> pair : data) {
			properties.withObjectProperty(pair.getKey()).put("value", pair.getValue());
		}

		Builder bodyBuilder = new FormBody.Builder();
		bodyBuilder.add("formdata", dataObject.toString());

		Request request = new Request.Builder()
				.url("http://" + mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080) + "/api/v1/"
						+ projectName + "/plugins/" + pluginName + "/form/" + formUuid + "/submit?lang=en")
				.post(bodyBuilder.build())
			.build();
		try (Response response = okHttpClient.newCall(request).execute()) {
			assertThat(response.isSuccessful()).as("POSTing data succeeded (response code is " + response.code() + ")")
					.isTrue();
		}
	}

	/**
	 * Expect that the form has been published with the JSON from the the given file
	 * @param form form
	 * @param expectedDataFile name of the file containing the expected JSON
	 * @throws NodeException
	 * @throws IOException
	 */
	protected void assertPublishedForm(Form form, String expectedDataFile) throws NodeException, IOException {
		AtomicReference<ObjectNode> expected = new AtomicReference<>();
		try (InputStream in = getClass().getResourceAsStream(expectedDataFile)) {
			expected.set(mapper.readValue(in, ObjectNode.class));
		}
		expected.get()
			.put(FIELD_DOWNLOAD_BASE_URL, String.format(FMT_DOWNLOAD_BASE_URL, form.getId()))
			.put("cms_id", form.getId())
			.put("type", form.getFormType())
			.put("name", form.getName())
			.put("submitOnce", FormConfigProvider.getConfig().get(form.getFormType()).submitOnce);

		operate(() -> assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), null, form, "en", true,
				response -> {
					try {
						JsonNode actual = mapper.readValue(response.getFields().getStringField("json").getString(),
								ObjectNode.class);
						assertThat(actual).as("Published JSON").isEqualTo(expected.get());
					} catch (IOException e) {
						throw new NodeException(e);
					}
				}));

	}
}
