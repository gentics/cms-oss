package com.gentics.contentnode.tests.nodeobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.testutils.DBTestContext;

public class PageLoadSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the page which has versions
	 */
	protected final static Integer PAGE_ID = 38;

	/**
	 * ID of another page (listed in the overview)
	 */
	protected final static Integer PAGE1_ID = 39;

	/**
	 * ID of another page (listed in the overview)
	 */
	protected final static Integer PAGE2_ID = 40;

	/**
	 * ID of the page containing overviews
	 */
	protected final static Integer OVERVIEW_PAGE_ID = 41;

	/**
	 * ID of the first folder
	 */
	protected final static Integer FOLDER1_ID = 24;

	/**
	 * ID of the second folder
	 */
	protected final static Integer FOLDER2_ID = 25;

	/**
	 * ID of the third folder
	 */
	protected final static Integer FOLDER3_ID = 26;

	/**
	 * ID of the first file
	 */
	protected final static Integer FILE1_ID = 13;

	/**
	 * ID of the second file
	 */
	protected final static Integer FILE2_ID = 14;

	/**
	 * ID of the third file
	 */
	protected final static Integer FILE3_ID = 15;

	/**
	 * ID of the first image
	 */
	protected final static Integer IMAGE1_ID = 10;

	/**
	 * ID of the second image
	 */
	protected final static Integer IMAGE2_ID = 11;

	/**
	 * ID of the third image
	 */
	protected final static Integer IMAGE3_ID = 12;

	/**
	 * expected current page name
	 */
	protected final static String CURRENT_PAGE_NAME = "Page with many Versions";

	/**
	 * expected current page content
	 */
	protected final static String CURRENT_PAGE_CONTENT = "This is the modified page content.<br/>\r\n<node overview1>";

	/**
	 * expected modified page content
	 */
	protected final static String MODIFIED_PAGE_CONTENT = "This is the modified page content.";

	/**
	 * expected initial page name
	 */
	protected final static String INITIAL_PAGE_NAME = "Page with Versions";

	/**
	 * expected initial page content
	 */
	protected final static String INITIAL_PAGE_CONTENT = "This is the initial page content.";

	/**
	 * name of the content tag
	 */
	protected final static String TAG_NAME = "text";

	/**
	 * name of the new overview1 tag
	 */
	protected final static String OVERVIEW1_TAG_NAME = "overview1";

	/**
	 * name of the new overview2 tag
	 */
	protected final static String OVERVIEW2_TAG_NAME = "overview2";

	/**
	 * name of the new overview3 tag
	 */
	protected final static String OVERVIEW3_TAG_NAME = "overview3";

	/**
	 * name of the new overview4 tag
	 */
	protected final static String OVERVIEW4_TAG_NAME = "overview4";

	private Folder folder1;

	private Folder folder2;

	private Folder folder3;

	private Page page1;

	private Page page2;

	private Page currentVersion;

	private File file1;

	private File file2;

	private File file3;

	private ImageFile image1;

	private ImageFile image2;

	private ImageFile image3;

	private NodeObjectVersion[] pageVersions;

	private Page overviewPage;

	private NodeObjectVersion[] overviewPageVersions;

	/**
	 * Restore the sandbox snapshot
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Note: since the tests in this test case do not modify data, it is not
		// necessary to restore a snapshot for every test case
		Transaction t = testContext.getContext().getTransaction();

		folder1 = (Folder) t.getObject(Folder.class, FOLDER1_ID);
		folder2 = (Folder) t.getObject(Folder.class, FOLDER2_ID);
		folder3 = (Folder) t.getObject(Folder.class, FOLDER3_ID);
		page1 = (Page) t.getObject(Page.class, PAGE1_ID);
		page2 = (Page) t.getObject(Page.class, PAGE2_ID);
		file1 = (File) t.getObject(File.class, FILE1_ID);
		file2 = (File) t.getObject(File.class, FILE2_ID);
		file3 = (File) t.getObject(File.class, FILE3_ID);
		image1 = (ImageFile) t.getObject(ImageFile.class, IMAGE1_ID);
		image2 = (ImageFile) t.getObject(ImageFile.class, IMAGE2_ID);
		image3 = (ImageFile) t.getObject(ImageFile.class, IMAGE3_ID);

		currentVersion = (Page) t.getObject(Page.class, PAGE_ID);
		assertNotNull("Check whether the current version of the page could be loaded", currentVersion);

		pageVersions = currentVersion.getVersions();

		// there should be 9 versions
		assertEquals("Check number of page versions", 9, pageVersions.length);

		// get the overview page
		overviewPage = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID);
		assertNotNull("Check whether the current version of the overview page could be loaded", overviewPage);

		overviewPageVersions = overviewPage.getVersions();

		// there whould be 4 versions
		assertEquals("Check number of overview page versions", 4, overviewPageVersions.length);
	}

	/**
	 * Test reading versions of the page
	 * TODO: check that the different page version reflect the following changes made in the page:
	 * 1. Change page name from "Page with Versions" to "Page with many Versions"
	 * 2. Modified content of tag "text" from "This is the initial page content." to "This is the modified page content."
	 * 3. Added new contenttag "overview1" (which lists a single folder "Folder1")
	 * 4. Added folder "Folder2" to "overview1"
	 * 5. Modified sorting of "overview1"
	 * @throws Exception
	 */
	@Test
	public void testReadingCurrentPage() throws Exception {
		// check that the current version has the expected data
		checkPage("current version", currentVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2, folder3 }, true,
				new Page[] { page2, page1, currentVersion });
	}

	/**
	 * Test reading the initial version
	 * @throws Exception
	 */
	@Test
	public void testFirstVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		// get oldest version (initial)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[8].getDate().getIntTimestamp());

		// check that the oldest version has the expected data
		checkPage("first version", oldVersion, INITIAL_PAGE_NAME, INITIAL_PAGE_CONTENT, false, null, false, null);
	}

	/**
	 * Test reading the version after changing name and content
	 * @throws Exception
	 */
	@Test
	public void testSecondVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (modified page name and content)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[7].getDate().getIntTimestamp());

		// check the page
		checkPage("second version", oldVersion, CURRENT_PAGE_NAME, MODIFIED_PAGE_CONTENT, false, null, false, null);
	}

	/**
	 * Test reading the version after adding overview1 tag
	 * @throws Exception
	 */
	@Test
	public void testThirdVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added new overview1 tag)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[6].getDate().getIntTimestamp());

		// check the page
		checkPage("third version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1}, false, null);
	}

	/**
	 * Test reading the version after adding an overview entry
	 * @throws Exception
	 */
	@Test
	public void testFourthVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added overview entry)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[5].getDate().getIntTimestamp());

		// check the page
		checkPage("fourth version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder2, folder1}, false, null);
	}

	/**
	 * Test reading the version after modifying the overview sorting
	 * @throws Exception
	 */
	@Test
	public void testFifthVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (modified overview sorting)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[4].getDate().getIntTimestamp());

		// check the page
		checkPage("fifth version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2}, false, null);
	}

	/**
	 * Test reading the version after adding another overview entry
	 * @throws Exception
	 */
	@Test
	public void testSixthVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added overview entry)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[3].getDate().getIntTimestamp());

		// check the page
		checkPage("sixth version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2, folder3}, false, null);
	}

	/**
	 * Test reading the version after adding overview2
	 * @throws Exception
	 */
	@Test
	public void testSeventhVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added new overview2 tag)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[2].getDate().getIntTimestamp());

		// check the page
		checkPage("seventh version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2, folder3}, true,
				new Page[] { page1});
	}

	/**
	 * Test reading the version after adding an entry to overview2
	 * @throws Exception
	 */
	@Test
	public void testEighthVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added overview entry to overview2)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[1].getDate().getIntTimestamp());

		// check the page
		checkPage("eighth version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2, folder3}, true,
				new Page[] { page1, currentVersion});
	}

	/**
	 * Test reading the version after adding another entry to overview2
	 * @throws Exception
	 */
	@Test
	public void testNinthVersion() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// next version (added overview entry to overview2)
		Page oldVersion = (Page) t.getObject(Page.class, PAGE_ID, pageVersions[0].getDate().getIntTimestamp());

		// check the page
		checkPage("ninth version", oldVersion, CURRENT_PAGE_NAME, CURRENT_PAGE_CONTENT, true, new Folder[] { folder1, folder2, folder3}, true,
				new Page[] { page2, page1, currentVersion});
	}

	/**
	 * Test reading a page overview with individual sorting
	 * @throws Exception
	 */
	@Test
	public void testPageOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get oldest version
		Page oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[3].getDate().getIntTimestamp());

		// check the overview1 tag
		checkOverviewTag(OVERVIEW1_TAG_NAME, "first version", oldVersion.getTag(OVERVIEW1_TAG_NAME), new Page[] { page1, currentVersion, page2});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[2].getDate().getIntTimestamp());

		// check the overview1 tag
		checkOverviewTag(OVERVIEW1_TAG_NAME, "second version", oldVersion.getTag(OVERVIEW1_TAG_NAME), new Page[] { page2, currentVersion, page1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[1].getDate().getIntTimestamp());

		// check the overview1 tag
		checkOverviewTag(OVERVIEW1_TAG_NAME, "third version", oldVersion.getTag(OVERVIEW1_TAG_NAME), new Page[] { currentVersion, page2, page1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[0].getDate().getIntTimestamp());

		// check the overview1 tag
		checkOverviewTag(OVERVIEW1_TAG_NAME, "fourth version", oldVersion.getTag(OVERVIEW1_TAG_NAME), new Page[] { currentVersion, page1});
	}

	/**
	 * Test reading a folder overview with individual sorting
	 * @throws Exception
	 */
	@Test
	public void testFolderOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get oldest version
		Page oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[3].getDate().getIntTimestamp());

		// check the overview2 tag
		checkOverviewTag(OVERVIEW2_TAG_NAME, "first version", oldVersion.getTag(OVERVIEW2_TAG_NAME), new Folder[] { folder1, folder2, folder3});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[2].getDate().getIntTimestamp());

		// check the overview2 tag
		checkOverviewTag(OVERVIEW2_TAG_NAME, "second version", oldVersion.getTag(OVERVIEW2_TAG_NAME), new Folder[] { folder3, folder2, folder1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[1].getDate().getIntTimestamp());

		// check the overview2 tag
		checkOverviewTag(OVERVIEW2_TAG_NAME, "third version", oldVersion.getTag(OVERVIEW2_TAG_NAME), new Folder[] { folder2, folder3, folder1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[0].getDate().getIntTimestamp());

		// check the overview2 tag
		checkOverviewTag(OVERVIEW2_TAG_NAME, "fourth version", oldVersion.getTag(OVERVIEW2_TAG_NAME), new Folder[] { folder2, folder1});
	}

	/**
	 * Test reading a file overview with individual sorting
	 * @throws Exception
	 */
	@Test
	public void testFileOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get oldest version
		Page oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[3].getDate().getIntTimestamp());

		// check the overview3 tag
		checkOverviewTag(OVERVIEW3_TAG_NAME, "first version", oldVersion.getTag(OVERVIEW3_TAG_NAME), new File[] { file2, file3, file1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[2].getDate().getIntTimestamp());

		// check the overview3 tag
		checkOverviewTag(OVERVIEW3_TAG_NAME, "second version", oldVersion.getTag(OVERVIEW3_TAG_NAME), new File[] { file1, file3, file2});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[1].getDate().getIntTimestamp());

		// check the overview3 tag
		checkOverviewTag(OVERVIEW3_TAG_NAME, "third version", oldVersion.getTag(OVERVIEW3_TAG_NAME), new File[] { file3, file1, file2});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[0].getDate().getIntTimestamp());

		// check the overview3 tag
		checkOverviewTag(OVERVIEW3_TAG_NAME, "fourth version", oldVersion.getTag(OVERVIEW3_TAG_NAME), new File[] { file3, file2});
	}

	/**
	 * Test reading a image overview with individual sorting
	 * @throws Exception
	 */
	@Test
	public void testImageOverview() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get oldest version
		Page oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[3].getDate().getIntTimestamp());

		// check the overview4 tag
		checkOverviewTag(OVERVIEW4_TAG_NAME, "first version", oldVersion.getTag(OVERVIEW4_TAG_NAME), new ImageFile[] { image1, image2, image3});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[2].getDate().getIntTimestamp());

		// check the overview4 tag
		checkOverviewTag(OVERVIEW4_TAG_NAME, "second version", oldVersion.getTag(OVERVIEW4_TAG_NAME), new ImageFile[] { image3, image2, image1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[1].getDate().getIntTimestamp());

		// check the overview4 tag
		checkOverviewTag(OVERVIEW4_TAG_NAME, "third version", oldVersion.getTag(OVERVIEW4_TAG_NAME), new ImageFile[] { image2, image3, image1});

		// get the next version
		oldVersion = (Page) t.getObject(Page.class, OVERVIEW_PAGE_ID, overviewPageVersions[0].getDate().getIntTimestamp());

		// check the overview4 tag
		checkOverviewTag(OVERVIEW4_TAG_NAME, "fourth version", oldVersion.getTag(OVERVIEW4_TAG_NAME), new ImageFile[] { image2, image1});
	}

	/**
	 * Check whether the page contains the expected data
	 * @param message descriptive part of the assertion messages
	 * @param page page to check
	 * @param expectedName expected page name
	 * @param expectedContent expected page content
	 * @param expectOverview1Tag true when the overview1 tag is expected to exist
	 * @param expectedOverview1Entries expected overview1 entries
	 * @param expectOverview2Tag true when the overview2 tag is expected to exist
	 * @param expectedOverview2Entries expected overview2 entries
	 * @throws Exception
	 */
	protected void checkPage(String message, Page page, String expectedName,
			String expectedContent, boolean expectOverview1Tag,
			Folder[] expectedOverview1Entries, boolean expectOverview2Tag, Page[] expectedOverview2Entries) throws Exception {
		// check page name
		assertEquals("Check page name of " + message, expectedName, page.getName());

		// check page content
		Tag textTag = page.getTag(TAG_NAME);

		assertNotNull("Check text tag of " + message, textTag);
		assertEquals("Check text tag content of " + message, expectedContent, textTag.getValues().iterator().next().getValueText());

		// check page overview1 tag
		Tag overview1Tag = page.getTag(OVERVIEW1_TAG_NAME);

		if (expectOverview1Tag) {
			checkOverviewTag(OVERVIEW1_TAG_NAME, message, overview1Tag, expectedOverview1Entries);
		} else {
			assertNull("Check whether overview tag of " + message + " does not exists", overview1Tag);
		}

		// check page overview2 tag
		Tag overview2Tag = page.getTag(OVERVIEW2_TAG_NAME);

		if (expectOverview2Tag) {
			checkOverviewTag(OVERVIEW2_TAG_NAME, message, overview2Tag, expectedOverview2Entries);
		} else {
			assertNull("Check whether overview2 tag of " + message + " does not exists", overview2Tag);
		}
	}

	/**
	 * Check the given overview tag
	 * @param tagName expected tag name
	 * @param message message
	 * @param overviewTag overview tag
	 * @param expectedOverviewEntries expected overview entries
	 * @throws Exception
	 */
	protected void checkOverviewTag(String tagName, String message,
			Tag overviewTag, NodeObject[] expectedOverviewEntries) throws Exception {
		assertNotNull("Check whether " + tagName + " tag of " + message + " exists", overviewTag);
		OverviewPartType partType = (OverviewPartType) overviewTag.getValues().iterator().next().getPartType();
		Collection items = partType.getItems();

		assertEquals("Check number of " + tagName + " items of " + message, expectedOverviewEntries.length, items.size());
		int i = 0;

		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			NodeObject item = (NodeObject) iterator.next();

			assertEquals("Check " + tagName + " item #" + i + " of " + message, expectedOverviewEntries[i], item);
			i++;
		}
	}
}
