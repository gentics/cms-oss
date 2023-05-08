package com.gentics.contentnode.tests.dirting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Test cases for attribute specific dirting
 */
@GCNFeature(set = { Feature.ATTRIBUTE_DIRTING })
public class AttributeDirtingTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Test
	public void testDirtFolderName() throws Exception {
		Node node = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder name
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setName("Changed Folder");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").containsKey(folder.getId());
			assertThat(dirted.get(folder.getId())).as("Attributes dirted for " + folder).containsOnly("name");
		}
	}

	@Test
	@GCNFeature(unset = { Feature.ATTRIBUTE_DIRTING })
	public void testDirtFolderNameWithoutFeature() throws Exception {
		Node node = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder name
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setName("Changed Folder");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").containsKey(folder.getId());
			assertThat(dirted.get(folder.getId())).as("Attributes dirted for " + folder).isNull();
		}
	}

	@Test
	public void testDirtFolderNameAndDescription() throws Exception {
		Node node = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG,
					"folder.description", "description", null, false, false, false, 0, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			folder = trx.getTransaction().getObject(folder, true);
			folder.setDescription("Description");
			folder.save();
			folder = trx.getTransaction().getObject(folder);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder name and description
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setName("Changed Folder");
			folder.setDescription("Changed Description");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").containsKey(folder.getId());
			assertThat(dirted.get(folder.getId())).as("Attributes dirted for " + folder).containsOnly("name", "description");
		}
	}

	@Test
	public void testDirtFolderIndividualChanges() throws Exception {
		Node node = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG,
					"folder.description", "description", null, false, false, false, 0, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			folder = trx.getTransaction().getObject(folder, true);
			folder.setDescription("Description");
			folder.save();
			folder = trx.getTransaction().getObject(folder);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder name
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setName("Changed Folder");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").containsKey(folder.getId());
			assertThat(dirted.get(folder.getId())).as("Attributes dirted for " + folder).containsOnly("name");
		}

		// change the folder description
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setDescription("Changed Description");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").containsKey(folder.getId());
			assertThat(dirted.get(folder.getId())).as("Attributes dirted for " + folder).containsOnly("name", "description");
		}
	}

	@Test
	public void testPageDependency() throws Exception {
		Node node = null;
		Folder folder = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			node = t.getObject(node, true);
			node.setUrlRenderWayPages(UrlRenderWay.STATIC_DYNAMIC.getValue());
			node.save();
			node = t.getObject(node);

			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.url", "url",
					null, false, false, false, 0, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			page = ContentNodeTestDataUtils.createTemplateAndPage(folder, "Testpage");
			t.getObject(page, true).publish();
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder pub_dir
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setPublishDir("/changed");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").isEmpty();

			dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, node);
			assertThat(dirted).as("Dirted pages").containsKey(page.getId());
			assertThat(dirted.get(page.getId())).as("Attributes dirted for " + page).containsOnly("url", FolderFactory.DUMMY_DIRT_ATTRIBUTE);
		}
	}

	@Test
	public void testPageDependencyAndRepublish() throws Exception {
		Node node = null;
		Folder folder = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			node = t.getObject(node, true);
			node.setUrlRenderWayPages(UrlRenderWay.STATIC_DYNAMIC.getValue());
			node.save();
			node = t.getObject(node);

			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.url", "url",
					null, false, false, false, 0, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			page = ContentNodeTestDataUtils.createTemplateAndPage(folder, "Testpage");
			t.getObject(page, true).publish();
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the folder pub_dir
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setPublishDir("/changed");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").isEmpty();

			dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, node);
			assertThat(dirted).as("Dirted pages").containsKey(page.getId());
			assertThat(dirted.get(page.getId())).as("Attributes dirted for " + page).containsOnly("url", FolderFactory.DUMMY_DIRT_ATTRIBUTE);
		}

		// republish the page
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			page = t.getObject(page, true);
			page.publish();
			page = t.getObject(page);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirted).as("Dirted folders").isEmpty();

			dirted = PublishQueue.getDirtedObjectIdsWithAttributes(Page.class, false, node);
			assertThat(dirted).as("Dirted pages").containsKey(page.getId());
			assertThat(dirted.get(page.getId())).as("Attributes dirted for " + page).isNull();
		}
	}

	@Test
	public void testDirtFileBinary() throws Exception {
		Node node = null;
		Folder folder = null;
		File file = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentRepository cr = node.getContentRepository();
			ContentNodeTestDataUtils.addTagmapEntry(cr, File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_INTEGER, "file.size", "size", null, false, false,
					false, 0, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			file = ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Test file contents".getBytes());
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change file binaries
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			file = t.getObject(file, true);
			file.setFileStream(new ByteArrayInputStream("Modified test file contents".getBytes()));
			file.save();
			file = t.getObject(file);
			trx.success();
		}

		// wait for dirting and assert
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			Map<Integer, Set<String>> dirted = PublishQueue.getDirtedObjectIdsWithAttributes(File.class, false, node);
			assertThat(dirted).as("Dirted files").containsKey(file.getId());
			assertThat(dirted.get(file.getId())).as("Attributes dirted for " + file).containsOnly("binarycontent", "size");
		}
	}

	@Test
	public void testDirtAttributeAndWholeObject() throws Exception {
		Node node = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode();
			trx.success();
		}

		// dirt attribute
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(node.getFolder(), Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// dirt folder as a whole
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(node.getFolder(), Action.DEPENDENCY, node.getId());
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			Map<Integer, Set<String>> dirtedFolders = PublishQueue.getDirtedObjectIdsWithAttributes(Folder.class, false, node);
			assertThat(dirtedFolders).as("Dirted folders").containsEntry(node.getFolder().getId(), null);
			trx.success();
		}
	}
}
