/*
 * @author norbert
 * @date 02.02.2011
 * @version $Id: MultiChannelingFallbackList.java,v 1.1.2.2 2011-02-09 17:45:14 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Node;

/**
 * This class implements the fallback mechanism for multichannelling:
 * <ol>
 * <li>An instance is created for a channel.</li>
 * <li>Then, objects are added with id, channelSetId and channelId</li>
 * <li>Objects with illegal channelId are ignored, if multiple objects with the same channelSetId are added, the "most local" one is stored, all others are ignored.
 * <li>Finally the list of id's can be fetched from the instance</li>
 * @author norbert
 */
public class MultiChannellingFallbackList {

	/**
	 * Map representing the channel inheritance order and maps channelIds to
	 * numbers. Channels with lower numbers always have precedence over channels
	 * with higher numbers
	 */
	protected Map<Object, Integer> channelInheritanceOrder = new HashMap<Object, Integer>();

	/**
	 * list of ids that were added without a channelset id (no fallback is done for those objects)
	 */
	protected List<ObjectInstance> idsWithoutChannelSet = new Vector<ObjectInstance>();

	/**
	 * Map of objects. Keys are the channelsetIds, values contain the object ids and channelIds
	 */
	protected Map<Object, ObjectInstance> idsWithChannelSet = new HashMap<Object, ObjectInstance>();

	/**
	 * List of filtered channel id's. If null, objects for all channel id's are returned
	 */
	protected List<Integer> filteredChannelIds = null;

	/**
	 * node of the channel to do the fallback for
	 */
	private int selectedNodeId;

	/**
	 * whether the selected node is a master node
	 */
	private boolean selectedNodeIsMaster;

	/**
	 * When an object gets disinherited, its id is stored here
	 */
	private Set<Integer> disinheritedObjects = new HashSet<>();

	/**
	 * Create an instance of the fallback list for the given channel
	 * @param channel channel
	 */
	public MultiChannellingFallbackList(Node channel) throws NodeException {
		if (channel == null) {
			throw new NodeException("Cannot build the multichannelling fallback without a channel");
		}

		selectedNodeId = channel.getId();

		selectedNodeIsMaster = !channel.isChannel();

		// we build the inheritance map

		// at first, add the channel itself with lowest number (highest precedence)
		int order = 1;

		channelInheritanceOrder.put(channel.getId(), order++);

		// now get the master nodes
		List<Node> masterNodes = channel.getMasterNodes();

		// and add them all
		for (Node node : masterNodes) {
			channelInheritanceOrder.put(node.getId(), order++);
		}

		// also add channel 0 as master channel
		channelInheritanceOrder.put(0, order++);
	}

	/**
	 * Add the given channel id to the list of filtered channel ids
	 * @param channelId filtered channel id
	 */
	public void addFilteredChannelId(Integer channelId) {
		if (filteredChannelIds == null) {
			filteredChannelIds = new Vector<Integer>();
		}
		filteredChannelIds.add(channelId);
	}

	/**
	 * Adds an object to the fallback list.
	 * @param id id of the object to add
	 * @param channelSetId id of the object's channelset
	 * @param channelId channel id of the object
	 * @param excluded whether the object is excluded from multichannelling
	 * @param disinheritedChannels set of disinherited channels for this object
	 * @throws NodeException
	 */
	public void addObject(Integer id, Integer channelSetId, Integer channelId, boolean excluded, Set<Node> disinheritedChannels) throws NodeException {
		if (disinheritedChannels == null || disinheritedChannels.isEmpty()) {
			addObject(id, channelSetId, channelId, excluded, (Integer)null);
		} else {
			for (Node n : disinheritedChannels) {
				addObject(id, channelSetId, channelId, excluded, n.getId());
			}
		}
	}

	/**
	 * Adds an object to the fallback list.
	 * @param id id of the object to add
	 * @param channelSetId id of the object's channelset
	 * @param channelId channel id of the object
	 * @param excluded whether the object is excluded from multichannelling
	 * @param disinheritedChannelId id of a disinherited channel for the object
	 * @throws NodeException
	 */
	public void addObject(Integer id, Integer channelSetId, Integer channelId, boolean excluded, Integer disinheritedChannelId) throws NodeException {
		// ignore objects with invalid channelId
		if (!channelInheritanceOrder.containsKey(channelId)
				|| disinheritedObjects.contains(id)) {
			return;
		}
		if (disinheritedChannelId != null && disinheritedChannelId != 0 && channelInheritanceOrder.containsKey(disinheritedChannelId)) {
			disinheritedObjects.add(id);
			return;
		}

		// now check whether the channelSetId is empty
		if (AbstractContentObject.isEmptyId(channelSetId)) {
			idsWithoutChannelSet.add(new ObjectInstance(id, channelId));
		} else {
			// create an instance
			ObjectInstance instance = new ObjectInstance(id, channelId);

			// check whether the instance is better then the currently stored
			// with the same channelset id (i.e. there is no instance stored or
			// the new one is more local). Also honor the excluded flag.
			if ((excluded && (channelId == selectedNodeId || (channelId == 0 && selectedNodeIsMaster)))
					|| (!excluded && instance.isBetterThan(idsWithChannelSet.get(channelSetId)))) {
				// store the instance
				idsWithChannelSet.put(channelSetId, instance);
			}
		}
	}

	/**
	 * Get the currently stored object ids. A call to this method will generate
	 * a new list and therefore should not be called to often
	 * @return currently stored object ids
	 */
	public List<Integer> getObjectIds() {
		List<Integer> ids = new Vector<Integer>(idsWithoutChannelSet.size() + idsWithChannelSet.size());

		// return all ids which were stored without a channelset
		for (ObjectInstance instance : idsWithoutChannelSet) {
			// check for filtered channel ids
			if (filteredChannelIds == null || filteredChannelIds.contains(instance.channelId)) {
				ids.add(instance.id);
			}
		}

		// and return the ids of the currently stored instances with channelset
		for (ObjectInstance instance : idsWithChannelSet.values()) {
			if (filteredChannelIds == null || filteredChannelIds.contains(instance.channelId)) {
				ids.add(instance.id);
			}
		}
		ids.removeAll(disinheritedObjects);
		return ids;
	}

	/**
	 * Inner class for grouping object id and channel id together
	 */
	protected class ObjectInstance {

		/**
		 * Object Id of the instance
		 */
		protected Integer id;

		/**
		 * Channel ID
		 */
		protected Integer channelId;

		/**
		 * Channel order of the object
		 */
		protected int channelOrder;

		/**
		 * Create an object instance with given id and channelId
		 * @param id id of the object
		 * @param channelId channelId of the object
		 */
		public ObjectInstance(Integer id, Integer channelId) {
			this.id = id;
			this.channelId = channelId;
			// determine the channel order
			this.channelOrder = channelInheritanceOrder.get(channelId);
		}

		/**
		 * Check whether this object instance is better (i.e. more local) than
		 * the given objectInstance
		 * @param objectInstance object instance to compare with
		 * @return true if this object instance is better and shall be kept in
		 *         the list, false if not
		 */
		public boolean isBetterThan(ObjectInstance objectInstance) {
			// an instance is always better then no instance
			if (objectInstance == null) {
				return true;
			}
			// smaller channel order is better
			return channelOrder <= objectInstance.channelOrder;
		}
	}
}
