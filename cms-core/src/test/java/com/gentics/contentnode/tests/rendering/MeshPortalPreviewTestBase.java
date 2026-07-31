package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.renderer.EchoRenderer;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.mesh.core.rest.common.RestModel;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.StringTemplateSource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 * Test cases for the "portal preview" of a Mesh portal
 */
public abstract class MeshPortalPreviewTestBase {
	/**
	 * Timestamp of the old version of the page
	 */
	public final static int OLD_TIMESTAMP = (int)(System.currentTimeMillis() / 1000L) - 3600;

	/**
	 * Test context
	 */
	public static DBTestContext context = new DBTestContext().config(map -> {
		// set call timeout to 5 seconds
		map.setProperty("mesh.client.callTimeout", "5");
	});

	/**
	 * REST App context
	 */
	protected static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(context).around(restContext);

	/**
	 * Comparator for Rest Models
	 */
	protected static final Comparator<RestModel> MODEL_COMPARATOR = Comparator.comparing(RestModel::toJson);

	/**
	 * Test node
	 */
	protected static Node node;

	/**
	 * Test CR
	 */
	protected static Integer meshCrId;

	/**
	 * ID of a non-mesh CR
	 */
	protected static Integer otherCrId;

	/**
	 * Construct ID
	 */
	protected static Integer constructId;

	/**
	 * Template
	 */
	protected static Template template;

	/**
	 * Page
	 */
	protected static Page page;

	protected static Page translation;

	/**
	 * Construct containing a single live editable part
	 */
	protected static Construct liveEditableConstruct;

	/**
	 * Set of tagmap entries (global IDs) that were created by the test (will be cleaned in teardown)
	 */
	protected Set<String> testTagmapEntries = new HashSet<>();

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

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

		page = Builder.create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Testpage");
			p.setLanguage(getLanguage("de"));
		}).at(1).build();

		page = Builder.update(page, p -> {
			ContentTag newTag = p.getContent().addContentTag(constructId);
			newTag.getValues().getByKeyname("part").setValueText("Embedded content");
			p.getContentTag("tag").getValues().getByKeyname("part").setValueText("This is the old content");
			p.getContentTag("live").getValues().getByKeyname("html")
					.setValueText("Live <b>editable</b> content, containing also embedded tags: [<node " + newTag.getName() + ">].");
		}).at(OLD_TIMESTAMP).build();

		page = Builder.update(page, p -> {
			p.getContentTag("tag").getValues().getByKeyname("part").setValueText("This is the content");
		}).build();

		// create the language variant
		translation = supply(() -> {
			Page translation = (Page) page.copy();
			translation.setLanguage(getLanguage("en"));
			translation.save();
			translation.unlock();
			return translation.reload();
		});

		update(supply(() -> node.getFolder()), upd -> {
			upd.setNameI18n(new I18nMap().put("de", "Ordner Name auf Deutsch"));
		}).build();

		SystemUser user = supply(t -> t.getObject(SystemUser.class, 1));
		assertThat(user).as("System user").isNotNull();

		update(user, upd -> {
			upd.setPassword(SystemUserFactory.hashPassword("system", upd.getId()));
		}).build();
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
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/echo");
			n.setPubDirSegment(true);
		}).build();

		// make the live editable construct live editable (this might be changed by tests)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(2);
		}).build();
	}

	/**
	 * Helper method to set the preview URL to the given node
	 * @param node node
	 * @param previewUrl preview URL to set
	 * @throws ReadOnlyException
	 */
	protected void setMeshPreviewUrl(Node node, String previewUrl) throws ReadOnlyException {
		// basic implementation sets the URL directly
		node.setMeshPreviewUrl(previewUrl);
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

			com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(null)
				.combine("node", sentNode)
				.build();

			Handlebars handlebars = new Handlebars();
			StringWriter outwriter = new StringWriter();
			com.github.jknack.handlebars.Template template = handlebars.compile(new StringTemplateSource(
					"portaltemplate",
					"<div class=\"tag\">{{{node.fields.tag}}}</div><div class=\"live\">{{{node.fields.live}}}</div>"));
			template.apply(context, outwriter);

			return outwriter.toString();
		}

		/**
		 * This method will render the object as plain text
		 * @param path path
		 * @param body posted body
		 * @return path
		 * @throws Exception
		 */
		@POST
		@Path("/renderplain/{path: .*}")
		public String renderPlain(@PathParam("path") String path, String body) throws Exception {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<?, ?> sentNode = objectMapper.readValue(body, Map.class);

			com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(null)
					.combine("node", sentNode)
					.build();

			Handlebars handlebars = new Handlebars();
			StringWriter outwriter = new StringWriter();
			com.github.jknack.handlebars.Template template = handlebars.compile(new StringTemplateSource(
					"portaltemplate",
					"tag: {{{node.fields.tag}}}, live: {{{node.fields.live}}}"));
			template.apply(context, outwriter);

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
