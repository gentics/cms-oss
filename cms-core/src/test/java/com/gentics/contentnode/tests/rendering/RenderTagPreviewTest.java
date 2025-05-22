package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for rendering tag previews
 */
public class RenderTagPreviewTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;
	private static Construct simpleHtmlConstruct;
	private static Construct complexConstruct;
	private static Template template;
	private static SystemUser systemUser;

	private Page page;
	private int pageId;

	private String innerTagName;
	private int innerTagId;

	private static String outerTagName = "html";
	private int outerTagId;

	private String complexTagName;
	private int complexTagId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		simpleHtmlConstruct = supply(t -> t.getObject(Construct.class, createConstruct(node, HTMLPartType.class, "test", "test")));

		complexConstruct = create(Construct.class, create -> {
			create.setAutoEnable(true);
			create.setKeyword("complex");
			create.setName("complex", 1);
			create.getNodes().add(node);

			List<Part> parts = create.getParts();

			// add HTML part
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("html");
				p.setName("html", 1);
				p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(HTMLPartType.class));
				p.setDefaultValue(create(Value.class, c -> {}).doNotSave().build());
			}).doNotSave().build());

			// add part which references ".text" from the html part
			parts.add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("referencing");
				p.setName("referencing", 1);
				p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(HTMLPartType.class));
				p.setDefaultValue(create(Value.class, c -> {
					c.setValueText("<node html.text>");
				}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		template = create(Template.class, create -> {
			create.setName("Test Template");
			create.setSource("<node %s>".formatted(outerTagName));
			create.addFolder(node.getFolder());

			create.getTags().put(outerTagName, create(TemplateTag.class, tag -> {
				tag.setConstructId(simpleHtmlConstruct.getId());
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName(outerTagName);
			}).doNotSave().build());
		}).build();

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			clear(node);
		});

		page = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(template.getId());

			ContentTag innerTag = create.getContent().addContentTag(simpleHtmlConstruct.getId());
			innerTagName = innerTag.getName();
			getPartType(HTMLPartType.class, innerTag, "test").setText("Inner Tag Content");

			getPartType(HTMLPartType.class, create.getContentTag(outerTagName), "test").setText("Outer Tag Content [<node %s>]".formatted(innerTagName));

			ContentTag complexTag = create.getContent().addContentTag(complexConstruct.getId());
			complexTagName = complexTag.getName();
			getPartType(HTMLPartType.class, complexTag, "html").setText("Content of the Complex Tag");
		}).build();

		pageId = execute(Page::getId, page);

		consume(p -> {
			outerTagId = p.getContentTag(outerTagName).getId();
			innerTagId = p.getContentTag(innerTagName).getId();
			complexTagId = p.getContentTag(complexTagName).getId();
		}, page);
	}

	/**
	 * Test rendering an embedded tag
	 * @throws NodeException
	 */
	@Test
	public void testRenderInnerTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse response = pageResource.renderTag(page.getGlobalId().toString(), innerTagName, null, null, LinksType.backend);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedInnerTag("Inner Tag Content"));
		}
	}

	/**
	 * Test rendering a tag coming from the template, which embeds another tag
	 * @throws NodeException
	 */
	@Test
	public void testRenderOuterTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse response = pageResource.renderTag(page.getGlobalId().toString(), outerTagName, null, null, LinksType.backend);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedOuterTag("Outer Tag Content [%s]", "Inner Tag Content"));
		}
	}

	/**
	 * Test rendering a "complex" tag (one where the rendered part references the other)
	 * @throws NodeException
	 */
	@Test
	public void testRenderComplexTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();
			PageRenderResponse response = pageResource.renderTag(page.getGlobalId().toString(), complexTagName, null, null, LinksType.backend);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedComplexTag("Content of the Complex Tag"));
		}
	}

	/**
	 * Test rendering the preview for an embedded tag
	 * @throws NodeException
	 */
	@Test
	public void testPreviewInnerTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();

			PageLoadResponse loadResponse = pageResource.load(page.getGlobalId().toString(), false, false, false, false,
					false, false, false, false, false, true, null, null);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Page model = loadResponse.getPage();
			model.getTags().get(innerTagName).getProperties().get("test").setStringValue("Modified Inner Tag Content");

			PageRenderResponse response = pageResource.renderTag(innerTagName, null, null, LinksType.backend, model);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedInnerTag("Modified Inner Tag Content"));
		}
	}

	/**
	 * Test rendering the preview for a tag coming from the template, which embeds another tag
	 * @throws NodeException
	 */
	@Test
	public void testPreviewOuterTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();

			PageLoadResponse loadResponse = pageResource.load(page.getGlobalId().toString(), false, false, false, false,
					false, false, false, false, false, true, null, null);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Page model = loadResponse.getPage();
			model.getTags().get(innerTagName).getProperties().get("test").setStringValue("Modified Inner Tag Content");
			model.getTags().get(outerTagName).getProperties().get("test").setStringValue("Modified Outer Tag Content [<node %s>]".formatted(innerTagName));

			PageRenderResponse response = pageResource.renderTag(outerTagName, null, null, LinksType.backend, model);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedOuterTag("Modified Outer Tag Content [%s]", "Modified Inner Tag Content"));
		}
	}

	/**
	 * Test rendering the preview for a "complex" tag
	 * @throws NodeException
	 */
	@Test
	public void testPreviewComplexTag() throws NodeException {
		try (Trx trx = new Trx(systemUser)) {
			PageResource pageResource = getPageResource();

			PageLoadResponse loadResponse = pageResource.load(page.getGlobalId().toString(), false, false, false, false,
					false, false, false, false, false, true, null, null);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Page model = loadResponse.getPage();
			model.getTags().get(complexTagName).getProperties().get("html").setStringValue("Modified Content of the Complex Tag");

			PageRenderResponse response = pageResource.renderTag(complexTagName, null, null, LinksType.backend, model);
			assertResponseOK(response);

			assertThat(response.getContent()).as("Rendered Tag").isEqualTo(getRenderedComplexTag("Modified Content of the Complex Tag"));
		}
	}

	protected String getRenderedOuterTag(String outerTagContent, String innerTagContent) {
		String renderedInnerTag = getRenderedTag(innerTagId, innerTagName, "test", innerTagContent);
		return getRenderedTag(outerTagId, outerTagName, "test", outerTagContent.formatted(renderedInnerTag));
	}

	protected String getRenderedInnerTag(String tagContent) {
		return getRenderedTag(innerTagId, innerTagName, "test", tagContent);
	}

	protected String getRenderedComplexTag(String tagContent) {
		return getRenderedTag(complexTagId, complexTagName, "complex", tagContent);
	}

	protected String getRenderedTag(int tagId, String tagName, String constructName, String tagContent) {
		return "<div data-gcn-pageid=\"%d\" data-gcn-tagid=\"%d\" data-gcn-tagname=\"%s\" data-gcn-i18n-constructname=\"%s\" class=\"aloha-block\" id=\"GENTICS_BLOCK_%d\">%s</div>"
				.formatted(pageId, tagId, tagName, constructName, tagId, tagContent);
	}
}
