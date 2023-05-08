package com.gentics.contentnode.tests.wastebin;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageTagPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for referencing objects that are put into the wastebin (and
 * restored from the wastebin)
 */
@GCNFeature(set = { Feature.WASTEBIN, Feature.MULTICHANNELLING })
public class WastebinReferenceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public final static String PARTNAME = "part";

	public final static String PAGEURL_TAGNAME = "pageurl";

	public final static String FILEURL_TAGNAME = "fileurl";

	public final static String PAGETAG_TAGNAME = "pagetag";

	public final static String OVERVIEW_TAGNAME = "overview";

	/**
	 * Test node
	 */
	private static Node node;

	private static int pageUrlConstructId;

	private static int fileUrlConstructId;

	private static int pageTagConstructId;

	private static int overviewConstructId;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws Exception {
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode();
			pageUrlConstructId = ContentNodeTestDataUtils.createConstruct(node, PageURLPartType.class, PAGEURL_TAGNAME, PARTNAME);
			fileUrlConstructId = ContentNodeTestDataUtils.createConstruct(node, FileURLPartType.class, FILEURL_TAGNAME, PARTNAME);
			pageTagConstructId = ContentNodeTestDataUtils.createConstruct(node, PageTagPartType.class, PAGETAG_TAGNAME, PARTNAME);
			overviewConstructId = ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, OVERVIEW_TAGNAME, PARTNAME);

			// make template editable
			Construct editableConstruct = trx.getTransaction().getObject(Construct.class, overviewConstructId, true);
			editableConstruct.getParts().get(0).getDefaultValue().setInfo(1);
			editableConstruct.save();

			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node " + PAGEURL_TAGNAME + "><node " + FILEURL_TAGNAME + "><node "
					+ PAGETAG_TAGNAME + "><node " + OVERVIEW_TAGNAME + ">", "Template",
					ContentNodeTestDataUtils.createTemplateTag(pageUrlConstructId, PAGEURL_TAGNAME, true, false),
					ContentNodeTestDataUtils.createTemplateTag(fileUrlConstructId, FILEURL_TAGNAME, true, false),
					ContentNodeTestDataUtils.createTemplateTag(pageTagConstructId, PAGETAG_TAGNAME, true, false),
					ContentNodeTestDataUtils.createTemplateTag(overviewConstructId, OVERVIEW_TAGNAME, true, false));

			trx.success();
		}
	}

	/**
	 * Test reference via Page (URL)
	 * @throws Exception
	 */
	@Test
	public void testPageUrl() throws Exception {
		doPageTest((sourcePage, targetPage) -> {
			ContentNodeTestDataUtils.getPartType(PageURLPartType.class, sourcePage.getContentTag(PAGEURL_TAGNAME), PARTNAME).setTargetPage(targetPage);
		});
	}

	/**
	 * Test reference via File (URL)
	 * @throws Exception
	 */
	@Test
	public void testFileUrl() throws Exception {
		doFileTest((sourcePage, targetFile) -> {
			ContentNodeTestDataUtils.getPartType(FileURLPartType.class, sourcePage.getContentTag(FILEURL_TAGNAME), PARTNAME).setTargetFile(targetFile);
		});
	}

	/**
	 * Test reference via Tag (Page)
	 * @throws Exception
	 */
	@Test
	public void testPageTag() throws Exception {
		doPageTest((sourcePage, targetPage) -> {
			ContentNodeTestDataUtils.getPartType(PageTagPartType.class, sourcePage.getContentTag(PAGETAG_TAGNAME), PARTNAME).setPageTag(targetPage,
					targetPage.getContentTag(PAGEURL_TAGNAME));
		});
	}

	@Test
	public void testPageOverviewDirect() throws Exception {
		doPageTest((sourcePage, targetPage) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node page.name>", Page.class, Overview.SELECTIONTYPE_SINGLE, 0,
					Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetPage));
		});
	}

	@Test
	public void testPageOverviewFromFolder() throws Exception {
		doPageTest((sourcePage, targetPage) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node page.name>", Page.class,
					Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetPage.getFolder()));
		});
	}

	@Test
	public void testFileOverviewDirect() throws Exception {
		doFileTest((sourcePage, targetFile) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node file.name>", File.class, Overview.SELECTIONTYPE_SINGLE, 0,
					Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetFile));
		});
	}

	@Test
	public void testFileOverviewFromFolder() throws Exception {
		doFileTest((sourcePage, targetFile) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node file.name>", File.class,
					Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetFile.getFolder()));
		});
	}

	@Test
	public void testFolderOverviewDirect() throws Exception {
		doFolderTest((sourcePage, targetFolder) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node folder.name>", Folder.class, Overview.SELECTIONTYPE_SINGLE, 0,
					Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetFolder));
		});
	}

	@Test
	public void testFolderOverviewFromFolder() throws Exception {
		doFolderTest((sourcePage, targetFolder) -> {
			ContentNodeTestDataUtils.fillOverview(sourcePage.getContentTag(OVERVIEW_TAGNAME), PARTNAME, "<node folder.name>", Folder.class,
					Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(targetFolder.getMother()));
		});
	}

	/**
	 * Do the test with a page reference
	 * @param referencer implementation that sets the reference
	 * @throws Exception
	 */
	protected void doPageTest(Consumer<Page, Page> referencer) throws Exception {
		Folder folder = null;
		Page targetPage = null;
		Page sourcePage = null;

		// setup data
		try (Trx trx = new Trx()) {
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			targetPage = ContentNodeTestDataUtils.createPage(folder, template, "target page");
			trx.getTransaction().getObject(targetPage, true).publish();

			sourcePage = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "source page");
			sourcePage = trx.getTransaction().getObject(sourcePage, true);

			referencer.apply(sourcePage, targetPage);

			sourcePage.save();
			sourcePage.publish();

			sourcePage = trx.getTransaction().getObject(sourcePage);

			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// delete target page and folder
		try (Trx trx = new Trx()) {
			targetPage.delete();
			folder.delete();
			trx.success();
		}

		// assert source page dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage);
			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// restore folder and target page
		try (Trx trx = new Trx(); WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			trx.getTransaction().getObject(folder).restore();
			folder = trx.getTransaction().getObject(folder);
			trx.getTransaction().getObject(targetPage).restore();
			targetPage = trx.getTransaction().getObject(targetPage);
			trx.success();
		}

		// publish target page
		try (Trx trx = new Trx()) {
			trx.getTransaction().getObject(targetPage, true).publish();
			trx.success();
		}

		// assert source and target page dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage, targetPage);
			trx.success();
		}
	}

	/**
	 * Do the test with a file reference
	 * @param referencer implementation setting the file reference
	 * @throws Exception
	 */
	protected void doFileTest(Consumer<Page, File> referencer) throws Exception {
		Folder folder = null;
		File targetFile = null;
		Page sourcePage = null;

		// setup data
		try (Trx trx = new Trx()) {
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			targetFile = ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile Content".getBytes());

			sourcePage = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "source page");
			sourcePage = trx.getTransaction().getObject(sourcePage, true);

			referencer.apply(sourcePage, targetFile);

			sourcePage.save();
			sourcePage.publish();

			sourcePage = trx.getTransaction().getObject(sourcePage);

			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// delete target file and folder
		try (Trx trx = new Trx()) {
			targetFile.delete();
			folder.delete();
			trx.success();
		}

		// assert source page dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage);
			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// restore target file and folder
		try (Trx trx = new Trx(); WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			trx.getTransaction().getObject(folder).restore();
			folder = trx.getTransaction().getObject(folder);
			trx.getTransaction().getObject(targetFile).restore();
			targetFile = trx.getTransaction().getObject(targetFile);
			trx.success();
		}

		// assert source page and target file dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage);
			ContentNodeTestUtils.assertDirtedObjects(File.class, node, targetFile);
			trx.success();
		}
	}

	/**
	 * Do the test with a folder reference
	 * @param referencer implementation setting the folder reference
	 * @throws Exception
	 */
	protected void doFolderTest(Consumer<Page, Folder> referencer) throws Exception {
		Folder folder = null;
		Folder targetFolder = null;
		Page sourcePage = null;

		// setup data
		try (Trx trx = new Trx()) {
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			targetFolder = ContentNodeTestDataUtils.createFolder(folder, "Folder");

			sourcePage = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "source page");
			sourcePage = trx.getTransaction().getObject(sourcePage, true);

			referencer.apply(sourcePage, targetFolder);

			sourcePage.save();
			sourcePage.publish();

			sourcePage = trx.getTransaction().getObject(sourcePage);

			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// delete target folder and mother folder
		try (Trx trx = new Trx()) {
			targetFolder.delete();
			folder.delete();
			trx.success();
		}

		// assert source page dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage);
			trx.success();
		}

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// restore target folder and mother folder
		try (Trx trx = new Trx(); WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			trx.getTransaction().getObject(folder).restore();
			folder = trx.getTransaction().getObject(folder);
			trx.getTransaction().getObject(targetFolder).restore();
			targetFolder = trx.getTransaction().getObject(targetFolder);
			trx.success();
		}

		// assert source page and target folder dirted
		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourcePage);
			ContentNodeTestUtils.assertDirtedObjects(Folder.class, node, targetFolder, folder);
			trx.success();
		}
	}

	@FunctionalInterface
	protected static interface Consumer<T, U> {
		void apply(T t, U u) throws Exception;
	}
}
