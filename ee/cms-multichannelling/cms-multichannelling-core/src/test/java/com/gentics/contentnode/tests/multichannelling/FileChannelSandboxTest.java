package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.FileCopyRequest;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.impl.MCFileResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;

/**
 * File tests for multichannelling support.
 *
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class FileChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test inheritance and localizing of files.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testFileInheritanceAndLocalizing() throws NodeException {
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

		// Create first test file in the root folder.
		File newFileFirst = t.createObject(File.class);

		newFileFirst.setDescription(C.file_channel_test.first_file_description);
		newFileFirst.setName(C.file_channel_test.first_file_name);
		newFileFirst.setFileStream(new ByteArrayInputStream(C.file_channel_test.first_file_data));
		newFileFirst.setFolderId(testFolder.getId());
		newFileFirst.save();

		// Create a second test file in the root folder.
		File newFileSecond = t.createObject(File.class);

		newFileSecond.setDescription(C.file_channel_test.second_file_description);
		newFileSecond.setName(C.file_channel_test.second_file_name);
		newFileSecond.setFileStream(new ByteArrayInputStream(C.file_channel_test.second_file_data));
		newFileSecond.setFolderId(testFolder.getId());

		// Create channel set id if there is no id set yet
		Integer channelSetId = newFileSecond.getChannelSetId();

		newFileSecond.save();
		t.commit(false);

		// Create a local copy of the second file and set it's channel info - a.k.a. make a
		// localized copy in the specified channel.
		File localCopy = (File) newFileSecond.copy();

		localCopy.setChannelInfo(channelId, channelSetId);

		localCopy.save();
		t.commit(false);

		// Execute rest request (without http) and retrieve all files for the test folder.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editableParameterBean = new EditableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		FileListResponse fileListResponse = ContentNodeRESTUtils.getFileResource().list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, fileListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the file list is null", fileListResponse.getItems());
		assertTrue("Check if the file list contains elements", fileListResponse.getItems().size() == 2);

		// Check again with legacy endpoints.
		LegacyFileListResponse legacyFileListResponse = ContentNodeRESTUtils.getFolderResource().getFiles(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the file list is null", legacyFileListResponse.getFiles());
		assertTrue("Check if the file list contains elements", legacyFileListResponse.getFiles().size() == 2);

		// Need these to check if we tested all the test cases below.
		boolean isFirstChecked = false;
		boolean isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.File file : fileListResponse.getItems()) {
			assertEquals("Check if file folder id is the same", testFolder.getId(), file.getFolderId());

			// Test if the first file has the correct data and is not inherited
			if (file.getId().equals(newFileFirst.getId())) {
				assertEquals("Check if file name is the same", C.file_channel_test.first_file_name, file.getName());
				assertEquals("Check if file description is the same", C.file_channel_test.first_file_description, file.getDescription());
				assertEquals("Check if the filesize is the same", C.file_channel_test.first_file_data.length, Integer.valueOf(file.getFileSize()).intValue());
				assertTrue("Check if the file is inherited", file.isInherited());
				isFirstChecked = true;
			}

			// Test if the second file has the correct data and is n
			if (file.getId().equals(localCopy.getId())) {
				assertEquals("Check if file name is the same", C.file_channel_test.second_file_name, file.getName());
				assertEquals("Check if file description is the same", C.file_channel_test.second_file_description, file.getDescription());
				assertEquals("Check if the filesize is the same", C.file_channel_test.second_file_data.length, Integer.valueOf(file.getFileSize()).intValue());
				assertTrue("Check if the file is inherited", !file.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both files", (isFirstChecked && isSecondChecked));
	}

	/**
	 * Test creating new file in a channel. Check that it's not in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testFileAsLocalObjectInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new file.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.file_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Refresh the permissions so we can use the new folder.
		t.refreshPermHandler();

		// Create test file in the channel.
		File newFileFirst = t.createObject(File.class);

		newFileFirst.setName(C.file_channel_test.first_file_name);
		newFileFirst.setDescription(C.file_channel_test.first_file_description);
		newFileFirst.setFileStream(new ByteArrayInputStream(C.file_channel_test.first_file_data));
		newFileFirst.setFolderId(testFolder.getId());

		// Set channel info for the new file.
		Integer channelSetId = newFileFirst.getChannelSetId();
		newFileFirst.setChannelInfo(channelId, channelSetId);

		newFileFirst.save();
		t.commit(false);

		// Get all files for our test folder in the channel we are working in.
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1)));
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editableParameterBean = new EditableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		FileListResponse fileListResponse = getFileResource().list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, fileListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the file list is null", fileListResponse.getItems());
		assertTrue("Check if the file list contains elements", fileListResponse.getItems().size() == 1);
		assertEquals("Check if file folder id is the same", testFolder.getId(), fileListResponse.getItems().get(0).getFolderId());
		assertEquals("Check if file name is the same", C.file_channel_test.first_file_name, fileListResponse.getItems().get(0).getName());
		assertEquals("Check if file description is the same", C.file_channel_test.first_file_description, fileListResponse.getItems().get(0).getDescription());
		assertTrue("Check if the file is inherited", !fileListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoint.
		LegacyFileListResponse legacyFileListResponse = getFolderResource().getFiles(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the file list is null", legacyFileListResponse.getFiles());
		assertTrue("Check if the file list contains elements", legacyFileListResponse.getFiles().size() == 1);
		assertEquals("Check if file folder id is the same", testFolder.getId(), legacyFileListResponse.getFiles().get(0).getFolderId());
		assertEquals("Check if file name is the same", C.file_channel_test.first_file_name, legacyFileListResponse.getFiles().get(0).getName());
		assertEquals("Check if file description is the same", C.file_channel_test.first_file_description, legacyFileListResponse.getFiles().get(0).getDescription());
		assertTrue("Check if the file is inherited", !legacyFileListResponse.getFiles().get(0).isInherited());

		// Get all files for test folder in the master node.
		fileListResponse = getFileResource().list(
			inFolder,
			new FileListParameterBean(),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, fileListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the file list contains elements", fileListResponse.getItems().size() == 0);

		// Check again with legacy endpoints.
		legacyFileListResponse = getFolderResource().getFiles(
			inFolder.folderId,
			inFolder,
			new FileListParameterBean(),
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);
		assertEquals("Check the reponse code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the file list contains elements", legacyFileListResponse.getFiles().size() == 0);
	}

	/**
	 * Test creation of a master file in a local folder.
	 * This is not possible, so the file must automatically be created in the channel of the local folder.
	 * @throws Exception
	 */
	@Test
	public void testCreateMasterFileInLocalFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create local test folder in the channel
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("Local folder");
		testFolder.setMotherId(masterNodeFolder.getId());
		testFolder.setChannelInfo(channelId, testFolder.getChannelSetId());

		testFolder.save();
		t.commit(false);

		// create a master file in the local test folder
		File newFile = t.createObject(File.class);
		newFile.setName(C.file_channel_test.first_file_name);
		newFile.setDescription(C.file_channel_test.first_file_description);
		newFile.setFileStream(new ByteArrayInputStream(C.file_channel_test.first_file_data));
		newFile.setFolderId(testFolder.getId());
		newFile.save();
		t.commit(false);

		// Get folder resource so we could use rest api to retrieve files.
		FileResource fileResource = ContentNodeRESTUtils.getFileResource();

		// get all inherited files
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1)));
		FileListParameterBean fileListParameterBean = new FileListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editableParameterBean = new EditableParameterBean();
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean();
		FileListResponse inheritedFilesResponse = fileResource.list(
			inFolder,
			fileListParameterBean.setOnline(true),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);
		assertResponseCode(inheritedFilesResponse);
		assertEquals("Check # of inherited files", 0, inheritedFilesResponse.getItems().size());

		// Check again with legacy endpoint.
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		LegacyFileListResponse legacyInheritedFilesResponse = folderResource.getFiles(
			inFolder.folderId,
			inFolder,
			fileListParameterBean.setOnline(true),
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);
		assertResponseCode(legacyInheritedFilesResponse);
		assertEquals("Check # of inherited files", 0, legacyInheritedFilesResponse.getFiles().size());

		// get all local files
		FileListResponse localFilesResponse = fileResource.list(
			inFolder,
			fileListParameterBean.setOnline(false),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editableParameterBean,
			wastebinParameterBean);
		assertResponseCode(localFilesResponse);
		assertEquals("Check # of local files", 1, localFilesResponse.getItems().size());

		// Check again with legacy endpoint.
		LegacyFileListResponse legacyLocalFilesResponse = folderResource.getFiles(
			inFolder.folderId,
			inFolder,
			fileListParameterBean.setOnline(false),
			filter,
			sort,
			paging,
			editableParameterBean,
			wastebinParameterBean);
		assertResponseCode(legacyLocalFilesResponse);
		assertEquals("Check # of local files", 1, localFilesResponse.getItems().size());

	}

	/**
	 * Files must never have an empty channelset after saving.
	 * @throws NodeException
	 */
	@Test
	public void testEmptyChannelsetFile() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node n = Creator.createNode("channelsettest", "channelsettest", "/", "/", Collections.<ContentLanguage> emptyList());
		File f = t.createObject(File.class);
		f.setFolderId(n.getFolder().getId());
		f.setName("blargh");
		f.setDescription("baer");
		f.getChannelSet();
		f.setFileStream(new ByteArrayInputStream(new byte[]{1,2,3,4,5}));
		f.save();
		assertFalse("Channelset must not be empty", f.getChannelSet().isEmpty());
	}

	/**
	 * Creates a file, tries loading and saving different settings of
	 * multichannelling exclusion and disinheriting.
	 * @throws Exception
	 */
	@Test
	public void fileMcExcludeTest() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentLanguage> noLanguage = Collections.emptyList();
		Node master = Creator.createNode("blub", "blub", "/", "/", noLanguage);
		Node channel = Creator.createNode("blub2", "blub2", "/", "/", noLanguage, master);

		// Create a file and check the default settings
		File f = Creator.createFile(master.getFolder(), "tst", new byte[] { 1, 2, 3, 4, 5 }, null);
		assertEquals("Exclusion state must be false by default", false, f.isExcluded());
		assertTrue("Disinherit list must be empty by default", f.getDisinheritedChannels().isEmpty());

		// Set a channel disinheritment
		f = t.getObject(File.class, f.getId(), true);
		f.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(new Node[]{channel})), false);

		f = t.getObject(File.class, f.getId());
		assertFalse("file must not be excluded", f.isExcluded());
		assertFalse("file must disinherit something", f.getDisinheritedChannels().isEmpty());
		assertEquals("channel node must be disinherited", channel, f.getDisinheritedChannels().iterator().next());

		// Set multichannelling exclusion (removes disinheriting)
		f = t.getObject(File.class, f.getId(), true);
		f.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
		f.save();
		f = t.getObject(File.class, f.getId());
		assertTrue("file must be excluded", f.isExcluded());
		assertTrue("file must disinherit nothing", f.getDisinheritedChannels().isEmpty());
	}

	/**
	 * Test copying a localized copy of a file using the REST API
	 * @throws Exception
	 */
	@Test
	public void testCopyLocalizedFile() throws Exception {
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Node channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));

		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", "Testfile contents".getBytes()));
		File localizedCopy = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, channel));

		Integer copyId = Trx.supply(() -> {
			FileCopyRequest request = new FileCopyRequest();
			com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
			fileModel.setId(localizedCopy.getId());
			request.setFile(fileModel);
			request.setNewFilename("testfile_copy.txt");

			FileUploadResponse response = getFileResource().copyFile(request);
			ContentNodeRESTUtils.assertResponseOK(response);

			com.gentics.contentnode.rest.model.File copy = response.getFile();
			assertThat(copy).as("Copy").isNotNull();
			return copy.getId();
		});

		// copy must be created and must be bound to the channel
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			File copy = t.getObject(File.class, copyId);
			assertThat(copy).as("Copy").isNotNull();
			assertThat(copy.getChannel()).as("Copy channel").isNotNull().isEqualTo(channel);
		});
}

	/**
	 * Test copying a localized file, that is restricted from another channel
	 * @throws Exception
	 */
	@Test
	public void testCopyLocalizedWithRestriction() throws Exception {
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Node channel1 = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel1", "channel1", "/"));
		Node channel11 = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(channel1, "Channel11", "channel11", "/"));
		Node channel2 = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel2", "channel2", "/"));

		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", "Testfile contents".getBytes()));
		Trx.operate(() -> {
			MCFileResourceImpl resource = new MCFileResourceImpl();
			try {
				DisinheritRequest request = new DisinheritRequest();
				request.setDisinherit(Arrays.asList(channel2.getId(), channel11.getId()));
				ContentNodeRESTUtils.assertResponseOK(resource.disinherit(file.getId().toString(), node.getId(), 0, request));
			} catch (Exception e) {
				fail(String.format("Disinheriting failed with %s", e.getLocalizedMessage()));
			}
		});
		File localizedCopy = Trx.supply(() -> ContentNodeTestDataUtils.localize(file, channel1));

		Integer copyId = Trx.supply(() -> {
			FileCopyRequest request = new FileCopyRequest();
			com.gentics.contentnode.rest.model.File fileModel = new com.gentics.contentnode.rest.model.File();
			fileModel.setId(localizedCopy.getId());
			request.setFile(fileModel);
			request.setNewFilename("testfile_copy.txt");

			FileUploadResponse response = getFileResource().copyFile(request);
			ContentNodeRESTUtils.assertResponseOK(response);

			com.gentics.contentnode.rest.model.File copy = response.getFile();
			assertThat(copy).as("Copy").isNotNull();
			return copy.getId();
		});

		// copy must be created and must be bound to channel1, but disinherited from channel11
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			File copy = t.getObject(File.class, copyId);
			assertThat(copy).as("Copy").isNotNull();
			assertThat(copy.getChannel()).as("Copy channel").isNotNull().isEqualTo(channel1);

			assertThat(copy.getDisinheritedChannels()).as("Copy disinherited in ").containsExactly(channel11);
		});
	}
}
