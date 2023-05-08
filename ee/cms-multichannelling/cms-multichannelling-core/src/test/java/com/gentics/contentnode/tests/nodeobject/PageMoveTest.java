package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Other test cases for moving pages
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.NICE_URLS })
public class PageMoveTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));
		operate(() -> clear(channel));
	}

	/**
	 * Test moving a local page from a local folder to an inherited folder
	 * @throws NodeException
	 */
	@Test
	public void testMoveFromLocalToInherited() throws NodeException {
		Folder source = supply(() -> createFolder(node.getFolder(), "Source", channel));
		Folder target = supply(() -> createFolder(node.getFolder(), "Target"));

		Page page = supply(() -> createPage(source, template, "Page", channel));

		OpResult result = supply(() -> page.move(target, channel.getId(), true));
		assertThat(result.isOK()).as("Moving succeeded").isTrue();
	}

	/**
	 * Test moving a local page from a local folder to an inherited folder with an obstructor in another local folder
	 * @throws NodeException
	 */
	@Test
	public void testMoveFromLocalToInheritedWithLocalObstructor() throws NodeException {
		Folder source = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Source");
			create.setChannelInfo(channel.getId(), create.getChannelSetId());
			create.setPublishDir("/source");
		}));
		Folder target = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Target");
			create.setPublishDir("/target");
		}));
		Folder obstructionFolder = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Obstructor");
			create.setChannelInfo(channel.getId(), create.getChannelSetId());
			create.setPublishDir("/target");
		}));
		supply(() -> create(Page.class, create -> {
			create.setFolderId(obstructionFolder.getId());
			create.setTemplateId(template.getId());
			create.setName("Page");
			create.setFilename("page.html");
			create.setChannelInfo(channel.getId(), create.getChannelSetId());
		}));

		Page page = supply(() -> createPage(source, template, "Page", channel));

		OpResult result = supply(() -> page.move(target, channel.getId(), true));
		assertThat(result.isOK()).as("Moving succeeded").isFalse();
	}

	/**
	 * Test moving a local page from a local folder to an inherited folder with an obstructor in another inherited folder
	 * @throws NodeException
	 */
	@Test
	public void testMoveFromLocalToInheritedWithInheritedObstructor() throws NodeException {
		Folder source = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Source");
			create.setChannelInfo(channel.getId(), create.getChannelSetId());
			create.setPublishDir("/source");
		}));
		Folder target = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Target");
			create.setPublishDir("/target");
		}));
		Folder obstructionFolder = supply(() -> create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Obstructor");
			create.setPublishDir("/target");
		}));
		supply(() -> create(Page.class, create -> {
			create.setFolderId(obstructionFolder.getId());
			create.setTemplateId(template.getId());
			create.setName("Page");
			create.setFilename("page.html");
		}));

		Page page = supply(() -> createPage(source, template, "Page", channel));

		OpResult result = supply(() -> page.move(target, channel.getId(), true));
		assertThat(result.isOK()).as("Moving succeeded").isFalse();
	}
}
