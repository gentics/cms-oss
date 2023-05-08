package com.gentics.contentnode.factory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;

/**
 * Represents a subset of channels from the channel tree identified by a start
 * channel, a multichannelling exclusion state and a set of stop nodes (e.g.
 * disinherited channels).
 *
 * @author escitalopram
 *
 */
public class ChannelTreeSegment {
	private final Node startChannel;

	private final Set<Node> restrictions;
	private final boolean excluded;
	private Set<Node> allNodes;
	private final Disinheritable<?> associatedObject;

	/**
	 * Creates a new ChannelTreeSegment.
	 *
	 * @param startChannel
	 *            the start channel of the segment
	 * @param excluded
	 *            the multichannelling exclusion state of the segment
	 * @param restrictions
	 *            stop nodes for the segment
	 */
	public ChannelTreeSegment(Node startChannel, boolean excluded, Collection<Node> restrictions) {
		this.startChannel = startChannel;
		this.excluded = excluded;
		this.restrictions = new HashSet<>(restrictions);
		this.associatedObject = null;
	}

	/**
	 * Creates a ChannelTreeSegment based on an object.
	 *
	 * @param associatedObject
	 *            the object to base this segment on.
	 * @param useAllLocalizations
	 *            if true, the resulting segment represents the visibility of
	 *            the whole object, including all localizations based on its
	 *            multichannelling restrictions. If false, the segment
	 *            represents the visibility of the exact channel variant
	 *            specified by associatedObject, restricted by localizations in
	 *            subchannels as well as restrictions from multichannelling
	 *            restrictions.
	 * @throws NodeException
	 */
	public ChannelTreeSegment(Disinheritable<?> associatedObject, boolean useAllLocalizations) throws NodeException {
		this.associatedObject = associatedObject;
		Disinheritable<?> masterObject = (Disinheritable<?>) associatedObject.getMaster();
		excluded = masterObject.isExcluded();
		Node channel = associatedObject.getChannel();
		if (channel == null) {
			channel = associatedObject.getOwningNode();
		}
		if (useAllLocalizations) {
			LocalizableNodeObject<?> master = associatedObject.getMaster();
			if (master.getChannel() != null) {
				startChannel = master.getChannel();
			} else {
				startChannel = master.getOwningNode();
			}
		} else {
			startChannel = channel;
		}
		restrictions = new HashSet<>();
		if (!excluded) {
			Transaction t = TransactionManager.getCurrentTransaction();
			restrictions.addAll(masterObject.getDisinheritedChannels());
			if (!useAllLocalizations) {
				for (Integer channelId : associatedObject.getChannelSet().keySet()) {
					if (channelId != 0 && channelId != channel.getId()) {
						restrictions.add(t.getObject(Node.class, channelId, -1, false));
					}
				}
			}
			simplifyEndNodes(restrictions, startChannel);
		}
	}

	/**
	 * Create a channel tree segment based on the visibility of the specified
	 * channel variant, but the specified channel tree segment is used instead
	 * of the existing multichannelling restrictions
	 *
	 * @param associatedObject
	 *            the object to base this segment on
	 * @param disinheritedOverride
	 *            the multichannelling restrictions to impose on the segment
	 *            instead of those of associatedObject's master
	 * @throws NodeException
	 */
	public ChannelTreeSegment(Disinheritable<?> associatedObject, ChannelTreeSegment disinheritedOverride) throws NodeException {
		this.associatedObject = associatedObject;
		excluded = disinheritedOverride.isExcluded();
		Node channel = associatedObject.getChannel();
		if (channel == null) {
			channel = associatedObject.getOwningNode();
		}
		startChannel = channel;
		restrictions = new HashSet<>();
		if (!excluded) {
			Transaction t = TransactionManager.getCurrentTransaction();
			restrictions.addAll(disinheritedOverride.getRestrictions());
			for (Integer channelId : associatedObject.getChannelSet().keySet()) {
				if (channelId != 0 && channelId != channel.getId()) {
					restrictions.add(t.getObject(Node.class, channelId));
				}
			}
			simplifyEndNodes(restrictions, startChannel);
		}
	}

	/**
	 * Builds a set containing all channels represented by this segment.
	 *
	 * @param node
	 *            the current node to process
	 * @throws NodeException
	 */
	private void addWithRestrictionsRecursively(Node node) throws NodeException {
		if (!restrictions.contains(node)) {
			allNodes.add(node);
			for (Node n : node.getChannels()) {
				addWithRestrictionsRecursively(n);
			}
		}
	}

	/**
	 * Returns the set of channel nodes represented by this tree segment.
	 *
	 * @return set of channel nodes
	 * @throws NodeException
	 */
	public Set<Node> getAllNodes() throws NodeException {
		if (allNodes == null) {
			allNodes = new HashSet<>();
			if (!excluded) {
				addWithRestrictionsRecursively(startChannel);
			} else {
				allNodes.add(startChannel);
			}
		}
		return allNodes;
	}

	/**
	 * Determines whether a given node is contained in this segment.
	 *
	 * @param node
	 *            the node to check for
	 * @return true iff the node is contained in this segment
	 * @throws NodeException
	 */
	public boolean contains(Node node) throws NodeException {
		return getAllNodes().contains(node);
	}

	/**
	 * Checks if this segment has common channels with another segment.
	 *
	 * @param other
	 *            the segment to check
	 * @return true iff at least one channel is contained by both segments
	 * @throws NodeException
	 */
	public boolean intersects(ChannelTreeSegment other) throws NodeException {
		return !CollectionUtils.intersection(getAllNodes(), other.getAllNodes()).isEmpty();
	}

	/**
	 * Gets for the associated object.
	 * @return the associated object
	 */
	public Disinheritable<?> getAssociatedObject() {
		return associatedObject;
	}

	/**
	 * Simplifies a set of end Channels (e.g. disinherited nodes) relative to a
	 * starting node. Channels that are subchannels of other end channels will
	 * be removed, as well as channels that are not channels of the starting
	 * node.
	 *
	 * @param endNodes
	 *            the set of end nodes to be simplified
	 * @param startNode
	 *            the start node to simplify from
	 * @throws NodeException
	 */
	public static void simplifyEndNodes(Set<Node> endNodes, Node startNode) throws NodeException {
		// clean out unrelated nodes
		for (Iterator<Node> i = endNodes.iterator(); i.hasNext();) {
			Node n = i.next();
			if (!n.getMasterNodes().contains(startNode)) {
				i.remove();
			}
		}
		// clean out child nodes of end nodes
		for (Iterator<Node> i = endNodes.iterator(); i.hasNext();) {
			Node n = i.next();
			Set<Node> masterNodes = new HashSet<>(n.getMasterNodes());
			masterNodes.retainAll(endNodes);
			if (!masterNodes.isEmpty()) {
				i.remove();
			}
		}
	}

	/**
	 * Returns the start channel of this segment.
	 * @return the start channel of this segment
	 */
	public Node getStartChannel() {
		return startChannel;
	}

	/**
	 * Returns the restrictions in effect for this segment.
	 * @return the restrictions in effect for this segment
	 */
	public Set<Node> getRestrictions() {
		return new HashSet<>(restrictions);
	}

	/**
	 * Returns whether the channel segment is restricted to the start channel.
	 * @return iff true, the channel segment is restricted to the start channel
	 */
	public boolean isExcluded() {
		return excluded;
	}

	/**
	 * Creates a new ChannelTreeSegment based on the current one, with additional restrictions.
	 * @param excluded whether an exclusion should be added
	 * @param restrictions set of channel nodes to be put as additional channel restrictions
	 * @return the newly constructed channel tree segment
	 * @throws NodeException
	 */
	public ChannelTreeSegment addRestrictions(boolean excluded, Set<Node> restrictions) throws NodeException {
		boolean newExcluded = this.excluded || excluded;
		Set<Node> newRestrictions = Collections.emptySet();
		if (!newExcluded) {
			newRestrictions = new HashSet<>(this.restrictions);
			newRestrictions.addAll(restrictions);
			simplifyEndNodes(newRestrictions, startChannel);
		}
		return new ChannelTreeSegment(startChannel, newExcluded, newRestrictions);
	}
}
