package com.gentics.contentnode.object;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;

/**
 * Interface for nodeobjects, that can be localized
 */
public interface LocalizableNodeObject<T> extends NodeObject, NamedNodeObject {

	/**
	 * Return this object
	 * @return this object
	 */
	T getObject();

	/**
	 * Get the channelset of this object. The keys will be the channel ids
	 * (node_ids) and the values will be the object ids. When there is no multichannelling, the map will be empty.
	 * @return channelset of this object.
	 * @throws NodeException
	 */
	Map<Integer, Integer> getChannelSet() throws NodeException;

	/**
	 * Get the channelset id of this object. This will never return null or 0, but will throw an exception, if the object is not new and does not have a channelset_id
	 * If the object is new and does not yet have a channelset_id, a new one will be created and assigned to the object.
	 * @return channelset id (never null or 0)
	 * @throws NodeException
	 */
	Integer getChannelSetId() throws NodeException;

	/**
	 * Get the channel of this object, if one set and multichannelling is supported.
	 * @return current channel of this object
	 * @throws NodeException
	 */
	Node getChannel() throws NodeException;

	/**
	 * Get the node, this object belongs to.
	 * This will return the (master) node of the object's folder (or the master node of the folder, if the object is a folder).
	 * For Templates, this will return null, since templates do not belong to a single node
	 * @return node/channel of the object
	 * @throws NodeException
	 */
	Node getOwningNode() throws NodeException;

	/**
	 * Set the channel information for the object. The channel information consist
	 * of the channelId and the channelSetId. The channelId identifies the
	 * channel, for which the object is valid. If set to 0 (which is the
	 * default), the object is not a localized copy and no local object in a
	 * channel, but is a normal object in a node. The channelSetId groups the
	 * master object and all its localized copies in channel together. This
	 * method may only be called for new objects.
	 * 
	 * @param channel
	 *            id new channel id. If set to 0, this object will be a master
	 *            object in a node and the channelSetId must be given as null
	 *            (which will create a new channelSetId)
	 * @param channelSetId
	 *            id of the channelset. If set to null, a new channelSetId will
	 *            be generated and the object will be a master (in a node or in
	 *            a channel)
	 * @throws ReadOnlyException when the object is not editable
	 * @throws NodeException in case of other errors
	 */
	void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException;

	/**
	 * Variant of {@link #setChannelInfo(Integer, Integer)} that allows changing the information for existing objects (but only, if the channelset id is not changed)
	 * This method is only used during an import
	 * @param channelId channel id
	 * @param channelSetId channelset id
	 * @param allowChange true to allow changing for existing objects
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException;

	/**
	 * Modify the channel id of an existing master object to a higher master
	 * @param channelId id of the higher channel (may be 0)
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void modifyChannelId(Integer channelId) throws ReadOnlyException, NodeException;

	/**
	 * Check whether the object is inherited from a master (in multichannel) into
	 * the current channel
	 * @return true when the object is inherited, false if not or
	 *         multichannelling is disabled
	 * @throws NodeException
	 */
	boolean isInherited() throws NodeException;

	/**
	 * Check whether the object is a master or a localized copy
	 * @return true for master objects, false for localized copies
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;

	/**
	 * Get the master object, if this object is a localized copy. If this
	 * object is not a localized copy or multichannelling is not activated,
	 * returns this object
	 * 
	 * @return master object for localized copies or this object
	 * @throws NodeException
	 */
	LocalizableNodeObject<T> getMaster() throws NodeException;

	/**
	 * If this object is a localized copy, get the next higher object (the object, which would be inherited into the object's channel, if this localized copy did not
	 * exist). If the object is a master, return null.
	 * @return next higher master or null
	 * @throws NodeException
	 */
	LocalizableNodeObject<T> getNextHigherObject() throws NodeException;

	/**
	 * Push this object into the given master
	 * @param master master node to push this object to
	 * @return target object
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	LocalizableNodeObject<T> pushToMaster(Node master) throws ReadOnlyException, NodeException;

	/**
	 * Get the name of the object.
	 * 
	 * @return The current name of object
	 */
	public String getName();

	/**
	 * Set the name of the object.
	 * 
	 * @param name Name that should be set
	 * @return The previous name of the object 
	 * @throws ReadOnlyException
	 * @throws NodeException 
	 */
	public abstract String setName(String name) throws ReadOnlyException, NodeException;
}
