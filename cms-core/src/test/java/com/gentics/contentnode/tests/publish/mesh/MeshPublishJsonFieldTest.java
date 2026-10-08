package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.object.parttype.groovy.GroovyPartType;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.json.JsonUtil;

/**
 * Test cases for publishing data into json fields in mesh
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
@RunWith(value = Parameterized.class)
public class MeshPublishJsonFieldTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	/**
	 * Name of the test tag in the template
	 */
	public final static String TEST_TAG_NAME = "testtag";

	/**
	 * Name of the groovy part in the test construct
	 */
	public final static String GROOVY_PART_NAME = "script";

	/**
	 * Name of the json part in the test construct
	 */
	public final static String JSON_PART_NAME = "json";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	private static Template template;

	private static String crEntryId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Parameter(0)
	public String description;

	@Parameter(1)
	public TestCase testCase;

	private static Construct testConstruct;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String executeGroovyScript = "page.tags.%s.parts.%s.execute".formatted(TEST_TAG_NAME, GROOVY_PART_NAME);
		String getJson = "page.tags.%s.parts.%s".formatted(TEST_TAG_NAME, JSON_PART_NAME);

		Collection<Object[]> data = new ArrayList<>();

		// test cases with groovy
		data.add(new Object[] { "groovy returns object", new TestCase().withTagName(executeGroovyScript)
				.withGroovyScript("return [\"json\": \"from object\"]").withExpectedJson("""
							{
								"json": "from object"
							}
						""") });

		data.add(new Object[] { "groovy returns array", new TestCase().withTagName(executeGroovyScript)
				.withGroovyScript("return [\"json\", \"from\", \"array\"]").withExpectedJson("""
							[
								"json",
								"from",
								"array"
							]
						""") });

		data.add(new Object[] {"groovy returns json string", new TestCase().withTagName(executeGroovyScript).withGroovyScript("""
				return "{\\"json\\": \\"from string\\"}"
				""").withExpectedJson("""
						{"json": "from string"}
						""")});

		data.add(new Object[] {"groovy returns non-json string", new TestCase().withTagName(executeGroovyScript).withGroovyScript("""
				return "this is a normal string"
				""").withExpectedJson("""
						"this is a normal string"
						""")});

		data.add(new Object[] {"groovy returns boolean", new TestCase().withTagName(executeGroovyScript).withGroovyScript("""
				return true
				""").withExpectedJson("""
						true
						""")});

		data.add(new Object[] {"groovy returns number", new TestCase().withTagName(executeGroovyScript).withGroovyScript("""
				return 4711
				""").withExpectedJson("""
						4711
						""")});

		// test cases with JSON part
		String objectJson = """
				{
					"json": "from part as object"
				}
				""";
		data.add(new Object[] {"object from json part", new TestCase().withTagName(getJson).withJson(objectJson).withExpectedJson(objectJson)});
		String arrayJson = """
				["json", "from", "part", "as", "array"]
				""";
		data.add(new Object[] {"array from json part", new TestCase().withTagName(getJson).withJson(arrayJson).withExpectedJson(arrayJson)});
		data.add(new Object[] {"boolean from json part", new TestCase().withTagName(getJson).withJson("true").withExpectedJson("true")});
		String stringJson = """
				"json from a string"
				""";
		data.add(new Object[] {"string from json part", new TestCase().withTagName(getJson).withJson(stringJson).withExpectedJson(stringJson)});

		return data;
	}

	/**
	 * Setup static test data
	 *
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		ContentLanguage german = supply(() -> getLanguage("de"));
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, german));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		TagmapEntryModel entry = new TagmapEntryModel();
		entry.setObject(Page.TYPE_PAGE);
		entry.setAttributeType(TagmapEntry.AttributeType.json.getType());
		entry.setTagname("empty");
		entry.setMapname("jsonfield");
		TagmapEntryResponse response = crResource.addEntry(Integer.toString(crId), entry);
		assertResponseOK(response);
		crEntryId = Integer.toString(response.getEntry().getId());

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		testConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("testconstruct");
			c.setName("Test Construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(GROOVY_PART_NAME);
				p.setName(GROOVY_PART_NAME, 1);
				p.setPartTypeId(getPartTypeId(GroovyPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}).doNotSave().build());
			}).doNotSave().build());

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(JSON_PART_NAME);
				p.setName(GROOVY_PART_NAME, 1);
				p.setPartTypeId(getPartTypeId(JSONPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
				}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		template = Builder.update(template, update -> {
			TemplateTag templateTag = Builder.create(TemplateTag.class, tag -> {
				tag.setConstructId(testConstruct.getId());
				tag.setEnabled(true);
				tag.setName(TEST_TAG_NAME);
				tag.setPublic(true);
			}).doNotSave().build();

			update.getTags().put(TEST_TAG_NAME, templateTag);
		}).build();
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Folder folder : node.getFolder().getChildFolders()) {
				t.getObject(folder, true).delete(true);
			}
		});
	}

	@Test
	public void test() throws Exception {
		// 1. Prepare tagmap entry
		TagmapEntryModel entry = new TagmapEntryModel();
		entry.setTagname(testCase.tagName);
		crResource.updateEntry(Integer.toString(crId), crEntryId,  entry);

		// 2. Prepare data (page)
		Folder testFolder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("/");
		}).build();

		Page testPage = Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(testFolder.getId());
			p.setName("Testpage");

			ContentTag testTag = p.getContentTag(TEST_TAG_NAME);
			getPartType(GroovyPartType.class, testTag, GROOVY_PART_NAME).setText(testCase.groovyScript);
			getPartType(JSONPartType.class, testTag, JSON_PART_NAME).setText(testCase.json);
		}).publish().unlock().build();

		// 3. Run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// 4. Assert data in Mesh
		assertObject("Published Page", mesh.client(), MESH_PROJECT_NAME, testPage, true, meshNode -> {
			assertThat(meshNode.getFields().getJsonField("jsonfield")).isNotNull().hasFieldOrPropertyWithValue("json",
					JsonUtil.toJsonNode(testCase.expectedJson));
		});
	}

	/**
	 * Encapsulation of a testcase
	 */
	protected static class TestCase {
		/**
		 * Tagname of the tagmap entry
		 */
		protected String tagName;

		/**
		 * Groovy script
		 */
		protected String groovyScript;

		/**
		 * JSON stored in the json part
		 */
		protected String json;

		/**
		 * Expected published JSON
		 */
		protected String expectedJson;

		/**
		 * Set the tagName of the tagmap entry
		 * @param tagName tagName
		 * @return fluent API
		 */
		public TestCase withTagName(String tagName) {
			this.tagName = tagName;
			return this;
		}

		/**
		 * Set the groovy script
		 * @param groovyScript script
		 * @return fluent API
		 */
		public TestCase withGroovyScript(String groovyScript) {
			this.groovyScript = groovyScript;
			return this;
		}

		/**
		 * Set the JSON to be stored in the JSON part
		 * @param json JSON
		 * @return fluent API
		 */
		public TestCase withJson(String json) {
			this.json = json;
			return this;
		}

		/**
		 * Set the expected published JSON
		 * @param expectedJson expected published JSON
		 * @return fluent API
		 */
		public TestCase withExpectedJson(String expectedJson) {
			this.expectedJson = expectedJson;
			return this;
		}
	}
}
