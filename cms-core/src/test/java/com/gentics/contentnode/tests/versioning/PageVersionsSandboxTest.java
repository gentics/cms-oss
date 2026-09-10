package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.LoaderHelperSource;
import com.gentics.contentnode.testutils.TestHelpersHandlebarsService;

/**
 * Page Versions Sandbox Test contains tests for versioned publishing
 */
public class PageVersionsSandboxTest {
	protected final static String HTML_CONSTRUCT_NAME = "html";

	protected final static String HTML_PART_NAME = "html";

	protected final static String OVERVIEW_CONSTRUCT_NAME = "overview";

	protected final static String OVERVIEW_PART_NAME = "overview";

	protected final static String PAGETAG_CONSTRUCT_NAME = "pagetag";

	protected final static String PAGETAG_PART_NAME = "pagetag";

	protected final static String HBS_CONSTRUCT_NAME = "hbs";

	protected final static String HBS_PART_NAME = "hbs";

	protected final static String TEMPLATE_TAG_NAME = "html";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;

	private static int htmlConstructId;
	private static int overviewConstructId;
	private static int pagetagConstructId;
	private static int hbsConstructId;

	private static Template template;


	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		TestHelpersHandlebarsService.addHelper(LoaderHelperSource.class);

		node = supply(() -> createNode());

		htmlConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, HTML_CONSTRUCT_NAME, HTML_PART_NAME));
		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, OVERVIEW_CONSTRUCT_NAME, OVERVIEW_PART_NAME));
		pagetagConstructId = supply(() -> createConstruct(node, PageTagPartType.class, PAGETAG_CONSTRUCT_NAME, PAGETAG_PART_NAME));
		hbsConstructId = supply(() -> createConstruct(node, HandlebarsPartType.class, HBS_CONSTRUCT_NAME, HBS_PART_NAME));

		template = create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName("Template");
			tmpl.setMlId(1);

			tmpl.getTags().put(TEMPLATE_TAG_NAME, create(TemplateTag.class, tt -> {
				tt.setConstructId(htmlConstructId);
				tt.setEnabled(true);
				tt.setPublic(true);
				tt.setName(TEMPLATE_TAG_NAME);
			}).doNotSave().build());

			tmpl.setSource("<node %s>".formatted(TEMPLATE_TAG_NAME));
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		template = update(template, upd -> {
			upd.setSource("<node %s>".formatted(TEMPLATE_TAG_NAME));
		}).build();
	}

	/**
	 * Test creation of a page version for a new page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePageVersion() throws Exception {
		String name = "Create Page Version";
		String filename = "create_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and check whether it has a page version with number 0.1
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);
		}).at(1000).build();

		// reload the page
		page = execute(Page::reload, page);
		assertThat(page).as("Created page").isNotNull();

		// versions
		NodeObjectVersion[] pageVersions = execute(Page::getVersions, page);
		assertThat(pageVersions).as("Page versions")
				.usingRecursiveFieldByFieldElementComparatorOnFields("versionNumber", "published", "current", "date")
				.containsExactly(new NodeObjectVersion(0, "0.1", null, new ContentNodeDate(1000), true, false));
	}

	/**
	 * Test publishing of a page version for a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishPageVersion() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and publish it
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);
		}).at(1000).build();

		// publish the page
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// reload
		page = execute(Page::reload, page);
		assertThat(page.getObjectInfo().getVersionTimestamp()).as("Version timestamp").isLessThan(0);

		// versions
		NodeObjectVersion[] pageVersions = execute(Page::getVersions, page);
		assertThat(pageVersions).as("Page versions")
				.usingRecursiveFieldByFieldElementComparatorOnFields("versionNumber", "published", "current", "date")
				.containsExactly(new NodeObjectVersion(0, "1.0", null, new ContentNodeDate(2000), true, true), new NodeObjectVersion(0, "0.1", null, new ContentNodeDate(1000), false, false));

		String expected = "Name: %s, Filename: %s, Version: %s".formatted(name, filename, "1.0");

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}

		// get the published version
		assertThat(getPublishedContent(page.getId())).as("Published page content").isEqualTo(expected);
	}

	/**
	 * Test modifying a published page. Check whether a correct version is created and whether rendering of the published version still renders the publishd (not
	 * modified) page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifyPublishedPage() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>";

		// create a page and publish it
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);
		}).at(1000).build();

		// publish the page
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// modify the page
		page = update(page, update -> {
			update.setName("Modified " + update.getName());
			update.setFilename("modified_" + update.getFilename());

			LongHTMLPartType partType = getPartType(LongHTMLPartType.class, update.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME);
			partType.setText("Modified Content: " + partType.getText());
		}).at(3000).doNotPublish().build();

		NodeObjectVersion[] pageVersions = execute(Page::getVersions, page);

		assertThat(pageVersions).as("Page versions")
				.usingRecursiveFieldByFieldElementComparatorOnFields("versionNumber", "published", "current", "date")
				.containsExactly(
						new NodeObjectVersion(0, "1.1", null, new ContentNodeDate(3000), true, false),
						new NodeObjectVersion(0, "1.0", null, new ContentNodeDate(2000), false, true),
						new NodeObjectVersion(0, "0.1", null, new ContentNodeDate(1000), false, false));

		// render a live preview of the page and check whether it renders the
		// modified content
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.render()).as("Rendered page")
					.isEqualTo("Modified Content: Name: Modified %s, Filename: modified_%s, Version: %s".formatted(name,
							filename, "1.1"));
			trx.success();
		}

		// render the page for publishing and check whether it is the
		// unmodified version of the page
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.getPublishedObject().render()).as("Rendered published page")
			.isEqualTo("Name: %s, Filename: %s, Version: %s".formatted(name,
					filename, "1.0"));
			trx.success();
		}
	}

	/**
	 * Test modifying a page which is referenced by another page. Check whether republishing the referencing page uses the published version of the modified page.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReferencingModifiedPage() throws Exception {

		String name = "Referenced Page";
		String filename = "referenced_page.html";
		String content = "Referenced Content";

		// create overview page
		Page overviewPage = create(Page.class, p -> {
			p.setName("Overview");
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			ContentTag overviewTag = p.getContent().addContentTag(overviewConstructId);
			Overview overview = getPartType(OverviewPartType.class, overviewTag, OVERVIEW_PART_NAME).getOverview();
			overview.setObjectClass(Page.class);
			overview.setSelectionType(Overview.SELECTIONTYPE_PARENT);
			overview.setOrderKind(Overview.ORDER_NAME);
			overview.setOrderWay(Overview.ORDERWAY_DESC);
			getPartType(OverviewPartType.class, overviewTag, OVERVIEW_PART_NAME).getValueObject().setValueText("Name: <node page.name>, Filename: <node page.filename>, Version: <node page.version.number>, Content: <node html><br/>");

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME)
					.setText("<node %s>".formatted(overviewTag.getName()));
		}).at(1000).build();

		// Create a referenced page
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);
		}).at(1000).build();

		// publish the pages
		overviewPage = update(overviewPage, p -> {}).at(2000).publish().build();
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		String expectedContent = "Name: " + name + ", Filename: " + filename + ", Version: 1.0, Content: " + content + "<br/>";
		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: 1.0, Content: <br/>";

		String overviewContent = getPublishedContent(overviewPage.getId());

		assertThat(overviewContent).as("Published overview page").isEqualTo(expectedContent);

		// modify the page
		page = update(page, upd -> {
			upd.setName(upd.getName() + " (Modified)");
			upd.setFilename("modified_" + upd.getFilename());
			LongHTMLPartType partType = getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME);
			partType.setText("Modified Content: " + partType.getText());
		}).at(3000).doNotPublish().build();

		// change and republish the overview page
		overviewPage = update(overviewPage, upd -> {
			upd.setName(upd.getName() + " republished");
		}).at(3000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// get the rendered content of the overview page
		overviewContent = getPublishedContent(overviewPage.getId());

		// change the expected content (overview has a new page, but referenced page is unmodified)
		expectedContent = "Name: " + name + ", Filename: " + filename + ", Version: 1.0, Content: " + content + "<br/>";
		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: 2.0, Content: <br/>";
		assertThat(overviewContent).as("Published overview page").isEqualTo(expectedContent);

		// publish the modified page
		page = update(page, upd -> {}).at(4000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// change the expected content.
		expectedContent = "Name: " + page.getName() + ", Filename: " + page.getFilename() + ", Version: 2.0, Content: Modified Content: Referenced Content<br/>";
		expectedContent += "Name: " + overviewPage.getName() + ", Filename: " + overviewPage.getFilename() + ", Version: 2.0, Content: <br/>";

		// get the overview content
		overviewContent = getPublishedContent(overviewPage.getId());
		assertThat(overviewContent).as("Published overview page").isEqualTo(expectedContent);
	}

	/**
	 * Test whether after moving a published page (and removing the original folder), the page will still be renderable in the published version And will render its
	 * current folder name (folder changes are not versioned)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMovePublishedPage() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Name: <node page.name>, folder name: <node page.folder.name>, Version: <node page.version.number>";


		// create two folders
		Folder folder1 = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setPublishDir("/");
			create.setName("Folder 1");
		}).build();

		Folder folder2 = create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setPublishDir("/");
			create.setName("Folder 2");
		}).build();

		// create a page and publish it
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);
		}).at(1000).build();

		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// move page to another folder and remove the original folder
		update(page, upd -> {
			upd.setFolderId(folder2.getId());
			upd.setName("Modified " + upd.getName());
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("bla");
		}).doNotPublish().build();

		consume(Folder::delete, folder1);

		// get the page content of the published version
		String expected = "Name: " + name + ", folder name: Folder 2, Version: 1.0";

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.getPublishedObject().render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}
	}

	/**
	 * Test re-rendering a published page after removing a tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRemoveTag() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Outer Tag, here comes the inner:[<node %s>]";
		String innerContent = "This is the inner Tag";

		AtomicReference<String> innerTagName = new AtomicReference<>();

		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			ContentTag outerTag = p.getContentTag(TEMPLATE_TAG_NAME);
			ContentTag innerTag = p.getContent().addContentTag(htmlConstructId);

			getPartType(LongHTMLPartType.class, innerTag, HTML_PART_NAME).setText(innerContent);
			getPartType(LongHTMLPartType.class, outerTag, HTML_PART_NAME).setText(content.formatted(innerTag.getName()));
			innerTagName.set(innerTag.getName());

		}).at(1000).build();

		// publish the page
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		page = update(page, upd -> {
			upd.getContentTags().remove(innerTagName.get());
		}).at(3000).doNotPublish().build();

		// get the page content of the published version
		String expected = "Outer Tag, here comes the inner:[This is the inner Tag]";

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.getPublishedObject().render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}
	}

	/**
	 * Test re-rendering a published page after adding a tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddTag() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "Outer Tag, here comes the inner:[<node innertag>]";
		String innerContent = "This is the inner Tag";

		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);

		}).at(1000).build();

		// publish the page
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// now add the inner tag
		page = update(page, upd -> {
			ContentTag innerTag = upd.getContent().addContentTag(htmlConstructId);
			innerTag.setName("innertag");
			getPartType(LongHTMLPartType.class, innerTag, HTML_PART_NAME).setText(innerContent);
		}).at(2000).doNotPublish().build();

		// get the page content of the published version
		String expected = "Outer Tag, here comes the inner:[]";

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.getPublishedObject().render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}
	}

	/**
	 * Test re-rendering a published page after changing the template (old template is removed) The page is expected to use its current template (template changes are not
	 * versioned)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangeTemplate() throws Exception {
		String name = "Publish Page Version";
		String filename = "publish_page_version.html";
		String content = "<node page.template.name>";

		Template newTemplate = execute(tmpl -> {
			Template copy = (Template)tmpl.copy();
			copy.setName("This is the new template");
			copy.save();
			return copy;
		}, template);
		int newTemplateId = newTemplate.getId();

		// create a page
		Page page = create(Page.class, p -> {
			p.setName(name);
			p.setFilename(filename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(newTemplateId);

			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(content);

		}).at(1000).build();

		// publish the page
		page = update(page, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// now change the template
		page = update(page, upd -> {
			upd.setTemplateId(template.getId());
		}).doNotPublish().build();

		// remove the (unused) template
		consume(Template::delete, newTemplate);

		// get the page content of the published version
		String expected = template.getName();

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(page.getPublishedObject().render()).as("Rendered page").isEqualTo(expected);
			trx.success();
		}
	}

	/**
	 * Test republish a page rendering a page tag to another page, that was modified but not published again The page is expected to render the published version of the
	 * page tag
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifiedPageTag() throws Exception {
		String targetName = "Target Page";
		String targetFilename = "target_page.html";
		String targetContent = "Target Content";
		String sourceName = "Source Page";
		String sourceFilename = "source_page.html";
		String sourceContent = "Here comes the page tag:[<node pagetag>]";

		// create the target page
		Page targetPage = create(Page.class, p -> {
			p.setName(targetName);
			p.setFilename(targetFilename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(targetContent);
		}).at(1000).build();
		Page finalTargetPage = targetPage;

		// create the source page
		Page sourcePage = create(Page.class, p -> {
			p.setName(sourceName);
			p.setFilename(sourceFilename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(sourceContent);

			// add the page tag
			ContentTag pagetagTag = p.getContent().addContentTag(pagetagConstructId);
			pagetagTag.setName("pagetag");
			getPartType(PageTagPartType.class, pagetagTag, PAGETAG_PART_NAME).setPageTag(finalTargetPage, finalTargetPage.getContentTag(TEMPLATE_TAG_NAME));
		}).at(1000).build();

		// publish the pages
		sourcePage = update(sourcePage, p -> {}).at(2000).publish().build();
		targetPage = update(targetPage, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// check the correct source page content
		assertThat(getPublishedContent(sourcePage.getId())).as("Published source page")
				.isEqualTo("Here comes the page tag:[Target Content]");

		// now modify the content of the target tag
		update(targetPage, upd -> {
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("Modified Target Page");
		}).at(3000).doNotPublish().build();

		// also modify the source page (to make sure it is really re-rendered in publish process)
		update(sourcePage, upd -> {
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("Here still comes the page tag:[<node pagetag>]");
		}).at(3000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// check the correct source page content after republishing
		assertThat(getPublishedContent(sourcePage.getId())).as("Published source page")
				.isEqualTo("Here still comes the page tag:[Target Content]");
	}

	/**
	 * Test republish a page rendering another page (that was modified but not republished) in handlebars
	 * 
	 * @throws Exception
	 */
	@Test
	public void testModifiedHandlebarsPage() throws Exception {
		String targetName = "Target Page";
		String targetFilename = "target_page.html";
		String targetContent = "Target Content";
		String sourceName = "Source Page";
		String sourceFilename = "source_page.html";
		String sourceContent = "Here comes the hbs tag:[<node hbs>]";

		// create the target page
		Page targetPage = create(Page.class, p -> {
			p.setName(targetName);
			p.setFilename(targetFilename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(targetContent);
		}).at(1000).build();
		Page finalTargetPage = targetPage;

		// create the source page
		Page sourcePage = create(Page.class, p -> {
			p.setName(sourceName);
			p.setFilename(sourceFilename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(sourceContent);

			// add the hbs tag
			ContentTag hbsTag = p.getContent().addContentTag(hbsConstructId);
			hbsTag.setName("hbs");
			getPartType(HandlebarsPartType.class, hbsTag, HBS_PART_NAME).setText("{{#with (gtx_test_page %d)}}{{gtx_render this}}{{/with}}".formatted(finalTargetPage.getId()));
		}).at(1000).build();

		// publish the pages
		sourcePage = update(sourcePage, p -> {}).at(2000).publish().build();
		targetPage = update(targetPage, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// check the correct source page content
		assertThat(getPublishedContent(sourcePage.getId())).as("Published source page")
				.isEqualTo("Here comes the hbs tag:[Target Content]");

		// now modify the content of the target tag
		targetPage = update(targetPage, upd -> {
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("Modified Target Page");
		}).at(3000).doNotPublish().build();

		// also modify the source page (to make sure it is really re-rendered in publish process)
		sourcePage = update(sourcePage, upd -> {
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("Here still comes the hbs tag:[<node hbs>]");
		}).at(3000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// check the correct source page content after republishing
		assertThat(getPublishedContent(sourcePage.getId())).as("Published source page")
				.isEqualTo("Here still comes the hbs tag:[Target Content]");
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
		String firstName = "First Page Variant";
		String firstFilename = "first.html";
		String secondName = "Second Page Variant";
		String secondFilename = "secondhtml";
		String sharedContent = "This is the shared content";

		// create the first page
		Page firstPage = create(Page.class, p -> {
			p.setName(firstName);
			p.setFilename(firstFilename);
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			getPartType(LongHTMLPartType.class, p.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText(sharedContent);
		}).at(1000).build();

		// create a page variant
		Page secondPage = execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(1000 * 1000);
			Page variant = p.createVariant();
			variant.setName(secondName);
			variant.setFilename(secondFilename);
			variant.save();
			return variant;
		}, firstPage);

		// publish the pages
		firstPage = update(firstPage, p -> {}).at(2000).publish().build();
		secondPage = update(secondPage, p -> {}).at(2000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// now modify the first page, but do not publish it
		firstPage = update(firstPage, upd -> {
			getPartType(LongHTMLPartType.class, upd.getContentTag(TEMPLATE_TAG_NAME), HTML_PART_NAME).setText("Modified shared content");
		}).at(3000).doNotPublish().build();

		// change template to have the pages republished
		template = update(template, upd -> {
			upd.setSource(upd.getSource() + " - modified");
		}).build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		assertThat(getPublishedContent(firstPage.getId())).as("Published content of first page").isEqualTo("This is the shared content - modified");
		assertThat(getPublishedContent(secondPage.getId())).as("Published content of second page").isEqualTo("This is the shared content - modified");

		// now publish the first page
		firstPage = update(firstPage, upd -> {
		}).at(4000).publish().build();

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		assertThat(getPublishedContent(firstPage.getId())).as("Published content of first page").isEqualTo("Modified shared content - modified");
		assertThat(getPublishedContent(secondPage.getId())).as("Published content of second page").isEqualTo("Modified shared content - modified");
	}

	/**
	 * Get the published content of the given page. This will fail if the page is not found in publish table
	 * 
	 * @param pageId
	 *            id of the page
	 * @return content of the published page
	 * @throws Exception
	 */
	public String getPublishedContent(int pageId) throws NodeException {
		String publishedContent = supply(() -> {
			return DBUtils.select("SELECT source FROM publish WHERE page_id = ? AND active = 1", pst -> {
				pst.setInt(1, pageId);
			}, DBUtils.firstString("source"));
		});

		if (publishedContent == null) {
			fail("Did not find published page content");
		}
		return publishedContent;
	}
}
