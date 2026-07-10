package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;

/**
 * Image tests for multichannelling support.
 *
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class ImageChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test inheritance and localizing of images.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testImageInheritanceAndLocalizing() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.file_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeRootFolder.getId());

		testFolder.save();
		t.commit(false);
		t.refreshPermHandler();

		// Create first test image in the root folder.
		ImageFile newImageFirst = t.createObject(ImageFile.class);

		newImageFirst.setDescription(C.image_channel_test.first_image_description);
		newImageFirst.setName(C.image_channel_test.first_image_name);
		newImageFirst.setFileStream(new ByteArrayInputStream(C.image_channel_test.first_image_data));
		newImageFirst.setFolderId(testFolder.getId());
		newImageFirst.save();

		// Create a second test file in the root folder.
		ImageFile newImageSecond = t.createObject(ImageFile.class);

		newImageSecond.setDescription(C.image_channel_test.second_image_description);
		newImageSecond.setName(C.image_channel_test.second_image_name);
		newImageSecond.setFileStream(new ByteArrayInputStream(C.image_channel_test.second_image_data));
		newImageSecond.setFolderId(testFolder.getId());

		// Create channel set id if there is no id set yet
		Integer channelSetId = newImageSecond.getChannelSetId();

		newImageSecond.save();
		t.commit(false);

		// Create a local copy of the second image and set it's channel info - a.k.a. make a
		// localized copy in the specified channel.
		ImageFile localCopy = (ImageFile) newImageSecond.copy();

		localCopy.setChannelInfo(channelId, channelSetId);

		localCopy.save();
		t.commit(false);

		// Execute rest request (without http) and retrieve all images for the test folder.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editableParameterBean = new EditableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		ImageListResponse imageListResponse = ContentNodeRESTUtils.getImageResource().list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, imageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the image list is null", imageListResponse.getItems());
		assertTrue("Check if the image list contains elements", imageListResponse.getItems().size() == 2);

		// Need these to check if we tested all the test cases below.
		boolean isFirstChecked = false;
		boolean isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.File image : imageListResponse.getItems()) {
			assertEquals("Check if file folder id is the same", testFolder.getId(), image.getFolderId());

			// Test if the first image has the correct data and is not inherited
			if (image.getId().equals(newImageFirst.getId())) {
				assertEquals("Check if image name is the same", C.image_channel_test.first_image_name, image.getName());
				assertEquals("Check if image description is the same", C.image_channel_test.first_image_description, image.getDescription());
				assertEquals("Check if the filesize is the same", C.image_channel_test.first_image_data.length, Integer.valueOf(image.getFileSize()).intValue());
				assertEquals("Check if the filetype is the same", C.image_channel_test.first_image_type, image.getFileType());
				assertTrue("Check if the image is inherited", image.isInherited());
				isFirstChecked = true;
			}

			// Test if the second image has the correct data and is n
			if (image.getId().equals(localCopy.getId())) {
				assertEquals("Check if image name is the same", C.image_channel_test.second_image_name, image.getName());
				assertEquals("Check if image description is the same", C.image_channel_test.second_image_description, image.getDescription());
				assertEquals("Check if the filesize is the same", C.image_channel_test.second_image_data.length, Integer.valueOf(image.getFileSize()).intValue());
				assertEquals("Check if the filetype is the same", C.image_channel_test.second_image_type, image.getFileType());
				assertTrue("Check if the image is inherited", !image.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both images", (isFirstChecked && isSecondChecked));
		// Check again with legacy endpoints.
		LegacyFileListResponse legacyImageListResponse = ContentNodeRESTUtils.getFolderResource().getImages(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyImageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the image list is null", legacyImageListResponse.getFiles());
		assertTrue("Check if the image list contains elements", legacyImageListResponse.getFiles().size() == 2);

		// Need these to check if we tested all the test cases below.
		isFirstChecked = false;
		isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.File image : legacyImageListResponse.getFiles()) {
			assertEquals("Check if file folder id is the same", testFolder.getId(), image.getFolderId());

			// Test if the first image has the correct data and is not inherited
			if (image.getId().equals(newImageFirst.getId())) {
				assertEquals("Check if image name is the same", C.image_channel_test.first_image_name, image.getName());
				assertEquals("Check if image description is the same", C.image_channel_test.first_image_description, image.getDescription());
				assertEquals("Check if the filesize is the same", C.image_channel_test.first_image_data.length, Integer.valueOf(image.getFileSize()).intValue());
				assertEquals("Check if the filetype is the same", C.image_channel_test.first_image_type, image.getFileType());
				assertTrue("Check if the image is inherited", image.isInherited());
				isFirstChecked = true;
			}

			// Test if the second image has the correct data and is n
			if (image.getId().equals(localCopy.getId())) {
				assertEquals("Check if image name is the same", C.image_channel_test.second_image_name, image.getName());
				assertEquals("Check if image description is the same", C.image_channel_test.second_image_description, image.getDescription());
				assertEquals("Check if the filesize is the same", C.image_channel_test.second_image_data.length, Integer.valueOf(image.getFileSize()).intValue());
				assertEquals("Check if the filetype is the same", C.image_channel_test.second_image_type, image.getFileType());
				assertTrue("Check if the image is inherited", !image.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both images", (isFirstChecked && isSecondChecked));
	}

	/**
	 * Test creating new image in a channel. Check that it's not in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testImageAsLocalObjectInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new image.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.file_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Refresh the permissions so we can use the new folder.
		t.refreshPermHandler();

		// Create test file in the channel.
		ImageFile newImageFirst = t.createObject(ImageFile.class);

		newImageFirst.setName(C.image_channel_test.first_image_name);
		newImageFirst.setDescription(C.image_channel_test.first_image_description);
		newImageFirst.setFileStream(new ByteArrayInputStream(C.image_channel_test.first_image_data));
		newImageFirst.setFolderId(testFolder.getId());

		// Set channel info for the new file.
		Integer channelSetId = newImageFirst.getChannelSetId();
		newImageFirst.setChannelInfo(channelId, channelSetId);
		newImageFirst.save();
		t.commit(false);

		// Get all files for our test folder in the channel we are working in.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editableParameterBean = new EditableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		ImageListResponse imageListResponse = getImageResource().list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, imageListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the image list is null", imageListResponse.getItems());
		assertTrue("Check if the image list contains elements", imageListResponse.getItems().size() == 1);
		assertEquals("Check if image folder id is the same", testFolder.getId(), imageListResponse.getItems().get(0).getFolderId());
		assertEquals("Check if image name is the same", C.image_channel_test.first_image_name, imageListResponse.getItems().get(0).getName());
		assertEquals("Check if image description is the same", C.image_channel_test.first_image_description, imageListResponse.getItems().get(0).getDescription());
		assertTrue("Check if the image is inherited", !imageListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoints.
		LegacyFileListResponse legacyFileListResponse = getFolderResource().getImages(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the image list is null", legacyFileListResponse.getFiles());
		assertTrue("Check if the image list contains elements", legacyFileListResponse.getFiles().size() == 1);
		assertEquals("Check if image folder id is the same", testFolder.getId(), legacyFileListResponse.getFiles().get(0).getFolderId());
		assertEquals("Check if image name is the same", C.image_channel_test.first_image_name, legacyFileListResponse.getFiles().get(0).getName());
		assertEquals("Check if image description is the same", C.image_channel_test.first_image_description, legacyFileListResponse.getFiles().get(0).getDescription());
		assertTrue("Check if the image is inherited", !legacyFileListResponse.getFiles().get(0).isInherited());

		// Get all files for test folder in the master node.
		FileListResponse fileListResponse = getFileResource().list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, fileListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the image list contains elements", fileListResponse.getItems().size() == 0);

		// Check again with legacy endpoints.
		legacyFileListResponse = getFolderResource().getFiles(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the image list contains elements", legacyFileListResponse.getFiles().size() == 0);
	}
}
