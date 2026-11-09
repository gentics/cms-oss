package com.gentics.contentnode.tests.publish;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.testutils.GenericTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for searching files by online status
 */
@RunWith(value = Parameterized.class)
public class OnlineStatusSearchSandboxTest extends AbstractOfflinePublishSandboxTest {

	/**
	 * Array containing false and true
	 */
	public final static boolean[] FALSE_TRUE = { false, true};

	/**
	 * Array containing null, false and true
	 */
	public final static Boolean[] NULL_FALSE_TRUE = { null, Boolean.FALSE, Boolean.TRUE};

	/**
	 * Id of the master node
	 */
	protected final static int MASTER_ID = 10;

	/**
	 * Id of the channel
	 */
	protected static int channelId = -1;

	protected static Map<Boolean, Map<Boolean, Map<Boolean, Integer>>> FILE_IDS = new HashMap<Boolean, Map<Boolean, Map<Boolean, Integer>>>();

	/**
	 * True if searching an image, false for a file
	 */
	protected boolean image;

	/**
	 * True for searching online, False for searching offline, null for both
	 */
	protected Boolean searchOnline;

	/**
	 * Search Scope
	 */
	protected SearchScope searchScope;

	/**
	 * True when the file is online in the master
	 */
	protected boolean onlineInMaster;

	/**
	 * True when the file is online in the channel
	 */
	protected boolean onlineInChannel;

	/**
	 * True when the legacy folder resource getFiles/Images endpoint should be used.
	 */
	protected boolean useLegacyEndpoint;

	@BeforeClass
	public static void setUpOnce() throws Exception {
		prepareSandbox();

		// create the channel
		channelId = createChannel(MASTER_ID, "Test Channel", "This is the test channel", "channel", "/Content.Node");

		// create some files
		for (boolean image : FALSE_TRUE) {
			Map<Boolean, Map<Boolean, Integer>> imageMap = new HashMap<Boolean, Map<Boolean, Integer>>();

			FILE_IDS.put(image, imageMap);
			for (boolean onlineInMaster : FALSE_TRUE) {
				Map<Boolean, Integer> masterMap = new HashMap<Boolean, Integer>();

				imageMap.put(onlineInMaster, masterMap);
				for (boolean onlineInChannel : FALSE_TRUE) {
					masterMap.put(onlineInChannel, createFile(image, onlineInMaster, onlineInChannel));
				}
			}
		}
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: image {0}, search online {1}, scope {2}, online in master {3}, channel {4}, legacy endpoint {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (boolean image : FALSE_TRUE) {
			for (Boolean searchOnline : NULL_FALSE_TRUE) {
				for (SearchScope searchScope : SearchScope.values()) {
					for (boolean onlineInMaster : FALSE_TRUE) {
						for (boolean onlineInChannel : FALSE_TRUE) {
							for (boolean useLegacyEndpoint : FALSE_TRUE) {
								testData.add(new Object[] {
									image,
									searchOnline,
									searchScope,
									onlineInMaster,
									onlineInChannel,
									useLegacyEndpoint
								});
							}
						}
					}
				}
			}
		}
		return testData;
	}

	/**
	 * Create an instance with the given test parameters
	 * @param image true for images
	 * @param searchOnline whether searching for online, offline or all files
	 * @param searchScope search scope
	 * @param onlineInMaster true if file is online in master
	 * @param onlineInChannel true if file is online in channel
	 * @param useLegacyEndpoint true if legacy getFiles/Images endpoint should be used
	 */
	public OnlineStatusSearchSandboxTest(boolean image, Boolean searchOnline, SearchScope searchScope, boolean onlineInMaster,
			boolean onlineInChannel, boolean useLegacyEndpoint) {
		this.image = image;
		this.searchOnline = searchOnline;
		this.searchScope = searchScope;
		this.onlineInMaster = onlineInMaster;
		this.onlineInChannel = onlineInChannel;
	}

	/**
	 * Test the search for files
	 * @throws Exception
	 */
	@Test
	public void testSearch() throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master node, channel and subchannel
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		Folder masterRootFolder = masterNode.getFolder();
		Node channel = t.getObject(Node.class, channelId);
		Folder channelRootFolder = channel.getFolder();

		// make a search
		Integer nodeId = null;
		String folderId = null;
		List<Integer> expectedFoundFiles = new Vector<Integer>();

		switch (searchScope) {
		case MASTER:
			nodeId = ObjectTransformer.getInteger(masterNode.getId(), nodeId);
			folderId = masterRootFolder.getId().toString();
			break;

		case CHANNEL:
			nodeId = ObjectTransformer.getInteger(channel.getId(), nodeId);
			folderId = channelRootFolder.getId().toString();
			break;
		}

		// determine, which files we expect to find
		for (Boolean image : FILE_IDS.keySet()) {
			for (Boolean onlineInMaster : FILE_IDS.get(image).keySet()) {
				for (Boolean onlineInChannel : FILE_IDS.get(image).get(onlineInMaster).keySet()) {
					FILE_IDS.get(image).get(onlineInMaster).get(onlineInChannel);
				}
			}
		}

		if (ObjectTransformer.getBoolean(searchOnline, true)) {
			// we expect online files
			switch (searchScope) {
			case MASTER:
				// online in master
				expectedFoundFiles.add(FILE_IDS.get(image).get(true).get(true));
				expectedFoundFiles.add(FILE_IDS.get(image).get(true).get(false));
				break;

			case CHANNEL:
				// online in channel
				expectedFoundFiles.add(FILE_IDS.get(image).get(true).get(true));
				expectedFoundFiles.add(FILE_IDS.get(image).get(false).get(true));
				break;
			}
		}
		if (!ObjectTransformer.getBoolean(searchOnline, false)) {
			// we expect offline files
			switch (searchScope) {
			case MASTER:
				// offline in master
				expectedFoundFiles.add(FILE_IDS.get(image).get(false).get(true));
				expectedFoundFiles.add(FILE_IDS.get(image).get(false).get(false));
				break;

			case CHANNEL:
				// offline in channel
				expectedFoundFiles.add(FILE_IDS.get(image).get(true).get(false));
				expectedFoundFiles.add(FILE_IDS.get(image).get(false).get(false));
				break;
			}
		}

		// make the search
		List<? extends com.gentics.contentnode.rest.model.File> foundFiles = null;

		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folderId);
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(nodeId).setOnline(searchOnline);
		if (image) {
			if (useLegacyEndpoint) {
				LegacyFileListResponse imgResponse = ContentNodeRESTUtils.getFolderResource().getImages(
					inFolder.folderId,
					inFolder,
					fileListParams,
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				assertResponseCode(imgResponse);
				foundFiles = imgResponse.getFiles();
			} else {
				ImageListResponse imgResponse = ContentNodeRESTUtils.getImageResource().list(
					inFolder,
					fileListParams,
					new FilterParameterBean(),
					new SortParameterBean(),
					new PagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				assertResponseCode(imgResponse);
				foundFiles = imgResponse.getItems();
			}
		} else {
			if (useLegacyEndpoint) {
				LegacyFileListResponse fileResponse = ContentNodeRESTUtils.getFolderResource().getFiles(
					inFolder.folderId,
					inFolder,
					fileListParams,
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				assertResponseCode(fileResponse);
				foundFiles = fileResponse.getFiles();
			} else {
				FileListResponse fileResponse = ContentNodeRESTUtils.getFileResource().list(
					inFolder,
					fileListParams,
					new FilterParameterBean(),
					new SortParameterBean(),
					new PagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				assertResponseCode(fileResponse);
				foundFiles = fileResponse.getItems();
			}
		}

		// make the assertions

		// number of results
		assertEquals("Check # of found files", expectedFoundFiles.size(), foundFiles.size());

		// check that exactly the expected files were found
		for (com.gentics.contentnode.rest.model.File file : foundFiles) {
			assertTrue("Found unexpected File " + file.getId(), expectedFoundFiles.contains(file.getId()));
		}

		// check that all files had the correct online status
		if (searchOnline != null) {
			for (com.gentics.contentnode.rest.model.File file : foundFiles) {
				assertEquals("Check online status of file " + file.getId(), searchOnline, file.isOnline());
			}
		}
	}

	/**
	 * Create a channel of the given master TODO: migrate this to the NodeObject functions, when creating nodes and channels is implemented
	 *
	 * @param masterId
	 *            master node id
	 * @param name
	 *            name of the channel
	 * @param description
	 *            of the channel
	 * @param host
	 *            hostname of the channel
	 * @param pubDir
	 *            pubdir of the channel
	 * @return id of the channel
	 * @throws NodeException
	 */
	protected static int createChannel(int masterId, String name, String description, String host, String pubDir) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Node masterNode = t.getObject(Node.class, masterId);
		Folder rootFolder = masterNode.getFolder();
		Object channelSetId = rootFolder.getChannelSetId();

		// create the node
		List<Integer> keys = DBUtils.executeInsert("INSERT INTO node (host, pub_dir, utf8, publish_fs) VALUES (?, ?, ?, ?)",
				new Object[] { host, pubDir, true, true });

		assertEquals("Check number of generated rows", 1, keys.size());
		int channelId = keys.get(0);

		// create the root folder
		List<Integer> folderKeys = DBUtils.executeInsert(
				"INSERT INTO folder (name, description, creator, cdate, editor, edate, node_id, type_id, mother, pub_dir, master_id, channel_id, channelset_id, is_master) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] {
			name, description, t.getUserId(), t.getUnixTimestamp(), t.getUserId(), t.getUnixTimestamp(), channelId, Node.TYPE_CHANNEL, 0, pubDir,
			rootFolder.getId(), channelId, channelSetId, 0 });

		assertEquals("Check number of generated rows", 1, folderKeys.size());
		int folderId = folderKeys.get(0);

		DBUtils.executeUpdate("UPDATE node SET folder_id = ? WHERE id = ?", new Object[] { folderId, channelId });

		// get all groups and parent groups of the user
		SystemUser user = t.getObject(SystemUser.class, t.getUserId());
		List<UserGroup> allParents = new Vector<UserGroup>();
		List<UserGroup> userGroups = user.getUserGroups();

		allParents.addAll(userGroups);

		for (UserGroup userGroup : userGroups) {
			List<UserGroup> parents = userGroup.getParents();

			for (UserGroup parent : parents) {
				if (!allParents.contains(parent)) {
					allParents.add(parent);
				}
			}
		}
		// set permissions
		String perm = "11000000111111111111111111111111";

		for (UserGroup userGroup : allParents) {
			DBUtils.executeInsert("INSERT INTO perm (usergroup_id, o_type, o_id, perm) VALUES (?, ?, ?, ?)",
					new Object[] { userGroup.getId(), Folder.TYPE_FOLDER_INTEGER, folderId, perm });
			DBUtils.executeInsert("INSERT INTO perm (usergroup_id, o_type, o_id, perm) VALUES (?, ?, ?, ?)",
					new Object[] { userGroup.getId(), Node.TYPE_CHANNEL_INTEGER, folderId, perm });
		}

		t.refreshPermHandler();
		t.commit(false);
		return channelId;
	}

	/**
	 * Create a file in the master
	 * @param image true when the file should be an image
	 * @param onlineInMaster true when the file should be set online in the master
	 * @param onlineInChannel true when the file should be set online in the channel
	 * @return id of the file
	 * @throws NodeException
	 */
	protected static int createFile(boolean image, boolean onlineInMaster, boolean onlineInChannel) throws Exception {
		// prepare additional data
		Transaction t = TransactionManager.getCurrentTransaction();

		StringBuffer nameAppendix = new StringBuffer();

		nameAppendix.append(image ? "t" : "f").append(onlineInMaster ? "t" : "f").append(onlineInChannel ? "t" : "f");

		// get the master node, channel and subchannel
		Node masterNode = t.getObject(Node.class, MASTER_ID);
		Folder masterRootFolder = masterNode.getFolder();
		Node channel = t.getObject(Node.class, channelId);

		File file = null;

		if (image) {
			file = t.createObject(ImageFile.class);
			file.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			file.setName("blume_" + nameAppendix.toString() + ".jpg");
		} else {
			file = t.createObject(File.class);
			file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
			file.setName("textfile_" + nameAppendix.toString() + ".txt");
		}
		file.setFolderId(masterRootFolder.getId());
		file.save();
		t.commit(false);

		// set the file to be online in specific nodes
		FileOnlineStatus.setOnline(file, onlineInMaster);
		FileOnlineStatus.setOnline(file, channel, onlineInChannel);

		return ObjectTransformer.getInt(file.getId(), -1);
	}

	/**
	 * Assert the response
	 * @param response response
	 * @throws Exception if the response is not OK
	 */
	protected void assertResponseCode(GenericResponse response) throws Exception {
		assertTrue("Response returned with code " + response.getResponseInfo().getResponseCode() + " (" + response.getResponseInfo().getResponseMessage() + ")",
				ResponseCode.OK == response.getResponseInfo().getResponseCode());
	}

	/**
	 * Enumeration of search scopes
	 */
	public static enum SearchScope {
		MASTER, CHANNEL
	}
}
