package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePreviewRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PagePreviewResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Sandbox Test for rendering a page
 */
@GCNFeature(unset = Feature.MULTICHANNELLING)
public class PageRenderSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the "Velocity" Construct
	 */
	public final static int VELOCITY_CONSTRUCT_ID = 2;

	/**
	 * ID of the "Text short" Construct
	 */
	public final static int TEXT_SHORT_CONSTRUCT_ID = 3;

	/**
	 * ID of the "Mixed Editable" Construct
	 */
	public final static int MIXED_CONSTRUCT_ID = 15;

	public final static String PAGE_ID = "45";

	/**
	 * ID of the user "editor"
	 */
	public final static int USER_EDITOR_ID = 26;

	/**
	 * ID of the root folder
	 */
	public final static int ROOT_FOLDER_ID = 27;

	/**
	 * ID of the overview construct
	 */
	public final static int OVERVIEW_CONSTRUCT_ID = 5;

	/**
	 * Name of the part of the overview construct
	 */
	public final static String OVERVIEW_PART_NAME = "ds";

	private Folder folder;

	private Folder subFolder;

	private Page rootDeOff_3;

	private Page rootDeOn_15;

	private Page rootDeOffEnOff_37;

	private Page rootEnOffDeOff_37;

	private Page rootDeOffEnOn_26;

	private Page rootEnOnDeOff_26;

	private Page rootDeOnEnOff_8;

	private Page rootEnOffDeOn_8;

	private Page rootDeOnEnOn_77;

	private Page rootEnOnDeOn_77;

	private Page subDeOff;

	private Page subDeOn;

	private Page subDeOffEnOff;

	private Page subEnOffDeOff;

	private Page subDeOffEnOn;

	private Page subEnOnDeOff;

	private Page subDeOnEnOff;

	private Page subEnOffDeOn;

	private Page subDeOnEnOn;

	private Page subEnOnDeOn;

	private Page overviewEn;

	private Page overviewDe;

	private Page rootOff_17;

	private Page rootOn_28;

	private Page subOff;

	private Page subOn;

	private SystemUser user;

	@Before
	public void setUp() throws Exception {
		testContext.getContext().getTransaction().commit();

		user = supply(t -> t.getObject(SystemUser.class, 1));
	}
	
	/**
	 * Test rendering a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPage() throws Exception {
		// use the page resource to render the page
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse renderResponse = pageResource.render(PAGE_ID, null, null, true, null, LinksType.frontend, false, false, false, 0);

			// check the results
			assertContent(StringUtils.readStream(getClass().getResourceAsStream("defaultContent.html")), renderResponse.getContent());
			// TODO check the correct tags
			// assertEditables(Arrays.asList("GENTICS_EDITABLE_100", "GENTICS_EDITABLE_101"), renderResponse.get);
			// assertBlocks(Arrays.asList("GENTICS_BLOCK_92"), renderResponse.getBlocks());
		}
	}

	/**
	 * Test rendering a page within non edit mode (read only)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageReadOnlyMode() throws Exception {
		// use the page resource to render the page
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse renderResponse = pageResource.render(PAGE_ID, null, null, false, null, LinksType.frontend, false, false, false, 0);

			// check the results
			assertContent(StringUtils.readStream(getClass().getResourceAsStream("defaultContentReadOnly.html")), renderResponse.getContent());
			// TODO check the correct tags
			// assertEditables(Arrays.asList("GENTICS_EDITABLE_100", "GENTICS_EDITABLE_101"), renderResponse.get);
			// assertBlocks(Arrays.asList("GENTICS_BLOCK_92"), renderResponse.getBlocks());
		}
	}

	/**
	 * Test rendering frontend links
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderFrontendLinks() throws Exception {
		try (Trx trx = new Trx(user)) {
			// use the page resource to render the page
			PageResource pageResource = getPageResource();
			PageRenderResponse renderResponse = pageResource.render(PAGE_ID, null, "<node page.url>", false, null, LinksType.frontend, false, false, false, 0);

			// check the results
			assertContent("/Content.Node/aloha3.en.html", renderResponse.getContent());
		}
	}

	/**
	 * Test rendering CR frontend links including a file link
	 * Both, linkway = "auto" and "portal" must render <plink> URLs for page links and file links.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderFrontendLinksCr() throws Exception {

		// Load the page
		com.gentics.contentnode.object.Page page = supply(t -> t.getObject(com.gentics.contentnode.object.Page.class, PAGE_ID));

		// Enable CR pub, disable filesystem pub
		operate(t -> {
			Node node = t.getObject(com.gentics.contentnode.object.Node.class, page.getFolder().getNode().getId(), true);
			node.setPublishFilesystem(false);
			node.setPublishContentmap(true);
			node.save();
		});

		File file = execute(p -> createFile(p.getFolder(), "testfile", "testcontent".getBytes()), page);

		// Create a file URL construct
		int fileConstructId = execute(p -> createConstruct(p.getNode(), FileURLPartType.class, "filetag", "fileurl"), page);
		TagCreateResponse response = Trx.supply(() -> {
			ContentTagCreateRequest tagCreateRequest = new ContentTagCreateRequest();
			TagCreateResponse resp = getPageResource().createTag(PAGE_ID, fileConstructId, null, tagCreateRequest);
			this.assertResponseOK(resp);
			return resp;
		});

		// Create a file URL tag in the page
		Trx.operate(t -> {
			com.gentics.contentnode.object.Page update = t.getObject(com.gentics.contentnode.object.Page.class, PAGE_ID, true);
			com.gentics.contentnode.object.Tag tag = update.getContent().getTag(response.getTag().getName());
			Value value = tag.getValues().getByKeyname("fileurl");
			value = t.getObject(value, true);
			value.setValueRef(file.getId());
			value.save();
			update.save();
		});

		String template = "<node page.url> | <node page.tags." + response.getTag().getName() + ".fileurl.url>";
		String expectedcontent = "<plink id=\"10007." + page.getId() + "\" /> | <plink id=\"10008." + file.getId() + "\" />";

		// Render with linkway = auto
		try (Trx trx = new Trx(user)) {
			NodePreferences nodePreferences = trx.getTransaction().getNodeConfig().getDefaultPreferences();
			nodePreferences.setProperty("contentnode.linkway",      "auto");
			nodePreferences.setProperty("contentnode.linkway_file", "auto");
			renderFrontendPageAndAssert(getPageResource(), PAGE_ID, template, expectedcontent);
			trx.success();
		}

		// Render with linkway = portal
		try (Trx trx = new Trx(user)) {
			NodePreferences nodePreferences = trx.getTransaction().getNodeConfig().getDefaultPreferences();
			nodePreferences.setProperty("contentnode.linkway",      "portal");
			nodePreferences.setProperty("contentnode.linkway_file", "portal");
			renderFrontendPageAndAssert(getPageResource(), PAGE_ID, template, expectedcontent);
			trx.success();
		}
	}

	protected void renderFrontendPageAndAssert(PageResourceImpl pageResource, String pageId,
			String template, String expectedContent) throws NodeException {
		PageRenderResponse renderResponse = pageResource.render(pageId, null,
				template, false, null, LinksType.frontend, false, false, false, 0);
		this.assertResponseOK(renderResponse);

		// check the results
		assertContent(expectedContent, renderResponse.getContent());
	}

	/**
	 * Transforms the object id into a local id (Integer) or global id (GlobalId)
	 * 
	 * @param id
	 * @return local id or global id
	 * @throws EntityNotFoundException
	 */
	public static Object getTransformObjectId(String id) throws EntityNotFoundException {
		// Create a globalid when the id string could be identified as a globalid conform string
		Object objectId = null;

		if (GlobalId.isGlobalId(id)) {
			objectId = new GlobalId(id);
		} else {
			try {
				objectId = Integer.parseInt(id);
			} catch (NumberFormatException e) {
				I18nString message = new CNI18nString("object.id.wrong");

				throw new EntityNotFoundException(message.toString());
			}
		}

		if (objectId == null) {
			I18nString message = new CNI18nString("object.id.wrong");

			throw new EntityNotFoundException(message.toString());
		} else {
			return objectId;
		}
	}

	/**
	 * Test rendering a live URL
	 * 
	 * @throws Exception
	 */
	@Test
	@GCNFeature(set = Feature.LIVE_URLS)
	public void testRenderLiveUrl() throws Exception {
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			Page page = pageResource.load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();

			assertContent("http://example.com/Content.Node/aloha3.en.html", page.getLiveUrl());
		}
	}

	/**
	 * Test rendering a live URL with "per node" feature
	 * @throws NodeException
	 */
	@Test
	@GCNFeature(set = Feature.LIVE_URLS_PER_NODE)
	public void testRenderLiveUrlPerNode() throws NodeException {
		Integer nodeId = Trx.supply(user, () -> {
			Page page = getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();
			assertThat(page.getLiveUrl()).isEmpty();
			return page.getMasterNodeId();
		});

		Trx.operate(user, () -> {
			NodeResourceImpl nodeResource = getNodeResource();

			nodeResource.activateFeature(Integer.toString(nodeId), NodeFeature.live_urls_per_node);
		});

		Trx.operate(user, () -> {
			Page page = getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();
			assertContent("http://example.com/Content.Node/aloha3.en.html", page.getLiveUrl());
		});
	}

	/**
	 * Test rendering backend links
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderBackendLinks() throws Exception {
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse renderResponse = pageResource.render(PAGE_ID, null, "<node page.url>", false, null, LinksType.backend, false, false, false, 0);

			// check the results
			assertContent("/alohapage?nodeid=5&language=2&sid=" + TransactionManager.getCurrentTransaction().getSession().getSessionId()
					+ "&real=newview&realid=45", renderResponse.getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a specific template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderTemplate() throws Exception {
		try (Trx trx = new Trx(user)) {
			// use the page resource to render the page
			PageResource pageResource = getPageResource();
			PageRenderResponse renderResponse = pageResource.render(PAGE_ID, null, "Page name:<node page.name>\n<node htmllive1>", true, null, LinksType.backend,
					false, false, false, 0);
			// check the results
			assertContent(StringUtils.readStream(getClass().getResourceAsStream("specialTemplateContent.html")), renderResponse.getContent());
		}
	}

	/**
	 * Test rendering a velocity tag in a page, that fetches a (foreign) folder and renders an object property that resolves the (foreign) folder's name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderVelocity1() throws Exception {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));

		Folder f = Trx.supply(() -> {
			// load the page
			Page page = getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage();

			// create a folder next to the page
			Folder folder = createFolder("Velocity Render Test Folder", page.getFolderId(), "<node folder.name>");

			// create a new velocity Tag
			Map<String, String> parts = new HashMap<String, String>();

			parts.put(
					"template",
					"#set($folder = $cms.imps.loader.getFolder("
							+ folder.getId()
							+ "))##\nfolder.name: $folder.name\nfolder.object.name_de: $folder.object.name_de\nfolder.object.name_de.parts.text: $folder.object.name_de.parts.text");
			createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource());

			return folder;
		});

		try (Trx trx = new Trx(user)) {
			// render the page
			PageRenderResponse renderResponse = getPageResource().render(PAGE_ID, null, "<node vtl>", false, null, LinksType.backend, false, false, false, 0);

			// check the results
			assertContent("folder.name: " + f.getName() + "\n" + "folder.object.name_de: " + f.getName() + "\n"
					+ "folder.object.name_de.parts.text: " + f.getName(), renderResponse.getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a velocity tag that fetches another page and renders a tag that resolves the other page's folder name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderVelocity2() throws Exception {
		// load the page
		Page page = supply(() -> getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage());

		// create a folder next to the page
		Folder folder = createFolder("Velocity Render Test Folder", page.getFolderId(), "<node folder.name>");

		// store the page as copy in
		Page otherPage = createPage(page.getTemplateId(), folder.getId(), null, null, null);

		// create a tag in the other page
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("text", "<node folder.name>");
		operate(() -> createTag(otherPage, TEXT_SHORT_CONSTRUCT_ID, "shorty", parts, getPageResource()));

		// create a new velocity Tag
		parts.clear();
		parts.put("template", "#set($page = $cms.imps.loader.getPage(" + otherPage.getId()
				+ "))##\npage.name: $page.name\npage.tags.shorty: $page.tags.shorty\npage.tags.shorty.parts.text: $page.tags.shorty.parts.text");
		operate(() -> createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		// render the page
		try (Trx trx = new Trx(user)) {
			PageRenderResponse renderResponse = getPageResource().render(PAGE_ID, null, "<node vtl>", false, null, LinksType.backend, false, false, false, 0);

			// check the results
			assertContent("page.name: " + otherPage.getName() + "\n" + "page.tags.shorty: " + folder.getName() + "\n" + "page.tags.shorty.parts.text: "
					+ folder.getName(), renderResponse.getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a velocity tag which renders parts of another tag which resolve parts of that other tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderVelocity3() throws Exception {
		// load the page
		Page page = supply(() -> getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage());

		// create the mixed tag
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("html", "<node htmllive>");
		parts.put("htmllive", "This is the output!");
		operate(() -> createTag(page, MIXED_CONSTRUCT_ID, "mixed", parts, getPageResource()));

		// create the velocity tag
		parts.clear();
		parts.put("template", "Node Notation (Tag): <node mixed>\n" + "Node Notation (Part html): <node mixed.parts.html>\n"
				+ "Node Notation (Part htmllive): <node mixed.parts.htmllive>\n" + "Velocity (Tag): $cms.page.tags.mixed\n"
				+ "Velocity (Part html): $cms.page.tags.mixed.parts.html\n" + "Velocity (Part htmllive): $cms.page.tags.mixed.parts.htmllive");
		operate(() -> createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		// render the mixed tag
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			assertContent("<div class=\"container\">\n" + "Live editable: This is the output!<br/>\n" + "Not live editable: This is the output!\n" + "</div>",
					pageResource.render(PAGE_ID, null, "<node mixed>", false, null, LinksType.backend, false, false, false, 0).getContent());

			// render the vtl tag
			assertContent(
					"Node Notation (Tag): <div class=\"container\">\n" + "Live editable: This is the output!<br/>\n" + "Not live editable: This is the output!\n"
							+ "</div>\n" + "Node Notation (Part html): This is the output!\n" + "Node Notation (Part htmllive): This is the output!\n"
							+ "Velocity (Tag): <div class=\"container\">\n" + "Live editable: This is the output!<br/>\n" + "Not live editable: This is the output!\n"
							+ "</div>\n" + "Velocity (Part html): This is the output!\n" + "Velocity (Part htmllive): This is the output!",
							pageResource.render(PAGE_ID, null, "<node vtl>", false, null, LinksType.backend, false, false, false, 0).getContent());
			trx.success();
		}
	}

	/**
	 * Test fix for RT#55706. Velocity tag parts should only inspect their own tag when looking for their "macros" and "template" tag parts and refrain from inheriting
	 * any of them from other tags that include them in the rendering process
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testVelocityConfigNonInheritance() throws NodeException {
		com.gentics.contentnode.object.Folder rootFolder = supply(t -> t.getObject(com.gentics.contentnode.object.Folder.class, ROOT_FOLDER_ID));
		Node node = supply(() -> rootFolder.getNode());

		// Create includer tag
		Construct includer = supply(t -> {
			Construct constr = t.createObject(Construct.class);

			constr.setKeyword("includer");
			constr.setName("includer (de)", 1);
			constr.setName("includer (en)", 2);
			constr.getNodes().add(node);
			{
				Part vtl = t.createObject(Part.class);

				vtl.setKeyname("vtl");
				vtl.setHidden(false);
				vtl.setEditable(0);
				vtl.setName("Velocity", 1);
				vtl.setName("Velocity", 2);
				vtl.setPartTypeId(33);

				Part macros = t.createObject(Part.class);

				macros.setKeyname("macros");
				macros.setHidden(true);
				macros.setEditable(0);
				macros.setName("Macros", 1);
				macros.setName("Macros", 2);
				macros.setPartTypeId(21);

				Part template = t.createObject(Part.class);

				template.setKeyname("template");
				template.setHidden(true);
				template.setEditable(0);
				template.setName("Template", 1);
				template.setName("Template", 2);
				template.setPartTypeId(21);

				Value mValue = t.createObject(Value.class);

				mValue.setPart(macros);
				mValue.setValueText("Te");
				macros.setDefaultValue(mValue);

				Value tValue = t.createObject(Value.class);

				tValue.setPart(template);
				tValue.setValueText("${cms.page.tags.includee}${cms.page.tags.includee2}");
				template.setDefaultValue(tValue);

				constr.getParts().add(vtl);
				constr.getParts().add(macros);
				constr.getParts().add(template);
			}
			constr.save();
			return constr;
		});

		// create includee tag without template
		Construct includee = supply(t -> {
			Construct constr = t.createObject(Construct.class);

			constr.setKeyword("includee");
			constr.setName("includee (de)", 1);
			constr.setName("includee (en)", 2);
			constr.getNodes().add(node);
			{
				Part vtl = t.createObject(Part.class);

				vtl.setKeyname("vtl");
				vtl.setHidden(false);
				vtl.setEditable(0);
				vtl.setName("Velocity", 1);
				vtl.setName("Velocity", 2);
				vtl.setPartTypeId(33);

				Part macros = t.createObject(Part.class);

				macros.setKeyname("macros");
				macros.setHidden(true);
				macros.setEditable(0);
				macros.setName("Macros", 1);
				macros.setName("Macros", 2);
				macros.setPartTypeId(21);

				Value mValue = t.createObject(Value.class);

				mValue.setPart(macros);
				mValue.setValueText("e");
				macros.setDefaultValue(mValue);

				constr.getParts().add(vtl);
				constr.getParts().add(macros);
			}
			constr.save();
			return constr;
		});

		// create includee tag without macro
		Construct includee2 = supply(t -> {
			Construct constr = t.createObject(Construct.class);

			constr.setKeyword("includee2");
			constr.setName("includee2 (de)", 1);
			constr.setName("includee2 (en)", 2);
			constr.getNodes().add(node);
			{
				Part vtl = t.createObject(Part.class);

				vtl.setKeyname("vtl");
				vtl.setHidden(false);
				vtl.setEditable(0);
				vtl.setName("Velocity", 1);
				vtl.setName("Velocity", 2);
				vtl.setPartTypeId(33);

				Part template = t.createObject(Part.class);

				template.setKeyname("template");
				template.setHidden(true);
				template.setEditable(0);
				template.setName("Template", 1);
				template.setName("Template", 2);
				template.setPartTypeId(21);

				Value tValue = t.createObject(Value.class);

				tValue.setPart(template);
				tValue.setValueText("${cms.page.tags.includee}st");
				template.setDefaultValue(tValue);

				constr.getParts().add(vtl);
				constr.getParts().add(template);
			}
			constr.save();
			return constr;
		});

		// create template
		Template pageTemplate = supply(t -> {
			Template tpl = t.createObject(Template.class);

			tpl.setFolderId(rootFolder.getId());
			tpl.setMlId(1);
			tpl.setName("Test template");
			tpl.setSource("<node includer>");
			tpl.save();
			return tpl;
		});

		// create page
		com.gentics.contentnode.object.Page page = supply(t -> {
			com.gentics.contentnode.object.Page p = t.createObject(com.gentics.contentnode.object.Page.class);

			p.setFolderId(rootFolder.getId());
			p.setTemplateId(pageTemplate.getId());
			ContentTag tag = p.getContent().addContentTag(ObjectTransformer.getInt(includer.getId(), 0));

			tag.setEnabled(true);
			tag.setName("includer");
			ContentTag tag2 = p.getContent().addContentTag(ObjectTransformer.getInt(includee.getId(), 0));

			tag2.setEnabled(true);
			tag2.setName("includee");
			ContentTag tag3 = p.getContent().addContentTag(ObjectTransformer.getInt(includee2.getId(), 0));

			tag3.setEnabled(true);
			tag3.setName("includee2");
			p.save();
			return p;
		});

		// render page
		try (Trx trx = new Trx(user)) {
			PageRenderResponse rendered = getPageResource().render(page.getId().toString(), ObjectTransformer.getInt(node.getId(), 0), null, false, "", LinksType.backend,
					false, false, false, 0);

			assertEquals("Test", rendered.getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a velocity tag in a page in edit mode, which is set to enabled=3 (enable after first edit).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderVelocityTagEnabledOnEditInEditMode() throws Exception {
		// load the page
		com.gentics.contentnode.object.Page page = supply(t -> t.getObject(com.gentics.contentnode.object.Page.class, ObjectTransformer.getInteger(PAGE_ID, 0),
				true));

		// Convert to REST model
		Collection<Reference> fillRefs = new Vector<Reference>();
		fillRefs.add(Reference.CONTENT_TAGS);
		Page restPage = supply(() -> ModelBuilder.getPage(page, fillRefs));

		// create a new velocity Tag
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("template", "");
		Tag tag = supply(() -> createTag(restPage, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		// Load the tag as contentnode.object
		ContentTag contentTag = supply(t -> {
			Integer tagid = ObjectTransformer.getInteger(tag.getId(), 0);
			com.gentics.contentnode.object.ContentTag cTag = t.getObject(com.gentics.contentnode.object.ContentTag.class, tagid, true);
			cTag.setEnabled(3);
			cTag.save();
			return cTag;
		});

		try (Trx trx = new Trx(user)) {
			Transaction t = trx.getTransaction();
			RenderType renderType = RenderType.getDefaultRenderType(t.getNodeConfig().getDefaultPreferences(), RenderType.EM_ALOHA, t.getSessionId(), 0);
			renderType.setParameter(AlohaRenderer.LINKS_TYPE, "backend");

			renderType.setEditMode(RenderType.EM_ALOHA);
			renderType.setFrontEnd(false);

			RenderResult renderResult = new RenderResult();
			// set the RR for the current transaction, so that parameters are not reset
			t.setRenderResult(renderResult);

			renderType.addRenderer("aloha");
			t.setRenderType(renderType);

			// push the page onto the rendertype stack
			renderType.push(page);

			try {
				contentTag.render(renderResult);
			} finally {
				renderType.pop();
			}

			// Check the return code
			assertEquals("OK", renderResult.getReturnCode());
			trx.success();
		}
	}

	/**
	 * Test rendering a page in edit mode.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderRenderMode() throws Exception {
		// load the page
		Page page = supply(() -> getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage());

		// create the velocity tag
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("template", "cms.rendermode.publish: $cms.rendermode.publish\n" + "cms.rendermode.live: $cms.rendermode.live\n"
				+ "cms.rendermode.edit: $cms.rendermode.edit\n" + "cms.rendermode.preview: $cms.rendermode.preview\n"
				+ "cms.rendermode.real: $cms.rendermode.real\n" + "cms.rendermode.backend: $cms.rendermode.backend\n"
				+ "cms.rendermode.frontend: $cms.rendermode.frontend\n");
		Tag tag = supply(() -> createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		// render the vtl tag (preview in backend mode)
		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			assertContent(
					"cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: false\n" + "cms.rendermode.preview: true\n"
							+ "cms.rendermode.real: false\n" + "cms.rendermode.backend: true\n" + "cms.rendermode.frontend: false\n",
							pageResource.render(PAGE_ID, null, "<node vtl>", false, null, LinksType.backend, false, false, false, 0).getContent());

			// render the vtl tag (preview in frontend mode)
			assertContent(
					"cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: false\n" + "cms.rendermode.preview: true\n"
							+ "cms.rendermode.real: false\n" + "cms.rendermode.backend: false\n" + "cms.rendermode.frontend: true\n",
							pageResource.render(PAGE_ID, null, "<node vtl>", false, null, LinksType.frontend, false, false, false, 0).getContent());

			// render the vtl tag in edit mode (backend)
			assertContent(
					"<div data-gcn-pageid=\"" + PAGE_ID + "\" data-gcn-tagid=\"" + tag.getId() + "\" data-gcn-tagname=\"vtl\" "
							+ "data-gcn-i18n-constructname=\"Velocity Code\" class=\"aloha-block\" id=\"GENTICS_BLOCK_"
							+ tag.getId() + "\">cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: true\n"
							+ "cms.rendermode.preview: false\n" + "cms.rendermode.real: false\n" + "cms.rendermode.backend: true\n" + "cms.rendermode.frontend: false\n</div>",
							pageResource.render(PAGE_ID, null, "<node vtl>", true, null, LinksType.backend, false, false, false, 0).getContent());

			// render the vtl tag in edit mode (backend)
			assertContent(
					"<div data-gcn-pageid=\"" + PAGE_ID + "\" data-gcn-tagid=\"" + tag.getId() + "\" data-gcn-tagname=\"vtl\" "
							+ "data-gcn-i18n-constructname=\"Velocity Code\" class=\"aloha-block\" id=\"GENTICS_BLOCK_"
							+ tag.getId() + "\">cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: true\n"
							+ "cms.rendermode.preview: false\n" + "cms.rendermode.real: false\n" + "cms.rendermode.backend: false\n" + "cms.rendermode.frontend: true\n</div>",
							pageResource.render(PAGE_ID, null, "<node vtl>", true, null, LinksType.frontend, false, false, false, 0).getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a page, that is locked by another user in edit mode.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderRenderModeLockedPage() throws Exception {
		// Load the page for update by the system user
		Page page = supply(() -> getPageResource().load(PAGE_ID, true, false, false, false, false, false, false, false, false, false, null, null).getPage());

		// create the velocity tag
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("template", "cms.rendermode.publish: $cms.rendermode.publish\n" + "cms.rendermode.live: $cms.rendermode.live\n"
				+ "cms.rendermode.edit: $cms.rendermode.edit\n" + "cms.rendermode.preview: $cms.rendermode.preview\n"
				+ "cms.rendermode.real: $cms.rendermode.real\n");
		operate(() -> createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		// login with another user
		SystemUser editor = supply(t -> t.getObject(SystemUser.class, USER_EDITOR_ID));
		try (Trx trx = new Trx(editor)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse response = pageResource.render(PAGE_ID, null, "<node vtl>", false, null, LinksType.backend, false, false, false, 0);
			assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

			// render the vtl tag
			assertContent("cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: false\n"
					+ "cms.rendermode.preview: true\n" + "cms.rendermode.real: false\n", response.getContent());

			response = pageResource.render(PAGE_ID, null, "<node vtl>", true, null, LinksType.backend, false, false, false, 0);
			assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

			// render the vtl tag in edit mode
			assertContent("cms.rendermode.publish: false\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: false\n"
					+ "cms.rendermode.preview: true\n" + "cms.rendermode.real: false\n", response.getContent());
			trx.success();
		}
	}

	/**
	 * Test rendering a page, in publish mode.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPublishRenderModePage() throws Exception {
		Node node = supply(() -> createNode());
		com.gentics.contentnode.object.Page nodePage = supply(() -> createTemplateAndPage(node.getFolder(), "testRenderPublishRenderModePage"));

		// Load the page for update by the system user
		Page page = supply(() -> getPageResource().load(nodePage.getId().toString(), true, false, false,
				false, false, false, false, false, false, false, null, null).getPage());

		// create the velocity tag
		Map<String, String> parts = new HashMap<String, String>();

		parts.put("template", "cms.rendermode.publish: $cms.rendermode.publish\n" + "cms.rendermode.live: $cms.rendermode.live\n"
				+ "cms.rendermode.edit: $cms.rendermode.edit\n" + "cms.rendermode.preview: $cms.rendermode.preview\n"
				+ "cms.rendermode.real: $cms.rendermode.real\n");
		operate(() -> createTag(page, VELOCITY_CONSTRUCT_ID, "vtl", parts, getPageResource()));

		try (Trx trx = new Trx(user)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse response = pageResource.render(page.getId().toString(), null, "<node vtl>", false, null, LinksType.frontend, false, false, true, 0);
			assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());

			// render the vtl tag
			assertContent("cms.rendermode.publish: true\n" + "cms.rendermode.live: false\n" + "cms.rendermode.edit: false\n"
					+ "cms.rendermode.preview: false\n" + "cms.rendermode.real: false\n", response.getContent());
			trx.success();
		}
	}

	/**
	 * Test overviews with pages selected manually, sorted manually
	 * 
	 * @throws Exception
	 */
	@Test
	public void testManualSelectSelfSortPageOverview() throws Exception {
		preparePageOverviewTestData();
		List<Integer> selected = Arrays.asList(rootDeOnEnOff_8.getId(), rootOn_28.getId(), rootDeOn_15.getId(), rootDeOffEnOn_26.getId(),
				rootOff_17.getId(), rootDeOnEnOn_77.getId(), rootDeOffEnOff_37.getId(), rootDeOff_3.getId());

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootDeOnEnOn_77));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootDeOnEnOn_77));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootOn_28, rootDeOn_15, rootDeOnEnOn_77));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootOn_28, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootOn_28, rootDeOn_15, rootEnOnDeOff_26, rootDeOnEnOn_77));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.SELF, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootOn_28, rootDeOn_15, rootEnOnDeOff_26, rootEnOnDeOn_77));
	}

	/**
	 * Test overviews with pages selected manually, sorted by name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testManualSelectSortByNamePageOverview() throws Exception {
		preparePageOverviewTestData();
		List<Integer> selected = Arrays.asList(rootDeOnEnOff_8.getId(), rootOn_28.getId(), rootDeOn_15.getId(), rootDeOffEnOn_26.getId(),
				rootOff_17.getId(), rootDeOnEnOn_77.getId(), rootDeOffEnOff_37.getId(), rootDeOff_3.getId());

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootOn_28));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, rootOn_28));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));
	}

	/**
	 * Test overviews with pages selected manually, sorted by priority
	 * 
	 * @throws Exception
	 */
	@Test
	public void testManualSelectSortByPriorityPageOverview() throws Exception {
		preparePageOverviewTestData();
		List<Integer> selected = Arrays.asList(rootDeOnEnOff_8.getId(), rootOn_28.getId(), rootDeOn_15.getId(), rootDeOffEnOn_26.getId(),
				rootOff_17.getId(), rootDeOnEnOn_77.getId(), rootDeOffEnOff_37.getId(), rootDeOff_3.getId());

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootDeOnEnOn_77));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootDeOnEnOn_77));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootOn_28, rootDeOnEnOn_77));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootOn_28, rootEnOnDeOn_77));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootOn_28, rootDeOnEnOn_77));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.MANUAL, false, OrderBy.PRIORITY, selected, "<node page.name>|",
				Arrays.asList(rootDeOnEnOff_8, rootDeOn_15, rootEnOnDeOff_26, rootOn_28, rootEnOnDeOn_77));
	}

	/**
	 * Test overviews with pages selected by folder, sorted by name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSelectFoldersSortByNamePageOverview() throws Exception {
		preparePageOverviewTestData();
		List<Integer> selected = Arrays.asList(folder.getId());

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootOn_28));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, rootOn_28));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.FOLDER, false, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));
	}

	/**
	 * Test overviews with pages selected by folder (recursively), sorted by name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSelectFoldersRecursiveSortByNamePageOverview() throws Exception {
		preparePageOverviewTestData();
		List<Integer> selected = Arrays.asList(folder.getId());

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, subDeOn, subDeOnEnOff, subDeOnEnOn));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, subEnOnDeOff, subEnOnDeOn));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, subDeOn, subDeOnEnOff, subDeOnEnOn, subEnOnDeOff));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, subDeOn, subDeOnEnOff, subEnOnDeOff, subEnOnDeOn));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootOn_28, subDeOn, subDeOnEnOff, subDeOnEnOn, subOn));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28, subEnOnDeOff, subEnOnDeOn, subOn));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, rootOn_28, subDeOn, subDeOnEnOff,
						subDeOnEnOn, subEnOnDeOff, subOn));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.FOLDER, true, OrderBy.ALPHABETICALLY, selected,
				"<node page.name>|", Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28, subDeOn, subDeOnEnOff,
						subEnOnDeOff, subEnOnDeOn, subOn));
	}

	/**
	 * Test overviews with pages from current folder, sorted by name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCurrentFolderSortByNamePageOverview() throws Exception {
		preparePageOverviewTestData();

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootOn_28));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, rootOn_28));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.AUTO, false, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28));
	}

	/**
	 * Test if an overview doesn't get initialized with default values out of nowhere
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmptyOverview() throws Exception {

		String tagname = "overview";

		// create a folder
		Folder folder = createFolder("Folder", ROOT_FOLDER_ID, "Ordner");
		int folderId = folder.getId();

		// create a template
		Template template = Trx.supply(t -> {
			Template newTemplate = t.createObject(Template.class);

			newTemplate.setName("Template with Overview");
			newTemplate.setMlId(1);
			newTemplate.setSource("<node " + tagname + ">");

			// create an empty overview add it to the template and save the template
			TemplateTag overviewTag = t.createObject(TemplateTag.class);
			overviewTag.setConstructId(OVERVIEW_CONSTRUCT_ID);
			overviewTag.setEnabled(true);
			overviewTag.setPublic(true);
			overviewTag.setName(tagname);
			newTemplate.getTags().put(overviewTag.getName(), overviewTag);
			newTemplate.getFolders().add(t.getObject(com.gentics.contentnode.object.Folder.class, folderId));
			newTemplate.save();

			return newTemplate;
		});

		// create a page with the template containing an empty overview
		Page overviewEn = createPage(ObjectTransformer.getInteger(template.getId(), 0), folderId, "Overview (En)", "en", null);

		// get the (hopefully) empty overview from the page tags
		Overview emptyOverview = overviewEn.getTags().get(tagname).getProperties().get(OVERVIEW_PART_NAME).getOverview();

		// check if all relevant fields are not accidently initialized
		assertEquals("Check if selectType is undefined", Overview.SelectType.UNDEFINED, emptyOverview.getSelectType());
		assertEquals("Check if orderBy is undefined", Overview.OrderBy.UNDEFINED, emptyOverview.getOrderBy());
		assertEquals("Check if orderDirection is asc", Overview.OrderDirection.ASC, emptyOverview.getOrderDirection());
		assertEquals("Check if listType is undefined", Overview.ListType.UNDEFINED, emptyOverview.getListType());
		List<Integer> selectedItemIds = emptyOverview.getSelectedItemIds();
		if (selectedItemIds != null) {
			Assert.assertTrue("Check if no items are selected", selectedItemIds.isEmpty());
		}
	}

	/**
	 * Test overviews with pages from current folder (recursively), sorted by name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCurrentFolderRecursiveSortByNamePageOverview() throws Exception {
		preparePageOverviewTestData();

		// no dsfallback, no empty cs
		doOverviewSortTest(false, false, false, overviewDe, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, subDeOn, subDeOnEnOff, subDeOnEnOn));
		doOverviewSortTest(false, false, false, overviewEn, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, subEnOnDeOff, subEnOnDeOn));

		// dsfallback, no empty cs
		doOverviewSortTest(true, false, false, overviewDe, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, subDeOn, subDeOnEnOff, subDeOnEnOn, subEnOnDeOff));
		doOverviewSortTest(true, false, false, overviewEn, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, subDeOn, subDeOnEnOff, subEnOnDeOff, subEnOnDeOn));

		// no dsfallback, empty cs
		doOverviewSortTest(false, true, false, overviewDe, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootOn_28, subDeOn, subDeOnEnOff, subDeOnEnOn, subOn));
		doOverviewSortTest(false, true, false, overviewEn, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28, subEnOnDeOff, subEnOnDeOn, subOn));

		// dsfallback, empty cs
		doOverviewSortTest(true, true, false, overviewDe, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOff_26, rootOn_28, subDeOn, subDeOnEnOff, subDeOnEnOn,
						subEnOnDeOff, subOn));
		doOverviewSortTest(true, true, false, overviewEn, ListType.PAGE, SelectType.AUTO, true, OrderBy.ALPHABETICALLY, null, "<node page.name>|",
				Arrays.asList(rootDeOn_15, rootDeOnEnOff_8, rootEnOnDeOff_26, rootEnOnDeOn_77, rootOn_28, subDeOn, subDeOnEnOff, subEnOnDeOff,
						subEnOnDeOn, subOn));
	}

	/**
	 * Tests that the various rendermode values are correct for preview.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRenderModes() throws Exception {
		final int templateId = ObjectTransformer.getInteger(createTemplate().getId(), 0);
		Page page = createPage(templateId, ROOT_FOLDER_ID, "TestRenderModes", null, 17);
		assertFalse(null == page);

		try (Trx trx = new Trx(user)) {
			final Map<String, String> parts = new HashMap<String, String>();
			parts.put("template",
					"cms.rendermode.backend:  $cms.rendermode.backend\n"
							+ "cms.rendermode.frontend: $cms.rendermode.frontend\n"
					);
			createTag(page, VELOCITY_CONSTRUCT_ID, "test", parts, getPageResource());
			assertContent(
					"cms.rendermode.backend:  false\n"
							+ "cms.rendermode.frontend: true\n",
							previewPage(page, getPageResource())
					);
			trx.success();
		}
	}

	/**
	 * Prepare the page overview test data
	 * 
	 * @throws Exception
	 */
	protected void preparePageOverviewTestData() throws Exception {
		String tagname = "overview";

		// create a folder
		folder = createFolder("Folder", ROOT_FOLDER_ID, "Ordner");
		int folderId = folder.getId();

		// create a template with an overview
		Template template = Trx.supply(t -> {
			Template newTemplate = t.createObject(Template.class);

			newTemplate.setName("Template with Overview");
			newTemplate.setMlId(1);
			newTemplate.setSource("<node " + tagname + ">");
			TemplateTag overviewTag = t.createObject(TemplateTag.class);

			overviewTag.setConstructId(OVERVIEW_CONSTRUCT_ID);
			overviewTag.setEnabled(true);
			overviewTag.setPublic(true);
			overviewTag.setName(tagname);
			newTemplate.getTags().put(overviewTag.getName(), overviewTag);
			newTemplate.getFolders().add(t.getObject(com.gentics.contentnode.object.Folder.class, folderId));
			newTemplate.save();

			return newTemplate;
		});

		int templateId = ObjectTransformer.getInteger(template.getId(), 0);

		// create a subfolder
		subFolder = createFolder("Subfolder", folderId, "Unterordner");
		int subFolderId = subFolder.getId();

		rootOff_17 = createPage(templateId, folderId, "RootOff", null, 17);
		rootOn_28 = createPage(templateId, folderId, "RootOn", null, 28);
		rootDeOff_3 = createPage(templateId, folderId, "RootDeOff", "de", 3);
		rootDeOn_15 = createPage(templateId, folderId, "RootDeOn", "de", 15);
		rootDeOffEnOff_37 = createPage(templateId, folderId, "RootDeOffEnOff", "de", 37);
		rootEnOffDeOff_37 = translatePage(rootDeOffEnOff_37, "RootEnOffDeOff", "en");
		rootDeOffEnOn_26 = createPage(templateId, folderId, "RootDeOffEnOn", "de", 26);
		rootEnOnDeOff_26 = translatePage(rootDeOffEnOn_26, "RootEnOnDeOff", "en");
		rootDeOnEnOff_8 = createPage(templateId, folderId, "RootDeOnEnOff", "de", 8);
		rootEnOffDeOn_8 = translatePage(rootDeOnEnOff_8, "RootEnOffDeOn", "en");
		rootDeOnEnOn_77 = createPage(templateId, folderId, "RootDeOnEnOn", "de", 77);
		rootEnOnDeOn_77 = translatePage(rootDeOnEnOn_77, "RootEnOnDeOn", "en");

		subOff = createPage(templateId, subFolderId, "SubOff", null, null);
		subOn = createPage(templateId, subFolderId, "SubOn", null, null);
		subDeOff = createPage(templateId, subFolderId, "SubDeOff", "de", null);
		subDeOn = createPage(templateId, subFolderId, "SubDeOn", "de", null);
		subDeOffEnOff = createPage(templateId, subFolderId, "SubDeOffEnOff", "de", null);
		subEnOffDeOff = translatePage(subDeOffEnOff, "SubEnOffDeOff", "en");
		subDeOffEnOn = createPage(templateId, subFolderId, "SubDeOffEnOn", "de", null);
		subEnOnDeOff = translatePage(subDeOffEnOn, "SubEnOnDeOff", "en");
		subDeOnEnOff = createPage(templateId, subFolderId, "SubDeOnEnOff", "de", null);
		subEnOffDeOn = translatePage(subDeOnEnOff, "SubEnOffDeOn", "en");
		subDeOnEnOn = createPage(templateId, subFolderId, "SubDeOnEnOn", "de", null);
		subEnOnDeOn = translatePage(subDeOnEnOn, "SubEnOnDeOn", "en");

		// publish some of the pages
		publishPages(false, rootOn_28, rootDeOn_15, rootEnOnDeOff_26, rootDeOnEnOff_8, rootDeOnEnOn_77, rootEnOnDeOn_77, subOn, subDeOn,
				subEnOnDeOff, subDeOnEnOff, subDeOnEnOn, subEnOnDeOn);

		overviewEn = createPage(ObjectTransformer.getInteger(template.getId(), 0), folderId, "Overview (En)", "en", null);
		overviewDe = translatePage(overviewEn, "Overview (De)", "de");
	}

	/**
	 * Do the overview sort test
	 * 
	 * @param dsFallback
	 *            value of the feature dsfallback
	 * @param emptyCs
	 *            value of the feature ds_empty_cs
	 * @param multichannelling
	 *            value of the feature multichannelling
	 * @param overviewPage
	 *            overview page
	 * @param listType
	 *            list type
	 * @param selectType
	 *            select type
	 * @param recursive
	 *            recursive
	 * @param orderBy
	 *            order by
	 * @param selectedItems
	 *            list of selected object ids
	 * @param source
	 *            overview source
	 * @param items
	 *            expected list of items (sorted)
	 * @throws NodeException 
	 */
	protected void doOverviewSortTest(boolean dsFallback, boolean emptyCs, boolean multichannelling, Page overviewPage, ListType listType,
			SelectType selectType, boolean recursive, OrderBy orderBy, List<Integer> selectedItems, String source,
			List<? extends ContentNodeItem> items) throws NodeException {
		for (int i = 0; i <= items.size(); ++i) {
			doOverviewSortTest(dsFallback, emptyCs, multichannelling, overviewPage, listType, selectType, i, recursive, orderBy, OrderDirection.ASC,
					selectedItems, source, i == 0 ? items : items.subList(0, i));
		}
		Collections.reverse(items);
		for (int i = 0; i <= items.size(); ++i) {
			doOverviewSortTest(dsFallback, emptyCs, multichannelling, overviewPage, listType, selectType, i, recursive, orderBy, OrderDirection.DESC,
					selectedItems, source, i == 0 ? items : items.subList(0, i));
		}
	}

	/**
	 * Do the overview sort test
	 * 
	 * @param dsFallback
	 *            value of the feature dsfallback
	 * @param emptyCs
	 *            value of the feature ds_empty_cs
	 * @param multichannelling
	 *            value of the feature multichannelling
	 * @param overviewPage
	 *            overview page
	 * @param listType
	 *            list type
	 * @param selectType
	 *            select type
	 * @param maxItems
	 *            maximum number of items allowed
	 * @param recursive
	 *            recursive
	 * @param orderBy
	 *            order by
	 * @param orderDirection
	 *            order direction
	 * @param selectedItems
	 *            list of selected object ids
	 * @param source
	 *            overview source
	 * @param items
	 *            expected list of items (sorted)
	 * @throws NodeException 
	 */
	protected void doOverviewSortTest(boolean dsFallback, boolean emptyCs, boolean multichannelling, Page overviewPage, ListType listType,
			SelectType selectType, int maxItems, boolean recursive, OrderBy orderBy, OrderDirection orderDirection, List<Integer> selectedItems,
			String source, List<? extends ContentNodeItem> items) throws NodeException {
		String tagname = "overview";

		// prepare the overview page
		Overview overview = overviewPage.getTags().get(tagname).getProperties().get(OVERVIEW_PART_NAME).getOverview();

		overview.setListType(listType);
		overview.setSelectType(selectType);
		overview.setMaxItems(maxItems);
		overview.setRecursive(recursive);
		overview.setOrderBy(orderBy);
		overview.setOrderDirection(orderDirection);
		overview.setSource(source);
		overview.setSelectedItemIds(selectedItems);
		savePage(overviewPage);

		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		boolean oldDsFallback = prefs.getFeature("dsfallback");
		boolean oldEmptyCs = prefs.getFeature("ds_empty_cs");
		boolean oldMultichannelling = prefs.getFeature("multichannelling");

		try (Trx trx = new Trx(user)) {
			prefs.setFeature("dsfallback", dsFallback);
			prefs.setFeature("ds_empty_cs", emptyCs);
			prefs.setFeature("multichannelling", multichannelling);
			PageRenderResponse renderedOverviewEn = getPageResource().render(Integer.toString(overviewPage.getId()), null, null, false, null, LinksType.backend,
					false, false, false, 0);

			assertResponseOK(renderedOverviewEn);
			StringBuffer expectedOutput = new StringBuffer();

			for (ContentNodeItem item : items) {
				expectedOutput.append(item.getName()).append("|");
			}
			StringBuffer message = new StringBuffer();

			message.append(overviewPage.getName()).append(listType).append(" ").append(selectType).append(", ordered by ").append(orderBy)
					.append(", ").append(orderDirection);
			if (dsFallback) {
				message.append(" dsfallback");
			}
			if (emptyCs) {
				message.append(" ds_empty_cs");
			}
			if (multichannelling) {
				message.append(" multichannelling");
			}
			assertEquals(message.toString(), expectedOutput.toString(), renderedOverviewEn.getContent());

			trx.success();
		} finally {
			prefs.setFeature("dsfallback", oldDsFallback);
			prefs.setFeature("ds_empty_cs", oldEmptyCs);
			prefs.setFeature("multichannelling", oldMultichannelling);
		}
	}

	/**
	 * Helper method to create a folder
	 * 
	 * @param name
	 *            name of the folder
	 * @param motherId
	 *            mother id
	 * @param nameDe
	 *            value of the object.name_de OE
	 * @return new folder
	 * @throws NodeException
	 */
	protected Folder createFolder(String name, Integer motherId, String nameDe) throws NodeException {
		return Trx.supply(() -> {
			// create a folder next to the page
			FolderCreateRequest folderCreateRequest = new FolderCreateRequest();

			folderCreateRequest.setMotherId(motherId.toString());
			folderCreateRequest.setName(name);
			FolderLoadResponse loadResponse = getFolderResource().create(folderCreateRequest);
			Folder folder = loadResponse.getFolder();

			// set the object property
			folder.getTags().get("object.name_de").getProperties().get("text").setStringValue(nameDe);
			folder.getTags().get("object.name_de").setActive(true);
			// save the folder
			FolderSaveRequest folderSaveRequest = new FolderSaveRequest();

			folderSaveRequest.setFolder(folder);
			getFolderResource().save(folder.getId().toString(), folderSaveRequest);

			return folder;
		});
	}

	/**
	 * Create a new page
	 * 
	 * @param templateId
	 *            template id
	 * @param folderId
	 *            folder id
	 * @param name
	 *            name (null to stick with the default name)
	 * @param language
	 *            language code (empty to create page without language)
	 * @param priority
	 *            priority (null to have no priority)
	 * @return new page
	 * @throws TransactionException
	 */
	protected Page createPage(Integer templateId, Integer folderId, String name, String language, Integer priority) throws NodeException {
		Page page = Trx.supply(() -> {
			PageCreateRequest pageCreateRequest = new PageCreateRequest();

			pageCreateRequest.setFolderId(folderId.toString());
			pageCreateRequest.setTemplateId(templateId);
			if (!ObjectTransformer.isEmpty(language)) {
				pageCreateRequest.setLanguage(language);
			}

			return getPageResource().create(pageCreateRequest).getPage();
		});

		if (!StringUtils.isEmpty(name) || priority != null) {
			if (!StringUtils.isEmpty(name)) {
				page.setName(name);
			}
			if (priority != null) {
				page.setPriority(priority);
			}
			savePage(page);
		}

		return page;
	}

	/**
	 * Translate the given page into the given language
	 * 
	 * @param page
	 *            page
	 * @param name
	 *            page name
	 * @param language
	 *            language
	 * @return translated page
	 * @throws TransactionException
	 */
	protected Page translatePage(Page page, String name, String language) throws NodeException {
		Page translation = Trx.supply(() -> {
			PageLoadResponse loadResponse = getPageResource().translate(page.getId(), language, false, null);

			assertResponseOK(loadResponse);
			return loadResponse.getPage();
		});

		if (!StringUtils.isEmpty(name)) {
			translation.setName(name);
			savePage(translation);
		}

		return translation;
	}

	/**
	 * Save the given page
	 * 
	 * @param page
	 *            page
	 * @throws TransactionException
	 */
	protected void savePage(Page page) throws NodeException {
		Trx.operate(() -> {
			PageSaveRequest request = new PageSaveRequest(page);

			request.setUnlock(true);
			assertResponseOK(getPageResource().save(Integer.toString(page.getId()), request));
		});
	}

	/**
	 * Publish the list of given pages
	 * 
	 * @param allLang
	 *            true for publishing all language variants, false for only the given pages
	 * @param pages
	 *            list of pages
	 * @throws TransactionException
	 */
	protected void publishPages(boolean allLang, Page... pages) throws NodeException {
		Trx.operate(() -> {
			PagePublishRequest request = new PagePublishRequest();

			request.setAlllang(allLang);

			for (Page page : pages) {
				assertResponseOK(getPageResource().publish(Integer.toString(page.getId()), null, request));
			}
		});
	}

	/**
	 * Create and save a tag in the page. The tag may be filled with string values
	 * 
	 * @param page
	 *            page
	 * @param constructId
	 *            construct id
	 * @param tagName
	 *            tag name
	 * @param parts
	 *            map of part keywords to values
	 * @param pageResource
	 *            page resource
	 */
	protected Tag createTag(Page page, Integer constructId, String tagName, Map<String, String> parts, PageResourceImpl pageResource) throws NodeException {
		Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);
		assertThat(construct).as("Construct").isNotNull();
		Map<String, Part> partMap = construct.getParts().stream().collect(Collectors.toMap(p -> p.getKeyname(), p -> p));

		Tag tag = new Tag();

		tag.setActive(true);
		tag.setConstructId(constructId);
		tag.setName(tagName);
		tag.setType(Type.CONTENTTAG);
		if (!ObjectTransformer.isEmpty(parts)) {
			Map<String, Property> props = new HashMap<String, Property>();

			for (Map.Entry<String, String> entry : parts.entrySet()) {
				if (!partMap.containsKey(entry.getKey()) ) {
					continue;
				}
				Property value = new Property();

				value.setType(partMap.get(entry.getKey()).getPartType(null).getPropertyType());
				value.setStringValue(entry.getValue());
				props.put(entry.getKey(), value);
			}
			tag.setProperties(props);
		}

		page.getTags().put(tagName, tag);

		// save the page
		pageResource.save(page.getId().toString(), new PageSaveRequest(page));

		// load the oage
		PageLoadResponse loadResponse = pageResource.load(ObjectTransformer.getString(page.getId(), null), false, false, false, false, false, false,
				false, false, false, false, null, null);

		assertResponseOK(loadResponse);
		return loadResponse.getPage().getTags().get(tagName);
	}

	/**
	 * Assert that exactly the expected content was found. This will normalize \r\n and remove timestamps before comparing
	 * 
	 * @param expected
	 *            expected content
	 * @param actual
	 *            actual content
	 * @throws NodeException
	 */
	protected void assertContent(String expected, String actual) throws NodeException {
		expected = removeTimestamps(expected.replaceAll("\r\n", "\n"));
		actual = removeTimestamps(actual.replaceAll("\r\n", "\n"));
		assertEquals("Check the rendered content", expected, actual);
	}

	/**
	 * Removes timestamps and fix newlines from the given string. Timestamps that are supported:
	 * <ul>
	 * <li>Timestamps in JSON objects that look like <code>"timestamp": 122311221.</code></li>
	 * <li>Timestamps in URLs that look like <code>time=12151658465</code></li>
	 * </ul>
	 * 
	 */
	private String removeTimestamps(String string) {
		return string.replaceAll("\"timestamp\"\\s:\\s[0-9]*", "\"timestamp\" : 0").replaceAll("time=[0-9]*", "time=0").replaceAll("\r\n", "\n");
	}

	/**
	 * Assert that the response code is {@link ResponseCode.OK}
	 * 
	 * @param response
	 *            response
	 */
	protected void assertResponseOK(GenericResponse response) {
		assertTrue(response.getResponseInfo().getResponseMessage(), response.getResponseInfo().getResponseCode() == ResponseCode.OK);
	}


	/**
	 * Creates a simple templates with a "test" tag that renders velocity.
	 *
	 * @return The created template
	 * @throws NodeException
	 */
	private Template createTemplate() throws NodeException {
		return supply(t -> {
			final Template template = t.createObject(Template.class);
			template.setName("TestTemplate");
			template.setMlId(1);
			template.setSource("<node test>");
			TemplateTag tag = t.createObject(TemplateTag.class);
			tag.setConstructId(VELOCITY_CONSTRUCT_ID);
			tag.setEnabled(true);
			tag.setPublic(true);
			tag.setName("test");
			template.getTags().put(tag.getName(), tag);
			template.getFolders().add(t.getObject(com.gentics.contentnode.object.Folder.class, ROOT_FOLDER_ID));
			template.save();
			return template;
		});
	}

	/**
	 * Renders the give page's preview, and asserts that the response is
	 * {@link ResponseCode.OK}.
	 *
	 * @param  page     Page to preview
	 * @param  resource Page resource with which to generate the preview
	 * @return The contents of the page preview
	 */
	private String previewPage(final Page page, final PageResourceImpl resource) throws NodeException {
		final PagePreviewResponse response = resource.preview(
			new PagePreviewRequest() {{setPage(page);}}
		);
		assertResponseOK(response);
		return response.getPreview();
	}
}
