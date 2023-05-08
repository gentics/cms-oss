package com.gentics.contentnode.tests.rendering;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering with teh channel directive
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
public class ChannelDirectiveRenderTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node sourceNode;

	protected static Node sourceChannel;

	protected static Node targetNode;

	protected static Integer pageConstructId;

	protected static Integer vtlConstructId;

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
		vtlConstructId = Trx.supply(
				() -> ContentNodeTestDataUtils.createVelocityConstruct(sourceNode, "velocity", "vtl"));
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
