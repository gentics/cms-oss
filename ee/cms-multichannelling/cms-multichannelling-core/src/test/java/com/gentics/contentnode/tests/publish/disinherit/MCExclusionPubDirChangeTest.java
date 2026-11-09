package com.gentics.contentnode.tests.publish.disinherit;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.DisinheritUtils;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for conflict checking before changing the publish directory of a folder
 */
@RunWith(value = Parameterized.class)
public class MCExclusionPubDirChangeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test data
	 */
	private static Node master;
	private static Node channel;
	private static Map<String, Map<Node, Folder>> obstructorMaps = new HashMap<String, Map<Node,Folder>>();
	private static Map<String, Map<Node, Folder>> toChangeMaps = new HashMap<String, Map<Node,Folder>>();

	private Inheritance folderInheritance;
	private Inheritance fileInheritance;
	private boolean conflict;
	private Inheritance changeInheritance;
	private Inheritance changeFileInheritance;

	/**
	 * Get the test variation data
	 * @return test variation data
	 */
	@Parameters(name = "{index}: folder {0}, file {1}, conflict {2}, changefolder {3}, changefile {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (Inheritance folderInheritance : Inheritance.values()) {
			for (Inheritance fileInheritance : Inheritance.values()) {
				if (!folderInheritance.allows(fileInheritance)) {
					continue;
				}
				for (boolean conflict : Arrays.asList(true, false)) {
					for (Inheritance changeInheritance : Inheritance.values()) {
						for (Inheritance changeFileInheritance : Inheritance.values()) {
							if (!changeInheritance.allows(changeFileInheritance)) {
								continue;
							}
							if (changeFileInheritance == Inheritance.localized_different) {
								continue;
							}
							data.add(new Object[] {folderInheritance, fileInheritance, conflict, changeInheritance, changeFileInheritance});
						}
					}
				}
			}
		}

		return data;
	}

	/**
	 * Create a test instance
	 * @param folderInheritance inheritance setting for the folder containing the obstructing file
	 * @param fileInheritance inheritance setting for the obstructing file
	 * @param conflict true if the obstructing file has the same name as the file in the changed folder
	 * @param changeInheritance inheritance setting for the folder that is changed
	 * @param changeFileInheritance inheritance setting for the file in the folder that is changed
	 */
	public MCExclusionPubDirChangeTest(Inheritance folderInheritance, Inheritance fileInheritance, boolean conflict, Inheritance changeInheritance,
			Inheritance changeFileInheritance) {
		this.folderInheritance = folderInheritance;
		this.fileInheritance = fileInheritance;
		this.conflict = conflict;
		this.changeInheritance = changeInheritance;
		this.changeFileInheritance = changeFileInheritance;
	}

	/**
	 * Create the test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setup() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		master = ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.NONE);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "chanel", "/Content.node", PublishTarget.NONE);

		for (Inheritance folderInheritance : Inheritance.values()) {
			for (Inheritance fileInheritance : Inheritance.values()) {
				if (!folderInheritance.allows(fileInheritance)) {
					continue;
				}
				Node folderChannel = folderInheritance.existsInMaster ? master : channel;
				String fileName = "file.txt";

				String changePubDir = getPubDir(folderInheritance, fileInheritance, false);
				String changeName = changePubDir.replaceAll("/", ".");
				toChangeMaps.put(changePubDir, createFolder(folderInheritance, changeName + ".toChange", changePubDir + "/toChange"));
				createFile(toChangeMaps.get(changePubDir).get(folderChannel), fileName, fileInheritance);

				for (boolean conflict : Arrays.asList(true, false)) {
					String pubDir = getPubDir(folderInheritance, fileInheritance, conflict);
					String name = pubDir.replaceAll("/", ".");
					obstructorMaps.put(pubDir, createFolder(folderInheritance, name, pubDir));

					String obstructorFileName = conflict ? fileName : "other" + fileName;
					createFile(obstructorMaps.get(pubDir).get(folderChannel), obstructorFileName, fileInheritance);
				}
			}
		}

		reReadObjects();
	}

	/**
	 * Test whether the pub dir of the folder can be changed without creating a duplicate
	 * @throws Exception
	 */
	@Test
	public void testCheckConsistency() throws Exception {
		String newPubDir = getPubDir(folderInheritance, fileInheritance, conflict);
		String oldPubDir = getPubDir(changeInheritance, changeFileInheritance, false);
		for (Folder folder : toChangeMaps.get(oldPubDir).values()) {
			if (folder == null) {
				continue;
			}
			boolean expectConflict = conflict;
			// no conflict, if files do not interfere (they do not exist in the same channels)
			if (expectConflict && fileInheritance.existsInMaster != changeFileInheritance.existsInMaster
					&& fileInheritance.existsInChannel != changeFileInheritance.existsInChannel) {
				expectConflict = false;
			}
			if (expectConflict && fileInheritance == Inheritance.local && changeFileInheritance == Inheritance.localized_different) {
				expectConflict = false;
			}
			if (expectConflict && changeFileInheritance == Inheritance.local && fileInheritance == Inheritance.localized_different) {
				expectConflict = false;
			}
			// no conflict if folder to change is localized and file does not exist in channel (or has different name) and we want to change the localized folder
			if (expectConflict && changeInheritance.localized
					&& (!changeFileInheritance.existsInChannel || changeFileInheritance == Inheritance.localized_different) && folder.getChannel() != null) {
				expectConflict = false;
			}
			if (expectConflict && changeInheritance.localized
					&& (!fileInheritance.existsInChannel || fileInheritance == Inheritance.localized_different) && folder.getChannel() != null) {
				expectConflict = false;
			}
			// no conflict, if folder to change is localized and we change the master folder and the file does not exist in the master
			if (expectConflict && changeInheritance.localized && (!changeFileInheritance.existsInMaster || !fileInheritance.existsInMaster) && folder.isMaster()) {
				expectConflict = false;
			}
			// no conflict, if folder is localized or local and file has different name or does not exist
			if (expectConflict && folder.getChannel() != null) {
				try (ChannelTrx trx = new ChannelTrx(folder.getChannel())) {
					List<File> files = folder.getFilesAndImages();
					expectConflict = false;
					for (File file : files) {
						if ("file.txt".equals(file.getName())) {
							expectConflict = true;
							break;
						}
					}
				}
			}
			// no conflict if changing folder in channel and file does not exist in channel
			if (expectConflict && !fileInheritance.existsInChannel && folder.getChannel() != null) {
				expectConflict = false;
			}
			assertEquals("Check whether changing pub_dir from " + folder.getPublishDir() + " to " + newPubDir + " is possible", !expectConflict,
					DisinheritUtils.checkPubDirChangeConsistency(folder, newPubDir));
		}
	}

	/**
	 * Get the publish directory for the given inheritance setting and conflict
	 * @param folderInheritance folder inheritance setting
	 * @param fileInheritance file inheritance setting
	 * @param conflict true if the file can produce a conflict
	 * @return publish directory
	 */
	protected static String getPubDir(Inheritance folderInheritance, Inheritance fileInheritance, boolean conflict) {
		StringBuilder str = new StringBuilder();
		str.append("/");
		str.append(folderInheritance.toString());
		str.append("/");
		str.append(fileInheritance.toString());
		str.append("/");
		str.append(conflict);
		str.append("/");
		return str.toString();
	}

	/**
	 * Create the folder and possibly its localized copy (depending on the inheritance setting)
	 * @param folderInheritance inheritance setting
	 * @param name folder name
	 * @param pubDir folder publish directory
	 * @return map of node -> folder
	 * @throws Exception
	 */
	protected static Map<Node, Folder> createFolder(Inheritance folderInheritance, String name, String pubDir) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder masterFolder = null;
		Folder channelFolder = null;

		if (folderInheritance.existsInMaster) {
			masterFolder = ContentNodeTestDataUtils.createFolder(master.getFolder(), name);
			masterFolder = t.getObject(Folder.class, masterFolder.getId(), true);
			masterFolder.setPublishDir(pubDir);
			masterFolder.save();
			t.commit(false);
			masterFolder = t.getObject(Folder.class, masterFolder.getId());
		}

		switch (folderInheritance) {
		case inherited:
			channelFolder = masterFolder;
			break;
		case disinherited:
			masterFolder.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(channel)), false);
			t.commit(false);
			break;
		case excluded:
			masterFolder.changeMultichannellingRestrictions(true, new HashSet<Node>(), false);
			t.commit(false);
			break;
		case localized_same:
			channelFolder = ContentNodeTestDataUtils.localize(masterFolder, channel);
			break;
		case localized_different:
			channelFolder = ContentNodeTestDataUtils.localize(masterFolder, channel);
			channelFolder = t.getObject(Folder.class, channelFolder.getId(), true);
			channelFolder.setPublishDir("/mod" + channelFolder.getPublishDir());
			channelFolder.save();
			t.commit(false);
			channelFolder = t.getObject(Folder.class, channelFolder.getId());
			break;
		case local:
			channelFolder = ContentNodeTestDataUtils.createFolder(master.getFolder(), name, channel);
			channelFolder = t.getObject(Folder.class, channelFolder.getId(), true);
			channelFolder.setPublishDir(pubDir);
			channelFolder.save();
			t.commit(false);
			channelFolder = t.getObject(Folder.class, channelFolder.getId());
			break;
		}

		Map<Node, Folder> folderMap = new HashMap<Node, Folder>();
		folderMap.put(master, masterFolder);
		folderMap.put(channel, channelFolder);
		return folderMap;
	}

	/**
	 * Create a file and possibly its localized copy
	 * @param folder folder containing the file
	 * @param name name of the file
	 * @param fileInheritance file inheritance setting
	 * @throws Exception
	 */
	protected static void createFile(Folder folder, String name, Inheritance fileInheritance) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		File masterFile = null;
		File channelFile = null;

		if (fileInheritance.existsInMaster) {
			masterFile = ContentNodeTestDataUtils.createFile(folder, name, "Content".getBytes());
		}

		switch (fileInheritance) {
		case inherited:
			channelFile = masterFile;
			break;
		case disinherited:
			masterFile.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(channel)), false);
			t.commit(false);
			break;
		case excluded:
			masterFile.changeMultichannellingRestrictions(true, new HashSet<Node>(), false);
			t.commit(false);
			break;
		case localized_same:
			channelFile = ContentNodeTestDataUtils.localize(masterFile, channel);
			break;
		case localized_different:
			channelFile = ContentNodeTestDataUtils.localize(masterFile, channel);
			channelFile = t.getObject(File.class, channelFile.getId(), true);
			channelFile.setName("mod" + channelFile.getName());
			channelFile.save();
			t.commit(false);
			channelFile = t.getObject(File.class, channelFile.getId());
			break;
		case local:
			channelFile = ContentNodeTestDataUtils.createFile(folder, name, "Content".getBytes(), channel);
			break;
		}
	}

	/**
	 * Reread all stored objects
	 * @throws NodeException
	 */
	protected static void reReadObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Map<Node, Folder> folderMap : obstructorMaps.values()) {
			for (Map.Entry<Node, Folder> entry : folderMap.entrySet()) {
				Folder folder = entry.getValue();
				if (folder != null) {
					entry.setValue(t.getObject(Folder.class, folder.getId()));
				}
			}
		}
		for (Map<Node, Folder> folderMap : toChangeMaps.values()) {
			for (Map.Entry<Node, Folder> entry : folderMap.entrySet()) {
				Folder folder = entry.getValue();
				if (folder != null) {
					entry.setValue(t.getObject(Folder.class, folder.getId()));
				}
			}
		}
	}

	/**
	 * Possible inheritance settings
	 */
	public static enum Inheritance {
		/**
		 * Object is created in the master and inherited in the channel
		 */
		inherited(true, true, false),

		/**
		 * Object is created in the master and disinherited in the channel
		 */
		disinherited(true, false, false),

		/**
		 * Object is created in the master and excluded from multichannelling
		 */
		excluded(true, false, false),

		/**
		 * Object is created in the master and localized with the same name/pubDir
		 */
		localized_same(true, true, true),

		/**
		 * Object is created in the master and localized with different name/pubDir
		 */
		localized_different(true, true, true),

		/**
		 * Object is created in the channel
		 */
		local(false, true, false);

		/**
		 * Whether the object will exist in the master
		 */
		protected boolean existsInMaster;

		/**
		 * Whether the object will exist in the channel
		 */
		protected boolean existsInChannel;

		/**
		 * Whether the object is localized in the channel
		 */
		protected boolean localized;

		/**
		 * Create an instance
		 * @param existsInMaster
		 * @param existsInChannel
		 * @param localized
		 */
		Inheritance(boolean existsInMaster, boolean existsInChannel, boolean localized) {
			this.existsInMaster = existsInMaster;
			this.existsInChannel = existsInChannel;
			this.localized = localized;
		}

		/**
		 * Check whether this inheritance setting for a folder allows the given inheritance setting for the file
		 * @param other inheritance setting for the file
		 * @return true if the combination is possible, false if not
		 */
		public boolean allows(Inheritance other) {
			if (other.existsInMaster) {
				if (!existsInMaster) {
					return false;
				}
			}
			if (other.existsInChannel) {
				if (!existsInChannel) {
					return false;
				}
			}
			if (this == excluded && other != excluded) {
				return false;
			}
			return true;
		}
	}
}
