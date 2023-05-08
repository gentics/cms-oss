package com.gentics.contentnode.factory.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Wastebin;

/**
 * Store for collections of IDs that are stored per node and wastebin state
 */
public class PerNodeStore implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6589659734910734645L;

	/**
	 * Map of object ids per Node (for each Wastebin State)
	 */
	protected Map<Integer, Map<Wastebin, Collection<Integer>>> idMap = Collections.synchronizedMap(new HashMap<Integer, Map<Wastebin, Collection<Integer>>>());

	/**
	 * Get the data for node and wastebin state. If the data is not yet stored in the ChildStore instance, it
	 * will be created by means of the given create handler
	 * @param nodeId node ID
	 * @param wastebin wastebin state
	 * @param create create handler
	 * @return stored IDs
	 */
	public Collection<Integer> get(Integer nodeId, Wastebin wastebin, CreateHandler create) throws NodeException {
		if (!contains(nodeId, wastebin)) {
			synchronized (this) {
				if (!contains(nodeId, wastebin)) {
					Collection<Integer> ids = create.create(nodeId, wastebin);
					if (ids == null) {
						throw new NodeException("Could not get child list");
					}

					put(nodeId, wastebin, ids);
				}
			}
		}
		return idMap.get(nodeId).get(wastebin);
	}

	/**
	 * Check whether the store contains data for the given node and wastebin state
	 * @param nodeId node ID
	 * @param wastebin wastebin state
	 * @return true iff the store contains data
	 */
	protected boolean contains(Integer nodeId, Wastebin wastebin) {
		if (idMap.get(nodeId) == null) {
			return false;
		} else if (idMap.get(nodeId).get(wastebin) == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Put the data for node and wastebin state
	 * @param nodeId node ID
	 * @param wastebin wastebin state
	 * @param ids collection of IDs
	 */
	protected void put(Integer nodeId, Wastebin wastebin, Collection<Integer> ids) {
		Map<Wastebin, Collection<Integer>> nodeData = idMap.get(nodeId);
		if (nodeData == null) {
			nodeData = new HashMap<>();
			idMap.put(nodeId, nodeData);
		}
		nodeData.put(wastebin, ids);
	}

	/**
	 * Interface for the create handler
	 */
	@FunctionalInterface
	public static interface CreateHandler {
		/**
		 * Create the stored ID collection
		 * @param nodeId node id
		 * @param wastebin wastebin state
		 * @return ID collection (must not be null)
		 * @throws NodeException
		 */
		Collection<Integer> create(Integer nodeId, Wastebin wastebin) throws NodeException;
	}
}
