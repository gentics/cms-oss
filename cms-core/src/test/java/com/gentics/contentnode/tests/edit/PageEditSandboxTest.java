/*
 * @author norbert
 * @date Dec 16, 2009
 * @version $Id: PageEditTest.java,v 1.6 2010-08-27 08:36:21 norbert Exp $
 */
package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.rest.model.request.MultiPagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test case for testing editing of pages
 * 
 * @author norbert
 */
public class PageEditSandboxTest {
	
	private static int timestamp = (int) (System.currentTimeMillis() / 1000);

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * id of the page which is edited
	 */
	public final static int PAGE_ID = 3;

	/**
	 * id of the template for creation of new pages
	 */
	public final static int TEMPLATE_ID = 70;

	/**
	 * Folder id for creation of new pages
	 */
	public final static int FOLDER_ID = 7;

	/**
	 * id of the content language
	 */
	public final static int CONTENTLANGUAGE_ID = 1;

	/**
	 * Number of threads for {@link #testThreadedPageCreation()}
	 */
	public final static int NUM_THREADS = 5;

	/**
	 * Number of pages per thread for {@link #testThreadedPageCreation()}
	 */
	public final static int NUM_PAGES_PER_THREAD = 10;

	/**
	 * Test editing meta data of a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEditingMetaData() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		String newName = "<p>The <strong class=\"superstrong\">edited</strong><br /> Name</p>";
		// tags will automatically be stripped from page names when saving, so we test for that as well
		String newExpectedName = "The edited Name";
		String newFileName = "editedfilename.html";
		String newDescription = "The edited description";
		int newLanguageId = 1;
		int newPriority = 99;

		// get the transaction timestamp (which will be used as edate)
		int transactionTimestamp = t.getUnixTimestamp();

		// get the page for editing
		Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

		// change some metadata
		page.setName(newName);
		page.setFilename(newFileName);
		page.setDescription(newDescription);
		page.setLanguageId(newLanguageId);
		page.setPriority(newPriority);

		// save the page
		page.save();

		// commit the transaction
		t.commit(false);

		// now read the page again and check whether it has the modified meta data
		Page readPage = (Page) t.getObject(Page.class, PAGE_ID);

		assertEquals("Check name of page", newExpectedName, readPage.getName());
		assertEquals("Check filename of page", newFileName, readPage.getFilename());
		assertEquals("Check description of page", newDescription, readPage.getDescription());
		assertEquals("Check language id of page", newLanguageId, readPage.getLanguageId().intValue());
		assertEquals("Check priority of page", newPriority, readPage.getPriority());

		// check whether the edate and editor have been set
		assertEquals("Check editor id of the page", testContext.USER_WITH_PERMS, readPage.getEditor().getId());
		assertEquals("Check edate of the page", transactionTimestamp, readPage.getEDate().getIntTimestamp());

		// also check the data directly in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM page WHERE id = " + PAGE_ID);

		if (res.next()) {
			assertEquals("Check name of page", newExpectedName, res.getString("name"));
			assertEquals("Check filename of page", newFileName, res.getString("filename"));
			assertEquals("Check description of page", newDescription, res.getString("description"));
			assertEquals("Check language id of page", newLanguageId, res.getInt("contentgroup_id"));
			assertEquals("Check priority of page", newPriority, res.getInt("priority"));
		} else {
			fail("Did not find the page in the database");
		}
	}

	/**
	 * Test whether the page caches behave like expected while editing a page
	 * 
	 * @throws Exception
	 */
	public void testPageCaches() throws Exception {
		String newName = "This is the new page name";

		// start a first transaction
		Transaction startFirstFetchLater = testContext.startTransactionWithPermissions(true);
		Transaction preEdit = testContext.startTransactionWithPermissions(false);
		// get the original page
		Page preEditPage = (Page) preEdit.getObject(Page.class, PAGE_ID);
		String oldName = preEditPage.getName();

		// start a new transaction for editing
		Transaction edit = testContext.startTransactionWithPermissions(false);
		// get the page for editing
		Page editPage = (Page) edit.getObject(Page.class, PAGE_ID, true);

		// now start a concurrent transaction
		Transaction concurrent = testContext.startTransactionWithPermissions(false);
		Page concurrentPage = (Page) concurrent.getObject(Page.class, PAGE_ID);

		assertEquals("Check page name before editing", oldName, concurrentPage.getName());

		// now edit the page
		editPage.setName(newName);

		// check names in other transactions
		assertEquals("Check page name for preedit after editing", oldName, ((Page) preEdit.getObject(Page.class, PAGE_ID)).getName());
		assertEquals("Check page name for concurrent after editing", oldName, ((Page) concurrent.getObject(Page.class, PAGE_ID)).getName());

		// save the page
		TransactionManager.setCurrentTransaction(edit);
		editPage.save();

		// check names in other transactions
		assertEquals("Check page name for preedit after saving", oldName, ((Page) preEdit.getObject(Page.class, PAGE_ID)).getName());
		assertEquals("Check page name for concurrent after saving", oldName, ((Page) concurrent.getObject(Page.class, PAGE_ID)).getName());

		// commit the edit transaction
		edit.commit();

		// start a final transaction
		Transaction postEdit = testContext.startTransactionWithPermissions(false);

		// check names in other transactions
		assertEquals("Check page name for preedit after commit", oldName, ((Page) preEdit.getObject(Page.class, PAGE_ID)).getName());
		assertEquals("Check page name for concurrent after commit", oldName, ((Page) concurrent.getObject(Page.class, PAGE_ID)).getName());
		assertEquals("Check page name for postedit after commit", newName, ((Page) postEdit.getObject(Page.class, PAGE_ID)).getName());
		assertEquals("Check page name for startFirstFetchLater after commit", newName, ((Page) startFirstFetchLater.getObject(Page.class, PAGE_ID)).getName());

		startFirstFetchLater.commit();
		preEdit.commit();
		concurrent.commit();
		postEdit.commit();
	}

	/**
	 * Test correct cache dirting behaviour with parallel running transactions
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCacheDirting() throws Exception {
		String newName = "This is the new page name";

		// start a transaction
		Transaction edit = testContext.startTransactionWithPermissions(false);

		// read page (putting it into cache)
		Page editPage = edit.getObject(Page.class, PAGE_ID, true);

		// store the original page name
		String oldName = editPage.getName();

		// modify page, save (without commit)
		editPage.setName(newName);
		editPage.save();

		// start second transaction
		Transaction read = testContext.startTransactionWithPermissions(false);

		// read page
		Page readPage = read.getObject(Page.class, PAGE_ID);

		// we expect the page name to be the old name
		assertEquals("Check the name before commiting edit transaction", oldName, readPage.getName());

		// commit first transaction
		edit.commit();

		// read page from second transaction again
		readPage = read.getObject(Page.class, PAGE_ID);
		// we expect the page name still to be the old name (the read transaction was started before the page was modified)
		assertEquals("Check the name after commiting edit transaction", oldName, readPage.getName());
		// commit the read transaction
		read.commit();

		// start a new read transaction
		read = testContext.startTransactionWithPermissions(false);
		// read page from the new transaction
		readPage = read.getObject(Page.class, PAGE_ID);
		// we expect the page name to be the new one
		assertEquals("Check the name for a new transaction", newName, readPage.getName());
	}

	/**
	 * Test editing an existing contenttag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEditContentTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		String tagName = "htmltag";
		String newValueText = "This is the edited content";
		int newValueRef = 99;
		int newInfo = 4711;

		// get the page for editing
		Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

		// get the contenttag for editing
		ContentTag tag = page.getContentTag(tagName);

		// edit the content
		Value value = tag.getValues().iterator().next();

		value.setValueText(newValueText);
		value.setValueRef(newValueRef);
		value.setInfo(newInfo);

		// save the page and commit the transaction
		page.save();
		t.commit(true);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page again and check the tag content
		page = (Page) t.getObject(Page.class, PAGE_ID);
		tag = page.getContentTag(tagName);
		value = tag.getValues().iterator().next();
		assertEquals("Check the value_text", newValueText, value.getValueText());
		assertEquals("Check the value_ref", newValueRef, value.getValueRef());
		assertEquals("Check the info", newInfo, value.getInfo());

		// now check in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery(
				"SELECT value.* FROM page LEFT JOIN contenttag ON page.content_id = contenttag.content_id left join value on value.contenttag_id = contenttag.id where page.id ="
						+ PAGE_ID);

		if (res.next()) {
			assertEquals("Check the value_text", newValueText, res.getString("value_text"));
			assertEquals("Check the value_ref", newValueRef, res.getInt("value_ref"));
			assertEquals("Check the info", newInfo, res.getInt("info"));
		} else {
			fail("Could not find data of the page in the database");
		}
	}

	/**
	 * Test adding a new contenttag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddingContentTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		String newValueText = "This is the new content";
		int constructId = 1;
		String expectedTagName = "htmllong1";

		// get the page for editing
		Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

		// add a new tag
		ContentTag newTag = page.getContent().addContentTag(constructId);

		// set a value for the tag
		Value value = newTag.getValues().iterator().next();

		value.setValueText(newValueText);

		// save the page
		page.save();
		t.commit(true);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page again and check for the new tag
		page = (Page) t.getObject(Page.class, PAGE_ID);
		Map<String, ContentTag> contentTags = page.getContent().getContentTags();

		// check the tags
		assertEquals("Check the number of contenttags in the page", 3, contentTags.size());

		// get the tag
		ContentTag loadedTag = contentTags.get(expectedTagName);

		assertNotNull("Check whether the name was found", loadedTag);
		assertEquals("Check the name of the tag (again)", expectedTagName, loadedTag.getName());
		assertEquals("Check the construct", constructId, loadedTag.getConstruct().getId().intValue());
		assertEquals("Check the content of the tag", newValueText, loadedTag.getValues().iterator().next().getValueText());

		// check number of tags in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery(
				"SELECT count(*) c FROM page LEFT JOIN contenttag ON page.content_id = contenttag.content_id WHERE page.id = " + PAGE_ID);

		res.next();
		assertEquals("Check number of tags in the database", 3, res.getInt("c"));

		// check tag content in the database
		res = testContext.getDBSQLUtils().executeQuery(
				"SELECT contenttag.construct_id, value.* FROM page LEFT JOIN contenttag ON page.content_id = contenttag.content_id LEFT JOIN value on contenttag.id = value.contenttag_id WHERE page.id = "
						+ PAGE_ID + " AND contenttag.name = '" + expectedTagName + "'");

		if (res.next()) {
			assertEquals("Check the construct", constructId, res.getInt("construct_id"));
			assertEquals("Check the content of the tag", newValueText, res.getString("value_text"));
		} else {
			fail("Could not find contenttag value in the database");
		}
	}

	/**
	 * Test removing a contenttag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRemovingContentTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		String tagName = "htmltag";

		// get the page for editing
		Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

		// get the map of all tags
		Map<String, ContentTag> tagMap = page.getContent().getContentTags();

		assertEquals("Check number of contenttags before removing one", 2, tagMap.size());

		// remove one (the only) tag
		tagMap.remove(tagName);

		// save the page
		page.save();
		t.commit(true);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		page = (Page) t.getObject(Page.class, PAGE_ID);

		assertEquals("Check number of contenttags after removing one", 1, page.getContent().getContentTags().size());

		// check number of contenttags in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery(
				"SELECT count(*) c FROM page LEFT JOIN contenttag ON page.content_id = contenttag.content_id WHERE page.id = " + PAGE_ID
				+ " AND contenttag.id IS NOT NULL");

		res.next();
		assertEquals("Check number of contenttags in the database", 1, res.getInt("c"));
	}

	/**
	 * Tests the creation of two identical pages (same folder, same name, same filename)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePageTwice() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		String newName = "The edited Name";
		String newFileName = "editedfilename.html";

		Page newFirstPage = (Page) t.createObject(Page.class);

		newFirstPage.setFilename(newFileName);
		newFirstPage.setName(newName);
		newFirstPage.setTemplateId(TEMPLATE_ID);
		newFirstPage.setFolderId(FOLDER_ID);

		// check whether the new page has no id
		assertNull("Check whether the new page has no id", newFirstPage.getId());

		// save the page
		newFirstPage.save();
		int contentSetID1 = ObjectTransformer.getInt(newFirstPage.getContentsetId(), 0);

		// commit the transaction
		t.commit(false);
		t = testContext.startTransactionWithPermissions(false);

		Page newSecondPage = (Page) t.createObject(Page.class);

		newSecondPage.setFilename(newFileName);
		newSecondPage.setName(newName);
		newSecondPage.setTemplateId(TEMPLATE_ID);
		newSecondPage.setFolderId(FOLDER_ID);

		// check whether the new page has no id
		assertNull("Check whether the new page has no id", newSecondPage.getId());
		// save the page
		newSecondPage.save();
		int contentSetID2 = ObjectTransformer.getInt(newSecondPage.getContentsetId(), 0);

		// commit the transaction
		t.commit(false);
		assertTrue("Both contentset ids should be different.", contentSetID1 != contentSetID2);

		assertFalse("Both filenames should be different", newFirstPage.getFilename().equals(newSecondPage.getFilename()));

	}
	
	/**
	 * Tests filename sanitize.
	 * @throws Exception
	 */
	@Test
	public void testSanitizeFilename() throws Exception {
		Page page = createAndSavePage("äëï.html");

		assertEquals("aeei.html", page.getFilename());
	}

	/**
	 * Tests leading/trailing spaces removal.
	 * @throws Exception
	 */
	@Test
	public void testLeadingTrailingSpace() throws Exception {
		Page page = createAndSavePage("  leadingtrailing.html  ");

		assertEquals("leadingtrailing.html", page.getFilename());
	}

	/**
	 * Test create many pages with the same proposed filename
	 * @throws Exception
	 */
	@Test
	public void testFilenameUniqueness() throws Exception {
		Set<String> fileNames = new HashSet<>();
		Set<String> pageNames = new HashSet<>();

		String[] proposedFilenames = new String[] {"filename.html", "FILENAME.html"};

		for (int i = 0; i < 300; i++) {
			Page page = null;
			try (Trx trx = new Trx(null, 1)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				page = t.createObject(Page.class);
				page.setFilename(proposedFilenames[i % 2]);
				page.setName("Page Name");
				page.setTemplateId(TEMPLATE_ID);
				page.setFolderId(FOLDER_ID);
				page.save();

				trx.success();
			}

			String checkedFilename = page.getFilename().toLowerCase();
			assertFalse("Duplicate filename '" + page.getFilename() + "' found", fileNames.contains(checkedFilename));
			fileNames.add(checkedFilename);

			assertFalse("Duplicate pagename '" + page.getName() + "' found", pageNames.contains(page.getName()));
			pageNames.add(page.getName());
		}
	}

	/**
	 * Creates and saves a page.
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	private Page createAndSavePage (String fileName) throws Exception {
		testContext.startTransactionWithPermissions(true);
		String pageName = "This is the new page";
		Transaction t = TransactionManager.getCurrentTransaction();

		Page newPage = t.createObject(Page.class);

		newPage.setFilename(fileName);
		newPage.setName(pageName);

		// set the template id (which should generate the content with the contenttags and values)
		newPage.setTemplateId(TEMPLATE_ID);

		// set the folder id
		newPage.setFolderId(FOLDER_ID);

		// save the page
		newPage.save();
		t.commit();
		
		t = testContext.startTransactionWithPermissions(false);

		int newPageId = ObjectTransformer.getInt(newPage.getId(), 0);

		return t.getObject(Page.class, newPageId);
	}

	/**
	 * Test creation of a new page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateNewPage() throws Exception {
		testContext.startTransactionWithPermissions(true);
		String tagName = "htmltag";
		String newValueText = "This is the new content";
		String fileName = "newpage.html";
		String pageName = "This is the new page";
		Transaction t = TransactionManager.getCurrentTransaction();

		Page newPage = (Page) t.createObject(Page.class);

		newPage.setFilename(fileName);
		newPage.setName(pageName);

		// check whether the new page has no id
		assertNull("Check whether the new page has no id", newPage.getId());

		// set the template id (which should generate the content with the contenttags and values)
		newPage.setTemplateId(TEMPLATE_ID);

		assertEquals("Check the number of contenttags after setting the template", 1, newPage.getContent().getContentTags().size());
		assertEquals("Check the name of the existing contenttag", tagName, newPage.getContent().getContentTags().keySet().iterator().next());

		// set the folder id
		newPage.setFolderId(FOLDER_ID);

		// set the content of the contenttag
		newPage.getContentTag(tagName).getValues().iterator().next().setValueText(newValueText);

		// save the page
		newPage.save();
		t.commit();

		t = testContext.startTransactionWithPermissions(false);

		int newPageId = ObjectTransformer.getInt(newPage.getId(), 0);

		assertTrue("Check whether the new page has a page id after saving", newPageId != 0);

		// now load the page
		Page page = (Page) t.getObject(Page.class, newPageId);

		assertNotNull("Check that the page now really exists", page);

		assertEquals("Check the content of the new page", newValueText, page.getContent().getContentTag(tagName).getValues().iterator().next().getValueText());

		// check existance of page, content, contenttag and value in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery(
				"SELECT page.id pageid, content.id contentid, contenttag.id contenttagid, value.id valueid, value.value_text FROM page LEFT JOIN content on page.content_id = content.id LEFT JOIN contenttag on content.id = contenttag.content_id LEFT JOIN value ON contenttag.id = value.contenttag_id WHERE page.id = "
						+ newPageId);

		if (res.next()) {
			assertEquals("Check the content of the new page in the database", newValueText, res.getString("value_text"));
			assertNotNull("Check pageid in the database", res.getObject("pageid"));
			assertNotNull("Check contentid in the database", res.getObject("contentid"));
			assertNotNull("Check contenttagid in the database", res.getObject("contenttagid"));
			assertNotNull("Check valueid in the database", res.getObject("valueid"));
		} else {
			fail("Could not find complete page data in the database");
		}

		// check that the channelset_id of the page was set
		assertTrue("Channelset ID must be set", ObjectTransformer.getInt(page.getChannelSetId(), 0) != 0);
	}

	/**
	 * Test whether locking a locked page fails
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLockingLockedPage() throws Exception {
		// first lock the page for another user
		testContext.getDBSQLUtils().executeQueryManipulation(
				"UPDATE content SET locked = unix_timestamp(), locked_by = 22 WHERE id IN (SELECT content_id FROM page WHERE id = " + PAGE_ID + ")");

		testContext.startTransactionWithPermissions(true);
		// get the transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			t.getObject(Page.class, PAGE_ID, true);
			fail("Expected ReadOnlyException was not thrown");
		} catch (ReadOnlyException e) {}
	}

	/**
	 * Test getting page for edit. This tests that:
	 * <ol>
	 * <li>The content is locked</li>
	 * <li>The Page object and all editable subobjects (Content, ContentTags, ...) are editable</li>
	 * <li>Every editable copy is an instance of its own (is never the same object as a readonly copy)</li>
	 * </ol>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLockingPage() throws Exception {
		testContext.startTransactionWithPermissions(true);

		// get the transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		// get Object for reading
		Page readOnlyPage = (Page) t.getObject(Page.class, PAGE_ID);

		Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

		// check whether the page is locked now for the user
		ResultSet rs = testContext.getDBSQLUtils().executeQuery(
				"SELECT locked, locked_by FROM page LEFT JOIN content ON page.content_id = content.id WHERE page.id = " + PAGE_ID);

		if (rs.next()) {
			assertEquals("Check whether the page is locked by the user", testContext.USER_WITH_PERMS.intValue(), rs.getInt("locked_by"));
			assertTrue("Check whether the page is really locked", rs.getInt("locked") != 0);
		}

		// check whether page, content, tags, values, etc. are editable now
		assertTrue("Check whether the page is editable", page.getObjectInfo().isEditable());
		assertNotSame("Check whether the readonly copy of the page is another object", readOnlyPage, page);

		assertTrue("Check whether content is editable", page.getContent().getObjectInfo().isEditable());
		assertNotSame("Check whether the readonly copy of the content is another object", readOnlyPage.getContent(), page.getContent());

		Map<String, ContentTag> tagMap = page.getContent().getContentTags();
		Map<String, ContentTag> readOnlyTagMap = readOnlyPage.getContent().getContentTags();

		for (Iterator<ContentTag> iterator = tagMap.values().iterator(); iterator.hasNext();) {
			ContentTag tag = iterator.next();
			ContentTag readOnlyTag = readOnlyTagMap.get(tag.getName());

			assertTrue("Check whether contenttag " + tag.getName() + " is editable", tag.getObjectInfo().isEditable());
			assertNotSame("Check whether the readonly copy of the contenttag " + tag.getName() + " is another object", readOnlyTag, tag);

			ValueList values = tag.getValues();

			for (Value value : values) {
				Value readOnlyValue = readOnlyTag.getValues().getByPartId(value.getPartId());

				assertTrue("Check whether value " + value.getPart().getKeyname() + " of contenttag " + tag.getName() + " is editable",
						value.getObjectInfo().isEditable());
				assertNotSame(
						"Check whether the readonly copy of the value " + value.getPart().getKeyname() + " of contenttag " + tag.getName() + " is another object",
						readOnlyValue, value);
			}
		}

		Map<String, ObjectTag> objectTags = page.getObjectTags();

		for (Iterator<ObjectTag> iterator = objectTags.values().iterator(); iterator.hasNext();) {
			ObjectTag tag = iterator.next();

			assertTrue("Check whether objecttag " + tag.getName() + " is editable", tag.getObjectInfo().isEditable());

			ValueList values = tag.getValues();

			for (Value value : values) {
				assertTrue("Check whether value " + value.getPart().getKeyname() + " of objecttag " + tag.getName() + " is editable",
						value.getObjectInfo().isEditable());
			}
		}
	}

	/**
	 * Test creating a page without a name and a filename
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePageWithoutData() throws Exception {
		testContext.startTransactionWithPermissions(true);

		// get the transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the content language
		ContentLanguage language = (ContentLanguage) t.getObject(ContentLanguage.class, CONTENTLANGUAGE_ID);

		assertNotNull("Check the page language", language);

		// create a new page with template and folder
		Page newPage = (Page) t.createObject(Page.class);

		newPage.setTemplateId(TEMPLATE_ID);
		newPage.setFolderId(FOLDER_ID);
		newPage.setLanguage(language);

		// save the page
		newPage.save();

		// right after saving, the page must have a name and a filename
		String pageName = newPage.getName();

		assertNotNull("Check the generated page name", pageName);
		assertEquals("Check that the page name was generated as expected", newPage.getId().toString(), pageName);
		String fileName = newPage.getFilename();

		assertNotNull("Check the generated filename", fileName);
		assertEquals("Check that the filename was generated as expected", newPage.getId() + ".de." + newPage.getTemplate().getMarkupLanguage().getExtension(),
				fileName);

		t.commit(false);

		// check that the data were stored
		Page savedPage = (Page) t.getObject(Page.class, newPage.getId());

		assertEquals("Check that the page name was really saved", newPage.getName(), savedPage.getName());
		assertEquals("Check that the filename was really saved", newPage.getFilename(), savedPage.getFilename());
	}

	/**
	 * Test publishing page variants (together)
	 * @throws Exception
	 */
	@Test
	public void testPublishPageVariants() throws Exception {
		testContext.startTransactionWithPermissions(true);
		PageResource pageResource = getPageResource();

		// assign the template to the folder
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, FOLDER_ID, true);

		folder.setTemplates(Arrays.asList(t.getObject(Template.class, TEMPLATE_ID)));
		folder.save();
		t.commit(false);

		// create a page
		PageCreateRequest createPage = new PageCreateRequest();

		createPage.setFolderId(ObjectTransformer.getString(FOLDER_ID, null));
		createPage.setTemplateId(TEMPLATE_ID);
		PageLoadResponse createResponse = pageResource.create(createPage);

		assertResponseOK(createResponse);
		com.gentics.contentnode.rest.model.Page page1 = createResponse.getPage();

		page1.setName("Page 1");
		PageSaveRequest saveRequest = new PageSaveRequest();

		saveRequest.setPage(page1);
		saveRequest.setUnlock(true);
		assertResponseOK(pageResource.save(ObjectTransformer.getString(page1.getId(), null), saveRequest));

		// create a variant
		createPage.setVariantId(page1.getId());
		createResponse = pageResource.create(createPage);
		assertResponseOK(createResponse);
		com.gentics.contentnode.rest.model.Page page2 = createResponse.getPage();

		page2.setName("Page 2");
		saveRequest.setPage(page2);
		saveRequest.setUnlock(true);
		assertResponseOK(pageResource.save(ObjectTransformer.getString(page2.getId(), null), saveRequest));

		// now try to publish both pages together
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();
		MultiPagePublishRequest publishRequest = new MultiPagePublishRequest();

		publishRequest.setIds(Arrays.asList(ObjectTransformer.getString(page1.getId(), null), ObjectTransformer.getString(page2.getId(), null)));
		publishRequest.setForegroundTime(Integer.MAX_VALUE);
		assertResponseOK(pageResource.publish(null, publishRequest));

		// check whether the pages are published now (do this in a new transaction, because otherwise we would not see the changes, that were made
		// by the transaction in the multipagepublishjob)
		TransactionManager.setCurrentTransaction(TransactionManager.getTransaction(t));
		t.commit();
		PageLoadResponse loadResponse = pageResource.load(ObjectTransformer.getString(page1.getId(), null), false, false, false, false, false, false, false,
				false, false, false, null, null);

		assertResponseOK(loadResponse);
		assertTrue("Check page status for page " + page1.getName(), loadResponse.getPage().isOnline());
		loadResponse = pageResource.load(ObjectTransformer.getString(page2.getId(), null), false, false, false, false, false, false, false, false, false, false, null, null);
		assertResponseOK(loadResponse);
		assertTrue("Check page status for page " + page2.getName(), loadResponse.getPage().isOnline());
	}

	/**
	 * Test creating, modifying and publishing pages in multiple threads
	 * @throws Exception
	 */
	@Test
	public void testThreadedPageCreation() throws Throwable {
		testContext.startTransactionWithPermissions(true);

		// assign the template to the folder
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, FOLDER_ID, true);

		folder.setTemplates(Arrays.asList(t.getObject(Template.class, TEMPLATE_ID)));
		folder.save();
		t.commit(false);

		// create the threads and start them
		List<PageCreator> threads = new ArrayList<PageEditSandboxTest.PageCreator>();

		for (int i = 0; i < NUM_THREADS; i++) {
			PageCreator thread = new PageCreator(NUM_PAGES_PER_THREAD);

			thread.setName("Thread-" + (i + 1));
			threads.add(thread);
			thread.start();
		}

		// wait for all threads to die
		for (PageCreator thread : threads) {
			thread.join();
		}

		// check that all threads succeeded
		for (PageCreator thread : threads) {
			thread.assertOK();
		}
	}

	/**
	 * Test creating a page as variant of another page, that has a very long page name
	 * @throws Exception
	 */
	@Test
	public void testPageVariantWithLongFilename() throws Exception {
		// create a page with a very long name (filename should be autogenerated)
		Transaction t = testContext.startTransactionWithPermissions(true);

		Page page = t.createObject(Page.class);
		page.setName(StringUtils.repeat("abcdefghij", 10));
		page.setTemplateId(TEMPLATE_ID);
		page.setFolderId(FOLDER_ID);
		page.save();
		t.commit();

		// check that the page has a filename now
		t = testContext.startTransactionWithPermissions(false);
		page = t.getObject(Page.class, page.getId());
		String filename = page.getFilename();
		assertFalse("Page filename must not be empty", StringUtils.isEmpty(filename));

		// now create a page variant
		PageResource res = getPageResource();
		PageCreateRequest req = new PageCreateRequest();
		req.setFolderId(Integer.toString(FOLDER_ID));
		req.setTemplateId(TEMPLATE_ID);
		req.setVariantId(ObjectTransformer.getInt(page.getId(), 0));
		PageLoadResponse response = res.create(req);
		assertResponseOK(response);
		String variant1Filename = response.getPage().getFileName();
		t.commit();

		assertFalse("Page variant must have a non-empty filename", StringUtils.isEmpty(variant1Filename));
		assertFalse("Page variant must have a unique filename", variant1Filename.equals(filename));

		// create another page variant
		t = testContext.startTransactionWithPermissions(false);
		res = getPageResource();
		response = res.create(req);
		assertResponseOK(response);
		String variant2Filename = response.getPage().getFileName();

		assertFalse("Page variant must have a non-empty filename", StringUtils.isEmpty(variant2Filename));
		assertFalse("Page variant must have a unique filename", variant2Filename.equals(filename));
		assertFalse("Page variant must have a unique filename", variant2Filename.equals(variant1Filename));
	}

	/**
	 * Get a page resource
	 * @return page resource
	 * @throws TransactionException
	 */
	protected PageResource getPageResource() throws TransactionException {
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(TransactionManager.getCurrentTransaction());
		return pageResource;
	}

	/**
	 * Make an assertion about the response
	 * @param response response
	 * @throws Exception
	 */
	protected void assertResponseOK(GenericResponse response) throws Exception {
		assertTrue("Response was not OK: " + response.getResponseInfo().getResponseMessage(), response.getResponseInfo().getResponseCode() == ResponseCode.OK);
	}

	/**
	 * Thread implementation that will create, modify and publish pages using the rest api
	 */
	protected class PageCreator extends Thread {

		/**
		 * Throwable that was thrown while the thread was running
		 */
		protected Throwable throwable;

		/**
		 * Transaction from the calling thread
		 */
		protected Transaction t;

		/**
		 * Number of pages to create/modify/publish
		 */
		protected int numPages;

		/**
		 * Create an instance
		 * @param numPages number of pages to handle
		 * @throws TransactionException
		 */
		public PageCreator(int numPages) throws TransactionException {
			this.numPages = numPages;
			this.t = TransactionManager.getCurrentTransaction();
		}

		@Override
		public void run() {
			Transaction threadTrans = null;

			try {
				// create a new transaction and set as current (for the thread)
				threadTrans = TransactionManager.getTransaction(t);
				TransactionManager.setCurrentTransaction(threadTrans);

				PageResource res = getPageResource();

				String threadName = Thread.currentThread().getName();

				for (int i = 0; i < numPages; ++i) {
					com.gentics.contentnode.rest.model.Page page = createPage(res);

					page.setName(threadName + " #" + (i + 1));
					page.getTags().get("htmltag").getProperties().get("html").setStringValue("Content#" + (i + 1) + " of page <page name>");
					savePage(res, page);
					publishPage(res, page);
				}
			} catch (Throwable e) {
				throwable = e;
			} finally {
				if (threadTrans != null) {
					try {
						threadTrans.commit();
					} catch (TransactionException e) {}
				}
			}
		}

		/**
		 * Assert that everything went fine
		 * @throws Exception
		 */
		public void assertOK() throws Throwable {
			if (throwable != null) {
				throw throwable;
			}
		}

		/**
		 * Create a new page
		 * @param res resource
		 * @return new page
		 * @throws Exception
		 */
		protected com.gentics.contentnode.rest.model.Page createPage(PageResource res) throws Exception {
			try {
				PageCreateRequest request = new PageCreateRequest();

				request.setFolderId(Integer.toString(FOLDER_ID));
				request.setTemplateId(TEMPLATE_ID);
				PageLoadResponse response = res.create(request);

				assertResponseOK(response);
				return response.getPage();
			} finally {
				TransactionManager.getCurrentTransaction().commit(false);
			}
		}

		/**
		 * Save the page
		 * @param res resource
		 * @param page page
		 * @throws Exception
		 */
		protected void savePage(PageResource res, com.gentics.contentnode.rest.model.Page page) throws Exception {
			try {
				PageSaveRequest request = new PageSaveRequest(page);

				request.setUnlock(true);
				GenericResponse response = res.save(Integer.toString(page.getId()), request);

				assertResponseOK(response);
			} finally {
				TransactionManager.getCurrentTransaction().commit(false);
			}
		}

		/**
		 * Publish the page
		 * @param res resource
		 * @param page page
		 * @throws Exception
		 */
		protected void publishPage(PageResource res, com.gentics.contentnode.rest.model.Page page) throws Exception {
			try {
				PagePublishRequest request = new PagePublishRequest();
				GenericResponse response = res.publish(Integer.toString(page.getId()), null, request);

				assertResponseOK(response);
			} finally {
				TransactionManager.getCurrentTransaction().commit(false);
			}
		}
	}
	/**
	 * Test whether deleted values have references creeping back into their prior content tag
	 * if a page is saved twice in the same transaction
	 * @throws NodeException
	 */
	@Test
	public void testValueRetention() throws NodeException {
		Transaction t = testContext.startTransaction(timestamp++);

		// Create Node
		Node node = t.createObject(Node.class);
		node.setPublishDir("/");
		Folder rootFolder = t.createObject(Folder.class);
		rootFolder.setName("testnode");
		rootFolder.setPublishDir("/");
		node.setFolder(rootFolder);
		node.setHostname("testhost");
		node.save();

		// Create a text Construct
		Construct textConstruct = t.createObject(Construct.class);
		textConstruct.setKeyword("textConstruct");
		textConstruct.setName("textConstruct", 1);
		Part textPart = t.createObject(Part.class);
		textPart.setPartTypeId(21); // HTML long
		textPart.setKeyname("text");
		Value textDefVal = t.createObject(Value.class);
		textPart.setDefaultValue(textDefVal);
		textPart.setEditable(2);
		textConstruct.getParts().add(textPart);
		textConstruct.save();

		// Create Template
		Template template = t.createObject(Template.class);
		template.setName("testtemplate");
		template.setSource("<node testtag>");
		TemplateTag ttag = t.createObject(TemplateTag.class);
		ttag.setConstructId(textConstruct.getId());
		ttag.setEnabled(3);
		ttag.setPublic(true);
		ttag.setName("testtag");
		Value tvalue = t.createObject(Value.class);
		tvalue.setPart(textPart);
		tvalue.setValueText("default value");
		ttag.getValues().add(tvalue);
		template.getTemplateTags().put("testtag", ttag);
		template.setFolderId(rootFolder.getId());
		template.save();

		// Create page
		Page page = t.createObject(Page.class);
		page.setName("testPage");
		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag ctag = page.getContentTag("testtag");
		Value cvalue = ctag.getValues().getByKeyname("text");
		cvalue.setValueText("original value");
		page.save();

		// Assert the setup worked
		t = testContext.startTransaction(timestamp++);
		t.dirtObjectCache(Page.class, page.getId());
		page = t.getObject(Page.class, page.getId(),true);

		Value testValue = page.getContentTag("testtag").getValues().getByKeyname("text");
		assertNotNull("Original value should be there", testValue);
		assertEquals("Original value doesn't match", "original value", testValue.getValueText());

		// Delete value from "text" part and replace by new one
		ctag = page.getContent().getContentTag("testtag");
		ValueList ctagvals = ctag.getValues();
		Value oldval = ctagvals.getByKeyname("text");
		oldval.delete();
		ctagvals.remove(oldval);
		Value newval = t.createObject(Value.class);
		newval.setPart(textPart);
		newval.setValueText("new value");
		ctagvals.add(newval);
		page.save();

		// Save the page again, but do not use a new transaction
		t.dirtObjectCache(Page.class, page.getId());
		page = t.getObject(Page.class, page.getId(),true);
		page.save();

		t = testContext.startTransaction(timestamp++);

		// Check for validity of the value
		t.dirtObjectCache(Page.class, page.getId());
		page = t.getObject(Page.class, page.getId());

		testValue = page.getContentTag("testtag").getValues().getByKeyname("text");
		assertNotNull("A value should still be there", testValue);
		assertEquals("Wrong value_text", "new value", testValue.getValueText());
	}

	/**
	 * Pages must never have an empty channelset after saving.
	 * @throws NodeException
	 */
	@Test
	public void testEmptyChannelsetPage() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node n = Creator.createNode("channelsettest", "channelsettest", "/", "/", Collections.<ContentLanguage> emptyList());
		Template tpl = Creator.createTemplate("channelsettest", "blargh", n.getFolder());
		Page p = t.createObject(Page.class);
		p.setFilename("x");
		p.setName("xy");
		p.setTemplateId(tpl.getId());
		p.setFolderId(n.getFolder().getId());
		p.getChannelSet();
		p.save();
		assertFalse("Channelset must not be empty", p.getChannelSet().isEmpty());
	}


}
