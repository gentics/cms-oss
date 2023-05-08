package com.gentics.contentnode.tests.multichannelling;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.tests.utils.OverviewHelper.ExpectedObject;

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
 * @author norbert
 */
public class OverviewPageChannelSandboxTest extends AbstractMultichannellingSandboxTest {
	private final static String RENDERING_TEMPLATE = "<node page.name> (<node page.id>)\n";
	
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
	
	/**
	 * Test overviews over pages, which are directly selected.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testPageOverviewSelected() throws Exception {
		// localize TestPage2 & publish localized copy
		com.gentics.contentnode.object.Page localTestPage2 = OverviewHelper.localizeAndPublishPage(channelId, C.overview_test_data.pages.page2_id);

		// localize TestPage4 & publish localized copy
		com.gentics.contentnode.object.Page localTestPage4 = OverviewHelper.localizeAndPublishPage(channelId, C.overview_test_data.testFolder2.pages.page4_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.pages.page2_id, C.overview_test_data.testFolder2.pages.page5_id,
				C.overview_test_data.pages.page1_id, C.overview_test_data.testFolder2.pages.page4_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.PAGE, SelectType.MANUAL, RENDERING_TEMPLATE, selectedItemsIds);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page2_name, C.overview_test_data.pages.page2_id),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id),
				objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.testFolder2.pages.page4_name, C.overview_test_data.testFolder2.pages.page4_id)));

		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page2_name, localTestPage2.getId(), true),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id),
				objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.testFolder2.pages.page4_name, localTestPage4.getId(), true)));
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, C.overview_test_data.testFolder2.pages.page4_id),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id),
				objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.pages.page2_name, C.overview_test_data.pages.page2_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, localTestPage4.getId(), true),
				objConvert(C.overview_test_data.pages.page2_name, localTestPage2.getId(), true),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id),
				objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), true);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		assertEquals("Check if the overvew changed after copying", overview, newOverview);
		overview = newOverview;

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over pages, which are from current folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testPageOverviewAuto() throws Exception {
		// localize TestPage2 & publish localized copy
		com.gentics.contentnode.object.Page localTestPage2 = OverviewHelper.localizeAndPublishPage(channelId, C.overview_test_data.pages.page2_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.PAGE, SelectType.AUTO, RENDERING_TEMPLATE, null);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.pages.page2_name, C.overview_test_data.pages.page2_id)));
		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.pages.page2_name, localTestPage2.getId(), true)));
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page2_name, C.overview_test_data.pages.page2_id),
				objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.pages.page1_name, C.overview_test_data.pages.page1_id),
				objConvert(C.overview_test_data.pages.page2_name, localTestPage2.getId(), true)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), false);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		assertEquals("Check if the overvew changed after copying", overview, newOverview);
		overview = newOverview;

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain,
				expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over pages, which are from selected folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testPageOverviewFolder() throws Exception {
		// localize TestPage4 & publish localized copy
		com.gentics.contentnode.object.Page localTestPage4 = OverviewHelper.localizeAndPublishPage(channelId, C.overview_test_data.testFolder2.pages.page4_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.testFolder2.folder_id);

		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.PAGE, SelectType.FOLDER, RENDERING_TEMPLATE, selectedItemsIds);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, C.overview_test_data.testFolder2.pages.page4_id),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id)));

		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, localTestPage4.getId(), true),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id)));
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, C.overview_test_data.testFolder2.pages.page4_id),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.pages.page4_name, localTestPage4.getId(), true),
				objConvert(C.overview_test_data.testFolder2.pages.page5_name, C.overview_test_data.testFolder2.pages.page5_id)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
		// Test localized overview page - reuse the output
		Page localizedTestPage = OverviewHelper.localizeAndPublishOverviewPage(channelId, testPage.getId(), true);
        
		// get preconfigured overview instance
		Overview newOverview = OverviewHelper.extractOverviewFromRestPage(localizedTestPage);

		// check if the overview has changed when the page is copied
		assertEquals("Check if the overvew changed after copying", overview, newOverview);
		overview = newOverview;
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain,
				expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
}
