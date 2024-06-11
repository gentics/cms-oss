package com.gentics.contentnode.tests.deleteobject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for deleting folder (structures)
 */
public class FolderDeleteTest {
	@Rule
	public DBTestContext testContext = new DBTestContext(true);

	/**
	 * Test deleting a folder structure, where a template is only linked to the subfolder, whereas the only page using the template
	 * is in the folder
	 * @throws Exception
	 */
	@Test
	public void testTemplateInSubfolderPageInFolder() throws Exception {
		Node testNode = null;
		Folder folder = null;
		Folder subFolder = null;
		Template template = null;
		Page page = null;

		// create the test data
		try (Trx trx = new Trx(null, 1)) {
			testNode = ContentNodeTestDataUtils.createNode();
			folder = ContentNodeTestDataUtils.createFolder(testNode.getFolder(), "Folder");
			subFolder = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
			template = ContentNodeTestDataUtils.createTemplate(subFolder, "Template");
			page = ContentNodeTestDataUtils.createPage(folder, template, "Page");
			trx.success();
		}

		// delete the folder
		try (Trx trx = new Trx(null, 1)) {
			FolderResource res = ContentNodeRESTUtils.getFolderResource();
			GenericResponse response = res.delete(folder.getId().toString(), null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
		}

		// check whether everything was deleted
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			assertNull(folder + " should have been deleted", t.getObject(Folder.class, folder.getId()));
			assertNull(subFolder + " should have been deleted", t.getObject(Folder.class, subFolder.getId()));
			assertNull(page + " should have been deleted", t.getObject(Page.class, page.getId()));
			assertNull(template + " should have been deleted", t.getObject(Template.class, template.getId()));
			trx.success();
		}
	}

	/**
	 * Test deleting a folder structure with a template. that is used in (but not linked to) another folder
	 * @throws Exception
	 */
	@Test
	public void testTemplateUsedInOtherFolder() throws Exception {
		Node testNode = null;
		Folder folder = null;
		Folder subFolder = null;
		Folder otherFolder = null;
		Template template = null;
		Page page = null;
		Page otherPage = null;

		// create the test data
		try (Trx trx = new Trx(null, 1)) {
			testNode = ContentNodeTestDataUtils.createNode();
			folder = ContentNodeTestDataUtils.createFolder(testNode.getFolder(), "Folder");
			subFolder = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
			otherFolder = ContentNodeTestDataUtils.createFolder(testNode.getFolder(), "Other Folder");
			template = ContentNodeTestDataUtils.createTemplate(subFolder, "Template");
			page = ContentNodeTestDataUtils.createPage(folder, template, "Page");
			otherPage = ContentNodeTestDataUtils.createPage(otherFolder, template, "Other Page");
			trx.success();
		}

		// delete the folder
		try (Trx trx = new Trx(null, 1)) {
			FolderResource res = ContentNodeRESTUtils.getFolderResource();
			GenericResponse response = res.delete(folder.getId().toString(), null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
		}

		// check whether everything was deleted as expected
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			assertNull(folder + " should have been deleted", t.getObject(Folder.class, folder.getId()));
			assertNull(subFolder + " should have been deleted", t.getObject(Folder.class, subFolder.getId()));
			assertNotNull(otherFolder + " should not have been deleted", t.getObject(Folder.class, otherFolder.getId()));
			assertNull(page + " should have been deleted", t.getObject(Page.class, page.getId()));
			assertNotNull(otherPage + " should not have been deleted", t.getObject(Page.class, otherPage.getId()));
			assertNotNull(template + " should not have been deleted", t.getObject(Template.class, template.getId()));
			trx.success();
		}
	}

	/**
	 * Test deleting a folder structure with a template, that is also linked to another folder
	 * @throws Exception
	 */
	@Test
	public void testTemplateLinkedToOtherFolder() throws Exception {
		Node testNode = null;
		Folder folder = null;
		Folder subFolder = null;
		Folder otherFolder = null;
		Template template = null;
		Page page = null;

		// create the test data
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();

			testNode = ContentNodeTestDataUtils.createNode();
			folder = ContentNodeTestDataUtils.createFolder(testNode.getFolder(), "Folder");
			subFolder = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
			otherFolder = ContentNodeTestDataUtils.createFolder(testNode.getFolder(), "Other Folder");
			template = ContentNodeTestDataUtils.createTemplate(subFolder, "Template");
			template = t.getObject(Template.class, template.getId(), true);
			template.getFolders().add(otherFolder);
			template.save();
			template = t.getObject(Template.class, template.getId());
			page = ContentNodeTestDataUtils.createPage(folder, template, "Page");
			trx.success();
		}

		// delete the folder
		try (Trx trx = new Trx(null, 1)) {
			FolderResource res = ContentNodeRESTUtils.getFolderResource();
			GenericResponse response = res.delete(folder.getId().toString(), null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
		}

		// check whether everything was deleted as expected
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			assertNull(folder + " should have been deleted", t.getObject(Folder.class, folder.getId()));
			assertNull(subFolder + " should have been deleted", t.getObject(Folder.class, subFolder.getId()));
			assertNotNull(otherFolder + " should not have been deleted", t.getObject(Folder.class, otherFolder.getId()));
			assertNull(page + " should have been deleted", t.getObject(Page.class, page.getId()));
			assertNotNull(template + " should not have been deleted", t.getObject(Template.class, template.getId()));
			template = t.getObject(Template.class, template.getId());
			assertFalse(template + " should be linked to at least one folder", template.getFolders().isEmpty());
			trx.success();
		}
	}

	/**
	 * Test moving a folder with a template only linked to this folder to the wastebin.
	 * The template must not be deleted
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMoveFolderWithTemplateToWastebin() throws Exception {
		try (FeatureClosure featureClosure = new FeatureClosure(Feature.WASTEBIN, true)) {
			Folder folder = null;
			Template template = null;
	
			try (Trx trx = new Trx(null, 1)) {				
				Node node = ContentNodeTestDataUtils.createNode();
				folder    = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
				template  = ContentNodeTestDataUtils.createTemplate(folder, "Template");
	
				folder.delete();
				trx.success();
			}
	
			try (Trx trx = new Trx(null, 1); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
				Transaction t = trx.getTransaction();
				assertNotNull("Folder must still exist in wastebin", t.getObject(folder));
				assertNotNull("Template must still exist", t.getObject(template));
			}
		}
	}
}
