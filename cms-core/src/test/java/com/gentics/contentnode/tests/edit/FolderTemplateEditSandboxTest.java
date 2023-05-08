package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * This class contains folder template related testcases. E.g. unlinking,
 * deleting of templates
 * 
 * @author johannes2
 * 
 */
public class FolderTemplateEditSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * This test will check if unlinking works for a template that is linked to
	 * subfolders but there are no pages that uses this template. It is expected
	 * that this will work.
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testUnlinkTemplateFromFolderWithMultipleTemplateLinksAndWithoutTemplatePages() throws Exception {

		/**
		 * FolderID for a folder that contains subfolder and one template that
		 * is linked to those subfolders. No page uses this template.
		 */
		final int FOLDER_WITH_LINKED_TEMPLATES_ID = 38;
		final int DUMMY_TEMPLATE_WITH_LINKED_FOLDERS_ID = 79;

		// Create a new transaction
		Transaction t = testContext.startTransactionWithPermissions(true);
		Folder folder = (Folder) t.getObject(Folder.class, FOLDER_WITH_LINKED_TEMPLATES_ID);

		folder.unlinkTemplate(DUMMY_TEMPLATE_WITH_LINKED_FOLDERS_ID);
		t.commit(false);

		ResultSet newTemplateFolderLinks = testContext.getDBSQLUtils().executeQuery(
				"SELECT * FROM template_folder WHERE template_id = " + DUMMY_TEMPLATE_WITH_LINKED_FOLDERS_ID + " and folder_id =" + FOLDER_WITH_LINKED_TEMPLATES_ID);

		assertEquals(
				"There should be only no template_folder link left for the template_id " + DUMMY_TEMPLATE_WITH_LINKED_FOLDERS_ID + " and folder_id "
				+ FOLDER_WITH_LINKED_TEMPLATES_ID,
				0,
				testContext.getDBSQLUtils().getSize(newTemplateFolderLinks));

	}

	/**
	 * This test will check if unlinking works for a template that has only one
	 * template page. It is expected that unlinking will fail because at this
	 * point unlinking is only allowed when there are no pages that reference
	 * this template.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnlinkTemplateFromFolderWithOneTemplatePage() throws Exception {

		/**
		 * FolderID for a folder that contains only one template and one page
		 * that uses this template
		 */
		final int FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_ONE_PAGE_ID = 37;
		final int DUMMY_TEMPLATE_WITH_ONE_PAGE_ID = 78;

		// Create a new transaction
		Transaction t = testContext.startTransactionWithPermissions(true);
		Folder folder = (Folder) t.getObject(Folder.class, FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_ONE_PAGE_ID);

		folder.unlinkTemplate(DUMMY_TEMPLATE_WITH_ONE_PAGE_ID);
		Collection<NodeMessage> messages = t.getRenderResult().getMessages();

		t.commit(false);

		// Check if the template folder link still exists
		ResultSet templateFolderLinks = testContext.getDBSQLUtils().executeQuery(
				"SELECT * FROM template_folder WHERE template_id = " + DUMMY_TEMPLATE_WITH_ONE_PAGE_ID + " and folder_id ="
				+ FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_ONE_PAGE_ID);

		assertEquals(
				"There should be only one template_folder link for the template_id " + DUMMY_TEMPLATE_WITH_ONE_PAGE_ID + " and folder_id "
				+ FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_ONE_PAGE_ID,
				1,
				testContext.getDBSQLUtils().getSize(templateFolderLinks));

		// Check for new messages
		int nNewMessages = messages.size();

		assertEquals("There should be exaclty one new message that indicates that the unlinking did not work.", 1, nNewMessages);
	}

	/**
	 * This test will check if unlinking works correct when we try to unlink a
	 * template from a folder that contains multiple templatepages. It is
	 * expected that unlinking will fail.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnlinkTemplateFromFolderWithMultipleTemplatePages() throws Exception {

		/**
		 * FolderID for a folder that contains only one template and some pages
		 * that uses this template
		 */
		final int FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_PAGES_ID = 36;
		final int DUMMY_TEMPLATE_WITH_PAGES_ID = 76;

		// Create a new transaction
		Transaction t = testContext.startTransactionWithPermissions(true);
		Folder folder = (Folder) t.getObject(Folder.class, FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_PAGES_ID);

		folder.unlinkTemplate(DUMMY_TEMPLATE_WITH_PAGES_ID);
		Collection<NodeMessage> messages = t.getRenderResult().getMessages();

		t.commit(false);

		// Check if the template folder link still exists
		ResultSet templateFolderLinks = testContext.getDBSQLUtils().executeQuery(
				"SELECT * FROM template_folder WHERE template_id = " + DUMMY_TEMPLATE_WITH_PAGES_ID + " and folder_id ="
				+ FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_PAGES_ID);

		assertEquals(
				"There should be only one template_folder link for the template_id " + DUMMY_TEMPLATE_WITH_PAGES_ID + " and folder_id "
				+ FOLDER_WITH_ONLY_ONE_LINKED_TEMPLATE_AND_PAGES_ID,
				1,
				testContext.getDBSQLUtils().getSize(templateFolderLinks));

		// Check for new messages
		int nNewMessages = messages.size();

		assertEquals("There should be exaclty one new message that indicates that the unlinking did not work.", 1, nNewMessages);
	}

	/**
	 * Test unlinking of a template from the last folder. The folder from which
	 * the template is currently linked is the only linked folder. There are no
	 * pages that uses this template. It is expected that unlinking works.
	 */
	@Test
	public void testUnlinkTemplateFromLastFolder() throws Exception {

		/**
		 * FolderID for a folder that contains only one linked template and no
		 * pages
		 */
		final int FOLDER_WITH_ONE_LINKED_TEMPLATE_ID = 35;
		final int DUMMY_TEMPLATE_WITHOUT_PAGES_ID = 77;
		// Create a new transaction
		Transaction t = testContext.startTransactionWithPermissions(true);
		Folder folder = (Folder) t.getObject(Folder.class, FOLDER_WITH_ONE_LINKED_TEMPLATE_ID);

		folder.unlinkTemplate(DUMMY_TEMPLATE_WITHOUT_PAGES_ID);
		t.commit(false);

		ResultSet templateFolderLinks = testContext.getDBSQLUtils().executeQuery(
				"SELECT * FROM template_folder WHERE template_id = " + DUMMY_TEMPLATE_WITHOUT_PAGES_ID + " and folder_id =" + FOLDER_WITH_ONE_LINKED_TEMPLATE_ID);

		assertEquals(
				"There should be no more template_folder link for the template_id " + DUMMY_TEMPLATE_WITHOUT_PAGES_ID + " and folder_id "
				+ FOLDER_WITH_ONE_LINKED_TEMPLATE_ID,
				0,
				testContext.getDBSQLUtils().getSize(templateFolderLinks));

	}
}
