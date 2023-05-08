package com.gentics.contentnode.tests.versioning;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.testutils.DBTestContext;

public class AbstractPageVersioningTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Name of the contenttag, every page will have
	 */
	public final static String CONTENTTAG_NAME = "html";

	/**
	 * Id of the template for using in the versions tests
	 */
	public final static int VERSIONS_TEMPLATE_ID = 80;

	/**
	 * Id of the folder for using in the versions tests
	 */
	public final static int VERSIONS_FOLDER_ID = 42;

	/**
	 * Name of the part of the tag
	 */
	public final static String PART_NAME = "html";

	/**
	 * Create a new page with given name, filename and content
	 * 
	 * @param templateId
	 *            id of the template
	 * @param folderId
	 *            id of the folder
	 * @param name
	 *            name of the Page
	 * @param filename
	 *            filename of the Page
	 * @param content
	 *            content of the Page
	 * @return the page
	 * @throws Exception
	 */
	protected Page createPage(int templateId, int folderId, String name, String filename, String content) throws Exception {
		// get the transaction
		Transaction t = TransactionManager.getCurrentTransaction();
		// create a new page
		Page page = (Page) t.createObject(Page.class);

		// set the meta data
		page.setTemplateId(templateId);
		page.setFolderId(folderId);
		page.setName(name);
		page.setFilename(filename);

		// set the content
		ContentTag contentTag = page.getContentTag(CONTENTTAG_NAME);

		assertNotNull("Check whether the contenttag exists", contentTag);
		Value value = (Value) contentTag.getValues().get(PART_NAME);

		assertNotNull("Check whether the part exists", value);
		value.setValueText(content);
		contentTag.setEnabled(true);

		// save the page
		page.save();

		// commit the transaction (but leave it open)
		t.commit(false);

		// this is a mega hack: we set the publisher of the page and the
		// page_nodeversion entry for the page, ín order to prevent the creation
		// of another page version when publishing the page via PHP.
		// if the PHP process would create a new page version, the timestamps of
		// the version would get mixed up, because PHP runs on a different
		// server and it would use the date of the other server
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE page SET publisher = " + t.getUserId() + " WHERE id = " + page.getId());
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE page_nodeversion SET publisher = " + t.getUserId() + " WHERE id = " + page.getId());

		return page;
	}
}
