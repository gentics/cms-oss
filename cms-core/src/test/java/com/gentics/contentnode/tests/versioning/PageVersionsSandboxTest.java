package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.clearNodeObjectCache;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.publishPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.setRenderType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.waitMS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * Page Versions Sandbox Test contains tests for versioned publishing
 */
public class PageVersionsSandboxTest extends AbstractPageVersioningTest {

	/**
	 * Id of the template for using in the versions tests
	 */
	public final static int VERSIONS_TEMPLATE_ID = 80;

	/**
	 * Id of the folder for using in the versions tests
	 */
	public final static int VERSIONS_FOLDER_ID = 42;

	/**
	 * Id of the overview page
	 */
	public final static int OVERVIEW_PAGE_ID = 52;

	/**
	 * Id of the page tag construct
	 */
	public final static int PAGE_TAG_CONSTRUCT_ID = 13;

	/**
	 * Id of the velocity construct
	 */
	public final static int VELOCITY_CONSTRUCT_ID = 2;

	/**
	 * Test creation of a page version for a new page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePageVersion() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		String name = "Create Page Version";
		String filename = "create_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and check whether it has a page version with number 0.1
		Page page = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Integer pageId = page.getId();

		clearNodeObjectCache();

		// wait a sec
		waitMS(1000);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page
		page = t.getObject(Page.class, pageId);
		assertNotNull("Check whether the create page was found", page);

		// get the versions and check them
		NodeObjectVersion[] pageVersions = page.getVersions();

		assertEquals("Check the # of page versions", 1, pageVersions.length);
		assertEquals("Check number of created version", "0.1", pageVersions[0].getNumber());
		assertEquals("Check whether version is published", false, pageVersions[0].isPublished());
	}

	/**
	 * Test publishing of a page version for a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPageVersion() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and publish it
		Page page = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Integer pageId = page.getId();

		clearNodeObjectCache();

		// wait a sec
		waitMS(1000);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page
		setRenderType(RenderType.EM_PREVIEW);
		RenderResult renderResult = new RenderResult();

		page = t.getObject(Page.class, pageId);
		assertTrue("Check whether the current version of the page was fetched now", page.getObjectInfo().getVersionTimestamp() < 0);
		assertNotNull("Check whether the created page was found", page);

		// get the versions and check them
		NodeObjectVersion[] pageVersions = page.getVersions();

		assertEquals("Check the # of page versions", 1, pageVersions.length);
		assertEquals("Check number of created version", "1.0", pageVersions[0].getNumber());
		assertEquals("Check whether version is published", true, pageVersions[0].isPublished());

		ContentNodeHelper.setLanguageId(1);

		// render live preview of page (current version)
		String livePreview = page.render(renderResult);

		assertEquals("Check the live preview of the page", "Name: " + name + ", Filename: " + filename + ", Version: 1.0", livePreview);

		// get the published version
		String publishedContent = getPublishedContent(ObjectTransformer.getInt(pageId, 0));

		assertEquals("Check the published content of the page", "Name: " + name + ", Filename: " + filename + ", Version: 1.0", publishedContent);
	}

	/**
	 * Test modifying a published page. Check whether a correct version is created and whether rendering of the published version still renders the publishd (not
	 * modified) page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifyPublishedPage() throws Exception {
		// create a page and publish it.
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and publish it
		Page page = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		Integer pageId = page.getId();

		clearNodeObjectCache();

		// wait a sec
		waitMS(1000);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(false);

		// modify the page
		page = t.getObject(Page.class, pageId, true);
		page.setName("Modified " + page.getName());
		page.setFilename("modified_" + page.getFilename());
		Value value = (Value) page.getContentTag(CONTENTTAG_NAME).getValues().get(PART_NAME);

		value.setValueText("Modified Content: " + value.getValueText());
		page.save();
		t.commit();

		// check whether a new version was created
		t = testContext.startTransactionWithPermissions(false);
		page = t.getObject(Page.class, pageId);
		NodeObjectVersion[] pageVersions = page.getVersions();

		assertEquals("Check number of page versions", 2, pageVersions.length);
		assertEquals("Check version number of last version", "1.1", pageVersions[0].getNumber());
		assertEquals("Check whether last version is published", false, pageVersions[0].isPublished());

		// render a live preview of the page and check whether it renders the
		// modified content
		setRenderType(RenderType.EM_PREVIEW);
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether the current version of the page was fetched now", page.getObjectInfo().getVersionTimestamp() < 0);
		RenderResult renderResult = new RenderResult();
		String livePreview = page.render(renderResult);

		assertEquals("Check preview content of the modified page",
				"Modified Content: Name: Modified " + name + ", Filename: modified_" + filename + ", Version: 1.1", livePreview);

		// render the page for publishing and check whether it is the
		// unmodified version of the page
		page = page.getPublishedObject();
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		renderResult = new RenderResult();
		String publishedPage = page.render(renderResult);

		assertEquals("Check published content of the modified page", "Name: " + name + ", Filename: " + filename + ", Version: 1.0", publishedPage);
	}

	/**
	 * Test modifying a page which is referenced by another page. Check whether republishing the referencing page uses the published version of the modified page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReferencingModifiedPage() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Referenced Page";
		String filename = "referenced_page.html";
		String content = "Referenced Content";

		// get the overview page
		Page overviewPage = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID);

		// Create a referenced page
		Page page = createPage(VERSIONS_TEMPLATE_ID, ObjectTransformer.getInt(overviewPage.getFolder().getId(), 0), name, filename, content);
		Integer pageId = page.getId();

		// wait a sec
		waitMS(1500);

		// publish the pages
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), overviewPage, page);
		overviewPage = t.getObject(Page.class, overviewPage.getId());
		NodeObjectVersion publishedOverviewVersion = overviewPage.getPublishedVersion();

		String expectedContent = "Name: " + name + ", Filename: " + filename + ", Version: 1.0, Content: " + content + "<br/>";

		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: "
				+ publishedOverviewVersion.getNumber() + ", Content: <br/>";

		String overviewContent = getPublishedContent(OVERVIEW_PAGE_ID);

		assertEquals("Check the content of the overview page", expectedContent, overviewContent);

		// wait a sec
		waitMS(1500);

		// modify the page
		t = testContext.startTransactionWithPermissions(false);
		page = t.getObject(Page.class, pageId, true);

		page.setName(page.getName() + " (Modified)");
		page.setFilename("modified_" + page.getFilename());
		Value value = (Value) page.getContentTag(CONTENTTAG_NAME).getValues().get(PART_NAME);

		value.setValueText("Modified Content: " + value.getValueText());
		page.save();
		t.commit(false);

		// change and republish the overview page
		overviewPage = t.getObject(Page.class, OVERVIEW_PAGE_ID, true);
		overviewPage.setName(overviewPage.getName() + " republished");
		overviewPage.save();
		t.commit(false);
		clearNodeObjectCache();

		// wait a sec
		waitMS(1500);

		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), overviewPage);
		overviewPage = t.getObject(Page.class, overviewPage.getId());
		publishedOverviewVersion = overviewPage.getPublishedVersion();

		// get the rendered content of the overview page
		overviewContent = getPublishedContent(OVERVIEW_PAGE_ID);
		// change the expected content (overview has a new page, but referenced page is unmodified)
		expectedContent = "Name: " + name + ", Filename: " + filename + ", Version: 1.0, Content: " + content + "<br/>";
		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: "
				+ publishedOverviewVersion.getNumber() + ", Content: <br/>";
		assertEquals("Check the content of the overview page after modifying referenced page", expectedContent, overviewContent);

		// wait a sec
		waitMS(1500);

		// publish the modified page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);
		page = t.getObject(Page.class, page.getId());

		// change the expected content.
		expectedContent = "Name: " + page.getName() + ", Filename: " + page.getFilename() + ", Version: 2.0, Content: Modified Content: Referenced Content<br/>";
		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: "
				+ publishedOverviewVersion.getNumber() + ", Content: <br/>";

		// get the overview content
		overviewContent = getPublishedContent(OVERVIEW_PAGE_ID);

		assertEquals("Check the content of the overview page after modifying referenced page", expectedContent, overviewContent);
	}

	/**
	 * Test whether after moving a published page (and removing the original folder), the page will still be renderable in the published version And will render its
	 * current folder name (folder changes are not versioned)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMovePublishedPage() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, folder name: <node page.folder.name>, Version: <node page.version.number>";

		// create two folders
		Folder folder1 = t.createObject(Folder.class);

		folder1.setMotherId(VERSIONS_FOLDER_ID);
		folder1.setPublishDir("/");
		folder1.setName("Folder 1");
		folder1.save();
		Folder folder2 = t.createObject(Folder.class);

		folder2.setMotherId(VERSIONS_FOLDER_ID);
		folder2.setPublishDir("/");
		folder2.setName("Folder 2");
		folder2.save();
		t.commit(false);

		// create a page and publish it
		Page page = createPage(VERSIONS_TEMPLATE_ID, ObjectTransformer.getInt(folder1.getId(), -1), name, filename, content);

		clearNodeObjectCache();

		// wait a sec
		waitMS(1500);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// wait a sec
		waitMS(1500);

		// we need a new transaction here, because otherwise, we would not create another version of the page
		t = testContext.startTransactionWithPermissions(false);

		// move page to another folder and remove the original folder
		page = t.getObject(Page.class, page.getId(), true);
		page.setFolderId(folder2.getId());
		page.setName("Modified " + page.getName());
		page.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("bla");
		page.save();
		folder1.delete();
		t.commit(false);

		// get the page content of the published version
		page = (Page) page.getPublishedObject();
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		setRenderType(RenderType.EM_PREVIEW);
		RenderResult renderResult = new RenderResult();
		String pageContent = page.render(renderResult);

		assertEquals("Check the rendered page after moving", "Name: " + name + ", folder name: Folder 2, Version: 1.0", pageContent);
	}

	/**
	 * Test re-rendering a published page after removing a tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRemoveTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Outer Tag, here comes the inner:[<node innertag>]";
		String innerContent = "This is the inner Tag";

		// create a page
		Page page = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);
		// add an inner tag
		ContentTag outerTag = page.getContentTag(CONTENTTAG_NAME);
		ContentTag innerTag = page.getContent().addContentTag(ObjectTransformer.getInt(outerTag.getConstruct().getId(), -1));

		innerTag.setName("innertag");
		innerTag.getValues().getByKeyname(PART_NAME).setValueText(innerContent);
		page.save();
		t.commit(false);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// wait a sec
		waitMS(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now remove the inner tag
		page = t.getObject(Page.class, page.getId(), true);
		page.getContent().getContentTags().remove(innerTag.getName());
		page.save();
		t.commit(false);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page content of the published version
		page = (Page) page.getPublishedObject();
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		setRenderType(RenderType.EM_PREVIEW);
		RenderResult renderResult = new RenderResult();
		String pageContent = page.render(renderResult);

		assertEquals("Check the rendered page after removing tag", "Outer Tag, here comes the inner:[This is the inner Tag]", pageContent);
	}

	/**
	 * Test re-rendering a published page after adding a tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Outer Tag, here comes the inner:[<node innertag>]";
		String innerContent = "This is the inner Tag";

		// create a page
		Page page = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, name, filename, content);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// wait a sec
		waitMS(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now add the inner tag
		page = t.getObject(Page.class, page.getId(), true);
		ContentTag outerTag = page.getContentTag(CONTENTTAG_NAME);
		ContentTag innerTag = page.getContent().addContentTag(ObjectTransformer.getInt(outerTag.getConstruct().getId(), -1));

		innerTag.setName("innertag");
		innerTag.getValues().getByKeyname(PART_NAME).setValueText(innerContent);
		page.save();
		t.commit(false);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page content of the published version
		page = (Page) page.getPublishedObject();
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		setRenderType(RenderType.EM_PREVIEW);
		RenderResult renderResult = new RenderResult();
		String pageContent = page.render(renderResult);

		assertEquals("Check the rendered page after removing tag", "Outer Tag, here comes the inner:[]", pageContent);
	}

	/**
	 * Test re-rendering a published page after changing the template (old template is removed) The page is expected to use its current template (template changes are not
	 * versioned)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplate() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "<node page.template.name>";

		// create a copy of the template
		Template template = t.getObject(Template.class, VERSIONS_TEMPLATE_ID);
		Template newTemplate = (Template) template.copy();

		newTemplate.setName("This is the new template");
		newTemplate.save();
		t.commit(false);

		// create a page
		Page page = createPage(ObjectTransformer.getInt(newTemplate.getId(), -1), VERSIONS_FOLDER_ID, name, filename, content);

		// publish the page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), page);

		// wait a sec
		waitMS(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now change the template
		page = t.getObject(Page.class, page.getId(), true);
		page.setTemplateId(VERSIONS_TEMPLATE_ID);
		page.save();
		t.commit(false);

		// remove the (unused) template
		newTemplate.delete();
		t.commit(false);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// get the page content of the published version
		page = (Page) page.getPublishedObject();
		assertNotNull("Check whether the created page was found", page);
		assertTrue("Check whether a versioned page was fetched now", page.getObjectInfo().getVersionTimestamp() > 0);
		setRenderType(RenderType.EM_PREVIEW);
		RenderResult renderResult = new RenderResult();
		String pageContent = page.render(renderResult);

		assertEquals("Check the rendered page after removing tag", template.getName(), pageContent);
	}

	/**
	 * Test republish a page rendering a page tag to another page, that was modified but not published again The page is expected to render the published version of the
	 * page tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifiedPageTag() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String targetName = "Target Page";
		String targetFilename = "target_page.html";
		String targetContent = "Target Content";
		String sourceName = "Source Page";
		String sourceFilename = "source_page.html";
		String sourceContent = "Here comes the page tag:[<node pagetag>]";

		// create the target page
		Page targetPage = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, targetName, targetFilename, targetContent);

		// create the source page
		Page sourcePage = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, sourceName, sourceFilename, sourceContent);
		// add the page tag
		ContentTag pageTagTag = sourcePage.getContent().addContentTag(PAGE_TAG_CONSTRUCT_ID);

		pageTagTag.setName("pagetag");
		pageTagTag.setEnabled(true);
		Value value = pageTagTag.getValues().iterator().next();
		PageTagPartType partType = (PageTagPartType) value.getPartType();

		partType.setPageTag(targetPage, targetPage.getContentTag(CONTENTTAG_NAME));
		sourcePage.save();
		t.commit(false);

		// publish the pages
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), sourcePage, targetPage);

		// check the correct source page content
		assertEquals("Check published source page content", "Here comes the page tag:[Target Content]",
				getPublishedContent(ObjectTransformer.getInt(sourcePage.getId(), -1)));

		// wait a sec
		waitMS(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now modify the content of the target tag
		targetPage = t.getObject(Page.class, targetPage.getId(), true);
		targetPage.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("Modified Target Page");
		targetPage.save();
		t.commit(false);

		// also modify the source page (to make sure it is really re-rendered in publish process)
		sourcePage = t.getObject(Page.class, sourcePage.getId(), true);
		sourcePage.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("Here still comes the page tag:[<node pagetag>]");
		sourcePage.save();
		t.commit(false);

		// publish the source page again
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), sourcePage);

		// check the correct source page content after republishing
		assertEquals("Check republished source page content", "Here still comes the page tag:[Target Content]",
				getPublishedContent(ObjectTransformer.getInt(sourcePage.getId(), -1)));
	}

	/**
	 * Test republish a page rendering another page (that was modified but not republished) in velocity
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifiedVelocityPage() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String targetName = "Target Page";
		String targetFilename = "target_page.html";
		String targetContent = "Target Content";
		String sourceName = "Source Page";
		String sourceFilename = "source_page.html";
		String sourceContent = "Here comes the vtl tag:[<node vtl>]";

		// create the target page
		Page targetPage = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, targetName, targetFilename, targetContent);

		// create the source page
		Page sourcePage = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, sourceName, sourceFilename, sourceContent);
		// add the vtl tag
		ContentTag pageTagTag = sourcePage.getContent().addContentTag(VELOCITY_CONSTRUCT_ID);

		pageTagTag.setName("vtl");
		pageTagTag.setEnabled(true);
		Value value = pageTagTag.getValues().getByKeyname("template");

		value.setValueText("#set($page = $cms.imps.loader.getPage(" + targetPage.getId() + "))$page");
		sourcePage.save();
		t.commit(false);

		// publish the pages
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), sourcePage, targetPage);

		// check the correct source page content
		assertEquals("Check published source page content", "Here comes the vtl tag:[Target Content]",
				getPublishedContent(ObjectTransformer.getInt(sourcePage.getId(), -1)));

		// wait a sec
		waitMS(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now modify the content of the target tag
		targetPage = t.getObject(Page.class, targetPage.getId(), true);
		targetPage.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("Modified Target Page");
		targetPage.save();
		t.commit(false);

		// also modify the source page (to make sure it is really re-rendered in publish process)
		sourcePage = t.getObject(Page.class, sourcePage.getId(), true);
		sourcePage.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("Here still comes the vtl tag:[<node vtl>]");
		sourcePage.save();
		t.commit(false);

		// publish the source page again
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), sourcePage);

		// check the correct source page content after republishing
		assertEquals("Check republished source page content", "Here still comes the vtl tag:[Target Content]",
				getPublishedContent(ObjectTransformer.getInt(sourcePage.getId(), -1)));
	}

	/**
	 * Test that versioned publishing for page variants works like expected. When a page variant is modified, but not published and another page variant is dirted, the
	 * other page variant still renders the old content. If the modified page variant is then published, both page variants will get republished and will show the new
	 * content.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifiedPageVariant() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		ContentNodeHelper.setLanguageId(1);

		String firstName = "First Page Variant";
		String firstFilename = "first.html";
		String secondName = "Second Page Variant";
		String secondFilename = "secondhtml";
		String sharedContent = "This is the shared content";

		// create the first page
		Page firstPage = createPage(VERSIONS_TEMPLATE_ID, VERSIONS_FOLDER_ID, firstName, firstFilename, sharedContent);

		// create a page variant
		Page secondPage = firstPage.createVariant();

		secondPage.setName(secondName);
		secondPage.setFilename(secondFilename);
		secondPage.save();
		t.commit(false);

		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), firstPage, secondPage);

		// wait a sec
		Thread.sleep(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		// now modify the first page, but do not publish it
		firstPage = t.getObject(Page.class, firstPage.getId(), true);
		firstPage.getContentTag(CONTENTTAG_NAME).getValues().getByKeyname(PART_NAME).setValueText("Modified shared content");
		firstPage.save();
		t.commit(false);

		// modify the template to dirt the pages
		Template template = t.getObject(Template.class, VERSIONS_TEMPLATE_ID, true);

		template.setSource(template.getSource() + " - modified");
		template.save();
		t.commit(false);

		// wait a sec
		Thread.sleep(1500);

		// republish, after dirts are done
		testContext.waitForDirtqueueWorker();
		testContext.publish(false);

		// wait a sec
		Thread.sleep(1500);

		// new transaction
		t = testContext.startTransactionWithPermissions(false);

		String firstPageContent = getPublishedContent(ObjectTransformer.getInt(firstPage.getId(), -1));

		assertEquals("Check published content of first page", "This is the shared content - modified", firstPageContent);
		String secondPageContent = getPublishedContent(ObjectTransformer.getInt(secondPage.getId(), -1));

		assertEquals("Check published content of second page", "This is the shared content - modified", secondPageContent);

		// now publish the first page
		publishPage(testContext.getContext(), testContext.getDBSQLUtils(), firstPage);

		// and check the published contents again
		firstPageContent = getPublishedContent(ObjectTransformer.getInt(firstPage.getId(), -1));
		assertEquals("Check published content of first page", "Modified shared content - modified", firstPageContent);
		secondPageContent = getPublishedContent(ObjectTransformer.getInt(secondPage.getId(), -1));
		assertEquals("Check published content of second page", "Modified shared content - modified", secondPageContent);
	}

	/**
	 * Get the published content of the given page. This will fail if the page is not found in publish table
	 * 
	 * @param pageId
	 *            id of the page
	 * @return content of the published page
	 * @throws Exception
	 */
	public String getPublishedContent(int pageId) throws Exception {
		ResultSet publish = testContext.getDBSQLUtils().executeQuery("SELECT source FROM publish WHERE page_id = " + pageId + " AND active = 1");

		if (publish.next()) {
			return publish.getString("source");
		} else {
			fail("Did not find published overview page");
			return null;
		}
	}
}
