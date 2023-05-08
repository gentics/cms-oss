package com.gentics.contentnode.testutils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;

/**
 * Encapsulates a channel forest consisting of two trees:
 * master(channel(subchannel),sidechannel) and otherMaster(otherChannel).
 * Channels will be set up to publish to content repositories.
 *
 * @author NOP, escitalopram
 *
 */
public class ChannelStructure {
	private Node master;
	private Node channel;
	private Node subChannel;
	private Node sideChannel;
	private Node otherMaster;
	private Node otherChannel;

	/**
	 * Creates a new instance with all Node objects initialized.
	 * @throws NodeException
	 */
	public ChannelStructure(ContentLanguage... languages) throws NodeException {
		// create channel structure
		master = ContentNodeTestDataUtils.createNode("master", "Master Node", PublishTarget.CONTENTREPOSITORY, languages);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/Content.Node", PublishTarget.CONTENTREPOSITORY);
		subChannel = ContentNodeTestDataUtils.createChannel(channel, "Subchannel", "subchannel", "/Content.Node", PublishTarget.CONTENTREPOSITORY);
		sideChannel = ContentNodeTestDataUtils.createChannel(master, "Side Channel", "sidechannel", "/Content.Node", PublishTarget.CONTENTREPOSITORY);

		// create another channel structure
		otherMaster = ContentNodeTestDataUtils.createNode("node", "Other Master Node", PublishTarget.CONTENTREPOSITORY, languages);
		otherChannel = ContentNodeTestDataUtils.createChannel(otherMaster, "Other Channel", "otherchannel", "/Content.Node", PublishTarget.CONTENTREPOSITORY);
	}

	/**
	 * Reloads all nodes
	 * @throws NodeException
	 */
	public void reloadObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		t.dirtObjectCache(Node.class, master.getId());
		master = t.getObject(Node.class, master.getId());
		t.dirtObjectCache(Node.class, channel.getId());
		channel = t.getObject(Node.class, channel.getId());
		t.dirtObjectCache(Node.class, subChannel.getId());
		subChannel = t.getObject(Node.class, subChannel.getId());
		t.dirtObjectCache(Node.class, sideChannel.getId());
		sideChannel = t.getObject(Node.class, sideChannel.getId());
		t.dirtObjectCache(Node.class, otherMaster.getId());
		otherMaster = t.getObject(Node.class, otherMaster.getId());
		t.dirtObjectCache(Node.class, otherChannel.getId());
		otherChannel = t.getObject(Node.class, otherChannel.getId());
	}

	/**
	 * Return all Node objects contained
	 * @return list of all Node objects contained
	 */
	public List<Node> getAllNodes() {
		return Arrays.asList(master, channel, subChannel, sideChannel, otherMaster, otherChannel);
	}

	/**
	 * Get the node for the given channel setting
	 * @param c
	 *            channel setting
	 * @return node
	 * @throws NodeException
	 */
	public Node getNode(Channel c) throws NodeException {
		switch (c) {
		case MASTER:
			return master;
		case CHANNEL:
			return channel;
		case SUBCHANNEL:
			return subChannel;
		case SIDECHANNEL:
			return sideChannel;
		case OTHERMASTER:
			return otherMaster;
		case OTHERCHANNEL:
			return otherChannel;
		case NONE:
			return null;
		default:
			throw new NodeException("No channel given");
		}
	}

	/**
	 * Channel setting. Intended to be used e.g. as test parameters.
	 */
	public enum Channel {
		MASTER,

		CHANNEL,

		SUBCHANNEL,

		SIDECHANNEL,

		OTHERMASTER,

		OTHERCHANNEL,

		NONE;

		/**
		 * Check whether the given channel is a subchannel of the given master
		 * channel
		 *
		 * @param master
		 *            master channel
		 * @return true if this channel is a subchannel, false if not
		 */
		public boolean channelOf(Channel master) {
			switch (master) {
			case MASTER:
				return Arrays.asList(CHANNEL, SUBCHANNEL, SIDECHANNEL).contains(this);
			case CHANNEL:
				return this == SUBCHANNEL;
			case OTHERMASTER:
				return this == OTHERCHANNEL;
			default:
				return false;
			}
		}

		/**
		 * Determine a list of all unordered combinations of channels that can
		 * be disinherited, so that this channel is still visible.
		 *
		 * @return list of channel combinations
		 */
		public List<List<Channel>> getChannelVariations() {
			List<Channel> empty = Collections.emptyList();
			switch (this) {
			case MASTER:
				return Arrays.asList(empty, Arrays.asList(CHANNEL),
						Arrays.asList(SUBCHANNEL), Arrays.asList(SIDECHANNEL),
						Arrays.asList(CHANNEL, SIDECHANNEL),
						Arrays.asList(SUBCHANNEL, SIDECHANNEL));
			case CHANNEL:
				return Arrays.asList(empty, Arrays.asList(SUBCHANNEL),
						Arrays.asList(SIDECHANNEL),
						Arrays.asList(SUBCHANNEL, SIDECHANNEL));
			case OTHERMASTER:
				return Arrays.asList(empty, Arrays.asList(OTHERCHANNEL));
			default:
				return Arrays.asList(empty);
			}
		}
		public static final List<Channel> mainTree = Collections.unmodifiableList(Arrays.asList(MASTER, CHANNEL, SUBCHANNEL, SIDECHANNEL));
		public static final List<Channel> otherTree = Collections.unmodifiableList(Arrays.asList(OTHERMASTER, OTHERCHANNEL));
		public static final List<Channel> masters = Collections.unmodifiableList(Arrays.asList(MASTER, OTHERMASTER));
		public boolean isMainTree() {
			return mainTree.contains(this);
		}
		public boolean isOtherTree() {
			return otherTree.contains(this);
		}
		public boolean isMaster() {
;			return masters.contains(this);
		}
	}
}
