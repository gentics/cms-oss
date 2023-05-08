package com.gentics.contentnode.tests.nodecopy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling references in node copy
 */
@GCNFeature(set = { Feature.WASTEBIN })
public class NodeCopyReferenceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		Integer pageUrlConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, PageURLPartType.class, "page", "url"));
		Trx.supply(() -> ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, pageUrlConstructId, "Page", "page"));
		Trx.supply(() -> ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, pageUrlConstructId, "Page", "page"));

		Integer fileUrlConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, FileURLPartType.class, "file", "url"));
		Trx.supply(() -> ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, fileUrlConstructId, "File", "file"));
		Trx.supply(() -> ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, fileUrlConstructId, "File", "file"));
	}

	/**
	 * Test copying folder that references a page in the wastebin
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testReferenceToPageInWastebin() throws NodeException {
		// create test data
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(folder, "Page"));

		// let the folder reference the page
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder editableFolder = t.getObject(folder, true);
			ObjectTag objectTag = editableFolder.getObjectTag("page");
			assertThat(objectTag).isNotNull();
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, objectTag, "url").setTargetPage(page);
			editableFolder.save();
		});

		// put page in wastebin
		Trx.operate(() -> page.delete());

		// copy the node
		Node copyOfNode = Trx.supply(() -> ContentNodeTestDataUtils.copy(node, true, true, true));

		// get copy of folder
		List<Folder> copiedFolders = Trx.supply(() -> copyOfNode.getFolder().getChildFolders());
		assertThat(copiedFolders).as("Copied folders").hasSize(1);
		Folder copyOfFolder = copiedFolders.get(0);

		assertThat(
				Trx.supply(() -> ContentNodeTestDataUtils.getPartType(PageURLPartType.class, copyOfFolder.getObjectTag("page"), "url").getValueObject()
						.getValueRef())).as("Page Reference").isEqualTo(0);
	}

	/**
	 * Test copying folder that references a file in the wastebin
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testReferenceToFileInWastebin() throws NodeException {
		// create test data
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folder, "file.txt", "File contents".getBytes()));

		// let the folder reference the file
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder editableFolder = t.getObject(folder, true);
			ObjectTag objectTag = editableFolder.getObjectTag("file");
			assertThat(objectTag).isNotNull();
			ContentNodeTestDataUtils.getPartType(FileURLPartType.class, objectTag, "url").setTargetFile(file);
			editableFolder.save();
		});

		// put file in wastebin
		Trx.operate(() -> file.delete());

		// copy the node
		Node copyOfNode = Trx.supply(() -> ContentNodeTestDataUtils.copy(node, true, true, true));

		// get copy of folder
		List<Folder> copiedFolders = Trx.supply(() -> copyOfNode.getFolder().getChildFolders());
		assertThat(copiedFolders).as("Copied folders").hasSize(1);
		Folder copyOfFolder = copiedFolders.get(0);

		assertThat(
				Trx.supply(() -> ContentNodeTestDataUtils.getPartType(FileURLPartType.class, copyOfFolder.getObjectTag("file"), "url").getValueObject()
						.getValueRef())).as("File Reference").isEqualTo(0);
	}

	/**
	 * Test copying pages that reference each other
	 * @throws NodeException
	 */
	@Test
	public void testCircularPageReference() throws NodeException {
		// create test data
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		Page page1 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(folder, "Page1"));
		Page page2 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(folder, "Page2"));

		// let the pages reference each other
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page editablePage1 = t.getObject(page1, true);
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, editablePage1.getObjectTag("page"), "url").setTargetPage(page2);
			editablePage1.save();

			Page editablePage2 = t.getObject(page2, true);
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, editablePage2.getObjectTag("page"), "url").setTargetPage(page1);
			editablePage2.save();
		});

		// copy the node
		Node copyOfNode = Trx.supply(() -> ContentNodeTestDataUtils.copy(node, true, true, true));

		// get copy of folder
		List<Folder> copiedFolders = Trx.supply(() -> copyOfNode.getFolder().getChildFolders());
		assertThat(copiedFolders).as("Copied folders").hasSize(1);
		Folder copyOfFolder = copiedFolders.get(0);

		// get copies of pages
		List<Page> copiedPages = Trx.supply(() -> copyOfFolder.getPages());
		assertThat(copiedPages).as("Copied pages").hasSize(2);
		Page copy1 = copiedPages.get(0);
		Page copy2 = copiedPages.get(1);

		assertThat(Trx.supply(() -> ContentNodeTestDataUtils.getPartType(PageURLPartType.class, copy1.getObjectTag("page"), "url").getTargetPage())).isEqualTo(
				copy2);
		assertThat(Trx.supply(() -> ContentNodeTestDataUtils.getPartType(PageURLPartType.class, copy2.getObjectTag("page"), "url").getTargetPage())).isEqualTo(
				copy1);
	}

	/**
	 * Test copying a folder that references a page, but pages are not copied
	 * @throws NodeException
	 */
	@Test
	public void testFolderReferencePageNoCopy() throws NodeException {
		// create test data
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(folder, "Page"));

		// let the folder reference the page
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder editableFolder = t.getObject(folder, true);
			ObjectTag objectTag = editableFolder.getObjectTag("page");
			assertThat(objectTag).isNotNull();
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, objectTag, "url").setTargetPage(page);
			editableFolder.save();
		});

		// copy the node (without copying pages)
		Node copyOfNode = Trx.supply(() -> ContentNodeTestDataUtils.copy(node, false, true, true));

		// get copy of folder
		List<Folder> copiedFolders = Trx.supply(() -> copyOfNode.getFolder().getChildFolders());
		assertThat(copiedFolders).as("Copied folders").hasSize(1);
		Folder copyOfFolder = copiedFolders.get(0);

		assertThat(Trx.supply(() -> ContentNodeTestDataUtils.getPartType(PageURLPartType.class, copyOfFolder.getObjectTag("page"), "url").getTargetPage())).as(
				"Page Reference").isEqualTo(page);
	}

	/**
	 * Test copying a folder that references a file, but files are not copied
	 * @throws NodeException
	 */
	@Test
	public void testFolderReferenceFileNoCopy() throws NodeException {
		// create test data
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folder, "file.txt", "File contents".getBytes()));

		// let the folder reference the file
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder editableFolder = t.getObject(folder, true);
			ObjectTag objectTag = editableFolder.getObjectTag("file");
			assertThat(objectTag).isNotNull();
			ContentNodeTestDataUtils.getPartType(FileURLPartType.class, objectTag, "url").setTargetFile(file);
			editableFolder.save();
		});

		// copy the node (without copying files)
		Node copyOfNode = Trx.supply(() -> ContentNodeTestDataUtils.copy(node, true, true, false));

		// get copy of folder
		List<Folder> copiedFolders = Trx.supply(() -> copyOfNode.getFolder().getChildFolders());
		assertThat(copiedFolders).as("Copied folders").hasSize(1);
		Folder copyOfFolder = copiedFolders.get(0);

		assertThat(Trx.supply(() -> ContentNodeTestDataUtils.getPartType(FileURLPartType.class, copyOfFolder.getObjectTag("file"), "url").getTargetFile())).as(
				"File Reference").isEqualTo(file);
	}
}
