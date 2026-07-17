package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Folder tests for multichannelling support.
 */
public class FolderChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	@Test
	public void testInheritFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		// get the root folder
		Folder masterNodeFolder = masterNode.getFolder();

		// Create test folder for our the new folder.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// create a new folder as child of the masters root folder
		Folder newFolder = t.createObject(Folder.class);

		newFolder.setName(C.folder_channel_test.new_folder_name);
		newFolder.setMotherId(testFolder.getId());
		newFolder.save();
		t.commit(false);

		// now use the REST API to get the folders in the channel
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(String.valueOf(ObjectTransformer.getInt(testFolder.getId(), 0)));
		FolderListParameterBean folderListParams = new FolderListParameterBean().setNodeId(channelId);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse listResponse = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin
		);

		assertEquals("Check for response code", ResponseCode.OK, listResponse.getResponseInfo().getResponseCode());
		assertEquals("Check for number of inherited folders", 1, listResponse.getItems().size());
		assertEquals("Check for id of inherited folder", newFolder.getId(), listResponse.getItems().get(0).getId());
		assertEquals("Check for name of inherited folder", C.folder_channel_test.new_folder_name, listResponse.getItems().get(0).getName());
		assertTrue("Check whether folder is inherited", listResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoint.
		LegacyFolderListResponse legacyListResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertEquals("Check for response code", ResponseCode.OK, legacyListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check for number of inherited folders", 1, legacyListResponse.getFolders().size());
		assertEquals("Check for id of inherited folder", newFolder.getId(), legacyListResponse.getFolders().get(0).getId());
		assertEquals("Check for name of inherited folder", C.folder_channel_test.new_folder_name, legacyListResponse.getFolders().get(0).getName());
		assertTrue("Check whether folder is inherited", legacyListResponse.getFolders().get(0).isInherited());
	}

	/**
	 * Test creating new folder in a channel. Check that it's not in the master node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testFolderAsLocalObjectInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new folder.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Create test folder in the channel.
		Folder newFolder = t.createObject(Folder.class);

		newFolder.setMotherId(testFolder.getId());
		newFolder.setName(C.folder_channel_test.new_folder_name);

		// Set channel info for the new folder.
		Integer channelSetId = newFolder.getChannelSetId();
		newFolder.setChannelInfo(channelId, channelSetId);

		newFolder.save();
		t.commit(false);

		// Get all folder for our test folder in the channel we are working in.
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(String.valueOf(ObjectTransformer.getInt(testFolder.getId(), -1)));
		FolderListParameterBean folderListParameterBean = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean().setSortBy(null).setSortOrder("desc");
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse folderListResponse = getFolderResource().list(
			inFolder,
			folderListParameterBean.clone().setNodeId(Integer.valueOf(channelId)),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, folderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", folderListResponse.getItems());
		assertTrue("Check if the folder list contains elements", folderListResponse.getItems().size() == 1);
		assertEquals("Check if folder folder id is the same", testFolder.getId(), folderListResponse.getItems().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, folderListResponse.getItems().get(0).getName());
		assertTrue("Check if the folder is inherited", !folderListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoint.
		LegacyFolderListResponse legacyFolderListResponse = getFolderResource().getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean.clone().setNodeId(Integer.valueOf(channelId)),
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFolderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", legacyFolderListResponse.getFolders());
		assertTrue("Check if the folder list contains elements", legacyFolderListResponse.getFolders().size() == 1);
		assertEquals("Check if folder folder id is the same", testFolder.getId(), legacyFolderListResponse.getFolders().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, legacyFolderListResponse.getFolders().get(0).getName());
		assertTrue("Check if the folder is inherited", !legacyFolderListResponse.getFolders().get(0).isInherited());

		// Get all folders for test folder in the master node.
		folderListResponse = getFolderResource().list(
			inFolder,
			folderListParameterBean,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, folderListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the folder list contains elements", folderListResponse.getItems().size() == 0);

		// Check again with legacy endpoint.
		legacyFolderListResponse = getFolderResource().getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean,
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFolderListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the folder list contains elements", legacyFolderListResponse.getFolders().size() == 0);
	}

	/**
	 * Test localizing of folders.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testFolderLocalizing() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeRootFolder.getId());

		testFolder.save();
		t.commit(false);

		// Create new folder in the test folder.
		Folder newFolder = t.createObject(Folder.class);

		newFolder.setMotherId(testFolder.getId());
		newFolder.setName(C.folder_channel_test.new_folder_name);

		// Create channel set id if there is no id set yet
		Integer channelSetId = newFolder.getChannelSetId();

		newFolder.save();
		t.commit(false);

		// Create a local copy of the test folder and set it's channel info - a.k.a. make a
		// localized copy in the specified channel.
		Folder localCopy = (Folder) newFolder.copy();

		localCopy.setChannelInfo(channelId, channelSetId);

		localCopy.save();
		t.commit(false);

		// Execute rest request (without http) and retrieve all folders for the test folder
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(String.valueOf(ObjectTransformer.getInt(testFolder.getId(), -1)));
		FolderListParameterBean folderListParameterBean = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse folderListResponse = getFolderResource().list(
			inFolder,
			folderListParameterBean.clone().setNodeId(channelId),
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, folderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", folderListResponse.getItems());
		assertTrue("Check if the folder list contains elements", folderListResponse.getItems().size() == 1);
		assertEquals("Check if folder mother id is the same", testFolder.getId(), folderListResponse.getItems().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, folderListResponse.getItems().get(0).getName());
		assertEquals("Check if folder id is the same", localCopy.getId(), folderListResponse.getItems().get(0).getId());
		assertTrue("Check if the folder is inherited", !folderListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoint.
		LegacyFolderListResponse legacyFolderListResponse = getFolderResource().getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean.clone().setNodeId(channelId),
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFolderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", legacyFolderListResponse.getFolders());
		assertTrue("Check if the folder list contains elements", legacyFolderListResponse.getFolders().size() == 1);
		assertEquals("Check if folder mother id is the same", testFolder.getId(), legacyFolderListResponse.getFolders().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, legacyFolderListResponse.getFolders().get(0).getName());
		assertEquals("Check if folder id is the same", localCopy.getId(), legacyFolderListResponse.getFolders().get(0).getId());
		assertTrue("Check if the folder is inherited", !legacyFolderListResponse.getFolders().get(0).isInherited());

		folderListResponse = getFolderResource().list(
			inFolder,
			folderListParameterBean,
			filter.toFilterParameterBean(),
			sort.setSortBy(null).setSortOrder("desc").toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, folderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", folderListResponse.getItems());
		assertTrue("Check if the folder list contains elements", folderListResponse.getItems().size() == 1);
		assertEquals("Check if folder mother id is the same", testFolder.getId(), folderListResponse.getItems().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, folderListResponse.getItems().get(0).getName());
		assertEquals("Check if folder id is the same", newFolder.getId(), folderListResponse.getItems().get(0).getId());
		assertTrue("Check if the folder is inherited", !folderListResponse.getItems().get(0).isInherited());

		// Check again with legacy endpoint.
		legacyFolderListResponse = getFolderResource().getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean,
			filter,
			sort.setSortBy(null).setSortOrder("desc"),
			paging,
			editable,
			wastebin);

		assertEquals("Check the reponse code", ResponseCode.OK, legacyFolderListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the folder list is null", legacyFolderListResponse.getFolders());
		assertTrue("Check if the folder list contains elements", legacyFolderListResponse.getFolders().size() == 1);
		assertEquals("Check if folder mother id is the same", testFolder.getId(), legacyFolderListResponse.getFolders().get(0).getMotherId());
		assertEquals("Check if folder name is the same", C.folder_channel_test.new_folder_name, legacyFolderListResponse.getFolders().get(0).getName());
		assertEquals("Check if folder id is the same", newFolder.getId(), legacyFolderListResponse.getFolders().get(0).getId());
		assertTrue("Check if the folder is inherited", !legacyFolderListResponse.getFolders().get(0).isInherited());
	}

	/**
	 * Test deleting a local and a inherited folder in a channel
	 * @throws Exception
	 */
	@Test
	public void testdeleteFolderFromChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// get the channel node that inherits the master node
		Node channel = t.getObject(Node.class, channelId);

		// get the root folder of the master
		Folder masterNodeFolder = masterNode.getFolder();

		// get the root folder of the channel
		Folder channelFolder = channel.getFolder();

		// Create a local test folder in the channel
		Folder localFolder = t.createObject(Folder.class);

		localFolder.setName(C.folder_channel_test.test_folder_name);
		localFolder.setMotherId(channelFolder.getId());

		// Set channel info for the new folder.
		Integer channelSetId = localFolder.getChannelSetId();
		localFolder.setChannelInfo(channelId, channelSetId);
		localFolder.save();
		t.commit(false);

		// Create an inherited test folder in the master
		Folder inheritedFolder = t.createObject(Folder.class);

		inheritedFolder.setName(C.folder_channel_test.test_folder_name);
		inheritedFolder.setMotherId(masterNodeFolder.getId());

		inheritedFolder.save();
		t.commit(false);

		// now use the REST API to get the folders in the channel

		// Test deleting the local folder in the channel
		assertEquals("Check response code", ResponseCode.OK,
				getFolderResource().delete(localFolder.getId().toString(), channelId, null).getResponseInfo().getResponseCode());

		assertError(() -> getFolderResource().delete(inheritedFolder.getId().toString(), channelId, null),
				NodeException.class, ResponseCode.FAILURE,
				"Can't delete an inherated folder, the folder has to be deleted in the master node.");

		// Delete from the master
		assertEquals("Check response code", ResponseCode.OK,
				getFolderResource().delete(inheritedFolder.getId().toString(), MASTER_ID, null).getResponseInfo().getResponseCode());
	}

	/**
	 * test creating two folders with the same name in two unrelated channel nodes (no channel is an ancestor of the other)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testSideChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node sideChannel = t.getObject(Node.class, sideChannelId);
		Folder channelFolder = channel.getFolder();
		Folder sideChannelFolder = sideChannel.getFolder();

		Folder f1 = t.createObject(Folder.class);

		f1.setChannelInfo(channel.getId(), null);
		f1.setName("testfolder");
		f1.setMotherId(channelFolder.getId());

		Folder f2 = t.createObject(Folder.class);

		f2.setChannelInfo(sideChannel.getId(), null);
		f2.setName("testfolder");
		f2.setMotherId(sideChannelFolder.getId());

		f1.save();
		f2.save();

		assertEquals("testfolder", f1.getName());
		assertEquals("testfolder", f2.getName());
	}

	/**
	 * test creating two folders with the same name in two related channel nodes (second is an descendant of the first)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testDescendantChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node subChannel = t.getObject(Node.class, subChannelId);
		Folder channelFolder = channel.getFolder();
		Folder subChannelFolder = subChannel.getFolder();

		Folder f1 = t.createObject(Folder.class);

		f1.setChannelInfo(channel.getId(), null);
		f1.setName("testfolder");
		f1.setMotherId(channelFolder.getId());

		Folder f2 = t.createObject(Folder.class);

		f2.setChannelInfo(subChannel.getId(), null);
		f2.setName("testfolder");
		f2.setMotherId(subChannelFolder.getId());

		f1.save();
		f2.save();

		assertEquals("testfolder", f1.getName());
		assertEquals("testfolder1", f2.getName());
	}

	/**
	 * test creating two folders with the same name in two related channel nodes (second is an ancestor of the first)
	 *
	 * @throws NodeException
	 */
	@Test
	public void testAncestorChannelSameNameCreation() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node channel = t.getObject(Node.class, channelId);
		Node subChannel = t.getObject(Node.class, subChannelId);
		Folder channelFolder = channel.getFolder();
		Folder subChannelFolder = subChannel.getFolder();

		Folder f1 = t.createObject(Folder.class);

		f1.setChannelInfo(subChannel.getId(), null);
		f1.setName("testfolder");
		f1.setMotherId(subChannelFolder.getId());

		Folder f2 = t.createObject(Folder.class);

		f2.setChannelInfo(channel.getId(), null);
		f2.setName("testfolder");
		f2.setMotherId(channelFolder.getId());

		f1.save();
		f2.save();

		assertEquals("testfolder", f1.getName());
		assertEquals("testfolder1", f2.getName());
	}

	/**
	 * Test deleting a master folder that contains templates, which have been localized (but the templates are also linked to other folders)
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolderWithLocalizedTemplates() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// create a template in the master folder
		Template doubleLinkedTemplate = t.createObject(Template.class);
		doubleLinkedTemplate.setFolderId(masterNodeRootFolder.getId());
		doubleLinkedTemplate.setName("Double Linked Template");
		doubleLinkedTemplate.setSource("Source");
		doubleLinkedTemplate.getFolders().add(masterNodeRootFolder);
		doubleLinkedTemplate.save();
		t.commit(false);

		// localize the template in the channel
		Template doubleLinkedTemplateCopy = (Template)doubleLinkedTemplate.copy();
		doubleLinkedTemplateCopy.setChannelInfo(channelId, doubleLinkedTemplate.getChannelSetId());
		doubleLinkedTemplateCopy.save();
		t.commit(false);

		// create a test folder
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName(C.folder_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);

		// make sure the template is linked to the folder
		doubleLinkedTemplate = t.getObject(Template.class, doubleLinkedTemplate.getId(), true);
		List<Folder> templateFolders = doubleLinkedTemplate.getFolders();
		if (!templateFolders.contains(testFolder)) {
			templateFolders.add(testFolder);
			doubleLinkedTemplate.save();
			t.commit(false);
		}

		// create another template, which is only linked to the test folder
		Template singleLinkedTemplate = t.createObject(Template.class);
		singleLinkedTemplate.setFolderId(testFolder.getId());
		singleLinkedTemplate.setName("Single Linked Template");
		singleLinkedTemplate.setSource("Source");
		singleLinkedTemplate.getFolders().add(testFolder);
		singleLinkedTemplate.save();
		t.commit(false);

		// localize the template in the channel
		Template singleLinkedTemplateCopy = (Template)singleLinkedTemplate.copy();
		singleLinkedTemplateCopy.setChannelInfo(channelId, singleLinkedTemplate.getChannelSetId());
		singleLinkedTemplateCopy.save();
		t.commit(false);

		// now delete the folder
		testFolder = t.getObject(Folder.class, testFolder.getId());
		testFolder.delete();
		t.commit(false);

		// template and localized copy must still exist
		assertNotNull("Double Linked master template does not exist any more", t.getObject(Template.class, doubleLinkedTemplate.getId()));
		assertNotNull("Double Linked localized template does not exist any more", t.getObject(Template.class, doubleLinkedTemplateCopy.getId()));
		assertNull("Single Linked master template still exists", t.getObject(Template.class, singleLinkedTemplate.getId()));
		assertNull("Single Linked localized template still exists", t.getObject(Template.class, singleLinkedTemplateCopy.getId()));
	}

	/**
	 * Test creation of a master folder in a local folder.
	 * This is not possible, so the folder must automatically be created in the channel of the local folder.
	 * @throws Exception
	 */
	@Test
	public void testCreateMasterFolderInLocalFolder() throws Exception {
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

		// create a master folder in the local test folder
		Folder newFolder = t.createObject(Folder.class);
		newFolder.setName("New folder");
		newFolder.setMotherId(testFolder.getId());
		newFolder.save();
		t.commit(false);

		// Get folder resource so we can use rest api to retrieve files.
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		// get all inherited folders
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(ObjectTransformer.getString(testFolder.getId(), null));
		FolderListParameterBean folderListParameterBean = new FolderListParameterBean().setNodeId(channelId).setInherited(true);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sort = new LegacySortParameterBean().setSortBy(null);
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse inheritedFoldersResponse = folderResource.list(
			inFolder,
			folderListParameterBean,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertResponseCode(inheritedFoldersResponse);
		assertEquals("Check # of inherited folders", 0, inheritedFoldersResponse.getItems().size());

		// Check again with legacy endpoint.
		LegacyFolderListResponse legacyInheritedFoldersResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean,
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertResponseCode(inheritedFoldersResponse);
		assertEquals("Check # of inherited folders", 0, legacyInheritedFoldersResponse.getFolders().size());

		// get all local folders
		folderListParameterBean = new FolderListParameterBean().setNodeId(channelId);

		FolderListResponse localFoldersResponse = folderResource.list(
			inFolder,
			folderListParameterBean,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertResponseCode(localFoldersResponse);
		assertEquals("Check # of local folders", 1, localFoldersResponse.getItems().size());

		// Check again with legacy endpoint.
		LegacyFolderListResponse legacyLocalFoldersResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParameterBean.recursiveIds,
			folderListParameterBean.addPrivileges,
			inFolder,
			folderListParameterBean,
			filter,
			sort,
			paging,
			editable,
			wastebin);

		assertResponseCode(localFoldersResponse);
		assertEquals("Check # of local folders", 1, legacyLocalFoldersResponse.getFolders().size());
	}

	/**
	 * Folders must never have an empty channelset after saving.
	 * @throws NodeException
	 */
	@Test
	public void testEmptyChannelsetFolder() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node n = Creator.createNode("channelsettest", "channelsettest", "/", "/", Collections.<ContentLanguage> emptyList());
		Folder f = t.createObject(Folder.class);
		f.setMotherId(n.getFolder().getId());
		f.setName("blargh");
		f.setDescription("baer");
		f.getChannelSet();
		f.save();
		assertFalse("Channelset must not be empty", f.getChannelSet().isEmpty());
	}

	@Test
	@GCNFeature(unset = Feature.MULTICHANNELLING)
	public void testListNodes() throws Exception {
		try (Trx trx = new Trx(Trx.supply(t -> t.getObject(SystemUser.class, 1)))) {
			FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

			LegacyFolderListResponse response = folderResource.getFolders(
				"0",
				Collections.emptyList(),
				false,
				new InFolderParameterBean(),
				new FolderListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());

			assertResponseCode(response);
		}
	}
}
