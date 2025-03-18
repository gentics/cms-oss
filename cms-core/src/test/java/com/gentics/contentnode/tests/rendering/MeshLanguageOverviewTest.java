package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.renderer.EchoRenderer;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;

/**
 * Test cases for overviews with languages with Mesh
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshLanguageOverviewTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(PreviewResource.class).build()));

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test CR
	 */
	private static Integer meshCrId;

	/**
	 * Overview construct ID
	 */
	private static Integer overviewConstructId;

	/**
	 * Template
	 */
	private static Template template;

	/**
	 * Page
	 */
	private static Page page;

	private static ContentLanguage german;

	private static ContentLanguage english;

	private static Page englishPage;

	private static Page germanPage;

	private static Page bothEnglishPage;

	private static Page bothGermanPage;

	private static Page noLanguagePage;

	@Parameter(0)
	public boolean dsfallback;

	@Parameter(1)
	public boolean dsEmptyCs;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: dsfallback {0}, ds_empty_cs {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean dsfallback : Arrays.asList(true, false)) {
			for (boolean dsEmptyCs : Arrays.asList(true, false)) {
				data.add(new Object[] { dsfallback, dsEmptyCs });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		RendererFactory.registerRenderer("echo", new EchoRenderer());
		meshCrId = createMeshCR(mesh, MESH_PROJECT_NAME);
		addTagmapEntry(Page.TYPE_PAGE, "overview", "page.tags.overview", GenticsContentAttribute.ATTR_TYPE_TEXT);

		german = Trx.supply(
				t -> t.getObject(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup WHERE code = 'de'", DBUtils.IDS).iterator().next()));
		english = Trx.supply(
				t -> t.getObject(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup WHERE code = 'en'", DBUtils.IDS).iterator().next()));

		node = Trx.supply(() -> createNode("host", "Node", PublishTarget.CONTENTREPOSITORY, german, english));
		overviewConstructId = Trx.supply(() -> createConstruct(node, OverviewPartType.class, "overview", "overview"));
		template = create(Template.class, t -> {
			t.setSource("");
			t.setName("Template");
			t.addFolder(node.getFolder());
		});

		englishPage = Trx.supply(() -> update(createPage(node.getFolder(), template, "English"), p -> {
			p.setLanguage(english);
			p.publish();
		}));
		germanPage = Trx.supply(() -> update(createPage(node.getFolder(), template, "German"), p -> {
			p.setLanguage(german);
			p.publish();
		}));
		bothEnglishPage = Trx.supply(() -> update(createPage(node.getFolder(), template, "Both - English"), p -> {
			p.setLanguage(english);
			p.publish();
		}));
		bothGermanPage = Trx.supply(() -> {
			Page copy = (Page) bothEnglishPage.copy();
			copy.setLanguage(german);
			copy.setName("Both - German");
			copy.save();

			copy.publish();
			return copy.reload();
		});
		noLanguagePage = Trx.supply(() -> update(createPage(node.getFolder(), template, "No Language"), p -> {
			p.publish();
		}));

		page = Trx.supply(() -> update(createPage(node.getFolder(), template, "Testpage"), p -> {
			p.getContent().getContentTags().put("overview", create(ContentTag.class, tag -> {
				tag.setConstructId(overviewConstructId);
				tag.setEnabled(true);
				tag.setName("overview");

				fillOverview(tag, "overview", "[<node page.id>|<node page.name>|<node page.language.code>]", Page.class, Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_SELECT, Overview.ORDERWAY_ASC, false,
						Arrays.asList(englishPage, germanPage, bothEnglishPage, noLanguagePage));
			}, false));
			p.setLanguage(german);
		}));
	}

	/**
	 * Add a tagmap entry to the CR
	 * @param objectType object type
	 * @param mapName map name
	 * @param tagName tag name
	 * @param attributeType attribute type
	 * @throws Exception
	 */
	protected static void addTagmapEntry(int objectType, String mapName, String tagName, int attributeType) throws Exception {
		TagmapEntryModel entry = new TagmapEntryModel();
		entry.setObject(objectType);
		entry.setMapname(mapName);
		entry.setTagname(tagName);
		entry.setAttributeType(attributeType);

		TagmapEntryResponse response = crResource.addEntry(String.valueOf(meshCrId), entry);
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	/**
	 * Add a tagmap entry to the CR
	 * @param objectType object type
	 * @param mapName map name
	 * @param tagName tag name
	 * @param attributeType attribute type
	 * @param targetType target type
	 * @param multivalue true for multivalue
	 * @throws Exception
	 */
	protected static void addTagmapEntry(int objectType, String mapName, String tagName, int attributeType, int targetType, boolean multivalue) throws Exception {
		TagmapEntryModel entry = new TagmapEntryModel();
		entry.setObject(objectType);
		entry.setMapname(mapName);
		entry.setTagname(tagName);
		entry.setAttributeType(attributeType);
		entry.setTargetType(targetType);
		entry.setMultivalue(multivalue);

		TagmapEntryResponse response = crResource.addEntry(String.valueOf(meshCrId), entry);
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	/**
	 * Setup the test node, clean and repair mesh CR
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		cleanMesh(mesh.client());
		node = update(node, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(meshCrId);
			n.setMeshPreviewUrl(appContext.getBaseUri() + "preview/echo");
			n.setPubDirSegment(true);
		});
		operate(() -> assertThat(node.getContentRepository().checkStructure(true)).as("Structure valid").isTrue());
	}

	/**
	 * Test preview
	 * @throws NodeException
	 */
	@Test
	public void testPreview() throws NodeException {
		try (FeatureClosure f1 = new FeatureClosure(Feature.DSFALLBACK, dsfallback)) {
			try (FeatureClosure f2 = new FeatureClosure(Feature.DS_EMPTY_CS, dsEmptyCs)) {
				String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
				assertThat(preview).as("Preview").isNotNull();
				NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
				assertOverview(sentNode);
			}
		}
	}

	/**
	 * Test instant publishing
	 * @throws NodeException
	 */
	@Test
	public void testInstantPublishing() throws NodeException {
		try (FeatureClosure f1 = new FeatureClosure(Feature.DSFALLBACK, dsfallback)) {
			try (FeatureClosure f2 = new FeatureClosure(Feature.DS_EMPTY_CS, dsEmptyCs)) {
				operate(t -> {
					update(t.getObject(ContentRepository.class, meshCrId), cr -> {
						cr.setInstantPublishing(true);
					});
				});

				operate(() -> update(page, Page::publish));

				assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshNode -> {
					assertOverview(meshNode);
				});
			}
		}
	}

	/**
	 * Test regular publishing
	 * @throws Exception
	 */
	@Test
	public void testPublishing() throws Exception {
		try (FeatureClosure f1 = new FeatureClosure(Feature.DSFALLBACK, dsfallback)) {
			try (FeatureClosure f2 = new FeatureClosure(Feature.DS_EMPTY_CS, dsEmptyCs)) {
				operate(t -> {
					update(t.getObject(ContentRepository.class, meshCrId), cr -> {
						cr.setInstantPublishing(false);
					});
				});

				operate(() -> update(page, Page::publish));

				// run publish process
				try (Trx trx = new Trx()) {
					context.publish(false);
					trx.success();
				}

				assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshNode -> {
					assertOverview(meshNode);
				});
			}
		}
	}

	/**
	 * Assert overview field
	 * @param node node
	 * @throws NodeException
	 */
	protected void assertOverview(NodeResponse node) throws NodeException {
		assertThat(node.getFields().getStringField("overview")).as("Field 'overview'").isNotNull();
		String renderedOverview = node.getFields().getStringField("overview").getString();

		if (dsfallback) {
			if (dsEmptyCs) {
				assertThat(renderedOverview).as("Field 'overview' value")
						.isEqualTo(expectedOverviewOutput(englishPage, germanPage, bothGermanPage, noLanguagePage));
			} else {
				assertThat(renderedOverview).as("Field 'overview' value")
						.isEqualTo(expectedOverviewOutput(englishPage, germanPage, bothGermanPage));
			}
		} else {
			if (dsEmptyCs) {
				assertThat(renderedOverview).as("Field 'overview' value")
						.isEqualTo(expectedOverviewOutput(germanPage, bothGermanPage, noLanguagePage));
			} else {
				assertThat(renderedOverview).as("Field 'overview' value")
						.isEqualTo(expectedOverviewOutput(germanPage, bothGermanPage));
			}
		}
	}

	/**
	 * Get the expected output containing the given pages
	 * @param pages pages
	 * @return expected output
	 * @throws NodeException
	 */
	protected String expectedOverviewOutput(Page...pages) throws NodeException {
		StringBuilder out = new StringBuilder();
		for (Page page : pages) {
			out.append(Trx
					.supply(() -> String.format("[%s|%s|%s]", page.getId(), page.getName(), page.getLanguage() != null ? page.getLanguage().getCode() : "")));
		}
		return out.toString();
	}

	/**
	 * Preview Resource
	 */
	@Path("/preview")
	public final static class PreviewResource {
		/**
		 * This method will return the posted body
		 * @param path path
		 * @param body posted body
		 * @return posted body
		 */
		@POST
		@Path("/echo/{path: .*}")
		public String echo(@PathParam("path") String path, String body) {
			return body;
		}

		/**
		 * This method will return the path
		 * @param path path
		 * @param body posted body
		 * @return path
		 */
		@POST
		@Path("/path/{path: .*}")
		public String path(@PathParam("path") String path, String body) {
			return path;
		}
	}
}
