package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Testcases for using &lt;node&gt;-Notation in VTL Templates
 */
@RunWith(value = Parameterized.class)
public class VelocityNodeNotationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Pattern for the template
	 */
	public static String TEMPLATE_PATTERN = "this is the template\n[%s]\nthis is the template";

	private static Node node;

	private static Template template;

	/**
	 * Construct rendering velocity
	 */
	private static Construct vtlConstruct;

	/**
	 * Construct rendering velocity with inline editable template
	 */
	private static Construct inlineEditableVtlConstruct;

	/**
	 * Construct containing a single, inline editable part
	 */
	private static Construct inlineEditableConstruct;

	@Parameters(name = "{index}: render {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedContent test : TestedContent.values()) {
			data.add(new Object[] { test });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		vtlConstruct = supply(() -> create(Construct.class, c -> {
			c.setKeyword("vtl");
			c.setName("VTL", 1);

			// Velocity part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("velocity");
				p.setHidden(false);
				p.setEditable(0);
				p.setName("velocity", 1);
				p.setPartTypeId(getPartTypeId(VelocityPartType.class));
			}, false));

			// Template part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("template");
				p.setHidden(true);
				p.setEditable(1);
				p.setName("Template", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));

			// Inline editable part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("inline");
				p.setHidden(true);
				p.setEditable(2);
				p.setName("Inline", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));

			// Editable part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("editable");
				p.setHidden(true);
				p.setEditable(1);
				p.setName("Editable", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));
		}));
		inlineEditableVtlConstruct = supply(() -> create(Construct.class, c -> {
			c.setKeyword("vtl_inline");
			c.setName("VTL Inline", 1);

			// Velocity part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("velocity");
				p.setHidden(false);
				p.setEditable(0);
				p.setName("velocity", 1);
				p.setPartTypeId(getPartTypeId(VelocityPartType.class));
			}, false));

			// Template part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("template");
				p.setHidden(true);
				p.setEditable(2);
				p.setName("Template", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));
		}));

		inlineEditableConstruct = supply(() -> create(Construct.class, c -> {
			c.setKeyword("inline");
			c.setName("Inline", 1);

			// Inline editable part
			c.getParts().add(create(Part.class, p -> {
				p.setKeyname("inline");
				p.setHidden(false);
				p.setEditable(2);
				p.setName("Inline", 1);
				p.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
			}, false));
		}));
	}

	/**
	 * Parameter defining the tested content
	 */
	@Parameter(0)
	public TestedContent tested;

	/**
	 * Test rendering an inline part using the VTL syntax
	 * @throws NodeException
	 */
	@Test
	public void testRenderVtlSyntax() throws NodeException {
		test(p -> {
			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "$cms.tag.parts.inline"));
			getPartType(LongHTMLPartType.class, tag, "inline").getValueObject().setValueText(tested.content);
			return tag.getName();
		});
	}

	/**
	 * Test rendering an inline part using the gtx_edit Directive
	 * @throws NodeException
	 */
	@Test
	public void testRenderDirective() throws NodeException {
		test(p -> {
			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "#gtx_edit(\"inline\")"));
			getPartType(LongHTMLPartType.class, tag, "inline").getValueObject().setValueText(tested.content);
			return tag.getName();
		});
	}

	/**
	 * Test rendering an inline part using the &lt;node&gt;-Notation
	 * @throws NodeException
	 */
	@Test
	public void testRenderNodeSyntax() throws NodeException {
		test(p -> {
			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "<node inline>"));
			getPartType(LongHTMLPartType.class, tag, "inline").getValueObject().setValueText(tested.content);
			return tag.getName();
		});
	}

	/**
	 * Test rendering another part (using VTL syntax), that renders the inline editable part using &lt;node&gt;-Notation
	 * @throws NodeException
	 */
	@Test
	public void testRenderIndirectNodeSyntax() throws NodeException {
		test(p -> {
			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "$cms.tag.parts.editable"));
			getPartType(LongHTMLPartType.class, tag, "editable").getValueObject().setValueText("<node inline>");
			getPartType(LongHTMLPartType.class, tag, "inline").getValueObject().setValueText(tested.content);
			return tag.getName();
		});
	}

	/**
	 * Test rendering another part (using &lt;node&gt;-Notation), that renders the inline editable part using &lt;node&gt;-Notation
	 * @throws NodeException
	 */
	@Test
	public void testRenderIndirectDoubleNodeSyntax() throws NodeException {
		test(p -> {
			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "<node editable>"));
			getPartType(LongHTMLPartType.class, tag, "editable").getValueObject().setValueText("<node inline>");
			getPartType(LongHTMLPartType.class, tag, "inline").getValueObject().setValueText(tested.content);
			return tag.getName();
		});
	}

	/**
	 * Test rendering another tag (containing an inline editable part) using the &lt;node&gt;-Notation
	 * @throws NodeException
	 */
	@Test
	public void testRenderOtherTagNodeSyntax() throws NodeException {
		test(p -> {
			ContentTag otherTag = p.getContent().addContentTag(inlineEditableConstruct.getId());
			otherTag.setEnabled(true);
			getPartType(LongHTMLPartType.class, otherTag, "inline").getValueObject().setValueText(tested.content);

			ContentTag tag = p.getContent().addContentTag(vtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "<node " + otherTag.getName() + ">"));
			return tag.getName();
		});
	}

	@Test
	public void testInlineEditableTemplate() throws NodeException {
		test(p -> {
			ContentTag otherTag = p.getContent().addContentTag(inlineEditableConstruct.getId());
			otherTag.setEnabled(true);
			getPartType(LongHTMLPartType.class, otherTag, "inline").getValueObject().setValueText(tested.content);

			ContentTag tag = p.getContent().addContentTag(inlineEditableVtlConstruct.getId());
			tag.setEnabled(true);
			getPartType(LongHTMLPartType.class, tag, "template").getValueObject().setValueText(String.format(TEMPLATE_PATTERN, "$cms.page.tags." + otherTag.getName()));
			return tag.getName();
		});
	}

	/**
	 * Generic test method. Will create a page, pass the page to the
	 * setupFunction, render the tag (which was returned by the setupFunction)
	 * and compare the result with expected result
	 * 
	 * @param setupFunction setup function. Gets the page and must return the name of the Velocity Tag
	 * @throws NodeException
	 */
	protected void test(Function<Page, String> setupFunction) throws NodeException {
		AtomicReference<String> tagName = new AtomicReference<>();
		Page page = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Page");

			tagName.set(setupFunction.apply(p));
		});

		String renderedContent = supply(() -> {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW)) {
				return page.render(String.format("<node %s>", tagName.get()), null, null, null, null, null);
			}
		});

		assertThat(renderedContent).as("Rendered tag").isEqualTo(tested.expected);
	}

	/**
	 * Enum of tested contents
	 */
	public static enum TestedContent {
		hash("before the evil part ##evil after the evil part"),
		dollar("before the evil part $cms.page after the evil part"),
		backslash("before the evil part \\evil after the evil part"),
		singlequote("before the evil part 'evil' after the evil part"),
		doublequote("before the evil part \"evil\" after the evil part"),
		exclamation("before the evil part !evil! after the evil part");

		/**
		 * Content containing evil characters
		 */
		public String content;

		/**
		 * Expected rendered content
		 */
		public String expected;

		/**
		 * Create instance with content
		 * @param content content
		 */
		private TestedContent(String content) {
			this.content = content;
			this.expected = String.format(TEMPLATE_PATTERN, content);
		}
	}
}
