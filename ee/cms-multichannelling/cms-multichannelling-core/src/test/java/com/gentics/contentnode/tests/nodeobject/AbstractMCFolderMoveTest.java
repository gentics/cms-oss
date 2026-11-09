package com.gentics.contentnode.tests.nodeobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for moving folders in multichannelling environments
 * TODO: test also with local objects
 */
abstract public class AbstractMCFolderMoveTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test Nodes
	 */
	private static Node master, channel, subChannel, sideChannel, otherNode, otherChannel;
	private Channel source;
	private Channel target;
	private LocalizedObject localized;
	private LocalizationPosition pos;
	private Channel locchannel;
	private Channel disinheritedChannel;
	private Folder movedFolder;
	private Folder subfolder;
	private Folder subSubFolder;
	private Page page1;
	private Page page2;
	private File file1;
	private File file2;



	/**
	 * Get the test variation data
	 * @param disinheritChannel TODO
	 * @return test variation data
	 */
	public static Collection<Object[]> data(Channel target, Channel disinheritChannel) {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (LocalizationPosition pos : LocalizationPosition.values()) {
			data.addAll(data(target, disinheritChannel, pos));
		}

		return data;
	}

	/**
	 * Get the test variation data
	 * @param target
	 * @param disinheritChannel
	 * @param pos
	 * @return test variation data
	 */
	public static Collection<Object[]> data(Channel target, Channel disinheritChannel, LocalizationPosition pos) {
		List<Channel> otherStructure = Arrays.asList(Channel.OTHERMASTER, Channel.OTHERCHANNEL);
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (Channel source : Channel.values()) {
			if (otherStructure.contains(source)) {
				// we don't move from the other node structure
				continue;
			}

			if (source == target) {
				// no need to move within the same channel
				continue;
			}

			for (LocalizedObject localized : LocalizedObject.values()) {
				if (localized == LocalizedObject.SELF && pos != LocalizationPosition.FOLDER) {
					// if the moved folder itself is localized, we don't need to differentiate between positions
					continue;
				}
				for (Channel locchannel : Channel.values()) {
					if (otherStructure.contains(locchannel)) {
						continue;
					} else if (localized == LocalizedObject.NONE && (locchannel != source || pos != LocalizationPosition.FOLDER)) {
						// if nothing is localized, we do not have to differentiate between localization channel and position
						continue;
					} else if (localized != LocalizedObject.NONE && !locchannel.channelOf(source)) {
						// if somthing is localized, the localization channel must be a subchannel of the source channel
						continue;
					}

					boolean canDisinherit = disinheritChannel != null && disinheritChannel.channelOf(source) && locchannel != disinheritChannel;

					if (canDisinherit) {
						data.add(new Object[] { source, target, localized, pos, locchannel, disinheritChannel });
					}

					data.add(new Object[] { source, target, localized, pos, locchannel, null });
				}
			}
		}

		return data;
	}

	/**
	 * Create a test instance
	 * @param source source channel of the folder to move
	 * @param target target channel of the folder to move
	 * @param localized localized object
	 * @param pos position of the localized object in the folder tree
	 * @param locchannel channel of the localized object
	 */
	public AbstractMCFolderMoveTest(Channel source, Channel target, LocalizedObject localized, LocalizationPosition pos, Channel locchannel, Channel disinherited) {
		this.source = source;
		this.target = target;
		this.localized = localized;
		this.pos = pos;
		this.locchannel = locchannel;
		this.disinheritedChannel = disinherited;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);

		// create channel structure
		master = ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.BOTH);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/Content.Node", PublishTarget.BOTH);
		subChannel = ContentNodeTestDataUtils.createChannel(channel, "Subchannel", "subchannel", "/Content.Node", PublishTarget.BOTH);
		sideChannel = ContentNodeTestDataUtils.createChannel(master, "Side Channel", "sidechannel", "/Content.Node", PublishTarget.BOTH);

		// create another channel structure
		otherNode = ContentNodeTestDataUtils.createNode("node", "Other Node", PublishTarget.BOTH);
		otherChannel = ContentNodeTestDataUtils.createChannel(otherNode, "Other Channel", "otherchannel", "/Content.Node", PublishTarget.BOTH);
	}

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		testContext.getContext().startTransaction();

		// clean the DB
		DBUtils.executeUpdate("DELETE FROM page", null);
		DBUtils.executeUpdate("DELETE FROM template", null);
		DBUtils.executeUpdate("DELETE FROM content", null);
		DBUtils.executeUpdate("DELETE FROM contentfile", null);
		DBUtils.executeUpdate("DELETE FROM node WHERE id NOT IN (?, ?, ?, ?, ?, ?)", new Object[] {
				master.getId(), channel.getId(), subChannel.getId(), sideChannel.getId(),
				otherNode.getId(), otherChannel.getId()
		});
		DBUtils.executeUpdate("DELETE FROM folder WHERE id NOT IN (?, ?, ?, ?, ?, ?)", new Object[] {
				master.getFolder().getId(), channel.getFolder().getId(), subChannel.getFolder().getId(), sideChannel.getFolder().getId(),
				otherNode.getFolder().getId(), otherChannel.getFolder().getId()
		});
		DBUtils.executeUpdate("DELETE FROM logcmd", null);
		DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
		DBUtils.executeUpdate("DELETE FROM publishqueue", null);

		testContext.getContext().clearNodeObjectCache();

		// create the folder structure to be moved
		Node sourceChannel = getNode(source);
		Transaction t = TransactionManager.getCurrentTransaction();

		Template template = t.createObject(Template.class);
		template.setSource("bla");
		template.addFolder(master.getFolder());
		template.save();
		t.commit(false);

		movedFolder = ContentNodeTestDataUtils.createFolder(master.getFolder(), "Moved Folder", sourceChannel);
		subfolder = ContentNodeTestDataUtils.createFolder(movedFolder, "Subfolder", sourceChannel);
		subSubFolder = ContentNodeTestDataUtils.createFolder(subfolder, "SubSubfolder", sourceChannel);

		if (disinheritedChannel != null) {
			Set<Node> disinherited = new HashSet<>();

			disinherited.add(getNode(disinheritedChannel));
			movedFolder.changeMultichannellingRestrictions(false, disinherited, true);
		}

		page1 = ContentNodeTestDataUtils.createPage(movedFolder, template, "Page1", sourceChannel);
		t.getObject(Page.class, page1.getId(), true).publish();
		t.commit(false);
		page2 = ContentNodeTestDataUtils.createPage(subfolder, template, "Page2", sourceChannel);
		t.getObject(Page.class, page2.getId(), true).publish();
		t.commit(false);

		file1 = ContentNodeTestDataUtils.createFile(movedFolder, "file1.bin", new byte[] {1, 2, 3}, sourceChannel);
		file2 = ContentNodeTestDataUtils.createFile(subfolder, "file2.bin", new byte[] {4, 5, 6}, sourceChannel);

		// do the requested localization
		Node localizationNode = getNode(locchannel);
		switch (localized) {
		case SELF:
			ContentNodeTestDataUtils.localize(movedFolder, localizationNode);
			break;
		case FOLDER:
			switch (pos) {
			case FOLDER:
				ContentNodeTestDataUtils.localize(subfolder, localizationNode);
				break;
			case SUBFOLDER:
				ContentNodeTestDataUtils.localize(subSubFolder, localizationNode);
				break;
			}
			break;
		case PAGE:
			switch (pos) {
			case FOLDER:
				t.getObject(Page.class, ContentNodeTestDataUtils.localize(page1, localizationNode).getId(), true).publish();
				t.commit(false);
				break;
			case SUBFOLDER:
				t.getObject(Page.class, ContentNodeTestDataUtils.localize(page2, localizationNode).getId(), true).publish();
				t.commit(false);
				break;
			}
			break;
		case FILE:
			switch (pos) {
			case FOLDER:
				ContentNodeTestDataUtils.localize(file1, localizationNode);
				break;
			case SUBFOLDER:
				ContentNodeTestDataUtils.localize(file2, localizationNode);
				break;
			}
			break;
		case NONE:
			// Nothing to do.
		}

		refreshObjectInstances();

		testContext.publish(false);

		assertObjectsCR();
	}

	@After
	public void tearDown() throws Exception {
		TransactionManager.getCurrentTransaction().commit();
		testContext.getContext().clearNodeObjectCache();
	}

	/**
	 * Free resources
	 * @throws Exception
	 */
	@AfterClass
	public static void afterClass() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test moving a folder
	 * @throws Exception
	 */
	@Test
	public void testMove() throws Exception {
		testContext.getContext().stopDirtQueueWorker();
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder toMove = t.getObject(Folder.class, movedFolder.getId(), true);

		Node sourceChannel = getNode(source);
		Node targetChannel = getNode(target);
		Node localizationChannel = getNode(locchannel);
		int targetChannelId = ObjectTransformer.getInt(targetChannel.getId(), 0);
		boolean expectFailure = localized != LocalizedObject.NONE && !localizationChannel.getMasterNodes().contains(targetChannel);

		try {
			Map<NodeObject, Node> channelInfo = getChannelInfo();

			if (disinheritedChannel != null) {
				assertRestrictions(true);
			}

			OpResult result = toMove.move(targetChannel.getFolder(), targetChannelId);
			t.commit(false);

			if (expectFailure) {
				assertEquals("Check result", OpResult.Status.FAILURE, result.getStatus());
			} else {
				assertEquals("Check result", OpResult.Status.OK, result.getStatus());

				// check whether the folder was moved
				assertFolderMoved(targetChannel, channelInfo);
				assertLocalizedNotMoved(targetChannel);
				assertRestrictions(
					disinheritedChannel != null
						// Disinheritance should disappear when we move to the disinherited channel (or a subchannel) ...
						&& disinheritedChannel != target && !target.channelOf(disinheritedChannel)
						// ... or when we move to another channel (sub-)structure.
						&& disinheritedChannel.channelOf(target));

				Map<NodeObject, Node> newChannelInfo = getChannelInfo();

				// check correct dirting
				testContext.waitForDirtqueueWorker();

				List<Folder> folders = Arrays.asList(movedFolder, subfolder, subSubFolder);
				List<Page> pages = Arrays.asList(page1, page2);
				List<File> files = Arrays.asList(file1, file2);

				for (Node checkedNode : Arrays.asList(master, channel, subChannel, sideChannel, otherNode, otherChannel)) {
					boolean containedBeforeMove = checkedNode.equals(sourceChannel) || checkedNode.isChannelOf(sourceChannel);
					boolean containedAfterMove = checkedNode.equals(targetChannel) || checkedNode.isChannelOf(targetChannel);

					// check dirted objects
					List<Integer> dirtedFolderIds = PublishQueue.getDirtedObjectIds(Folder.class, false, checkedNode);
					List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, checkedNode);
					List<Integer> dirtedFileIds = PublishQueue.getDirtedObjectIds(File.class, false, checkedNode);

					String dirtMessage = "be dirted for " + checkedNode.getFolder().getName();
					for (Folder folder : folders) {
						assertIdContained(dirtMessage, folder, dirtedFolderIds, containedAfterMove && wasMoved(channelInfo, newChannelInfo, folder)
								&& !isLocalized(folder, checkedNode));
					}
					for (Page page : pages) {
						assertIdContained(dirtMessage, page, dirtedPageIds,
								containedAfterMove && wasMoved(channelInfo, newChannelInfo, page) && !isLocalized(page, checkedNode));
					}
					for (File file : files) {
						// Files are sometimes dirted, even if the node_id changes for the folder only, and not the file
						// (the node_id for the file is taken from the channel_id). For some reason this happens for files
						// only and not for pages. So we check for dirting only, if we assume the file has changed.
						if (containedAfterMove && wasMoved(channelInfo, newChannelInfo, file) && !isLocalized((ContentFile) file, checkedNode)) {
							assertIdContained(dirtMessage, file, dirtedFileIds, true);
						}
					}

					// check removed objects
					List<Integer> removedFolderIds = PublishQueue.getRemovedObjectIds(Folder.class, false, checkedNode);
					List<Integer> removedPageIds = PublishQueue.getRemovedObjectIds(Page.class, false, checkedNode);
					List<Integer> removedFileIds = PublishQueue.getRemovedObjectIds(File.class, false, checkedNode);

					String removeMessage = "be removed from " + checkedNode.getFolder().getName();
					for (Folder folder : folders) {
						assertIdContained(removeMessage, folder, removedFolderIds, containedBeforeMove && !containedAfterMove);
					}
					for (Page page : pages) {
						assertIdContained(removeMessage, page, removedPageIds, containedBeforeMove && !containedAfterMove);
					}
					for (File file : files) {
						assertIdContained(removeMessage, file, removedFileIds, containedBeforeMove && !containedAfterMove);
					}
				}

				// check logcmd entries
				for (Folder folder : folders) {
					if (wasMoved(channelInfo, newChannelInfo, folder)) {
					assertLogCmdEntry(ActionLogger.MOVE, Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), 0), ObjectTransformer.getInt(targetChannel.getFolder().getMaster().getId(), 0));
				}
				}
				for (Page page : pages) {
					if (wasMoved(channelInfo, newChannelInfo, page)) {
					assertLogCmdEntry(ActionLogger.MOVE, Page.TYPE_PAGE, ObjectTransformer.getInt(page.getId(), 0), targetChannelId);
				}
				}
				for (File file : files) {
					if (wasMoved(channelInfo, newChannelInfo, file)) {
					assertLogCmdEntry(ActionLogger.MOVE, File.TYPE_FILE, ObjectTransformer.getInt(file.getId(), 0), targetChannelId);
				}
				}

				// check that after a publish process, all objects are in the correct CRs
				testContext.publish(false);
				assertObjectsCR();
			}
		} finally {
			t.commit(false);
		}
	}

	/**
	 * Check whether the object is localized in the channel
	 * @param object object
	 * @param channel channel
	 * @return true if the object is localized, false if not
	 * @throws NodeException
	 */
	protected boolean isLocalized(LocalizableNodeObject<? extends NodeObject> object, Node channel) throws NodeException {
		NodeObject variant = MultichannellingFactory.getChannelVariant(object, channel);
		if (variant == null) {
			return false;
		}
		return !object.equals(variant);
	}

	/**
	 * Assert that the id of the object is contained (or not contained) in the given id list
	 * @param message message to be appended to the assertion message
	 * @param object object to check
	 * @param ids list of ids
	 * @param expected true if the id is expected to be contained, false if it is expected to not be contained
	 * @throws Exception
	 */
	protected void assertIdContained(String message, NodeObject object, List<Integer> ids, boolean expected) throws Exception {
		int id = ObjectTransformer.getInt(object.getId(), 0);
		if (expected) {
			assertTrue(object + " should " + message, ids.contains(id));
		} else {
			assertFalse(object + " should not " + message, ids.contains(id));
		}
	}

	/**
	 * Get channel info for all objects
	 * @return map of object -> channel
	 * @throws NodeException
	 */
	protected Map<NodeObject, Node> getChannelInfo() throws NodeException {
		Map<NodeObject, Node> channelInfo = new HashMap<NodeObject, Node>();
		channelInfo.put(movedFolder, movedFolder.getChannel());
		channelInfo.put(subfolder, subfolder.getChannel());
		channelInfo.put(subSubFolder, subSubFolder.getChannel());
		channelInfo.put(page1, page1.getChannel());
		channelInfo.put(page2, page2.getChannel());
		channelInfo.put(file1, file1.getChannel());
		channelInfo.put(file2, file2.getChannel());
		return channelInfo;
	}

	// TODO remove
	protected Map<NodeObject, Boolean> getLocalizationInfo() {
		Map<NodeObject, Boolean> localizationInfo = new HashMap<>();

		localizationInfo.put(movedFolder, localized == LocalizedObject.SELF);

		boolean isLocalized = localized == LocalizedObject.FOLDER && source == locchannel;

		localizationInfo.put(subfolder, isLocalized && pos == LocalizationPosition.FOLDER);
		localizationInfo.put(subSubFolder, isLocalized && pos == LocalizationPosition.SUBFOLDER);

		isLocalized = localized == LocalizedObject.PAGE && source == locchannel;
		localizationInfo.put(page1, isLocalized && pos == LocalizationPosition.FOLDER);
		localizationInfo.put(page2, isLocalized && pos == LocalizationPosition.SUBFOLDER);

		isLocalized = localized == LocalizedObject.FILE && source == locchannel;
		localizationInfo.put(file1, isLocalized && pos == LocalizationPosition.FOLDER);
		localizationInfo.put(file2, isLocalized && pos == LocalizationPosition.SUBFOLDER);

		return localizationInfo;
	}

	/**
	 * Check whether the given object was moved
	 * @param oldChannelInfo old channelinfo
	 * @param currentChannelInfo new channelinfo
	 * @param object object in question
	 * @return true if the object was moved, false if not
	 */
	protected boolean wasMoved(Map<NodeObject, Node> oldChannelInfo, Map<NodeObject, Node> currentChannelInfo, NodeObject object) {
		if (oldChannelInfo.get(object) == null || currentChannelInfo.get(object) == null) {
			// if the object did not belong to a channel before, or does not belong to a channel now,
			// it was moved
			return true;
		} else {
			// if the object belonged to a channel and does so now, it was moved, if the channels are different
			return !oldChannelInfo.get(object).equals(currentChannelInfo.get(object));
		}
	}

	/**
	 * Assert that the {@link #movedFolder} was moved into the target node
	 * @param targetNode target node
	 * @param channelInfo channels of the objects before they were moved
	 * @throws Exception
	 */
	protected void assertFolderMoved(Node targetNode, Map<NodeObject, Node> channelInfo) throws Exception {
		refreshObjectInstances();
		List<Folder> folders = targetNode.getFolder().getChildFolders();

		assertTrue("Folder must have been moved to " + targetNode.getMaster(), folders.contains(movedFolder));

		assertNode(
			movedFolder,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, null));

		assertNode(
			subfolder,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(subfolder)));

		assertNode(
			subSubFolder,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(subSubFolder)));

		assertNode(
			page1,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(page1)));

		assertNode(
			page2,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(page2)));

		assertNode(
			file1,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(file1)));

		assertNode(
			file2,
			targetNode.getMaster(),
			getExpectedTargetChannel(targetNode, channelInfo.get(file2)));
	}

	protected void assertLocalizedNotMoved(Node masterNode) throws Exception {
		if (localized == LocalizedObject.NONE) {
			return;
		}

		if (masterNode.isChannel()) {
			masterNode = masterNode.getMaster();
		}

		NodeObject localizedObject;
		Node expectedChannel = getNode(locchannel);

		try (ChannelTrx trx = new ChannelTrx(expectedChannel)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean localizedInFolder = pos == LocalizationPosition.FOLDER;

			switch (localized) {
			case SELF:
				localizedObject = t.getObject(movedFolder);
				break;

			case FOLDER:
				localizedObject = t.getObject(localizedInFolder ? subfolder : subSubFolder);
				break;

			case PAGE:
				localizedObject = t.getObject(localizedInFolder ? page1 : page2);
				break;

			case FILE:
				localizedObject = t.getObject(localizedInFolder ? file1 : file2);
				break;

			case NONE:
			default:
				// Nothing to do, this is handled above.
				localizedObject = null;
			}
		}

		switch (localized) {
		case SELF:
			assertNode((Folder) localizedObject, masterNode, expectedChannel);
			break;

		case FOLDER:
			assertNode((Folder) localizedObject, masterNode, expectedChannel);
			break;

		case PAGE:
			assertNode((Page) localizedObject, masterNode, expectedChannel);
			break;

		case FILE:
			assertNode((ContentFile) localizedObject, masterNode, expectedChannel);
			break;

		case NONE:
		default:
			// Nothing to do.
		}
	}

	protected void assertRestrictions(boolean shouldBeDisinherited) throws NodeException {
		List<Disinheritable<?>> allObjects = Arrays.asList(movedFolder, subfolder, subSubFolder, page1, page2, file1, file2);

		if (shouldBeDisinherited) {
			for (Disinheritable<?> obj : allObjects) {
				assertThat(obj.getDisinheritedChannels())
					.as("Object must be disinherited in " + disinheritedChannel.name())
					.containsExactly(getNode(disinheritedChannel));
			}
		} else {
			for (Disinheritable<?> obj : allObjects) {
				assertThat(obj.getDisinheritedChannels())
					.as("Object must not be disinherited in any channel")
					.isEmpty();
			}
		}
	}

	/**
	 * Get the expected target channel when an object belonging to the old channel was moved to the target node
	 * @param targetNode target node
	 * @param oldChannel old channel
	 * @return expected channel, the object must belong now
	 */
	protected Node getExpectedTargetChannel(Node targetNode, Node oldChannel) throws NodeException {
		return targetNode.isChannel() ? targetNode : null;
	}

	/**
	 * Assert that all objects are contained in the correct CRs
	 * @throws Exception
	 */
	protected void assertObjectsCR() throws Exception {
		for (Node checkedNode : Arrays.asList(master, channel, subChannel, sideChannel, otherNode, otherChannel)) {
			ContentMap contentMap = checkedNode.getContentMap();
			assertNotNull(checkedNode.getFolder().getName() + " must publish into a contentmap", contentMap);
			CNWriteableDatasource datasource = contentMap.getDatasource();
			for (Folder folder : Arrays.asList(movedFolder, subfolder, subSubFolder)) {
				assertObjectCR(folder, checkedNode, datasource);
			}

			for (Page page : Arrays.asList(page1, page2)) {
				assertObjectCR(page, checkedNode, datasource);
			}

			for (File file : Arrays.asList(file1, file2)) {
				assertObjectCR((ContentFile)file, checkedNode, datasource);
			}
		}
	}

	/**
	 * Assert that the given object is present in the datasource for the given node (if the object is visible in the node) or absent from the datasource (if not visible in the node)
	 * @param object object to check
	 * @param node node for which the check shall be done
	 * @param datasource datasource to search in
	 * @throws Exception
	 */
	protected <T extends NodeObject> void assertObjectCR(LocalizableNodeObject<T> object, Node node, Datasource datasource) throws Exception {
		String ttype = ObjectTransformer.getString(object.getTType(), null);
		Resolvable res = PortalConnectorFactory.getContentObject(ttype + "." + object.getId(), datasource);
		if (MultichannellingFactory.isVisibleInNode(node, object)) {
			assertNotNull(object + " must be contained in CR of " + node.getFolder().getName(), res);
		} else {
			assertNull(object + " must not be contained in CR of " + node.getFolder().getName(), res);
		}

		T variant = MultichannellingFactory.getChannelVariant(object, node);
		if (variant != null && !object.equals(variant)) {
			res = PortalConnectorFactory.getContentObject(ttype + "." + variant.getId(), datasource);
			assertNotNull(variant + " must be contained in CR of " + node.getFolder().getName(), res);
		}
	}

	/**
	 * Assert that the folder belongs to the given channel in the given node
	 * @param folder folder
	 * @param node node
	 * @param channel channel
	 * @throws Exception
	 */
	protected void assertNode(Folder folder, Node node, Node channel) throws Exception {
		assertEquals(folder + " must belong to node " + node + " now", node, folder.getNode());
		assertEquals(folder + " must belong to channel " + channel + " now", channel, folder.getChannel());
	}

	/**
	 * Assert that the page belongs to the given channel in the given node
	 * @param page page
	 * @param node node
	 * @param channel channel
	 * @throws Exception
	 */
	protected void assertNode(Page page, Node node, Node channel) throws Exception {
		assertEquals(page + " must belong to node " + node + " now", node, page.getOwningNode());
		assertEquals(page + " must belong to channel " + channel + " now", channel, page.getChannel());
	}

	/**
	 * Assert that the file belongs to the given channel in the given node
	 * @param file file
	 * @param node node
	 * @param channel channel
	 * @throws Exception
	 */
	protected void assertNode(File file, Node node, Node channel) throws Exception {
		assertEquals(file + " must belong to node " + node + " now", node, ((ContentFile)file).getOwningNode());
		assertEquals(file + " must belong to channel " + channel + " now", channel, file.getChannel());
	}

	/**
	 * Assert existence of the given logcmd entry
	 * @param cmdDescId cmd id
	 * @param objType object type
	 * @param objId object id
	 * @param objId2 secondary object id
	 * @throws Exception
	 */
	protected void assertLogCmdEntry(final int cmdDescId, final int objType, final int objId, final int objId2) throws Exception {
		RecordCounter exec = new RecordCounter() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, cmdDescId);
				stmt.setInt(2, objType);
				stmt.setInt(3, objId);
				stmt.setInt(4, objId2);
			}
		};
		DBUtils.executeStatement("SELECT * FROM logcmd WHERE cmd_desc_id = ? AND o_type = ? AND o_id = ? AND o_id2 = ?", exec);
		assertTrue("Logcmd entry for " + cmdDescId + ", " + objType + ", " + objId + ", " + objId2 + " not found", exec.getCount() > 0);
	}

	/**
	 * Refresh all object instances
	 * @throws Exception
	 */
	protected void refreshObjectInstances() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		movedFolder = t.getObject(Folder.class, movedFolder.getId());
		subfolder = t.getObject(Folder.class, subfolder.getId());
		subSubFolder = t.getObject(Folder.class, subSubFolder.getId());
		page1 = t.getObject(Page.class, page1.getId());
		page2 = t.getObject(Page.class, page2.getId());
		file1 = t.getObject(File.class, file1.getId());
		file2 = t.getObject(File.class, file2.getId());
	}

	/**
	 * Get the node for the given channel setting
	 * @param c channel setting
	 * @return node
	 * @throws NodeException
	 */
	protected Node getNode(Channel c) throws NodeException {
		switch(c) {
		case MASTER:
			return master;
		case CHANNEL:
			return channel;
		case SUBCHANNEL:
			return subChannel;
		case SIDECHANNEL:
			return sideChannel;
		case OTHERMASTER:
			return otherNode;
		case OTHERCHANNEL:
			return otherChannel;
		default:
			throw new NodeException("No channel given");
		}
	}

	protected <T extends NodeObject> T getLocalizedObject(Class<T> objType) throws NodeException {
		try (ChannelTrx trx = new ChannelTrx(getNode(locchannel))) {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean localizedInFolder = pos == LocalizationPosition.FOLDER;

			if (Folder.class.isAssignableFrom(objType)) {
				if (localized == LocalizedObject.SELF) {
					return t.getObject(objType, movedFolder.getId());
				}

				return t.getObject(objType, (localizedInFolder ? subfolder : subSubFolder).getId());
			}

			if (Page.class.isAssignableFrom(objType)) {
				return t.getObject(objType, (localizedInFolder ? page1 : page2).getId());
			}

			if (File.class.isAssignableFrom(objType)) {
				return t.getObject(objType, (localizedInFolder ? file1 : file2).getId());
			}

			return null;
		}
	}

	/**
	 * Channel setting
	 */
	public enum Channel {
		MASTER,

		CHANNEL,

		SUBCHANNEL,

		SIDECHANNEL,

		OTHERMASTER,

		OTHERCHANNEL;

		/**
		 * Check whether the given channel is a subchannel of the given master channel
		 * @param master master channel
		 * @return true if this channel is a subchannel, false if not
		 */
		public boolean channelOf(Channel master) {
			switch (master) {
			case MASTER:
				return this != MASTER && this != OTHERMASTER && this != OTHERCHANNEL;
			case CHANNEL:
				return this == SUBCHANNEL;
			case OTHERMASTER:
				return this == OTHERCHANNEL;
			default:
				return false;
			}
		}
	}

	/**
	 * Setting for the object, which is localized
	 */
	public enum LocalizedObject {
		SELF,
		FOLDER,
		PAGE,
		FILE,
		NONE;
	}

	/**
	 * Setting for where the object is localized
	 */
	public enum LocalizationPosition {
		FOLDER,
		SUBFOLDER;
	}

	/**
	 * SQLExecutor instance, that counts the number of returned records
	 */
	protected static class RecordCounter extends SQLExecutor {
		/**
		 * Record count
		 */
		protected int count = 0;

		@Override
		public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
			while (rs.next()) {
				count++;
			}
		}

		/**
		 * Get the record count
		 * @return record count
		 */
		public int getCount() {
			return count;
		}
	}
}
