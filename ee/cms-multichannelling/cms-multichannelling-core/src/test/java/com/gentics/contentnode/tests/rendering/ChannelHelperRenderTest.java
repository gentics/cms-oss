package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering with the channel helper
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
public class ChannelHelperRenderTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node sourceNode;

	protected static Node sourceChannel;

	protected static Node targetNode;

	protected static Integer pageConstructId;

	protected static Integer hbsConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		sourceNode = Trx.supply(
				() -> ContentNodeTestDataUtils.createNode("source", "Source", PublishTarget.NONE));
		sourceChannel = Trx.supply(
				() -> ContentNodeTestDataUtils.createChannel(sourceNode, "Channel", "channel", "/"));

		targetNode = Trx.supply(
				() -> ContentNodeTestDataUtils.createNode("target", "Target", PublishTarget.NONE));

		pageConstructId = Trx.supply(
				() -> ContentNodeTestDataUtils.createConstruct(sourceNode, PageURLPartType.class,
						"pageurl",
						"page"));
		hbsConstructId = Trx.supply(
				() -> ContentNodeTestDataUtils.createConstruct(sourceNode, HandlebarsPartType.class, "hbs", "hbs"));
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
			"<node hbs>",
			"Template",
			ContentNodeTestDataUtils.createTemplateTag(hbsConstructId, "hbs", true, false),
			ContentNodeTestDataUtils.createTemplateTag(pageConstructId, "page", true, false)));
		Page sourcePage = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page p = ContentNodeTestDataUtils.createPage(sourceFolder, sourceTemplate, "SourcePage");

			p = t.getObject(p, true);

			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, p.getContentTag("page"), "page")
				.setTargetPage(p);

			String valueText = String.format("{{#gtx_channel %s}}<PageId:{{ cms.page.tags.page.parts.page.id}}>{{/gtx_channel}}", sourceChannel.getId());

			ContentNodeTestDataUtils.getPartType(HandlebarsPartType.class, p.getContentTag("hbs"), "hbs")
				.getValueObject()
				.setValueText(valueText);

			p.save();

			return t.getObject(p);
		});

		com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(sourcePage, Collections.singleton(Reference.CONTENT_TAGS));

		Property pageProperty = restPage.getTags().get("page").getProperties().get("page");

		pageProperty.setPageId(targetPage.getId());
		pageProperty.setNodeId(targetPage.getChannel().getId());

		SystemUser systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		PageRenderResponse renderResponse = Trx.supply(systemUser, () -> new PageResourceImpl().renderTag("hbs", sourcePage.getNode().getId(), null, LinksType.backend, restPage));

		assertResponseCodeOk(renderResponse);

		Matcher matcher = Pattern.compile("<PageId:(\\d+)>").matcher(renderResponse.getContent());

		assertThat(matcher.find())
			.as("Page ID found in rendered content")
			.isTrue();

		assertThat(matcher.group(1))
			.as("Rendered page ID")
			.isEqualTo(targetPage.getId().toString());
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
				() -> ContentNodeTestDataUtils.createTemplate(sourceNode.getFolder(), "<node hbs>",
						"Template",
						ContentNodeTestDataUtils.createTemplateTag(hbsConstructId, "hbs", true, false),
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
				valueText = String.format("{{#gtx_channel %s}}{{gtx_render cms.page.tags.page}}{{/gtx_channel}}",
						targetNode.getId());
			} else {
				valueText = String.format("{{#gtx_channel \"%s\"}}{{gtx_render cms.page.tags.page}}{{/gtx_channel}}",
						targetNode.getGlobalId());
			}

			ContentNodeTestDataUtils.getPartType(HandlebarsPartType.class, p.getContentTag("hbs"),
							"hbs").getValueObject()
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
