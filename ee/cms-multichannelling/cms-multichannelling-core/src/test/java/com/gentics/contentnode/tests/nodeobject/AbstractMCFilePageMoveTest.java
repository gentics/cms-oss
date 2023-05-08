package com.gentics.contentnode.tests.nodeobject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for moving files and pages
 */
abstract public class AbstractMCFilePageMoveTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static int setupTime = 1;
	protected static int firstPublishTime = setupTime + 1;
	protected static int firstCheckTime = firstPublishTime + 1;
	protected static int moveTime = firstCheckTime + 1;
	protected static int secondPublishTime = moveTime + 1;
	protected static int secondCheckTime = secondPublishTime + 1;

	protected Channel source;
	protected Channel target;
	protected boolean differentFolder;
	protected Channel localized;
	protected boolean targetExcluded;
	protected List<Channel> targetDisinherited;

	protected static ChannelStructure channels;

	protected Folder sourceFolder;
	protected Folder targetFolder;
	protected Node sourceNode;
	protected Node targetNode;


	
	public static Collection<Object[]> data(Channel target) {
		List<Object[]> data = new ArrayList<Object[]>();

		for (boolean differentFolder : Arrays.asList(true, false)) {
			for (boolean targetExcluded : Arrays.asList(true, false)) {
				data.addAll(data(target, differentFolder, targetExcluded));
			}
		}

		return data;
	}

	public static Collection<Object[]> data(Channel target, boolean differentFolder, boolean targetExcluded) {
		List<Object[]> data = new ArrayList<Object[]>();

		for (Channel source : Channel.values()) {
			for (Channel localized : Channel.values()) {
				for (List<Channel> targetDisinherited : target.getChannelVariations()) {
					if (!filterVariation(source, target, differentFolder, localized, targetExcluded, targetDisinherited)) {
						continue;
					}

					data.add(new Object[] {source, target, differentFolder, localized, targetExcluded, targetDisinherited});
				}
			}
		}

		return data;
	}

	/**
	 * Filter the given variation
	 * @param source source node
	 * @param target target node
	 * @param differentFolder true for moving to a different folder
	 * @param localized channel for localized object (may be null)
	 * @param targetExcluded true if the target is excluded from multichannelling
	 * @param targetDisinherited list of disinherited channels
	 * @return true if the variation is tested, false if not
	 */
	protected static boolean filterVariation(Channel source, Channel target, boolean differentFolder, Channel localized, boolean targetExcluded, List<Channel> targetDisinherited) {
		List<Channel> otherStructure = Arrays.asList(Channel.OTHERMASTER, Channel.OTHERCHANNEL);
		// NONE is only valid for 'localized'
		if (source == Channel.NONE || target == Channel.NONE) {
			return false;
		}
		// only move from the first structure
		if (otherStructure.contains(source)) {
			return false;
		}
		// we cannot move, if source and target nodes and folder are identical
		if (source == target && !differentFolder) {
			return false;
		}
		// when moving to the other structure, we always will move to the other folder
		if (otherStructure.contains(target) && !differentFolder) {
			return false;
		}
		// the channel of the localized copy must be a channel of the source node
		if (localized != null && !localized.channelOf(source) && localized != Channel.NONE) {
			return false;
		}
		// if the target is excluded from multichannelling, it has no disinherited channels
		if (targetExcluded && !ObjectTransformer.isEmpty(targetDisinherited)) {
			return false;
		}
		// if the target is excluded from multichannelling, we can only move into master node
		if (targetExcluded && !Arrays.asList(Channel.MASTER, Channel.OTHERMASTER).contains(target)) {
			return false;
		}
		// moving just between channels is not possible if the folder is excluded from multichannelling
		if (!differentFolder && targetExcluded) {
			return false;
		}
		// folders are the same and object is localized in a channel, in which the folder is disinherited
		if (!differentFolder && containsOrMasterOf(targetDisinherited, localized)) {
			return false;
		}
		// folders are the same and folder is disinherited in source channel
		if (!differentFolder && containsOrMasterOf(targetDisinherited, source)) {
			return false;
		}

		return true;
	}

	protected static boolean containsOrMasterOf(List<Channel> channels, Channel c) {
		if (c == null || ObjectTransformer.isEmpty(channels)) {
			return false;
		}
		for (Channel test : channels) {
			if (c.equals(test) || c.channelOf(test)) {
				return true;
			}
		}

		return false;
	}

	public AbstractMCFilePageMoveTest(Channel source, Channel target, boolean differentFolder, Channel localized, boolean targetExcluded,
			List<Channel> targetDisinherited) {
		this.source = source;
		this.target = target;
		this.differentFolder = differentFolder;
		this.localized = localized;
		this.targetExcluded = targetExcluded;
		this.targetDisinherited = targetDisinherited;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);

		channels = new ChannelStructure();
	}

	/**
	 * Set up test data
	 * @throws NodeException
	 */
	public void setUpBaseData() throws Exception {
		testContext.startTransaction(setupTime);
		Transaction t = TransactionManager.getCurrentTransaction();;

		// clean the DB
		DBUtils.executeUpdate("DELETE FROM page", null);
		DBUtils.executeUpdate("DELETE FROM page_nodeversion", null);
		DBUtils.executeUpdate("DELETE FROM nodeversion", null);
		DBUtils.executeUpdate("DELETE FROM template", null);
		DBUtils.executeUpdate("DELETE FROM content", null);
		DBUtils.executeUpdate("DELETE FROM contentfile", null);
		DBUtils.executeUpdate("DELETE FROM node WHERE id NOT IN (?, ?, ?, ?, ?, ?)", new Object[] {
				channels.getNode(Channel.MASTER).getId(),
				channels.getNode(Channel.CHANNEL).getId(),
				channels.getNode(Channel.SUBCHANNEL).getId(),
				channels.getNode(Channel.SIDECHANNEL).getId(),
				channels.getNode(Channel.OTHERMASTER).getId(),
				channels.getNode(Channel.OTHERCHANNEL).getId()
		});
		DBUtils.executeUpdate("DELETE FROM folder WHERE id NOT IN (?, ?, ?, ?, ?, ?)", new Object[] {
				channels.getNode(Channel.MASTER).getFolder().getId(),
				channels.getNode(Channel.CHANNEL).getFolder().getId(),
				channels.getNode(Channel.SUBCHANNEL).getFolder().getId(),
				channels.getNode(Channel.SIDECHANNEL).getFolder().getId(),
				channels.getNode(Channel.OTHERMASTER).getFolder().getId(),
				channels.getNode(Channel.OTHERCHANNEL).getFolder().getId()
		});
		DBUtils.executeUpdate("DELETE FROM logcmd", null);
		DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
		DBUtils.executeUpdate("DELETE FROM publishqueue", null);
		DBUtils.executeUpdate("DELETE FROM contentgroup", null);
		DBUtils.executeUpdate("DELETE FROM publish", null);

		sourceNode = channels.getNode(source);
		sourceFolder = ContentNodeTestDataUtils.createFolder(sourceNode.getFolder(), "Move Source");

		targetNode = channels.getNode(target);
		if (differentFolder) {
			targetFolder = ContentNodeTestDataUtils.createFolder(targetNode.getFolder(), "Move Target");
		} else {
			targetFolder = sourceFolder;
		}

		targetFolder.changeMultichannellingRestrictions(targetExcluded, getNodes(targetDisinherited), false);
		targetFolder = t.getObject(Folder.class, targetFolder.getId());
	}

	/**
	 * Set up test data
	 * @throws NodeException
	 */
	public void setUpPublish() throws Exception {
		reReadObjects();
		testContext.publish(firstPublishTime);
		testContext.startTransaction(firstCheckTime);
	}

	/**
	 * Free resources
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		TransactionManager.getCurrentTransaction().commit();
		testContext.getContext().clearNodeObjectCache();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		PortalConnectorFactory.destroy();
	}

	abstract void reReadObjects() throws NodeException;

	/**
	 * Reread objects
	 * @throws NodeException
	 */
	protected void reReadBaseObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (sourceFolder != null) {
			sourceFolder = t.getObject(Folder.class, sourceFolder.getId());
		}
		if (targetFolder != null) {
			targetFolder = t.getObject(Folder.class, targetFolder.getId());
		}
	}

	/**
	 * Assert that the objects have been published into the correct channels
	 * @throws Exception
	 */
	protected void assertPublishedObjects(List<? extends Disinheritable<? extends NodeObject>> checkObjects) throws Exception {
		for (Disinheritable<? extends NodeObject> object : checkObjects) {
			ChannelTreeSegment objectSegment = new ChannelTreeSegment(object, false);
			for (Node node : channels.getAllNodes()) {
				ContentNodeTestUtils.assertPublishCR(object, node,
						objectSegment.getAllNodes().contains(node) && object.equals(MultichannellingFactory.getChannelVariant(object, node)));
			}
		}
	}

	/**
	 * Get the set of nodes for the given channels settings
	 * @param channels channels setting
	 * @return set of nodes
	 * @throws NodeException
	 */
	protected Set<Node> getNodes(List<Channel> channelList) throws NodeException {
		Set<Node> nodes = new HashSet<>();
		if (!ObjectTransformer.isEmpty(channelList)) {
			for (Channel c : channelList) {
				nodes.add(channels.getNode(c));
			}
		}
		return nodes;
	}
}
