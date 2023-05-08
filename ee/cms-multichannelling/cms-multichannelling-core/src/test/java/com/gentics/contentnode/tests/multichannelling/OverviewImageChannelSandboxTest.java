package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.tests.utils.OverviewHelper;
import com.gentics.contentnode.tests.utils.OverviewHelper.ExpectedObject;
import com.gentics.contentnode.rest.model.Page;

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
public class OverviewImageChannelSandboxTest extends AbstractMultichannellingSandboxTest {
	private final static String RENDERING_TEMPLATE = "<node image.name> (<node image.id>)\n";
	
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
			String words[] = name.split("\\.");

			name = words[0] + "-localized." + words[1];
			return new OverviewHelper.ExpectedObject(name, id);
		} else {
			return new OverviewHelper.ExpectedObject(name, id);
		}
	}
	
	/**
	 * Test overviews over images, which are directly selected.
	 * 
	 * @throws Exception if anything goes wrong.
	 */
	@Test
	public void testImageOverviewSelected() throws Exception {
		// localize TestImage1 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage1 = OverviewHelper.localizeImage(channelId, C.overview_test_data.images.image1_id);
        
		// localize TestImage4 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage4 = OverviewHelper.localizeImage(channelId, C.overview_test_data.testFolder2.images.image4_id);

		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.images.image1_id, C.overview_test_data.images.image2_id,
				C.overview_test_data.testFolder2.images.image3_id, C.overview_test_data.testFolder2.images.image4_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.IMAGE, SelectType.MANUAL, RENDERING_TEMPLATE, selectedItemsIds);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id)));
        
		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true)));  
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id),
				objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true),
				objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id)));
        
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
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.SELF, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.DESC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over images, which are from current folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testImageOverviewAuto() throws Exception {
		// localize TestImage1 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage1 = OverviewHelper.localizeImage(channelId, C.overview_test_data.images.image1_id);
        
		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
        
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.IMAGE, SelectType.AUTO, RENDERING_TEMPLATE, null);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id)));

		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id)));
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
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
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over images, which are from current folder - recursive.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testImageOverviewAutoRecursive() throws Exception {
		// localize TestImage1 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage1 = OverviewHelper.localizeImage(channelId, C.overview_test_data.images.image1_id);
          
		// localize TestImage4 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage4 = OverviewHelper.localizeImage(channelId, C.overview_test_data.testFolder2.images.image4_id);
	
		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
	  
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.IMAGE, SelectType.AUTO, RENDERING_TEMPLATE, null, true);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id)));

		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true),
				objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true)));

		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);
        
		// render the page again
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.images.image1_name, C.overview_test_data.images.image1_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id)));

		// render the page for the channel
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.images.image2_name, C.overview_test_data.images.image2_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.images.image1_name, localTestImage1.getId(), true),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
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
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
    
	/**
	 * Test overviews over images, which are from selected folder.
	 * 
	 * @throws Exception if something goes wrong.
	 */
	@Test
	public void testImageOverviewFolder() throws Exception {
		// localize TestImage4 & publish localized copy
		com.gentics.contentnode.object.ImageFile localTestImage4 = OverviewHelper.localizeImage(channelId, C.overview_test_data.testFolder2.images.image4_id);
	
		// create a new page over the REST API
		Page testPage = OverviewHelper.createPage(C.overview_test_data.folder_id, C.overview_test_data.templates.template1_id);
    	
		// create a list with IDs for items to select
		List<Integer> selectedItemsIds = Arrays.asList(C.overview_test_data.testFolder2.folder_id);
	  
		// get the overview
		Overview overview = OverviewHelper.getConfiguredOverviewFromPage(testPage, ListType.IMAGE, SelectType.FOLDER, RENDERING_TEMPLATE, selectedItemsIds);

		// create expected rendering result for main node
		String expectedOutputMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id)));

		// create expected rendering result for channel node
		String expectedOutputChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true)));
        
		// render for main and channel nodes and check the expected results
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.ALPHABETICALLY, OrderDirection.ASC, expectedOutputMain, expectedOutputChannel);

		// create expected rendering result for main node with different ordering
		String expectedOutputCdateMain = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image4_name, C.overview_test_data.testFolder2.images.image4_id),
				objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id)));

		// create expected rendering result for channel node with different ordering
		String expectedOutputCdateChannel = OverviewHelper.generateExpectedRenderingOutput(
				Arrays.asList(objConvert(C.overview_test_data.testFolder2.images.image3_name, C.overview_test_data.testFolder2.images.image3_id),
				objConvert(C.overview_test_data.testFolder2.images.image4_name, localTestImage4.getId(), true)));
        
		// render for main and channel nodes and check the expected results for different ordering
		OverviewHelper.renderingTest(testPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain, expectedOutputCdateChannel);
        
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
		OverviewHelper.renderingTest(localizedTestPage, channelId, overview, OrderBy.CDATE, OrderDirection.ASC, expectedOutputCdateMain,
				expectedOutputCdateChannel);
	}
}
