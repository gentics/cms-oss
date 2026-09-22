package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.VelocityPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.NodeObjectHandler;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering with teh channel directive
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
public class ChannelDirectiveRenderTest {
	/**
	 * Keyword of the page construct
	 */
	public final static String PAGE_CONSTRUCT_KEYWORD = "pageurl";

	/**
	 * Keyword of the page part
	 */
	public final static String PAGE_PART_KEYWORD = "page";

	/**
	 * Keyword of the vtl construct
	 */
	public final static String VTL_CONSTRUCT_KEYWORD = "velocity";

	/**
	 * Keyword of the vtl part
	 */
	public final static String VTL_PART_KEYWORD = "vtl";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node sourceNode;

	protected static Node sourceChannel;

	protected static Node targetNode;

	protected static Integer pageConstructId;

	protected static Integer vtlConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		sourceNode = Trx.supply(
				() -> ContentNodeTestDataUtils.createNode("source", "Source", PublishTarget.NONE));
		sourceChannel = Trx.supply(
				() -> ContentNodeTestDataUtils.createChannel(sourceNode, "Channel", "channel", "/"));

		targetNode = Trx.supply(
				() -> ContentNodeTestDataUtils.createNode("target", "Target", PublishTarget.NONE));

		pageConstructId = Trx.supply(
				() -> ContentNodeTestDataUtils.createConstruct(sourceNode, PageURLPartType.class,
						PAGE_CONSTRUCT_KEYWORD,
						PAGE_PART_KEYWORD));
		vtlConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createVelocityConstruct(sourceNode,
				VTL_CONSTRUCT_KEYWORD, VTL_PART_KEYWORD));
	}

	/**
	 * Test rendering a link to another page of another node in the scope of that node, if the
	 * rendered page is in a local folder
	 *
	 * @throws NodeException
	 */
	@Test
	public void testRenderLinkWithLocalId() throws NodeException {
		this.testRenderLink(false);
	}

	@Test
	public void testRenderLinkWithGlobalId() throws NodeException {
		this.testRenderLink(true);
	}

	@Test
	public void testPageResourceRenderTag() throws NodeException {
		Folder targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "TargetFolder"));
		Template targetTemplate = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(targetFolder, "TargetPage"));
		Page targetPage = Trx.supply(() -> ContentNodeTestDataUtils.createPage(targetFolder, targetTemplate, "TargetPage", sourceChannel));

		Trx.operate(t -> t.getObject(targetPage, true).publish());

		Folder sourceFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "SourceFolder"));
		Template sourceTemplate = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(
			sourceNode.getFolder(),
			"<node vtl>",
			"Template",
			ContentNodeTestDataUtils.createTemplateTag(vtlConstructId, "vtl", true, false),
			ContentNodeTestDataUtils.createTemplateTag(pageConstructId, "page", true, false)));
		Page sourcePage = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(sourceFolder, sourceTemplate, "SourcePage");

			p = t.getObject(p, true);

			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, p.getContentTag("page"), "page")
				.setTargetPage(p);

			String valueText = String.format("#gtx_channel(%s)<PageId:$cms.page.tags.page.parts.page.id>#end", sourceChannel.getId());

			ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, p.getContentTag("vtl"), "template")
				.getValueObject()
				.setValueText(valueText);

			p.save();

			return t.getObject(p);
		});

		com.gentics.contentnode.rest.model.Page restPage = supply(() -> ModelBuilder.getPage(sourcePage, Collections.singleton(Reference.CONTENT_TAGS)));

		Property pageProperty = restPage.getTags().get("page").getProperties().get("page");

		pageProperty.setPageId(targetPage.getId());
		pageProperty.setNodeId(supply(() -> targetPage.getChannel().getId()));

		SystemUser systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		PageRenderResponse renderResponse = Trx.supply(systemUser, () -> new PageResourceImpl().renderTag("vtl", sourcePage.getNode().getId(), null, LinksType.backend, restPage));

		assertResponseCodeOk(renderResponse);

		Matcher matcher = Pattern.compile("<PageId:(\\d+)>").matcher(renderResponse.getContent());

		assertThat(matcher.find())
			.as("Page ID found in rendered content")
			.isTrue();

		assertThat(matcher.group(1))
			.as("Rendered page ID")
			.isEqualTo(targetPage.getId().toString());
	}

	/**
	 * Test rendering a tag of a page in the master node from a channel local page in a channel local folder
	 * with and without using the channel directive
	 * @throws NodeException
	 */
	@Test
	public void testRenderMasterPageTagFromLocalPage() throws NodeException {
		final String vtlTagName = "vtltag";
		final String pageTagName = "pagetag";
		final String tagContentMaster = "Tag of the target page in the master";
		final String tagContentChannel = "Tag of the target page in the channel";

		SystemUser user = supply(t -> t.getObject(SystemUser.class, 1));

		Template template = create(Template.class, t -> {
			t.setName("Template");
			t.setFolderId(sourceNode.getFolder().getId());

			TemplateTag vtlTag = create(TemplateTag.class, tt -> {
				tt.setConstructId(vtlConstructId);
				tt.setName(vtlTagName);
				tt.setEnabled(true);
				tt.setPublic(true);
			}).doNotSave().build();
			t.getTemplateTags().put(vtlTag.getName(), vtlTag);

			TemplateTag pageTag = create(TemplateTag.class, tt -> {
				tt.setConstructId(pageConstructId);
				tt.setName(pageTagName);
				tt.setEnabled(true);
				tt.setPublic(true);
			}).doNotSave().build();
			t.getTemplateTags().put(pageTag.getName(), pageTag);

			t.setSource("<html><head></head><body><node %s></body></html>".formatted(vtlTagName));
		}).unlock().build();

		Folder targetFolder = create(Folder.class, f -> {
			f.setMotherId(sourceNode.getFolder().getId());
			f.setName("Master Folder");
		}).build();
		Page targetPage = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(sourceNode, targetFolder);
			p.setName("Target Page");

			getPartType(LongHTMLPartType.class, p.getContentTag(vtlTagName), VelocityPartType.INPUT_TEMPLATE)
					.setText(tagContentMaster);
		}).unlock().build();

		supply(() -> update(localize(targetPage, sourceChannel), p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag(vtlTagName), VelocityPartType.INPUT_TEMPLATE)
					.setText(tagContentChannel);
		}).unlock().build());

		Folder localSourceFolder = create(Folder.class, f -> {
			f.setMotherId(sourceNode.getFolder().getId());
			f.setName("Source Folder");
			f.setChannelInfo(sourceChannel.getId(), null);
		}).build();

		Page localSourcePage = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(sourceChannel, localSourceFolder);
			p.setChannelInfo(sourceChannel.getId(), null);
			p.setName("Source Page");

			getPartType(PageURLPartType.class, p.getContentTag(pageTagName), PAGE_PART_KEYWORD).setTargetPage(targetPage);
		}).unlock().build();

		// render without channel directive
		testRenderPage(user, localSourcePage, p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag(vtlTagName), VelocityPartType.INPUT_TEMPLATE)
					.setText("#gtx_render($cms.page.tags.%s.parts.%s.target.tags.%s)"
							.formatted(pageTagName, PAGE_PART_KEYWORD, vtlTagName));
		}, tagContentChannel);

		// render with channel directive for channel
		testRenderPage(user, localSourcePage, p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag(vtlTagName), VelocityPartType.INPUT_TEMPLATE)
					.setText("#gtx_channel(%d)#gtx_render($cms.page.tags.%s.parts.%s.target.tags.%s)#end"
							.formatted(sourceChannel.getId(), pageTagName, PAGE_PART_KEYWORD, vtlTagName));
		}, tagContentChannel);

		// render with channel directive for master
		testRenderPage(user, localSourcePage, p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag(vtlTagName), VelocityPartType.INPUT_TEMPLATE)
					.setText("#gtx_channel(%d)#gtx_render($cms.page.tags.%s.parts.%s.target.tags.%s)#end"
							.formatted(sourceNode.getId(), pageTagName, PAGE_PART_KEYWORD, vtlTagName));
		}, tagContentMaster);
	}

	/**
	 * Test rendering the page in the channel
	 * @param user user rendering the page
	 * @param page rendered page
	 * @param updater updater to update the page before rendering
	 * @param expectedContent expected content (part of it)
	 * @throws NodeException
	 */
	private void testRenderPage(SystemUser user, Page page, NodeObjectHandler<Page> updater, String expectedContent) throws NodeException {
		page = update(page, updater).unlock().build();

		try (Trx trx = new Trx(user);
				RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA);
				ChannelTrx ctrx = new ChannelTrx(sourceChannel)) {
			String renderedPage = page.render();

			// rendering should not produce error messages
			RenderResult renderResult = trx.getTransaction().getRenderResult();
			assertThat(renderResult).doesNotContainErrors();

			// page should contain Aloha Editor
			assertThat(renderedPage).as("Page rendered in Edit Mode")
				.contains("gcmsui-scripts-launcher.js")
				.contains("Aloha.settings");

			// page should render the tag of the other page
			assertThat(renderedPage).as("Page rendered in Edit Mode")
				.contains(expectedContent);
		}
	}

	private void testRenderLink(boolean isGlobalId) throws NodeException {
		Folder targetFolder = Trx.supply(
				() -> ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "TargetFolder"));
		Page targetPage = Trx.supply(
				() -> ContentNodeTestDataUtils.createTemplateAndPage(targetFolder, "TargetPage"));
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.getObject(targetPage, true).publish();
		});

		Folder sourceFolder = Trx.supply(
				() -> ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "SourceFolder",
						sourceChannel));
		Template sourceTemplate = Trx.supply(
				() -> ContentNodeTestDataUtils.createTemplate(sourceNode.getFolder(), "<node vtl>",
						"Template",
						ContentNodeTestDataUtils.createTemplateTag(vtlConstructId, "vtl", true, false),
						ContentNodeTestDataUtils.createTemplateTag(pageConstructId, "page", true, false)));
		Page sourcePage = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(sourceFolder, sourceTemplate, "SourcePage",
					sourceChannel);

			p = t.getObject(p, true);
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, p.getContentTag("page"),
							"page")
					.setTargetPage(targetPage);

			String valueText = "";

			if (!isGlobalId) {
				valueText = String.format("#gtx_channel(%s)$cms.page.tags.page#end",
						targetNode.getId());
			} else {
				valueText = String.format("#gtx_channel(\"%s\")$cms.page.tags.page#end",
						targetNode.getGlobalId());
			}

			ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, p.getContentTag("vtl"),
							"template").getValueObject()
					.setValueText(valueText);
			p.save();

			return t.getObject(p);
		});

		Trx.operate(() -> {
			try (ChannelTrx cTrx = new ChannelTrx(
					sourceChannel); RenderTypeTrx rtTrx = RenderTypeTrx.publish()) {
				assertThat(sourcePage.render()).as(sourcePage + " rendered").
						startsWith("http://target/Content.node/home/TargetPage")
						.endsWith(".html");
			}
		});
	}

}
