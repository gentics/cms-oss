package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for nice URLs in multichannelling environments
 */
@GCNFeature(set = { Feature.NICE_URLS, Feature.MULTICHANNELLING })
public class NiceURLMultichannellingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;

	/**
	 * Setup common test data
	 * @throws NodeException 
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/channel"));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Clean nodes
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			for (Node n : Arrays.asList(node, channel)) {
				for (Folder f : n.getFolder().getChildFolders()) {
					f.delete(true);
				}
			}
		});
	}

	/**
	 * Test copying a page into a folder, that is excluded from inheritance
	 * @throws Exception
	 */
	@Test
	public void testCopyPageToExcludedFolder() throws Exception {
		Folder sourceFolder = Trx.supply(() -> createFolder(node.getFolder(), "Source"));
		Folder targetFolder = Trx.supply(() -> createFolder(node.getFolder(), "Target"));

		targetFolder = Trx.execute(f -> update(f, update -> {
			update.changeMultichannellingRestrictions(true, Collections.emptySet(), true);
		}), targetFolder);

		Page page = Trx.supply(() -> createPage(sourceFolder, template, "Page"));

		try (Trx trx = new Trx()) {
			PageResource resource = ContentNodeRESTUtils.getPageResource();
			PageCopyRequest copy = new PageCopyRequest();
			copy.setCreateCopy(true);
			copy.setNodeId(node.getId());
			copy.setSourcePageIds(Arrays.asList(page.getId()));
			copy.setTargetFolders(Arrays.asList(new TargetFolder(targetFolder.getId(), node.getId())));

			PageCopyResponse response = resource.copy(copy, -1);
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
		}
	}

	/**
	 * Test that the uniqueness check between the URL of a local page and an inherited nice URL correctly considers the node/channel pubDir
	 * @throws Exception
	 */
	@Test
	public void testUniquenessLocalURLWithInheritedNiceURL() throws Exception {
		// create a local page with URL /channel/page
		create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(channel, channel.getFolder());
			p.setChannelInfo(channel.getId(), null);
			p.setName("Local Page");
			p.setFilename("page");
		}).save().build();

		// try to create an inherited page with nice URL /page (would be /channel/page in the Channel)
		Page masterPage = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(node, node.getFolder());
			p.setName("Master Page");
			p.setFilename("masterpage");
			p.setNiceUrl("/page");
		}).save().build();

		// nice URL should have been made unique
		assertThat(masterPage).as("Master Page").hasFieldOrPropertyWithValue("niceUrl", "/page1");
	}

	/**
	 * Test that the uniqueness check between a local nice URL and an inherited URL correctly considers the node/channel pubDir
	 * @throws Exception
	 */
	@Test
	public void testUniquenessLocalNiceURLWithInheritedURL() throws Exception {
		// create a local page with nice URL /page (full URL will be /channel/page)
		create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(channel, channel.getFolder());
			p.setChannelInfo(channel.getId(), null);
			p.setName("Local Page");
			p.setFilename("localpage");
			p.setNiceUrl("/page");
		}).save().build();

		// create an inherited page with filename "page" (URL in the channel would be /channel/page)
		Page masterPage = create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolder(node, node.getFolder());
			p.setName("Master Page");
			p.setFilename("page");
		}).save().build();

		// filename should have been made unique
		assertThat(masterPage).as("Master Page").hasFieldOrPropertyWithValue("filename", "page1");
	}
}
