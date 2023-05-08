package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.tests.utils.OverviewHelper.ExpectedObject;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * Overview Tests for multichannel support.
 * Test data is expected like following:
 * <ul>
 * <li>Folder OverviewTests (46)
 *   <ul>
 *     <li>Template OverviewTemplate (82)</li>
 *     <li>Page TestPage1 (56)</li>
 *     <li>Page TestPage2 (54)</li>
 *     <li>Page TestPage3 (55) [not published]</li>
 *     <li>Image TestImage1 (18)</li>
 *     <li>Image TestImage2 (17)</li>
 *     <li>File TestFile1 (22)</li>
 *     <li>File TestFile2 (21)</li>
 *     <li>Folder TestFolder1 (48)</li>
 *     <li>Folder TestFolder2 (47)
 *       <ul>
 *         <li>Page TestPage4 (59)</li>
 *         <li>Page TestPage5 (57)</li>
 *         <li>Page TestPage6 (58) [not published]</li>
 *         <li>Image TestImage3 (20)</li>
 *         <li>Image TestImage4 (19)</li>
 *         <li>File TestFile3 (24)</li>
 *         <li>File TestFile4 (23)</li>
 *       </ul>
 *     </li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
@RunWith(value = Parameterized.class)
public class OverviewFolderChannelSandboxTest extends AbstractMultichannellingSandboxTest {
	private final static String RENDERING_TEMPLATE = "<node folder.name> (<node folder.id>)\n";

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: recursive {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean recursive : Arrays.asList(true, false)) {
			data.add(new Object[] {recursive});
		}
		return data;
	}

	/**
	 * Flag to mark recursive folder selection
	 */
	protected boolean recursive;

	/**
	 * Generated subfolder, that will be listed, when the recursive flag is set
	 */
	protected Folder folder;

	/**
	 * Create test instance
	 * @param recursive true to select folders recursively
	 */
	public OverviewFolderChannelSandboxTest(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * Convert specified name and id values to an {@link ExpectedObject} instance,
	 * that is going to be used when generating expected string for rendering.
	 * 
	 * @param name The name property for the {@link ExpectedObject} instance.
	 * @param id The ID property for the {@link ExpectedObject} instance.
	 * @return the created {@link ExpectedObject} instance.
	 */
	private OverviewHelper.ExpectedObject objConvert(String name, int id) {
		return objConvert(name, ObjectTransformer.getInt(id, -1), false);
	}
	
	/**
	 * Convert specified name and id values to an {@link ExpectedObject} instance,
	 * that is going to be used when generating expected string for rendering.
	 * 
	 * @param name The name property for the {@link ExpectedObject} instance.
	 * @param id The ID property for the {@link ExpectedObject} instance.
	 * @param localized true if the expected object should be localized, false - otherwise.
	 * @return the created {@link ExpectedObject} instance.
	 */
	private OverviewHelper.ExpectedObject objConvert(String name, Object id, boolean localized) {
		return objConvert(name, ObjectTransformer.getInt(id, -1), localized);
	}
	
	/**
	 * Convert specified name and id values to an {@link ExpectedObject} instance,
	 * that is going to be used when generating expected string for rendering.
	 * 
	 * @param name The name property for the {@link ExpectedObject} instance.
	 * @param id The ID property for the {@link ExpectedObject} instance.
	 * @param localized true if the expected object should be localized, false - otherwise.
	 * @return the created {@link ExpectedObject} instance.
	 */
	private OverviewHelper.ExpectedObject objConvert(String name, int id, boolean localized) {
		if (localized) {
			return new OverviewHelper.ExpectedObject(name + "-localized", id);
		} else {
			return new OverviewHelper.ExpectedObject(name, id);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.tests.multichannelling.AbstractMultichannellingSandboxTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();

		// create a subfolder under Folder TestFolder1 (48)
		Transaction t = TransactionManager.getCurrentTransaction();
		folder = t.createObject(Folder.class);
		folder.setMotherId(C.overview_test_data.testFolder1.folder_id);
		folder.setName("New Recursive Folder");
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);
		folder = t.getObject(Folder.class, folder.getId());
	}

	/**
	 * Test overviews over folders, which are directly selected.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testFolderOverviewSelected() throws Exception {
		// localize TestFolder2 & publish localized copy
		com.gentics.contentnode.object.Folder localTestFolder2 = OverviewHelper.localizeFolder(channelId, C.overview_test_data.testFolder2.folder_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.folder_id, C.overview_test_data.testFolder1.folder_id,
				C.overview_test_data.testFolder2.folder_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.FOLDER, SelectType.MANUAL, RENDERING_TEMPLATE, selectedItemsIds, recursive);

		// create expected rendering result for main node
		String expectedOutputMain = null;
		if (recursive) {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0))));
		} else {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		}

		// create expected rendering result for channel node
		String expectedOutputChannel = null;
		if (recursive) {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0))));
		} else {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true)));
		}

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = null;
		if (recursive) {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id)));
		} else {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id)));
		}

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = null;
		if (recursive) {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id)));
		} else {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.folder_name, C.overview_test_data.folder_id)));
		}
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), true);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		// check if the overview has changed when the page is copied
		assertEquals("Check if the overvew changed after copyingg", overview, newOverview);
		overview = newOverview;

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over folders, which are from current folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testFolderOverviewAuto() throws Exception {
		// localize TestFolder2 & publish localized copy
		com.gentics.contentnode.object.Folder localTestFolder2 = OverviewHelper.localizeFolder(channelId, C.overview_test_data.testFolder2.folder_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.FOLDER, SelectType.AUTO, RENDERING_TEMPLATE, null, recursive);

		// create expected rendering result for main node
		String expectedOutputMain = null;
		if (recursive) {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		} else {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		}

		// create expected rendering result for channel node
		String expectedOutputChannel = null;
		if (recursive) {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true)));
		} else {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true)));
		}

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = null;
		if (recursive) {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0))));
		} else {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id)));
		}
        
		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = null;
		if (recursive) {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true)));
		} else {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, localTestFolder2.getId(), true)));
		}
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), true);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		// check if the overview has changed when the page is copied
		assertEquals("Check if the overvew changed aftercopyingng", overview, newOverview);
		overview = newOverview;
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain,
				expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over folders, which are from selected folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testFolderOverviewFolder() throws Exception {
		// localize TestFolder1 & publish localized copy
		com.gentics.contentnode.object.Folder localTestFolder1 = OverviewHelper.localizeFolder(channelId, C.overview_test_data.testFolder1.folder_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.folder_id);

		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.FOLDER, SelectType.FOLDER, RENDERING_TEMPLATE, selectedItemsIds, recursive);

		// create expected rendering result for main node
		String expectedOutputMain = null;
		if (recursive) {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		} else {
			expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		}

		// create expected rendering result for channel node
		String expectedOutputChannel = null;
		if (recursive) {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, localTestFolder1.getId(), true),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		} else {
			expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder1.folder_name, localTestFolder1.getId(), true),
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id)));
		}

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results
		String expectedOutputCdateMain = null;
		if (recursive) {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0))));
		} else {
			expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, C.overview_test_data.testFolder1.folder_id)));
		}

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = null;
		if (recursive) {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(folder.getName(), ObjectTransformer.getInt(folder.getId(), 0)),
					objConvert(C.overview_test_data.testFolder1.folder_name, localTestFolder1.getId(), true)));
		} else {
			expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(Arrays.asList(
					objConvert(C.overview_test_data.testFolder2.folder_name, C.overview_test_data.testFolder2.folder_id),
					objConvert(C.overview_test_data.testFolder1.folder_name, localTestFolder1.getId(), true)));
		}

		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), true);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		// check if the overview has changed when the page is copied
		assertEquals("Check if the overvew changed aftecopyinging", overview, newOverview);
		overview = newOverview;
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain,
				expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
}
