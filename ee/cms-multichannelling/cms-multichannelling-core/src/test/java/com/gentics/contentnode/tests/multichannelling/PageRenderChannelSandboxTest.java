package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.rest.model.Editor;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.rest.PageRenderSandboxTest;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * Page tests for multichannelling support.
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class PageRenderChannelSandboxTest extends AbstractMultichannellingSandboxTest {
	private int hbsConstructId;

	@Before
	public void setup() throws NodeException {
		user = supply(t -> t.getObject(SystemUser.class, 1));
		Node node = supply(t -> t.getObject(Node.class, PageRenderSandboxTest.NODE_ID));

		hbsConstructId = supply(() -> createConstruct(node, HandlebarsPartType.class, PageRenderSandboxTest.HBS_PART_NAME, PageRenderSandboxTest.HBS_PART_NAME));
	}

	/**
	 * Test rendering node properties
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testRenderNodeMetaPropertiesInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		// Get the channel node that inherits the master node
		Node channel = t.getObject(Node.class, channelId, true);

		// Get the root folder of the channel
		Folder channelFolder = channel.getFolder();

		// Create a local test page in the channel
		Page page = t.createObject(Page.class);

		page.setName(C.page_channel_test.new_page_name);
		page.setFolderId(channelFolder.getId());
		page.setTemplateId(C.page_channel_test.new_page_template_id);

		// Set channel info for the new page.
		Integer channelSetId = page.getChannelSetId();

		page.setChannelInfo(channelId, channelSetId);

		page.save();
		t.commit(false);

		// Create a new velocity Tag
		Map<String, String> parts = new HashMap<String, String>();

		// Set all properties to false
		channel.setHttps(false);
		channel.setUtf8(false);
		channel.setEditorversion(Editor.LiveEditor.ordinal());
		channel.save();

		parts.put(PageRenderSandboxTest.HBS_PART_NAME, "{{gtx_render cms.node.https}}\n{{gtx_render cms.node.master.id}}\n{{gtx_render cms.node.utf8}}\n{{gtx_render cms.node.alohaeditor}}");

		ContentTag contentTag = createTag(page, hbsConstructId, "hbs", parts);
		page.save();

		// Reload  the page
		t.commit(false);
		page = t.getObject(Page.class, page.getId());

		// Render the page
		PageRenderResponse renderResponse = pageResource.render(
				page.getId().toString(), channelId, "<node hbs>", false, null, LinksType.backend, false, false, false, 0);

		// Check the results
		assertEquals("Check the rendered content", "false\n" + MASTER_ID + "\nfalse\nfalse", renderResponse.getContent());

		// Set all properties to true
		channel.setHttps(true);
		channel.setUtf8(true);
		channel.setEditorversion(Editor.AlohaEditor.ordinal());
		channel.save();

		contentTag.getValues().getByKeyname(PageRenderSandboxTest.HBS_PART_NAME).setValueText(
			"{{gtx_render cms.node.https}}\n{{gtx_render cms.node.master.id}}\n{{gtx_render cms.node.utf8}}\n{{gtx_render cms.node.alohaeditor}}"
		);
		contentTag.save();

		// Reload  the page
		t.commit(false);
		page = t.getObject(Page.class, page.getId());

		// Render the page
		renderResponse = pageResource.render(
				page.getId().toString(), channelId, "<node hbs>", false, null, LinksType.backend, false, false, false, 0);

		// Check the results
		assertEquals("Check the rendered content", "true\n" + MASTER_ID + "\ntrue\ntrue", renderResponse.getContent());
	}

	/**
	 * Create and save a tag in the page. The tag may be filled with string values
	 * 
	 * @param page page
	 * @param constructId construct id
	 * @param tagName tag name
	 * @param parts map of part keywords to values
	 */
	protected ContentTag createTag(Page page, Integer constructId, String tagName, Map<String, String> parts)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		ContentTag contentTag = t.createObject(ContentTag.class);

		contentTag.setEnabled(true);
		contentTag.setConstructId(constructId);
		contentTag.setName(tagName);
		contentTag.setContentId(page.getContent().getId());

		contentTag.save();

		ValueList valueList = contentTag.getValues();
		if (!ObjectTransformer.isEmpty(parts)) {
			for (Map.Entry<String, String> entry : parts.entrySet()) {
				Value value = valueList.getByKeyname(entry.getKey());
				if (value != null) {
					value.setValueText(entry.getValue());
					value.save();
				}
			}
		}

		contentTag.save();

		page.getTags().put(tagName, contentTag);

		return contentTag;
	}
}
