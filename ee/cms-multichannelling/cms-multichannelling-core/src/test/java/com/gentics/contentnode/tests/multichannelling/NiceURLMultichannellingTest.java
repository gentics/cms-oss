package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

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
		node = Trx.supply(() -> createNode());
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/"));

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
}
