package com.gentics.contentnode.object;

import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * A Stageable node object, that has channels.
 * 
 * @author plyhun
 *
 */
public interface StageableChanneledNodeObject extends StageableNodeObject {

	/**
	 * Get the channel root of this entity, if available
	 * 
	 * @return
	 * @throws NodeException 
	 */
	Optional<Node> maybeGetChannel() throws NodeException;

	/**
	 * Check if the object is inherited from another channel's object.
	 * 
	 * @return
	 * @throws NodeException
	 */
	boolean isInherited() throws NodeException;

	/**
	 * Check whether the object is a master object
	 * @return true for master
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;

	@Override
	default Optional<StageableChanneledNodeObject> maybeHasChannels() throws NodeException {
		return TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)
				? Optional.of(this) : Optional.empty();
	}
}
