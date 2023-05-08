package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.addTagmapEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Test cases for attribute dirting in multichannelling environments
 */
@GCNFeature(set = { Feature.ATTRIBUTE_DIRTING, Feature.MULTICHANNELLING })
public class MCAttributeDirtingTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Test
	public void testChannelDependency() throws Exception {
		Node node = null;
		Node channel = null;
		Folder folderMaster = null;
		Folder folderLocal = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			node = ContentNodeTestDataUtils.createNode("testhost", "Test Node", PublishTarget.CONTENTREPOSITORY);
			node = update(node, n -> {
				n.setUrlRenderWayPages(UrlRenderWay.STATIC_DYNAMIC.getValue());
			});
			update(node.getContentRepository(), cr -> {
				cr.getEntries().add(create(TagmapEntry.class, entry -> {
					entry.setObject(Page.TYPE_PAGE);
					entry.setAttributeTypeId(GenticsContentAttribute.ATTR_TYPE_TEXT);
					entry.setTagname("page.url");
					entry.setMapname("url");
				}, false));
			});

			channel = ContentNodeTestDataUtils.createChannel(node, "Test Channel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
			channel = update(channel, c -> {
				c.setUrlRenderWayPages(UrlRenderWay.STATIC_DYNAMIC.getValue());
			});
			update(channel.getContentRepository(), cr -> {
				cr.getEntries().add(create(TagmapEntry.class, entry -> {
					entry.setObject(Page.TYPE_PAGE);
					entry.setAttributeTypeId(GenticsContentAttribute.ATTR_TYPE_TEXT);
					entry.setTagname("page.url");
					entry.setMapname("url");
				}, false));
			});

			folderMaster = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			folderLocal = ContentNodeTestDataUtils.localize(folderMaster, channel);

			page = ContentNodeTestDataUtils.createTemplateAndPage(folderMaster, "Page");
			t.getObject(page, true).publish();

			trx.success();
		}

		// run publish process (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change pub_dir of the local folder
		update(folderLocal, folder -> {
			folder.setPublishDir("/test");
		});

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			assertThat(PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, node)).as("Pages dirted in Node").isEmpty();
			assertThat(PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, channel)).as("Pages dirted in Channel").containsOnlyKeys(
					page.getId()).containsEntry(page.getId(), new HashSet<>(Arrays.asList("url", FolderFactory.DUMMY_DIRT_ATTRIBUTE)));
			trx.success();
		}
	}

	/**
	 * Test dependency handling when master node publishes to a CR and the channel does not
	 * @throws Exception
	 */
	@Test
	@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUB_DIR_SEGMENT })
	public void testDependencyChannelNoCr() throws Exception {
		// create node which publishes into a CR and uses publish directory segments
		Node node = supply(() -> update(createNode("master", "master", PublishTarget.CONTENTREPOSITORY), n -> {
			n.setPubDirSegment(true);
		}));
		// create channel which does not publish into the CR
		supply(() -> update(createChannel(node, "channel", "channel", "/", PublishTarget.NONE), n -> {
			n.setPubDirSegment(true);
		}));

		// create tagmap entry for folder path for pages
		operate(() -> {
			// first remove all other tagmap entries (not node_id)
			update(node.getContentRepository(), upd -> {
				upd.getEntries().removeIf(entry -> !StringUtils.equals(entry.getMapname(), "node_id"));
			});

			addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT,
					"page.folder.path", "folderpath", null, false, false, false, -1, null, null);
		});

		// create template
		Template template = supply(() -> createTemplate(node.getFolder(), "Template"));

		// create folder and subfolder
		Folder folder = supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			f.setPublishDir("folder-pubdir");
		}));
		Folder subFolder = supply(() -> update(createFolder(folder, "Subfolder"), f -> {
			f.setPublishDir("subfolder-pubdir");
		}));

		// create page in the subfolder
		Page page = supply(() -> update(createPage(subFolder, template, "Page"), Page::publish));

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert that the page was published with the attribute "url"
		assertPublishCR(page, node, true, res -> {
			assertThat(res.get("folderpath")).as("Attribute folderpath").isEqualTo("/home/folder-pubdir/subfolder-pubdir/");
		});

		// assert the dependencies
		operate(() -> {
			assertThat(page).as("Published page").dependsOn("folderpath", folder, "pub_dir", node.getId()).dependsOn("folderpath", subFolder, "pub_dir", node.getId());
		});
	}
}
