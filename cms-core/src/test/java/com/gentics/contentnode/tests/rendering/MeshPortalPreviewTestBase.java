package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.renderer.EchoRenderer;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;

/**
 * Test cases for the "portal preview" of a Mesh portal
 */
public abstract class MeshPortalPreviewTestBase {
	@ClassRule
	public static DBTestContext context = new DBTestContext().config(map -> {
		// set call timeout to 5 seconds
		map.setProperty("mesh.client.callTimeout", "5");
	});

	/**
	 * Comparator for Rest Models
	 */
	private static final Comparator<RestModel> MODEL_COMPARATOR = Comparator.comparing(RestModel::toJson);

	/**
	 * Test node
	 */
	protected static Node node;

	/**
	 * Test CR
	 */
	private static Integer meshCrId;

	/**
	 * ID of a non-mesh CR
	 */
	private static Integer otherCrId;

	/**
	 * Construct ID
	 */
	private static Integer constructId;

	/**
	 * Template
	 */
	private static Template template;

	/**
	 * Page
	 */
	private static Page page;

	private static Page translation;

	/**
	 * Construct containing a single live editable part
	 */
	private static Construct liveEditableConstruct;

	/**
	 * Set of tagmap entries (global IDs) that were created by the test (will be cleaned in teardown)
	 */
	private Set<String> testTagmapEntries = new HashSet<>();

	@BeforeClass
	public static void setupOnce() throws Exception {
		RendererFactory.registerRenderer("echo", new EchoRenderer());
		meshCrId = createMeshCR("localhost", 1234, "test");
		otherCrId = Trx.supply(() -> createContentRepository("Other", false, false, "bla").getId());
		addTagmapEntry(Page.TYPE_PAGE, "tag", "page.tags.tag", GenticsContentAttribute.ATTR_TYPE_TEXT, false);
		addTagmapEntry(Page.TYPE_PAGE, "live", "page.tags.live", GenticsContentAttribute.ATTR_TYPE_TEXT, false);
		addTagmapEntry(Page.TYPE_PAGE, "tagpart", "page.tags.tag.parts.part", GenticsContentAttribute.ATTR_TYPE_TEXT, false);
		addTagmapEntry(Page.TYPE_PAGE, "pageurl", "page.url", GenticsContentAttribute.ATTR_TYPE_TEXT, false);
		addTagmapEntry(Page.TYPE_PAGE, "pagelink", "page", GenticsContentAttribute.ATTR_TYPE_OBJ, Page.TYPE_PAGE, false);
		addTagmapEntry(Page.TYPE_PAGE, "pagelinks", "page.folder.pages", GenticsContentAttribute.ATTR_TYPE_OBJ, Page.TYPE_PAGE, true);
		addTagmapEntry(Page.TYPE_PAGE, "foldername", "page.folder.name", GenticsContentAttribute.ATTR_TYPE_TEXT, false);

		node = Trx.supply(() -> createNode("host", "Node", PublishTarget.CONTENTREPOSITORY, getLanguage("de"), getLanguage("en")));
		constructId = Trx.supply(() -> createConstruct(node, LongHTMLPartType.class, "construct", "part"));

		liveEditableConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setIconName("icon");
			c.setKeyword("live");
			c.setMayBeSubtag(false);
			c.setMayContainSubtags(true);
			c.setName("live", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(2);
				p.setHidden(false);
				p.setKeyname("html");
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));
		}));
		Trx.supply(() -> createConstruct(node, LongHTMLPartType.class, "live", "html"));

		template = create(Template.class, t -> {
			t.setSource("<div class=\"tag\"><node tag></div><div class=\"live\"><node live></div>");
			t.setName("Template");
			t.addFolder(node.getFolder());

			t.getTags().put("tag", create(TemplateTag.class, tag -> {
				tag.setConstructId(constructId);
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName("tag");
			}, false));

			t.getTags().put("live", create(TemplateTag.class, tag -> {
				tag.setConstructId(liveEditableConstruct.getId());
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName("live");
			}, false));
		});
		page = Trx.supply(() -> update(createPage(node.getFolder(), template, "Testpage", null, getLanguage("de")), p -> {
			ContentTag newTag = p.getContent().addContentTag(constructId);
			newTag.getValues().getByKeyname("part").setValueText("Embedded content");
			p.getContentTag("tag").getValues().getByKeyname("part").setValueText("This is the content");
			p.getContentTag("live").getValues().getByKeyname("html")
					.setValueText("Live <b>editable</b> content, containing also embedded tags: [<node " + newTag.getName() + ">].");
		}));

		// create the language variant
		translation = supply(() -> {
			Page translation = (Page) page.copy();
			translation.setLanguage(getLanguage("en"));
			translation.save();
			translation.unlock();
			return translation.reload();
		});

		update(node.getFolder(), upd -> {
			upd.setNameI18n(new I18nMap().put("de", "Ordner Name auf Deutsch"));
		});
	}

	/**
	 * Add a tagmap entry to the CR
	 * @param objectType object type
	 * @param mapName map name
	 * @param tagName tag name
	 * @param attributeType attribute type
	 * @param multivalue multivalue flag
	 * @return tagmap entry model
	 * @throws Exception
	 */
	protected static TagmapEntryModel addTagmapEntry(int objectType, String mapName, String tagName, int attributeType, boolean multivalue) throws Exception {
		TagmapEntryModel entry = new TagmapEntryModel();
		entry.setObject(objectType);
		entry.setMapname(mapName);
		entry.setTagname(tagName);
		entry.setAttributeType(attributeType);
		entry.setMultivalue(multivalue);

		TagmapEntryResponse response = crResource.addEntry(String.valueOf(meshCrId), entry);
		ContentNodeRESTUtils.assertResponseOK(response);
		return response.getEntry();
	}

	/**
	 * Add a tagmap entry for testing
	 * @param objectType object type
	 * @param mapName map name
	 * @param tagName tag name
	 * @param attributeType attribute type
	 * @param multivalue multivalue flag
	 * @throws Exception
	 */
	protected void addTestTagmapEntry(int objectType, String mapName, String tagName, int attributeType, boolean multivalue) throws Exception {
		testTagmapEntries.add(addTagmapEntry(objectType, mapName, tagName, attributeType, multivalue).getGlobalId());
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
	protected static void addTagmapEntry(int objectType, String mapName, String tagName, int attributeType, int targetType, boolean multivalue)
			throws Exception {
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
	 * Setup the test node
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		update(node, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(meshCrId);
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/echo");
			n.setPubDirSegment(true);
		});

		// make the live editable construct live editable (this might be changed by tests)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(2);
		});
	}

	/**
	 * Clean test data
	 * @throws Exception
	 */
	@After
	public void after() throws Exception {
		for (String entryId : testTagmapEntries) {
			crResource.deleteEntry(meshCrId.toString(), entryId);
		}
		testTagmapEntries.clear();
	}

	/**
	 * Test that portal preview is null when no CR set
	 * @throws NodeException
	 */
	@Test
	public void testNoCR() throws NodeException {
		update(node, n -> {
			n.setContentrepositoryId(null);
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when non-mesh CR is set
	 * @throws NodeException
	 */
	@Test
	public void testWrongCR() throws NodeException {
		update(node, n -> {
			n.setContentrepositoryId(otherCrId);
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when node does not publish into CR
	 * @throws NodeException
	 */
	@Test
	public void testNotPublishCR() throws NodeException {
		update(node, n -> {
			n.setPublishContentmap(false);
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when feature is off
	 * @throws NodeException
	 */
	@Test
	public void testNoFeature() throws NodeException {
		try (FeatureClosure feature = new FeatureClosure(Feature.MESH_CONTENTREPOSITORY, false)) {
			String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
			assertThat(preview).as("Preview").isNull();
		}
	}

	/**
	 * Test that portal preview is null when preview URL is not set
	 * @throws NodeException
	 */
	@Test
	public void testNoPreviewUrl() throws NodeException {
		update(node, n -> {
			n.setMeshPreviewUrl(null);
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test data posted to preview URL for Preview
	 * @throws NodeException
	 */
	@Test
	public void testPreview() throws NodeException {
		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getUuid()).as("UUID").isEqualTo(MeshPublisher.getMeshUuid(page));
		assertThat(sentNode.getFields().getStringField("tag")).as("Field 'tag'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tag").getString()).as("Field 'tag' value").isEqualTo("This is the content");
		assertThat(sentNode.getFields().getStringField("tagpart")).as("Field 'tagpart'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tagpart").getString()).as("Field 'tagpart' value").isEqualTo("This is the content");
		assertThat(sentNode.getFields().getNodeField("pagelink")).as("Field 'pagelink'").isNotNull();
		assertThat(sentNode.getFields().getNodeField("pagelink").getUuid()).as("Field 'pagelink'").isEqualTo(MeshPublisher.getMeshUuid(page));
		assertThat(sentNode.getFields().getNodeFieldList("pagelinks")).as("Field 'pagelinks'").isNotNull();
		assertThat(sentNode.getFields().getNodeFieldList("pagelinks").getItems()).as("Field 'pagelinks'").usingElementComparatorOnFields("uuid")
				.containsOnly(new NodeFieldListItemImpl().setUuid(MeshPublisher.getMeshUuid(page)));
	}

	/**
	 * Test data posted to preview URL for Edit Mode
	 * @throws NodeException
	 */
	@Test
	public void testEdit() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));

		// make the live editable construct not live editable, because that would spoil the response (so that it is not valid json any more)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(1);
		});

		String expectedEdit = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render("<node tag>", TransactionManager.getCurrentTransaction().getRenderResult(), null, null, null, null);
			}
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getUuid()).as("UUID").isEqualTo(MeshPublisher.getMeshUuid(page));
		assertThat(sentNode.getFields().getStringField("tag")).as("Field 'tag'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tag").getString()).as("Field 'tag' value").isEqualTo(expectedEdit);
		assertThat(sentNode.getFields().getStringField("tagpart")).as("Field 'tagpart'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tagpart").getString()).as("Field 'tagpart' value").isEqualTo("This is the content");
	}

	/**
	 * Test path sent to preview URL
	 * @throws NodeException
	 */
	@Test
	public void testPreviewPath() throws NodeException {
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/path");
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Posted path").isEqualTo("Content.node/home/");
	}

	/**
	 * Test preview with a micronode
	 * @throws Exception
	 */
	@Test
	public void testMicronode() throws Exception {
		addTestTagmapEntry(Page.TYPE_PAGE, "singletag", "page.tags.tag", AttributeType.micronode.getType(), false);

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);

		assertThat(sentNode.getFields().getMicronodeField("singletag")).as("Field 'singletag'").usingComparator(MODEL_COMPARATOR)
				.isEqualTo(reference("This is the content"));
	}

	/**
	 * Test preview with list of micronodes
	 * @throws Exception
	 */
	@Test
	public void testMicronodeList() throws Exception {
		addTestTagmapEntry(Page.TYPE_PAGE, "tags", "page.tags", AttributeType.micronode.getType(), true);

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getMicronodeFieldList("tags")).as("Field 'tags'").isNotNull();
		assertThat(sentNode.getFields().getMicronodeFieldList("tags").getItems()).as("Micronodes").usingElementComparator(MODEL_COMPARATOR)
				.containsOnly(reference("This is the content"), reference("Embedded content"));
	}

	/**
	 * Create Reference MicronodeResponse
	 * @param content tag content
	 * @return MicronodeResponse
	 */
	protected MicronodeResponse reference(String content) {
		MicronodeResponse ref = new MicronodeResponse().setMicroschema(new MicroschemaReferenceImpl().setName("test_construct"));
		ref.getFields().put("part", new StringFieldImpl().setString(content));
		return ref;
	}

	/**
	 * Test that rendering the preview will use the SID, which is set in the DynamicUrlFactory of the RenderType in the surrounding transaction.
	 * @throws NodeException
	 */
	@Test
	public void testSidInUrl() throws NodeException {
		// make the live editable construct not live editable, because that would spoil the response (so that it is not valid json any more)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(1);
		});

		Integer nodeId = Trx.supply(() -> node.getId());
		Integer pageId = Trx.supply(() -> page.getId());
		String sid = "thisisthesid";
		String expectedUrl = String.format("/alohapage?nodeid=%d&language=1&sid=%s&real=newview&realid=%d", nodeId, sid, pageId);
		String preview = null;
		try (FeatureClosure f = new FeatureClosure(Feature.MANAGELINKURL_ONLYFORPUBLISH, true)) {
			preview = Trx.supply(t -> {
				RenderType r = new RenderType();
				r.setRenderUrlFactory(new DynamicUrlFactory(sid));
				t.setRenderType(r);
				return RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA);
			});
		}
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("pageurl")).as("Page URL Field").isNotNull();
		assertThat(sentNode.getFields().getStringField("pageurl").getString()).as("Page URL Field").isEqualTo(expectedUrl);
	}

	/**
	 * Test adding renderMode query parameter
	 * @throws NodeException
	 */
	@Test
	public void testRenderModeParam() throws NodeException {
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/query");
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Posted query").isEqualTo("{renderMode=[preview]}");

		preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		assertThat(preview).as("Posted query").isEqualTo("{renderMode=[edit]}");
	}

	/**
	 * Test rendering the page in preview mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderPreview() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/render");
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		String expectedPreview = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render();
			}
		});
		assertThat(preview).as("Rendered page").isEqualTo(expectedPreview);
	}

	/**
	 * Test rendering the page in edit mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderEdit() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/render");
		});

		String edit = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		String expectedEdit = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render();
			}
		});
		assertThat(edit).as("Rendered page").isEqualTo(expectedEdit);
	}

	/**
	 * Test rendering a single tag in edit mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderTag() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/render");
		});

		Set<String> tagNames = Trx.supply(() -> page.getContent().getContentTags().keySet());
		for (String tag : tagNames) {
			PageRenderResponse response = Trx.supply(user,
					() -> new PageResourceImpl().renderTag(Integer.toString(page.getId()), tag, null, null, LinksType.backend));
			assertResponseCodeOk(response);

			String tagEdit = response.getContent();

			String expectedTagEdit = Trx.supply(user, t -> {
				try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
					rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
					return page.render("<node " + tag + ">", t.getRenderResult(), null, null, null, null);
				}
			});
			assertThat(tagEdit).as("Rendered tag '" + tag + "'").isEqualTo(expectedTagEdit);
		}
	}

	/**
	 * Test call to endpoint, which "freezes" (i.e. does not return within 1 minute)
	 * @throws NodeException
	 */
	@Test(timeout = 20_000L)
	public void testFreeze() throws NodeException {
		update(node, n -> {
			n.setMeshPreviewUrl(getRestAppContext().getBaseUri() + "preview/freeze");
		});

		Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
	}

	/**
	 * Test rendering translated folder data
	 * @throws NodeException
	 */
	@Test
	public void testRenderTranslatedFolder() throws NodeException {
		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("foldername")).as("Field 'foldername'").isNotNull();
		assertThat(sentNode.getFields().getStringField("foldername").getString()).as("Field 'foldername' value").isEqualTo("Ordner Name auf Deutsch");

		preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(translation, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("foldername")).as("Field 'foldername'").isNotNull();
		assertThat(sentNode.getFields().getStringField("foldername").getString()).as("Field 'foldername' value").isEqualTo("Node");
	}

	protected abstract RESTAppContext getRestAppContext();

	/**
	 * Preview Resource
	 */
	@Path("/preview")
	public final static class PreviewResource {
		@Context
		protected UriInfo uriInfo;

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

		/**
		 * This method will return the query string
		 * @param path path
		 * @param body posted body
		 * @return path
		 */
		@POST
		@Path("/query/{path: .*}")
		public String query(@PathParam("path") String path, String body) {
			return uriInfo.getQueryParameters().toString();
		}

		/**
		 * This method will render the object
		 * @param path path
		 * @param body posted body
		 * @return path
		 * @throws Exception
		 */
		@POST
		@Path("/render/{path: .*}")
		public String render(@PathParam("path") String path, String body) throws Exception {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<?, ?> sentNode = objectMapper.readValue(body, Map.class);

			VelocityContext context = new VelocityContext();
			context.put("node", sentNode);

			StringResourceRepository srr = StringResourceLoader.getRepository();
			srr.putStringResource("portaltemplate", "<div class=\"tag\">$node.fields.tag</div><div class=\"live\">$node.fields.live</div>");

			StringWriter outwriter = new StringWriter();

			Velocity.getTemplate("portaltemplate").merge(context, outwriter);

			return outwriter.toString();
		}

		@POST
		@Path("/freeze/{path: .*}")
		public void freeze() throws Exception {
			// sleep for 1 minute
			Thread.sleep(1 * 60 * 1000);
		}
	}
}
