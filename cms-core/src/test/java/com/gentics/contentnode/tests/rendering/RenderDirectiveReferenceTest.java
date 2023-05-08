package com.gentics.contentnode.tests.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for rendering other pages or tags of other pages using the render directive, when the other pages reference objects
 */
public class RenderDirectiveReferenceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Construct velocityConstruct;

	private static Construct htmlConstruct;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		velocityConstruct = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Construct construct = ContentNodeTestDataUtils.createVelocityConstruct(node);
			construct = t.getObject(construct, true);
			for (Part p : construct.getParts()) {
				if ("template".equals(p.getKeyname())) {
					p.setEditable(1);
				}
			}
			construct.save();
			return t.getObject(construct);
		});
		htmlConstruct = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(Construct.class,
				ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "html", "html")));
		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "[<node vtl>][<node html1>][<node html2>]", "Template",
				ContentNodeTestDataUtils.createTemplateTag(velocityConstruct.getId(), "vtl", true, false),
				ContentNodeTestDataUtils.createTemplateTag(htmlConstruct.getId(), "html1", true, false),
				ContentNodeTestDataUtils.createTemplateTag(htmlConstruct.getId(), "html2", true, false)));
	}

	/**
	 * Test rendering another page using the render directive, where tags in the other page reference each other
	 * @throws Exception
	 */
	@Test
	public void testRenderOtherPage() throws Exception {
		Page page1 = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 1");
			p = t.getObject(p, true);
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html1"), "html").getValueObject().setValueText("<node html2>");
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html2"), "html").getValueObject().setValueText("Content in Page 1");
			p.save();
			return t.getObject(p);
		});

		String content1 = "[][Content in Page 1][Content in Page 1]";
		try (Trx trx = new Trx(); RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			assertThat(page1.render()).as("Rendered content page 1").isEqualTo(content1);
		}
		

		Page page2 = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 2");
			p = t.getObject(p, true);
			ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, p.getContentTag("vtl"), "template").getValueObject()
					.setValueText("#set($page1 = $cms.imps.loader.getPage(" + page1.getId() + "))##\n$page1\n#gtx_render($page1)");
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html2"), "html").getValueObject().setValueText("Content in Page 2");
			p.save();
			return t.getObject(p);
		});

		String content2 = "[" + content1 + "\n" + content1 + "][][Content in Page 2]";
		try (Trx trx = new Trx(); RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			assertThat(page2.render()).as("Rendered content").isEqualTo(content2);
		}
	}

	/**
	 * Test rendering a tag in another page, using the render directive, where the other tag references a tag in the other page
	 * @throws Exception
	 */
	@Test
	public void testRenderTagOfOtherPage() throws Exception {
		Page page1 = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 1");
			p = t.getObject(p, true);
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html1"), "html").getValueObject().setValueText("<node html2>");
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html2"), "html").getValueObject().setValueText("Content in Page 1");
			p.save();
			return t.getObject(p);
		});

		String content1 = "[][Content in Page 1][Content in Page 1]";
		try (Trx trx = new Trx(); RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			assertThat(page1.render()).as("Rendered content page 1").isEqualTo(content1);
		}

		Page page2 = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 2");
			p = t.getObject(p, true);
			ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, p.getContentTag("vtl"), "template").getValueObject()
					.setValueText("#set($page1 = $cms.imps.loader.getPage(" + page1.getId() + "))##\n$page1.tags.html1\n#gtx_render($page1.tags.html1)");
			ContentNodeTestDataUtils.getPartType(HTMLPartType.class, p.getContentTag("html2"), "html").getValueObject().setValueText("Content in Page 2");
			p.save();
			return t.getObject(p);
		});

		try (Trx trx = new Trx(); RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			assertThat(page2.render()).as("Rendered content").isEqualTo("[Content in Page 1\nContent in Page 1][][Content in Page 2]");
		}
	}
}
